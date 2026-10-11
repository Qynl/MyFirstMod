package dev.qynl.racers.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.util.Sounds;
import dev.qynl.racers.registry.ModItems;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.TrackPlacement;
import dev.qynl.racers.util.Interp;
import dev.qynl.racers.util.Stopwatch;

/**
 * The race state machine for one circuit: countdown, racing, results.
 *
 * <p>Responsibilities: ordering the grid, watching checkpoint progress, lap and
 * race timing, live standings, rubber banding, pit stop bookkeeping, finishing
 * rules, prizes and the HUD snapshot.
 */
public final class RaceSession {
    public enum State {
        COUNTDOWN,
        RACING,
        FINISHED
    }

    private final MinecraftServer server;
    private final TrackPlacement placement;
    private final RaceFormat format;
    private final List<RaceEntry> entries = new ArrayList<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final long createdMs = System.currentTimeMillis();
    public State state = State.COUNTDOWN;
    public int countdown;
    public long raceStartMs;
    public String banner = "";
    public int bannerTicks;
    public int finishedTicks;
    private long lastOvertakeCheck;
    private int abandoned;

    public RaceSession(MinecraftServer server, TrackPlacement placement, RaceFormat format) {
        this.server = server;
        this.placement = placement;
        this.format = format;
        this.countdown = format.countdownTicks;
        this.raceStartMs = System.currentTimeMillis() + countdown * 50L;
    }

    public TrackPlacement placement() {
        return placement;
    }

    public RaceFormat format() {
        return format;
    }

    public List<RaceEntry> entries() {
        return entries;
    }

    public RaceEntry entryFor(CarEntity car) {
        for (RaceEntry entry : entries) {
            if (entry.car == car) {
                return entry;
            }
        }
        return null;
    }

    public RaceEntry entryFor(PlayerEntity player) {
        for (RaceEntry entry : entries) {
            if (player.getUuid().equals(entry.playerId)) {
                return entry;
            }
        }
        return null;
    }

    public boolean contains(PlayerEntity player) {
        return entryFor(player) != null || spectators.contains(player.getUuid());
    }

    public void addSpectator(PlayerEntity player) {
        spectators.add(player.getUuid());
    }

    public Set<ServerPlayerEntity> viewers() {
        Set<ServerPlayerEntity> viewers = new HashSet<>();
        for (RaceEntry entry : entries) {
            ServerPlayerEntity player = entry.player();
            if (player != null) {
                viewers.add(player);
            }
        }
        for (UUID id : spectators) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null) {
                viewers.add(player);
            }
        }
        return viewers;
    }

    /** Assigns the grid: players first, then the AI filling the remaining slots. */
    public void buildGrid(List<CarEntity> cars) {
        int slot = 0;
        for (CarEntity car : cars) {
            if (entries.size() >= placement.gridSlots()) {
                break;
            }
            Vec3d pos = placement.gridPos(slot);
            float yaw = placement.startYaw();
            car.teleportCleanly(pos.x, pos.y, pos.z, yaw);
            car.setControlLocked(true);
            car.setRaceState(1);
            car.resetHealth();
            UUID playerId = car.getDriver() instanceof PlayerEntity player ? player.getUuid() : null;
            String name = playerId == null ? "AI " + (entries.size() + 1)
                    : car.getDriver().getName().getString();
            double skill = MathHelper.clamp(format.aiSkill + (entries.size() % 3) * 0.02, 0.2, 1.0);
            RaceEntry entry = new RaceEntry(car, playerId, name, playerId == null,
                    playerId == null ? skill : 0.0);
            entry.lapStartMs = System.currentTimeMillis();
            entries.add(entry);
            slot++;
        }
    }

    public void addEntry(RaceEntry entry) {
        entries.add(entry);
    }

    public int lapCount() {
        return format.laps;
    }

    // ------------------------------------------------------------------
    // ticking
    // ------------------------------------------------------------------
    public void tick() {
        long now = System.currentTimeMillis();
        if (bannerTicks > 0) {
            bannerTicks--;
        }
        switch (state) {
            case COUNTDOWN -> tickCountdown(now);
            case RACING -> tickRacing(now);
            case FINISHED -> tickFinished();
            default -> {
            }
        }
    }

    private void tickCountdown(long now) {
        int previous = countdown;
        countdown--;
        if (countdown <= 0) {
            state = State.RACING;
            raceStartMs = now;
            banner("GO!", 40);
            for (RaceEntry entry : entries) {
                entry.car.setControlLocked(false);
                entry.car.setAiDriving(entry.ai);
                entry.lapStartMs = now;
                ServerPlayerEntity player = entry.player();
                if (player != null) {
                    player.playSoundToPlayer(Sounds.of(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                            SoundCategory.NEUTRAL, 1.0F, 1.6F);
                }
            }
            return;
        }
        if (countdown % 20 == 0 || previous > 100) {
            int seconds = (countdown + 19) / 20;
            for (ServerPlayerEntity player : viewers()) {
                player.playSoundToPlayer(Sounds.of(SoundEvents.BLOCK_NOTE_BLOCK_PLING),
                        SoundCategory.NEUTRAL, 1.0F, seconds <= 1 ? 2.0F : 1.2F);
            }
        }
    }

    private void tickRacing(long now) {
        updateProgress(now);
        for (RaceEntry entry : entries) {
            if (entry.finished) {
                continue;
            }
            CarEntity car = entry.car;
            if (car.isRemoved()) {
                entry.finished = true;
                entry.finishMs = now - raceStartMs;
                entry.finishPosition = ++abandoned;
                continue;
            }
            if (entry.offTrackTicks > 0) {
                entry.offTrackTicks--;
            }
        }
        int finished = countFinished();
        if (finished >= entries.size()) {
            finishAll();
        } else if (finished > 0 && finishedWindow(now)) {
            // give everyone else a chance, then close the session
            finishAll();
        }
        if (now - lastOvertakeCheck > 250L) {
            lastOvertakeCheck = now;
            trackOvertakes();
        }
    }

    private void tickFinished() {
        finishedTicks++;
        if (finishedTicks == 100) {
            for (RaceEntry entry : entries) {
                entry.car.setRaceState(0);
            }
        }
    }

    public int finishedTicks() {
        return finishedTicks;
    }

    private boolean finishedWindow(long now) {
        long elapsed = now - firstFinishMs();
        return elapsed > 5000L + format.laps * 15000L;
    }

    private long firstFinishMs() {
        for (RaceEntry entry : entries) {
            if (entry.finishMs >= 0) {
                return raceStartMs + entry.finishMs;
            }
        }
        return Long.MAX_VALUE;
    }

    private boolean anyFinished() {
        return countFinished() > 0;
    }

    private int countFinished() {
        int count = 0;
        for (RaceEntry entry : entries) {
            if (entry.finished) {
                count++;
            }
        }
        return count;
    }

    private void finishAll() {
        long now = System.currentTimeMillis();
        for (RaceEntry entry : entries) {
            if (!entry.finished) {
                entry.finished = true;
                entry.finishMs = now - raceStartMs;
                entry.finishPosition = countFinished();
            }
        }
        state = State.FINISHED;
        finishedTicks = 0;
        awardPrizes();
        announceResults();
    }

    // ------------------------------------------------------------------
    // progress
    // ------------------------------------------------------------------
    private void updateProgress(long now) {
        int checkpoints = placement.checkpointCount();
        for (RaceEntry entry : entries) {
            CarEntity car = entry.car;
            if (entry.finished) {
                entry.progress = 1_000_000.0 - entry.finishPosition * 10.0;
                continue;
            }
            Vec3d target = placement.checkpointPos(entry.nextCheckpoint);
            double distance = Math.hypot(car.getX() - target.x, car.getZ() - target.z);
            double radius = Math.max(3.5, placement.track.halfWidth() + 2.0);
            if (distance < radius) {
                if (entry.nextCheckpoint == 0) {
                    // crossed the line
                    entry.lastLapMs = now - entry.lapStartMs;
                    if (entry.bestLapMs == 0L || entry.lastLapMs < entry.bestLapMs) {
                        entry.bestLapMs = entry.lastLapMs;
                        ServerPlayerEntity player = entry.player();
                        if (player != null) {
                            StatsManager.get(server).recordLap(player, placement.track,
                                    entry.bestLapMs);
                        }
                    }
                    entry.lapStartMs = now;
                    entry.lap++;
                    if (entry.lap > format.laps) {
                        entry.finished = true;
                        entry.finishMs = now - raceStartMs;
                        entry.finishPosition = countFinished();
                        onFinish(entry);
                        continue;
                    } else {
                        ServerPlayerEntity player = entry.player();
                        if (player != null) {
                            player.sendMessage(Text.translatable("race.fabricracers.lap",
                                            entry.lap, format.laps,
                                            Stopwatch.formatShort(entry.lastLapMs))
                                    .formatted(Formatting.GOLD), true);
                        }
                    }
                }
                entry.nextCheckpoint = (entry.nextCheckpoint + 1) % checkpoints;
            }
            entry.progress = (entry.lap - 1) * 100000.0 + entry.nextCheckpoint * 1000.0
                    - distance;
            entry.totalMs = now - raceStartMs;
        }
        rank();
    }

    private void onFinish(RaceEntry entry) {
        CarEntity car = entry.car;
        // the car keeps circulating with the AI so the podium ceremony looks alive
        if (!entry.ai && entry.player() != null) {
            entry.aiTakeover = true;
            car.setAiDriving(true);
            car.setAiRubberBand(false);
            ServerPlayerEntity player = entry.player();
            player.sendMessage(Text.translatable("race.fabricracers.finished",
                    entry.finishPosition, Stopwatch.format(entry.finishMs))
                    .formatted(Formatting.GREEN), false);
            player.playSoundToPlayer(Sounds.of(SoundEvents.ENTITY_PLAYER_LEVELUP), SoundCategory.NEUTRAL,
                    0.7F, 1.3F);
            if (entry.finishPosition <= 3) {
                StatsManager.get(server).awardTrophy(player, entry.finishPosition);
            }
        }
        ServerWorld world = (ServerWorld) car.getWorld();
        world.spawnParticles(ParticleTypes.FIREWORK, car.getX(), car.getY() + 1.0, car.getZ(),
                20, 0.4, 0.4, 0.4, 0.15);
        world.playSound(null, car.getX(), car.getY(), car.getZ(),
                SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.NEUTRAL, 0.8F, 1.2F);
    }

    private void rank() {
        entries.sort(Comparator.comparingDouble((RaceEntry entry) -> -entry.progress));
        int position = 1;
        for (RaceEntry entry : entries) {
            entry.position = position++;
        }
    }

    private void trackOvertakes() {
        rank();
        for (RaceEntry entry : entries) {
            if (entry.lastPosition != 0 && entry.position < entry.lastPosition) {
                entry.overtakes += entry.lastPosition - entry.position;
            }
            entry.lastPosition = entry.position;
        }
    }

    public void announce(String message) {
        banner(message, 60);
        for (ServerPlayerEntity player : viewers()) {
            player.sendMessage(Text.literal(message).formatted(Formatting.AQUA), false);
        }
    }

    private void banner(String message, int ticks) {
        banner = message;
        bannerTicks = ticks;
    }

    private void announceResults() {
        StringBuilder builder = new StringBuilder();
        builder.append("=== ").append(placement.track.name).append(" results ===\n");
        List<RaceEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(entry -> entry.finishPosition));
        for (RaceEntry entry : sorted) {
            builder.append(entry.finishPosition).append(". ").append(entry.label())
                    .append("  ").append(Stopwatch.format(entry.finishMs))
                    .append(entry.bestLapMs > 0 ? "  (best lap " + Stopwatch.format(entry.bestLapMs) + ")" : "")
                    .append("\n");
        }
        for (ServerPlayerEntity player : viewers()) {
            player.sendMessage(Text.literal(builder.toString()).formatted(Formatting.WHITE), false);
        }
    }

    private void awardPrizes() {
        StatsManager stats = StatsManager.get(server);
        for (RaceEntry entry : entries) {
            ServerPlayerEntity player = entry.player();
            if (player == null) {
                continue;
            }
            stats.recordRace(player, placement.track, entry);
            if (entry.finishPosition == 1) {
                stats.givePrize(player, format.prizeEmeralds + 4);
            } else if (entry.finishPosition <= 3) {
                stats.givePrize(player, format.prizeEmeralds);
            }
        }
    }

    // ------------------------------------------------------------------
    // helpers used by the AI + HUD
    // ------------------------------------------------------------------
    /** 0.9 .. 1.12 speed multiplier that keeps the pack together. */
    public double rubberBand(CarEntity car, RaceEntry entry) {
        if (!format.rubberBand) {
            return 1.0;
        }
        double best = 0.0;
        double worst = Double.MAX_VALUE;
        for (RaceEntry other : entries) {
            best = Math.max(best, other.progress);
            worst = Math.min(worst, other.progress);
        }
        double spread = best - worst;
        if (spread < 4500.0) {
            return 1.0;
        }
        double behind = best - entry.progress;
        double factor = 1.0 + MathHelper.clamp(behind / 60000.0, 0.0, 0.11);
        if (behind < spread * 0.25) {
            factor = 0.965;
        }
        return MathHelper.clamp(factor, 0.92, 1.12);
    }

    /** Position of a car in the live standings. */
    public int positionOf(CarEntity car) {
        RaceEntry entry = entryFor(car);
        return entry == null ? 0 : entry.position;
    }

    public boolean isFinished() {
        return state == State.FINISHED;
    }

    public boolean isLive() {
        return state == State.RACING || state == State.COUNTDOWN;
    }

    public String standingsText() {
        StringBuilder builder = new StringBuilder();
        List<RaceEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(entry -> entry.finished ? entry.finishPosition : 100 + entry.position));
        int shown = 0;
        for (RaceEntry entry : sorted) {
            if (shown++ >= 10) {
                break;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(entry.position).append(' ').append(entry.label()).append(' ')
                    .append(entry.finished ? Stopwatch.format(entry.finishMs) : "L" + entry.lap)
                    .append(' ').append(entry.bestLapMs > 0 ? Stopwatch.format(entry.bestLapMs) : "");
        }
        return builder.toString();
    }

    public ModNetworking.Hud hudFor(ServerPlayerEntity player) {
        RaceEntry entry = entryFor(player);
        CarEntity car = entry != null ? entry.car : (player.getVehicle() instanceof CarEntity c ? c : null);
        int flags = 0;
        if (car != null) {
            if (car.isDrifting()) {
                flags |= ModNetworking.Hud.FLAG_DRIFTING;
            }
            if (car.isBoosting()) {
                flags |= ModNetworking.Hud.FLAG_BOOSTING;
            }
            if (car.healthFraction() < 0.6F) {
                flags |= ModNetworking.Hud.FLAG_DAMAGED;
            }
        }
        if (entry != null && entry.finished) {
            flags |= ModNetworking.Hud.FLAG_FINISHED;
        }
        int position = entry != null ? entry.position : 0;
        int lap = entry != null ? Math.min(entry.lap, format.laps) : 0;
        return new ModNetworking.Hud(state.ordinal(), Math.max(0, countdown / 20 + 1),
                lap, format.laps, position, entries.size(),
                (int) (entry != null ? entry.totalMs : 0),
                (int) (entry != null && entry.lapStartMs > 0
                        ? System.currentTimeMillis() - entry.lapStartMs : 0),
                (int) (entry != null ? entry.bestLapMs : 0),
                (int) Math.round(Math.abs(car != null ? car.getSpeed() : 0.0) * 3.6),
                car != null ? car.getBoostCharge() : 0.0,
                (int) (car != null ? car.getDriftScore() : 0),
                0, 0, flags,
                placement.track.name,
                bannerTicks > 0 ? banner : "",
                standingsText(),
                car != null ? car.carType().displayName : "");
    }

    /** Freeroam HUD for a player who is driving but not racing. */
    public static ModNetworking.Hud freeRoamHud(ServerPlayerEntity player, CarEntity car) {
        int flags = 0;
        if (car.isDrifting()) {
            flags |= ModNetworking.Hud.FLAG_DRIFTING;
        }
        if (car.isBoosting()) {
            flags |= ModNetworking.Hud.FLAG_BOOSTING;
        }
        if (car.healthFraction() < 0.6F) {
            flags |= ModNetworking.Hud.FLAG_DAMAGED;
        }
        if (car.isBroken()) {
            flags |= ModNetworking.Hud.FLAG_DAMAGED;
        }
        return new ModNetworking.Hud(-1, 0, 0, 0, 0, 0, 0, 0, 0,
                (int) Math.round(Math.abs(car.getSpeed()) * 3.6), car.getBoostCharge(),
                (int) car.getDriftScore(), 0, 0, flags, "",
                "", "", car.carType().displayName);
    }

    public String describe() {
        return placement.track.name + " | " + format.describe()
                + " | " + entries.size() + " cars | state " + state
                + " | age " + Interp.seconds((System.currentTimeMillis() - createdMs));
    }
}
