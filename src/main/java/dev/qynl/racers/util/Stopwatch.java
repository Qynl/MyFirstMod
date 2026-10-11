package dev.qynl.racers.util;

import java.util.ArrayList;
import java.util.List;

import dev.qynl.racers.car.CarEntity;

/**
 * Lap timer. The same class is used by the stopwatch item (client side, one
 * instance) and by the race engine (one instance per {@code Track}).
 */
public final class Stopwatch {
    private final List<Long> laps = new ArrayList<>();
    private long startedAt = -1L;
    private long stoppedAt = -1L;

    public void start(long now) {
        startedAt = now;
        stoppedAt = -1L;
        laps.clear();
    }

    public void stop(long now) {
        if (startedAt >= 0) {
            stoppedAt = now;
        }
    }

    public void reset() {
        startedAt = -1L;
        stoppedAt = -1L;
        laps.clear();
    }

    public boolean running() {
        return startedAt >= 0 && stoppedAt < 0;
    }

    public long elapsed(long now) {
        if (startedAt < 0) {
            return 0L;
        }
        return (stoppedAt < 0 ? now : stoppedAt) - startedAt;
    }

    public long split(long now) {
        if (!running()) {
            return 0L;
        }
        long lap = elapsed(now);
        long previous = laps.isEmpty() ? 0L : laps.get(laps.size() - 1);
        laps.add(lap);
        return lap - previous;
    }

    public List<Long> laps() {
        return laps;
    }

    public long best() {
        long best = Long.MAX_VALUE;
        long previous = 0L;
        for (long lap : laps) {
            best = Math.min(best, lap - previous);
            previous = lap;
        }
        return best == Long.MAX_VALUE ? 0L : best;
    }

    private static final Stopwatch CLIENT = new Stopwatch();

    /** The client side singleton used by the stopwatch item + HUD. */
    public static Stopwatch items() {
        return CLIENT;
    }

    public static String format(long millis) {
        if (millis <= 0) {
            return "--:--.---";
        }
        long minutes = millis / 60000L;
        long seconds = (millis % 60000L) / 1000L;
        long ms = millis % 1000L;
        return String.format("%d:%02d.%03d", minutes, seconds, ms);
    }

    public static String formatShort(long millis) {
        if (millis <= 0) {
            return "-.---";
        }
        return String.format("%d.%03d", millis / 1000L, millis % 1000L);
    }

    /** Used by the stopwatch item: first press starts, second press stops, third resets. */
    public void toggle() {
        long now = System.currentTimeMillis();
        if (running()) {
            stop(now);
        } else if (startedAt < 0) {
            start(now);
        } else {
            reset();
        }
    }

    /** The live split while running, without recording a lap. */
    public long currentLap(long now) {
        if (!running()) {
            return 0L;
        }
        long previous = laps.isEmpty() ? 0L : laps.get(laps.size() - 1);
        return elapsed(now) - previous;
    }

    public static String trackLabel(CarEntity car) {
        return car == null ? "-" : car.data().type.displayName;
    }
}
