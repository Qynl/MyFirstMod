package dev.qynl.myfirstmod.gui;

import dev.qynl.myfirstmod.battle.BattleSandbox;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.faction.FactionRelation;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CreatorScreenHandler extends ScreenHandler {
    public static final EquipmentSlot[] EQUIPMENT_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    private static final Identifier[] ENTITY_CYCLE = {
            Identifier.of("minecraft", "villager"),
            Identifier.of("minecraft", "skeleton"),
            Identifier.of("minecraft", "zombie"),
            Identifier.of("minecraft", "pillager"),
            Identifier.of("minecraft", "vindicator"),
            Identifier.of("minecraft", "piglin_brute"),
            Identifier.of("minecraft", "piglin"),
            Identifier.of("minecraft", "witch"),
            Identifier.of("minecraft", "evoker"),
            Identifier.of("minecraft", "iron_golem"),
            Identifier.of("minecraft", "wolf"),
            Identifier.of("minecraft", "breeze"),
            Identifier.of("minecraft", "bogged"),
            Identifier.of("minecraft", "allay"),
            Identifier.of("minecraft", "blaze"),
            Identifier.of("minecraft", "wither_skeleton"),
            Identifier.of("minecraft", "stray"),
            Identifier.of("minecraft", "drowned"),
            Identifier.of("minecraft", "warden"),
            Identifier.of("minecraft", "ravager")
    };

    private static final String[] ROLE_CYCLE = {
            "melee", "ranged", "medic", "pyrotechnic", "engineer", "tank", "assassin", "scout", "necromancer", "berserker", "bard", "bombardier", "druid", "paladin", "support"
    };

    private static final String[] RANK_CYCLE = {
            "soldier", "veteran", "captain", "commander", "warlord", "specialist", "recruit"
    };

    private static final String[] MOUNT_CYCLE = {
            "", "minecraft:horse", "minecraft:skeleton_horse", "minecraft:spider", "minecraft:wolf", "minecraft:ravager"
    };

    private static final String[] AURA_CYCLE = {
            "none", "flame", "soul_flame", "enchanted", "portal", "heart", "totem", "electric_spark"
    };

    private static final String[] DEATH_CYCLE = {
            "none", "fireworks", "healing_mist", "explosion", "lightning", "poison_cloud"
    };

    private static final float[] SCALE_CYCLE = {
            0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f
    };

    private static int battleFactionAIdx = 0;
    private static int battleFactionBIdx = 1;

    public final SimpleInventory equipmentInventory = new SimpleInventory(6);
    public final SimpleInventory unitInventory = new SimpleInventory(9);
    private final PlayerInventory playerInventory;

    public CreatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.CREATOR, syncId);
        this.playerInventory = playerInventory;

        // Equipment slots (0..5)
        for (int i = 0; i < 6; i++) {
            this.addSlot(new Slot(equipmentInventory, i, 255 + (i * 20), 140));
        }

        // Unit Inventory slots (6..14) -> 9 slots (3x3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                this.addSlot(new Slot(unitInventory, index, 255 + (col * 20), 168 + (row * 20)));
            }
        }

        // Player Inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 20 + col * 18, 160 + row * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 20 + col * 18, 218));
        }

        if (playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());
            String equippedId = data.getEquippedUnit(serverPlayer.getUuid());
            UnitDefinition unit = data.units.get(equippedId);
            if (unit == null) unit = data.units.get(data.firstUnit());
            if (unit != null) {
                loadUnitIntoSlots(unit);
            }
        }
    }

    public void loadUnitIntoSlots(UnitDefinition unit) {
        if (unit == null) return;
        for (int i = 0; i < 6; i++) {
            equipmentInventory.setStack(i, unit.equipment.getOrDefault(EQUIPMENT_SLOTS[i], ItemStack.EMPTY).copy());
        }
        for (int i = 0; i < 9; i++) {
            if (i < unit.inventory.size()) {
                unitInventory.setStack(i, unit.inventory.get(i).copy());
            } else {
                unitInventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    public void saveSlotsToUnit(UnitDefinition unit) {
        if (unit == null) return;
        for (int i = 0; i < 6; i++) {
            ItemStack stack = equipmentInventory.getStack(i);
            if (!stack.isEmpty()) {
                unit.equipment.put(EQUIPMENT_SLOTS[i], stack.copy());
            } else {
                unit.equipment.remove(EQUIPMENT_SLOTS[i]);
            }
        }
        unit.inventory.clear();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = unitInventory.getStack(i);
            if (!stack.isEmpty()) {
                unit.inventory.add(stack.copy());
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack originalStack = slot.getStack();
        ItemStack copy = originalStack.copy();

        // If from editor slots (0..14), move to player inventory (15..50)
        if (slotIndex < 15) {
            if (!this.insertItem(originalStack, 15, 51, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // From player inventory to editor slots
            if (!this.insertItem(originalStack, 0, 15, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (originalStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return copy;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return true;
        UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

        switch (id) {
            case 0 -> { // Save equipment/inventory to currently equipped unit
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    saveSlotsToUnit(unit);
                    data.markDirty();
                }
            }
            case 1 -> { // Spawn 1 equipped unit
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    UnitSpawner.spawnAtPlayer(serverPlayer, unit);
                }
            }
            case 2 -> { // Spawn squad (5 units)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    UnitSpawner.spawnSquadAtPlayer(serverPlayer, unit, 5);
                }
            }
            case 3 -> { // Cycle to next unit
                String nextId = data.cycleEquippedUnit(player.getUuid(), 1);
                UnitDefinition unit = data.units.get(nextId);
                if (unit != null) {
                    loadUnitIntoSlots(unit);
                }
            }
            case 6 -> { // Cycle to prev unit
                String nextId = data.cycleEquippedUnit(player.getUuid(), -1);
                UnitDefinition unit = data.units.get(nextId);
                if (unit != null) {
                    loadUnitIntoSlots(unit);
                }
            }
            case 4 -> { // Duplicate currently equipped unit
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition copy = data.duplicateUnit(equippedId);
                if (copy != null) {
                    data.setEquippedUnit(player.getUuid(), copy.id);
                    loadUnitIntoSlots(copy);
                }
            }
            case 5 -> { // Delete currently equipped unit
                String equippedId = data.getEquippedUnit(player.getUuid());
                if (!equippedId.isBlank()) {
                    data.deleteUnit(equippedId);
                    String next = data.firstUnit();
                    data.setEquippedUnit(player.getUuid(), next);
                    loadUnitIntoSlots(data.units.get(next));
                }
            }
            case 10 -> { // Start standard battle (8 vs 8)
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                String fa = fKeys.get(Math.abs(battleFactionAIdx) % fKeys.size());
                String fb = fKeys.get(Math.abs(battleFactionBIdx) % fKeys.size());
                BattleSandbox.startBattle(serverPlayer, fa, fb, 8);
            }
            case 11 -> { // Clear all battle mobs
                BattleSandbox.clearAllBattleMobs(serverPlayer.getServerWorld());
            }
            case 12 -> { // Reset battle stats
                BattleStats.get(serverPlayer.getServer()).reset();
            }
            case 20 -> { // Start large battle (16 vs 16)
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                String fa = fKeys.get(Math.abs(battleFactionAIdx) % fKeys.size());
                String fb = fKeys.get(Math.abs(battleFactionBIdx) % fKeys.size());
                BattleSandbox.startBattle(serverPlayer, fa, fb, 16);
            }
            case 21 -> { // Start massive battle (24 vs 24)
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                String fa = fKeys.get(Math.abs(battleFactionAIdx) % fKeys.size());
                String fb = fKeys.get(Math.abs(battleFactionBIdx) % fKeys.size());
                BattleSandbox.startBattle(serverPlayer, fa, fb, 24);
            }
            case 22 -> { // Start mega war (32 vs 32)
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                String fa = fKeys.get(Math.abs(battleFactionAIdx) % fKeys.size());
                String fb = fKeys.get(Math.abs(battleFactionBIdx) % fKeys.size());
                BattleSandbox.startBattle(serverPlayer, fa, fb, 32);
            }
            case 23 -> { // Start titan clash (48 vs 48)
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                String fa = fKeys.get(Math.abs(battleFactionAIdx) % fKeys.size());
                String fb = fKeys.get(Math.abs(battleFactionBIdx) % fKeys.size());
                BattleSandbox.startBattle(serverPlayer, fa, fb, 48);
            }
            case 65 -> { // Cycle Faction A for battle
                battleFactionAIdx++;
            }
            case 66 -> { // Cycle Faction B for battle
                battleFactionBIdx++;
            }
            case 30 -> { // Restore default presets
                data.units.clear();
                data.factions.clear();
                data.initDefaultsIfEmpty();
                data.markDirty();
                loadUnitIntoSlots(data.units.get(data.firstUnit()));
            }
            case 31 -> { // Toggle building AI
                data.buildingEnabled = !data.buildingEnabled;
                data.markDirty();
            }
            case 32 -> { // Toggle potion throwing AI
                data.potionsEnabled = !data.potionsEnabled;
                data.markDirty();
            }
            case 33 -> { // Toggle fireworks artillery AI
                data.fireworksEnabled = !data.fireworksEnabled;
                data.markDirty();
            }
            case 34 -> { // Toggle shield defense AI
                data.shieldDefenseEnabled = !data.shieldDefenseEnabled;
                data.markDirty();
            }
            case 35 -> { // Toggle friendly fire
                data.friendlyFireAllowed = !data.friendlyFireAllowed;
                data.markDirty();
            }
            case 49 -> { // Prev Base Entity
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.entityId = dev.qynl.myfirstmod.entity.DynamicEntityRegistry.cycleEntity(unit.entityId, -1);
                    data.markDirty();
                }
            }
            case 50 -> { // Cycle Base Entity (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.entityId = dev.qynl.myfirstmod.entity.DynamicEntityRegistry.cycleEntity(unit.entityId, 1);
                    data.markDirty();
                }
            }
            case 48 -> { // Prev Role
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < ROLE_CYCLE.length; i++) {
                        if (ROLE_CYCLE[i].equalsIgnoreCase(unit.role)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.role = ROLE_CYCLE[(currentIndex - 1 + ROLE_CYCLE.length) % ROLE_CYCLE.length];
                    data.markDirty();
                }
            }
            case 51 -> { // Cycle Role (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < ROLE_CYCLE.length; i++) {
                        if (ROLE_CYCLE[i].equalsIgnoreCase(unit.role)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.role = ROLE_CYCLE[(currentIndex + 1) % ROLE_CYCLE.length];
                    data.markDirty();
                }
            }
            case 47 -> { // Prev Faction
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null && !data.factions.isEmpty()) {
                    List<String> fKeys = new ArrayList<>(data.factions.keySet());
                    int idx = fKeys.indexOf(unit.factionId);
                    unit.factionId = fKeys.get((idx - 1 + fKeys.size()) % fKeys.size());
                    data.markDirty();
                }
            }
            case 52 -> { // Cycle Faction (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null && !data.factions.isEmpty()) {
                    List<String> fKeys = new ArrayList<>(data.factions.keySet());
                    int idx = fKeys.indexOf(unit.factionId);
                    unit.factionId = fKeys.get((idx + 1) % fKeys.size());
                    data.markDirty();
                }
            }
            case 46 -> { // Prev Rank
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < RANK_CYCLE.length; i++) {
                        if (RANK_CYCLE[i].equalsIgnoreCase(unit.rank)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.rank = RANK_CYCLE[(currentIndex - 1 + RANK_CYCLE.length) % RANK_CYCLE.length];
                    data.markDirty();
                }
            }
            case 53 -> { // Cycle Rank (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < RANK_CYCLE.length; i++) {
                        if (RANK_CYCLE[i].equalsIgnoreCase(unit.rank)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.rank = RANK_CYCLE[(currentIndex + 1) % RANK_CYCLE.length];
                    data.markDirty();
                }
            }
            case 54 -> { // Toggle Commander
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.commander = !unit.commander;
                    data.markDirty();
                }
            }
            case 55 -> { // Cycle Mount (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.mount = dev.qynl.myfirstmod.entity.DynamicEntityRegistry.cycleMount(unit.mount, 1);
                    data.markDirty();
                }
            }
            case 59 -> { // Prev Mount
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    unit.mount = dev.qynl.myfirstmod.entity.DynamicEntityRegistry.cycleMount(unit.mount, -1);
                    data.markDirty();
                }
            }
            case 45 -> { // Prev Aura
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < AURA_CYCLE.length; i++) {
                        if (AURA_CYCLE[i].equalsIgnoreCase(unit.particleAura)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.particleAura = AURA_CYCLE[(currentIndex - 1 + AURA_CYCLE.length) % AURA_CYCLE.length];
                    data.markDirty();
                }
            }
            case 56 -> { // Cycle Aura (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < AURA_CYCLE.length; i++) {
                        if (AURA_CYCLE[i].equalsIgnoreCase(unit.particleAura)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.particleAura = AURA_CYCLE[(currentIndex + 1) % AURA_CYCLE.length];
                    data.markDirty();
                }
            }
            case 44 -> { // Prev Death Action
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < DEATH_CYCLE.length; i++) {
                        if (DEATH_CYCLE[i].equalsIgnoreCase(unit.deathAction)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.deathAction = DEATH_CYCLE[(currentIndex - 1 + DEATH_CYCLE.length) % DEATH_CYCLE.length];
                    data.markDirty();
                }
            }
            case 57 -> { // Cycle Death Action (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 0;
                    for (int i = 0; i < DEATH_CYCLE.length; i++) {
                        if (DEATH_CYCLE[i].equalsIgnoreCase(unit.deathAction)) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.deathAction = DEATH_CYCLE[(currentIndex + 1) % DEATH_CYCLE.length];
                    data.markDirty();
                }
            }
            case 43 -> { // Prev Scale
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 2; // default 1.0f
                    for (int i = 0; i < SCALE_CYCLE.length; i++) {
                        if (Math.abs(SCALE_CYCLE[i] - unit.scale) < 0.05f) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.scale = SCALE_CYCLE[(currentIndex - 1 + SCALE_CYCLE.length) % SCALE_CYCLE.length];
                    data.markDirty();
                }
            }
            case 58 -> { // Cycle Scale (Next)
                String equippedId = data.getEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(equippedId);
                if (unit != null) {
                    int currentIndex = 2; // default 1.0f
                    for (int i = 0; i < SCALE_CYCLE.length; i++) {
                        if (Math.abs(SCALE_CYCLE[i] - unit.scale) < 0.05f) {
                            currentIndex = i;
                            break;
                        }
                    }
                    unit.scale = SCALE_CYCLE[(currentIndex + 1) % SCALE_CYCLE.length];
                    data.markDirty();
                }
            }
            case 70 -> { // Cycle Kingdom ↔ Raiders Relation
                Faction k = data.factions.get("kingdom");
                Faction r = data.factions.get("raiders");
                if (k != null && r != null) {
                    FactionRelation next = k.relations.getOrDefault("raiders", FactionRelation.HOSTILE).next();
                    k.relations.put("raiders", next);
                    r.relations.put("kingdom", next);
                    data.markDirty();
                }
            }
            case 71 -> { // Cycle Kingdom ↔ Villagers Relation
                Faction k = data.factions.get("kingdom");
                Faction v = data.factions.get("villagers");
                if (k != null && v != null) {
                    FactionRelation next = k.relations.getOrDefault("villagers", FactionRelation.ALLIED).next();
                    k.relations.put("villagers", next);
                    v.relations.put("kingdom", next);
                    data.markDirty();
                }
            }
            case 72 -> { // Cycle Raiders ↔ Villagers Relation
                Faction r = data.factions.get("raiders");
                Faction v = data.factions.get("villagers");
                if (r != null && v != null) {
                    FactionRelation next = r.relations.getOrDefault("villagers", FactionRelation.HOSTILE).next();
                    r.relations.put("villagers", next);
                    v.relations.put("raiders", next);
                    data.markDirty();
                }
            }
            case 80 -> loadPresetToEquipped(data, player.getUuid(), "royal_cavalry");
            case 81 -> loadPresetToEquipped(data, player.getUuid(), "war_wolf");
            case 82 -> loadPresetToEquipped(data, player.getUuid(), "battle_bear");
            case 83 -> loadPresetToEquipped(data, player.getUuid(), "iron_titan");
            case 84 -> loadPresetToEquipped(data, player.getUuid(), "camel_dragoon");
            case 85 -> loadPresetToEquipped(data, player.getUuid(), "holy_paladin");
        }

        return true;
    }

    private void loadPresetToEquipped(UnitWorldData data, UUID playerUuid, String templateId) {
        UnitDefinition template = data.units.get(templateId);
        if (template != null) {
            data.setEquippedUnit(playerUuid, template.id);
            loadUnitIntoSlots(template);
        }
    }
}
