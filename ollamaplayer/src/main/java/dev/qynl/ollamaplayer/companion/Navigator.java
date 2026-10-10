package dev.qynl.ollamaplayer.companion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Follows a precomputed path by steering velocity each tick.
 * Handles jumping onto higher blocks, waiting for drops, and detects getting stuck.
 */
public final class Navigator {
    public enum Result { NONE, MOVING, ARRIVED, FAILED }

    private List<BlockPos> path;
    private int index;
    private int stuckTicks;
    private Vec3d lastPos;

    public boolean hasPath() {
        return path != null;
    }

    public void setPath(List<BlockPos> newPath) {
        this.path = newPath;
        this.index = 0;
        this.stuckTicks = 0;
        this.lastPos = null;
    }

    public void clear() {
        path = null;
        index = 0;
        stuckTicks = 0;
        lastPos = null;
    }

    public Result tick(LivingEntity body, double speed) {
        if (path == null) return Result.NONE;
        Vec3d pos = body.getPos();

        if (lastPos != null && pos.squaredDistanceTo(lastPos) < 0.0025) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPos = pos;
        if (stuckTicks > 35) {
            clear();
            return Result.FAILED;
        }

        while (index < path.size()) {
            BlockPos n = path.get(index);
            double hd = Math.hypot(n.getX() + 0.5 - pos.x, n.getZ() + 0.5 - pos.z);
            double dy = n.getY() - pos.y;
            if (hd < 0.45 && Math.abs(dy) < 0.9) index++;
            else break;
        }
        if (index >= path.size()) {
            clear();
            return Result.ARRIVED;
        }

        BlockPos n = path.get(index);
        double dx = n.getX() + 0.5 - pos.x;
        double dz = n.getZ() + 0.5 - pos.z;
        double hd = Math.hypot(dx, dz);
        double dy = n.getY() - pos.y;

        if (hd < 0.45 && dy < -0.9) {
            // Walked off a ledge: let gravity do the work, don't wobble over the spot.
            Vec3d v = body.getVelocity();
            body.setVelocity(0, v.y, 0);
            body.velocityModified = true;
            return Result.MOVING;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        body.setYaw(yaw);
        body.setHeadYaw(yaw);

        double vx = 0, vz = 0;
        if (hd > 1.0e-4) {
            vx = dx / hd * speed;
            vz = dz / hd * speed;
        }
        double vy = body.getVelocity().y;
        if (body.isOnGround() && (dy > 0.5 || stuckTicks > 8)) {
            vy = 0.42; // jump up a block (or hop an obstacle when stuck)
        }
        body.setVelocity(vx, vy, vz);
        body.velocityModified = true;
        return Result.MOVING;
    }
}
