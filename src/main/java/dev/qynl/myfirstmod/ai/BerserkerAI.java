package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class BerserkerAI {
    private BerserkerAI() {}

    public static void executeBerserker(ServerWorld world, MobEntity berserker, UnitDefinition unit) {
        if (berserker == null || !berserker.isAlive()) return;

        // Bloodrage activates when health drops below 50%
        if (berserker.getHealth() < berserker.getMaxHealth() * 0.5f) {
            if (!berserker.hasStatusEffect(StatusEffects.STRENGTH)) {
                berserker.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                // Enter Bloodrage state
                berserker.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1, false, false));
                berserker.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, false));
                berserker.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 200, 1, false, false));

                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, berserker.getX(), berserker.getY() + 1.2, berserker.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
                world.spawnParticles(ParticleTypes.FLAME, berserker.getX(), berserker.getBodyY(0.5), berserker.getZ(), 15, 0.3, 0.5, 0.3, 0.08);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, berserker.getX(), berserker.getBodyY(0.5), berserker.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
                world.playSound(null, berserker.getX(), berserker.getY(), berserker.getZ(), SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, SoundCategory.HOSTILE, 1.3f, 0.8f);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, berserker.getX(), berserker.getY() + 1.2, berserker.getZ(), "🔥 BLOODRAGE!", net.minecraft.util.Formatting.RED);
            } else if (berserker.age % 15 == 0) {
                // Bloodrage aura particles while enraged
                world.spawnParticles(ParticleTypes.FLAME, berserker.getX(), berserker.getBodyY(0.5), berserker.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
            }
        }
    }
}
