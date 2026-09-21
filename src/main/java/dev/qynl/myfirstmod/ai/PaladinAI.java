package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.List;

public final class PaladinAI {
    private PaladinAI() {}

    public static void executePaladin(ServerWorld world, MobEntity paladin, LivingEntity target, UnitDefinition unit) {
        if (paladin == null || target == null || !target.isAlive()) return;

        double distSq = paladin.squaredDistanceTo(target);
        if (distSq > 9.0) {
            paladin.getNavigation().startMovingTo(target, 1.2);
            return;
        }

        paladin.getLookControl().lookAt(target, 45.0f, 45.0f);

        if (paladin.age % 14 == 0) {
            paladin.swingHand(Hand.MAIN_HAND);

            float damage = unit.attackDamage;
            boolean isUndead = target.getType().isIn(EntityTypeTags.UNDEAD);

            if (isUndead) {
                damage *= 1.75f; // Huge Radiant Smite bonus vs Undead
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getBodyY(0.5), target.getZ(), 30, 0.4, 0.8, 0.4, 0.2);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 20, 0.4, 0.5, 0.4, 0.15);
                world.spawnParticles(ParticleTypes.FLASH, target.getX(), target.getBodyY(0.8), target.getZ(), 1, 0, 0, 0, 0);
            } else {
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.08);
            }

            boolean targetWasAlive = target.isAlive();
            target.damage(world.getDamageSources().mobAttack(paladin), damage);
            dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(world, target.getX(), target.getBodyY(0.75), target.getZ(), damage, isUndead);
            if (isUndead) {
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, target.getX(), target.getBodyY(1.1), target.getZ(), "⚡ SMITE!", net.minecraft.util.Formatting.GOLD);
            }

            // Holy Smite healing wave for nearby allies
            MinecraftServer server = world.getServer();
            String myFaction = UnitSystem.getTagValue(paladin, "faction:");

            if (server != null && myFaction != null) {
                List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, paladin.getBoundingBox().expand(8.0),
                        e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity ally : allies) {
                    ally.heal(2.5f);
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnHeal(world, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 2.5f);
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 0.5, ally.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
                }

                BattleStats.get(server).recordDamage(myFaction, damage);
                if (targetWasAlive && !target.isAlive()) {
                    BattleStats.get(server).recordKill(myFaction);
                    String vFaction = UnitSystem.getTagValue(target, "faction:");
                    if (vFaction != null) BattleStats.get(server).recordDeath(vFaction);
                    VeteranProgression.recordKillForUnit(world, paladin);
                }
            }

            // Visual impact shockwave & audio
            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.08);
            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            world.playSound(null, paladin.getX(), paladin.getY(), paladin.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.NEUTRAL, 0.9f, 1.4f);
            world.playSound(null, paladin.getX(), paladin.getY(), paladin.getZ(), SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.NEUTRAL, 0.7f, 1.6f);
        }
    }
}
