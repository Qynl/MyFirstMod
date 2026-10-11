package dev.qynl.ollamaplayer.companion;

/**
 * Level 0 reactions: instant, deterministic, no model involved. These are the cases where thinking
 * first would be too slow, such as burning or running out of air.
 */
public final class Reflex {
    private Reflex() {}

    public enum Action { NONE, LEAVE_FIRE, SURFACE }

    /** Air below this (ticks of breath left) means Nova must surface now. Vanilla max is 300. */
    public static final int LOW_AIR = 90;

    public static Action decide(boolean onFire, boolean inWater, int air) {
        if (inWater && air < LOW_AIR) return Action.SURFACE;
        if (onFire && !inWater) return Action.LEAVE_FIRE;
        return Action.NONE;
    }
}
