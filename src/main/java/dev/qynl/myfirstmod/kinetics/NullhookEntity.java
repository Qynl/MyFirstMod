package dev.qynl.myfirstmod.kinetics;

import dev.qynl.myfirstmod.boss.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * The Nullhook — a void-energy grapple fired from the Nullhook item.
 *
 * <p>State machine: {@code FLYING → ANCHORED → RETRACTING → discard}.
 * While anchored, the server pulls the owner toward the anchor point every
 * tick (with matching client prediction in {@code KineticsClientController})
 * so the ride is butter-smooth.
 */
public class NullhookEntity extends Entity {
    public static final byte STATE_FLYING = 0;
    public static final byte STATE_ANCHORED = 1;
    public static final byte STATE_RETRACTING = 2;

    private static final TrackedData<Byte> HOOK_STATE =
            DataTracker.registerData(NullhookEntity.class, TrackedDataHandlerRegistry.BYTE);

    private static final double ENERGY_DRAIN_PER_TICK = 0.12;

    private UUID ownerUuid;
    private Vec3d anchor = Vec3d.ZERO;
    private Vec3d initialPos = null;
    private int ownerMissingTicks = 0;
    private int anchoredTicks = 0;

    public NullhookEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public NullhookEntity(World world, PlayerEntity owner) {
        this(ModEntities.NULLHOOK, world);
        this.ownerUuid = owner.getUuid();
        this.setPosition(owner.getEyePos().subtract(0.0, 0.25, 0.0));
        Vec3d look = owner.getRotationVector();
        this.setVelocity(look.multiply(KineticsConfig.get().grappleHookSpeed));
        this.setYaw(yawFrom(look));
        this.setPitch(pitchFrom(look));
    }

    // ------------------------------------------------------------------
    // Data tracking
    // ------------------------------------------------------------------

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(HOOK_STATE, STATE_FLYING);
    }

    public byte getHookState() {
        return this.getDataTracker().get(HOOK_STATE);
    }

    private void setHookState(byte state) {
        this.getDataTracker().set(HOOK_STATE, state);
    }

    public boolean isAnchored() {
        return getHookState() == STATE_ANCHORED;
    }

    public Vec3d getAnchor() {
        return anchor;
    }

    public PlayerEntity getOwner() {
        if (this.ownerUuid == null || this.getWorld() == null) return null;
        return this.getWorld().getPlayerByUuid(this.ownerUuid);
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        KineticsConfig cfg = KineticsConfig.get();

        switch (getHookState()) {
            case STATE_FLYING -> tickFlying(world, cfg);
            case STATE_ANCHORED -> tickAnchored(world, cfg);
            case STATE_RETRACTING -> tickRetracting();
            default -> {
            }
        }

        if (this.age > 400) {
            this.discard();
        }
    }

    private void tickFlying(ServerWorld world, KineticsConfig cfg) {
        if (initialPos == null) {
            initialPos = this.getPos();
        }
        Vec3d pos = this.getPos();
        Vec3d velocity = this.getVelocity();
        Vec3d next = pos.add(velocity);

        BlockHitResult hit = this.getWorld().raycast(new RaycastContext(
                pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));

        if (hit.getType() == HitResult.Type.BLOCK) {
            anchorAt(hit.getPos());
            return;
        }

        this.setPosition(next);
        this.setYaw(yawFrom(velocity));
        this.setPitch(pitchFrom(velocity));

        if (this.age % 2 == 0) {
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.x, pos.y, pos.z, 2, 0.02, 0.02, 0.02, 0.0);
        }

        double travelled = this.getPos().subtract(initialPos).length();
        if (travelled > cfg.grappleMaxRange || velocity.lengthSquared() < 0.01) {
            startRetracting();
        }
    }

    private void tickAnchored(ServerWorld world, KineticsConfig cfg) {
        PlayerEntity owner = getOwner();
        if (!(owner instanceof ServerPlayerEntity player) || owner.isDead()) {
            if (++ownerMissingTicks > 10) {
                this.discard();
            }
            return;
        }
        ownerMissingTicks = 0;

        // Register with the manager on the first anchored tick.
        if (anchoredTicks == 0) {
            KineticsState state = KineticsManager.getState(player);
            state.grappleEntityId = this.getId();
            state.markDirty();
        }
        anchoredTicks++;

        Vec3d ownerCenter = player.getPos().add(0.0, 0.5, 0.0);
        double dist = anchor.distanceTo(ownerCenter);

        // Release conditions
        if (player.isSneaking()
                || anchoredTicks > cfg.grappleMaxTicks
                || dist < cfg.grappleReleaseDistance
                || (player.isOnGround() && anchoredTicks > 40)
                || !hasLineOfSightToAnchor(player)) {
            release(player, world);
            return;
        }

        // Energy drain
        KineticsState state = KineticsManager.getState(player);
        state.energy -= ENERGY_DRAIN_PER_TICK;
        state.markDirty();
        if (state.energy <= 0.0) {
            state.energy = 0.0;
            release(player, world);
            return;
        }

        // The pull — server authority. The client predicts the same math.
        Vec3d pull = KineticMath.grapplePull(ownerCenter, anchor, cfg);
        player.setVelocity(pull);
        player.velocityModified = true;

        if (this.age % 4 == 0) {
            Vec3d mid = anchor.multiply(0.5).add(player.getPos().add(0.0, 0.8, 0.0).multiply(0.5));
            world.spawnParticles(ParticleTypes.GLOW, mid.x, mid.y, mid.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    private void tickRetracting() {
        PlayerEntity owner = getOwner();
        if (owner == null) {
            if (++ownerMissingTicks > 10) {
                this.discard();
            }
            return;
        }
        ownerMissingTicks = 0;
        Vec3d toOwner = owner.getEyePos().subtract(this.getPos());
        double dist = toOwner.length();
        if (dist < 1.2) {
            this.discard();
            return;
        }
        this.setVelocity(toOwner.multiply(2.2 / dist));
        this.setPosition(this.getPos().add(this.getVelocity()));
    }

    // ------------------------------------------------------------------
    // Transitions
    // ------------------------------------------------------------------

    private void anchorAt(Vec3d pos) {
        this.anchor = pos;
        this.setPosition(pos);
        this.setVelocity(Vec3d.ZERO);
        this.setHookState(STATE_ANCHORED);
        this.anchoredTicks = 0;

        ServerWorld world = (ServerWorld) this.getWorld();
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE,
                SoundCategory.PLAYERS, 1.0F, 0.7F);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ARROW_HIT,
                SoundCategory.PLAYERS, 0.5F, 0.6F);
        world.spawnParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 10, 0.15, 0.15, 0.15, 0.02);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 6, 0.1, 0.1, 0.1, 0.01);

        PlayerEntity owner = getOwner();
        if (owner instanceof ServerPlayerEntity player && initialPos != null) {
            KineticsManager.noteGrapple(player, initialPos.distanceTo(pos));
        }
    }

    /** Detaches with a satisfying little upward flick. */
    private void release(ServerPlayerEntity player, ServerWorld world) {
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x, v.y + 0.18, v.z);
        player.velocityModified = true;
        KineticsManager.handleGrappleEnd(player);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BREEZE_LAND,
                SoundCategory.PLAYERS, 0.6F, 1.3F);
        startRetracting();
    }

    public void startRetracting() {
        if (getHookState() == STATE_RETRACTING) return;
        this.setHookState(STATE_RETRACTING);
        this.setVelocity(Vec3d.ZERO);
        PlayerEntity owner = getOwner();
        if (owner instanceof ServerPlayerEntity player) {
            KineticsManager.handleGrappleEnd(player);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private boolean hasLineOfSightToAnchor(PlayerEntity player) {
        Vec3d eye = player.getEyePos();
        BlockHitResult hit = this.getWorld().raycast(new RaycastContext(
                eye, anchor, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    private static float yawFrom(Vec3d v) {
        return (float) Math.toDegrees(Math.atan2(-v.x, v.z));
    }

    private static float pitchFrom(Vec3d v) {
        double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
        return (float) Math.toDegrees(-Math.atan2(v.y, horiz));
    }

    // ------------------------------------------------------------------
    // NBT
    // ------------------------------------------------------------------

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("Owner")) {
            this.ownerUuid = nbt.getUuid("Owner");
        }
        if (nbt.contains("AnchorX")) {
            this.anchor = new Vec3d(
                    nbt.getDouble("AnchorX"), nbt.getDouble("AnchorY"), nbt.getDouble("AnchorZ"));
        }
        this.setHookState(nbt.getByte("HookState"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        if (this.ownerUuid != null) {
            nbt.putUuid("Owner", this.ownerUuid);
        }
        nbt.putDouble("AnchorX", anchor.x);
        nbt.putDouble("AnchorY", anchor.y);
        nbt.putDouble("AnchorZ", anchor.z);
        nbt.putByte("HookState", getHookState());
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
