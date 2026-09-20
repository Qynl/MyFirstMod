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

public final class PaladinAI {
    private PaladinAI() {}

    public static void executePaladin(ServerWorld world, MobEntity paladin, LivingEntity target, UnitDefinition unit) {
        if (paladin == null || target == null || !target.isAlive()) return;

        double distSq = paladin.squaredDistanceTo(target);
        if (distSq > 9.0) {
            paladin.getNavigation().startMovingTo(target, 1.15);
            return;
        }

        paladin.getLookControl().lookAt(target, 30.0f, 30.0f);

        if (paladin.age % 14 == 0) {
            paladin.swingHand(Hand.MAIN_HAND);

            float damage = unit.attackDamage;
            if (target.isUndead()) {
                damage *= 1.6f; // Radiant smite bonus vs undead
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getBodyY(0.5), target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            }

            boolean targetWasAlive = target.isAlive();
            target.damage(world.getDamageSources().mobAttack(paladin), damage);

            // Holy Smite healing wave for nearby allies
            MinecraftServer server = world.getServer();
            String myFaction = UnitSystem.getTagValue(paladin, "faction:");

            if (server != null && myFaction != null) {
                List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, paladin.getBoundingBox().expand(8.0),
                        e -> e.isAlive() && FactionManager.isAllied(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity ally : allies) {
                    ally.heal(2.0f);
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

            world.spawnParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getBodyY(0.5), target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            world.playSound(null, paladin.getX(), paladin.getY(), paladin.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME.value(), SoundCategory.NEUTRAL, 1.0f, 1.4f);
        }
    }
}
