package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NullWardenManager {
    private static final Map<RegistryKey<World>, ArenaState> ARENAS = new HashMap<>();
    private static final BlockPos CENTER = new BlockPos(0, 80, 0);
    private static final int ARENA_RADIUS = 22;

    private NullWardenManager() {}

    public static void enterArena(ServerWorld world, ServerPlayerEntity player) {
        ArenaState arena = ARENAS.computeIfAbsent(world.getRegistryKey(), key -> new ArenaState());
        arena.participants.add(player.getUuid());

        if (arena.defeated) {
            buildReturnPortal(world);
            return;
        }

        if (arena.boss != null && arena.boss.isAlive()) {
            arena.bar.addPlayer(player);
            return;
        }

        startEncounter(world, player, arena);
    }

    public static void tick(MinecraftServer server) {
        ARENAS.entrySet().removeIf(entry -> server.getWorld(entry.getKey()) == null);

        for (Map.Entry<RegistryKey<World>, ArenaState> entry : ARENAS.entrySet()) {
            ServerWorld world = server.getWorld(entry.getKey());
            if (world != null) tickArena(server, world, entry.getValue());
        }
    }

    private static void startEncounter(ServerWorld world, ServerPlayerEntity player, ArenaState arena) {
        buildArena(world);

        arena.boss = EntityType.WARDEN.create(world);
        if (arena.boss == null) return;

        var maxHealth = arena.boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(500.0);

        var attackDamage = arena.boss.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage != null) attackDamage.setBaseValue(18.0);

        var movement = arena.boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movement != null) movement.setBaseValue(0.38);

        arena.boss.setHealth(500.0F);
        arena.boss.refreshPositionAndAngles(0.5, 81, 0.5, 180, 0);
        arena.boss.setCustomName(Text.literal("THE NULL WARDEN"));
        arena.boss.setCustomNameVisible(false);
        arena.boss.setAi(false);
        world.spawnEntity(arena.boss);

        arena.bar = new ServerBossBar(
                Text.literal("THE NULL WARDEN"),
                ServerBossBar.Color.PURPLE,
                ServerBossBar.Style.NOTCHED_20
        );
        arena.bar.setDarkenSky(true);
        arena.bar.setThickenFog(true);

        for (UUID uuid : arena.participants) {
            ServerPlayerEntity participant = world.getServer().getPlayerManager().getPlayer(uuid);
            if (participant != null && participant.getServerWorld() == world) {
                arena.bar.addPlayer(participant);
            }
        }

        arena.intro = 100;
        arena.phase = 1;
        arena.attack = Attack.NONE;
        arena.arenaPulse = 0;
        arena.idleTicks = 0;
        arena.defeated = false;
        arena.rewarded = false;

        world.playSound(null, CENTER, SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.HOSTILE, 5.0F, 0.5F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                0.5, 82, 0.5, 180, 4, 2, 4, 0.04);

        player.sendMessage(Text.literal("THE NULL WARDEN"), false);
        player.sendMessage(Text.literal("The city was never meant to open this door."), false);
    }

    private static void tickArena(MinecraftServer server, ServerWorld world, ArenaState arena) {
        if (arena.boss == null) return;

        updateParticipants(server, world, arena);

        if (arena.participants.isEmpty()) {
            arena.idleTicks++;
            if (arena.idleTicks >= 600) resetEncounter(world, arena);
            return;
        }
        arena.idleTicks = 0;

        if (arena.defeated) {
            if (arena.portalCooldown > 0) arena.portalCooldown--;
            if (arena.portalCooldown == 0) buildReturnPortal(world);
            return;
        }

        if (!arena.boss.isAlive()) {
            finish(world, arena);
            return;
        }

        arena.ticks++;

        if (arena.intro > 0) {
            arena.intro--;
            arena.boss.setAi(false);
            arena.boss.setInvulnerable(true);
            arena.boss.setVelocity(Vec3d.ZERO);

            if (arena.intro % 10 == 0) {
                world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                        SoundCategory.HOSTILE, 2.0F, 0.5F + arena.intro / 250.0F);
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                        24, 1.3, 1.2, 1.3, 0.02);
            }

            if (arena.intro == 0) {
                arena.boss.setInvulnerable(false);
                arena.boss.setAi(true);
                world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                        SoundCategory.HOSTILE, 4.0F, 0.55F);
            }
            return;
        }

        arena.boss.setAi(true);

        float hp = arena.boss.getHealth() / arena.boss.getMaxHealth();
        arena.bar.setPercent(Math.max(0.0F, hp));

        int newPhase = hp > 0.75F ? 1 : hp > 0.50F ? 2 : hp > 0.20F ? 3 : 4;
        if (newPhase != arena.phase) {
            arena.phase = newPhase;
            arena.attack = Attack.NONE;
            phaseShift(world, arena);
        }

        if (arena.ticks % 4 == 0) {
            int amount = arena.phase == 4 ? 12 : arena.phase >= 3 ? 8 : 4;
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                    amount, .65, .9, .65, .02);
        }

        arena.arenaPulse++;
        if (arena.arenaPulse >= (arena.phase == 4 ? 90 : 150)) {
            arena.arenaPulse = 0;
            arenaEvent(world, arena);
        }

        if (arena.attack != Attack.NONE) {
            arena.attack.windup--;
            telegraph(world, arena);
            if (arena.attack.windup <= 0) {
                resolveAttack(world, arena);
                arena.attack = Attack.NONE;
            }
        } else if (arena.ticks >= arena.nextAttackTick) {
            beginAttack(world, arena);
        }

        if (arena.phase >= 2 && arena.ticks % 240 == 0) summonEcho(world, arena);
    }

    private static void updateParticipants(MinecraftServer server, ServerWorld world, ArenaState arena) {
        Set<UUID> active = new HashSet<>();

        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world && player.isAlive()
                    && player.squaredDistanceTo(CENTER.getX() + .5, CENTER.getY() + 1, CENTER.getZ() + .5) <= 28 * 28) {
                active.add(uuid);
                if (arena.bar != null) arena.bar.addPlayer(player);
            } else if (arena.bar != null && player != null) {
                arena.bar.removePlayer(player);
            }
        }

        arena.participants.retainAll(active);
    }

    private static void beginAttack(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = target(world, arena);
        if (target == null) return;

        arena.attack = switch (arena.phase) {
            case 1 -> Attack.VOID_CLEAVE;
            case 2 -> arena.ticks % 2 == 0 ? Attack.SCULK_RING : Attack.VOID_RAIN;
            case 3 -> arena.ticks % 2 == 0 ? Attack.NULL_DASH : Attack.GRAVITY_WELL;
            default -> switch ((arena.ticks / 42) % 3) {
                case 0 -> Attack.REALITY_TEAR;
                case 1 -> Attack.GRAVITY_WELL;
                default -> Attack.COLLAPSE;
            };
        };

        arena.attack.windup = arena.attack.windupTicks;
        arena.nextAttackTick = arena.ticks + arena.attack.recoveryTicks + arena.attack.windupTicks;

        world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.HOSTILE, 2.5F, 0.75F);
    }

    private static void telegraph(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = target(world, arena);
        if (target == null) return;

        double radius = arena.attack.radius;
        int points = arena.attack == Attack.COLLAPSE ? 72 : 48;
        float size = arena.attack.windup < 7 ? 1.8F : 1.1F;

        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2.0 / points;
            world.spawnParticles(
                    new DustParticleEffect(0x6A22CC, size),
                    target.getX() + Math.cos(angle) * radius,
                    target.getY() + 0.08,
                    target.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0
            );
        }

        world.spawnParticles(
                arena.attack.windup < 7 ? ParticleTypes.EXPLOSION : ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 0.4, target.getZ(),
                arena.attack.windup < 7 ? 3 : 12, .5, .15, .5, .02
        );
    }

    private static void resolveAttack(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = target(world, arena);
        if (target == null || arena.boss == null) return;

        switch (arena.attack) {
            case VOID_CLEAVE -> {
                damageIfClose(world, arena, target, 3.0, 10);
                shockwave(world, target, 3.0);
            }
            case SCULK_RING -> {
                damageArea(world, arena, target, 4.0, 13);
                shockwave(world, target, 4.0);
            }
            case VOID_RAIN -> {
                damageArea(world, arena, target, 6.5, 9);
                shockwave(world, target, 7.0);
            }
            case NULL_DASH -> {
                damageIfClose(world, arena, target, 4.0, 16);
                arena.boss.teleport(target.getX(), target.getY(), target.getZ(), false);
                shockwave(world, target, 3.5);
            }
            case GRAVITY_WELL -> {
                damageIfClose(world, arena, target, 7.0, 12);
                gravityPulse(world, arena, 0.55);
            }
            case REALITY_TEAR -> {
                damageArea(world, arena, target, 8.0, 14);
                shockwave(world, target, 8.0);
            }
            case COLLAPSE -> {
                damageArea(world, arena, target, 9.0, 20);
                gravityPulse(world, arena, 0.85);
                shockwave(world, target, 9.0);
            }
            default -> {}
        }
    }

    private static void damageIfClose(ServerWorld world, ArenaState arena, ServerPlayerEntity target, double radius, float damage) {
        if (arena.boss.squaredDistanceTo(target) <= radius * radius) {
            target.damage(world.getDamageSources().mobAttack(arena.boss), damage);
        }
    }

    private static void damageArea(ServerWorld world, ArenaState arena, ServerPlayerEntity center, double radius, float damage) {
        double radiusSq = radius * radius;
        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.squaredDistanceTo(center) <= radiusSq) {
                player.damage(world.getDamageSources().mobAttack(arena.boss), damage);
            }
        }
    }

    private static void shockwave(ServerWorld world, ServerPlayerEntity center, double radius) {
        for (int i = 0; i < 80; i++) {
            double angle = i * Math.PI * 2.0 / 80.0;
            world.spawnParticles(
                    ParticleTypes.PORTAL,
                    center.getX() + Math.cos(angle) * radius,
                    center.getY() + 0.15,
                    center.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0
            );
        }
    }

    private static void gravityPulse(ServerWorld world, ArenaState arena, double strength) {
        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            Vec3d delta = arena.boss.getPos().subtract(player.getPos());
            double length = delta.length();
            if (length > 0.1 && length < 15) {
                player.addVelocity(delta.normalize().multiply(strength));
                player.velocityModified = true;
            }
        }
    }

    private static void phaseShift(ServerWorld world, ArenaState arena) {
        world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                SoundCategory.HOSTILE, 3.5F, 0.45F);
        world.spawnParticles(ParticleTypes.EXPLOSION,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                14, 1.5, 1.5, 1.5, .04);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                160, 3, 2, 3, .06);

        for (ServerPlayerEntity player : participants(world, arena)) {
            player.sendMessage(Text.literal("NULL WARDEN // PHASE " + arena.phase), true);
        }
    }

    private static void arenaEvent(ServerWorld world, ArenaState arena) {
        if (arena.phase >= 2) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0 + arena.ticks * 0.01;
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        Math.cos(angle) * 15, 81, Math.sin(angle) * 15,
                        10, .3, .2, .3, .02);
            }
        }

        if (arena.phase >= 3) {
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2.0 + arena.ticks * 0.02;
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        Math.cos(angle) * 10, 82, Math.sin(angle) * 10,
                        20, .5, .4, .5, .03);
            }
        }
    }

    private static void summonEcho(ServerWorld world, ArenaState arena) {
        long echoCount = world.getEntitiesByType(EntityType.WARDEN, entity ->
                entity != arena.boss && entity.isAlive()).size();

        if (echoCount >= Math.min(3, arena.phase)) return;

        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;

        double angle = arena.ticks * 0.045;
        echo.refreshPositionAndAngles(
                Math.cos(angle) * 11, 81, Math.sin(angle) * 11, 0, 0
        );
        echo.setCustomName(Text.literal("NULL ECHO"));
        echo.setCustomNameVisible(false);

        var max = echo.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (max != null) max.setBaseValue(65.0);
        var damage = echo.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(8.0);
        echo.setHealth(65.0F);

        world.spawnEntity(echo);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                echo.getX(), echo.getY() + 1, echo.getZ(),
                90, .8, 1, .8, .06);
    }

    private static ServerPlayerEntity target(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null || !player.isAlive()) continue;

            double distance = player.squaredDistanceTo(arena.boss);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }

        return best;
    }

    private static Set<ServerPlayerEntity> participants(ServerWorld world, ArenaState arena) {
        Set<ServerPlayerEntity> result = new HashSet<>();
        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world) result.add(player);
        }
        return result;
    }

    private static void finish(ServerWorld world, ArenaState arena) {
        if (arena.defeated || arena.rewarded) return;

        arena.defeated = true;
        arena.attack = Attack.NONE;
        arena.boss.setHealth(1.0F);
        arena.boss.setInvulnerable(true);
        arena.boss.setAi(false);
        arena.bar.setVisible(false);

        world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_DEATH,
                SoundCategory.HOSTILE, 5.0F, .45F);

        world.spawnParticles(ParticleTypes.EXPLOSION,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                48, 2, 2, 2, .08);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                300, 4, 3, 4, .08);

        for (ServerPlayerEntity player : participants(world, arena)) {
            player.getInventory().offerOrDrop(new ItemStack(ModItems.NULLBLADE));
            player.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
            player.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"), false);
            player.sendMessage(Text.literal("The Nullblade answers to you now."), false);
        }

        arena.rewarded = true;
        arena.portalCooldown = 1;
        buildReturnPortal(world);
    }

    private static void resetEncounter(ServerWorld world, ArenaState arena) {
        if (arena.boss != null && arena.boss.isAlive()) arena.boss.discard();
        if (arena.bar != null) arena.bar.clearPlayers();
        arena.boss = null;
        arena.bar = null;
        arena.participants.clear();
        arena.attack = Attack.NONE;
        arena.defeated = false;
        arena.rewarded = false;
        arena.ticks = 0;
        arena.idleTicks = 0;
        arena.portalCooldown = 0;
    }

    private static void buildArena(ServerWorld world) {
        // The Null Realm is dedicated to this encounter, so this footprint is deterministic and safe.
        for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++) {
            for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
                double radius = Math.sqrt(x * x + z * z);
                if (radius <= ARENA_RADIUS) {
                    world.setBlockState(new BlockPos(x, 80, z),
                            radius < 17
                                    ? Blocks.POLISHED_BLACKSTONE.getDefaultState()
                                    : Blocks.CRYING_OBSIDIAN.getDefaultState());
                }

                for (int y = 81; y <= 100; y++) {
                    if (radius <= ARENA_RADIUS) {
                        world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int x = (int) Math.round(Math.cos(angle) * 15);
            int z = (int) Math.round(Math.sin(angle) * 15);

            for (int y = 81; y < 92; y++) {
                world.setBlockState(new BlockPos(x, y, z), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                world.setBlockState(new BlockPos(x + (Math.abs(z) % 2), y, z), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }

            world.setBlockState(new BlockPos(x, 92, z), Blocks.SCULK_CATALYST.getDefaultState());
        }

        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI * 2.0 / 32.0;
            world.setBlockState(
                    new BlockPos(
                            (int) Math.round(Math.cos(angle) * 9),
                            81,
                            (int) Math.round(Math.sin(angle) * 9)
                    ),
                    Blocks.CRYING_OBSIDIAN.getDefaultState()
            );
        }
    }

    private static void buildReturnPortal(ServerWorld world) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(
                        new BlockPos(13 + x, 81 + y, 13),
                        ModBlocks.VOID_PORTAL.getDefaultState()
                );
            }
        }

        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                13.5, 83, 13.5, 160, 1.5, 2.2, 1.5, .12);
    }

    private enum Attack {
        NONE(0, 0, 0),
        VOID_CLEAVE(24, 90, 3),
        SCULK_RING(28, 92, 4),
        VOID_RAIN(30, 100, 7),
        NULL_DASH(24, 86, 4),
        GRAVITY_WELL(26, 95, 7),
        REALITY_TEAR(20, 72, 8),
        COLLAPSE(30, 70, 9);

        final int windupTicks;
        final int recoveryTicks;
        final double radius;
        int windup;

        Attack(int windupTicks, int recoveryTicks, double radius) {
            this.windupTicks = windupTicks;
            this.recoveryTicks = recoveryTicks;
            this.radius = radius;
        }
    }

    private static final class ArenaState {
        WardenEntity boss;
        ServerBossBar bar;
        final Set<UUID> participants = new HashSet<>();

        int ticks;
        int phase = 1;
        int intro;
        int arenaPulse;
        int idleTicks;
        int nextAttackTick;
        int portalCooldown;

        Attack attack = Attack.NONE;
        boolean defeated;
        boolean rewarded;
    }
}
