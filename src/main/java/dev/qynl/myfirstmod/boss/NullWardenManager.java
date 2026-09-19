package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class NullWardenManager {
    private static final UUID BOSS_ID = UUID.fromString("8a0c7d4d-4e9d-4d91-8b3d-9f0d0f8c4b01");
    private static WardenEntity boss;
    private static ServerBossBar bar;
    private static int ticks;
    private static int phase = 1;
    private static boolean defeated;

    public static void enterArena(ServerWorld world, ServerPlayerEntity player) {
        if (defeated) {
            buildReturnPortal(world);
            return;
        }
        if (boss != null && boss.isAlive()) {
            bar.addPlayer(player);
            return;
        }
        buildArena(world);
        boss = EntityType.WARDEN.create(world);
        if (boss == null) return;
        boss.refreshPositionAndAngles(0.5, 83, 0.5, 180, 0);
        boss.setCustomName(Text.literal("THE NULL WARDEN"));
        boss.setCustomNameVisible(true);
        boss.setHealth(boss.getMaxHealth());
        world.spawnEntity(boss);

        bar = new ServerBossBar(Text.literal("THE NULL WARDEN"), BossBar.Color.PURPLE, BossBar.Style.NOTCHED_10);
        bar.setDarkenSky(true);
        bar.setThickenFog(true);
        bar.addPlayer(player);
        ticks = 0;
        phase = 1;
        world.playSound(null, new BlockPos(0, 82, 0), SoundEvents.ENTITY_WARDEN_EMERGE, net.minecraft.sound.SoundCategory.HOSTILE, 3f, 0.65f);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, 0.5, 83, 0.5, 1, 0, 0, 0, 0);
    }

    public static void tick(ServerWorld world) {
        if (boss == null || !boss.isAlive()) {
            if (bar != null && !defeated) bar.clearPlayers();
            return;
        }
        ticks++;
        float hp = boss.getHealth() / boss.getMaxHealth();
        bar.setPercent(Math.max(0, hp));
        if (hp <= 0.75f && phase == 1) phase = 2;
        if (hp <= 0.50f && phase == 2) phase = 3;
        if (hp <= 0.20f && phase == 3) phase = 4;

        if (ticks % 8 == 0) {
            world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY()+1.0, boss.getZ(), 8, 0.8, 1.0, 0.8, 0.02);
        }

        if (ticks % (phase == 1 ? 100 : phase == 2 ? 82 : phase == 3 ? 68 : 52) == 0) {
            voidPulse(world);
        }

        if (ticks % 240 == 0 && phase >= 2) {
            summonEcho(world);
        }

        if (hp <= 0.01f) {
            finish(world);
        }
    }

    private static void voidPulse(ServerWorld world) {
        double radius = phase >= 4 ? 7 : phase >= 3 ? 6 : 5;
        world.spawnParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY()+0.2, boss.getZ(), 80, radius/2, 0.4, radius/2, 0.12);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(boss) <= radius*radius) {
                player.damage(world.getDamageSources().mobAttack(boss), phase >= 3 ? 8.0f : 5.0f);
            }
        }
        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, net.minecraft.sound.SoundCategory.HOSTILE, 1.6f, phase == 4 ? 0.55f : 0.8f);
    }

    private static void summonEcho(ServerWorld world) {
        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;
        double angle = (ticks / 20.0) % (Math.PI * 2);
        echo.refreshPositionAndAngles(Math.cos(angle)*9, 82, Math.sin(angle)*9, 0, 0);
        echo.setCustomName(Text.literal("VOID ECHO"));
        echo.setHealth(Math.min(echo.getMaxHealth(), 150));
        world.spawnEntity(echo);
        world.spawnParticles(ParticleTypes.PORTAL, echo.getX(), echo.getY()+1, echo.getZ(), 50, 0.5, 1, 0.5, 0.08);
    }

    private static void finish(ServerWorld world) {
        if (defeated) return;
        defeated = true;
        boss.setHealth(1);
        boss.setInvulnerable(true);
        boss.setAi(false);
        if (bar != null) bar.setVisible(false);
        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_DEATH, net.minecraft.sound.SoundCategory.HOSTILE, 3f, 0.6f);
        world.spawnParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY()+1, boss.getZ(), 20, 1.5, 1.5, 1.5, 0.1);
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
            player.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"), false);
        }
        buildReturnPortal(world);
    }

    private static void buildArena(ServerWorld world) {
        for (int x=-16;x<=16;x++) for(int z=-16;z<=16;z++) {
            double r=Math.sqrt(x*x+z*z);
            if(r<=16) world.setBlockState(new BlockPos(x,80,z), r<14 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.CRYING_OBSIDIAN.getDefaultState());
        }
        for(int i=0;i<4;i++){
            double a=i*Math.PI/2;
            int x=(int)Math.round(Math.cos(a)*12), z=(int)Math.round(Math.sin(a)*12);
            for(int y=81;y<90;y++) world.setBlockState(new BlockPos(x,y,z), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
            world.setBlockState(new BlockPos(x,90,z), Blocks.SCULK_CATALYST.getDefaultState());
        }
        for(int y=81;y<96;y++) {
            world.setBlockState(new BlockPos(-16,y,0), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(16,y,0), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0,y,-16), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0,y,16), Blocks.BLACKSTONE.getDefaultState());
        }
    }

    private static void buildReturnPortal(ServerWorld world) {
        for(int x=-1;x<=1;x++) for(int y=0;y<3;y++) {
            world.setBlockState(new BlockPos(10+x,81+y,10), ModBlocks.VOID_PORTAL.getDefaultState());
        }
    }
}
