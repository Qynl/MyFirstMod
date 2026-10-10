package dev.qynl.ollamaplayer.companion;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * The companion's physical body. Fabric's FakePlayer has an empty tick() and is invulnerable,
 * so this subclass supplies the pieces the companion needs: gravity, collision movement,
 * hunger/regeneration, and the ability to get hurt.
 */
public final class NovaBody extends FakePlayer {
    private static final double GRAVITY = 0.08;
    private static final double DRAG_Y = 0.98;

    public NovaBody(ServerWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Override
    public void tick() {
        // Vanilla player ticking is disabled for fake players, so we advance the body ourselves.
        Vec3d v = getVelocity();
        double vy = v.y;
        if (!isOnGround() || vy > 0) vy -= GRAVITY;
        vy *= DRAG_Y;

        move(MovementType.SELF, new Vec3d(v.x, vy, v.z));
        // Collision may have stopped us. Keep the horizontal intent, clear vertical once grounded.
        setVelocity(v.x, isOnGround() ? 0.0 : vy, v.z);
        velocityModified = true;

        if (!getWorld().isClient()) {
            getHungerManager().update(this);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false;
    }
}
