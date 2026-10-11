package dev.qynl.racers.registry;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.AiCarEntity;
import dev.qynl.racers.car.PlayerCarEntity;

/** Entity types. Two of them, sharing everything except the driver source. */
public final class ModEntities {
    public static final EntityType<PlayerCarEntity> PLAYER_CAR = Registry.register(
            Registries.ENTITY_TYPE, FabricRacers.id("player_car"),
            EntityType.Builder.<PlayerCarEntity>create(PlayerCarEntity::new, SpawnGroup.MISC)
                    .dimensions(1.9F, 1.25F)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .build("player_car"));

    public static final EntityType<AiCarEntity> AI_CAR = Registry.register(
            Registries.ENTITY_TYPE, FabricRacers.id("ai_car"),
            EntityType.Builder.<AiCarEntity>create(AiCarEntity::new, SpawnGroup.MISC)
                    .dimensions(1.9F, 1.25F)
                    .maxTrackingRange(16)
                    .trackingTickInterval(1)
                    .build("ai_car"));

    private ModEntities() {
    }

    public static void init() {
        // CarEntity extends Entity (not LivingEntity), so no attribute defaults are needed.
    }
}
