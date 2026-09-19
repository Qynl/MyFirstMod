package dev.qynl.myfirstmod.boss;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
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
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NullWardenManager {
    private static final Map<RegistryKey<World>, ArenaState> ARENAS = new HashMap<>();
    private static final BlockPos CENTER = new BlockPos(0, 80, 0);
    private static final int ARENA_RADIUS = 22;
    private static final int SOFT_BOUNDARY = 26;
    private static final int HARD_BOUNDARY = 34;
    private static final BlockPos RETURN_PORTAL_ORIGIN = new BlockPos(13, 81, 13);

    private static final BlockPos[] PYLONS = {
            new BlockPos(15, 81, 0),
            new BlockPos(0, 81, 15),
            new BlockPos(-15, 81, 0),
            new BlockPos(0, 81, -15)
    };

    private NullWardenManager() {}

    private static NullWardenState state(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                NullWardenState.type(),
                NullWardenState.ID
        );
    }

    private static ArenaState arena(ServerWorld world) {
        return ARENAS.computeIfAbsent(world.getRegistryKey(), key -> loadArena(world));
    }

    private static ArenaState loadArena(ServerWorld world) {
        NullWardenState saved = state(world);
        ArenaState arena = new ArenaState();
        arena.participants.addAll(saved.participants);
        arena.rewardedPlayers.addAll(saved.rewardedPlayers);
        arena.echoes.addAll(saved.echoes);
        arena.defeated = saved.defeated;
        arena.rewarded = saved.rewarded;
        arena.returnPortalBuilt = saved.returnPortalBuilt;
        arena.phase = Math.max(1, saved.phase);
        arena.activePylons = saved.activePylons;
        arena.bossUuid = saved.bossUuid;

        if (arena.bossUuid != null) {
            Entity entity = world.getEntity(arena.bossUuid);
            if (entity instanceof WardenEntity warden && warden.isAlive()) {
                arena.boss = warden;
                restoreEchoes(world, arena);
                rebuildBossBar(world, arena);
                return arena;
            }
        }

        if (arena.defeated) {
            if (arena.returnPortalBuilt) buildReturnPortal(world, arena);
            return arena;
        }

        if (arena.bossUuid != null || !arena.echoes.isEmpty()) {
            cleanupOrphans(world, arena);
            clearSavedEncounter(world);
        }

        return new ArenaState();
    }

    private static void restoreEchoes(ServerWorld world, ArenaState arena) {
        arena.echoes.removeIf(uuid -> {
            Entity entity = world.getEntity(uuid);
            return !(entity instanceof WardenEntity) || !entity.isAlive();
        });
    }

    private static void cleanupOrphans(ServerWorld world, ArenaState arena) {
        if (arena.bossUuid != null) {
            Entity boss = world.getEntity(arena.bossUuid);
            if (boss != null) boss.discard();
        }
        for (UUID uuid : arena.echoes) {
            Entity echo = world.getEntity(uuid);
            if (echo != null) echo.discard();
        }
        arena.echoes.clear();
    }

    private static void clearSavedEncounter(ServerWorld world) {
        NullWardenState saved = state(world);
        saved.bossUuid = null;
        saved.participants.clear();
        saved.rewardedPlayers.clear();
        saved.echoes.clear();
        saved.defeated = false;
        saved.rewarded = false;
        saved.returnPortalBuilt = false;
        saved.phase = 1;
        saved.activePylons = 0;
        saved.dirty();
    }

    private static void persist(ServerWorld world, ArenaState arena) {
        NullWardenState saved = state(world);
        saved.bossUuid = arena.boss == null ? arena.bossUuid : arena.boss.getUuid();
        saved.participants.clear();
        saved.participants.addAll(arena.participants);
        saved.rewardedPlayers.clear();
        saved.rewardedPlayers.addAll(arena.rewardedPlayers);
        saved.echoes.clear();
        saved.echoes.addAll(arena.echoes);
        saved.defeated = arena.defeated;
        saved.rewarded = arena.rewarded;
        saved.returnPortalBuilt = arena.returnPortalBuilt;
        saved.phase = arena.phase;
        saved.activePylons = arena.activePylons;
        saved.dirty();
    }

    public static void enterArena(ServerWorld world, ServerPlayerEntity player) {
        ArenaState arena = arena(world);

        if (arena.defeated) {
            arena.participants.add(player.getUuid());
            rewardIfEligible(player, arena);
            buildReturnPortal(world, arena);
            persist(world, arena);
            return;
        }

        arena.participants.add(player.getUuid());

        if (arena.boss != null && arena.boss.isAlive()) {
            if (arena.bar != null) arena.bar.addPlayer(player);
            player.sendMessage(Text.literal("The Null Warden is already awake."), true);
            persist(world, arena);
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
        if (arena.boss == null) {
            arena.participants.clear();
            return;
        }

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
        arena.boss.setAiDisabled(true);
        arena.boss.setInvulnerable(true);
        world.spawnEntity(arena.boss);

        arena.bossUuid = arena.boss.getUuid();
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
        arena.ticks = 0;
        arena.phase = 1;
        arena.attack = Attack.NONE;
        arena.attackWindup = 0;
        arena.attackTarget = null;
        arena.nextAttackTick = 40;
        arena.arenaPulse = 0;
        arena.idleTicks = 0;
        arena.defeated = false;
        arena.rewarded = false;
        arena.returnPortalBuilt = false;
        arena.activePylons = 0;

        world.playSound(null, CENTER, SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.HOSTILE, 5.0F, 0.5F);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                0.5, 82, 0.5, 180, 4, 2, 4, 0.04);

        player.sendMessage(Text.literal("THE NULL WARDEN"), false);
        player.sendMessage(Text.literal("The city was never meant to open this door."), false);
        persist(world, arena);
    }

    private static void rebuildBossBar(ServerWorld world, ArenaState arena) {
        if (arena.boss == null || arena.defeated) return;

        arena.bar = new ServerBossBar(
                Text.literal("THE NULL WARDEN"),
                ServerBossBar.Color.PURPLE,
                ServerBossBar.Style.NOTCHED_20
        );
        arena.bar.setDarkenSky(true);
        arena.bar.setThickenFog(true);
        arena.bar.setPercent(arena.boss.getHealth() / arena.boss.getMaxHealth());

        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world) arena.bar.addPlayer(player);
        }
    }

    private static void tickArena(MinecraftServer server, ServerWorld world, ArenaState arena) {
        if (arena.boss == null) {
            if (arena.defeated) {
                for (UUID uuid : arena.participants) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                    if (player != null && player.getServerWorld() == world) rewardIfEligible(player, arena);
                }
                return;
            }
            return;
        }

        updateActiveParticipants(server, world, arena);
        if (arena.activeParticipants.isEmpty()) {
            arena.idleTicks++;
            if (arena.idleTicks >= 600) resetEncounter(world, arena);
            return;
        }
        arena.idleTicks = 0;

        if (arena.defeated) {
            for (UUID uuid : arena.participants) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null && player.getServerWorld() == world) rewardIfEligible(player, arena);
            }
            if (!arena.returnPortalBuilt) buildReturnPortal(world, arena);
            return;
        }

        if (!arena.boss.isAlive()) {
            finish(world, arena);
            return;
        }

        arena.ticks++;

        if (arena.intro > 0) {
            arena.intro--;
            arena.boss.setAiDisabled(true);
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
                arena.boss.setAiDisabled(false);
                world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                        SoundCategory.HOSTILE, 4.0F, 0.55F);
            }
            return;
        }

        arena.boss.setAiDisabled(arena.attack != Attack.NONE);
        arena.boss.setInvulnerable(arena.activePylons != 0);

        float hp = arena.boss.getHealth() / arena.boss.getMaxHealth();
        if (arena.bar != null) arena.bar.setPercent(Math.max(0.0F, hp));

        int newPhase = hp > 0.75F ? 1 : hp > 0.50F ? 2 : hp > 0.20F ? 3 : 4;
        if (newPhase != arena.phase) {
            arena.phase = newPhase;
            arena.attack = Attack.NONE;
            arena.attackWindup = 0;
            arena.attackTarget = null;
            activatePylons(arena, newPhase);
            phaseShift(world, arena);
            persist(world, arena);
        }

        tickPylons(world, arena);

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
            arena.attackWindup--;
            telegraph(world, arena);
            if (arena.attackWindup <= 0) {
                resolveAttack(world, arena);
                arena.attack = Attack.NONE;
                arena.attackTarget = null;
                arena.boss.setAiDisabled(false);
            }
        } else if (arena.activePylons == 0 && arena.ticks >= arena.nextAttackTick) {
            beginAttack(world, arena);
        }

        if (arena.phase >= 2 && arena.ticks % 260 == 0) summonEcho(world, arena);

        if (arena.ticks % 40 == 0) persist(world, arena);
    }

    private static void updateActiveParticipants(MinecraftServer server, ServerWorld world, ArenaState arena) {
        arena.activeParticipants.clear();

        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world && player.isAlive()
                    && player.squaredDistanceTo(CENTER.getX() + .5, CENTER.getY() + 1, CENTER.getZ() + .5)
                    <= HARD_BOUNDARY * HARD_BOUNDARY) {
                double distance = Math.sqrt(player.squaredDistanceTo(
                        CENTER.getX() + .5, CENTER.getY() + 1, CENTER.getZ() + .5));

                if (distance > SOFT_BOUNDARY && player.age % 20 == 0) {
                    player.sendMessage(Text.literal("The Null Realm is collapsing at the edge."), true);
                }

                arena.activeParticipants.add(uuid);
                if (arena.bar != null) arena.bar.addPlayer(player);
            } else if (arena.bar != null && player != null) {
                arena.bar.removePlayer(player);
            }
        }
    }

    private static void activatePylons(ArenaState arena, int phase) {
        int count = phase == 2 ? 2 : phase == 3 ? 3 : 4;
        arena.activePylons = (1 << count) - 1;
        arena.pylonProgress = new int[4];
    }

    private static void tickPylons(ServerWorld world, ArenaState arena) {
        if (arena.activePylons == 0) return;

        for (int i = 0; i < 4; i++) {
            if ((arena.activePylons & (1 << i)) == 0) continue;

            BlockPos pylon = PYLONS[i];
            boolean cleansing = false;

            for (UUID uuid : arena.activeParticipants) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
                if (player != null && player.isSneaking()
                        && player.squaredDistanceTo(pylon.getX() + .5, pylon.getY() + 1, pylon.getZ() + .5) <= 3.0 * 3.0) {
                    cleansing = true;
                    break;
                }
            }

            if (cleansing) {
                arena.pylonProgress[i]++;
                if (arena.pylonProgress[i] % 10 == 0) {
                    world.playSound(null, pylon, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                            SoundCategory.BLOCKS, 0.8F, 1.3F);
                    world.spawnParticles(ParticleTypes.SCULK_SOUL,
                            pylon.getX() + .5, pylon.getY() + 1.5, pylon.getZ() + .5,
                            12, .35, .5, .35, .02);
                }

                if (arena.pylonProgress[i] >= 50) {
                    arena.activePylons &= ~(1 << i);
                    arena.pylonProgress[i] = 0;
                    world.setBlockState(pylon.up(11), Blocks.AIR.getDefaultState());
                    world.playSound(null, pylon, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                            SoundCategory.BLOCKS, 1.5F, .6F);
                    world.spawnParticles(ParticleTypes.EXPLOSION,
                            pylon.getX() + .5, pylon.getY() + 1, pylon.getZ() + .5,
                            8, .6, 1.2, .6, .03);

                    if (arena.activePylons == 0) {
                        arena.boss.setInvulnerable(false);
                        for (ServerPlayerEntity player : participants(world, arena)) {
                            player.sendMessage(Text.literal("The Null Warden is exposed."), true);
                        }
                    }
                    persist(world, arena);
                }
            } else if (arena.pylonProgress[i] > 0 && arena.ticks % 10 == 0) {
                arena.pylonProgress[i] = Math.max(0, arena.pylonProgress[i] - 2);
            }

            if (arena.activePylons != 0 && arena.ticks % 10 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        pylon.getX() + .5, pylon.getY() + 11.5, pylon.getZ() + .5,
                        4, .3, .2, .3, .01);
            }
        }
    }

    private static void beginAttack(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = selectTarget(world, arena);
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

        arena.attackTarget = target.getUuid();
        arena.attackWindup = arena.attack.windupTicks;
        arena.nextAttackTick = arena.ticks + arena.attack.recoveryTicks + arena.attack.windupTicks;

        String name = arena.attack.displayName;
        for (ServerPlayerEntity player : participants(world, arena)) {
            player.sendMessage(Text.literal("NULL WARDEN // " + name), true);
        }

        world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.HOSTILE, 2.5F, 0.75F);
    }

    private static ServerPlayerEntity attackTarget(ServerWorld world, ArenaState arena) {
        if (arena.attackTarget != null) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(arena.attackTarget);
            if (player != null && arena.activeParticipants.contains(player.getUuid())) return player;
        }
        return selectTarget(world, arena);
    }

    private static ServerPlayerEntity selectTarget(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        int offset = arena.ticks / 20;

        int index = 0;
        for (UUID uuid : arena.activeParticipants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            if (arena.phase >= 2 && (index + offset) % 3 == 0) {
                return player;
            }

            double distance = player.squaredDistanceTo(arena.boss);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
            index++;
        }

        return best;
    }

    private static void telegraph(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = attackTarget(world, arena);
        if (target == null) return;

        int remaining = arena.attackWindup;
        float size = remaining < 7 ? 1.8F : 1.1F;

        switch (arena.attack) {
            case VOID_CLEAVE -> {
                Vec3d forward = arena.boss.getRotationVector().normalize();
                for (int i = 0; i < 20; i++) {
                    double distance = 1.5 + i * 0.25;
                    double spread = (i % 5 - 2) * 0.35;
                    world.spawnParticles(new DustParticleEffect(new Vector3f(.65F, .1F, .9F), size),
                            arena.boss.getX() + forward.x * distance - forward.z * spread,
                            arena.boss.getY() + .15,
                            arena.boss.getZ() + forward.z * distance + forward.x * spread,
                            1, 0, 0, 0, 0);
                }
            }
            case SCULK_RING -> ringParticles(world, arena.boss.getX(), arena.boss.getY() + .1,
                    arena.boss.getZ(), 6.0, ParticleTypes.SCULK_SOUL, 64);
            case VOID_RAIN -> {
                for (UUID uuid : arena.activeParticipants) {
                    ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
                    if (player != null) {
                        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                                player.getX(), player.getY() + .1, player.getZ(),
                                remaining < 7 ? 20 : 8, .7, .05, .7, .01);
                    }
                }
            }
            case NULL_DASH -> {
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        target.getX(), target.getY() + 1, target.getZ(),
                        remaining < 7 ? 30 : 10, .5, .8, .5, .03);
            }
            case GRAVITY_WELL, REALITY_TEAR, COLLAPSE -> ringParticles(
                    world, target.getX(), target.getY() + .08, target.getZ(),
                    arena.attack.radius,
                    remaining < 7 ? ParticleTypes.EXPLOSION : ParticleTypes.REVERSE_PORTAL,
                    arena.attack == Attack.COLLAPSE ? 72 : 48
            );
            default -> {}
        }
    }

    private static void resolveAttack(ServerWorld world, ArenaState arena) {
        ServerPlayerEntity target = attackTarget(world, arena);
        if (target == null || arena.boss == null) return;

        switch (arena.attack) {
            case VOID_CLEAVE -> {
                Vec3d forward = arena.boss.getRotationVector().normalize();
                Vec3d toTarget = target.getPos().subtract(arena.boss.getPos());
                double distance = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
                double dot = distance < .01 ? 1 : (forward.x * toTarget.x + forward.z * toTarget.z) / distance;
                if (distance <= 5.0 && dot > .55) target.damage(world.getDamageSources().mobAttack(arena.boss), 12);
                shockwave(world, target, 3.0);
            }
            case SCULK_RING -> {
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    double d = player.distanceTo(arena.boss);
                    if (d > 4.0 && d < 8.0) player.damage(world.getDamageSources().mobAttack(arena.boss), 13);
                }
                ringParticles(world, arena.boss.getX(), arena.boss.getY() + .15, arena.boss.getZ(),
                        6.0, ParticleTypes.SCULK_SOUL, 80);
            }
            case VOID_RAIN -> {
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    if (player.squaredDistanceTo(target) <= 2.5 * 2.5) {
                        player.damage(world.getDamageSources().mobAttack(arena.boss), 10);
                    }
                }
            }
            case NULL_DASH -> {
                Vec3d from = arena.boss.getPos();
                arena.boss.lookAt(net.minecraft.entity.EntityAnchorArgumentType.EntityAnchor.EYES, target.getPos());
                arena.boss.teleport(target.getX(), target.getY(), target.getZ(), false);
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    if (player.squaredDistanceTo(from.x, from.y, from.z) < 2.5 * 2.5
                            || player.squaredDistanceTo(arena.boss) < 3.5 * 3.5) {
                        player.damage(world.getDamageSources().mobAttack(arena.boss), 16);
                    }
                }
                shockwave(world, target, 3.5);
            }
            case GRAVITY_WELL -> {
                gravityPulse(world, arena, .6);
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    if (player.squaredDistanceTo(target) < 3.5 * 3.5) {
                        player.damage(world.getDamageSources().mobAttack(arena.boss), 12);
                    }
                }
            }
            case REALITY_TEAR -> {
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    if (player.squaredDistanceTo(target) < 4.0 * 4.0) {
                        player.damage(world.getDamageSources().mobAttack(arena.boss), 14);
                    }
                }
                world.playSound(null, target.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN,
                        SoundCategory.HOSTILE, 1.6F, .7F);
            }
            case COLLAPSE -> {
                for (ServerPlayerEntity player : activePlayers(world, arena)) {
                    if (player.squaredDistanceTo(target) < 5.0 * 5.0) {
                        player.damage(world.getDamageSources().mobAttack(arena.boss), 20);
                    }
                }
                gravityPulse(world, arena, .85);
                shockwave(world, target, 9.0);
            }
            default -> {}
        }
    }

    private static void gravityPulse(ServerWorld world, ArenaState arena, double strength) {
        for (ServerPlayerEntity player : activePlayers(world, arena)) {
            Vec3d delta = arena.boss.getPos().subtract(player.getPos());
            double length = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            if (length > .1 && length < 15) {
                double scale = Math.min(strength, .9);
                player.addVelocity(delta.x / length * scale, .12, delta.z / length * scale);
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
            player.sendMessage(Text.literal("The pylons are feeding it. Sneak beside each one to cleanse it."), false);
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
        arena.echoes.removeIf(uuid -> {
            Entity entity = world.getEntity(uuid);
            return entity == null || !entity.isAlive();
        });

        if (arena.echoes.size() >= Math.min(3, arena.phase)) return;

        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;

        double angle = arena.ticks * 0.045;
        echo.refreshPositionAndAngles(
                Math.cos(angle) * 11, 81, Math.sin(angle) * 11, 0, 0
        );
        echo.setCustomName(Text.literal("NULL ECHO"));
        echo.setCustomNameVisible(false);
        echo.setPersistent(true);

        var max = echo.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (max != null) max.setBaseValue(65.0);
        var damage = echo.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(8.0);
        echo.setHealth(65.0F);

        world.spawnEntity(echo);
        arena.echoes.add(echo.getUuid());

        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                echo.getX(), echo.getY() + 1, echo.getZ(),
                90, .8, 1, .8, .06);
        persist(world, arena);
    }

    private static void finish(ServerWorld world, ArenaState arena) {
        if (arena.defeated) return;

        arena.defeated = true;
        arena.attack = Attack.NONE;
        arena.attackTarget = null;
        arena.attackWindup = 0;
        arena.activePylons = 0;

        arena.boss.setHealth(1.0F);
        arena.boss.setInvulnerable(true);
        arena.boss.setAiDisabled(true);

        if (arena.bar != null) arena.bar.setVisible(false);

        world.playSound(null, arena.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_DEATH,
                SoundCategory.HOSTILE, 5.0F, .45F);
        world.spawnParticles(ParticleTypes.EXPLOSION,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                48, 2, 2, 2, .08);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                arena.boss.getX(), arena.boss.getY() + 1, arena.boss.getZ(),
                300, 4, 3, 4, .08);

        for (ServerPlayerEntity player : activePlayers(world, arena)) {
            rewardIfEligible(player, arena);
        }

        arena.rewarded = arena.rewardedPlayers.containsAll(arena.participants);
        buildReturnPortal(world, arena);
        persist(world, arena);
    }

    private static void rewardIfEligible(ServerPlayerEntity player, ArenaState arena) {
        if (!arena.participants.contains(player.getUuid())) return;
        if (!arena.rewardedPlayers.add(player.getUuid())) return;

        player.getInventory().offerOrDrop(new ItemStack(ModItems.NULLBLADE));
        player.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
        player.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"), false);
        player.sendMessage(Text.literal("The Nullblade answers to you now."), false);
    }

    private static void resetEncounter(ServerWorld world, ArenaState arena) {
        cleanupOrphans(world, arena);
        removeReturnPortal(world, arena);
        clearSavedEncounter(world);

        if (arena.bar != null) {
            for (UUID uuid : arena.participants) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
                if (player != null) arena.bar.removePlayer(player);
            }
        }

        arena.boss = null;
        arena.bossUuid = null;
        arena.bar = null;
        arena.participants.clear();
        arena.activeParticipants.clear();
        arena.rewardedPlayers.clear();
        arena.echoes.clear();
        arena.attack = Attack.NONE;
        arena.attackTarget = null;
        arena.attackWindup = 0;
        arena.defeated = false;
        arena.rewarded = false;
        arena.returnPortalBuilt = false;
        arena.activePylons = 0;
        arena.ticks = 0;
        arena.idleTicks = 0;
        arena.nextAttackTick = 40;
        arena.arenaPulse = 0;
    }

    private static void buildArena(ServerWorld world) {
        for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++) {
            for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
                double radius = Math.sqrt(x * x + z * z);
                if (radius <= ARENA_RADIUS) {
                    world.setBlockState(new BlockPos(x, 80, z),
                            radius < 17
                                    ? Blocks.POLISHED_BLACKSTONE.getDefaultState()
                                    : Blocks.CRYING_OBSIDIAN.getDefaultState());

                    for (int y = 81; y <= 100; y++) {
                        world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        for (int i = 0; i < 4; i++) {
            BlockPos pylon = PYLONS[i];
            for (int y = 0; y < 11; y++) {
                world.setBlockState(pylon.add(0, y, 0), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                world.setBlockState(pylon.add(i % 2, y, 1), Blocks.POLISHED_BLACKSTONE.getDefaultState());
            }
            world.setBlockState(pylon.up(11), Blocks.SCULK_CATALYST.getDefaultState());
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

    private static void buildReturnPortal(ServerWorld world, ArenaState arena) {
        if (arena.returnPortalBuilt) return;

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(
                        RETURN_PORTAL_ORIGIN.add(x, y, 0),
                        ModBlocks.VOID_PORTAL.getDefaultState()
                );
            }
        }

        arena.returnPortalBuilt = true;
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                13.5, 83, 13.5, 160, 1.5, 2.2, 1.5, .12);
    }

    private static void removeReturnPortal(ServerWorld world, ArenaState arena) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                BlockPos pos = RETURN_PORTAL_ORIGIN.add(x, y, 0);
                if (world.getBlockState(pos).isOf(ModBlocks.VOID_PORTAL)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }
        arena.returnPortalBuilt = false;
    }

    private static void ringParticles(ServerWorld world, double x, double y, double z,
                                      double radius, net.minecraft.particle.ParticleEffect particle, int points) {
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2.0 / points;
            world.spawnParticles(particle,
                    x + Math.cos(angle) * radius,
                    y,
                    z + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    private static void shockwave(ServerWorld world, ServerPlayerEntity center, double radius) {
        ringParticles(world, center.getX(), center.getY() + .15, center.getZ(),
                radius, ParticleTypes.PORTAL, 80);
    }

    private static Set<ServerPlayerEntity> activePlayers(ServerWorld world, ArenaState arena) {
        Set<ServerPlayerEntity> result = new HashSet<>();
        for (UUID uuid : arena.activeParticipants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world && player.isAlive()) result.add(player);
        }
        return result;
    }

    private static Set<ServerPlayerEntity> participants(ServerWorld world, ArenaState arena) {
        Set<ServerPlayerEntity> result = new HashSet<>();
        for (UUID uuid : arena.participants) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && player.getServerWorld() == world) result.add(player);
        }
        return result;
    }

    public static void saveReturnPoint(ServerPlayerEntity player, RegistryKey<World> worldKey,
                                       BlockPos pos, float yaw, float pitch) {
        NullWardenState state = state(player.getServerWorld());
        state.returnPoints.put(player.getUuid(), new NullWardenState.ReturnPointData(
                worldKey.getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), yaw, pitch
        ));
        state.dirty();
    }

    public static NullWardenState.ReturnPointData takeReturnPoint(ServerPlayerEntity player) {
        NullWardenState state = state(player.getServerWorld());
        NullWardenState.ReturnPointData point = state.returnPoints.remove(player.getUuid());
        state.dirty();
        return point;
    }

    private enum Attack {
        NONE(0, 0, 0, "IDLE"),
        VOID_CLEAVE(24, 90, 5, "VOID CLEAVE"),
        SCULK_RING(28, 92, 6, "SCULK RING"),
        VOID_RAIN(30, 100, 2.5, "VOID RAIN"),
        NULL_DASH(24, 86, 4, "NULL DASH"),
        GRAVITY_WELL(26, 95, 7, "GRAVITY WELL"),
        REALITY_TEAR(20, 72, 8, "REALITY TEAR"),
        COLLAPSE(30, 70, 9, "REALITY COLLAPSE");

        final int windupTicks;
        final int recoveryTicks;
        final double radius;
        final String displayName;

        Attack(int windupTicks, int recoveryTicks, double radius, String displayName) {
            this.windupTicks = windupTicks;
            this.recoveryTicks = recoveryTicks;
            this.radius = radius;
            this.displayName = displayName;
        }
    }

    private static final class ArenaState {
        WardenEntity boss;
        UUID bossUuid;
        ServerBossBar bar;

        final Set<UUID> participants = new HashSet<>();
        final Set<UUID> activeParticipants = new HashSet<>();
        final Set<UUID> rewardedPlayers = new HashSet<>();
        final Set<UUID> echoes = new HashSet<>();

        int ticks;
        int phase = 1;
        int intro;
        int arenaPulse;
        int idleTicks;
        int nextAttackTick = 40;
        int attackWindup;

        int activePylons;
        int[] pylonProgress = new int[4];

        UUID attackTarget;
        Attack attack = Attack.NONE;

        boolean defeated;
        boolean rewarded;
        boolean returnPortalBuilt;
    }
}
