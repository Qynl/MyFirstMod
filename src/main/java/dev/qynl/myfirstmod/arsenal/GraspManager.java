package dev.qynl.myfirstmod.arsenal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.qynl.myfirstmod.boss.NullWardenEntity;
import dev.qynl.myfirstmod.item.NullGraspItem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Server-side state for the Grasp of the Null gauntlet.
 *
 * <p>Handles three things:
 * <ul>
 *   <li><b>Grabbing</b> — a creature (or, while sneaking, a block lifted as a
 *       falling-block entity) is seized at range and suspended ahead of the
 *       player, trailing sculk energy.</li>
 *   <li><b>Holding</b> — every tick the held entity is steered toward a point
 *       in front of the player's eyes, with gravity suppressed and fall damage
 *       zeroed, so it hovers and sways naturally.</li>
 *   <li><b>Throwing</b> — the entity is hurled along the player's look vector.
 *       Impacts deal damage that scales with flight speed, splash to nearby
 *       creatures, and ignite thrown creepers on arrival.</li>
 * </ul>
 */
public final class GraspManager {
    private static final double REACH = 14.0;
    private static final double HOLD_PULL = 0.5;
    private static final double HOLD_MAX_SPEED = 1.3;
    private static final double HOLD_BREAK_DISTANCE = 10.0;
    private static final double THROW_SPEED = 2.7;
    private static final int THROWN_TRACK_TICKS = 80;

    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private static final List<Thrown> THROWN = new ArrayList<>();

    private static final class Hold {
        final Entity entity;
        final boolean hadNoGravity;
        int age;

        Hold(Entity entity, boolean hadNoGravity) {
            this.entity = entity;
            this.hadNoGravity = hadNoGravity;
        }
    }

    private static final class Thrown {
        final Entity entity;
        final UUID thrower;
        int age;
        double peakSpeed;

        Thrown(Entity entity, UUID thrower, double initialSpeed) {
            this.entity = entity;
            this.thrower = thrower;
            this.peakSpeed = initialSpeed;
        }
    }

    private GraspManager() {
    }

    public static boolean isHolding(ServerPlayerEntity player) {
        return HOLDS.containsKey(player.getUuid());
    }

    /** Seize the creature the player is looking at, if there is one in reach. */
    public static boolean tryGrabEntity(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        Vec3d end = eye.add(look.multiply(REACH));

        double blockDistance = blockDistance(player, eye, end);
        Box searchBox = player.getBoundingBox().stretch(look.multiply(REACH)).expand(1.5);
        EntityHitResult hit = ProjectileUtil.raycast(player, eye, end, searchBox,
                entity -> canGrab(entity), blockDistance * blockDistance);
        if (hit == null) return false;

        grab(player, hit.getEntity());
        return true;
    }

    /** Sneak-grab: rip the targeted block out of the world and hold it. */
    public static boolean tryGrabBlock(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(REACH));
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(
                eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return false;

        BlockPos pos = hit.getBlockPos();
        ServerWorld world = player.getServerWorld();
        if (!world.canPlayerModifyAt(player, pos)) return false;

        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.isLiquid()) return false;
        if (state.getHardness(world, pos) < 0.0F) return false; // bedrock & friends
        if (world.getBlockEntity(pos) != null) return false;    // keep chests honest

        FallingBlockEntity block = FallingBlockEntity.spawnFromBlock(world, pos, state);
        grab(player, block);
        return true;
    }

    private static double blockDistance(ServerPlayerEntity player, Vec3d eye, Vec3d end) {
        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(
                eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? REACH : hit.getPos().distanceTo(eye);
    }

    private static boolean canGrab(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator()) return false;
        if (isHeldByAnyone(entity)) return false;
        if (entity instanceof TntEntity || entity instanceof FallingBlockEntity) return true;
        if (!(entity instanceof LivingEntity)) return false;
        if (entity instanceof PlayerEntity) return false;
        if (entity instanceof NullWardenEntity
                || entity instanceof WitherEntity
                || entity instanceof EnderDragonEntity) return false;
        return entity.getWidth() <= 2.5F;
    }

    private static boolean isHeldByAnyone(Entity entity) {
        for (Hold hold : HOLDS.values()) {
            if (hold.entity == entity) return true;
        }
        return false;
    }

    private static void grab(ServerPlayerEntity player, Entity target) {
        target.stopRiding();
        HOLDS.put(player.getUuid(), new Hold(target, target.hasNoGravity()));
        target.setNoGravity(true);
        target.fallDistance = 0.0F;

        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                target.getX(), target.getBodyY(0.5), target.getZ(),
                14, target.getWidth() * 0.5, target.getHeight() * 0.5, target.getWidth() * 0.5, 0.02);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                SoundCategory.PLAYERS, 1.0F, 0.55F);
        world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
    }

    /** Hurl whatever is being held along the player's look vector. */
    public static void throwHeld(ServerPlayerEntity player) {
        Hold hold = HOLDS.remove(player.getUuid());
        if (hold == null) return;

        Entity entity = hold.entity;
        entity.setNoGravity(hold.hadNoGravity);
        entity.fallDistance = 0.0F;

        Vec3d velocity = player.getRotationVec(1.0F).multiply(THROW_SPEED).add(0.0, 0.12, 0.0);
        entity.setVelocity(velocity);
        entity.velocityModified = true;

        if (entity instanceof FallingBlockEntity block) {
            block.setHurtEntities(6.0F, 40); // anvil rules for thrown masonry
        }
        THROWN.add(new Thrown(entity, player.getUuid(), velocity.length()));

        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.SONIC_BOOM,
                entity.getX(), entity.getBodyY(0.5), entity.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                SoundCategory.PLAYERS, 0.5F, 1.5F);
    }

    /** Sneak-use while holding: set the victim down without the fireworks. */
    public static void releaseGently(ServerPlayerEntity player) {
        Hold hold = HOLDS.remove(player.getUuid());
        if (hold == null) return;
        releaseEntity(hold);
        player.getServerWorld().playSound(null, hold.entity.getBlockPos(),
                SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, 0.7F, 1.2F);
    }

    /** Disconnect / death cleanup. */
    public static void clear(ServerPlayerEntity player) {
        Hold hold = HOLDS.remove(player.getUuid());
        if (hold != null) releaseEntity(hold);
    }

    private static void releaseEntity(Hold hold) {
        Entity entity = hold.entity;
        entity.setNoGravity(hold.hadNoGravity);
        entity.setVelocity(entity.getVelocity().multiply(0.2));
        entity.velocityModified = true;
        entity.fallDistance = 0.0F;
    }

    public static void tick(MinecraftServer server) {
        tickHolds(server);
        tickThrown();
    }

    private static void tickHolds(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Hold>> iterator = HOLDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Hold> entry = iterator.next();
            Hold hold = entry.getValue();
            Entity entity = hold.entity;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            boolean invalid = player == null
                    || !player.isAlive()
                    || entity.isRemoved()
                    || !entity.isAlive()
                    || entity.getWorld() != player.getWorld()
                    || !isWieldingGrasp(player);
            if (invalid) {
                iterator.remove();
                releaseEntity(hold);
                continue;
            }

            double holdDistance = 1.9 + entity.getWidth() * 0.6;
            Vec3d target = player.getEyePos()
                    .add(player.getRotationVec(1.0F).multiply(holdDistance))
                    .add(0.0, -entity.getHeight() * 0.5, 0.0);
            Vec3d delta = target.subtract(entity.getPos());
            if (delta.length() > HOLD_BREAK_DISTANCE) {
                iterator.remove();
                releaseEntity(hold);
                continue;
            }

            Vec3d velocity = delta.multiply(HOLD_PULL);
            if (velocity.length() > HOLD_MAX_SPEED) {
                velocity = velocity.normalize().multiply(HOLD_MAX_SPEED);
            }
            entity.setVelocity(velocity);
            entity.velocityModified = true;
            entity.fallDistance = 0.0F;

            if (entity instanceof FallingBlockEntity block) {
                block.timeFalling = 1; // never times out while suspended
            }
            if (entity instanceof MobEntity mob) {
                mob.getNavigation().stop();
            }

            hold.age++;
            if (entity.getWorld() instanceof ServerWorld world) {
                if (hold.age % 2 == 0) {
                    world.spawnParticles(ParticleTypes.SCULK_CHARGE_POP,
                            entity.getX(), entity.getBodyY(0.6), entity.getZ(),
                            1, entity.getWidth() * 0.4, entity.getHeight() * 0.4, entity.getWidth() * 0.4, 0.0);
                }
                if (hold.age % 30 == 0) {
                    world.playSound(null, entity.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                            SoundCategory.PLAYERS, 0.4F, 0.6F);
                }
            }
        }
    }

    private static boolean isWieldingGrasp(ServerPlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof NullGraspItem
                || player.getOffHandStack().getItem() instanceof NullGraspItem;
    }

    private static void tickThrown() {
        Iterator<Thrown> iterator = THROWN.iterator();
        while (iterator.hasNext()) {
            Thrown thrown = iterator.next();
            Entity entity = thrown.entity;
            if (entity.isRemoved() || !(entity.getWorld() instanceof ServerWorld world)) {
                iterator.remove();
                continue;
            }

            double speed = entity.getVelocity().length();
            thrown.peakSpeed = Math.max(thrown.peakSpeed, speed);

            if (++thrown.age > THROWN_TRACK_TICKS) {
                iterator.remove();
                continue;
            }
            if (thrown.age <= 2) continue; // arm after leaving the player's hand

            List<Entity> directVictims = world.getOtherEntities(entity,
                    entity.getBoundingBox().expand(0.35),
                    victim -> victim instanceof LivingEntity
                            && victim.isAlive()
                            && !victim.getUuid().equals(thrown.thrower)
                            && !isHeldByAnyone(victim));

            boolean collided = entity.horizontalCollision || entity.verticalCollision || entity.isOnGround();
            boolean struckSomeone = !directVictims.isEmpty() && speed > 0.4;
            if (struckSomeone || collided) {
                impact(world, thrown, directVictims);
                iterator.remove();
            }
        }
    }

    private static void impact(ServerWorld world, Thrown thrown, List<Entity> directVictims) {
        Entity entity = thrown.entity;
        float power = (float) Math.min(14.0, 4.0 + thrown.peakSpeed * 3.5);

        ServerPlayerEntity thrower = world.getServer().getPlayerManager().getPlayer(thrown.thrower);
        DamageSource source = thrower != null
                ? world.getDamageSources().playerAttack(thrower)
                : world.getDamageSources().fallingBlock(entity);

        for (Entity victim : directVictims) {
            ((LivingEntity) victim).damage(source, power);
            Vec3d push = entity.getVelocity();
            victim.addVelocity(push.x * 0.4, 0.25, push.z * 0.4);
        }

        List<Entity> splash = world.getOtherEntities(entity,
                entity.getBoundingBox().expand(2.0),
                victim -> victim instanceof LivingEntity
                        && victim.isAlive()
                        && !victim.getUuid().equals(thrown.thrower)
                        && !directVictims.contains(victim)
                        && !isHeldByAnyone(victim));
        for (Entity victim : splash) {
            ((LivingEntity) victim).damage(source, power * 0.5F);
            Vec3d away = victim.getPos().subtract(entity.getPos());
            double length = Math.max(0.01, away.horizontalLength());
            victim.addVelocity(away.x / length * 0.35, 0.2, away.z / length * 0.35);
        }

        // The projectile pays a toll too...
        if (entity instanceof LivingEntity living) {
            living.damage(world.getDamageSources().flyIntoWall(), Math.min(10.0F, power * 0.6F));
        }
        // ...and thrown creepers arrive primed.
        if (entity instanceof CreeperEntity creeper) {
            creeper.ignite();
        }

        world.spawnParticles(ParticleTypes.EXPLOSION,
                entity.getX(), entity.getBodyY(0.5), entity.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
        world.spawnParticles(ParticleTypes.CRIT,
                entity.getX(), entity.getBodyY(0.5), entity.getZ(), 18, 0.6, 0.5, 0.6, 0.25);
        world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
    }
}
