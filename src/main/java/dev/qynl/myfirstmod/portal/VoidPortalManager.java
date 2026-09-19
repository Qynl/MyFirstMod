package dev.qynl.myfirstmod.portal;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.boss.NullWardenState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VoidPortalManager {
    public static final RegistryKey<World> NULL_REALM =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MyFirstMod.MOD_ID, "null_realm"));

    private static final int TELEPORT_COOLDOWN = 80;
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private VoidPortalManager() {}

    public static boolean tryIgnite(ServerPlayerEntity player, BlockPos clicked) {
        ServerWorld world = player.getServerWorld();

        if (!nearSculk(world, clicked, 14) || !validSeal(world, clicked)) {
            player.sendMessage(Text.literal("The seal is incomplete."), true);
            return false;
        }

        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 2; x++) {
                world.setBlockState(clicked.add(x, y, 0), ModBlocks.VOID_PORTAL.getDefaultState());
            }
        }

        world.playSound(null, clicked, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.BLOCKS, 1.6F, 0.55F);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                clicked.getX() + 2.0, clicked.getY() + 2.5, clicked.getZ() + 0.5,
                180, 1.4, 2.0, 0.4, 0.12);
        player.sendMessage(Text.literal("The ancient seal opens..."), true);
        return true;
    }

    private static boolean validSeal(ServerWorld world, BlockPos origin) {
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                boolean frame = y == 0 || y == 4 || x == 0 || x == 3;
                if (frame && !world.getBlockState(origin.add(x, y, 0))
                        .isOf(Blocks.REINFORCED_DEEPSLATE)) {
                    return false;
                }
            }
        }

        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 2; x++) {
                if (!world.getBlockState(origin.add(x, y, 0)).isAir()
                        && !world.getBlockState(origin.add(x, y, 0)).isOf(ModBlocks.VOID_PORTAL)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean nearSculk(ServerWorld world, BlockPos center, int radius) {
        for (BlockPos p : BlockPos.iterateOutwards(center, radius, 5, radius)) {
            if (world.getBlockState(p).isOf(Blocks.SCULK_CATALYST)) return true;
        }
        return false;
    }

    public static void tryEnter(Entity entity) {
        if (!(entity instanceof ServerPlayerEntity player)) return;

        int cooldown = COOLDOWNS.getOrDefault(player.getUuid(), 0);
        if (cooldown > 0) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (player.getServerWorld().getRegistryKey().equals(NULL_REALM)) {
            COOLDOWNS.put(player.getUuid(), TELEPORT_COOLDOWN);
            returnPlayer(player);
            return;
        }

        ServerWorld target = server.getWorld(NULL_REALM);
        if (target == null) {
            player.sendMessage(Text.literal("The Null Realm cannot be reached."), true);
            return;
        }

        COOLDOWNS.put(player.getUuid(), TELEPORT_COOLDOWN);

        BlockPos returnPos = player.getBlockPos();
        RegistryKey<World> returnWorld = player.getServerWorld().getRegistryKey();
        float returnYaw = player.getYaw();
        float returnPitch = player.getPitch();

        // Return data is owned by the Null Realm encounter state, not the
        // origin world's state. This makes returning work across dimensions
        // and survives a server restart.
        NullWardenManager.saveReturnPoint(
                player, returnWorld, returnPos, returnYaw, returnPitch
        );

        dev.qynl.myfirstmod.realm.RealmExpedition.prepare(target);
        player.teleport(target, 0.5, 81.0, 160.5, 180, 0);
        dev.qynl.myfirstmod.realm.RealmExpedition.welcome(player);
    }

    public static void clear() { COOLDOWNS.clear(); }

    public static void tick(MinecraftServer server) {
        COOLDOWNS.replaceAll((uuid, value) -> Math.max(0, value - 1));
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() == 0);

        NullWardenManager.tick(server);
    }

    public static void returnPlayer(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        NullWardenState.ReturnPointData point = NullWardenManager.takeReturnPoint(player);

        ServerWorld destination = server.getOverworld();
        if (point != null) {
            Identifier id = Identifier.tryParse(point.worldId());
            if (id != null) {
                ServerWorld stored = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
                if (stored != null) destination = stored;
            }
        }

        BlockPos spawn = point == null
                ? destination.getSpawnPos()
                : new BlockPos(point.x(), point.y(), point.z());

        player.teleport(destination,
                spawn.getX() + 0.5,
                spawn.getY() + 1.0,
                spawn.getZ() + 0.5,
                point == null ? player.getYaw() : point.yaw(),
                point == null ? player.getPitch() : point.pitch());

        player.sendMessage(Text.literal("The gate closes behind you."), true);
    }
}
