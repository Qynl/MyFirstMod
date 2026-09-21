package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.BoggedEntity;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.entity.mob.EvokerEntity;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.passive.CamelEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

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
                    ally.heal(4.0f);
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, false, false));
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnHeal(world, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 4.0f);
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 5, 0.25, 0.25, 0.25, 0.05);
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

                world.spawnParticles(ParticleTypes.SONIC_BOOM, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                world.playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2.0f, 1.0f);

                target.damage(world.getDamageSources().sonicBoom(warden), 16.0f);
                target.takeKnockback(1.5, -dx, -dz);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, target.getX(), target.getBodyY(0.75), target.getZ(), 16.0f, true);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, target.getX(), target.getBodyY(1.1), target.getZ(), "💥 SONIC BOOM!", net.minecraft.util.Formatting.DARK_AQUA);
            }
            return false;
        }

        // 3. Breeze Wind Blast & Shockwave
        if (mob instanceof BreezeEntity breeze && target != null && target.isAlive()) {
            if (mob.age % 35 == 0) {
                breeze.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                double dx = target.getX() - breeze.getX();
                double dz = target.getZ() - breeze.getZ();
                world.playSound(null, breeze.getX(), breeze.getY(), breeze.getZ(), SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 1.2f, 1.0f);
                world.spawnParticles(ParticleTypes.GUST_EMITTER_LARGE, target.getX(), target.getY() + 0.5, target.getZ(), 1, 0, 0, 0, 0);

                // Expanding wind swirl ring
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI * i) / 16.0;
                    double px = target.getX() + Math.cos(angle) * 3.5;
                    double pz = target.getZ() + Math.sin(angle) * 3.5;
                    world.spawnParticles(ParticleTypes.CLOUD, px, target.getY() + 0.2, pz, 1, 0, 0.1, 0, 0.05);
                }

                List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(4.0),
                        e -> e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity enemy : enemies) {
                    float gustDmg = unit.attackDamage * 1.2f;
                    enemy.damage(world.getDamageSources().mobAttack(breeze), gustDmg);
                    enemy.takeKnockback(1.8, -dx, -dz);
                    enemy.addVelocity(0.0, 0.45, 0.0);
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, enemy.getX(), enemy.getBodyY(0.75), enemy.getZ(), gustDmg, false);
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, enemy.getX(), enemy.getBodyY(1.1), enemy.getZ(), "🌪 GUST KNOCKBACK!", net.minecraft.util.Formatting.AQUA);
                }
            }
            return false;
        }

        // 4. Bogged Poison Sniper
        if (mob instanceof BoggedEntity bogged && target != null && target.isAlive()) {
            if (mob.age % 30 == 0 && mob.squaredDistanceTo(target) < 900.0) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1, false, true));
                world.spawnParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            }
            return false;
        }

        // 5. Blaze Firestorm Volley
        if (mob instanceof BlazeEntity blaze && target != null && target.isAlive()) {
            if (mob.age % 30 == 0) {
                world.playSound(null, blaze.getX(), blaze.getY(), blaze.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1.0f, 1.0f);
                target.setOnFireFor(5.0f);
                target.damage(world.getDamageSources().onFire(), 5.0f);
                world.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
            }
            return false;
        }

        // 6. Wither Skeleton Shadow Curse
        if (mob instanceof WitherSkeletonEntity witherSkel && target != null && target.isAlive()) {
            if (mob.age % 25 == 0 && mob.squaredDistanceTo(target) < 16.0) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 1, false, true));
                world.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getBodyY(0.5), target.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
            }
            return false;
        }

        // 7. Evoker Fang Spell Barrage
        if (mob instanceof EvokerEntity evoker && target != null && target.isAlive()) {
            if (mob.age % 70 == 0) {
                evoker.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                world.playSound(null, evoker.getX(), evoker.getY(), evoker.getZ(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.2f, 1.0f);
                float yaw = (float) MathHelper.atan2(target.getZ() - evoker.getZ(), target.getX() - evoker.getX());

                for (int i = 0; i < 7; i++) {
                    double progress = i / 6.0;
                    double fx = evoker.getX() + (target.getX() - evoker.getX()) * progress;
                    double fz = evoker.getZ() + (target.getZ() - evoker.getZ()) * progress;
                    double fy = evoker.getY();

                    EvokerFangsEntity fangs = new EvokerFangsEntity(world, fx, fy, fz, yaw, i * 2, evoker);
                    world.spawnEntity(fangs);
                }
            }
            return false;
        }

        // 8. Witch Tactical Battle Alchemist
        if (mob instanceof WitchEntity witch) {
            if (mob.age % 45 == 0 && myFaction != null) {
                witch.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                List<LivingEntity> woundedAllies = world.getEntitiesByClass(LivingEntity.class, witch.getBoundingBox().expand(10.0),
                        e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")) && e.getHealth() < e.getMaxHealth() * 0.7f);

                if (!woundedAllies.isEmpty()) {
                    LivingEntity ally = woundedAllies.get(0);
                    ally.heal(6.0f);
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1));
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getBodyY(0.5), ally.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(null, ally.getX(), ally.getY(), ally.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.NEUTRAL, 0.9f, 1.2f);
                } else if (target != null && target.isAlive()) {
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
                    world.spawnParticles(ParticleTypes.WITCH, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                    world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_SPLASH_POTION_THROW, SoundCategory.HOSTILE, 0.9f, 0.9f);
                }
            }
            return false;
        }

        // 9. Ravager Earthshaker Stomp
        if (mob instanceof RavagerEntity ravager && target != null && target.isAlive()) {
            if (mob.age % 60 == 0 && mob.squaredDistanceTo(target) < 25.0) {
                world.playSound(null, ravager.getX(), ravager.getY(), ravager.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.8f, 0.9f);
                world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, ravager.getX(), ravager.getY() + 0.5, ravager.getZ(), 2, 0, 0, 0, 0);

                // Earthshaker ground radial shockwave
                for (int i = 0; i < 24; i++) {
                    double angle = (2 * Math.PI * i) / 24.0;
                    double px = ravager.getX() + Math.cos(angle) * 5.0;
                    double pz = ravager.getZ() + Math.sin(angle) * 5.0;
                    world.spawnParticles(ParticleTypes.CLOUD, px, ravager.getY() + 0.2, pz, 1, 0, 0.15, 0, 0.05);
                }

                List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, ravager.getBoundingBox().expand(6.0),
                        e -> e != ravager && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity enemy : enemies) {
                    float stompDmg = unit.attackDamage * 1.5f;
                    enemy.damage(world.getDamageSources().mobAttack(ravager), stompDmg);
                    enemy.addVelocity(0.0, 0.65, 0.0);
                    enemy.takeKnockback(1.4, ravager.getX() - enemy.getX(), ravager.getZ() - enemy.getZ());
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, enemy.getX(), enemy.getBodyY(0.75), enemy.getZ(), stompDmg, true);
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, enemy.getX(), enemy.getBodyY(1.1), enemy.getZ(), "💥 EARTHSHAKER!", net.minecraft.util.Formatting.RED);
                }
            }
            return false;
        }

        // 10. Wolf Pack Howl & Frenzy
        if (mob instanceof WolfEntity wolf) {
            if (mob.age % 80 == 0 && myFaction != null) {
                List<WolfEntity> pack = world.getEntitiesByClass(WolfEntity.class, wolf.getBoundingBox().expand(14.0),
                        w -> w.isAlive() && myFaction.equals(UnitSystem.getTagValue(w, "faction:")));

                if (pack.size() >= 2) {
                    world.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.NEUTRAL, 1.2f, 1.0f);
                    for (WolfEntity member : pack) {
                        member.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 180, 1));
                        member.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 180, pack.size() >= 4 ? 1 : 0));
                        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, member.getX(), member.getBodyY(0.6), member.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
            return false;
        }

        // 11. Iron Golem Earthshaker Slam
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

        // 12. Polar Bear / Grizzly Battle Bear Stomp & Fury
        if (mob instanceof PolarBearEntity bear && target != null && target.isAlive()) {
            if (mob.age % 40 == 0 && mob.squaredDistanceTo(target) < 16.0) {
                bear.setWarning(true);
                bear.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                world.playSound(null, bear.getX(), bear.getY(), bear.getZ(), SoundEvents.ENTITY_POLAR_BEAR_WARNING, SoundCategory.NEUTRAL, 1.4f, 0.8f);
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 2, 0.3, 0.3, 0.3, 0.0);

                target.damage(world.getDamageSources().mobAttack(bear), unit.attackDamage * 1.4f);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
                target.takeKnockback(1.2, bear.getX() - target.getX(), bear.getZ() - target.getZ());
            }
            return false;
        }

        // 13. Spider Web Snare Trap
        if (mob instanceof SpiderEntity spider && target != null && target.isAlive()) {
            if (mob.age % 50 == 0 && mob.squaredDistanceTo(target) < 25.0) {
                spider.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                world.playSound(null, spider.getX(), spider.getY(), spider.getZ(), SoundEvents.ENTITY_SPIDER_STEP, SoundCategory.HOSTILE, 1.0f, 1.2f);
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3, false, true));
                world.spawnParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            }
            return false;
        }

        // 14. Piglin Brute Bloodlust Whirlwind
        if (mob instanceof PiglinBruteEntity brute && target != null && target.isAlive()) {
            if (mob.age % 30 == 0 && mob.squaredDistanceTo(target) < 12.0) {
                brute.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                world.playSound(null, brute.getX(), brute.getY(), brute.getZ(), SoundEvents.ENTITY_PIGLIN_BRUTE_ANGRY, SoundCategory.HOSTILE, 1.2f, 1.0f);
                world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.6), target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                target.damage(world.getDamageSources().mobAttack(brute), unit.attackDamage * 1.35f);
            }
            return false;
        }

        return false;
    }
}
