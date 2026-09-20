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
    public static final EntityType<dev.qynl.myfirstmod.kingdom.RootboundPriorEntity> ROOTBOUND_PRIOR=Registry.register(Registries.ENTITY_TYPE,Identifier.of(MyFirstMod.MOD_ID,"rootbound_prior"),EntityType.Builder.create(dev.qynl.myfirstmod.kingdom.RootboundPriorEntity::new,SpawnGroup.MONSTER).dimensions(1.4f,3).maxTrackingRange(48).trackingTickInterval(1).build("myfirstmod:rootbound_prior"));
    public static final EntityType<dev.qynl.myfirstmod.mob.VeilWispEntity> VEIL_WISP=Registry.register(Registries.ENTITY_TYPE,Identifier.of(MyFirstMod.MOD_ID,"veil_wisp"),EntityType.Builder.create(dev.qynl.myfirstmod.mob.VeilWispEntity::new,SpawnGroup.CREATURE).dimensions(.7f,.7f).build("myfirstmod:veil_wisp"));
    public static void register() {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(VEIL_WISP,dev.qynl.myfirstmod.mob.VeilWispEntity.attributes());
        net.minecraft.entity.SpawnRestriction.register(VEIL_WISP,net.minecraft.entity.SpawnLocationTypes.ON_GROUND,net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(type,world,spawnType,pos,random)->world.getBlockState(pos.down()).isOf(dev.qynl.myfirstmod.block.ModBlocks.VEILSTONE)||world.getBlockState(pos.down()).isOf(dev.qynl.myfirstmod.block.ModBlocks.PRISMSTONE));
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ROOTBOUND_PRIOR,dev.qynl.myfirstmod.kingdom.RootboundPriorEntity.attributes());
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
