package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class BardAI {
    private BardAI() {}

    public static void executeBard(ServerWorld world, MobEntity bard, UnitDefinition unit) {
        if (bard == null || !bard.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(bard, "faction:");
        if (myFaction == null) return;

        // Perform war song melody every 40 ticks (2 seconds)
        if (bard.age % 40 == 0) {
            List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, bard.getBoundingBox().expand(14.0),
                    e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            if (!allies.isEmpty()) {
                bard.swingHand(Hand.MAIN_HAND);
                // Musical note particles
                world.spawnParticles(ParticleTypes.NOTE, bard.getX(), bard.getY() + 1.2, bard.getZ(), 8, 0.4, 0.4, 0.4, 0.5);

                // Play pleasant musical chord
                float pitch = 0.8f + (world.random.nextFloat() * 0.8f);
                world.playSound(null, bard.getX(), bard.getY(), bard.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.NEUTRAL, 1.2f, pitch);

                for (LivingEntity ally : allies) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0, false, false));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0, false, false));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0, false, false));
                }
            }
        }
    }
}
