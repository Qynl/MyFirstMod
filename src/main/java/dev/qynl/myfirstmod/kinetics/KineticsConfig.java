package dev.qynl.myfirstmod.kinetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import dev.qynl.myfirstmod.MyFirstMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tunable settings for the Null Kinetics movement suite.
 *
 * <p>The file lives at {@code config/myfirstmod-kinetics.json} and is loaded on
 * both the client and the dedicated server. The server is authoritative for
 * every ability (it applies the real velocity), while the client only uses its
 * local copy to predict the exact same motion, so identical files give the
 * smoothest feel. Values can be hot-reloaded with {@code /kinetics reload}.
 */
public final class KineticsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Master switch for the whole movement suite. */
    public boolean enabled = true;

    // ------------------------------------------------------------------
    // Energy
    // ------------------------------------------------------------------
    public double maxEnergy = 100.0;
    /** Energy regenerated per tick while grounded. */
    public double energyRegenGround = 0.5;
    /** Energy regenerated per tick while airborne. */
    public double energyRegenAir = 0.18;
    /** Ticks after landing before regeneration resumes (lets chains breathe). */
    public int energyRegenDelay = 10;

    // ------------------------------------------------------------------
    // Individual ability toggles
    // ------------------------------------------------------------------
    public boolean dashEnabled = true;
    public boolean doubleJumpEnabled = true;
    public boolean wallRunEnabled = true;
    public boolean glideEnabled = true;
    public boolean slideEnabled = true;
    public boolean grappleEnabled = true;
    public boolean flowEnabled = true;

    // ------------------------------------------------------------------
    // Void Dash
    // ------------------------------------------------------------------
    public double dashSpeed = 1.35;
    public int dashCooldownTicks = 26;
    public double dashEnergy = 22.0;
    /** How much of the current vertical velocity is kept when dashing. */
    public double dashVerticalDamp = 0.35;

    // ------------------------------------------------------------------
    // Null Step (air jump)
    // ------------------------------------------------------------------
    public int airJumps = 2;
    public double airJumpVelocity = 0.62;
    public double airJumpHorizontalBoost = 0.18;
    public double airJumpEnergy = 12.0;
    /** Minimum ticks between two air jumps. */
    public int airJumpCooldownTicks = 6;

    // ------------------------------------------------------------------
    // Sculk Stride (wall run / wall jump)
    // ------------------------------------------------------------------
    public int wallRunMaxTicks = 44;
    public double wallRunSpeed = 0.34;
    /** Gravity applied per tick while wall running (vanilla is ~0.08). */
    public double wallRunGravity = 0.012;
    public double wallRunEnergyPerTick = 0.35;
    public double wallJumpUp = 0.52;
    public double wallJumpOut = 0.44;
    public double wallJumpKeepMomentum = 0.6;
    public double wallJumpEnergy = 8.0;
    /** Horizontal speed needed to start a wall run. */
    public double wallRunMinSpeed = 0.18;

    // ------------------------------------------------------------------
    // Null Drift (glide)
    // ------------------------------------------------------------------
    public double glideFallSpeed = 0.42;
    public double glideForwardAccel = 0.028;
    public double glideMaxHorizontalSpeed = 0.95;
    public double glideEnergyPerTick = 0.30;

    // ------------------------------------------------------------------
    // Phase Slide
    // ------------------------------------------------------------------
    public double slideSpeed = 1.02;
    public int slideTicks = 12;
    public double slideEnergy = 12.0;
    /** Velocity multiplier per tick while sliding (lower = longer slide). */
    public double slideFriction = 0.975;
    public int slideCooldownTicks = 14;

    // ------------------------------------------------------------------
    // Nullhook (grapple)
    // ------------------------------------------------------------------
    public double grappleHookSpeed = 2.8;
    public int grappleMaxRange = 44;
    public int grappleMaxTicks = 150;
    public double grapplePullSpeed = 1.12;
    public double grappleEnergy = 20.0;
    /** Distance at which the hook releases the player. */
    public double grappleReleaseDistance = 1.8;

    // ------------------------------------------------------------------
    // Flow (style multiplier)
    // ------------------------------------------------------------------
    public int flowMaxStacks = 6;
    /** Movement speed bonus per Flow stack. */
    public double flowSpeedPerStack = 0.04;
    /** Energy cost reduction per Flow stack. */
    public double flowCostReductionPerStack = 0.05;
    /** Ticks after the last stylish action before Flow starts decaying. */
    public int flowWindowTicks = 80;

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------
    /** Ticks of fall damage immunity after any kinetic action. */
    public int fallDamageGraceTicks = 30;
    /** When true, abilities require the Heart of the Null in the inventory. */
    public boolean requireRelic = false;
    /** HUD visibility hint (players can also toggle it with a keybind). */
    public boolean hudEnabled = true;

    // ------------------------------------------------------------------

    public static KineticsConfig INSTANCE = new KineticsConfig();

    private KineticsConfig() {}

    public static KineticsConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                KineticsConfig read = GSON.fromJson(Files.readString(path), KineticsConfig.class);
                if (read != null) {
                    read.clamp();
                    INSTANCE = read;
                    return;
                }
            } catch (Exception e) {
                MyFirstMod.LOGGER.error("Failed to read kinetics config, restoring defaults", e);
            }
        }
        INSTANCE = new KineticsConfig();
        INSTANCE.clamp();
        save();
    }

    public static void save() {
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            MyFirstMod.LOGGER.error("Failed to save kinetics config", e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("myfirstmod-kinetics.json");
    }

    /** Guards against absurd values that would break the movement simulation. */
    private void clamp() {
        maxEnergy = clamp(maxEnergy, 20, 1000);
        energyRegenGround = clamp(energyRegenGround, 0, 10);
        energyRegenAir = clamp(energyRegenAir, 0, 10);
        dashSpeed = clamp(dashSpeed, 0.3, 4.0);
        dashCooldownTicks = (int) clamp(dashCooldownTicks, 2, 600);
        airJumps = (int) clamp(airJumps, 0, 8);
        airJumpVelocity = clamp(airJumpVelocity, 0.2, 1.4);
        wallRunMaxTicks = (int) clamp(wallRunMaxTicks, 5, 400);
        wallRunSpeed = clamp(wallRunSpeed, 0.1, 1.0);
        wallJumpUp = clamp(wallJumpUp, 0.1, 1.4);
        wallJumpOut = clamp(wallJumpOut, 0.1, 1.4);
        glideFallSpeed = clamp(glideFallSpeed, 0.1, 3.0);
        glideMaxHorizontalSpeed = clamp(glideMaxHorizontalSpeed, 0.2, 3.0);
        slideSpeed = clamp(slideSpeed, 0.2, 3.0);
        slideTicks = (int) clamp(slideTicks, 3, 100);
        slideFriction = clamp(slideFriction, 0.8, 0.999);
        grappleHookSpeed = clamp(grappleHookSpeed, 0.5, 5.0);
        grappleMaxRange = (int) clamp(grappleMaxRange, 8, 128);
        grapplePullSpeed = clamp(grapplePullSpeed, 0.3, 3.0);
        flowMaxStacks = (int) clamp(flowMaxStacks, 1, 10);
        flowSpeedPerStack = clamp(flowSpeedPerStack, 0, 0.25);
        fallDamageGraceTicks = (int) clamp(fallDamageGraceTicks, 0, 200);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
