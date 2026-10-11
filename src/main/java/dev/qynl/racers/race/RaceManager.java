package dev.qynl.racers.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.AiCarEntity;
import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.PlayerCarEntity;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.registry.ModEntities;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.TrackManager;
import dev.qynl.racers.track.TrackPlacement;
import dev.qynl.racers.util.Cars;

/**
 * The race engine's front door: creates sessions, spawns the AI, feeds the HUD
 * and cleans up when players leave.
 */
public final class RaceManager {
    private static final Map<MinecraftServer, RaceManager> INSTANCES = new HashMap<>();

    private final MinecraftServer server;
    private final List<RaceSession> sessions = new ArrayList<>();
    private final Map<UUID, Long> lastHud = new HashMap<>();
    private int spawnCounter;

    private RaceManager(MinecraftServer server) {
        this.server = server;
    }

    public static RaceManager get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return INSTANCES.computeIfAbsent(server, RaceManager::new);
    }

    public static void forget(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    public List<RaceSession> sessions() {
        return sessions;
    }

    // ------------------------------------------------------------------
    // lookups
    // ------------------------------------------------------------------
    public RaceSession sessionFor(CarEntity car) {
        for (RaceSession session : sessions) {
            if (session.isLive() && session.entryFor(car) != null) {
                return session;
            }
        }
        return null;
    }

    public RaceEntry entryFor(CarEntity car) {
        RaceSession session = sessionFor(car);
        return session == null ? null : session.entryFor(car);
    }

    public RaceSession sessionFor(PlayerEntity player) {
        for (RaceSession session : sessions) {
            if (session.isLive() && session.contains(player)) {
                return session;
            }
        }
        return null;
    }

    public boolean isRacing(PlayerEntity player) {
        return sessionFor(player) != null;
    }

    // ------------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------------
    public RaceSession start(ServerPlayerEntity host, TrackPlacement placement, RaceFormat format,
                             int aiCount, double aiSkill) {
        RaceSession session = new RaceSession(server, placement, format);
        List<CarEntity> cars = new ArrayList<>();

        // players who are sitting in a car near the track, host first
        for (ServerPlayerEntity player : ModNetworking.players(server)) {
            if (!(player.getVehicle() instanceof CarEntity car)) {
                continue;
            }
            if (player.getUuid().equals(host.getUuid())) {
                continue;
            }
            if (placement.distanceTo(car.getPos()) > 220.0) {
                continue;
            }
            cars.add(car);
        }
        if (!(host.getVehicle() instanceof CarEntity hostCar)) {
            host.sendMessage(Text.translatable("race.fabricracers.need_car")
                    .formatted(Formatting.RED), false);
            return null;
        }
        cars.add(0, hostCar);

        // spawn the AI cars
        int spawned = 0;
        for (int i = 0; i < MathHelper.clamp(aiCount, 0, 15); i++) {
            AiCarEntity ai = spawnAiCar(placement, format, aiSkill + (i % 3) * 0.02, hostCar);
            if (ai == null) {
                break;
            }
            float yaw = placement.startYaw();
            Vec3d pos = placement.gridPos(cars.size());
            ai.teleportCleanly(pos.x, pos.y, pos.z, yaw);
            cars.add(ai);
            spawned++;
        }

        session.buildGrid(cars);
        if (session.entries().isEmpty()) {
            host.sendMessage(Text.translatable("race.fabricracers.need_car").formatted(Formatting.RED),
                    false);
            return null;
        }
        for (RaceEntry entry : session.entries()) {
            ServerPlayerEntity player = entry.player();
            if (player != null) {
                StatsManager.get(server).get(player).racesStarted++;
                StatsManager.get(server).markDirty();
            }
        }
        sessions.add(session);
        session.announce("Grid set: " + session.entries().size() + " cars, "
                + format.laps + " laps of " + placement.track.name);
        FabricRacers.LOGGER.info("Race started: {}", session.describe());
        return session;
    }

    private AiCarEntity spawnAiCar(TrackPlacement placement, RaceFormat format, double skill,
                                   CarEntity template) {
        var world = (net.minecraft.server.world.ServerWorld) template.getWorld();
        AiCarEntity car = new AiCarEntity(ModEntities.AI_CAR, world);
        CarData data = template.data() == null
                ? CarData.fresh(template.carType(), null)
                : new CarData(template.carType(), palettise(spawnCounter++), template.data().secondaryColor,
                        template.data().accentColor, balancedLevels(template, format),
                        1.0F, 1.0F, null, "FabricRacers AI", true);
        car.initFromData(data);
        car.setAiSkill(skill);
        car.setAiRubberBand(format.rubberBand);
        car.setAiDriving(true);
        Vec3d pos = placement.gridPos(0);
        car.refreshPositionAndAngles(pos.x, pos.y, pos.z, placement.startYaw(), 0.0F);
        if (!world.spawnEntity(car)) {
            return null;
        }
        return car;
    }

    /** AI cars are tuned to roughly match the player's car, so races stay fair. */
    private int[] balancedLevels(CarEntity template, RaceFormat format) {
        int[] levels = new int[dev.qynl.racers.car.Tune.values().length];
        int target = MathHelper.clamp((int) Math.round(format.aiSkill * 6.0), 0, 12);
        int[] order = {0, 3, 5, 1, 2, 4, 6};
        for (int i = 0; i < target; i++) {
            int tuneIndex = order[i % order.length];
            levels[tuneIndex] = Math.min(dev.qynl.racers.car.Tune.values()[tuneIndex].maxLevel,
                    levels[tuneIndex] + 1);
        }
        return levels;
    }

    private int palettise(int index) {
        int[] colors = {0xDC3B36, 0x2C5CC4, 0xF2C42A, 0x4FD05A, 0xF07A28, 0x8C3EBE,
                0x3CC8DC, 0xF090B8, 0xC8C8CE, 0x189454};
        return colors[Math.floorMod(index, colors.length)];
    }

    public void stop(RaceSession session, String reason) {
        for (RaceEntry entry : session.entries()) {
            entry.car.setControlLocked(false);
            entry.car.setRaceState(0);
            if (entry.car instanceof AiCarEntity ai) {
                ai.discard();
            }
        }
        sessions.remove(session);
        if (reason != null && !reason.isEmpty()) {
            session.announce(reason);
        }
    }

    public void shutdown() {
        for (RaceSession session : new ArrayList<>(sessions)) {
            stop(session, null);
        }
        sessions.clear();
    }

    public void onJoin(ServerPlayerEntity player) {
        ModNetworking.sendPlacements(player, TrackManager.get(server));
    }

    public void onDisconnect(ServerPlayerEntity player) {
        lastHud.remove(player.getUuid());
        for (RaceSession session : new ArrayList<>(sessions)) {
            RaceEntry entry = session.entryFor(player);
            if (entry != null && !entry.finished) {
                entry.finished = true;
                entry.finishMs = System.currentTimeMillis() - session.raceStartMs;
                StatsManager.get(server).recordWreck(player);
                session.announce(entry.label() + " left the race");
            }
        }
    }

    // ------------------------------------------------------------------
    // per tick
    // ------------------------------------------------------------------
    public void tick() {
        for (RaceSession session : new ArrayList<>(sessions)) {
            session.tick();
            if (session.state == RaceSession.State.FINISHED && session.finishedTicks() > 240) {
                stop(session, null);
            }
        }
        long now = System.currentTimeMillis();
        for (ServerPlayerEntity player : ModNetworking.players(server)) {
            long last = lastHud.getOrDefault(player.getUuid(), 0L);
            if (now - last < 180L) {
                continue;
            }
            lastHud.put(player.getUuid(), now);
            if (!(player.getVehicle() instanceof CarEntity car)) {
                continue;
            }
            RaceSession session = sessionFor(player);
            if (session != null) {
                ModNetworking.sendHud(player, session.hudFor(player));
            } else {
                ModNetworking.sendHud(player, RaceSession.freeRoamHud(player, car));
            }
            StatsManager.get(server).recordDrive(car);
        }
    }

    /** Clears the client HUD when a player stops driving. */
    public void clearHud(ServerPlayerEntity player) {
        lastHud.remove(player.getUuid());
    }
}
