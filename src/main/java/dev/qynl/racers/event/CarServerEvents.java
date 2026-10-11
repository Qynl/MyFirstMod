package dev.qynl.racers.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.race.RaceSession;
import dev.qynl.racers.stats.StatsManager;

/**
 * Small bits of glue: mounting/dismounting bookkeeping, camera hints, keeping
 * players out of their own way while racing and clearing the HUD.
 */
public final class CarServerEvents {
    private CarServerEvents() {
    }

    public static void init() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            var session = RaceManager.get(player.server).sessionFor(player);
            if (session != null) {
                session.announce(player.getName().getString() + " left the area");
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Safety net: cars that somehow ended up under the world get parked one block up
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (player.getVehicle() instanceof CarEntity car && car.getY() < -64.0) {
                    car.teleportCleanly(car.getX(), 70.0, car.getZ(), car.getYaw());
                    player.sendMessage(Text.translatable("message.fabricracers.recovered")
                            .formatted(Formatting.YELLOW), true);
                }
            }
        });
    }

    /** Called when a player leaves a car: clears the HUD state and keeps stats fresh. */
    public static void onDismount(PlayerEntity player, CarEntity car) {
        RaceSession session = RaceManager.get(player.getServer()).sessionFor(player);
        if (session == null && player instanceof ServerPlayerEntity serverPlayer) {
            StatsManager.get(serverPlayer.server).recordDrive(car);
        }
    }
}
