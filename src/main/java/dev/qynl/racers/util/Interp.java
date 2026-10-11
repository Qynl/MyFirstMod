package dev.qynl.racers.util;

import net.minecraft.util.math.MathHelper;

/** Tiny math helpers shared by the physics, HUD and AI code. */
public final class Interp {
    private Interp() {
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double lerp(double from, double to, double delta) {
        return from + (to - from) * delta;
    }

    public static float lerp(float from, float to, float delta) {
        return from + (to - from) * delta;
    }

    public static double approach(double current, double target, double step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }

    /** Difference between two Minecraft yaw angles, in -180..180. */
    public static double yawDelta(double from, double to) {
        return MathHelper.wrapDegrees(to - from);
    }

    /** Seconds as a one decimal string. */
    public static String seconds(long millis) {
        return String.format("%.1fs", millis / 1000.0);
    }

    public static int percent(double value) {
        return (int) Math.round(clamp(value, 0.0, 1.0) * 100.0);
    }

    public static double smoothstep(double value) {
        double t = clamp(value, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
