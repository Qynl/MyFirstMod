package dev.qynl.racers.car;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * A car a player can own, drive, tune and race. The physics live in
 * {@link CarEntity}; this class only adds the player specific bits.
 */
public class PlayerCarEntity extends CarEntity {
    public PlayerCarEntity(EntityType<? extends PlayerCarEntity> type, World world) {
        super(type, world);
    }

    @Override
    public boolean isPlayerDriven() {
        return !aiDriving;
    }
}
