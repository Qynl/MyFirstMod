package dev.qynl.myfirstmod.unit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionRelation;
import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.*;

public final class UnitWorldData extends PersistentState {
    public final Map<String, UnitDefinition> units = new LinkedHashMap<>();
    public final Map<String, Faction> factions = new LinkedHashMap<>();
    public final Map<UUID, String> equipped = new HashMap<>();
    public final Map<String, net.minecraft.util.math.BlockPos> outposts = new HashMap<>();

    // Simulation Settings
    public int aiTickInterval = 10;
    public boolean buildingEnabled = true;
    public boolean potionsEnabled = true;
    public boolean fireworksEnabled = true;
    public boolean shieldDefenseEnabled = true;
    public boolean friendlyFireAllowed = false;
    public int maxUnits = 100;

    private static final String KEY = "myfirstmod_unit_data";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public UnitWorldData() {
        initDefaultsIfEmpty();
    }

    public static UnitWorldData get(MinecraftServer server) {
        if (server == null) return new UnitWorldData();
        PersistentStateManager psm = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return psm.getOrCreate(new Type<>(UnitWorldData::new, UnitWorldData::fromNbt, null), KEY);
    }

    public static UnitWorldData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        UnitWorldData data = new UnitWorldData();
        data.units.clear();
        data.factions.clear();
        data.equipped.clear();

        if (nbt.contains("units")) {
            NbtList list = nbt.getList("units", 10);
            for (int i = 0; i < list.size(); i++) {
                UnitDefinition def = new UnitDefinition(list.getCompound(i));
                if (def.id != null && !def.id.isBlank() && def.entityId != null) {
                    data.units.put(def.id, def);
                }
            }
        }

        if (nbt.contains("factions")) {
            NbtList list = nbt.getList("factions", 10);
            for (int i = 0; i < list.size(); i++) {
                Faction faction = new Faction(list.getCompound(i));
                if (faction.id != null && !faction.id.isBlank()) {
                    data.factions.put(faction.id, faction);
                }
            }
        }

        if (nbt.contains("equipped")) {
            NbtCompound eq = nbt.getCompound("equipped");
            for (String key : eq.getKeys()) {
                try {
                    data.equipped.put(UUID.fromString(key), eq.getString(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (nbt.contains("outposts")) {
            NbtCompound outpostsTag = nbt.getCompound("outposts");
            for (String factionKey : outpostsTag.getKeys()) {
                int[] coords = outpostsTag.getIntArray(factionKey);
                if (coords.length == 3) {
                    data.outposts.put(factionKey, new net.minecraft.util.math.BlockPos(coords[0], coords[1], coords[2]));
                }
            }
        }

        if (nbt.contains("settings")) {
            NbtCompound s = nbt.getCompound("settings");
            data.aiTickInterval = s.contains("tick_interval") ? s.getInt("tick_interval") : 10;
            data.buildingEnabled = !s.contains("building") || s.getBoolean("building");
            data.potionsEnabled = !s.contains("potions") || s.getBoolean("potions");
            data.fireworksEnabled = !s.contains("fireworks") || s.getBoolean("fireworks");
            data.shieldDefenseEnabled = !s.contains("shield") || s.getBoolean("shield");
            data.friendlyFireAllowed = s.getBoolean("friendly_fire");
            data.maxUnits = s.contains("max_units") ? s.getInt("max_units") : 100;
        }

        data.initDefaultsIfEmpty();
        return data;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList unitList = new NbtList();
        for (UnitDefinition def : units.values()) {
            unitList.add(def.toNbt());
        }
        nbt.put("units", unitList);

        NbtList factionList = new NbtList();
        for (Faction faction : factions.values()) {
            factionList.add(faction.toNbt());
        }
        nbt.put("factions", factionList);

        NbtCompound eq = new NbtCompound();
        for (Map.Entry<UUID, String> entry : equipped.entrySet()) {
            eq.putString(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("equipped", eq);

        NbtCompound outpostsTag = new NbtCompound();
        for (Map.Entry<String, net.minecraft.util.math.BlockPos> entry : outposts.entrySet()) {
            net.minecraft.util.math.BlockPos p = entry.getValue();
            outpostsTag.putIntArray(entry.getKey(), new int[]{p.getX(), p.getY(), p.getZ()});
        }
        nbt.put("outposts", outpostsTag);

        NbtCompound s = new NbtCompound();
        s.putInt("tick_interval", aiTickInterval);
        s.putBoolean("building", buildingEnabled);
        s.putBoolean("potions", potionsEnabled);
        s.putBoolean("fireworks", fireworksEnabled);
        s.putBoolean("shield", shieldDefenseEnabled);
        s.putBoolean("friendly_fire", friendlyFireAllowed);
        s.putInt("max_units", maxUnits);
        nbt.put("settings", s);

        return nbt;
    }

    public void initDefaultsIfEmpty() {
        if (factions.isEmpty()) {
            Faction kingdom = new Faction("kingdom", "Kingdom of Eldoria", "#3B82F6", "Noble kingdom defenders, royal knights, and archers.");
            kingdom.perks.add("military_discipline");
            kingdom.perks.add("heavy_armor");
            kingdom.perks.add("rally");
            kingdom.perks.add("regeneration");

            Faction raiders = new Faction("raiders", "Iron Raiders", "#EF4444", "Savage raiders, berserkers, warlords, and pyrotechnicians.");
            raiders.perks.add("swift_army");
            raiders.perks.add("pyrotechnics");
            raiders.perks.add("veterans");
            raiders.perks.add("berserk_fury");

            Faction villagers = new Faction("villagers", "Village Alliance", "#10B981", "Local militia, civil guards, and healers.");
            villagers.perks.add("holy_might");
            villagers.perks.add("fortification");

            Faction undead = new Faction("undead", "Undead Legion", "#8B5CF6", "Cursed legion of necromancers, dread knights, and snipers.");
            undead.perks.add("necromancy");
            undead.perks.add("night_fighters");
            undead.perks.add("heavy_armor");

            Faction arcane = new Faction("arcane", "Arcane Order", "#06B6D4", "Mystic order of bards, battle mages, and golems.");
            arcane.perks.add("war_song");
            arcane.perks.add("fire_mastery");
            arcane.perks.add("holy_might");

            // Relations setup
            kingdom.relations.put("raiders", FactionRelation.HOSTILE);
            kingdom.relations.put("undead", FactionRelation.HOSTILE);
            kingdom.relations.put("villagers", FactionRelation.ALLIED);
            kingdom.relations.put("arcane", FactionRelation.ALLIED);

            raiders.relations.put("kingdom", FactionRelation.HOSTILE);
            raiders.relations.put("villagers", FactionRelation.HOSTILE);
            raiders.relations.put("undead", FactionRelation.NEUTRAL);
            raiders.relations.put("arcane", FactionRelation.HOSTILE);

            villagers.relations.put("kingdom", FactionRelation.ALLIED);
            villagers.relations.put("raiders", FactionRelation.HOSTILE);
            villagers.relations.put("undead", FactionRelation.HOSTILE);
            villagers.relations.put("arcane", FactionRelation.ALLIED);

            undead.relations.put("kingdom", FactionRelation.HOSTILE);
            undead.relations.put("villagers", FactionRelation.HOSTILE);
            undead.relations.put("arcane", FactionRelation.HOSTILE);

            arcane.relations.put("kingdom", FactionRelation.ALLIED);
            arcane.relations.put("villagers", FactionRelation.ALLIED);
            arcane.relations.put("raiders", FactionRelation.HOSTILE);
            arcane.relations.put("undead", FactionRelation.HOSTILE);

            factions.put(kingdom.id, kingdom);
            factions.put(raiders.id, raiders);
            factions.put(villagers.id, villagers);
            factions.put(undead.id, undead);
            factions.put(arcane.id, arcane);
        }

        if (units.isEmpty()) {
            // 1. Royal Knight
            UnitDefinition knight = new UnitDefinition("royal_knight", "Royal Knight", Identifier.of("minecraft", "villager"));
            knight.description = "Frontline elite tank and swordsman of Eldoria.";
            knight.factionId = "kingdom";
            knight.role = "melee";
            knight.rank = "captain";
            knight.maxHealth = 40.0f;
            knight.attackDamage = 7.5f;
            knight.armor = 15.0f;
            knight.armorToughness = 4.0f;
            knight.knockbackResistance = 0.4f;
            knight.canBlockShield = true;
            knight.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            knight.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            knight.equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            knight.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            knight.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            knight.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            knight.inventory.add(new ItemStack(Items.COOKED_BEEF, 8));
            units.put(knight.id, knight);

            // 2. Royal Archer
            UnitDefinition archer = new UnitDefinition("royal_archer", "Royal Archer", Identifier.of("minecraft", "skeleton"));
            archer.description = "Long-range marksman supporting the Eldorian army.";
            archer.factionId = "kingdom";
            archer.role = "ranged";
            archer.rank = "soldier";
            archer.maxHealth = 24.0f;
            archer.attackDamage = 5.0f;
            archer.retreatHealth = 0.25f;
            archer.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            archer.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            archer.equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
            archer.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            archer.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            archer.inventory.add(new ItemStack(Items.ARROW, 64));
            units.put(archer.id, archer);

            // 3. Field Medic
            UnitDefinition medic = new UnitDefinition("field_medic", "Field Medic", Identifier.of("minecraft", "villager"));
            medic.description = "Dedicated combat healer with splash potions and regeneration rays.";
            medic.factionId = "kingdom";
            medic.role = "medic";
            medic.rank = "specialist";
            medic.maxHealth = 28.0f;
            medic.healRange = 16.0f;
            medic.healAllies = true;
            medic.canThrowPotions = true;
            medic.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            medic.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            medic.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
            medic.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
            medic.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.POTION));
            medic.inventory.add(new ItemStack(Items.SPLASH_POTION, 6));
            medic.inventory.add(new ItemStack(Items.GOLDEN_APPLE, 2));
            units.put(medic.id, medic);

            // 4. Royal Pyrotechnic (Fireworks Artillery)
            UnitDefinition pyro = new UnitDefinition("royal_pyro", "Royal Pyrotechnic", Identifier.of("minecraft", "pillager"));
            pyro.description = "Heavy artillery specialist bombarding enemies with explosive fireworks.";
            pyro.factionId = "kingdom";
            pyro.role = "pyrotechnic";
            pyro.rank = "artillery";
            pyro.maxHealth = 30.0f;
            pyro.canShootFireworks = true;
            pyro.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            pyro.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            pyro.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            pyro.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.FIREWORK_ROCKET));
            pyro.inventory.add(new ItemStack(Items.FIREWORK_ROCKET, 64));
            units.put(pyro.id, pyro);

            // 5. Combat Engineer
            UnitDefinition engineer = new UnitDefinition("combat_engineer", "Combat Engineer", Identifier.of("minecraft", "villager"));
            engineer.description = "Field fortification builder placing defensive barricades and torches.";
            engineer.factionId = "kingdom";
            engineer.role = "engineer";
            engineer.rank = "engineer";
            engineer.maxHealth = 32.0f;
            engineer.canBuild = true;
            engineer.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            engineer.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            engineer.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_PICKAXE));
            engineer.inventory.add(new ItemStack(Items.COBBLESTONE, 64));
            engineer.inventory.add(new ItemStack(Items.TORCH, 16));
            engineer.inventory.add(new ItemStack(Items.LADDER, 8));
            units.put(engineer.id, engineer);

            // 6. Royal Commander
            UnitDefinition commander = new UnitDefinition("royal_commander", "Royal Commander", Identifier.of("minecraft", "vindicator"));
            commander.description = "Tactical leader rallying troops with war horn buffs.";
            commander.factionId = "kingdom";
            commander.role = "melee";
            commander.rank = "commander";
            commander.commander = true;
            commander.squad = "1st Royal Guard";
            commander.maxHealth = 60.0f;
            commander.attackDamage = 9.0f;
            commander.armor = 16.0f;
            commander.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            commander.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            commander.equipment.put(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            commander.equipment.put(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            commander.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_AXE));
            commander.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            units.put(commander.id, commander);

            // 7. Raider Berserker
            UnitDefinition berserker = new UnitDefinition("raider_berserker", "Raider Berserker", Identifier.of("minecraft", "piglin_brute"));
            berserker.description = "Furious warrior charging fearlessly in Bloodrage.";
            berserker.factionId = "raiders";
            berserker.role = "berserker";
            berserker.rank = "soldier";
            berserker.maxHealth = 40.0f;
            berserker.attackDamage = 8.5f;
            berserker.armor = 10.0f;
            berserker.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            berserker.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            berserker.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            units.put(berserker.id, berserker);

            // 8. Raider Crossbowman
            UnitDefinition crossbowman = new UnitDefinition("raider_crossbowman", "Raider Crossbowman", Identifier.of("minecraft", "pillager"));
            crossbowman.description = "Ruthless ranged pillager firing rapid volleys.";
            crossbowman.factionId = "raiders";
            crossbowman.role = "ranged";
            crossbowman.rank = "soldier";
            crossbowman.maxHealth = 26.0f;
            crossbowman.attackDamage = 5.5f;
            crossbowman.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            crossbowman.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            crossbowman.inventory.add(new ItemStack(Items.ARROW, 64));
            units.put(crossbowman.id, crossbowman);

            // 9. Raider Alchemist
            UnitDefinition alchemist = new UnitDefinition("raider_alchemist", "Raider Alchemist", Identifier.of("minecraft", "witch"));
            alchemist.description = "Sinister potion thrower launching poison and harm flasks.";
            alchemist.factionId = "raiders";
            alchemist.role = "support";
            alchemist.rank = "specialist";
            alchemist.maxHealth = 30.0f;
            alchemist.canThrowPotions = true;
            alchemist.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.SPLASH_POTION));
            alchemist.inventory.add(new ItemStack(Items.SPLASH_POTION, 8));
            units.put(alchemist.id, alchemist);

            // 10. Undead Necromancer
            UnitDefinition necro = new UnitDefinition("undead_necromancer", "Undead Necromancer", Identifier.of("minecraft", "evoker"));
            necro.description = "Dark summoner raising undead thralls to overwhelm foes.";
            necro.factionId = "undead";
            necro.role = "necromancer";
            necro.rank = "commander";
            necro.maxHealth = 45.0f;
            necro.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            necro.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            necro.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            units.put(necro.id, necro);

            // 11. Arcane Bard
            UnitDefinition bard = new UnitDefinition("arcane_bard", "Arcane Bard", Identifier.of("minecraft", "villager"));
            bard.description = "Musical spellcaster playing harmonic war songs that buff allies.";
            bard.factionId = "arcane";
            bard.role = "bard";
            bard.rank = "specialist";
            bard.maxHealth = 30.0f;
            bard.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            bard.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            bard.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BARD_LUTE));
            units.put(bard.id, bard);

            // 12. Royal Heavy Cavalry
            UnitDefinition cav = new UnitDefinition("royal_cavalry", "Royal Heavy Cavalry", Identifier.of("minecraft", "villager"));
            cav.description = "Armored mounted knight charging with lance speed.";
            cav.factionId = "kingdom";
            cav.role = "melee";
            cav.rank = "veteran";
            cav.mount = "minecraft:horse";
            cav.maxHealth = 45.0f;
            cav.attackDamage = 9.0f;
            cav.armor = 14.0f;
            cav.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            cav.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            cav.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            cav.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            units.put(cav.id, cav);

            // 13. Dread Skeleton Cavalry
            UnitDefinition dreadCav = new UnitDefinition("dread_cavalry", "Dread Skeleton Cavalry", Identifier.of("minecraft", "wither_skeleton"));
            dreadCav.description = "Terrifying wither knight riding a skeletal steed.";
            dreadCav.factionId = "undead";
            dreadCav.role = "melee";
            dreadCav.rank = "veteran";
            dreadCav.mount = "minecraft:skeleton_horse";
            dreadCav.particleAura = "soul_flame";
            dreadCav.maxHealth = 48.0f;
            dreadCav.attackDamage = 10.0f;
            dreadCav.armor = 12.0f;
            dreadCav.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            dreadCav.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            units.put(dreadCav.id, dreadCav);

            // 14. Holy Paladin
            UnitDefinition paladin = new UnitDefinition("holy_paladin", "Holy Paladin Crusader", Identifier.of("minecraft", "villager"));
            paladin.description = "Divine crusader striking smite lightning and shielding allies.";
            paladin.factionId = "kingdom";
            paladin.role = "paladin";
            paladin.rank = "captain";
            paladin.particleAura = "totem";
            paladin.deathAction = "healing_mist";
            paladin.maxHealth = 55.0f;
            paladin.attackDamage = 8.5f;
            paladin.armor = 16.0f;
            paladin.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
            paladin.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            paladin.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PALADIN_MACE));
            paladin.equipment.put(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            units.put(paladin.id, paladin);

            // 15. Nature Druid
            UnitDefinition druid = new UnitDefinition("nature_druid", "Grove Druid", Identifier.of("minecraft", "villager"));
            druid.description = "Nature mystic casting entangling roots and healing mists.";
            druid.factionId = "villagers";
            druid.role = "druid";
            druid.rank = "specialist";
            druid.particleAura = "heart";
            druid.maxHealth = 32.0f;
            druid.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
            druid.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            druid.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.DRUID_STAFF));
            units.put(druid.id, druid);

            // 16. Siege Bombardier
            UnitDefinition bomb = new UnitDefinition("siege_bombardier", "Siege Bombardier", Identifier.of("minecraft", "piglin"));
            bomb.description = "Heavy sapper launching cluster TNT charges.";
            bomb.factionId = "raiders";
            bomb.role = "bombardier";
            bomb.rank = "artillery";
            bomb.particleAura = "flame";
            bomb.deathAction = "explosion";
            bomb.maxHealth = 35.0f;
            bomb.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            bomb.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            bomb.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BOMBARDIER_MORTAR));
            units.put(bomb.id, bomb);

            // 17. Shadow Assassin
            UnitDefinition assassin = new UnitDefinition("shadow_assassin", "Shadowfang Assassin", Identifier.of("minecraft", "stray"));
            assassin.description = "Stealth assassin wielding an obsidian dagger for devastating backstabs.";
            assassin.factionId = "undead";
            assassin.role = "assassin";
            assassin.rank = "specialist";
            assassin.movementSpeed = 0.32f;
            assassin.maxHealth = 28.0f;
            assassin.attackDamage = 8.0f;
            assassin.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(ModItems.ASSASSIN_DAGGER));
            units.put(assassin.id, assassin);

            // 17. Tempest Breeze
            UnitDefinition breeze = new UnitDefinition("tempest_breeze", "Tempest Breeze", Identifier.of("minecraft", "breeze"));
            breeze.description = "Whirlwind elemental blasting enemies backwards with wind charges.";
            breeze.factionId = "arcane";
            breeze.role = "ranged";
            breeze.rank = "specialist";
            breeze.particleAura = "portal";
            breeze.deathAction = "fireworks";
            breeze.maxHealth = 40.0f;
            units.put(breeze.id, breeze);

            // 18. Poison Bogged
            UnitDefinition bogged = new UnitDefinition("poison_bogged", "Mossy Bogged Sniper", Identifier.of("minecraft", "bogged"));
            bogged.description = "Swamp sharpshooter firing deadly venom-tipped arrows.";
            bogged.factionId = "undead";
            bogged.role = "ranged";
            bogged.rank = "soldier";
            bogged.maxHealth = 26.0f;
            bogged.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            units.put(bogged.id, bogged);

            // 19. Fairy Combat Medic (Allay)
            UnitDefinition allay = new UnitDefinition("fairy_medic", "Celestial Pixie Medic", Identifier.of("minecraft", "allay"));
            allay.description = "Flying aerial medic bestowing fairy dust and rapid regeneration.";
            allay.factionId = "arcane";
            allay.role = "medic";
            allay.rank = "specialist";
            allay.particleAura = "heart";
            allay.maxHealth = 25.0f;
            units.put(allay.id, allay);

            // 20. Colossal Iron Golem Titan
            UnitDefinition golem = new UnitDefinition("iron_titan", "Colossal Iron Titan", Identifier.of("minecraft", "iron_golem"));
            golem.description = "Towering 1.5x scale juggernaut smashing entire enemy ranks.";
            golem.factionId = "villagers";
            golem.role = "tank";
            golem.rank = "warlord";
            golem.scale = 1.35f;
            golem.maxHealth = 150.0f;
            golem.attackDamage = 16.0f;
            golem.armor = 20.0f;
            golem.armorToughness = 8.0f;
            golem.knockbackResistance = 1.0f;
            units.put(golem.id, golem);

            // 21. Savage War Wolf (Beast Archetype)
            UnitDefinition wolf = new UnitDefinition("war_wolf", "Savage War Wolf", Identifier.of("minecraft", "wolf"));
            wolf.description = "Fierce combat beast with razor fangs and adapted armor enhancements.";
            wolf.factionId = "kingdom";
            wolf.role = "berserker";
            wolf.rank = "soldier";
            wolf.scale = 1.15f;
            wolf.maxHealth = 36.0f;
            wolf.attackDamage = 8.0f;
            wolf.movementSpeed = 0.32f;
            wolf.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
            wolf.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.ARMADILLO_SCUTE));
            units.put(wolf.id, wolf);

            // 22. Armored Battle Bear
            UnitDefinition bear = new UnitDefinition("battle_bear", "Armored Battle Bear", Identifier.of("minecraft", "polar_bear"));
            bear.description = "Heavy apex predator juggernaut crushing enemy defenses.";
            bear.factionId = "raiders";
            bear.role = "tank";
            bear.rank = "warlord";
            bear.scale = 1.25f;
            bear.maxHealth = 85.0f;
            bear.attackDamage = 12.0f;
            bear.armor = 14.0f;
            bear.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            bear.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
            units.put(bear.id, bear);

            // 23. Desert Camel Dragoon
            UnitDefinition camelDragoon = new UnitDefinition("camel_dragoon", "Desert Camel Dragoon", Identifier.of("minecraft", "villager"));
            camelDragoon.description = "Mounted marksman elevated high above enemy ranks on a swift desert camel.";
            camelDragoon.factionId = "kingdom";
            camelDragoon.role = "ranged";
            camelDragoon.rank = "veteran";
            camelDragoon.mount = "minecraft:camel";
            camelDragoon.maxHealth = 48.0f;
            camelDragoon.attackDamage = 7.0f;
            camelDragoon.equipment.put(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            camelDragoon.equipment.put(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            camelDragoon.equipment.put(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            camelDragoon.inventory.add(new ItemStack(Items.ARROW, 64));
            units.put(camelDragoon.id, camelDragoon);
        }
    }

    public void saveUnit(UnitDefinition unit) {
        if (unit == null || unit.id == null || unit.id.isBlank()) return;
        units.put(unit.id, unit);
        markDirty();
    }

    public boolean deleteUnit(String id) {
        if (id == null || !units.containsKey(id)) return false;
        units.remove(id);
        markDirty();
        return true;
    }

    public UnitDefinition duplicateUnit(String id) {
        UnitDefinition source = units.get(id);
        if (source == null) return null;
        String newId = id + "_copy";
        int count = 1;
        while (units.containsKey(newId)) {
            count++;
            newId = id + "_copy_" + count;
        }
        UnitDefinition copy = new UnitDefinition(source.toNbt());
        copy.id = newId;
        copy.name = source.name + " (" + count + ")";
        units.put(newId, copy);
        markDirty();
        return copy;
    }

    public void saveFaction(Faction faction) {
        if (faction == null || faction.id == null || faction.id.isBlank()) return;
        factions.put(faction.id, faction);
        markDirty();
    }

    public boolean deleteFaction(String id) {
        if (id == null || !factions.containsKey(id)) return false;
        factions.remove(id);
        markDirty();
        return true;
    }

    public String firstUnit() {
        return units.keySet().stream().findFirst().orElse("");
    }

    public String getEquippedUnit(UUID playerUuid) {
        String id = equipped.get(playerUuid);
        if (id != null && units.containsKey(id)) {
            return id;
        }
        return firstUnit();
    }

    public void setEquippedUnit(UUID playerUuid, String unitId) {
        equipped.put(playerUuid, unitId);
        markDirty();
    }

    public String cycleEquippedUnit(UUID playerUuid) {
        return cycleEquippedUnit(playerUuid, 1);
    }

    public String cycleEquippedUnit(UUID playerUuid, int direction) {
        if (units.isEmpty()) return "";
        List<String> keys = new ArrayList<>(units.keySet());
        String current = equipped.get(playerUuid);
        int index = current == null ? 0 : keys.indexOf(current);
        if (index == -1) index = 0;
        int nextIndex = (index + direction + keys.size()) % keys.size();
        String nextUnitId = keys.get(nextIndex);
        equipped.put(playerUuid, nextUnitId);
        markDirty();
        return nextUnitId;
    }

    public String exportToJson(String unitId) {
        UnitDefinition unit = units.get(unitId);
        if (unit == null) return "{}";
        return GSON.toJson(unit.toJson());
    }

    public boolean importFromJson(String jsonString) {
        try {
            JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
            UnitDefinition def = UnitDefinition.fromJson(obj);
            if (def.id != null && !def.id.isBlank()) {
                saveUnit(def);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
