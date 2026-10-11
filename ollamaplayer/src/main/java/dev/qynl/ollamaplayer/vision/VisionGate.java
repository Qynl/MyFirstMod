package dev.qynl.ollamaplayer.vision;

import java.util.Set;

/**
 * Decides when a picture may be sent to the model. Only event-driven triggers qualify, the feature
 * must be switched on, and pictures are spaced by a minimum gap. Nothing is sent continuously.
 */
public final class VisionGate {
    public static final Set<String> TRIGGERS = Set.of("NEW_AREA");

    private final long minGapNanos;
    private long last = Long.MIN_VALUE / 2;

    public VisionGate(long minGapNanos) {
        this.minGapNanos = minGapNanos;
    }

    public boolean allow(String trigger, boolean enabled, long now) {
        if (!enabled || !TRIGGERS.contains(trigger)) return false;
        if (now - last < minGapNanos) return false;
        last = now;
        return true;
    }
}
