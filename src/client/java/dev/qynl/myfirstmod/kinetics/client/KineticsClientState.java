package dev.qynl.myfirstmod.kinetics.client;

import dev.qynl.myfirstmod.kinetics.KineticsNetworking;

/**
 * Client-side mirror of the authoritative kinetics state, kept fresh by the
 * {@code KineticsStatePayload} sync. The controller mixes in locally
 * predicted values (cooldowns, window timers) for instant HUD feedback.
 */
public final class KineticsClientState {
    /** Singleton handle so call sites can use a short local alias. */
    public static final KineticsClientState INSTANCE = new KineticsClientState();

    private KineticsClientState() {}

    // ---- server mirror ------------------------------------------------
    public static volatile float energy = 100.0F;
    public static volatile float maxEnergy = 100.0F;
    public static volatile int airJumps = 0;
    public static volatile int dashCooldown = 0;
    public static volatile int flowStacks = 0;
    public static volatile boolean wallRunning = false;
    public static volatile boolean gliding = false;
    public static volatile boolean sliding = false;
    public static volatile boolean grappling = false;
    public static volatile int grappleEntityId = -1;
    public static volatile int abilityMask = 0xFF;

    // ---- local prediction ---------------------------------------------
    public static int localDashCooldown = 0;
    public static int localSlideCooldown = 0;
    public static int localAirJumps = 0;
    public static int wallRunTicksLeft = 0;
    public static int glideTicksHeld = 0;
    public static int dashFlashTicks = 0;
    public static int flowPulseTicks = 0;

    public static boolean hudVisible = true;

    // ------------------------------------------------------------------

    public static void apply(KineticsNetworking.KineticsStatePayload payload) {
        energy = payload.energy();
        maxEnergy = payload.maxEnergy();
        airJumps = payload.airJumps();
        dashCooldown = payload.dashCooldown();
        if (payload.flowStacks() > flowStacks) {
            flowPulseTicks = 20;
        }
        flowStacks = payload.flowStacks();
        wallRunning = (payload.flags() & KineticsNetworking.FLAG_WALL_RUNNING) != 0;
        gliding = (payload.flags() & KineticsNetworking.FLAG_GLIDING) != 0;
        sliding = (payload.flags() & KineticsNetworking.FLAG_SLIDING) != 0;
        grappling = (payload.flags() & KineticsNetworking.FLAG_GRAPPLING) != 0;
        grappleEntityId = payload.grappleEntityId();
        abilityMask = payload.abilityMask();
    }

    public static boolean abilityEnabled(int bit) {
        return (abilityMask & bit) != 0;
    }

    public static int dashCooldownDisplay() {
        return Math.max(dashCooldown, localDashCooldown);
    }

    public static int slideCooldownDisplay() {
        return localSlideCooldown;
    }

    public static void reset() {
        energy = 100.0F;
        maxEnergy = 100.0F;
        airJumps = 0;
        dashCooldown = 0;
        flowStacks = 0;
        wallRunning = false;
        gliding = false;
        sliding = false;
        grappling = false;
        grappleEntityId = -1;
        localDashCooldown = 0;
        localSlideCooldown = 0;
        localAirJumps = 0;
        wallRunTicksLeft = 0;
        glideTicksHeld = 0;
        dashFlashTicks = 0;
    }
}
