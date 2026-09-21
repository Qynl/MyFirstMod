package dev.qynl.myfirstmod.visual;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class CombatVisualEffects {
    private CombatVisualEffects() {}

    /**
     * Spawns an expanding ground shockwave ring with dual particle types.
     */
    public static void spawnExpandingShockwave(ServerWorld world, double x, double y, double z, double maxRadius, ParticleEffect innerParticle, ParticleEffect outerParticle) {
        if (world == null) return;
        for (double r = 1.0; r <= maxRadius; r += 1.2) {
            int points = (int) (r * 10);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double px = x + Math.cos(angle) * r;
                double pz = z + Math.sin(angle) * r;
                world.spawnParticles(innerParticle, px, y + 0.15, pz, 1, 0, 0, 0, 0);
                if (i % 2 == 0 && outerParticle != null) {
                    world.spawnParticles(outerParticle, px, y + 0.3, pz, 1, 0, 0.05, 0, 0.02);
                }
            }
        }
    }

    /**
     * Spawns a celestial vertical pillar of light with orbiting spiral rings.
     */
    public static void spawnOrbitalLightPillar(ServerWorld world, double x, double y, double z, double height, ParticleEffect coreParticle, ParticleEffect spiralParticle) {
        if (world == null) return;
        // Central core beam
        for (double dy = 0.0; dy <= height; dy += 0.35) {
            world.spawnParticles(coreParticle, x, y + dy, z, 3, 0.2, 0.1, 0.2, 0.02);

            // Orbiting double spiral
            if (spiralParticle != null) {
                double angle1 = dy * 1.5;
                double angle2 = angle1 + Math.PI;
                double r = 0.8 + 0.2 * Math.sin(dy * 0.8);

                world.spawnParticles(spiralParticle, x + Math.cos(angle1) * r, y + dy, z + Math.sin(angle1) * r, 1, 0, 0, 0, 0);
                world.spawnParticles(spiralParticle, x + Math.cos(angle2) * r, y + dy, z + Math.sin(angle2) * r, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Spawns a directional cone blast (for mortars, bellows, dragon breaths, sonic waves).
     */
    public static void spawnDirectionalCone(ServerWorld world, Vec3d origin, Vec3d direction, double length, double spreadAngle, ParticleEffect particle, int density) {
        if (world == null) return;
        Vec3d dirNorm = direction.normalize();

        for (int i = 0; i < density; i++) {
            double dist = (i / (double) density) * length;
            double currentSpread = Math.tan(Math.toRadians(spreadAngle)) * dist;

            double randOffset1 = (world.random.nextDouble() - 0.5) * 2.0 * currentSpread;
            double randOffset2 = (world.random.nextDouble() - 0.5) * 2.0 * currentSpread;

            Vec3d pos = origin.add(dirNorm.multiply(dist)).add(randOffset1, randOffset2, randOffset1);
            world.spawnParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0.05);
        }
    }

    /**
     * Spawns an arcane summoning circle on the ground with pentagram or star points.
     */
    public static void spawnArcaneCircle(ServerWorld world, double x, double y, double z, double radius, ParticleEffect ringParticle, ParticleEffect runeParticle) {
        if (world == null) return;
        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;
            world.spawnParticles(ringParticle, px, y + 0.1, pz, 1, 0, 0.02, 0, 0);
        }

        // Inner 5-point star nodes
        for (int s = 0; s < 5; s++) {
            double starAngle = (2 * Math.PI * s) / 5.0;
            double sx = x + Math.cos(starAngle) * (radius * 0.65);
            double sz = z + Math.sin(starAngle) * (radius * 0.65);
            world.spawnParticles(runeParticle, sx, y + 0.2, sz, 4, 0.1, 0.2, 0.1, 0.05);
        }
    }

    /**
     * Spawns a cinematic critical strike blood / soul splash effect.
     */
    public static void spawnCriticalSlash(ServerWorld world, double x, double y, double z, boolean isHoly) {
        if (world == null) return;
        if (isHoly) {
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 15, 0.35, 0.45, 0.35, 0.1);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.25, 0.25, 0.25, 0.08);
            world.spawnParticles(ParticleTypes.FLASH, x, y + 0.2, z, 1, 0, 0, 0, 0);
        } else {
            world.spawnParticles(ParticleTypes.CRIT, x, y, z, 16, 0.4, 0.5, 0.4, 0.15);
            world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 4, 0.2, 0.3, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
        }
    }

    /**
     * Plays a layered cinematic sound at a location.
     */
    public static void playLayeredSound(ServerWorld world, double x, double y, double z, SoundEvent mainSound, float mainPitch, SoundEvent subSound, float subPitch, float volume) {
        if (world == null) return;
        if (mainSound != null) {
            world.playSound(null, x, y, z, mainSound, SoundCategory.PLAYERS, volume, mainPitch);
        }
        if (subSound != null) {
            world.playSound(null, x, y, z, subSound, SoundCategory.PLAYERS, volume * 0.8f, subPitch);
        }
    }
}
