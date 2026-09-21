package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class RangedAI {
    private RangedAI() {}

    public static void executeRanged(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || !target.isAlive()) return;

        double distSq = mob.squaredDistanceTo(target);
        double dist = Math.sqrt(distSq);

        // Check ammunition
        boolean infinite = unit.infiniteAmmo || mob.getCommandTags().contains("infinite_ammo");
        int ammo = UnitSystem.getNumericTag(mob, "ammo:");
        if (!infinite && ammo <= 0) {
            // Out of ammo: switch to melee or retreat
            CombatAI.executeMelee(world, mob, target, unit);
            return;
        }

        // Tactical positioning & kiting: keep between 8 and 24 blocks
        if (dist < 7.0) {
            // Too close: backpedal away while aiming
            double dx = mob.getX() - target.getX();
            double dz = mob.getZ() - target.getZ();
            mob.getNavigation().startMovingTo(mob.getX() + dx * 2.5, mob.getY(), mob.getZ() + dz * 2.5, 1.25);
        } else if (dist > 25.0) {
            // Too far: close distance
            mob.getNavigation().startMovingTo(target, 1.1);
        } else {
            // Optimal shooting distance: hold ground
            mob.getNavigation().stop();
        }

        mob.getLookControl().lookAt(target, 45.0f, 45.0f);

        // Aiming focus particles right before firing
        if (mob.age % 25 == 20 && dist <= 32.0) {
            world.spawnParticles(ParticleTypes.CRIT, mob.getX(), mob.getEyeY() - 0.1, mob.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
        }

        // Shoot projectile every 25 ticks
        if (mob.age % 25 == 0 && dist <= 32.0) {
            mob.swingHand(Hand.MAIN_HAND);

            ItemStack weapon = mob.getMainHandStack();
            boolean isCrossbow = weapon.isOf(Items.CROSSBOW);

            double dx = target.getX() - mob.getX();
            double dy = (target.getBodyY(0.5)) - (mob.getEyeY() - 0.1);
            double dz = target.getZ() - mob.getZ();

            // Arrow projectile with authentic velocity & spread
            ArrowEntity arrow = new ArrowEntity(world, mob, new ItemStack(Items.ARROW), weapon);
            arrow.setPosition(mob.getX(), mob.getEyeY() - 0.1, mob.getZ());
            arrow.setVelocity(dx, dy + dist * 0.045, dz, 1.9f, 1.2f);
            arrow.setDamage(Math.max(3.0, unit.attackDamage));
            arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;

            // Poison sniper or flame arrow trails
            if (mob.getCommandTags().contains("unit:bogged_sniper") || mob.getCommandTags().contains("unit:undead_sniper")) {
                world.spawnParticles(ParticleTypes.ITEM_SLIME, mob.getX(), mob.getEyeY(), mob.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
            }

            world.spawnEntity(arrow);

            if (isCrossbow) {
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.NEUTRAL, 1.1f, 0.95f + (world.random.nextFloat() * 0.1f));
            } else {
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.NEUTRAL, 1.1f, 0.9f + (world.random.nextFloat() * 0.2f));
            }

            if (!infinite) {
                UnitSystem.consumeTag(mob, "ammo:");
            }
        }
    }
}
