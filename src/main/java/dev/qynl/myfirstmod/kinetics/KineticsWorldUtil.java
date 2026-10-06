package dev.qynl.myfirstmod.kinetics;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Shared geometry helpers for the Null Kinetics movement suite.
 *
 * <p>These run on both sides: the client uses them to predict motion and the
 * server uses them to validate and apply the authoritative motion.
 */
public final class KineticsWorldUtil {
    /** How close (in blocks) the player must hug a wall to interact with it. */
    public static final double WALL_PROXIMITY = 0.22;

    private KineticsWorldUtil() {}

    /**
     * Finds the direction from the player toward the nearest wall they are
     * pressing against, or {@code null} if no wall is in reach. Both the foot
     * and upper-body block layers are checked so walls that only reach the
     * torso still count.
     *
     * @return a horizontal direction pointing from the player <b>toward</b> the wall
     */
    public static Direction findWallDirection(Entity player) {
        World world = player.getWorld();
        Box playerBox = player.getBoundingBox();
        BlockPos base = player.getBlockPos();
        Direction best = null;
        double bestGap = Double.MAX_VALUE;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            double gap = Double.MAX_VALUE;
            for (int layer = 0; layer <= 1; layer++) {
                BlockPos pos = base.up(layer).offset(dir);
                if (world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                    continue;
                }
                Box wallBox = new Box(pos);
                // Expand the player box slightly along the wall axis and check overlap.
                Box reach = playerBox.expand(
                        Math.abs(dir.getOffsetX()) * WALL_PROXIMITY,
                        0.0,
                        Math.abs(dir.getOffsetZ()) * WALL_PROXIMITY);
                if (!reach.intersects(wallBox)) {
                    continue;
                }
                double candidate = switch (dir) {
                    case NORTH -> playerBox.minZ - wallBox.maxZ;
                    case SOUTH -> wallBox.minZ - playerBox.maxZ;
                    case WEST -> playerBox.minX - wallBox.maxX;
                    case EAST -> wallBox.minX - playerBox.maxX;
                    default -> Double.MAX_VALUE;
                };
                gap = Math.min(gap, candidate);
            }
            if (gap >= -WALL_PROXIMITY && gap < bestGap) {
                bestGap = gap;
                best = dir;
            }
        }
        return best;
    }

    /** Unit vector pointing away from the given wall direction (toward the player). */
    public static Vec3d wallNormal(Direction wallDir) {
        return new Vec3d(-wallDir.getOffsetX(), 0.0, -wallDir.getOffsetZ());
    }

    /** True while the player is in a state where kinetic abilities are legal. */
    public static boolean canUseKinetics(Entity player) {
        if (player.isSpectator() || player.hasVehicle()) return false;
        if (player.isSubmergedInWater() || player.isInLava()) return false;
        if (!(player instanceof net.minecraft.entity.LivingEntity living)) return false;
        if (living.isFallFlying() || living.isClimbing() || living.isSleeping()) return false;
        return true;
    }
}
