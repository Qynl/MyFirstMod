package dev.qynl.myfirstmod.equipment;

import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;

public final class ItemCapabilities {
    public enum Capability {
        MELEE_WEAPON,
        SHIELD,
        BOW,
        CROSSBOW,
        TRIDENT,
        ARROW,
        FIREWORK,
        FOOD,
        HEALING_POTION,
        OFFENSIVE_POTION,
        BUFF_POTION,
        BUILDING_BLOCK,
        TORCH,
        LADDER,
        TOOL,
        OTHER
    }

    private ItemCapabilities() {}

    public static Capability classify(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Capability.OTHER;
        Item item = stack.getItem();

        if (item instanceof ShieldItem || stack.isOf(Items.SHIELD)) {
            return Capability.SHIELD;
        }

        if (item instanceof CrossbowItem || stack.isOf(Items.CROSSBOW)) {
            return Capability.CROSSBOW;
        }

        if (item instanceof BowItem || stack.isOf(Items.BOW)) {
            return Capability.BOW;
        }

        if (item instanceof TridentItem || stack.isOf(Items.TRIDENT)) {
            return Capability.TRIDENT;
        }

        if (stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW)) {
            return Capability.ARROW;
        }

        if (stack.isOf(Items.FIREWORK_ROCKET) || stack.isOf(Items.FIREWORK_STAR)) {
            return Capability.FIREWORK;
        }

        if (stack.isOf(Items.TORCH) || stack.isOf(Items.SOUL_TORCH) || stack.isOf(Items.LANTERN) || stack.isOf(Items.SOUL_LANTERN)) {
            return Capability.TORCH;
        }

        if (stack.isOf(Items.LADDER) || stack.isOf(Items.SCAFFOLDING)) {
            return Capability.LADDER;
        }

        if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION) || stack.isOf(Items.POTION)) {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
                for (StatusEffectInstance effect : contents.getEffects()) {
                    var type = effect.getEffectType();
                    if (type.equals(StatusEffects.INSTANT_DAMAGE) || type.equals(StatusEffects.POISON) ||
                        type.equals(StatusEffects.SLOWNESS) || type.equals(StatusEffects.WEAKNESS) ||
                        type.equals(StatusEffects.WITHER) || type.equals(StatusEffects.BLINDNESS)) {
                        return Capability.OFFENSIVE_POTION;
                    }
                    if (type.equals(StatusEffects.INSTANT_HEALTH) || type.equals(StatusEffects.REGENERATION)) {
                        return Capability.HEALING_POTION;
                    }
                    if (type.equals(StatusEffects.STRENGTH) || type.equals(StatusEffects.SPEED) ||
                        type.equals(StatusEffects.RESISTANCE) || type.equals(StatusEffects.FIRE_RESISTANCE)) {
                        return Capability.BUFF_POTION;
                    }
                }
            }
            return Capability.HEALING_POTION;
        }

        if (stack.contains(DataComponentTypes.FOOD) || stack.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
            return Capability.FOOD;
        }

        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem ||
            stack.isOf(ModItems.PALADIN_MACE) || stack.isOf(ModItems.ASSASSIN_DAGGER)) {
            return Capability.MELEE_WEAPON;
        }

        if (item instanceof MiningToolItem || item instanceof ShearsItem) {
            return Capability.TOOL;
        }

        if (item instanceof BlockItem) {
            return Capability.BUILDING_BLOCK;
        }

        return Capability.OTHER;
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return classify(stack) == Capability.MELEE_WEAPON;
    }

    public static boolean isShield(ItemStack stack) {
        return classify(stack) == Capability.SHIELD;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        Capability c = classify(stack);
        return c == Capability.BOW || c == Capability.CROSSBOW || c == Capability.TRIDENT;
    }

    public static boolean isFood(ItemStack stack) {
        return classify(stack) == Capability.FOOD;
    }

    public static boolean isFirework(ItemStack stack) {
        return classify(stack) == Capability.FIREWORK;
    }

    public static boolean isSplashPotion(ItemStack stack) {
        return stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION);
    }
}
