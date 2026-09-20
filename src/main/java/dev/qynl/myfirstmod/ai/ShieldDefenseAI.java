package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class ShieldDefenseAI {
    private ShieldDefenseAI() {}

    public static void executeShield(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || !unit.canBlockShield) return;

        boolean hasOffhandShield = mob.getEquippedStack(EquipmentSlot.OFFHAND).isOf(Items.SHIELD);
        boolean hasMainhandShield = mob.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.SHIELD);

        if (!hasOffhandShield && !hasMainhandShield) return;

        double distSq = mob.squaredDistanceTo(target);

        // When enemy is close (< 5 blocks) and facing, raise shield
        if (distSq < 25.0 && target.isAlive()) {
            Hand hand = hasOffhandShield ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mob.setCurrentHand(hand);

            if (mob.age % 30 == 0) {
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.NEUTRAL, 0.7f, 1.1f);
                world.spawnParticles(ParticleTypes.CRIT, mob.getX(), mob.getBodyY(0.5), mob.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
            }
        } else {
            if (mob.isUsingItem()) {
                mob.clearActiveItem();
            }
        }
    }
}
