package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
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
            bombardier.getLookControl().lookAt(target, 40.0f, 40.0f);
            bombardier.swingHand(Hand.MAIN_HAND);

            MinecraftServer server = world.getServer();
            String myFaction = UnitSystem.getTagValue(bombardier, "faction:");

            // Calculate impact point
            double tx = target.getX();
            double ty = target.getY() + 0.5;
            double tz = target.getZ();

            // Mortar launch sound
            world.playSound(null, bombardier.getX(), bombardier.getY(), bombardier.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 0.8f, 1.8f);

            // Explosive canister impact
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, tx, ty + 0.5, tz, 2, 0.2, 0.2, 0.2, 0.0);
            world.spawnParticles(ParticleTypes.FLAME, tx, ty + 0.5, tz, 25, 1.0, 1.0, 1.0, 0.1);
            world.playSound(null, tx, ty, tz, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 1.5f, 1.0f);

            float damage = 10.0f;
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(4.5),
                    e -> e != bombardier && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            for (LivingEntity enemy : enemies) {
                boolean wasAlive = enemy.isAlive();
                enemy.damage(world.getDamageSources().explosion(bombardier, null), damage);
                enemy.takeKnockback(0.6, bombardier.getX() - enemy.getX(), bombardier.getZ() - enemy.getZ());

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
