package dev.qynl.racers.car;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/**
 * A car that is always driven by the AI - the opponents in single player races
 * and the "let the AI show you the line" demo cars.
 */
public class AiCarEntity extends CarEntity {
    public AiCarEntity(EntityType<? extends AiCarEntity> type, World world) {
        super(type, world);
        setAiDriving(true);
    }

    @Override
    public boolean isPlayerDriven() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(net.minecraft.entity.Entity passenger) {
        // spectators can still hop in for a taxi ride, but they cannot take over
        return getPassengerList().isEmpty() && passenger instanceof net.minecraft.entity.player.PlayerEntity;
    }
}
