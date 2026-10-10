package dev.qynl.ollamaplayer.companion;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* over walkable block positions for a 1-wide, 2-tall walker (player-sized).
 * Supports: walking (4 + diagonal), stepping up 1, dropping up to 3 blocks.
 * Runs on the server thread with hard expansion and range caps so it can never stall a tick for long.
 */
public final class Pathfinder {
    private static final int MAX_EXPANSIONS = 3500;
    private static final int MAX_RANGE = 36;
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final Set<Block> HAZARDS = Set.of(
            Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.SWEET_BERRY_BUSH);

    /** What counts as the destination, plus an admissible-ish estimate of remaining cost. */
    public record Goal(java.util.function.Predicate<BlockPos> reached, java.util.function.ToDoubleFunction<BlockPos> estimate) {}

    private Pathfinder() {}

    /** Returns the list of feet positions from start to goal (inclusive), or null if none was found. */
    public static List<BlockPos> find(World world, BlockPos start, Goal goal) {
        if (goal.reached().test(start)) return List.of(start);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Long, Node> best = new HashMap<>();
        Set<Long> closed = new HashSet<>();
        Node root = new Node(start, 0, goal.estimate().applyAsDouble(start), null);
        open.add(root);
        best.put(start.asLong(), root);

        int expansions = 0;
        while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
            Node cur = open.poll();
            if (!closed.add(cur.pos.asLong())) continue;
            expansions++;
            if (goal.reached().test(cur.pos)) return reconstruct(cur);

            for (int[] d : DIRS) {
                BlockPos next = step(world, cur.pos, d[0], d[1]);
                if (next == null) continue;
                if (Math.abs(next.getX() - start.getX()) > MAX_RANGE || Math.abs(next.getZ() - start.getZ()) > MAX_RANGE) continue;
                long key = next.asLong();
                if (closed.contains(key)) continue;
                double cost = cur.g + moveCost(cur.pos, next);
                Node old = best.get(key);
                if (old == null || cost < old.g) {
                    Node n = new Node(next, cost, cost + goal.estimate().applyAsDouble(next), cur);
                    best.put(key, n);
                    open.add(n);
                }
            }
        }
        return null;
    }

    private static double moveCost(BlockPos from, BlockPos to) {
        int dy = to.getY() - from.getY();
        double horizontal = (from.getX() != to.getX() && from.getZ() != to.getZ()) ? 1.42 : 1.0;
        if (dy > 0) return horizontal + 1.5;
        if (dy < 0) return horizontal + 0.6 * -dy;
        return horizontal;
    }

    /** One horizontal move from p in direction (dx,dz), including step-up and drop-down options. */
    private static BlockPos step(World world, BlockPos p, int dx, int dz) {
        int x = p.getX() + dx;
        int z = p.getZ() + dz;
        int y = p.getY();
        BlockPos flat = new BlockPos(x, y, z);

        if (dx != 0 && dz != 0) {
            // Don't cut corners through walls.
            if (!passable(world, new BlockPos(x, y, p.getZ())) || !passable(world, new BlockPos(p.getX(), y, z))
                    || !passable(world, new BlockPos(x, y + 1, p.getZ())) || !passable(world, new BlockPos(p.getX(), y + 1, z))) {
                return null;
            }
        }

        if (standable(world, flat)) return flat;

        // Step up one block: need head room above us and the cell above the target floor free.
        BlockPos up = flat.up();
        if (passable(world, p.up(2)) && standable(world, up)) return up;

        // Drop down (up to 3): the way ahead must be open at our height, then fall column is clear.
        if (!passable(world, flat) || !passable(world, flat.up())) return null;
        for (int k = 1; k <= 3; k++) {
            BlockPos c = new BlockPos(x, y - k, z);
            if (!passable(world, c)) return null;
            if (standable(world, c)) return c;
        }
        return null;
    }

    static boolean passable(World world, BlockPos p) {
        BlockState s = world.getBlockState(p);
        if (!s.getFluidState().isEmpty()) return false;
        if (HAZARDS.contains(s.getBlock())) return false;
        return s.getCollisionShape(world, p).isEmpty();
    }

    static boolean solid(World world, BlockPos p) {
        BlockState s = world.getBlockState(p);
        if (!s.getFluidState().isEmpty()) return false;
        if (HAZARDS.contains(s.getBlock())) return false;
        return !s.getCollisionShape(world, p).isEmpty();
    }

    static boolean standable(World world, BlockPos p) {
        return passable(world, p) && passable(world, p.up()) && solid(world, p.down());
    }

    private static List<BlockPos> reconstruct(Node end) {
        List<BlockPos> out = new ArrayList<>();
        for (Node n = end; n != null; n = n.parent) out.add(n.pos);
        Collections.reverse(out);
        return out;
    }

    private static final class Node {
        final BlockPos pos;
        final double g;
        final double f;
        final Node parent;

        Node(BlockPos pos, double g, double f, Node parent) {
            this.pos = pos;
            this.g = g;
            this.f = f;
            this.parent = parent;
        }
    }
}
