package dev.qynl.racers.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import dev.qynl.racers.car.CarEntity;

/** Small world queries the car code needs in a lot of places. */
public final class Cars {
    private Cars() {
    }

    /** Height of the first solid-ish surface under {@code pos}, or NaN if there is none nearby. */
    public static double groundHeight(World world, Vec3d pos) {
        BlockPos.Mutable mutable = BlockPos.ofFloored(pos.x, pos.y, pos.z).mutableCopy();
        int start = (int) Math.floor(pos.y) + 2;
        for (int y = start; y > start - 8; y--) {
            mutable.set(pos.x, y, pos.z);
            var state = world.getBlockState(mutable);
            if (!state.isAir() && state.isSolidBlock(world, mutable)) {
                return y + 1.0;
            }
        }
        return Double.NaN;
    }

    /** Snaps a spawn position to the surface so cars never spawn inside the ground. */
    public static double snapCarY(World world, Vec3d pos) {
        double ground = groundHeight(world, pos);
        if (Double.isNaN(ground)) {
            return pos.y;
        }
        return ground;
    }

    public static boolean groundIsSolid(World world, BlockPos pos) {
        BlockPos below = pos.down();
        var state = world.getBlockState(below);
        return !state.isAir() && state.isSolidBlock(world, below);
    }

    /** Finds the closest car to a point within {@code radius} blocks. */
    public static CarEntity carNear(World world, Vec3d pos, double radius) {
        double best = radius * radius;
        CarEntity found = null;
        for (CarEntity car : world.getEntitiesByClass(CarEntity.class,
                Box.of(pos, radius * 2.0, radius * 2.0, radius * 2.0), car -> !car.isRemoved())) {
            double d = car.getPos().squaredDistanceTo(pos);
            if (d < best) {
                best = d;
                found = car;
            }
        }
        return found;
    }

    /** How much room a car needs, used to sanity check the spawn spot. */
    public static Box carBox(Vec3d pos, float width, float length) {
        return new Box(pos.x - width / 2.0, pos.y, pos.z - length / 2.0,
                pos.x + width / 2.0, pos.y + 1.1, pos.z + length / 2.0);
    }
}
