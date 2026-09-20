package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class SpecialEntityAI {
    private SpecialEntityAI() {}

    public static boolean handleSpecialEntity(ServerWorld world, MobEntity mob, LivingEntity target, UnitDefinition unit) {
        if (mob == null || !mob.isAlive()) return false;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(mob, "faction:");

        // 1. Allay Flying Fairy Healer
        if (mob instanceof AllayEntity allay) {
            if (mob.age % 25 == 0 && myFaction != null) {
                List<LivingEntity> injured = world.getEntitiesByClass(LivingEntity.class, allay.getBoundingBox().expand(16.0),
                        e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")) && e.getHealth() < e.getMaxHealth());

                if (!injured.isEmpty()) {
                    LivingEntity ally = injured.get(0);
                    allay.getNavigation().startMovingTo(ally, 1.4);
                    ally.heal(3.0f);
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, false, false));
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                    world.playSound(null, ally.getX(), ally.getY(), ally.getZ(), SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.NEUTRAL, 1.0f, 1.4f);
                }
            }
            return true;
        }

        // 2. Warden Sonic Boom Shockwave
        if (mob instanceof WardenEntity warden && target != null && target.isAlive()) {
            if (mob.age % 80 == 0) {
                double dx = target.getX() - warden.getX();
                double dy = target.getY() - warden.getY();
                double dz = target.getZ() - warden.getZ();

                // Sonic Boom shockwave
                world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 1.0f);

                target.damage(world.getDamageSources().sonicBoom(warden), 16.0f);
                target.takeKnockback(1.5, -dx, -dz);
            }
            return false;
        }

        // 3. Blaze Firestorm Volley
        if (mob instanceof BlazeEntity blaze && target != null && target.isAlive()) {
            if (mob.age % 30 == 0) {
                world.playSound(null, blaze.getX(), blaze.getY(), blaze.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
                target.setOnFireFor(5.0f);
                target.damage(world.getDamageSources().onFire(), 5.0f);
                world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            }
            return false;
        }

        // 4. Iron Golem Earthshaker Slam
        if (mob instanceof IronGolemEntity golem && target != null && target.isAlive()) {
            double distSq = golem.squaredDistanceTo(target);
            if (distSq < 16.0 && mob.age % 35 == 0) {
                golem.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                world.spawnParticles(ParticleTypes.EXPLOSION, golem.getX(), golem.getY(), golem.getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.NEUTRAL, 1.5f, 0.8f);

                List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, golem.getBoundingBox().expand(4.0),
                        e -> e != golem && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity enemy : enemies) {
                    enemy.damage(world.getDamageSources().mobAttack(golem), unit.attackDamage * 1.3f);
                    enemy.takeKnockback(0.8, golem.getX() - enemy.getX(), golem.getZ() - enemy.getZ());
                }
            }
        }

        return false;
    }
}
