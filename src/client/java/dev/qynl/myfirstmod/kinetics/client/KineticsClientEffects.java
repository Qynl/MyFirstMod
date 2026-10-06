package dev.qynl.myfirstmod.kinetics.client;

/**
 * Local, purely cosmetic effects for Null Kinetics: the dash FOV punch and
 * HUD flash timers. Everything here is client-only and prediction-safe.
 */
public final class KineticsClientEffects {
    private KineticsClientEffects() {}

    private static double fovKick = 0.0;

    /** Degrees of extra FOV contributed to {@code GameRenderer#getFov}. */
    public static double fovKick() {
        return fovKick * 6.5;
    }

    /** Call with strength 1.0 for a dash, ~0.6 for a grapple pull. */
    public static void kick(double strength) {
        fovKick = Math.min(1.4, fovKick + strength);
    }

    /** Decay — invoked once per client tick from the controller. */
    public static void tick() {
        fovKick *= 0.80;
        if (fovKick < 0.01) {
            fovKick = 0.0;
        }
    }
}
