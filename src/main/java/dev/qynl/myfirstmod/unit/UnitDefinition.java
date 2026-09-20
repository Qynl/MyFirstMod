package dev.qynl.myfirstmod.unit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.qynl.myfirstmod.equipment.ItemCapabilities;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UnitDefinition {
    public String id = "custom_unit";
    public String name = "Custom Unit";
    public String description = "";
    public Identifier entityId = Identifier.of("minecraft", "villager");
    public String factionId = "kingdom";

    // Attributes
    public float maxHealth = 20.0f;
    public float movementSpeed = 0.25f;
    public float attackDamage = 3.0f;
    public float followRange = 24.0f;
    public float armor = 0.0f;
    public float armorToughness = 0.0f;
    public float knockbackResistance = 0.0f;
    public float attackSpeed = 4.0f;
    public float scale = 1.0f;

    // Role, Rank & Organization
    public String role = "melee"; // melee, ranged, medic, pyrotechnic, engineer, tank, assassin, scout, necromancer, berserker, bard, bombardier, druid, paladin, support
    public String rank = "soldier"; // recruit, soldier, veteran, captain, commander, warlord, specialist
    public String squad = "";
    public boolean commander = false;

    // Mount & Aesthetics
    public String mount = ""; // "", "minecraft:horse", "minecraft:skeleton_horse", "minecraft:ravager", "minecraft:spider", "minecraft:wolf"
    public String particleAura = "none"; // none, flame, soul_flame, enchanted, portal, heart, totem, electric_spark
    public String deathAction = "none"; // none, fireworks, healing_mist, explosion
    public boolean enchantedGear = false;

    // Behavior & Targeting
    public String targetPriority = "nearest"; // nearest, commander, medic, ranged, weakest, players
    public boolean attackHostile = true;
    public boolean protectAllies = true;
    public boolean attackPlayers = false;
    public boolean healAllies = true;
    public float healRange = 14.0f;
    public float retreatHealth = 0.0f; // 0 = disabled, 0.25 = retreat below 25% hp
    public boolean canBlockShield = true;
    public boolean canUsePotions = true;
    public boolean canThrowPotions = true;
    public boolean canShootFireworks = true;
    public boolean canBuild = true;
    public boolean infiniteAmmo = false;

    // Variants
    public String variant = "";
    public boolean isBaby = false;

    // Equipment, Inventory & Passive Effects
    public final Map<EquipmentSlot, ItemStack> equipment = new LinkedHashMap<>();
    public final List<ItemStack> inventory = new ArrayList<>();
    public final List<String> passiveEffects = new ArrayList<>();

    public UnitDefinition(String id, String name, Identifier entityId) {
        this.id = id;
        this.name = name;
        this.entityId = entityId;
    }

    public UnitDefinition(NbtCompound nbt) {
        this.id = nbt.getString("id");
        this.name = nbt.getString("name");
        this.description = nbt.getString("description");
        this.entityId = Identifier.tryParse(nbt.getString("entity"));
        if (this.entityId == null) this.entityId = Identifier.of("minecraft", "villager");
        this.factionId = nbt.getString("faction");

        this.maxHealth = nbt.contains("health") ? nbt.getFloat("health") : 20.0f;
        this.movementSpeed = nbt.contains("speed") ? nbt.getFloat("speed") : 0.25f;
        this.attackDamage = nbt.contains("damage") ? nbt.getFloat("damage") : 3.0f;
        this.followRange = nbt.contains("range") ? nbt.getFloat("range") : 24.0f;
        this.armor = nbt.getFloat("armor");
        this.armorToughness = nbt.getFloat("toughness");
        this.knockbackResistance = nbt.getFloat("knockback");
        this.attackSpeed = nbt.contains("attack_speed") ? nbt.getFloat("attack_speed") : 4.0f;
        this.scale = nbt.contains("scale") ? nbt.getFloat("scale") : 1.0f;

        this.role = nbt.contains("role") ? nbt.getString("role") : "melee";
        this.rank = nbt.contains("rank") ? nbt.getString("rank") : "soldier";
        this.squad = nbt.getString("squad");
        this.commander = nbt.getBoolean("commander");

        this.mount = nbt.getString("mount");
        this.particleAura = nbt.contains("aura") ? nbt.getString("aura") : "none";
        this.deathAction = nbt.contains("death_action") ? nbt.getString("death_action") : "none";
        this.enchantedGear = nbt.getBoolean("enchanted");

        this.targetPriority = nbt.contains("priority") ? nbt.getString("priority") : "nearest";
        this.attackHostile = !nbt.contains("attack_hostile") || nbt.getBoolean("attack_hostile");
        this.protectAllies = !nbt.contains("protect_allies") || nbt.getBoolean("protect_allies");
        this.attackPlayers = nbt.getBoolean("attack_players");
        this.healAllies = !nbt.contains("heal_allies") || nbt.getBoolean("heal_allies");
        this.healRange = nbt.contains("heal_range") ? nbt.getFloat("heal_range") : 14.0f;
        this.retreatHealth = nbt.getFloat("retreat");
        this.canBlockShield = !nbt.contains("can_block") || nbt.getBoolean("can_block");
        this.canUsePotions = !nbt.contains("can_potions") || nbt.getBoolean("can_potions");
        this.canThrowPotions = !nbt.contains("can_throw_potions") || nbt.getBoolean("can_throw_potions");
        this.canShootFireworks = !nbt.contains("can_fireworks") || nbt.getBoolean("can_fireworks");
        this.canBuild = !nbt.contains("can_build") || nbt.getBoolean("can_build");
        this.infiniteAmmo = nbt.getBoolean("infinite_ammo");

        this.variant = nbt.getString("variant");
        this.isBaby = nbt.getBoolean("is_baby");

        RegistryWrapper.WrapperLookup lookup = DynamicRegistryManager.of(Registries.REGISTRIES);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (nbt.contains("item_" + slot.getName())) {
                equipment.put(slot, ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("item_" + slot.getName())));
            }
        }

        if (nbt.contains("inventory")) {
            NbtList list = nbt.getList("inventory", 10);
            for (int i = 0; i < list.size(); i++) {
                inventory.add(ItemStack.fromNbtOrEmpty(lookup, list.getCompound(i)));
            }
        }

        if (nbt.contains("passives")) {
            NbtList list = nbt.getList("passives", 8);
            for (int i = 0; i < list.size(); i++) {
                passiveEffects.add(list.getString(i));
            }
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id == null ? "" : id);
        nbt.putString("name", name == null ? "" : name);
        nbt.putString("description", description == null ? "" : description);
        nbt.putString("entity", entityId == null ? "minecraft:villager" : entityId.toString());
        nbt.putString("faction", factionId == null ? "independent" : factionId);

        nbt.putFloat("health", maxHealth);
        nbt.putFloat("speed", movementSpeed);
        nbt.putFloat("damage", attackDamage);
        nbt.putFloat("range", followRange);
        nbt.putFloat("armor", armor);
        nbt.putFloat("toughness", armorToughness);
        nbt.putFloat("knockback", knockbackResistance);
        nbt.putFloat("attack_speed", attackSpeed);
        nbt.putFloat("scale", scale);

        nbt.putString("role", role == null ? "melee" : role);
        nbt.putString("rank", rank == null ? "soldier" : rank);
        nbt.putString("squad", squad == null ? "" : squad);
        nbt.putBoolean("commander", commander);

        nbt.putString("mount", mount == null ? "" : mount);
        nbt.putString("aura", particleAura == null ? "none" : particleAura);
        nbt.putString("death_action", deathAction == null ? "none" : deathAction);
        nbt.putBoolean("enchanted", enchantedGear);

        nbt.putString("priority", targetPriority == null ? "nearest" : targetPriority);
        nbt.putBoolean("attack_hostile", attackHostile);
        nbt.putBoolean("protect_allies", protectAllies);
        nbt.putBoolean("attack_players", attackPlayers);
        nbt.putBoolean("heal_allies", healAllies);
        nbt.putFloat("heal_range", healRange);
        nbt.putFloat("retreat", retreatHealth);
        nbt.putBoolean("can_block", canBlockShield);
        nbt.putBoolean("can_potions", canUsePotions);
        nbt.putBoolean("can_throw_potions", canThrowPotions);
        nbt.putBoolean("can_fireworks", canShootFireworks);
        nbt.putBoolean("can_build", canBuild);
        nbt.putBoolean("infinite_ammo", infiniteAmmo);

        nbt.putString("variant", variant == null ? "" : variant);
        nbt.putBoolean("is_baby", isBaby);

        RegistryWrapper.WrapperLookup lookup = DynamicRegistryManager.of(Registries.REGISTRIES);
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                nbt.put("item_" + entry.getKey().getName(), entry.getValue().encode(lookup));
            }
        }

        NbtList list = new NbtList();
        for (ItemStack stack : inventory) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack.encode(lookup));
            }
        }
        nbt.put("inventory", list);

        NbtList pList = new NbtList();
        for (String p : passiveEffects) {
            pList.add(NbtString.of(p));
        }
        nbt.put("passives", pList);

        return nbt;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("entity", entityId.toString());
        json.addProperty("faction", factionId);

        JsonObject attrs = new JsonObject();
        attrs.addProperty("max_health", maxHealth);
        attrs.addProperty("movement_speed", movementSpeed);
        attrs.addProperty("attack_damage", attackDamage);
        attrs.addProperty("follow_range", followRange);
        attrs.addProperty("armor", armor);
        attrs.addProperty("armor_toughness", armorToughness);
        attrs.addProperty("knockback_resistance", knockbackResistance);
        attrs.addProperty("scale", scale);
        json.add("attributes", attrs);

        JsonObject org = new JsonObject();
        org.addProperty("role", role);
        org.addProperty("rank", rank);
        org.addProperty("squad", squad);
        org.addProperty("commander", commander);
        org.addProperty("mount", mount);
        org.addProperty("aura", particleAura);
        org.addProperty("death_action", deathAction);
        json.add("organization", org);

        JsonObject beh = new JsonObject();
        beh.addProperty("target_priority", targetPriority);
        beh.addProperty("attack_hostile", attackHostile);
        beh.addProperty("protect_allies", protectAllies);
        beh.addProperty("attack_players", attackPlayers);
        beh.addProperty("heal_allies", healAllies);
        beh.addProperty("heal_range", healRange);
        beh.addProperty("retreat_health", retreatHealth);
        beh.addProperty("can_block", canBlockShield);
        beh.addProperty("can_potions", canUsePotions);
        beh.addProperty("can_throw_potions", canThrowPotions);
        beh.addProperty("can_fireworks", canShootFireworks);
        beh.addProperty("can_build", canBuild);
        beh.addProperty("infinite_ammo", infiniteAmmo);
        json.add("behavior", beh);

        return json;
    }

    public static UnitDefinition fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : id;
        Identifier entityId = json.has("entity") ? Identifier.tryParse(json.get("entity").getAsString()) : Identifier.of("minecraft", "villager");
        UnitDefinition def = new UnitDefinition(id, name, entityId);

        if (json.has("description")) def.description = json.get("description").getAsString();
        if (json.has("faction")) def.factionId = json.get("faction").getAsString();

        if (json.has("attributes")) {
            JsonObject a = json.getAsJsonObject("attributes");
            if (a.has("max_health")) def.maxHealth = a.get("max_health").getAsFloat();
            if (a.has("movement_speed")) def.movementSpeed = a.get("movement_speed").getAsFloat();
            if (a.has("attack_damage")) def.attackDamage = a.get("attack_damage").getAsFloat();
            if (a.has("follow_range")) def.followRange = a.get("follow_range").getAsFloat();
            if (a.has("armor")) def.armor = a.get("armor").getAsFloat();
            if (a.has("armor_toughness")) def.armorToughness = a.get("armor_toughness").getAsFloat();
            if (a.has("knockback_resistance")) def.knockbackResistance = a.get("knockback_resistance").getAsFloat();
            if (a.has("scale")) def.scale = a.get("scale").getAsFloat();
        }

        if (json.has("organization")) {
            JsonObject o = json.getAsJsonObject("organization");
            if (o.has("role")) def.role = o.get("role").getAsString();
            if (o.has("rank")) def.rank = o.get("rank").getAsString();
            if (o.has("squad")) def.squad = o.get("squad").getAsString();
            if (o.has("commander")) def.commander = o.get("commander").getAsBoolean();
            if (o.has("mount")) def.mount = o.get("mount").getAsString();
            if (o.has("aura")) def.particleAura = o.get("aura").getAsString();
            if (o.has("death_action")) def.deathAction = o.get("death_action").getAsString();
        }

        return def;
    }

    public void apply(LivingEntity entity) {
        applyAttributes(entity);
        applyEquipment(entity);
        applyInventory(entity);
        applyVariants(entity);
    }

    public void applyAttributes(LivingEntity entity) {
        setAttribute(EntityAttributes.GENERIC_MAX_HEALTH, maxHealth, entity);
        setAttribute(EntityAttributes.GENERIC_MOVEMENT_SPEED, movementSpeed, entity);
        setAttribute(EntityAttributes.GENERIC_ATTACK_DAMAGE, attackDamage, entity);
        setAttribute(EntityAttributes.GENERIC_FOLLOW_RANGE, followRange, entity);
        setAttribute(EntityAttributes.GENERIC_ARMOR, armor, entity);
        setAttribute(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, armorToughness, entity);
        setAttribute(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, knockbackResistance, entity);
        setAttribute(EntityAttributes.GENERIC_ATTACK_SPEED, attackSpeed, entity);
        setAttribute(EntityAttributes.GENERIC_SCALE, scale, entity);

        entity.setHealth(Math.min(maxHealth, entity.getMaxHealth()));
    }

    public void applyEquipment(LivingEntity entity) {
        dev.qynl.myfirstmod.equipment.MobEquipmentAdapter.applyEquipment(entity, this);
    }

    public void applyInventory(LivingEntity entity) {
        if (entity instanceof InventoryOwner owner) {
            Inventory inv = owner.getInventory();
            for (int i = 0; i < inventory.size() && i < inv.size(); i++) {
                inv.setStack(i, inventory.get(i).copy());
            }
        }
    }

    public void applyVariants(LivingEntity entity) {
        if (entity instanceof VillagerEntity villager) {
            if (this.isBaby) villager.setBaby(true);
        }
    }

    private static void setAttribute(RegistryEntry<EntityAttribute> attribute, double value, LivingEntity entity) {
        if (attribute == null) return;
        var instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(Math.max(0.01, value));
        }
    }
}
