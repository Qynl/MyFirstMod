package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
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
    private static final BlockPos CENTER = new BlockPos(0, 80, 0);

    private static WardenEntity boss;
    private static ServerBossBar bar;
    private static int ticks;
    private static int phase = 1;
    private static int intro;
    private static int attackWindup;
    private static int attackType;
    private static int arenaPulse;
    private static boolean defeated;

    private NullWardenManager() {}

    public static void enterArena(ServerWorld world, ServerPlayerEntity player) {
        if (defeated) {
            buildReturnPortal(world);
            return;
        }

        if (boss != null && boss.isAlive()) {
            if (bar != null) bar.addPlayer(player);
            return;
        }

        buildArena(world);

        boss = EntityType.WARDEN.create(world);
        if (boss == null) return;

        var maxHealth = boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(500.0);

        var attackDamage = boss.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(18.0);

        var movement = boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) movement.setBaseValue(0.38);

        boss.setHealth(500.0F);
        boss.refreshPositionAndAngles(0.5, 81, 0.5, 180, 0);
        boss.setCustomName(Text.literal("THE NULL WARDEN"));
        boss.setCustomNameVisible(false);
        boss.setAi(false);
        world.spawnEntity(boss);

        bar = new ServerBossBar(Text.literal("THE NULL WARDEN"), ServerBossBar.Color.PURPLE, ServerBossBar.Style.NOTCHED_20);
        bar.setDarkenSky(true);
        bar.setThickenFog(true);
        bar.addPlayer(player);

        ticks = 0;
        phase = 1;
        intro = 100;
        attackWindup = 0;
        attackType = 0;
        arenaPulse = 0;

        world.playSound(null, CENTER, SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 5.0F, 0.5F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, 0.5, 82, 0.5, 180, 4, 2, 4, 0.04);
        player.sendMessage(Text.literal("THE NULL WARDEN"), false);
        player.sendMessage(Text.literal("The city was never meant to open this door."), false);
    }

    public static void tick(ServerWorld world) {
        if (boss == null) return;

        if (!boss.isAlive() && !defeated) {
            finish(world);
            return;
        }
        if (defeated) {
            buildReturnPortal(world);
            return;
        }

        ticks++;

        if (intro > 0) {
            intro--;
            boss.setAi(false);
            boss.setInvulnerable(true);
            boss.setVelocity(Vec3d.ZERO);

            if (intro % 10 == 0) {
                world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 2.0F, 0.5F + intro / 250.0F);
                world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), 24, 1.3, 1.2, 1.3, 0.02);
            }

            if (intro == 0) {
                boss.setInvulnerable(false);
                boss.setAi(true);
                world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 4.0F, 0.55F);
            }
            return;
        }

        boss.setAi(true);

        float hp = boss.getHealth() / boss.getMaxHealth();
        if (bar != null) bar.setPercent(Math.max(0.0F, hp));

        int newPhase = hp > 0.75F ? 1 : hp > 0.50F ? 2 : hp > 0.20F ? 3 : 4;
        if (newPhase != phase) {
            phase = newPhase;
            attackWindup = 0;
            phaseShift(world);
        }

        if (ticks % 3 == 0) {
            int amount = phase == 4 ? 12 : phase >= 3 ? 8 : 4;
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), amount, .65, .9, .65, .02);
        }

        arenaPulse++;
        if (arenaPulse >= (phase == 4 ? 90 : 150)) {
            arenaPulse = 0;
            arenaEvent(world);
        }

        if (attackWindup > 0) {
            attackWindup--;
            telegraph(world);
            if (attackWindup == 0) resolveAttack(world);
        } else {
            int interval = switch (phase) {
                case 1 -> 90;
                case 2 -> 72;
                case 3 -> 58;
                default -> 42;
            };
            if (ticks % interval == 0) beginAttack(world);
        }
    }

    private static void phaseShift(ServerWorld world) {
        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 3.5F, 0.45F);
        world.spawnParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 1, boss.getZ(), 14, 1.5, 1.5, 1.5, .04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, boss.getX(), boss.getY() + 1, boss.getZ(), 160, 3, 2, 3, .06);

        for (ServerPlayerEntity p : world.getPlayers()) {
            p.sendMessage(Text.literal("NULL WARDEN // PHASE " + phase), true);
        }

        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, Math.cos(a) * 15.0, 82, Math.sin(a) * 15.0, 18, .4, .4, .4, .03);
        }
    }

    private static void beginAttack(ServerWorld world) {
        if (nearest(world) == null) return;

        attackType = switch (phase) {
            case 1 -> 1;
            case 2 -> 2 + (ticks / 72) % 2;
            case 3 -> 4 + (ticks / 58) % 2;
            default -> 6 + (ticks / 42) % 3;
        };

        attackWindup = switch (attackType) {
            case 1 -> 24;
            case 2, 3 -> 28;
            case 4, 5 -> 24;
            case 6, 7 -> 20;
            default -> 30;
        };

        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 2.5F, 0.75F);
    }

    private static void telegraph(ServerWorld world) {
        ServerPlayerEntity target = nearest(world);
        if (target == null) return;

        double radius = switch (attackType) {
            case 1 -> 3.0;
            case 2, 4 -> 4.0;
            case 3, 5 -> 6.0;
            case 6, 7 -> 7.0;
            default -> 8.0;
        };

        int points = 48;
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            world.spawnParticles(
                    new DustParticleEffect(0x6A22CC, attackWindup < 7 ? 1.8F : 1.1F),
                    target.getX() + Math.cos(a) * radius,
                    target.getY() + 0.08,
                    target.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0
            );
        }

        world.spawnParticles(
                attackWindup < 7 ? ParticleTypes.EXPLOSION : ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 0.4, target.getZ(),
                attackWindup < 7 ? 3 : 12, .5, .15, .5, .02
        );
    }

    private static void resolveAttack(ServerWorld world) {
        ServerPlayerEntity target = nearest(world);
        if (target == null || boss == null) return;

        switch (attackType) {
            case 1 -> {
                damageIfClose(world, target, 3.0, 10);
                shockwave(world, target, 3.0);
            }
            case 2 -> {
                damageIfClose(world, target, 4.0, 13);
                shockwave(world, target, 4.0);
            }
            case 3 -> {
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (p.squaredDistanceTo(target) < 49) p.damage(world.getDamageSources().mobAttack(boss), 8);
                }
                shockwave(world, target, 7.0);
            }
            case 4 -> {
                damageIfClose(world, target, 4.0, 16);
                boss.teleport(target.getX(), target.getY(), target.getZ(), false);
                shockwave(world, target, 3.5);
            }
            case 5 -> {
                damageIfClose(world, target, 7.0, 18);
                gravityPulse(world, 0.55);
            }
            case 6 -> {
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (p.squaredDistanceTo(boss) < 100) p.damage(world.getDamageSources().mobAttack(boss), 12);
                }
                shockwave(world, target, 8.0);
            }
            case 7 -> gravityPulse(world, 0.85);
            default -> {
                damageIfClose(world, target, 9.0, 22);
                shockwave(world, target, 9.0);
            }
        }
    }

    private static void damageIfClose(ServerWorld world, ServerPlayerEntity target, double radius, float damage) {
        if (boss.squaredDistanceTo(target) <= radius * radius) {
            target.damage(world.getDamageSources().mobAttack(boss), damage);
        }
    }

    private static void shockwave(ServerWorld world, ServerPlayerEntity center, double radius) {
        for (int i = 0; i < 80; i++) {
            double a = i * Math.PI * 2 / 80.0;
            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    center.getX() + Math.cos(a) * radius,
                    center.getY() + 0.15,
                    center.getZ() + Math.sin(a) * radius,
                    1, 0, 0, 0, 0
            );
        }
    }

    private static void gravityPulse(ServerWorld world, double strength) {
        for (ServerPlayerEntity p : world.getPlayers()) {
            Vec3d delta = boss.getPos().subtract(p.getPos());
            double length = delta.length();
            if (length > 0.1 && length < 15) {
                p.addVelocity(delta.normalize().multiply(strength));
                p.velocityModified = true;
            }
        }
    }

    private static void arenaEvent(ServerWorld world) {
        if (phase >= 2) {
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0 + ticks * 0.01;
                world.spawnParticles(ParticleTypes.SCULK_SOUL, Math.cos(a) * 15, 81, Math.sin(a) * 15, 10, .3, .2, .3, .02);
            }
        }

        if (phase >= 3) {
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2.0 + ticks * 0.02;
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL, Math.cos(a) * 10, 82, Math.sin(a) * 10, 20, .5, .4, .5, .03);
            }
        }
    }

    private static void summonEcho(ServerWorld world) {
        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;

        double a = ticks * 0.045;
        echo.refreshPositionAndAngles(Math.cos(a) * 11, 81, Math.sin(a) * 11, 0, 0);
        echo.setCustomName(Text.literal("NULL ECHO"));
        echo.setHealth(65.0F);
        var max = echo.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (max != null) max.setBaseValue(65.0);
        var damage = echo.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(8.0);
        world.spawnEntity(echo);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, echo.getX(), echo.getY() + 1, echo.getZ(), 90, .8, 1, .8, .06);
    }

    private static ServerPlayerEntity nearest(ServerWorld world) {
        if (boss == null) return null;

        ServerPlayerEntity best = null;
        double distance = Double.MAX_VALUE;
        for (ServerPlayerEntity p : world.getPlayers()) {
            double d = p.squaredDistanceTo(boss);
            if (d < distance) {
                distance = d;
                best = p;
            }
        }
        return best;
    }

    private static void finish(ServerWorld world) {
        if (defeated) return;

        defeated = true;
        attackWindup = 0;
        boss.setHealth(1.0F);
        boss.setInvulnerable(true);
        boss.setAi(false);

        if (bar != null) bar.setVisible(false);

        world.playSound(null, boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 5.0F, .45F);

        for (int i = 0; i < 6; i++) {
            world.spawnParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 1, boss.getZ(), 8, 1.5, 1.5, 1.5, .05);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, boss.getX(), boss.getY() + 1, boss.getZ(), 80, 3, 2, 3, .08);
        }

        for (ServerPlayerEntity p : world.getPlayers()) {
            p.getInventory().offerOrDrop(new ItemStack(ModItems.NULLBLADE));
            p.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
            p.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"), false);
            p.sendMessage(Text.literal("The Nullblade answers to you now."), false);
        }

        buildReturnPortal(world);
    }

    private static void buildArena(ServerWorld world) {
        for (int x = -22; x <= 22; x++) {
            for (int z = -22; z <= 22; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r <= 22) {
                    world.setBlockState(new BlockPos(x, 80, z),
                            r < 17 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : Blocks.CRYING_OBSIDIAN.getDefaultState());

                    if (r < 14 && (Math.abs(x) + Math.abs(z)) % 7 == 0) {
                        world.setBlockState(new BlockPos(x, 81, z), Blocks.SCULK.getDefaultState());
                    }
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int x = (int) Math.round(Math.cos(a) * 15);
            int z = (int) Math.round(Math.sin(a) * 15);

            for (int y = 81; y < 92; y++) {
                world.setBlockState(new BlockPos(x, y, z), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                if (Math.abs(x) % 2 == 0) world.setBlockState(new BlockPos(x + 1, y, z), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }

            world.setBlockState(new BlockPos(x, 92, z), Blocks.SCULK_CATALYST.getDefaultState());
        }

        for (int y = 81; y < 96; y++) {
            world.setBlockState(new BlockPos(-22, y, 0), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(22, y, 0), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0, y, -22), Blocks.BLACKSTONE.getDefaultState());
            world.setBlockState(new BlockPos(0, y, 22), Blocks.BLACKSTONE.getDefaultState());
        }

        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI * 2 / 32.0;
            world.setBlockState(
                    new BlockPos((int) Math.round(Math.cos(a) * 9), 81, (int) Math.round(Math.sin(a) * 9)),
                    Blocks.CRYING_OBSIDIAN.getDefaultState()
            );
        }
    }

    private static void buildReturnPortal(ServerWorld world) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(new BlockPos(13 + x, 81 + y, 13), ModBlocks.VOID_PORTAL.getDefaultState());
            }
        }

        world.spawnParticles(ParticleTypes.PORTAL, 13.5, 83, 13.5, 160, 1.5, 2.2, 1.5, .12);
    }
}
