package dev.qynl.racers.race;

import java.util.UUID;

import net.minecraft.server.network.ServerPlayerEntity;

import dev.qynl.racers.car.CarEntity;

/** One car in a race: a player entry, an AI entry, or both (AI takeover after finishing). */
public final class RaceEntry {
    public final CarEntity car;
    public final UUID playerId;
    public final String name;
    public final boolean ai;
    public double skill;

    public int lap = 1;
    public int nextCheckpoint;
    public long lapStartMs;
    public long bestLapMs;
    public long lastLapMs;
    public long totalMs;
    public long finishMs = -1L;
    public boolean finished;
    public int finishPosition;
    public int position;
    public double progress;
    public boolean aiTakeover;
    /** Place in the live standings as of the previous check, used to count overtakes. */
    public int lastPosition;
    public int offTrackTicks;
    public int pitStops;
    public int overtakes;

    public RaceEntry(CarEntity car, UUID playerId, String name, boolean ai, double skill) {
        this.car = car;
        this.playerId = playerId;
        this.name = name;
        this.ai = ai;
        this.skill = skill;
        this.lapStartMs = System.currentTimeMillis();
    }

    public boolean isPlayerEntry() {
        return playerId != null;
    }

    public ServerPlayerEntity player() {
        if (playerId == null || car.getServer() == null) {
            return null;
        }
        return car.getServer().getPlayerManager().getPlayer(playerId);
    }

    public String label() {
        return name + (ai ? " (AI)" : "");
    }

    public String timeLabel() {
        if (finishMs >= 0) {
            return dev.qynl.racers.util.Stopwatch.format(finishMs);
        }
        return "L" + Math.min(lap, 99);
    }
}
