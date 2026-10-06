package dev.qynl.myfirstmod.kinetics;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Deterministic movement math shared by the client prediction and the server
 * authority. Every ability funnels through here so both sides compute
 * byte-identical velocities and the player never rubber-bands.
 */
public final class KineticMath {
    private KineticMath() {}

    // ------------------------------------------------------------------
    // Void Dash
    // ------------------------------------------------------------------

    /**
     * Dashes along the look direction. When the player aims near the horizon
     * while grounded the dash is flattened for a grounded, skate-like burst.
     */
    public static Vec3d dashVelocity(Entity player, KineticsConfig cfg) {
        Vec3d look = player.getRotationVector();
        Vec3d current = player.getVelocity();
        Vec3d dir = look;

        // Flatten near-horizontal dashes so ground dashing feels punchy.
        if (Math.abs(look.y) < 0.35 && player.isOnGround()) {
            dir = new Vec3d(look.x, 0.0, look.z);
            if (dir.lengthSquared() < 1.0E-4) {
                dir = new Vec3d(current.x, 0.0, current.z);
            }
        }
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vec3d(0, 1, 0);
        }
        dir = dir.normalize();

        return new Vec3d(
                dir.x * cfg.dashSpeed + current.x * 0.2,
                dir.y * cfg.dashSpeed + current.y * cfg.dashVerticalDamp,
                dir.z * cfg.dashSpeed + current.z * 0.2);
    }

    // ------------------------------------------------------------------
    // Null Step (air jump)
    // ------------------------------------------------------------------

    public static Vec3d airJumpVelocity(Entity player, KineticsConfig cfg) {
        Vec3d current = player.getVelocity();
        Vec3d look = player.getRotationVector();
        Vec3d horizLook = new Vec3d(look.x, 0, look.z);
        if (horizLook.lengthSquared() < 1.0E-4) {
            horizLook = new Vec3d(current.x, 0, current.z);
        }
        if (horizLook.lengthSquared() < 1.0E-4) {
            horizLook = new Vec3d(0, 0, 1);
        }
        horizLook = horizLook.normalize();

        return new Vec3d(
                current.x + horizLook.x * cfg.airJumpHorizontalBoost,
                cfg.airJumpVelocity,
                current.z + horizLook.z * cfg.airJumpHorizontalBoost);
    }

    // ------------------------------------------------------------------
    // Wall jump
    // ------------------------------------------------------------------

    /**
     * @param wallDir horizontal direction from the player toward the wall
     */
    public static Vec3d wallJumpVelocity(Entity player, Direction wallDir, KineticsConfig cfg) {
        Vec3d current = player.getVelocity();
        Vec3d normal = KineticsWorldUtil.wallNormal(wallDir);

        // Keep some of the momentum that runs along the wall.
        Vec3d along = new Vec3d(current.x, 0, current.z);
        along = along.subtract(normal.multiply(along.dotProduct(normal)));
        if (along.lengthSquared() < 1.0E-4) {
            along = new Vec3d(-normal.z, 0, normal.x);
        }
        along = along.normalize();

        return new Vec3d(
                normal.x * cfg.wallJumpOut + along.x * MathHelper.clamp(
                        Math.sqrt(current.x * current.x + current.z * current.z), 0, 0.6) * cfg.wallJumpKeepMomentum,
                cfg.wallJumpUp,
                normal.z * cfg.wallJumpOut + along.z * MathHelper.clamp(
                        Math.sqrt(current.x * current.x + current.z * current.z), 0, 0.6) * cfg.wallJumpKeepMomentum);
    }

    // ------------------------------------------------------------------
    // Sculk Stride (wall run) — applied every tick while active
    // ------------------------------------------------------------------

    /**
     * @param wallDir horizontal direction from the player toward the wall
     * @return the velocity the player should have this tick
     */
    public static Vec3d wallRunTick(Entity player, Direction wallDir, KineticsConfig cfg) {
        Vec3d current = player.getVelocity();
        Vec3d normal = KineticsWorldUtil.wallNormal(wallDir);

        // Project current motion onto the wall plane to find the run direction.
        Vec3d along = new Vec3d(current.x, 0, current.z);
        along = along.subtract(normal.multiply(along.dotProduct(normal)));
        if (along.lengthSquared() < 1.0E-4) {
            Vec3d look = player.getRotationVector();
            along = new Vec3d(look.x, 0, look.z);
            along = along.subtract(normal.multiply(along.dotProduct(normal)));
        }
        if (along.lengthSquared() < 1.0E-4) {
            along = new Vec3d(-normal.z, 0, normal.x);
        }
        along = along.normalize();

        double fall = Math.max(current.y - cfg.wallRunGravity, -0.12);
        // Slight pull into the wall keeps the player glued around corners.
        return new Vec3d(
                along.x * cfg.wallRunSpeed - normal.x * 0.02,
                fall,
                along.z * cfg.wallRunSpeed - normal.z * 0.02);
    }

    // ------------------------------------------------------------------
    // Null Drift (glide) — applied every tick while active
    // ------------------------------------------------------------------

    public static Vec3d glideTick(Entity player, KineticsConfig cfg) {
        Vec3d current = player.getVelocity();
        Vec3d look = player.getRotationVector();
        Vec3d horizLook = new Vec3d(look.x, 0, look.z);
        if (horizLook.lengthSquared() > 1.0E-4) {
            horizLook = horizLook.normalize();
        } else {
            horizLook = Vec3d.ZERO;
        }

        double x = current.x + horizLook.x * cfg.glideForwardAccel;
        double z = current.z + horizLook.z * cfg.glideForwardAccel;

        // Soft cap on horizontal glide speed.
        double horizLen = Math.sqrt(x * x + z * z);
        if (horizLen > cfg.glideMaxHorizontalSpeed) {
            double scale = cfg.glideMaxHorizontalSpeed / horizLen;
            x *= scale;
            z *= scale;
        }

        return new Vec3d(x, Math.max(current.y, -cfg.glideFallSpeed), z);
    }

    // ------------------------------------------------------------------
    // Phase Slide
    // ------------------------------------------------------------------

    /** Direction the slide should carry the player, or null if not moving. */
    public static Vec3d slideDirection(Entity player) {
        Vec3d current = player.getVelocity();
        Vec3d horiz = new Vec3d(current.x, 0, current.z);
        if (horiz.lengthSquared() > 1.0E-4) {
            return horiz.normalize();
        }
        Vec3d look = player.getRotationVector();
        Vec3d flat = new Vec3d(look.x, 0, look.z);
        return flat.lengthSquared() > 1.0E-4 ? flat.normalize() : null;
    }

    /**
     * Counteracts vanilla ground friction so the slide keeps its momentum.
     * Vanilla multiplies horizontal velocity by {@code 0.91 * slipperiness}
     * each tick; we cancel that and apply our own gentler decay.
     */
    public static Vec3d slideFrictionTick(Entity player, Vec3d velocity, double slipperiness, KineticsConfig cfg) {
        double vanillaFriction = 0.91 * slipperiness;
        if (vanillaFriction <= 0.0 || vanillaFriction >= 1.0) {
            return velocity;
        }
        double restore = cfg.slideFriction / vanillaFriction;
        return new Vec3d(velocity.x * restore, velocity.y, velocity.z * restore);
    }

    // ------------------------------------------------------------------
    // Nullhook pull
    // ------------------------------------------------------------------

    public static Vec3d grapplePull(Vec3d from, Vec3d anchor, KineticsConfig cfg) {
        Vec3d toAnchor = anchor.subtract(from);
        double dist = toAnchor.length();
        if (dist < 1.0E-4) {
            return Vec3d.ZERO;
        }
        Vec3d dir = toAnchor.multiply(1.0 / dist);
        // Accelerate slightly as the player closes in, for a snappy finish.
        double speed = cfg.grapplePullSpeed + Math.max(0.0, (12.0 - dist)) * 0.02;
        return new Vec3d(dir.x * speed, dir.y * speed - 0.02, dir.z * speed);
    }
}
