package dev.qynl.myfirstmod.boss;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.world.World;

public class NullWardenEntity extends WardenEntity {
    public NullWardenEntity(EntityType<? extends WardenEntity> type, World world) {
        super(type, world);
    }
}
