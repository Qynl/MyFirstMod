package dev.qynl.racers.track;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.ai.RacingLine;

/**
 * One physical copy of a {@link Track} in the world.
 *
 * <p>The structure file is always written with its lowest corner at 0,0, so a
 * placement is just: corner position + rotation + the dimensions of the file.
 * Everything the race engine needs (grid, checkpoints, racing line) is then
 * transformed on the fly.
 */
public final class TrackPlacement {
    public final UUID id;
    public final Track track;
    public final BlockPos anchor;
    /** 0, 90, 180 or 270 degrees clockwise (Minecraft yaw). */
    public final int rotation;
    private RacingLine line;

    public TrackPlacement(UUID id, Track track, BlockPos anchor, int rotation) {
        this.id = id;
        this.track = track;
        this.anchor = anchor.toImmutable();
        this.rotation = ((rotation % 360) + 360) % 360;
    }

    /** Rotation matrix for this placement. */
    private double[] rotate(double x, double z) {
        return switch (rotation) {
            case 90 -> new double[]{-z, x};
            case 180 -> new double[]{-x, -z};
            case 270 -> new double[]{z, -x};
            default -> new double[]{x, z};
        };
    }

    /** The offset applied after rotation so the structure still grows towards +X/+Z. */
    private double[] shift() {
        int sx = Math.max(1, track.size[0]);
        int sz = Math.max(1, track.size[2]);
        return switch (rotation) {
            case 90 -> new double[]{sz - 1.0, 0.0};
            case 180 -> new double[]{sx - 1.0, sz - 1.0};
            case 270 -> new double[]{0.0, sx - 1.0};
            default -> new double[]{0.0, 0.0};
        };
    }

    /** Local block coordinate (structure space) -> world block position, unrotated. */
    public BlockPos localToWorld(int x, int y, int z) {
        double[] r = rotate(x, z);
        double[] s = shift();
        return new BlockPos((int) Math.round(anchor.getX() + r[0] + s[0]),
                anchor.getY() + y,
                (int) Math.round(anchor.getZ() + r[1] + s[1]));
    }

    /** Local point (usually a checkpoint) -> world position at the given local Y. */
    public Vec3d pointToWorld(double x, double z, double y) {
        double[] r = rotate(x, z);
        double[] s = shift();
        return new Vec3d(anchor.getX() + r[0] + s[0], anchor.getY() + y, anchor.getZ() + r[1] + s[1]);
    }

    public float yawToWorld(double localYaw) {
        return (float) (((localYaw + rotation) % 360.0 + 360.0) % 360.0);
    }

    /** World position of a grid slot (car feet position). */
    public Vec3d gridPos(int index) {
        if (track.grid.isEmpty()) {
            return pointToWorld(track.halfWidth(), track.halfWidth(), TrackStructure.CAR_Y);
        }
        double[] slot = track.grid.get(Math.floorMod(index, track.grid.size()));
        return pointToWorld(slot[0], slot[1], TrackStructure.CAR_Y);
    }

    public int gridSlots() {
        return Math.max(4, track.grid.size());
    }

    /** World position of the start line centre. */
    public Vec3d startPos() {
        if (track.checkpoints.isEmpty()) {
            return gridPos(0);
        }
        double[] cp = track.checkpoints.get(0);
        return pointToWorld(cp[0], cp[1], TrackStructure.CAR_Y);
    }

    public float startYaw() {
        if (track.checkpoints.isEmpty()) {
            return yawToWorld(0);
        }
        double[] cp = track.checkpoints.get(0);
        return yawToWorld(cp.length > 2 ? cp[2] : 0.0);
    }

    public Vec3d checkpointPos(int index) {
        if (track.checkpoints.isEmpty()) {
            return startPos();
        }
        double[] cp = track.checkpoints.get(Math.floorMod(index, track.checkpoints.size()));
        return pointToWorld(cp[0], cp[1], TrackStructure.CAR_Y);
    }

    public int checkpointCount() {
        return Math.max(1, track.checkpoints.size());
    }

    /** Lazily built closed racing line in world coordinates. */
    public RacingLine line() {
        if (line == null) {
            List<double[]> points = new ArrayList<>();
            for (double[] cp : track.checkpoints) {
                Vec3d world = pointToWorld(cp[0], cp[1], TrackStructure.CAR_Y);
                points.add(new double[]{world.x, world.z});
            }
            if (points.size() >= 3) {
                line = RacingLine.fromWaypoints(points, track.halfWidth(), 40);
            }
            if (line == null) {
                List<double[]> fallback = new ArrayList<>();
                for (int i = 0; i < gridSlots(); i++) {
                    Vec3d world = gridPos(i);
                    fallback.add(new double[]{world.x, world.z});
                }
                if (fallback.size() >= 3) {
                    line = RacingLine.fromWaypoints(fallback, track.halfWidth(), 30);
                }
            }
        }
        return line;
    }

    public Vec3d pitBox() {
        if (track.pitBox == null) {
            return null;
        }
        return pointToWorld(track.pitBox[0], track.pitBox.length > 2 ? track.pitBox[2] : 0.0,
                track.pitBox.length > 1 ? track.pitBox[1] : TrackStructure.CAR_Y);
    }

    /** Rough bounds of the placed structure, used for the "you are here" checks. */
    public boolean contains(Vec3d pos, double margin) {
        int sx = Math.max(1, track.size[0]);
        int sz = Math.max(1, track.size[2]);
        double xs = anchor.getX() - margin;
        double zs = anchor.getZ() - margin;
        double xe = anchor.getX() + (rotation % 180 == 0 ? sx : sz) + margin;
        double ze = anchor.getZ() + (rotation % 180 == 0 ? sz : sx) + margin;
        return pos.x >= Math.min(xs, xe) && pos.x <= Math.max(xs, xe)
                && pos.z >= Math.min(zs, ze) && pos.z <= Math.max(zs, ze);
    }

    public double distanceTo(Vec3d pos) {
        Vec3d centre = new Vec3d(anchor.getX() + track.size[0] / 2.0, anchor.getY(),
                anchor.getZ() + track.size[2] / 2.0);
        return centre.distanceTo(pos);
    }

    public String label() {
        return track.name + " @ " + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ();
    }
}
