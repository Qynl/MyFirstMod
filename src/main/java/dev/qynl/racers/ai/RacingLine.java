package dev.qynl.racers.ai;

import java.util.List;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A closed racing line: the smooth path the AI drivers aim at, in world
 * coordinates, with per-point curvature and width.
 *
 * <p>Lines are built once per track from its checkpoints (which the track
 * format stores in driving order) and then cached by {@link dev.qynl.racers.track.Track}.
 */
public final class RacingLine {
    private final double[] xs;
    private final double[] zs;
    private final double[] curv;
    private final double[] half;
    private final int size;

    private RacingLine(double[] xs, double[] zs, double[] curv, double[] half) {
        this.xs = xs;
        this.zs = zs;
        this.curv = curv;
        this.half = half;
        this.size = xs.length;
    }

    /** Builds a loop from ordered waypoints, resampled to ~1m spacing. */
    public static RacingLine fromWaypoints(List<double[]> waypoints, double halfWidth, int samples) {
        int n = waypoints.size();
        if (n < 3) {
            return null;
        }
        double[] px = new double[n];
        double[] pz = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = waypoints.get(i)[0];
            pz[i] = waypoints.get(i)[1];
        }
        // Catmull-Rom through the loop
        java.util.ArrayList<double[]> dense = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            double[] p0 = {px[(i - 1 + n) % n], pz[(i - 1 + n) % n]};
            double[] p1 = {px[i], pz[i]};
            double[] p2 = {px[(i + 1) % n], pz[(i + 1) % n]};
            double[] p3 = {px[(i + 2) % n], pz[(i + 2) % n]};
            for (int s = 0; s < samples; s++) {
                double t = s / (double) samples;
                double t2 = t * t;
                double t3 = t2 * t;
                double x = 0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t
                        + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                        + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3);
                double z = 0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t
                        + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                        + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3);
                dense.add(new double[]{x, z});
            }
        }
        // resample to 1 block spacing
        java.util.ArrayList<double[]> out = new java.util.ArrayList<>();
        double carry = 0.0;
        for (int i = 0; i < dense.size(); i++) {
            double[] a = dense.get(i);
            double[] b = dense.get((i + 1) % dense.size());
            double seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double travelled = carry;
            while (travelled < seg) {
                double f = seg <= 1e-6 ? 0.0 : travelled / seg;
                out.add(new double[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f});
                travelled += 1.0;
            }
            carry = travelled - seg;
        }
        if (out.size() < 8) {
            return null;
        }
        int m = out.size();
        double[] xs = new double[m];
        double[] zs = new double[m];
        double[] halfArr = new double[m];
        for (int i = 0; i < m; i++) {
            xs[i] = out.get(i)[0];
            zs[i] = out.get(i)[1];
            halfArr[i] = halfWidth;
        }
        double[] curv = computeCurvature(xs, zs);
        return new RacingLine(xs, zs, curv, halfArr);
    }

    private static double[] computeCurvature(double[] xs, double[] zs) {
        int m = xs.length;
        double[] k = new double[m];
        for (int i = 0; i < m; i++) {
            int a = (i - 3 + m) % m;
            int b = (i + 3) % m;
            double x1 = xs[b] - xs[i];
            double z1 = zs[b] - zs[i];
            double x2 = xs[i] - xs[a];
            double z2 = zs[i] - zs[a];
            double cross = x1 * z2 - z1 * x2;
            double d1 = Math.hypot(x1, z1);
            double d2 = Math.hypot(x2, z2);
            double d3 = Math.hypot(x1 - x2, z1 - z2);
            double area = d1 * d2 * d3;
            k[i] = area < 1e-4 ? 0.0 : Math.abs(cross * 2.0 / area);
        }
        // light smoothing so a single noisy sample cannot make the AI brake-check
        double[] smooth = new double[m];
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            for (int o = -2; o <= 2; o++) {
                sum += k[(i + o + m) % m];
            }
            smooth[i] = sum / 5.0;
        }
        return smooth;
    }

    public int size() {
        return size;
    }

    public double x(int index) {
        return xs[Math.floorMod(index, size)];
    }

    public double z(int index) {
        return zs[Math.floorMod(index, size)];
    }

    public double curvature(int index) {
        return curv[Math.floorMod(index, size)];
    }

    public double halfWidth(int index) {
        return half[Math.floorMod(index, size)];
    }

    public Vec3d pointAt(int index) {
        return new Vec3d(x(index), 0.0, z(index));
    }

    /** Index of the closest point, searching around {@code hint}. */
    public int nearestIndex(double x, double z, int hint) {
        if (size == 0) {
            return 0;
        }
        int best = Math.floorMod(hint, size);
        double bestDist = Double.MAX_VALUE;
        int window = Math.max(12, size / 4);
        for (int o = -window; o <= window; o++) {
            int i = Math.floorMod(best + o, size);
            double d = Math.hypot(xs[i] - x, zs[i] - z);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** Index roughly {@code distance} blocks ahead of {@code from}. */
    public int indexAhead(int from, double distance) {
        return Math.floorMod(from + (int) Math.round(distance), size);
    }

    /** Strongest curvature within the next {@code distance} blocks. */
    public double maxCurvatureAhead(int from, double distance) {
        int steps = Math.max(1, (int) Math.round(distance));
        double max = 0.0;
        for (int i = 0; i <= steps; i++) {
            max = Math.max(max, curvature(from + i));
        }
        return max;
    }

    public double yaw(int index) {
        int next = Math.floorMod(index + 2, size);
        int prev = Math.floorMod(index - 2, size);
        double dx = xs[next] - xs[prev];
        double dz = zs[next] - zs[prev];
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    /** Lateral offset (clamped to the road) of a point, used for overtaking lines. */
    public int offsetIndex(int index, double lateral) {
        int next = Math.floorMod(index + 3, size);
        int prev = Math.floorMod(index - 3, size);
        double tx = xs[next] - xs[prev];
        double tz = zs[next] - zs[prev];
        double len = Math.max(0.001, Math.hypot(tx, tz));
        double rx = -tz / len;
        double rz = tx / len;
        double px = xs[Math.floorMod(index, size)] + rx * lateral;
        double pz = zs[Math.floorMod(index, size)] + rz * lateral;
        // return the closest index to the offset point (cheap: we are within ~2 blocks)
        return nearestIndex(px, pz, index);
    }

    public static double distance(double x1, double z1, double x2, double z2) {
        return Math.hypot(x1 - x2, z1 - z2);
    }

    public static double angleTo(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static double yawError(double desired, double current) {
        return MathHelper.wrapDegrees(desired - current);
    }
}
