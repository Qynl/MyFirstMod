package dev.qynl.myfirstmod.portal;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class VoidPortalManager {
    public static final RegistryKey<World> NULL_REALM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MyFirstMod.MOD_ID, "null_realm"));
    private static final Map<java.util.UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<java.util.UUID, BlockPos> RETURN_POINTS = new HashMap<>();

    public static boolean tryIgnite(ServerPlayerEntity player, BlockPos clicked) {
        ServerWorld world = player.getServerWorld();
        if (!nearSculk(world, clicked, 14)) return false;

        // A deliberately recognizable ancient-city style 4x5 seal.
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 3; x++) {
                boolean frame = y == 0 || y == 4 || x == 0 || x == 3;
                if (frame && !world.getBlockState(clicked.add(x, y, 0)).isOf(Blocks.REINFORCED_DEEPSLATE)) return false;
            }
        }

        for (int y = 1; y <= 3; y++) {
            for (int x = 1; x <= 2; x++) {
                world.setBlockState(clicked.add(x, y, 0), ModBlocks.VOID_PORTAL.getDefaultState());
            }
        }

        world.playSound(null, clicked, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.5f, 0.55f);
        world.spawnParticles(ParticleTypes.PORTAL, clicked.getX() + 2.0, clicked.getY() + 2.5, clicked.getZ() + 0.5,
                120, 1.4, 2.0, 0.4, 0.18);
        player.sendMessage(Text.literal("The ancient seal opens..."), true);
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

        COOLDOWNS.put(player.getUuid(), 80);

        if (player.getServerWorld().getRegistryKey().equals(NULL_REALM)) {
            returnPlayer(player);
            return;
        }

        RETURN_POINTS.put(player.getUuid(), player.getBlockPos());
        ServerWorld target = server.getWorld(NULL_REALM);
        if (target == null) return;
        player.teleport(target, 0.5, 82, 0.5, player.getYaw(), player.getPitch());
        NullWardenManager.enterArena(target, player);
    }

    public static void tick(MinecraftServer server) {
        COOLDOWNS.replaceAll((uuid, value) -> Math.max(0, value - 1));
        COOLDOWNS.entrySet().removeIf(e -> e.getValue() == 0);
        ServerWorld realm = server.getWorld(NULL_REALM);
        if (realm != null) NullWardenManager.tick(realm);
    }

    public static void returnPlayer(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        BlockPos spawn = player.getSpawnPointPosition();
        if (spawn == null) spawn = overworld.getSpawnPos();
        player.teleport(overworld, spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal("The gate closes behind you."), true);
    }
}
