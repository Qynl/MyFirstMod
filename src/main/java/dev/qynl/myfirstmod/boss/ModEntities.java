package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.kinetics.NullhookEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

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

    public static final EntityType<NullhookEntity> NULLHOOK = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MyFirstMod.MOD_ID, "nullhook"),
            // Explicit lambda (instead of a constructor reference) so generic
            // inference resolves to EntityType<NullhookEntity> despite the
            // entity having two constructors.
            EntityType.Builder.create((EntityType<NullhookEntity> type, World world) ->
                            new NullhookEntity(type, world), SpawnGroup.MISC)
                    .dimensions(0.35f, 0.35f)
                    .maxTrackingRange(64)
                    .trackingTickInterval(1)
                    .build("myfirstmod:nullhook")
    );

    public static void register() {}
}
