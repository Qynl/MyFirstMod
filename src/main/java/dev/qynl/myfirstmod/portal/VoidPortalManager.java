package dev.qynl.myfirstmod.portal;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class VoidPortalManager {
    public static final RegistryKey<World> NULL_REALM = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(MyFirstMod.MOD_ID, "null_realm"));
    private static final Map<java.util.UUID, Integer> COOLDOWNS = new HashMap<>();

    public static boolean tryIgnite(ServerPlayerEntity player, BlockPos clicked) {
        if (!nearSculk(player.getServerWorld(), clicked, 12)) return false;
        for (int w = -1; w <= 1; w++) {
            for (int h = 0; h < 3; h++) {
                BlockPos p = clicked.up(h).east(w);
                if (!player.getServerWorld().getBlockState(p).isOf(Blocks.REINFORCED_DEEPSLATE)) return false;
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 3; y++) {
                player.getServerWorld().setBlockState(clicked.up(y).east(x), ModBlocks.VOID_PORTAL.getDefaultState());
            }
        }
        player.getServerWorld().playSound(null, clicked, net.minecraft.sound.SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, net.minecraft.sound.SoundCategory.BLOCKS, 1.2f, 0.65f);
        player.getServerWorld().spawnParticles(ParticleTypes.PORTAL, clicked.getX()+0.5, clicked.getY()+1.0, clicked.getZ()+0.5, 80, 1.2, 1.2, 1.2, 0.15);
        player.sendMessage(Text.literal("The ancient seal opens..."), true);
        return true;
    }

    private static boolean nearSculk(ServerWorld world, BlockPos center, int radius) {
        for (BlockPos p : BlockPos.iterateOutwards(center, radius, 3, radius)) {
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
        ServerWorld target = server.getWorld(NULL_REALM);
        if (target == null) return;
        COOLDOWNS.put(player.getUuid(), 80);
        player.teleport(target, 0.5, 80, 0.5, player.getYaw(), player.getPitch());
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
        player.teleport(overworld, player.getSpawnPointPosition() != null ? player.getSpawnPointPosition().getX() + 0.5 : 0.5,
                player.getSpawnPointPosition() != null ? player.getSpawnPointPosition().getY() + 1 : 80,
                player.getSpawnPointPosition() != null ? player.getSpawnPointPosition().getZ() + 0.5 : 0.5,
                player.getYaw(), player.getPitch());
    }
}
