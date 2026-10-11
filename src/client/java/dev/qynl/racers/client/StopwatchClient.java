package dev.qynl.racers.client;

import net.minecraft.client.MinecraftClient;

import dev.qynl.racers.registry.ModComponents;
import dev.qynl.racers.util.Stopwatch;

/** Client side bridge so the stopwatch item can start/stop/save lap times. */
public final class StopwatchClient {
    private StopwatchClient() {
    }

    public static void toggle() {
        Stopwatch stopwatch = Stopwatch.items();
        if (stopwatch.running()) {
            var player = MinecraftClient.getInstance().player;
            if (player != null && player.getVehicle() instanceof dev.qynl.racers.car.CarEntity car) {
                stopwatch.stop(System.currentTimeMillis());
                var laps = stopwatch.laps();
                long lap = laps.isEmpty() ? stopwatch.elapsed(System.currentTimeMillis())
                        : laps.get(laps.size() - 1);
                if (player.getMainHandStack().getItem() instanceof dev.qynl.racers.item.ToolItems.Stopwatch) {
                    ModComponents.setLapTime(player.getMainHandStack(), lap);
                }
            } else {
                stopwatch.stop(System.currentTimeMillis());
            }
        } else {
            stopwatch.toggle();
        }
    }

    public static void lap() {
        Stopwatch.items().split(System.currentTimeMillis());
    }
}
