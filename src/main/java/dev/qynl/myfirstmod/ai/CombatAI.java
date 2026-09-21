package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class CombatAI {
    private CombatAI() {}

    public static void executeMelee(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || !target.isAlive()) return;

        double distSq = mob.squaredDistanceTo(target);
        double reach = Math.max(3.2, (mob.getWidth() + target.getWidth()) * 1.6);
        double reachSq = reach * reach;

        // Pathfind towards target
        if (distSq > reachSq) {
            // Galloping dust clouds when mounted cavalry is pursuing
            if (mob.hasVehicle() && mob.age % 4 == 0) {
                world.spawnParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.1, mob.getZ(), 2, 0.2, 0.05, 0.2, 0.02);
            }

            // Jump Attack / Lunge when closing the final 4-6 blocks
            if (distSq <= 36.0 && distSq > reachSq && mob.isOnGround() && mob.age % 30 == 0) {
                Vec3d leapDir = target.getPos().subtract(mob.getPos()).normalize();
                mob.addVelocity(leapDir.x * 0.35, 0.25, leapDir.z * 0.35);
                mob.velocityModified = true;
                world.spawnParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY(), mob.getZ(), 4, 0.2, 0.1, 0.2, 0.05);
                world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.NEUTRAL, 0.8f, 1.4f);
            }

            mob.getNavigation().startMovingTo(target, 1.25);
            return;
        }

        // Within melee reach: turn directly towards target
        mob.getLookControl().lookAt(target, 45.0f, 45.0f);

        // Attack cooldown check based on attackSpeed attribute
        int attackRate = Math.max(6, (int)(20.0f / Math.max(1.0f, unit.attackSpeed)));
        if (mob.age % attackRate == 0) {
            // Alternate mainhand and offhand swings if dual-wielding
            boolean hasOffhandWeapon = !mob.getOffHandStack().isEmpty() && !(mob.getOffHandStack().getItem() instanceof ShieldItem);
            Hand swingHand = (hasOffhandWeapon && (mob.age / attackRate) % 2 == 1) ? Hand.OFF_HAND : Hand.MAIN_HAND;
            mob.swingHand(swingHand);

            float damage = unit.attackDamage;
            var dmgAttr = mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (dmgAttr != null) {
                damage = (float) dmgAttr.getValue();
            }

            // Check for critical strike if unit is falling/airborne
            boolean isCrit = mob.fallDistance > 0.0f && !mob.isOnGround() && !mob.isClimbing() && !mob.isTouchingWater();
            if (isCrit) {
                damage *= 1.5f;
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 15, 0.3, 0.4, 0.3, 0.15);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            }

            // Mounted Cavalry Charge bonus
            if (mob.hasVehicle()) {
                damage *= 1.35f;
                world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_HORSE_GALLOP, SoundCategory.NEUTRAL, 1.2f, 1.1f);
                target.takeKnockback(1.4, mob.getX() - target.getX(), mob.getZ() - target.getZ());
            }

            // Sweeping weapon slash visual arc
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);

            // Fire aspect ignite check & fiery swing trail
            int fireLevel = UnitSystem.getNumericTag(mob, "fire_aspect:");
            if (fireLevel > 0) {
                target.setOnFireFor(fireLevel * 4);
                world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.4, 0.3, 0.08);
                world.spawnParticles(ParticleTypes.LAVA, target.getX(), target.getBodyY(0.5), target.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
                world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.NEUTRAL, 0.8f, 1.2f);
            }

            boolean targetWasAlive = target.isAlive();
            target.damage(world.getDamageSources().mobAttack(mob), damage);
            dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, target.getX(), target.getBodyY(0.75), target.getZ(), damage, isCrit);

            // Audio feedback
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.NEUTRAL, 0.9f, 0.95f + (world.random.nextFloat() * 0.15f));

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

                    String deathAction = UnitSystem.getTagValue(target, "death:");
                    DeathActionHandler.handleDeath(world, target, deathAction, victimFaction);
                }
            }
        }
    }
}
