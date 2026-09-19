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

    public static void register() {}
}
