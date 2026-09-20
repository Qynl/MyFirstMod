package dev.qynl.myfirstmod.ai;

import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

public final class DeathActionHandler {
    private DeathActionHandler() {}

    public static void handleDeath(ServerWorld world, LivingEntity entity, String deathAction, String factionId) {
        if (deathAction == null || deathAction.isEmpty() || deathAction.equalsIgnoreCase("none")) return;

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        switch (deathAction.toLowerCase()) {
            case "explosion" -> {
                world.createExplosion(entity, x, y, z, 2.5f, World.ExplosionSourceType.MOB);
            }
            case "healing_mist" -> {
                world.spawnParticles(ParticleTypes.HEART, x, y + 1.0, z, 25, 1.5, 0.8, 1.5, 0.1);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.5, z, 30, 2.0, 1.0, 2.0, 0.1);
                world.playSound(null, x, y, z, SoundEvents.BLOCK_BEACON_ACTIVATE.value(), SoundCategory.PLAYERS, 1.5f, 1.5f);

                List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class, entity.getBoundingBox().expand(8.0),
                        e -> e.isAlive() && FactionManager.isAllied(world.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity ally : allies) {
                    ally.heal(12.0f);
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
                }
            }
            case "lightning" -> {
                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.refreshPositionAfterTeleport(x, y, z);
                    world.spawnEntity(lightning);
                }
            }
            case "fireworks" -> {
                world.spawnParticles(ParticleTypes.FIREWORK, x, y + 1.0, z, 50, 0.5, 0.8, 0.5, 0.25);
                world.playSound(null, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST.value(), SoundCategory.PLAYERS, 2.0f, 1.0f);
            }
            case "poison_cloud" -> {
                world.spawnParticles(ParticleTypes.EFFECT, x, y + 0.5, z, 40, 2.0, 0.5, 2.0, 0.05);
                List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class, entity.getBoundingBox().expand(6.0),
                        e -> e.isAlive() && FactionManager.isHostile(world.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

                for (LivingEntity enemy : enemies) {
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 140, 1));
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 1));
                }
            }
            default -> {}
        }
    }
}
