package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<NullWardenEntity> NULL_WARDEN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MyFirstMod.MOD_ID, "null_warden"),
            EntityType.Builder.create(NullWardenEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.0f, 3.5f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build("myfirstmod:null_warden")
    );

    public static final EntityType<dev.qynl.myfirstmod.mob.RiftSentinelEntity> RIFT_SENTINEL = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(MyFirstMod.MOD_ID, "rift_sentinel"),
            EntityType.Builder.create(dev.qynl.myfirstmod.mob.RiftSentinelEntity::new, SpawnGroup.MONSTER)
                    .dimensions(.6f, 1.95f).build("myfirstmod:rift_sentinel"));
    public static final EntityType<dev.qynl.myfirstmod.mob.ShardstalkerEntity> SHARDSTALKER = Registry.register(
            Registries.ENTITY_TYPE, Identifier.of(MyFirstMod.MOD_ID, "shardstalker"),
            EntityType.Builder.create(dev.qynl.myfirstmod.mob.ShardstalkerEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.4f, .9f).build("myfirstmod:shardstalker"));

    public static final EntityType<dev.qynl.myfirstmod.mob.RiftHeraldEntity> RIFT_HERALD=Registry.register(
            Registries.ENTITY_TYPE,Identifier.of(MyFirstMod.MOD_ID,"rift_herald"),
            EntityType.Builder.create(dev.qynl.myfirstmod.mob.RiftHeraldEntity::new,SpawnGroup.MONSTER)
                    .dimensions(1.3f,2.4f).maxTrackingRange(48).trackingTickInterval(2).build("myfirstmod:rift_herald"));
    public static final EntityType<dev.qynl.myfirstmod.keep.GraveRegentEntity> GRAVE_REGENT=Registry.register(Registries.ENTITY_TYPE,Identifier.of(MyFirstMod.MOD_ID,"grave_regent"),EntityType.Builder.create(dev.qynl.myfirstmod.keep.GraveRegentEntity::new,SpawnGroup.MONSTER).dimensions(1.5f,3.5f).maxTrackingRange(64).trackingTickInterval(1).build("myfirstmod:grave_regent"));
    public static void register() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(GRAVE_REGENT,dev.qynl.myfirstmod.keep.GraveRegentEntity.attributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                RIFT_HERALD,dev.qynl.myfirstmod.mob.RiftHeraldEntity.attributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                NULL_WARDEN, net.minecraft.entity.mob.WardenEntity.addAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                RIFT_SENTINEL, dev.qynl.myfirstmod.mob.RiftSentinelEntity.attributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                SHARDSTALKER, dev.qynl.myfirstmod.mob.ShardstalkerEntity.attributes());
        net.minecraft.entity.SpawnRestriction.register(RIFT_SENTINEL,
                net.minecraft.entity.SpawnLocationTypes.ON_GROUND, net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.entity.mob.HostileEntity::canSpawnInDark);
        net.minecraft.entity.SpawnRestriction.register(SHARDSTALKER,
                net.minecraft.entity.SpawnLocationTypes.ON_GROUND, net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.entity.mob.HostileEntity::canSpawnInDark);
    }
}
