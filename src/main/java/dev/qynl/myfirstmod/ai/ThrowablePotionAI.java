package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class ThrowablePotionAI {
    private ThrowablePotionAI() {}

    public static void executePotionThrowing(ServerWorld world, MobEntity thrower, LivingEntity target, UnitDefinition unit) {
        if (thrower == null || target == null || !target.isAlive()) return;
        if (!unit.canThrowPotions) return;

        double distSq = thrower.squaredDistanceTo(target);
        if (distSq > 256.0) return; // 16 blocks max range

        // Throw offensive potion every 40 ticks (2 seconds)
        if (thrower.age % 40 == 0) {
            thrower.getLookControl().lookAt(target, 40.0f, 40.0f);
            thrower.swingHand(Hand.MAIN_HAND);

            ItemStack potionStack = getOffensivePotionStack(thrower);
            if (potionStack.isEmpty()) {
                potionStack = PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.HARMING);
            }

            throwPotion(world, thrower, target, potionStack);

            if (!unit.infiniteAmmo) {
                consumePotionStack(thrower);
            }
        }
    }

    public static void throwSplashHealing(ServerWorld world, MobEntity thrower, LivingEntity allyTarget) {
        if (thrower == null || allyTarget == null || !allyTarget.isAlive()) return;

        thrower.getLookControl().lookAt(allyTarget, 40.0f, 40.0f);
        thrower.swingHand(Hand.MAIN_HAND);

        ItemStack healingPotion = PotionContentsComponent.createStack(Items.SPLASH_POTION, Potions.HEALING);
        throwPotion(world, thrower, allyTarget, healingPotion);

        consumePotionStack(thrower);
    }

    public static void throwPotion(ServerWorld world, MobEntity thrower, LivingEntity target, ItemStack potionStack) {
        double distSq = thrower.squaredDistanceTo(target);
        double dist = Math.sqrt(distSq);

        double dx = target.getX() - thrower.getX();
        double dy = target.getBodyY(0.4) - (thrower.getEyeY() - 0.1);
        double dz = target.getZ() - thrower.getZ();

        PotionEntity potion = new PotionEntity(world, thrower);
        potion.setItem(potionStack);
        potion.setPosition(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
        potion.setVelocity(dx, dy + dist * 0.12, dz, 0.9f, 2.0f);

        world.spawnEntity(potion);
        world.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.NEUTRAL, 0.8f, 1.0f);
    }

    private static ItemStack getOffensivePotionStack(MobEntity mob) {
        if (mob.getMainHandStack().isOf(Items.SPLASH_POTION)) return mob.getMainHandStack();
        if (mob.getOffHandStack().isOf(Items.SPLASH_POTION)) return mob.getOffHandStack();
        if (mob instanceof net.minecraft.entity.InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumePotionStack(MobEntity mob) {
        if (mob.getMainHandStack().isOf(Items.SPLASH_POTION)) {
            ItemStack stack = mob.getMainHandStack();
            stack.decrement(1);
            mob.equipStack(EquipmentSlot.MAINHAND, stack);
            return;
        }
        if (mob.getOffHandStack().isOf(Items.SPLASH_POTION)) {
            ItemStack stack = mob.getOffHandStack();
            stack.decrement(1);
            mob.equipStack(EquipmentSlot.OFFHAND, stack);
            return;
        }
        if (mob instanceof net.minecraft.entity.InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
                    stack.decrement(1);
                    owner.getInventory().setStack(i, stack);
                    return;
                }
            }
        }
    }
}
