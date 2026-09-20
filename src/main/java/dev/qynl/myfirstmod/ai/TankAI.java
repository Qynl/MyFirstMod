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

public final class TankAI {
    private TankAI() {}

    public static void executeTank(ServerWorld world, MobEntity tank, UnitDefinition unit) {
        if (tank == null || !tank.isAlive()) return;

        MinecraftServer server = world.getServer();
        String myFaction = UnitSystem.getTagValue(tank, "faction:");
        if (myFaction == null) return;

        // Taunt routine: force enemies to target this tank instead of vulnerable medics or archers
        if (tank.age % 40 == 0) {
            List<MobEntity> nearbyEnemies = world.getEntitiesByClass(MobEntity.class, tank.getBoundingBox().expand(14.0),
                    e -> e != tank && e.isAlive() && FactionManager.isHostile(server, myFaction, UnitSystem.getTagValue(e, "faction:")));

            for (MobEntity enemy : nearbyEnemies) {
                LivingEntity enemyTarget = enemy.getTarget();
                if (enemyTarget != null && enemyTarget != tank) {
                    String targetRole = UnitSystem.getTagValue(enemyTarget, "role:");
                    if ("medic".equalsIgnoreCase(targetRole) || "ranged".equalsIgnoreCase(targetRole) || "healer".equalsIgnoreCase(targetRole)) {
                        enemy.setTarget(tank); // Taunt!
                    }
                }
            }

            // If surrounded by 3 or more enemies, gain iron fortitude buff
            if (nearbyEnemies.size() >= 3) {
                tank.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 1, false, false));
                world.spawnParticles(ParticleTypes.ENCHANTED_HIT, tank.getX(), tank.getY() + 0.8, tank.getZ(), 8, 0.4, 0.4, 0.4, 0.05);
                world.playSound(null, tank.getX(), tank.getY(), tank.getZ(), SoundEvents.ITEM_SHIELD_BLOCK.value(), SoundCategory.NEUTRAL, 0.6f, 0.8f);
            }
        }
    }
}
