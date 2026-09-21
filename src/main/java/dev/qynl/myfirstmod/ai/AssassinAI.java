package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
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
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public final class AssassinAI {
    private AssassinAI() {}

    public static void executeAssassin(ServerWorld world, MobEntity assassin, UnitDefinition unit) {
        if (assassin == null || !assassin.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(assassin, "faction:");
        if (myFaction == null) return;

        // Prioritize High-Value Targets: Commanders, Medics, Pyros
        LivingEntity target = assassin.getTarget();
        if (target == null || !target.isAlive() || assassin.age % 40 == 0) {
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, assassin.getBoundingBox().expand(unit.followRange),
                    e -> e != assassin && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            target = enemies.stream().min(Comparator.comparingDouble(e -> {
                double score = assassin.squaredDistanceTo(e);
                if (e.getCommandTags().contains("commander")) score -= 8000;
                String role = UnitSystem.getTagValue(e, "role:");
                if ("medic".equalsIgnoreCase(role) || "healer".equalsIgnoreCase(role)) score -= 6000;
                if ("pyrotechnic".equalsIgnoreCase(role) || "artillery".equalsIgnoreCase(role)) score -= 4000;
                return score;
            })).orElse(null);

            if (target != null) {
                assassin.setTarget(target);
            }
        }

        if (target == null) return;

        double distSq = assassin.squaredDistanceTo(target);

        // Shadow Step Blink: if 6 to 14 blocks away, blink behind target!
        if (distSq >= 36.0 && distSq <= 196.0 && assassin.age % 80 == 0) {
            Vec3d behindTarget = target.getPos().add(target.getRotationVector().multiply(-1.8));
            world.spawnParticles(ParticleTypes.PORTAL, assassin.getX(), assassin.getY() + 0.8, assassin.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
            assassin.requestTeleport(behindTarget.x, behindTarget.y, behindTarget.z);
            world.spawnParticles(ParticleTypes.SMOKE, behindTarget.x, behindTarget.y + 0.5, behindTarget.z, 25, 0.3, 0.5, 0.3, 0.05);
            world.playSound(null, behindTarget.x, behindTarget.y, behindTarget.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 1.4f);
            assassin.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 2, false, false));
        }

        // Stealth sprint when approaching
        if (distSq > 16.0) {
            assassin.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, false, false));
            assassin.getNavigation().startMovingTo(target, 1.35);
        } else {
            // Close range strike
            assassin.getLookControl().lookAt(target, 45.0f, 45.0f);

            if (assassin.age % 12 == 0) {
                assassin.swingHand(Hand.MAIN_HAND);

                float damage = unit.attackDamage * 1.5f;

                // Check for backstab angle
                double dot = assassin.getRotationVector().dotProduct(target.getRotationVector());
                boolean isBackstab = dot > 0.3;
                if (isBackstab) {
                    damage *= 1.85f; // Devastating backstab multiplier
                    world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
                    world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.6), target.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.2f, 0.8f);
                }

                boolean targetWasAlive = target.isAlive();
                target.damage(world.getDamageSources().mobAttack(assassin), damage);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 0.9f, 1.2f);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);

                if (server != null) {
                    BattleStats stats = BattleStats.get(server);
                    stats.recordDamage(myFaction, damage);
                    if (targetWasAlive && !target.isAlive()) {
                        stats.recordKill(myFaction);
                        String vFaction = UnitSystem.getTagValue(target, "faction:");
                        if (vFaction != null) stats.recordDeath(vFaction);
                        VeteranProgression.recordKillForUnit(world, assassin);
                    }
                }
            }
        }
    }
}
