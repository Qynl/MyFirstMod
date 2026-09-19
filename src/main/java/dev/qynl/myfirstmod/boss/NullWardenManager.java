package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class NullWardenManager {
    private static final UUID BOSS_ID = UUID.fromString("8a0c7d4d-4e9d-4d91-8b3d-9f0d0f8c4b01");
    private static WardenEntity boss;
    private static ServerBossBar bar;
    private static int ticks;
    private static int phase = 1;
    private static boolean defeated;
    private static int intro = 0;

    private NullWardenManager() {}

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

        boss.refreshPositionAndAngles(0.5, 84, 0.5, 180, 0);
        boss.setCustomName(Text.literal("THE NULL WARDEN"));
        boss.setCustomNameVisible(false);
        boss.setHealth(boss.getMaxHealth());
        world.spawnEntity(boss);

        bar = new ServerBossBar(Text.literal("THE NULL WARDEN"), ServerBossBar.Color.PURPLE, ServerBossBar.Style.NOTCHED_10);
        bar.setDarkenSky(true);
        bar.setThickenFog(true);
        bar.addPlayer(player);

        ticks = 0;
        phase = 1;
        intro = 100;

        world.playSound(null, new BlockPos(0, 83, 0), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 4f, 0.55f);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, 0.5, 84, 0.5, 140, 3, 1.5, 3, 0.04);
    }

    public static void tick(ServerWorld world) {
        if (boss == null) return;

        if (!boss.isAlive() && !defeated) {
            finish(world);
            return;
        }
        if (defeated) return;

        ticks++;
        if (intro > 0) {
            intro--;
            boss.setAi(false);
            boss.setInvulnerable(true);
            boss.setVelocity(0, 0, 0);
            if (intro % 8 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), 18, 1.2, 1.4, 1.2, 0.03);
            }
            if (intro == 1) {
                boss.setInvulnerable(false);
                boss.setAi(true);
                world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 3f, 0.6f);
            }
        }

        float hp = boss.getHealth() / boss.getMaxHealth();
        bar.setPercent(Math.max(0, hp));

        int newPhase = hp > .75f ? 1 : hp > .50f ? 2 : hp > .20f ? 3 : 4;
        if (newPhase != phase) {
            phase = newPhase;
            phaseShift(world);
        }

        if (ticks % 4 == 0) {
            world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), phase >= 3 ? 10 : 5, .65, .9, .65, .015);
        }

        int interval = switch (phase) {
            case 1 -> 105;
            case 2 -> 82;
            case 3 -> 65;
            default -> 48;
        };
        if (ticks % interval == 0) ability(world);

        if (phase >= 2 && ticks % 240 == 0) summonEcho(world);
        if (phase == 4 && ticks % 180 == 0) gravityPulse(world);
    }

    private static void phaseShift(ServerWorld world) {
        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 2.5f, 0.55f);
        world.spawnParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 1, boss.getZ(), 8, 1, 1, 1, .03);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), 100, 2.5, 2, 2.5, .06);
        for (ServerPlayerEntity p : world.getPlayers()) {
            p.sendMessage(Text.literal("THE NULL WARDEN • PHASE " + phase), true);
        }
    }

    private static void ability(ServerWorld world) {
        if (boss == null) return;
        ServerPlayerEntity target = nearest(world);
        if (target == null) return;

        if (phase == 1) {
            telegraph(world, target, 2.5, 25);
            target.damage(world.getDamageSources().mobAttack(boss), 7);
        } else if (phase == 2) {
            ring(world, target, 3.5);
            target.damage(world.getDamageSources().mobAttack(boss), 10);
        } else if (phase == 3) {
            telegraph(world, target, 4.5, 18);
            target.damage(world.getDamageSources().mobAttack(boss), 13);
            if (boss.squaredDistanceTo(target) > 64) boss.teleport(target.getX(), target.getY(), target.getZ(), false);
        } else {
            ring(world, target, 6);
            target.damage(world.getDamageSources().mobAttack(boss), 16);
        }
    }

    private static void telegraph(ServerWorld world, PlayerEntity target, double radius, int duration) {
        BlockPos center = target.getBlockPos();
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI * 2 / 36;
            world.spawnParticles(new DustParticleEffect(0x6A22CC, 1.3f),
                    center.getX()+0.5+Math.cos(a)*radius, center.getY()+0.05,
                    center.getZ()+0.5+Math.sin(a)*radius, 1, 0, 0, 0, 0);
        }
        if (duration > 0) world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.getX()+.5, center.getY()+.4, center.getZ()+.5, 40, radius/2, .2, radius/2, .04);
    }

    private static void ring(ServerWorld world, PlayerEntity target, double radius) {
        BlockPos center=target.getBlockPos();
        for(int i=0;i<64;i++){
            double a=i*Math.PI*2/64;
            world.spawnParticles(ParticleTypes.PORTAL, center.getX()+.5+Math.cos(a)*radius, center.getY()+.1, center.getZ()+.5+Math.sin(a)*radius,1,0,0,0,0);
        }
    }

    private static void gravityPulse(ServerWorld world) {
        for (ServerPlayerEntity p : world.getPlayers()) {
            Vec3d d = boss.getPos().subtract(p.getPos());
            if (d.length() < 14) p.addVelocity(d.normalize().multiply(.35));
        }
    }

    private static void summonEcho(ServerWorld world) {
        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;
        double a=(ticks/20.0)%(Math.PI*2);
        echo.refreshPositionAndAngles(Math.cos(a)*9,83,Math.sin(a)*9,0,0);
        echo.setCustomName(Text.literal("VOID ECHO"));
        echo.setHealth(120);
        world.spawnEntity(echo);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, echo.getX(), echo.getY()+1, echo.getZ(), 80, .7, 1, .7, .08);
    }

    private static ServerPlayerEntity nearest(ServerWorld world) {
        ServerPlayerEntity best=null;
        double distance=Double.MAX_VALUE;
        for(ServerPlayerEntity p:world.getPlayers()){
            double d=p.squaredDistanceTo(boss);
            if(d<distance){distance=d;best=p;}
        }
        return best;
    }

    private static void finish(ServerWorld world) {
        if (defeated) return;
        defeated=true;
        boss.setHealth(1);
        boss.setInvulnerable(true);
        boss.setAi(false);
        if(bar!=null) bar.setVisible(false);
        world.playSound(null,boss.getBlockPos(),SoundEvents.ENTITY_WARDEN_DEATH,SoundCategory.HOSTILE,4f,.55f);
        world.spawnParticles(ParticleTypes.EXPLOSION,boss.getX(),boss.getY()+1,boss.getZ(),28,2,2,2,.08);
        for(ServerPlayerEntity p:world.getPlayers()){
            p.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
            p.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"),false);
        }
        buildReturnPortal(world);
    }

    private static void buildArena(ServerWorld world) {
        for(int x=-22;x<=22;x++) for(int z=-22;z<=22;z++){
            double r=Math.sqrt(x*x+z*z);
            if(r<=22) world.setBlockState(new BlockPos(x,80,z), r<17?Blocks.POLISHED_BLACKSTONE.getDefaultState():Blocks.CRYING_OBSIDIAN.getDefaultState());
        }
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            int x=(int)Math.round(Math.cos(a)*15),z=(int)Math.round(Math.sin(a)*15);
            for(int y=81;y<91;y++) world.setBlockState(new BlockPos(x,y,z),Blocks.REINFORCED_DEEPSLATE.getDefaultState());
            world.setBlockState(new BlockPos(x,91,z),Blocks.SCULK_CATALYST.getDefaultState());
        }
        for(int y=81;y<97;y++){
            world.setBlockState(new BlockPos(-22,y,0),Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(22,y,0),Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0,y,-22),Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0,y,22),Blocks.BLACKSTONE.getDefaultState());
        }
    }

    private static void buildReturnPortal(ServerWorld world) {
        for(int x=-1;x<=1;x++) for(int y=0;y<4;y++)
            world.setBlockState(new BlockPos(13+x,81+y,13),dev.qynl.myfirstmod.block.ModBlocks.VOID_PORTAL.getDefaultState());
        world.spawnParticles(ParticleTypes.PORTAL,13.5,83,13.5,120,1.2,2,1.2,.12);
    }
}
