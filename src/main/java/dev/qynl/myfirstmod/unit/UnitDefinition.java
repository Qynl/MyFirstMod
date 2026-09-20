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
import java.util.ArrayList;
import net.minecraft.inventory.Inventory;
import net.minecraft.entity.InventoryOwner;

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
    public float armor = 0;
    public float armorToughness = 0;
    public float knockbackResistance = 0;
    public String role = "melee";
    public String rank = "soldier";
    public String squad = "";
    public boolean commander = false;
    public String targetPriority = "nearest";
    public boolean attackHostile = true;
    public boolean protectAllies = true;
    public boolean attackPlayers = false;
    public boolean healAllies = true;
    public float healRange = 12;
    public float retreatHealth = 0.0f;
    public final Map<EquipmentSlot, ItemStack> equipment = new LinkedHashMap<>();
    public final ArrayList<ItemStack> inventory = new ArrayList<>();

    public UnitDefinition(String id, String name, Identifier entityId) { this.id = id; this.name = name; this.entityId = entityId; }
    public UnitDefinition(NbtCompound nbt) {
        id = nbt.getString("id"); name = nbt.getString("name"); description = nbt.getString("description");
        entityId = Identifier.tryParse(nbt.getString("entity")); factionId = nbt.getString("faction");
        maxHealth = nbt.getFloat("health"); movementSpeed = nbt.getFloat("speed"); attackDamage = nbt.getFloat("damage"); followRange = nbt.getFloat("range"); armor = nbt.getFloat("armor"); armorToughness = nbt.getFloat("toughness"); knockbackResistance = nbt.getFloat("knockback"); role = nbt.getString("role"); rank = nbt.getString("rank"); squad = nbt.getString("squad"); commander = nbt.getBoolean("commander"); targetPriority = nbt.getString("priority");
        attackHostile = nbt.getBoolean("attack_hostile"); protectAllies = nbt.getBoolean("protect_allies"); attackPlayers = nbt.getBoolean("attack_players"); healAllies = nbt.getBoolean("heal_allies"); healRange = nbt.getFloat("heal_range"); retreatHealth = nbt.getFloat("retreat");
        for (EquipmentSlot slot : EquipmentSlot.values()) if (nbt.contains("item_" + slot.getName())) equipment.put(slot, ItemStack.fromNbtOrEmpty(Registries.ITEM.getReadOnlyWrapper(), nbt.getCompound("item_" + slot.getName())));
        var list=nbt.getList("inventory",10); for(int i=0;i<list.size();i++) inventory.add(ItemStack.fromNbtOrEmpty(Registries.ITEM.getReadOnlyWrapper(),list.getCompound(i)));
    }
    public NbtCompound toNbt() {
        NbtCompound n = new NbtCompound(); n.putString("id", id); n.putString("name", name); n.putString("description", description == null ? "" : description); n.putString("entity", entityId.toString()); n.putString("faction", factionId);
        n.putFloat("health", maxHealth); n.putFloat("speed", movementSpeed); n.putFloat("damage", attackDamage); n.putFloat("range", followRange); n.putFloat("armor", armor); n.putFloat("toughness", armorToughness); n.putFloat("knockback", knockbackResistance); n.putString("role", role); n.putString("rank", rank); n.putString("squad", squad); n.putBoolean("commander", commander); n.putString("priority", targetPriority); n.putBoolean("attack_hostile", attackHostile); n.putBoolean("protect_allies", protectAllies); n.putBoolean("attack_players", attackPlayers); n.putBoolean("heal_allies", healAllies); n.putFloat("heal_range", healRange); n.putFloat("retreat", retreatHealth);
        for (var e : equipment.entrySet()) n.put("item_" + e.getKey().getName(), e.getValue().encode(Registries.ITEM.getReadOnlyWrapper())); var list=new net.minecraft.nbt.NbtList(); for(var stack:inventory) list.add(stack.encode(Registries.ITEM.getReadOnlyWrapper())); n.put("inventory",list); return n;
    }
    public void apply(LivingEntity entity) {
        set(EntityAttributes.MAX_HEALTH, maxHealth, entity); set(EntityAttributes.MOVEMENT_SPEED, movementSpeed, entity); set(EntityAttributes.ATTACK_DAMAGE, attackDamage, entity); set(EntityAttributes.FOLLOW_RANGE, followRange, entity); set(EntityAttributes.ARMOR, armor, entity); set(EntityAttributes.ARMOR_TOUGHNESS, armorToughness, entity); set(EntityAttributes.KNOCKBACK_RESISTANCE, knockbackResistance, entity);
        entity.setHealth(Math.min(maxHealth, entity.getMaxHealth()));
        for (var e : equipment.entrySet()) entity.equipStack(e.getKey(), e.getValue().copy());
        if(entity instanceof InventoryOwner owner){ Inventory inv=owner.getInventory(); for(int i=0;i<inventory.size() && i<inv.size();i++) inv.setStack(i,inventory.get(i).copy()); }
    }
    private static void set(net.minecraft.registry.entry.RegistryEntry<EntityAttribute> attribute, double value, LivingEntity entity) { var instance = entity.getAttributeInstance(attribute); if (instance != null) instance.setBaseValue(Math.max(0.01, value)); }
}
