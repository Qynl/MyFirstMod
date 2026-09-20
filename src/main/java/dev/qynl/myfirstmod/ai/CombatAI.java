package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public final class CombatAI {
    private CombatAI() {}

    public static void executeMelee(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || !target.isAlive()) return;

        double distSq = mob.squaredDistanceTo(target);
        double reach = Math.max(3.0, (mob.getWidth() + target.getWidth()) * 1.5);
        double reachSq = reach * reach;

        // Pathfind towards target
        if (distSq > reachSq) {
            mob.getNavigation().startMovingTo(target, 1.15);
            return;
        }

        // Within melee reach: look at target and attack
        mob.getLookControl().lookAt(target, 30.0f, 30.0f);

        // Attack cooldown check
        int attackRate = Math.max(8, (int)(20.0f / Math.max(1.0f, unit.attackSpeed)));
        if (mob.age % attackRate == 0) {
            mob.swingHand(Hand.MAIN_HAND);

            float damage = unit.attackDamage;
            var dmgAttr = mob.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
            if (dmgAttr != null) {
                damage = (float) dmgAttr.getValue();
            }

            // Check for critical strike if unit is airborne
            boolean isCrit = mob.fallDistance > 0.0f && !mob.isOnGround() && !mob.isClimbing() && !mob.isTouchingWater();
            if (isCrit) {
                damage *= 1.5f;
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
            }

            boolean targetWasAlive = target.isAlive();
            target.damage(world.getDamageSources().mobAttack(mob), damage);

            // Hit sound and particles
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 0.8f, 1.0f);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);

            // Record battle damage & kills
            MinecraftServer server = world.getServer();
            if (server != null) {
                String attackerFaction = UnitSystem.getTagValue(mob, "faction:");
                String victimFaction = UnitSystem.getTagValue(target, "faction:");
                BattleStats stats = BattleStats.get(server);

                if (attackerFaction != null) {
                    stats.recordDamage(attackerFaction, damage);
                }

                if (targetWasAlive && !target.isAlive()) {
                    if (attackerFaction != null) stats.recordKill(attackerFaction);
                    if (victimFaction != null) stats.recordDeath(victimFaction);
                    VeteranProgression.recordKillForUnit(world, mob);
                }
            }
        }
    }
}
