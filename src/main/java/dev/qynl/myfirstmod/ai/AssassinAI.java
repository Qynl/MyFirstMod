package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitWorldData;
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

        // Assassin stealth sprint when approaching
        if (distSq > 16.0) {
            assassin.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, false, false));
            assassin.getNavigation().startMovingTo(target, 1.35);
        } else {
            // Close range strike
            assassin.getLookControl().lookAt(target, 40.0f, 40.0f);

            if (assassin.age % 12 == 0) {
                assassin.swingHand(Hand.MAIN_HAND);

                float damage = unit.attackDamage * 1.5f; // Bonus assassin damage

                // Check for backstab angle
                double dot = assassin.getRotationVector().dotProduct(target.getRotationVector());
                boolean isBackstab = dot > 0.4;
                if (isBackstab) {
                    damage *= 1.75f; // Huge critical backstab multiplier
                    world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.15);
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT.value(), SoundCategory.NEUTRAL, 1.0f, 0.8f);
                }

                boolean targetWasAlive = target.isAlive();
                target.damage(world.getDamageSources().mobAttack(assassin), damage);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG.value(), SoundCategory.NEUTRAL, 0.9f, 1.2f);

                if (server != null) {
                    BattleStats stats = BattleStats.get(server);
                    stats.recordDamage(myFaction, damage);
                    if (targetWasAlive && !target.isAlive()) {
                        stats.recordKill(myFaction);
                        String vFaction = UnitSystem.getTagValue(target, "faction:");
                        if (vFaction != null) stats.recordDeath(vFaction);
                    }
                }
            }
        }
    }
}
