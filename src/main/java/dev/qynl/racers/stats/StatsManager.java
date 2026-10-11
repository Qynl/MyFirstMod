package dev.qynl.racers.stats;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.race.RaceEntry;
import dev.qynl.racers.registry.ModItems;
import dev.qynl.racers.track.Track;
import dev.qynl.racers.util.Stopwatch;

/**
 * Per-player racing records: races, wins, best laps per circuit, drift points,
 * odometers and earned trophies.
 *
 * <p>Stored as JSON under {@code <world>/fabricracers/stats/<uuid>.json} so a
 * world keeps its championship history when it is copied around.
 */
public final class StatsManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, StatsManager> INSTANCES = new HashMap<>();

    private final MinecraftServer server;
    private final Map<UUID, RacerProfile> profiles = new HashMap<>();
    private boolean dirty;

    private StatsManager(MinecraftServer server) {
        this.server = server;
    }

    public static StatsManager get(MinecraftServer server) {
        if (server == null) {
            return null;
        }
        return INSTANCES.computeIfAbsent(server, StatsManager::new);
    }

    public static void forget(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    private Path dir() {
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("fabricracers").resolve("stats");
    }

    public RacerProfile get(PlayerEntity player) {
        RacerProfile profile = profiles.computeIfAbsent(player.getUuid(), id -> new RacerProfile());
        profile.name = player.getName().getString();
        return profile;
    }

    public void load() {
        profiles.clear();
        Path path = dir();
        if (!Files.isDirectory(path)) {
            return;
        }
        try (var stream = Files.list(path)) {
            for (Path file : stream.filter(f -> f.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String fileName = file.getFileName().toString();
                    UUID id = UUID.fromString(fileName.substring(0, fileName.length() - 5));
                    profiles.put(id, RacerProfile.fromJson(json));
                } catch (Exception exception) {
                    FabricRacers.LOGGER.warn("Skipping unreadable profile {}", file, exception);
                }
            }
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not read the stats folder", exception);
        }
        FabricRacers.LOGGER.info("Loaded {} racer profiles", profiles.size());
    }

    public void save() {
        if (!dirty) {
            return;
        }
        try {
            Files.createDirectories(dir());
            for (Map.Entry<UUID, RacerProfile> entry : profiles.entrySet()) {
                Path file = dir().resolve(entry.getKey().toString() + ".json");
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    GSON.toJson(entry.getValue().toJson(), writer);
                }
            }
            dirty = false;
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not write the stats folder", exception);
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void onJoin(ServerPlayerEntity player) {
        RacerProfile profile = get(player);
        profile.name = player.getName().getString();
        player.sendMessage(Text.translatable("race.fabricracers.welcome",
                Text.literal(profile.titleName()).formatted(Formatting.GOLD)).formatted(Formatting.GRAY),
                false);
        ModNetworking.sendProfile(player, toNbt(profile));
    }

    // ------------------------------------------------------------------
    // recording
    // ------------------------------------------------------------------
    public void recordLap(ServerPlayerEntity player, Track track, long millis) {
        RacerProfile profile = get(player);
        Long best = profile.bestLaps.get(track.id);
        if (best == null || millis < best) {
            profile.bestLaps.put(track.id, millis);
            player.sendMessage(Text.translatable("race.fabricracers.track_record",
                    track.name, Stopwatch.format(millis)).formatted(Formatting.LIGHT_PURPLE),
                    false);
        }
        markDirty();
    }

    public void recordRace(ServerPlayerEntity player, Track track, RaceEntry entry) {
        RacerProfile profile = get(player);
        profile.racesStarted++;
        profile.laps += Math.max(0, entry.lap - 1);
        profile.overtakes += entry.overtakes;
        profile.distance += entry.car.odometer();
        profile.topSpeedKmh = Math.max(profile.topSpeedKmh, Math.abs(entry.car.getSpeed()) * 3.6);
        profile.driftPoints += (int) entry.car.getDriftScore();
        profile.trackRaces.merge(track.id, 1, Integer::sum);
        if (entry.finished) {
            profile.racesFinished++;
            if (entry.finishPosition == 1) {
                profile.wins++;
                profile.podiums++;
            } else if (entry.finishPosition <= 3) {
                profile.podiums++;
            }
        } else {
            profile.dnfs++;
        }
        markDirty();
        ModNetworking.sendProfile(player, toNbt(profile));
    }

    public void recordDrive(CarEntity car) {
        if (car.getDriver() instanceof ServerPlayerEntity player) {
            RacerProfile profile = get(player);
            profile.distance = Math.max(profile.distance, car.odometer());
            profile.topSpeedKmh = Math.max(profile.topSpeedKmh, Math.abs(car.getSpeed()) * 3.6);
            profile.driftPoints = Math.max(profile.driftPoints, (int) car.getDriftScore());
            markDirty();
        }
    }

    public void awardTrophy(ServerPlayerEntity player, int position) {
        RacerProfile profile = get(player);
        profile.trophies++;
        markDirty();
        String itemName = switch (position) {
            case 1 -> "trophy_gold";
            case 2 -> "trophy_silver";
            default -> "trophy_bronze";
        };
        ItemStack stack = new ItemStack(switch (position) {
            case 1 -> ModItems.TROPHY_GOLD;
            case 2 -> ModItems.TROPHY_SILVER;
            default -> ModItems.TROPHY_BRONZE;
        });
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        player.sendMessage(Text.translatable("race.fabricracers.trophy", position,
                Text.translatable("item.fabricracers." + itemName)).formatted(Formatting.GOLD), false);
    }

    public void givePrize(ServerPlayerEntity player, int emeralds) {
        if (emeralds <= 0) {
            return;
        }
        RacerProfile profile = get(player);
        profile.prizesWon += emeralds;
        ItemStack stack = new ItemStack(Items.EMERALD, emeralds);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        markDirty();
    }

    public void recordWreck(ServerPlayerEntity player) {
        RacerProfile profile = get(player);
        profile.dnfs++;
        markDirty();
    }

    public void sendRecord(ServerPlayerEntity player) {
        RacerProfile profile = get(player);
        player.sendMessage(Text.literal(profile.summary()).formatted(Formatting.GOLD), false);
        if (!profile.bestLaps.isEmpty()) {
            player.sendMessage(Text.literal("Best laps:").formatted(Formatting.GRAY), false);
            profile.bestLaps.forEach((track, millis) -> player.sendMessage(
                    Text.literal(" - " + track + ": " + Stopwatch.format(millis))
                            .formatted(Formatting.WHITE), false));
        }
        ModNetworking.sendProfile(player, toNbt(profile));
    }

    public NbtCompound toNbt(RacerProfile profile) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", profile.name);
        nbt.putInt("races", profile.racesStarted);
        nbt.putInt("finished", profile.racesFinished);
        nbt.putInt("wins", profile.wins);
        nbt.putInt("podiums", profile.podiums);
        nbt.putInt("laps", profile.laps);
        nbt.putInt("overtakes", profile.overtakes);
        nbt.putInt("drift", profile.driftPoints);
        nbt.putInt("cars", profile.carsPlaced);
        nbt.putInt("tracks", profile.tracksRecorded);
        nbt.putInt("trophies", profile.trophies);
        nbt.putDouble("distance", profile.distance);
        nbt.putDouble("topSpeed", profile.topSpeedKmh);
        nbt.putString("title", profile.titleName());
        NbtList laps = new NbtList();
        profile.bestLaps.forEach((track, millis) -> {
            NbtCompound entry = new NbtCompound();
            entry.putString("track", track);
            entry.putLong("ms", millis);
            laps.add(entry);
        });
        nbt.put("bestLaps", laps);
        return nbt;
    }

    public java.util.Collection<RacerProfile> profiles() {
        return profiles.values();
    }

    /** Leaderboard for a track: best laps, sorted. */
    public java.util.List<Map.Entry<String, Long>> leaderboard(String trackId) {
        java.util.List<Map.Entry<String, Long>> list = new java.util.ArrayList<>();
        for (RacerProfile profile : profiles.values()) {
            Long best = profile.bestLaps.get(trackId);
            if (best != null) {
                list.add(new java.util.AbstractMap.SimpleEntry<>(profile.name, best));
            }
        }
        list.sort(Map.Entry.comparingByValue());
        return list;
    }
}
