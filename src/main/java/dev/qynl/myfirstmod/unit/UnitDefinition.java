package dev.qynl.myfirstmod.unit;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

/** A data-only unit template. It intentionally contains no entity instances. */
public final class UnitDefinition {
    public String id;
    public String name;
    public String description;
    public Identifier entityId;
    public String factionId = "independent";
    public float maxHealth = 20;
    public float movementSpeed = 0.1f;
    public float attackDamage = 2;
    public float followRange = 16;
    public String role = "melee";
    public boolean attackHostile = true;
    public boolean protectAllies = true;
    public boolean attackPlayers = false;
    public float retreatHealth = 0.0f;
    public final Map<EquipmentSlot, ItemStack> equipment = new LinkedHashMap<>();

    public UnitDefinition(String id, String name, Identifier entityId) { this.id = id; this.name = name; this.entityId = entityId; }
    public UnitDefinition(NbtCompound nbt) {
        id = nbt.getString("id"); name = nbt.getString("name"); description = nbt.getString("description");
        entityId = Identifier.tryParse(nbt.getString("entity")); factionId = nbt.getString("faction");
        maxHealth = nbt.getFloat("health"); movementSpeed = nbt.getFloat("speed"); attackDamage = nbt.getFloat("damage"); followRange = nbt.getFloat("range"); role = nbt.getString("role");
        attackHostile = nbt.getBoolean("attack_hostile"); protectAllies = nbt.getBoolean("protect_allies"); attackPlayers = nbt.getBoolean("attack_players"); retreatHealth = nbt.getFloat("retreat");
        for (EquipmentSlot slot : EquipmentSlot.values()) if (nbt.contains("item_" + slot.getName())) equipment.put(slot, ItemStack.fromNbtOrEmpty(Registries.ITEM.getReadOnlyWrapper(), nbt.getCompound("item_" + slot.getName())));
    }
    public NbtCompound toNbt() {
        NbtCompound n = new NbtCompound(); n.putString("id", id); n.putString("name", name); n.putString("description", description == null ? "" : description); n.putString("entity", entityId.toString()); n.putString("faction", factionId);
        n.putFloat("health", maxHealth); n.putFloat("speed", movementSpeed); n.putFloat("damage", attackDamage); n.putFloat("range", followRange); n.putString("role", role); n.putBoolean("attack_hostile", attackHostile); n.putBoolean("protect_allies", protectAllies); n.putBoolean("attack_players", attackPlayers); n.putFloat("retreat", retreatHealth);
        for (var e : equipment.entrySet()) n.put("item_" + e.getKey().getName(), e.getValue().encode(Registries.ITEM.getReadOnlyWrapper())); return n;
    }
    public void apply(LivingEntity entity) {
        set(EntityAttributes.MAX_HEALTH, maxHealth, entity); set(EntityAttributes.MOVEMENT_SPEED, movementSpeed, entity); set(EntityAttributes.ATTACK_DAMAGE, attackDamage, entity); set(EntityAttributes.FOLLOW_RANGE, followRange, entity);
        entity.setHealth(Math.min(maxHealth, entity.getMaxHealth()));
        for (var e : equipment.entrySet()) entity.equipStack(e.getKey(), e.getValue().copy());
    }
    private static void set(net.minecraft.registry.entry.RegistryEntry<EntityAttribute> attribute, double value, LivingEntity entity) { var instance = entity.getAttributeInstance(attribute); if (instance != null) instance.setBaseValue(Math.max(0.01, value)); }
}
