package dev.qynl.racers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.qynl.racers.command.RaceCommands;
import dev.qynl.racers.event.CarServerEvents;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.registry.ModBlocks;
import dev.qynl.racers.registry.ModComponents;
import dev.qynl.racers.registry.ModEntities;
import dev.qynl.racers.registry.ModItems;
import dev.qynl.racers.registry.ModSounds;
import dev.qynl.racers.shop.PartShop;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.TrackManager;

/**
 * FabricRacers - a complete racing game inside Minecraft.
 *
 * <p>Feature map (see README.md for the player facing docs):
 * <ul>
 *   <li>{@link dev.qynl.racers.car.CarEntity} - arcade/semi-physical car physics with
 *       gears, grip limits, drifting, boost, damage and tuning levels.</li>
 *   <li>{@link dev.qynl.racers.ai.AiDriver} - AI drivers that follow a racing line,
 *       brake for corners, overtake, avoid traffic, rubber-band and make mistakes.</li>
 *   <li>{@link dev.qynl.racers.track.TrackManager} - tracks are handled through
 *       placeable track "anchors" backed by vanilla structure files.</li>
 *   <li>{@link dev.qynl.racers.race.RaceManager} - the race engine: formats, grids,
 *       countdown, checkpoints, laps, standings, pit stops, prizes and world records.</li>
 * </ul>
 */
public class FabricRacers implements ModInitializer {
    public static final String MOD_ID = "fabricracers";
    public static final Logger LOGGER = LoggerFactory.getLogger("FabricRacers");

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModItems.init();
        ModEntities.init();
        ModComponents.init();
        ModSounds.init();
        ModNetworking.init();
        PartShop.init();

        CarServerEvents.init();
        RaceCommands.init();

        // --- world lifecycle -------------------------------------------------
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            StatsManager.get(server).load();
            TrackManager.get(server).load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            StatsManager.get(server).save();
            TrackManager.get(server).save();
            RaceManager.get(server).shutdown();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RaceManager.get(server).tick();
            TrackManager.get(server).tick();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            StatsManager.get(server).onJoin(handler.getPlayer());
            TrackManager.get(server).syncTo(handler.getPlayer());
            RaceManager.get(server).onJoin(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                RaceManager.get(server).onDisconnect(handler.getPlayer()));

        LOGGER.info("FabricRacers loaded - start your engines.");
    }
}
