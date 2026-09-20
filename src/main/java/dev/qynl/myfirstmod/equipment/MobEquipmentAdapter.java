package dev.qynl.myfirstmod.equipment;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class MobEquipmentAdapter {
    private MobEquipmentAdapter() {}

    public static boolean isHumanoidOrCanWieldVisually(LivingEntity entity) {
        if (entity == null) return false;
        return entity instanceof AbstractSkeletonEntity ||
               entity instanceof ZombieEntity ||
               entity instanceof AbstractPiglinEntity ||
               entity instanceof IllagerEntity ||
               entity instanceof WitchEntity ||
               entity instanceof PlayerEntity ||
               entity instanceof VillagerEntity ||
               entity instanceof EndermanEntity ||
               entity instanceof WardenEntity ||
               entity instanceof AllayEntity ||
               entity instanceof FoxEntity;
    }

    public static void applyEquipment(LivingEntity entity, UnitDefinition unit) {
        if (entity == null || unit == null) return;

        boolean isHumanoid = isHumanoidOrCanWieldVisually(entity);

        float totalArmorBonus = 0.0f;
        float totalToughnessBonus = 0.0f;
        float totalKnockbackBonus = 0.0f;
        float totalDamageBonus = 0.0f;
        boolean hasHelmet = false;
        boolean hasFireAspect = false;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = unit.equipment.getOrDefault(slot, ItemStack.EMPTY);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();

            if (isHumanoid) {
                // Humanoid / bipedal mobs can equip and render items cleanly
                entity.equipStack(slot, stack.copy());
            } else {
                // Animal / Quadruped / Non-humanoid / Modded beasts:
                // Intelligently adapt armor and weapon stats without glitchy 3D model clipping
                if (slot == EquipmentSlot.HEAD) {
                    hasHelmet = true;
                }

                // Extract Armor & Toughness from armor items
                if (item instanceof ArmorItem armor) {
                    totalArmorBonus += armor.getProtection();
                    totalToughnessBonus += armor.getToughness();
                }

                // Check Wolf Armor & Horse Armor for native body slot
                if (entity instanceof WolfEntity wolf && item instanceof AnimalArmorItem) {
                    wolf.equipStack(EquipmentSlot.BODY, stack.copy());
                } else if (entity instanceof HorseEntity horse && item instanceof AnimalArmorItem) {
                    horse.equipStack(EquipmentSlot.BODY, stack.copy());
                }

                // Extract weapon damage & knockback from weapon items
                if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                    if (item instanceof SwordItem sword) {
                        totalDamageBonus += sword.getMaterial().getAttackDamage() + 3.0f;
                    } else if (item instanceof AxeItem axe) {
                        totalDamageBonus += axe.getMaterial().getAttackDamage() + 5.0f;
                    } else if (item instanceof MaceItem) {
                        totalDamageBonus += 6.0f;
                    } else if (item instanceof TridentItem) {
                        totalDamageBonus += 8.0f;
                    }

                    // Check for Fire Aspect enchantment
                    var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
                    if (enchantments != null && !enchantments.isEmpty()) {
                        String enchStr = enchantments.toString().toLowerCase();
                        if (enchStr.contains("fire_aspect") || enchStr.contains("flame")) {
                            hasFireAspect = true;
                        }
                        if (enchStr.contains("sharpness")) {
                            totalDamageBonus += 3.0f;
                        }
                        if (enchStr.contains("knockback")) {
                            totalKnockbackBonus += 0.5f;
                        }
                        if (enchStr.contains("protection")) {
                            totalArmorBonus += 3.0f;
                        }
                    }

                    // Natural wielders like Foxes and Allays can still hold their weapon visibly
                    if (entity instanceof FoxEntity || entity instanceof AllayEntity) {
                        entity.equipStack(slot, stack.copy());
                    }
                }
            }
        }

        // Apply adapted bonuses directly to beast attributes
        if (!isHumanoid) {
            if (totalArmorBonus > 0) {
                addAttributeBonus(entity, EntityAttributes.GENERIC_ARMOR, "beast_armor_bonus", totalArmorBonus);
            }
            if (totalToughnessBonus > 0) {
                addAttributeBonus(entity, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, "beast_toughness_bonus", totalToughnessBonus);
            }
            if (totalKnockbackBonus > 0) {
                addAttributeBonus(entity, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, "beast_knockback_bonus", totalKnockbackBonus);
            }
            if (totalDamageBonus > 0) {
                addAttributeBonus(entity, EntityAttributes.GENERIC_ATTACK_DAMAGE, "beast_damage_bonus", totalDamageBonus);
                entity.getCommandTags().add("weapon_empowered");
            }
            if (hasFireAspect) {
                entity.getCommandTags().add("fire_aspect:2");
            }
            if (hasHelmet || entity.getType().isIn(EntityTypeTags.UNDEAD)) {
                entity.getCommandTags().add("sun_immune");
            }
            if (totalArmorBonus > 0 || totalToughnessBonus > 0) {
                entity.getCommandTags().add("armored_beast");
            }
        }
    }

    private static void addAttributeBonus(LivingEntity entity, RegistryEntry<EntityAttribute> attribute, String modifierName, double amount) {
        if (entity == null || attribute == null) return;
        EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
        if (instance != null) {
            instance.setBaseValue(instance.getBaseValue() + amount);
        }
    }
}
