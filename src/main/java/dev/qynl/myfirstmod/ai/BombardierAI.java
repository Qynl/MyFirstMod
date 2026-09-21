package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class BombardierAI {
    private BombardierAI() {}

    public static void executeBombardier(ServerWorld world, MobEntity bombardier, LivingEntity target, UnitDefinition unit) {
        if (bombardier == null || target == null || !target.isAlive()) return;

        double distSq = bombardier.squaredDistanceTo(target);
        if (distSq < 36.0 || distSq > 900.0) return; // 6 to 30 blocks range

        // Mortar canister salvo every 50 ticks
        if (bombardier.age % 50 == 0) {
            bombardier.getLookControl().lookAt(target, 45.0f, 45.0f);
            bombardier.swingHand(Hand.MAIN_HAND);

            MinecraftServer server = world.getServer();
            String myFaction = UnitSystem.getTagValue(bombardier, "faction:");

            // Calculate impact point
            double tx = target.getX();
            double ty = target.getY() + 0.5;
            double tz = target.getZ();

            // Muzzle flash particles at shooter
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bombardier.getX(), bombardier.getEyeY(), bombardier.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
            world.spawnParticles(ParticleTypes.FLAME, bombardier.getX(), bombardier.getEyeY(), bombardier.getZ(), 8, 0.2, 0.2, 0.2, 0.08);

            // Mortar launch sound
            world.playSound(null, bombardier.getX(), bombardier.getY(), bombardier.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1.0f, 1.8f);

            // Ballistic arc tracer particles
            for (int i = 1; i < 6; i++) {
                double progress = i / 6.0;
                double px = bombardier.getX() + (tx - bombardier.getX()) * progress;
                double pz = bombardier.getZ() + (tz - bombardier.getZ()) * progress;
                double py = bombardier.getEyeY() + (ty - bombardier.getEyeY()) * progress + Math.sin(progress * Math.PI) * 3.5;
                world.spawnParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0, 0, 0);
            }

            // Explosive canister impact & shockwave
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, tx, ty + 0.5, tz, 3, 0.3, 0.3, 0.3, 0.0);
            world.spawnParticles(ParticleTypes.FLAME, tx, ty + 0.5, tz, 40, 1.5, 1.2, 1.5, 0.15);
            world.spawnParticles(ParticleTypes.LAVA, tx, ty + 0.5, tz, 8, 0.5, 0.5, 0.5, 0.08);
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, tx, ty + 0.8, tz, 12, 0.4, 0.6, 0.4, 0.05);
            world.playSound(null, tx, ty, tz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1.8f, 0.85f);

            float damage = 12.0f;
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(4.5),
                    e -> e != bombardier && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            for (LivingEntity enemy : enemies) {
                boolean wasAlive = enemy.isAlive();
                enemy.damage(world.getDamageSources().explosion(bombardier, null), damage);
                enemy.takeKnockback(0.8, bombardier.getX() - enemy.getX(), bombardier.getZ() - enemy.getZ());
                enemy.addVelocity(0.0, 0.35, 0.0);

                if (server != null && myFaction != null) {
                    BattleStats.get(server).recordDamage(myFaction, damage);
                    if (wasAlive && !enemy.isAlive()) {
                        BattleStats.get(server).recordKill(myFaction);
                        String vFaction = UnitSystem.getTagValue(enemy, "faction:");
                        if (vFaction != null) BattleStats.get(server).recordDeath(vFaction);
                        VeteranProgression.recordKillForUnit(world, bombardier);
                    }
                }
            }
        }
    }
}
