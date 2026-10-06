package dev.qynl.myfirstmod.kinetics;

/**
 * Per-player, server-side tracking for the Null Kinetics suite.
 * All fields are authoritative; a summary is mirrored to the client for HUD.
 */
public final class KineticsState {
    // Energy
    public double energy = 100.0;
    public int regenDelayTicks = 0;

    // Null Step
    public int airJumpsLeft = 0;
    public int airJumpCooldown = 0;

    // Void Dash
    public int dashCooldown = 0;

    // Active windows (world-tick expiry, -1 = inactive)
    public long wallRunUntil = -1;
    public long glideUntil = -1;
    public long slideUntil = -1;
    public int slideCooldown = 0;

    // Flow
    public int flowStacks = 0;
    public int flowTimer = 0;
    public boolean flowModifierApplied = false;

    // Fall damage grace
    public long fallGraceUntil = -1;

    // Grapple
    public int grappleEntityId = -1;

    // Advancement counters (persist only for the session, cheap and harmless)
    public int airborneJumpsChain = 0;
    public int wallJumpsChain = 0;
    public int totalGlideTicks = 0;
    public boolean grappleAdvancementDone = false;

    // Networking
    public boolean dirty = true;
    public long lastSyncTick = -1;

    /** Marks the state as changed so the manager pushes a fresh sync. */
    public void markDirty() {
        this.dirty = true;
    }

    public boolean isWallRunning(long tick) {
        return wallRunUntil >= tick;
    }

    public boolean isGliding(long tick) {
        return glideUntil >= tick;
    }

    public boolean isSliding(long tick) {
        return slideUntil >= tick;
    }

    public boolean hasFallGrace(long tick) {
        return fallGraceUntil >= tick;
    }
}
