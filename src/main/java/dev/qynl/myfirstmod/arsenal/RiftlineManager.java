package dev.qynl.myfirstmod.arsenal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

/**
 * Server-side physics for the Riftline grapple.
 *
 * <p>While a player is "zipping", their velocity is blended toward the latched
 * anchor every tick, which preserves momentum and makes chained casts flow into
 * one another. A short fall-damage grace window after every zip rewards bold
 * swings instead of punishing them.
 */
public final class RiftlineManager {
    private static final double ARRIVE_DISTANCE = 2.1;
    private static final double MAX_ZIP_SPEED = 1.45;
    private static final int ZIP_TIMEOUT_TICKS = 120;
    private static final int GRACE_TICKS = 140;

    private static final Map<UUID, Zip> ZIPS = new HashMap<>();
    private static final Map<UUID, Integer> FALL_GRACE = new HashMap<>();

    private static final class Zip {
        final ServerWorld world;
        final Vec3d anchor;
        int ticks;

        Zip(ServerWorld world, Vec3d anchor) {
            this.world = world;
            this.anchor = anchor;
        }
    }

    private RiftlineManager() {
    }

    /** Called by the anchor projectile when it bites into a block. */
    public static void latch(ServerPlayerEntity player, BlockHitResult hit) {
        Vec3d target = hit.getPos().add(Vec3d.of(hit.getSide().getVector()).multiply(0.45));
        ServerWorld world = player.getServerWorld();
        ZIPS.put(player.getUuid(), new Zip(world, target));

        world.spawnParticles(ParticleTypes.SCULK_SOUL, target.x, target.y, target.z,
                10, 0.2, 0.2, 0.2, 0.03);
        world.playSound(null, target.x, target.y, target.z,
                SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.2F, 0.65F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, 0.8F, 0.5F);
    }

    public static void clear(UUID playerId) {
        ZIPS.remove(playerId);
        FALL_GRACE.remove(playerId);
    }

    public static void tick(MinecraftServer server) {
        tickZips(server);
        tickFallGrace(server);
    }

    private static void tickZips(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Zip>> iterator = ZIPS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Zip> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            Zip zip = entry.getValue();

            if (player == null || !player.isAlive() || player.getServerWorld() != zip.world) {
                iterator.remove();
                continue;
            }

            // Sneaking cuts the line and keeps whatever momentum you earned.
            if (player.isSneaking() && zip.ticks > 1) {
                iterator.remove();
                release(player, false);
                continue;
            }

            Vec3d position = player.getPos().add(0.0, player.getHeight() * 0.5, 0.0);
            Vec3d delta = zip.anchor.subtract(position);
            double distance = delta.length();

            if (distance < ARRIVE_DISTANCE || zip.ticks++ > ZIP_TIMEOUT_TICKS) {
                iterator.remove();
                player.setVelocity(player.getVelocity().multiply(0.6).add(0.0, 0.32, 0.0));
                player.velocityModified = true;
                release(player, true);
                continue;
            }

            // Blend current velocity toward the anchor: snappy, but momentum carries.
            double speed = Math.min(MAX_ZIP_SPEED, 0.55 + distance * 0.06);
            Vec3d velocity = player.getVelocity().multiply(0.35)
                    .add(delta.multiply(speed * 0.65 / distance));
            player.setVelocity(velocity);
            player.velocityModified = true;
            player.fallDistance = 0.0F;

            drawTether(zip.world, position, zip.anchor);
            if (zip.ticks % 5 == 0) {
                zip.world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 0.5F, 1.6F);
            }
        }
    }

    private static void drawTether(ServerWorld world, Vec3d from, Vec3d to) {
        int points = (int) Math.min(14, Math.max(3, from.distanceTo(to)));
        for (int i = 0; i < points; i++) {
            double f = (i + world.getRandom().nextDouble()) / points;
            Vec3d point = from.lerp(to, f);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void tickFallGrace(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = FALL_GRACE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }
            player.fallDistance = 0.0F;
            int remaining = entry.getValue() - 1;
            boolean landed = player.isOnGround() && remaining < GRACE_TICKS - 5;
            if (remaining <= 0 || landed) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private static void release(ServerPlayerEntity player, boolean arrived) {
        FALL_GRACE.put(player.getUuid(), GRACE_TICKS);
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                arrived ? SoundEvents.BLOCK_CHAIN_BREAK : SoundEvents.BLOCK_CHAIN_FALL,
                SoundCategory.PLAYERS, 0.9F, arrived ? 1.3F : 0.8F);
    }
}
