package dev.qynl.racers.track;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.net.ModNetworking;

/**
 * Owns every circuit definition and every placed copy of one.
 *
 * <p>Stock circuits live inside the jar, player made ones in the world folder.
 * Placements are stored in {@code placements.json} so a world remembers its
 * tracks and the race engine can find them again after a restart.
 */
public final class TrackManager {
    private static final Map<MinecraftServer, TrackManager> INSTANCES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] STOCK_FALLBACK = {
            "asphalt_park", "kart_circuit", "speed_oval", "chaos_junction", "drift_arena"};

    private final MinecraftServer server;
    private final Map<String, Track> tracks = new LinkedHashMap<>();
    private final List<TrackPlacement> placements = new ArrayList<>();
    private final Map<String, TrackStructure> structures = new HashMap<>();
    private final Map<UUID, BlockPos> wandAnchors = new HashMap<>();
    private final Map<UUID, Integer> wandRotations = new HashMap<>();
    private final Map<UUID, TrackRecorder> recorders = new HashMap<>();

    private TrackManager(MinecraftServer server) {
        this.server = server;
    }

    public static TrackManager get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return INSTANCES.computeIfAbsent(server, TrackManager::new);
    }

    public static void forget(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    public Path worldDir() {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("fabricracers").resolve("tracks");
    }

    // ------------------------------------------------------------------
    // loading
    // ------------------------------------------------------------------
    public void load() {
        tracks.clear();
        placements.clear();
        structures.clear();

        // stock tracks from the jar
        for (String id : stockIds()) {
            InputStream stream = TrackManager.class.getResourceAsStream(
                    "/data/fabricracers/tracks/" + id + ".json");
            if (stream == null) {
                continue;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Track track = Track.fromJson(id, JsonParser.parseReader(reader).getAsJsonObject(),
                        sizeOf(id), true);
                tracks.put(id, track);
            } catch (Exception exception) {
                FabricRacers.LOGGER.error("Could not load stock track {}", id, exception);
            }
        }

        // player tracks from the world
        Path path = worldDir();
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path file : stream.filter(f -> f.toString().endsWith(".json")
                        && !f.getFileName().toString().startsWith("_")).toList()) {
                    String fileName = file.getFileName().toString();
                    String id = fileName.substring(0, fileName.length() - 5);
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        Track track = Track.fromJson(id, JsonParser.parseReader(reader).getAsJsonObject(),
                                sizeOf(id), false);
                        tracks.put(id, track);
                    } catch (Exception exception) {
                        FabricRacers.LOGGER.warn("Skipping unreadable track {}", id, exception);
                    }
                }
            } catch (IOException exception) {
                FabricRacers.LOGGER.error("Could not read the tracks folder", exception);
            }
        }

        loadPlacements();
        FabricRacers.LOGGER.info("Loaded {} circuits and {} placed tracks",
                tracks.size(), placements.size());
    }

    private List<String> stockIds() {
        InputStream index = TrackManager.class.getResourceAsStream("/data/fabricracers/tracks/_index.json");
        if (index != null) {
            try (Reader reader = new InputStreamReader(index, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<String> ids = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray("tracks")) {
                    ids.add(element.getAsString());
                }
                if (!ids.isEmpty()) {
                    return ids;
                }
            } catch (Exception exception) {
                FabricRacers.LOGGER.warn("Falling back to the built-in track list", exception);
            }
        }
        return List.of(STOCK_FALLBACK);
    }

    public int[] sizeOf(String id) {
        TrackStructure structure = structure(id);
        return structure == null ? new int[]{1, 1, 1} : structure.size();
    }

    public TrackStructure structure(String id) {
        TrackStructure cached = structures.get(id);
        if (cached != null) {
            return cached;
        }
        TrackStructure loaded = TrackStructure.load(worldDir(), id);
        if (loaded != null) {
            structures.put(id, loaded);
        }
        return loaded;
    }

    private void loadPlacements() {
        Path file = worldDir().resolve("placements.json");
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : json.getAsJsonArray("placements")) {
                JsonObject entry = element.getAsJsonObject();
                Track track = tracks.get(entry.get("track").getAsString());
                if (track == null) {
                    continue;
                }
                TrackPlacement placement = new TrackPlacement(
                        UUID.fromString(entry.get("id").getAsString()), track,
                        new BlockPos(entry.get("x").getAsInt(), entry.get("y").getAsInt(),
                                entry.get("z").getAsInt()),
                        entry.has("rotation") ? entry.get("rotation").getAsInt() : 0);
                placements.add(placement);
            }
        } catch (Exception exception) {
            FabricRacers.LOGGER.error("Could not read placements.json", exception);
        }
    }

    public void save() {
        Path file = worldDir().resolve("placements.json");
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        for (TrackPlacement placement : placements) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", placement.id.toString());
            entry.addProperty("track", placement.track.id);
            entry.addProperty("x", placement.anchor.getX());
            entry.addProperty("y", placement.anchor.getY());
            entry.addProperty("z", placement.anchor.getZ());
            entry.addProperty("rotation", placement.rotation);
            array.add(entry);
        }
        json.add("placements", array);
        try {
            Files.createDirectories(worldDir());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not save placements.json", exception);
        }
    }

    public void tick() {
        for (TrackRecorder recorder : recorders.values()) {
            recorder.tick();
        }
    }

    // ------------------------------------------------------------------
    // accessors
    // ------------------------------------------------------------------
    public List<Track> all() {
        return new ArrayList<>(tracks.values());
    }

    public Track track(String id) {
        return tracks.get(id);
    }

    public List<TrackPlacement> placements() {
        return placements;
    }

    public TrackPlacement placementNear(Vec3d pos, double radius) {
        TrackPlacement best = null;
        double bestDistance = radius;
        for (TrackPlacement placement : placements) {
            double distance = placement.distanceTo(pos);
            if (distance < bestDistance && placement.contains(pos, 40.0)) {
                bestDistance = distance;
                best = placement;
            }
        }
        if (best == null) {
            for (TrackPlacement placement : placements) {
                double distance = placement.distanceTo(pos);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = placement;
                }
            }
        }
        return best;
    }

    public TrackPlacement placementAt(BlockPos pos) {
        for (TrackPlacement placement : placements) {
            if (placement.contains(pos.toCenterPos(), 0.0)) {
                return placement;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // wand bookkeeping
    // ------------------------------------------------------------------
    public void markWandAnchor(ServerPlayerEntity player, BlockPos pos) {
        wandAnchors.put(player.getUuid(), pos.toImmutable());
        wandRotations.put(player.getUuid(), suggestedRotation(player));
    }

    public BlockPos wandAnchor(ServerPlayerEntity player) {
        return wandAnchors.get(player.getUuid());
    }

    /** Tracks are laid out heading away from the way the player is looking. */
    public int suggestedRotation(ServerPlayerEntity player) {
        float yaw = player.getYaw();
        if (yaw >= -45.0F && yaw < 45.0F) {
            return 0;       // south, design +X is south-east, close enough
        }
        if (yaw >= 45.0F && yaw < 135.0F) {
            return 90;
        }
        if (yaw >= -135.0F && yaw < -45.0F) {
            return 270;
        }
        return 180;
    }

    // ------------------------------------------------------------------
    // placing / removing
    // ------------------------------------------------------------------
    public boolean placeTrack(ServerPlayerEntity player, Track track, BlockPos anchor, int rotation) {
        TrackStructure structure = structure(track.id);
        if (structure == null) {
            ModNetworking.send(player, ModNetworking.ServerAction.of(
                    ModNetworking.ACT_PLACE_RESULT, 0, 0, 0, 0,
                    "That track's structure file is missing."));
            return false;
        }
        if (overlapsExisting(anchor, rotation, structure)) {
            ModNetworking.send(player, ModNetworking.ServerAction.of(
                    ModNetworking.ACT_PLACE_RESULT, 0, 0, 0, 0,
                    "There is already a track here. Remove it first (sneak + wand)."));
            return false;
        }
        long start = System.currentTimeMillis();
        int placed = structure.place(player.getServerWorld(), anchor, rotation, null, null);
        TrackPlacement placement = new TrackPlacement(UUID.randomUUID(), track, anchor, rotation);
        placements.add(placement);
        save();
        syncAll();
        player.sendMessage(Text.translatable("message.fabricracers.track_placed",
                track.name, placed, track.sizeLabel()).formatted(Formatting.GREEN), false);
        player.sendMessage(Text.translatable("message.fabricracers.track_placed_tip")
                .formatted(Formatting.GRAY), false);
        ModNetworking.send(player, ModNetworking.ServerAction.of(
                ModNetworking.ACT_PLACE_RESULT, 0, 1, placed, 0,
                "Placed " + track.name + " (" + placed + " blocks in "
                        + (System.currentTimeMillis() - start) + "ms)"));
        // sanity: are there checkpoint markers where the JSON says?
        FabricRacers.LOGGER.info("Placed {} at {} ({}) - {} blocks", track.id, anchor, rotation, placed);
        return true;
    }

    private boolean overlapsExisting(BlockPos anchor, int rotation, TrackStructure structure) {
        int sx = structure.size()[0];
        int sz = structure.size()[2];
        int width = rotation % 180 == 0 ? sx : sz;
        int depth = rotation % 180 == 0 ? sz : sx;
        for (TrackPlacement placement : placements) {
            boolean xOverlap = anchor.getX() < placement.anchor.getX() + placement.track.size[0]
                    && placement.anchor.getX() < anchor.getX() + width;
            boolean zOverlap = anchor.getZ() < placement.anchor.getZ() + placement.track.size[2]
                    && placement.anchor.getZ() < anchor.getZ() + depth;
            if (xOverlap && zOverlap) {
                return true;
            }
        }
        return false;
    }

    public boolean removePlacement(ServerPlayerEntity player, TrackPlacement placement, boolean clearBlocks) {
        placements.remove(placement);
        save();
        int cleared = 0;
        if (clearBlocks) {
            TrackStructure structure = structure(placement.track.id);
            if (structure != null) {
                cleared = structure.clear(player.getServerWorld(), placement.anchor, placement.rotation);
            }
        }
        syncAll();
        player.sendMessage(Text.translatable("message.fabricracers.track_removed",
                placement.track.name, cleared).formatted(Formatting.YELLOW), false);
        return true;
    }

    /** Registers a player made track: writes its structure + definition next to the world. */
    public Track registerTrack(String id, Track track, TrackStructure structure) {
        try {
            Files.createDirectories(worldDir());
            Path nbt = worldDir().resolve(id + ".nbt");
            if (!Files.exists(nbt)) {
                TrackStructure.write(nbt, structure::writeCompressed);
            }
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not write track structure {}", id, exception);
        }
        tracks.put(id, track);
        JsonObject json = new JsonObject();
        json.addProperty("name", track.name);
        json.addProperty("short_name", track.shortName);
        json.addProperty("structure", track.structure);
        json.addProperty("width", track.width);
        json.addProperty("default_laps", track.defaultLaps);
        json.addProperty("author", track.author);
        JsonArray grid = new JsonArray();
        for (double[] slot : track.grid) {
            JsonArray point = new JsonArray();
            point.add(slot[0]);
            point.add(TrackStructure.CAR_Y);
            point.add(slot[1]);
            grid.add(point);
        }
        json.add("grid", grid);
        JsonArray checkpoints = new JsonArray();
        for (double[] cp : track.checkpoints) {
            JsonArray point = new JsonArray();
            point.add(cp[0]);
            point.add(TrackStructure.CAR_Y);
            point.add(cp[1]);
            point.add(cp[2]);
            checkpoints.add(point);
        }
        json.add("checkpoints", checkpoints);
        try (Writer writer = Files.newBufferedWriter(worldDir().resolve(id + ".json"),
                StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not write track definition {}", id, exception);
        }
        syncAll();
        return track;
    }

    public TrackRecorder recorder(ServerPlayerEntity player) {
        return recorders.computeIfAbsent(player.getUuid(), id -> new TrackRecorder(server, player));
    }

    public void stopRecording(ServerPlayerEntity player) {
        recorders.remove(player.getUuid());
    }

    // ------------------------------------------------------------------
    // sync
    // ------------------------------------------------------------------
    public void syncTo(ServerPlayerEntity player) {
        ModNetworking.sendPlacements(player, this);
    }

    public void syncAll() {
        for (ServerPlayerEntity player : ModNetworking.players(server)) {
            syncTo(player);
        }
    }
}
