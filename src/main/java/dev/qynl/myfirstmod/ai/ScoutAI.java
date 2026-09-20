package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class ScoutAI {
    private ScoutAI() {}

    public static void executeScout(ServerWorld world, MobEntity scout, UnitDefinition unit) {
        if (scout == null || !scout.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(scout, "faction:");
        if (myFaction == null) return;

        // Spotting routine every 30 ticks
        if (scout.age % 30 == 0) {
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, scout.getBoundingBox().expand(32.0),
                    e -> e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            if (!enemies.isEmpty()) {
                // Mark enemy commander with Glowing effect
                LivingEntity enemyLeader = enemies.stream().filter(e -> e.getCommandTags().contains("commander")).findFirst().orElse(enemies.get(0));
                enemyLeader.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 120, 0, false, false));

                // Alert particle effect
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, scout.getX(), scout.getY() + 1.2, scout.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                world.playSound(null, scout.getX(), scout.getY(), scout.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.8f, 1.6f);

                // Alert nearby squad members
                List<MobEntity> squadAllies = world.getEntitiesByClass(MobEntity.class, scout.getBoundingBox().expand(20.0),
                        e -> e != scout && e.isAlive() && myFaction.equals(UnitSystem.getTagValue(e, "faction:")));

                for (MobEntity ally : squadAllies) {
                    if (ally.getTarget() == null) {
                        ally.setTarget(enemyLeader);
                    }
                }
            }
        }
    }
}
