package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.faction.FactionPerk;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class FireworksArtilleryAI {
    private FireworksArtilleryAI() {}

    public static void executeArtillery(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || target == null || !target.isAlive()) return;
        if (!unit.canShootFireworks) return;

        double distSq = mob.squaredDistanceTo(target);
        if (distSq > 1024.0) return; // 32 blocks max range

        double dist = Math.sqrt(distSq);

        // Physical rocket inventory check
        boolean infinite = unit.infiniteAmmo || mob.getCommandTags().contains("infinite_ammo");
        ItemStack physicalRocket = findPhysicalRocket(mob);

        if (!infinite && physicalRocket.isEmpty()) {
            return;
        }

        // Keep artillery distance (10 - 25 blocks)
        if (dist < 8.0) {
            double dx = mob.getX() - target.getX();
            double dz = mob.getZ() - target.getZ();
            mob.getNavigation().startMovingTo(mob.getX() + dx * 2, mob.getY(), mob.getZ() + dz * 2, 1.2);
        } else if (dist > 26.0) {
            mob.getNavigation().startMovingTo(target, 1.0);
        } else {
            mob.getNavigation().stop();
        }

        mob.getLookControl().lookAt(target, 40.0f, 40.0f);

        // Fire rocket salvo every 35 ticks
        if (mob.age % 35 == 0) {
            mob.swingHand(Hand.MAIN_HAND);

            double dx = target.getX() - mob.getX();
            double dy = target.getBodyY(0.5) - (mob.getEyeY() - 0.1);
            double dz = target.getZ() - mob.getZ();

            ItemStack rocketStack = physicalRocket.isEmpty() ? new ItemStack(Items.FIREWORK_ROCKET) : physicalRocket.copy();
            rocketStack.setCount(1);

            FireworkRocketEntity rocket = new FireworkRocketEntity(world, mob.getX(), mob.getEyeY() - 0.1, mob.getZ(), rocketStack);
            rocket.setVelocity(dx, dy + dist * 0.08, dz, 1.6f, 1.2f);

            world.spawnEntity(rocket);
            world.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.NEUTRAL, 1.0f, 1.0f);

            // Trigger bombardment impact effect at target area
            detonateFireworksBombardment(world, mob, target, unit);

            if (!infinite && !physicalRocket.isEmpty()) {
                physicalRocket.decrement(1);
            }
        }
    }

    private static void detonateFireworksBombardment(ServerWorld world, MobEntity shooter, LivingEntity target, UnitDefinition unit) {
        double tx = target.getX();
        double ty = target.getY() + 0.5;
        double tz = target.getZ();

        MinecraftServer server = world.getServer();
        String shooterFaction = UnitSystem.getTagValue(shooter, "faction:");

        float damage = 8.0f;
        double radius = 4.0;
        if (server != null && FactionManager.hasPerk(server, shooterFaction, FactionPerk.PYROTECHNICS)) {
            damage = 13.0f;
            radius = 5.5;
        }

        // Particle fireworks burst
        world.spawnParticles(ParticleTypes.FIREWORK, tx, ty + 1.0, tz, 40, 1.2, 1.2, 1.2, 0.15);
        world.spawnParticles(ParticleTypes.EXPLOSION, tx, ty + 0.5, tz, 3, 0.4, 0.4, 0.4, 0.0);
        world.playSound(null, tx, ty, tz, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.NEUTRAL, 1.8f, 1.0f);
        world.playSound(null, tx, ty, tz, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.NEUTRAL, 1.4f, 1.2f);

        // Apply explosive area damage to enemies
        List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(radius),
                e -> e != shooter && e.isAlive() && FactionManager.isHostile(server, shooterFaction, UnitSystem.getTagValue(e, "faction:")));

        for (LivingEntity enemy : enemies) {
            boolean wasAlive = enemy.isAlive();
            enemy.damage(world.getDamageSources().explosion(shooter, null), damage);
            dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, enemy.getX(), enemy.getBodyY(0.75), enemy.getZ(), damage, true);
            enemy.takeKnockback(0.4, shooter.getX() - enemy.getX(), shooter.getZ() - enemy.getZ());

            if (server != null) {
                BattleStats stats = BattleStats.get(server);
                if (shooterFaction != null) stats.recordDamage(shooterFaction, damage);
                if (wasAlive && !enemy.isAlive()) {
                    if (shooterFaction != null) stats.recordKill(shooterFaction);
                    String vFaction = UnitSystem.getTagValue(enemy, "faction:");
                    if (vFaction != null) stats.recordDeath(vFaction);
                    VeteranProgression.recordKillForUnit(world, shooter);
                }
            }
        }
    }

    public static ItemStack findPhysicalRocket(MobEntity mob) {
        if (mob == null) return ItemStack.EMPTY;
        if (mob.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) return mob.getOffHandStack();
        if (mob.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) return mob.getMainHandStack();
        if (mob instanceof InventoryOwner owner) {
            for (int i = 0; i < owner.getInventory().size(); i++) {
                ItemStack stack = owner.getInventory().getStack(i);
                if (stack.isOf(Items.FIREWORK_ROCKET)) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }
}
