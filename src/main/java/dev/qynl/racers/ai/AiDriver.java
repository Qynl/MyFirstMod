package dev.qynl.racers.ai;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.qynl.racers.car.CarControls;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.CarStats;
import dev.qynl.racers.race.RaceEntry;
import dev.qynl.racers.race.RaceSession;
import dev.qynl.racers.track.TrackPlacement;

/**
 * The AI driver. One instance per car, stateless between ticks except for its
 * small memory of where it is on the line, how long it has been stuck and
 * whether it is currently in the middle of a mistake.
 *
 * <p>Skill (0..1) controls three things: how far ahead it looks, how close to
 * the grip limit it dares to corner, and how often it makes a mistake. Rubber
 * banding is optional and per race.
 */
public class AiDriver {
    private final Random random = new Random();
    private int lineIndex;
    private int lastLineIndex;
    private int stuckTicks;
    private int reverseTicks;
    private int mistakeTicks;
    private double mistakeSteer;
    private int mistakeCooldown = 60;
    private int boostHold;
    private int overtakeTicks;
    private double overtakeOffset;
    private Vec3d roamTarget;
    private int roamTicks;
    private int targetRefresh;

    public CarControls tick(CarEntity car) {
        CarControls controls = new CarControls();
        if (car.isRemoved() || car.isBroken()) {
            controls.handbrake = true;
            return controls;
        }
        RaceSession session = sessionFor(car);
        TrackPlacement placement = session == null ? null : session.placement();
        if (placement != null && placement.line() != null) {
            raceLine(car, session, placement, controls);
        } else {
            roam(car, controls);
        }
        return controls;
    }

    private RaceSession sessionFor(CarEntity car) {
        if (car.getWorld().isClient()) {
            return null;
        }
        var manager = car.raceManager();
        return manager == null ? null : manager.sessionFor(car);
    }

    // ------------------------------------------------------------------
    // racing
    // ------------------------------------------------------------------
    private void raceLine(CarEntity car, RaceSession session, TrackPlacement placement,
                          CarControls controls) {
        RacingLine line = placement.line();
        double skill = car.aiSkill();
        double speed = Math.abs(car.getSpeed());
        int nearest = line.nearestIndex(car.getX(), car.getZ(), lineIndex);
        if (nearest != lastLineIndex) {
            lineIndex = nearest;
            lastLineIndex = nearest;
        }

        // --- how far ahead to look --------------------------------------
        double look = 7.0 + speed * (0.45 + 0.25 * skill);
        if (mistakeTicks > 0) {
            look *= 0.6;
        }
        int targetIndex = line.indexAhead(lineIndex, look);
        double lateral = 0.0;

        // --- overtaking: pick a side when someone is in the way ----------
        RaceEntry me = session == null ? null : session.entryFor(car);
        if (overtakeTicks > 0) {
            overtakeTicks--;
            lateral = overtakeOffset;
        } else {
            CarEntity ahead = carAhead(car, 14.0);
            if (ahead != null && Math.abs(car.getSpeed()) > 3.0) {
                double side = chooseSide(car, line, ahead);
                if (side != 0.0) {
                    overtakeOffset = side * Math.min(2.6, line.halfWidth(lineIndex) * 0.55);
                    overtakeTicks = 40 + random.nextInt(30);
                    lateral = overtakeOffset;
                }
            }
        }
        if (lateral != 0.0) {
            targetIndex = line.offsetIndex(targetIndex, lateral);
        }
        Vec3d target = line.pointAt(targetIndex);

        // --- steering ----------------------------------------------------
        double desiredYaw = RacingLine.angleTo(car.getX(), car.getZ(), target.x, target.z);
        double error = RacingLine.yawError(desiredYaw, car.getYaw());
        double steerGain = 0.055 + 0.035 * (1.0 - skill);
        double steer = MathHelper.clamp(error * steerGain, -1.0, 1.0);
        if (error > 60.0) {
            steer = Math.signum(error);
        }
        if (mistakeTicks > 0) {
            steer = MathHelper.clamp(steer + mistakeSteer, -1.0, 1.0);
        }
        controls.analogSteer = steer;

        // --- targets ------------------------------------------------------
        CarStats stats = car.stats();
        double brakeDistance = 6.0 + speed * speed / Math.max(3.0, stats.brakeForce() * 0.85);
        double curvature = Math.max(line.maxCurvatureAhead(lineIndex, brakeDistance), 1e-4);
        double maxLateral = stats.grip() * (0.72 + 0.26 * skill);
        double cornerSpeed = Math.sqrt(maxLateral / curvature);
        double targetSpeed = Math.min(stats.topSpeed() * (0.86 + 0.14 * skill), cornerSpeed);

        // do not ask for more than the road can give
        targetSpeed = Math.max(4.0, targetSpeed);

        // rubber banding keeps the pack close and the races fun
        if (me != null && session != null && car.aiRubberBand()) {
            targetSpeed *= session.rubberBand(car, me);
        }
        if (mistakeTicks > 0) {
            targetSpeed *= 0.72;
        }

        // --- throttle / brake --------------------------------------------
        if (speed < targetSpeed * 0.97) {
            controls.analogThrottle = Math.min(1.0, 0.55 + 0.45 * skill);
            controls.forward = true;
        } else if (speed > targetSpeed * 1.05) {
            controls.analogThrottle = 0.0;
            controls.back = true;
        } else {
            controls.analogThrottle = 0.35;
        }
        // trail braking: ease off when the nose is already turning hard
        if (Math.abs(steer) > 0.75 && speed > targetSpeed * 0.9) {
            controls.analogThrottle = 0.0;
            controls.back = true;
        }
        // handbrake drifts when the corner is tight and the driver is good
        if (skill > 0.62 && curvature > 0.055 && speed > 8.0 && random.nextInt(40) == 0) {
            controls.handbrake = true;
        }
        // boost on the straights
        if (boostHold > 0) {
            boostHold--;
            controls.boost = true;
        } else if (curvature < 0.012 && car.getBoostCharge() > 0.55 && speed > stats.topSpeed() * 0.5
                && random.nextInt(35) == 0) {
            boostHold = 25 + random.nextInt(25);
        }

        // --- avoidance ----------------------------------------------------
        if (aheadBlocked(car, 5.0) || carAhead(car, 4.5) != null) {
            controls.forward = false;
            controls.analogThrottle = 0.0;
            controls.back = speed > 6.0;
        }

        // --- mistakes -----------------------------------------------------
        if (mistakeCooldown > 0) {
            mistakeCooldown--;
        } else if (mistakeTicks <= 0) {
            double chance = (1.0 - skill) * 0.09 + 0.004;
            if (random.nextDouble() < chance) {
                mistakeTicks = 12 + random.nextInt(22);
                mistakeSteer = (random.nextDouble() - 0.5) * 0.65;
                mistakeCooldown = 60 + random.nextInt(220);
            }
        }
        if (mistakeTicks > 0) {
            mistakeTicks--;
        }

        // --- stuck / recovery ---------------------------------------------
        if (car.stuckTicks() > 26) {
            reverseTicks = 22 + random.nextInt(20);
        }
        if (reverseTicks > 0) {
            reverseTicks--;
            controls.forward = false;
            controls.analogThrottle = 0.0;
            controls.back = true;
            controls.analogSteer = -steer;
        }
    }

    /** Finds a car close ahead in the driving direction. */
    private CarEntity carAhead(CarEntity car, double range) {
        Vec3d forward = Vec3d.fromPolar(0.0F, car.getYaw());
        List<CarEntity> cars = car.getWorld().getEntitiesByClass(CarEntity.class,
                car.getBoundingBox().expand(range, 3.0, range), other -> other != car);
        CarEntity closest = null;
        double best = range * range;
        for (CarEntity other : cars) {
            Vec3d delta = other.getPos().subtract(car.getPos());
            double along = delta.dotProduct(forward);
            if (along < 0.5 || along > range) {
                continue;
            }
            double lateral = Math.abs(delta.x * -forward.z + delta.z * forward.x);
            if (lateral > 2.6) {
                continue;
            }
            double d = delta.horizontalLengthSquared();
            if (d < best) {
                best = d;
                closest = other;
            }
        }
        return closest;
    }

    private double chooseSide(CarEntity car, RacingLine line, CarEntity ahead) {
        int index = line.nearestIndex(car.getX(), car.getZ(), lineIndex);
        double half = line.halfWidth(index);
        if (half < 2.2) {
            return 0.0;
        }
        double lateral = ahead.getX() * 0.0;
        Vec3d forward = Vec3d.fromPolar(0.0F, car.getYaw());
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x);
        Vec3d delta = ahead.getPos().subtract(car.getPos());
        double offset = delta.dotProduct(right);
        // go around the side the other car is NOT on
        return offset > 0.0 ? -1.0 : 1.0;
    }

    /** Cheap forward probe: is there a wall right in front of us? */
    private boolean aheadBlocked(CarEntity car, double distance) {
        World world = car.getWorld();
        Vec3d forward = Vec3d.fromPolar(0.0F, car.getYaw());
        for (double d = 1.6; d <= distance; d += 1.2) {
            BlockPos pos = BlockPos.ofFloored(car.getX() + forward.x * d, car.getY() + 0.3,
                    car.getZ() + forward.z * d);
            var state = world.getBlockState(pos);
            if (!state.isAir() && state.isSolidBlock(world, pos)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // roaming: drive around when there is no track at all
    // ------------------------------------------------------------------
    private void roam(CarEntity car, CarControls controls) {
        World world = car.getWorld();
        if (roamTarget == null || roamTicks-- <= 0
                || car.getPos().distanceTo(roamTarget) < 6.0) {
            pickRoamTarget(car, world);
        }
        if (roamTarget == null) {
            controls.handbrake = true;
            return;
        }
        double desiredYaw = RacingLine.angleTo(car.getX(), car.getZ(), roamTarget.x, roamTarget.z);
        double error = RacingLine.yawError(desiredYaw, car.getYaw());
        controls.analogSteer = MathHelper.clamp(error * 0.05, -1.0, 1.0);

        boolean blocked = aheadBlocked(car, 7.0);
        if (blocked) {
            controls.analogSteer = car.getYaw() > 0 ? -1.0 : 1.0;
            if (Math.abs(car.getSpeed()) < 5.0) {
                // try a bit of a shuffle to get free
                controls.forward = random.nextBoolean();
                controls.back = !controls.forward;
            }
            if (random.nextInt(20) == 0) {
                pickRoamTarget(car, world);
            }
            return;
        }
        double limit = car.stats().topSpeed() * 0.55;
        double speed = Math.abs(car.getSpeed());
        if (speed < limit) {
            controls.forward = true;
            controls.analogThrottle = 0.7;
        }
        if (Math.abs(error) > 45.0 && speed > 12.0) {
            controls.back = true;
            controls.forward = false;
        }
        if (car.stuckTicks() > 30) {
            reverseTicks = 25;
        }
        if (reverseTicks > 0) {
            reverseTicks--;
            controls.back = true;
            controls.forward = false;
            controls.analogSteer = -controls.analogSteer;
        }
    }

    private void pickRoamTarget(CarEntity car, World world) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = 40.0 + random.nextDouble() * 60.0;
            double x = car.getX() + Math.cos(angle) * distance;
            double z = car.getZ() + Math.sin(angle) * distance;
            BlockPos pos = BlockPos.ofFloored(x, car.getY(), z);
            var ground = world.getBlockState(pos.down());
            if (ground.isAir()) {
                for (int y = 0; y < 12; y++) {
                    if (!world.getBlockState(pos.down(y)).isAir()) {
                        pos = pos.down(y);
                        break;
                    }
                }
            }
            if (!world.getBlockState(pos.down()).isAir()
                    && world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir()) {
                roamTarget = new Vec3d(x, pos.getY(), z);
                roamTicks = 260 + random.nextInt(200);
                return;
            }
        }
        roamTicks = 120;
        roamTarget = null;
    }

    /** Used by the renderer to draw the AI thinking about its line. */
    public int lineIndex() {
        return lineIndex;
    }
}
