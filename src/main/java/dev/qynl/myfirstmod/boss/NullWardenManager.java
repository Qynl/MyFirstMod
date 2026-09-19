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
import net.minecraft.particle.ParticleEffect;
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
    private static final int HARD_BOUNDARY = 34;
    private static final BlockPos RETURN_PORTAL = new BlockPos(13, 81, 13);
    private static final BlockPos[] PYLONS = {
            new BlockPos(15, 81, 0), new BlockPos(0, 81, 15),
            new BlockPos(-15, 81, 0), new BlockPos(0, 81, -15)
    };

    private NullWardenManager() {}

    private static NullWardenState saved(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                NullWardenState.type(), NullWardenState.ID);
    }

    private static ArenaState getArena(ServerWorld world) {
        return ARENAS.computeIfAbsent(world.getRegistryKey(), key -> load(world));
    }

    private static ArenaState load(ServerWorld world) {
        NullWardenState data = saved(world);
        ArenaState a = new ArenaState();
        a.participants.addAll(data.participants);
        a.eligiblePlayers.addAll(data.eligiblePlayers);
        a.rewardedPlayers.addAll(data.rewardedPlayers);
        a.echoes.addAll(data.echoes);
        a.defeated = data.defeated;
        a.phase = Math.max(1, data.phase);
        a.activePylons = data.activePylons;
        a.pylonProgress = data.pylonProgress.clone();
        a.bossUuid = data.bossUuid;
        a.returnPortalBuilt = data.returnPortalBuilt;

        if (a.bossUuid != null) {
            Entity entity = world.getEntity(a.bossUuid);
            if (entity instanceof NullWardenEntity w && w.isAlive()) {
                a.boss = w;
                a.bar = createBar();
                a.bar.setPercent(w.getHealth() / w.getMaxHealth());
                for (UUID id : a.participants) {
                    ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
                    if (p != null && p.getServerWorld() == world) a.bar.addPlayer(p);
                }
                return a;
            }
        }

        if (a.defeated) {
            if (a.returnPortalBuilt) buildReturnPortal(world, a);
            return a;
        }

        cleanup(world, a);
        clearSaved(world);
        return new ArenaState();
    }

    private static ServerBossBar createBar() {
        ServerBossBar bar = new ServerBossBar(
                Text.literal("THE NULL WARDEN"),
                ServerBossBar.Color.PURPLE,
                ServerBossBar.Style.NOTCHED_20);
        bar.setDarkenSky(true);
        bar.setThickenFog(true);
        return bar;
    }

    private static void persist(ServerWorld world, ArenaState a) {
        NullWardenState data = saved(world);
        data.bossUuid = a.boss == null ? a.bossUuid : a.boss.getUuid();
        data.participants.clear();
        data.participants.addAll(a.participants);
        data.eligiblePlayers.clear();
        data.eligiblePlayers.addAll(a.eligiblePlayers);
        data.rewardedPlayers.clear();
        data.rewardedPlayers.addAll(a.rewardedPlayers);
        data.echoes.clear();
        data.echoes.addAll(a.echoes);
        data.defeated = a.defeated;
        data.rewarded = a.rewardedPlayers.containsAll(a.eligiblePlayers);
        data.returnPortalBuilt = a.returnPortalBuilt;
        data.phase = a.phase;
        data.activePylons = a.activePylons;
        data.pylonProgress = a.pylonProgress.clone();
        data.dirty();
    }

    private static void clearSaved(ServerWorld world) {
        NullWardenState data = saved(world);
        data.bossUuid = null;
        data.participants.clear();
        data.eligiblePlayers.clear();
        data.rewardedPlayers.clear();
        data.echoes.clear();
        data.defeated = false;
        data.rewarded = false;
        data.returnPortalBuilt = false;
        data.phase = 1;
        data.activePylons = 0;
        data.pylonProgress = new int[4];
        data.dirty();
    }

    public static void enterArena(ServerWorld world, ServerPlayerEntity player) {
        ArenaState a = getArena(world);
        a.participants.add(player.getUuid());

        if (a.defeated) {
            rewardIfEligible(player, a);
            buildReturnPortal(world, a);
            persist(world, a);
            return;
        }

        if (a.boss != null && a.boss.isAlive()) {
            if (a.bar != null) a.bar.addPlayer(player);
            player.sendMessage(Text.literal("The Null Warden is already awake."), true);
            persist(world, a);
            return;
        }

        start(world, player, a);
    }

    public static void tick(MinecraftServer server) {
        ARENAS.entrySet().removeIf(e -> server.getWorld(e.getKey()) == null);
        for (Map.Entry<RegistryKey<World>, ArenaState> entry : ARENAS.entrySet()) {
            ServerWorld world = server.getWorld(entry.getKey());
            if (world != null) tickArena(server, world, entry.getValue());
        }
    }

    private static void start(ServerWorld world, ServerPlayerEntity player, ArenaState a) {
        buildArena(world);
        a.boss = ModEntities.NULL_WARDEN.create(world);
        if (a.boss == null) {
            a.participants.clear();
            return;
        }

        var hp = a.boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        var damage = a.boss.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        var speed = a.boss.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (hp != null) hp.setBaseValue(500);
        if (damage != null) damage.setBaseValue(18);
        if (speed != null) speed.setBaseValue(.38);

        a.boss.setHealth(500);
        a.boss.refreshPositionAndAngles(.5, 81, .5, 180, 0);
        a.boss.setCustomName(Text.literal("THE NULL WARDEN"));
        a.boss.setCustomNameVisible(false);
        a.boss.setAiDisabled(true);
        a.boss.setInvulnerable(true);
        a.boss.setVisualState(1, 0, 0, false);
        world.spawnEntity(a.boss);

        a.bossUuid = a.boss.getUuid();
        a.bar = createBar();
        for (UUID id : a.participants) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p != null && p.getServerWorld() == world) a.bar.addPlayer(p);
        }

        a.ticks = 0;
        a.intro = 100;
        a.phase = 1;
        a.activePylons = 0;
        a.pylonProgress = new int[4];
        a.attack = Attack.NONE;
        a.attackTarget = null;
        a.nextAttackTick = 40;
        a.idleTicks = 0;
        a.victoryTicks = 0;
        a.targetRotation = 0;
        a.recoveryTicks = 0;
        a.defeated = false;
        a.rewardedPlayers.clear();
        a.eligiblePlayers.clear();
        a.returnPortalBuilt = false;
        a.joinTicks.clear();

        world.playSound(null, CENTER, SoundEvents.ENTITY_WARDEN_EMERGE,
                SoundCategory.HOSTILE, 5, .5f);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, .5, 82, .5,
                180, 4, 2, 4, .04);
        player.sendMessage(Text.literal("THE NULL WARDEN"), false);
        player.sendMessage(Text.literal("The city was never meant to open this door."), false);
        persist(world, a);
    }

    private static void tickArena(MinecraftServer server, ServerWorld world, ArenaState a) {
        if (a.boss == null) {
            if (a.defeated) rewardPending(server, world, a);
            return;
        }

        updatePlayers(server, world, a);
        if (a.activeParticipants.isEmpty()) {
            if (++a.idleTicks >= 600) reset(world, a);
            return;
        }
        a.idleTicks = 0;

        if (a.defeated) {
            rewardPending(server, world, a);
            if (!a.returnPortalBuilt) buildReturnPortal(world, a);
            if (++a.victoryTicks >= 100) {
                a.boss.discard();
                a.boss = null;
                a.bossUuid = null;
                persist(world, a);
            }
            return;
        }

        if (!a.boss.isAlive()) {
            finish(world, a);
            return;
        }

        a.ticks++;
        updateEligibility(a);

        // Ambient arena pulse: the room itself should feel alive between attacks.
        if (a.ticks % 5 == 0) {
            double pulse = 0.9 + Math.sin(a.ticks * 0.08) * 0.18;
            world.spawnParticles(ParticleTypes.SCULK_SOUL,
                    CENTER.getX() + 0.5, 81.2, CENTER.getZ() + 0.5,
                    5, 5.5 * pulse, 0.15, 5.5 * pulse, 0.008);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    a.boss.getX(), a.boss.getY() + 1.0, a.boss.getZ(),
                    3, 1.1, 1.4, 1.1, 0.015);
        }
        // Ritual pathways continuously connect the boss to every active pylon.
        if (a.ticks % 3 == 0 && a.activePylons != 0) {
            for (int i = 0; i < 4; i++) {
                if ((a.activePylons & (1 << i)) == 0) continue;
                BlockPos p = PYLONS[i];
                double bx = a.boss.getX(), bz = a.boss.getZ();
                double px = p.getX() + .5, pz = p.getZ() + .5;
                for (int n = 0; n < 10; n++) {
                    double k = (n + ((a.ticks % 15) / 15.0)) / 10.0;
                    world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                            bx + (px - bx) * k, 80.35, bz + (pz - bz) * k,
                            1, .08, .02, .08, .005);
                }
            }
        }
        if (a.ticks % 20 == 0) {
            for (int i = 0; i < 4; i++) {
                if ((a.activePylons & (1 << i)) == 0) continue;
                BlockPos p = PYLONS[i];
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        p.getX() + 0.5, p.getY() + 13, p.getZ() + 0.5,
                        10, 0.5, 1.2, 0.5, 0.015);
            }
        }

        if (a.intro > 0) {
            a.intro--;
            a.boss.setAiDisabled(true);
            a.boss.setVisualState(1, 0, 0, false);
            // Cinematic awakening: the arena contracts toward the boss in pulses.
            double t = (100 - a.intro) / 100.0;
            double radius = Math.max(1.0, 9.0 - t * 7.5);
            ring(world, a.boss.getX(), a.boss.getY() + .08, a.boss.getZ(),
                    radius, ParticleTypes.REVERSE_PORTAL, 80);
            if (a.intro % 5 == 0) {
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        a.boss.getX(), a.boss.getY() + 1.2, a.boss.getZ(),
                        20, 2.0 + t, 1.5, 2.0 + t, .025);
                world.spawnParticles(ParticleTypes.END_ROD,
                        a.boss.getX(), a.boss.getY() + 1.0, a.boss.getZ(),
                        8, 1.0, .8, 1.0, .02);
            }
            a.boss.setInvulnerable(true);
            a.boss.setVelocity(Vec3d.ZERO);
            if (a.intro % 10 == 0) {
                world.playSound(null, a.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                        SoundCategory.HOSTILE, 2, .5f + a.intro / 250f);
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(),
                        24, 1.3, 1.2, 1.3, .02);
            }
            if (a.intro == 0) {
                a.boss.setInvulnerable(false);
                a.boss.setAiDisabled(false);
                world.playSound(null, a.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                        SoundCategory.HOSTILE, 4, .55f);
            }
            return;
        }

        a.boss.setAiDisabled(a.attack != Attack.NONE);
        a.boss.setInvulnerable(a.activePylons != 0);

        applyPhaseMovement(world, a);

        a.boss.setVisualState(a.phase, attackVisualId(a.attack),
                a.attack == Attack.NONE ? 0 : (int) Math.min(100,
                        100.0 * a.attackWindup / Math.max(1, a.attack.windup)), false);

        if (a.bar != null && a.attack != Attack.NONE && a.attackWindup % 5 == 0) {
            a.bar.setName(Text.literal("THE NULL WARDEN  //  " + a.attack.name));
        } else if (a.bar != null && a.attack == Attack.NONE) {
            a.bar.setName(Text.literal("THE NULL WARDEN  //  PHASE " + a.phase));
        }

        float health = a.boss.getHealth() / a.boss.getMaxHealth();
        if (a.bar != null) a.bar.setPercent(Math.max(0, health));

        int phase = health > .75f ? 1 : health > .50f ? 2 : health > .20f ? 3 : 4;
        if (phase != a.phase) {
            a.phase = phase;
            a.attack = Attack.NONE;
            a.attackTarget = null;
            a.attackX = a.attackY = a.attackZ = 0;
            a.attackWindup = 0;
            a.activePylons = (1 << phase) - 1;
            a.pylonProgress = new int[4];
            a.hazardTicks = 0;
            a.hazardPattern = -1;
            phaseShift(world, a);
            persist(world, a);
        }

        tickPylons(world, a);
        tickPylonPressure(world, a);
        tickPhaseArena(world, a);

        if (a.attack != Attack.NONE) {
            a.attackWindup--;
            // Telegraphs are visual warnings, not a particle flood. Updating every
            // second tick keeps the signal crisp while cutting arena particle load.
            if ((a.attackWindup & 1) == 0) telegraph(world, a);
            if (a.attackWindup <= 0) {
                resolveAttack(world, a);
                a.attack = Attack.NONE;
                a.attackTarget = null;
                a.boss.setAiDisabled(false);
            }
        } else if (a.ticks >= a.nextAttackTick) {
            beginAttack(world, a);
        }

        // Short recovery windows are intentional: the boss cannot chain attacks
        // indefinitely, giving players a reliable moment to reposition or cleanse.
        if (a.attack == Attack.NONE && a.recoveryTicks > 0) {
            a.recoveryTicks--;
            a.boss.setAiDisabled(false);
        }

        int echoInterval = switch (a.phase) {
            case 2 -> 300;
            case 3 -> 220;
            default -> 160;
        };
        if (a.phase >= 2 && a.ticks % echoInterval == 0) summonEcho(world, a);
        if (a.ticks % 40 == 0) persist(world, a);
    }

    private static void updatePlayers(MinecraftServer server, ServerWorld world, ArenaState a) {
        a.activeParticipants.clear();
        for (UUID id : a.participants) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null && p.getServerWorld() == world && p.isAlive()
                    && p.squaredDistanceTo(.5, 81, .5) <= HARD_BOUNDARY * HARD_BOUNDARY) {
                a.activeParticipants.add(id);
                a.joinTicks.putIfAbsent(id, a.ticks);
                if (a.bar != null) a.bar.addPlayer(p);
            } else if (p != null && a.bar != null) {
                a.bar.removePlayer(p);
            }
        }
    }

    private static void updateEligibility(ArenaState a) {
        for (UUID id : a.activeParticipants) {
            int joined = a.joinTicks.getOrDefault(id, a.ticks);
            if (a.ticks - joined >= 100) a.eligiblePlayers.add(id);
        }
    }

    private static void tickPylons(ServerWorld world, ArenaState a) {
        if (a.activePylons == 0) return;

        for (int i = 0; i < 4; i++) {
            if ((a.activePylons & (1 << i)) == 0) continue;
            BlockPos p = PYLONS[i];
            boolean cleansing = false;
            ServerPlayerEntity cleanser = null;
            for (UUID id : a.activeParticipants) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(id);
                if (player != null && player.isSneaking()
                        && player.squaredDistanceTo(p.getX() + .5, p.getY() + 1, p.getZ() + .5) <= 9) {
                    cleansing = true;
                    cleanser = player;
                    break;
                }
            }

            // Cleansing is deliberately exposed: the Warden keeps attacking while
            // somebody works on a pylon. The objective is now a contested space,
            // not a safe maintenance phase.
            if (cleansing) {
                a.pylonProgress[i]++;
                if (a.pylonProgress[i] % 5 == 0 && cleanser != null) {
                    world.spawnParticles(ParticleTypes.SCULK_SOUL,
                            cleanser.getX(), cleanser.getY() + 1.0, cleanser.getZ(),
                            8, .25, .35, .25, .01);
                }
                if (a.pylonProgress[i] % 10 == 0) {
                    cleanser.sendMessage(Text.literal("NULL PYLON // " + a.pylonProgress[i] + "/50"), true);
                    world.playSound(null, p, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                            SoundCategory.BLOCKS, .8f, 1.3f);
                    world.spawnParticles(ParticleTypes.SCULK_SOUL,
                            p.getX() + .5, p.getY() + 12, p.getZ() + .5,
                            12, .35, .5, .35, .02);
                }
                if (a.pylonProgress[i] >= 50) {
                    a.activePylons &= ~(1 << i);
                    a.pylonProgress[i] = 0;
                    world.setBlockState(p.up(11), Blocks.AIR.getDefaultState());
                    world.playSound(null, p, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                            SoundCategory.BLOCKS, 1.5f, .55f);
                    world.spawnParticles(ParticleTypes.EXPLOSION,
                            p.getX() + .5, p.getY() + 12, p.getZ() + .5,
                            8, .6, 1.2, .6, .03);
                    for (ServerPlayerEntity player : participants(world, a)) {
                        player.sendMessage(Text.literal(a.activePylons == 0
                                ? "The Null Warden is exposed."
                                : "Pylon cleansed. Keep moving."), true);
                    }
                    persist(world, a);
                }
            } else if (a.pylonProgress[i] > 0 && a.ticks % 10 == 0) {
                a.pylonProgress[i] = Math.max(0, a.pylonProgress[i] - 2);
            }
        }
    }

    /**
     * Active pylons are deliberately dangerous to stand beside. Their pressure
     * scales with the phase, turning cleansing into a real contested objective:
     * players must commit to the pylon while managing a local hazard.
     */
    private static void tickPylonPressure(ServerWorld world, ArenaState a) {
        if (a.activePylons == 0) return;

        int interval = switch (a.phase) {
            case 2 -> 45;
            case 3 -> 35;
            default -> 25;
        };
        if (a.ticks % interval != 0) return;

        float damage = switch (a.phase) {
            case 2 -> 5.0f;
            case 3 -> 7.0f;
            default -> 9.0f;
        };
        double radius = switch (a.phase) {
            case 2 -> 3.5;
            case 3 -> 4.25;
            default -> 5.0;
        };

        for (int i = 0; i < 4; i++) {
            if ((a.activePylons & (1 << i)) == 0) continue;

            BlockPos p = PYLONS[i];
            world.spawnParticles(ParticleTypes.SCULK_SOUL,
                    p.getX() + .5, p.getY() + 12.2, p.getZ() + .5,
                    16, .6, .7, .6, .025);

            for (ServerPlayerEntity player : activePlayers(world, a)) {
                double distance = player.squaredDistanceTo(
                        p.getX() + .5, player.getY(), p.getZ() + .5);
                if (distance <= radius * radius) {
                    player.damage(world.getDamageSources().mobAttack(a.boss), damage);

                    if (a.phase >= 3) {
                        Vec3d pull = new Vec3d(
                                p.getX() + .5 - player.getX(),
                                0,
                                p.getZ() + .5 - player.getZ());
                        double length = Math.sqrt(pull.x * pull.x + pull.z * pull.z);
                        if (length > .1) {
                            double strength = a.phase == 3 ? .16 : .24;
                            player.addVelocity(
                                    pull.x / length * strength,
                                    .04,
                                    pull.z / length * strength);
                            player.velocityModified = true;
                        }
                    }
                }
            }

            ring(world, p.getX() + .5, 81.2, p.getZ() + .5,
                    radius, ParticleTypes.REVERSE_PORTAL, 40);
        }
    }

    private static int attackVisualId(Attack attack) {
        return switch (attack) {
            case NONE -> 0;
            case VOID_CLEAVE -> 1;
            case SCULK_RING -> 2;
            case VOID_RAIN -> 3;
            case NULL_DASH -> 4;
            case GRAVITY_WELL -> 5;
            case REALITY_TEAR -> 6;
            case COLLAPSE -> 7;
        };
    }


    /**
     * Each late phase changes the floor pattern, not just the Warden's attack list.
     * Hazards always telegraph first and only resolve while the Warden is between
     * attacks, keeping the arena readable instead of turning the fight into noise.
     */
    private static void applyPhaseMovement(ServerWorld world, ArenaState a) {
        if (a.boss == null || a.attack != Attack.NONE || a.intro > 0 || a.recoveryTicks > 0) return;

        double angle = Math.atan2(a.boss.getZ() - .5, a.boss.getX() - .5);
        double radius = switch (a.phase) {
            case 1 -> 2.0;
            case 2 -> 7.0;
            case 3 -> 11.0;
            default -> 14.0;
        };

        double desiredAngle = angle + (a.phase % 2 == 0 ? 0.018 : -0.018);
        double targetX = .5 + Math.cos(desiredAngle) * radius;
        double targetZ = .5 + Math.sin(desiredAngle) * radius;
        double dx = targetX - a.boss.getX();
        double dz = targetZ - a.boss.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);

        if (len > .15) {
            double speed = switch (a.phase) {
                case 1 -> .025;
                case 2 -> .045;
                case 3 -> .065;
                default -> .08;
            };
            a.boss.addVelocity(dx / len * speed, 0, dz / len * speed);
            a.boss.setVelocity(a.boss.getVelocity().multiply(.82, 1.0, .82));
            a.boss.velocityModified = true;
        }

        if (a.ticks % 10 == 0) {
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    a.boss.getX(), a.boss.getY() + .15, a.boss.getZ(),
                    2 + a.phase, .25, .05, .25, .004);
        }
    }

    private static void tickPhaseArena(ServerWorld world, ArenaState a) {
        if (a.phase < 2 || a.defeated || a.attack != Attack.NONE) {
            if (a.hazardTicks > 0) {
                a.hazardTicks = 0;
                a.hazardPattern = -1;
            }
            return;
        }

        int interval = switch (a.phase) {
            case 2 -> 100;
            case 3 -> 80;
            default -> 60;
        };
        int warning = switch (a.phase) {
            case 2 -> 30;
            case 3 -> 24;
            default -> 20;
        };

        if (a.hazardTicks <= 0) {
            if (a.ticks % interval != 0) return;
            a.hazardTicks = warning;
            a.hazardPattern++;
            int patterns = a.phase == 3 ? 8 : 4;
            if (a.hazardPattern >= patterns) a.hazardPattern = 0;
            world.playSound(null, CENTER, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                    SoundCategory.HOSTILE, .8f, 1.0f + a.phase * .12f);
        }

        a.hazardTicks--;

        if (a.hazardTicks > 0) {
            if (a.phase == 2) {
                warningWedge(world, a.hazardPattern, a.hazardTicks, warning);
            } else if (a.phase == 3) {
                warningRingSegment(world, a.hazardPattern, a.hazardTicks, warning);
            } else {
                warningCollapseRing(world, a.hazardTicks, warning);
            }
        } else {
            if (a.phase == 2) {
                resolveWedge(world, a);
            } else if (a.phase == 3) {
                resolveRingSegment(world, a);
            } else {
                resolveCollapseRing(world, a);
            }
        }
    }

    private static void warningWedge(ServerWorld world, int sector, int remaining, int warning) {
        double centerAngle = sector * Math.PI / 2.0 + Math.PI / 4.0;
        double strength = .35 + (warning - remaining) / (double) warning * .65;
        for (int r = 7; r <= 21; r += 2) {
            for (int side = -2; side <= 2; side++) {
                double angle = centerAngle + side * .045;
                world.spawnParticles(ParticleTypes.SCULK_SOUL,
                        .5 + Math.cos(angle) * r, 80.25,
                        .5 + Math.sin(angle) * r, 1, .08, .02, .08, .004 + strength * .006);
            }
        }
    }

    private static void warningRingSegment(ServerWorld world, int segment, int remaining, int warning) {
        double start = segment * Math.PI / 4.0;
        double end = start + Math.PI / 4.0;
        for (double angle = start; angle <= end; angle += Math.PI / 32.0) {
            for (int r = 8; r <= 20; r += 3) {
                world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                        .5 + Math.cos(angle) * r, 80.25,
                        .5 + Math.sin(angle) * r, 1, .05, .02, .05,
                        .006 + (warning - remaining) * .0003);
            }
        }
    }

    private static void warningCollapseRing(ServerWorld world, int remaining, int warning) {
        double radius = 19.5 - (warning - remaining) * .12;
        ring(world, .5, 80.25, .5, radius, ParticleTypes.REVERSE_PORTAL, 96);
        ring(world, .5, 80.28, .5, 15.0, ParticleTypes.SCULK_SOUL, 64);
    }

    private static void resolveWedge(ServerWorld world, ArenaState a) {
        double centerAngle = a.hazardPattern * Math.PI / 2.0 + Math.PI / 4.0;
        for (ServerPlayerEntity p : activePlayers(world, a)) {
            double dx = p.getX() - .5;
            double dz = p.getZ() - .5;
            double radius = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx);
            double diff = Math.atan2(Math.sin(angle - centerAngle), Math.cos(angle - centerAngle));
            if (radius >= 6 && radius <= 22 && Math.abs(diff) < Math.PI / 4.0) {
                p.damage(world.getDamageSources().magic(), 7);
                p.addVelocity(-dx * .025, .18, -dz * .025);
                p.velocityModified = true;
            }
        }
        world.playSound(null, CENTER, SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
                SoundCategory.HOSTILE, 1.1f, 1.1f);
        ring(world, .5, 80.25, .5, 20, ParticleTypes.SCULK_SOUL, 96);
    }

    private static void resolveRingSegment(ServerWorld world, ArenaState a) {
        double start = a.hazardPattern * Math.PI / 4.0;
        double end = start + Math.PI / 4.0;
        for (ServerPlayerEntity p : activePlayers(world, a)) {
            double dx = p.getX() - .5;
            double dz = p.getZ() - .5;
            double radius = Math.sqrt(dx * dx + dz * dz);
            double angle = Math.atan2(dz, dx);
            double diff = Math.atan2(Math.sin(angle - (start + end) / 2),
                    Math.cos(angle - (start + end) / 2));
            if (radius >= 8 && radius <= 21 && Math.abs(diff) < Math.PI / 9.0) {
                p.damage(world.getDamageSources().magic(), 9);
            }
        }
        world.playSound(null, CENTER, SoundEvents.BLOCK_END_PORTAL_SPAWN,
                SoundCategory.HOSTILE, 1.0f, 1.35f);
        ring(world, .5, 80.25, .5, 18, ParticleTypes.REVERSE_PORTAL, 96);
    }

    private static void resolveCollapseRing(ServerWorld world, ArenaState a) {
        for (ServerPlayerEntity p : activePlayers(world, a)) {
            double dx = p.getX() - .5;
            double dz = p.getZ() - .5;
            double radius = Math.sqrt(dx * dx + dz * dz);
            if (radius > 15.5 && radius <= 23) {
                p.damage(world.getDamageSources().magic(), 11);
                p.addVelocity(-dx * .035, .12, -dz * .035);
                p.velocityModified = true;
            }
        }
        world.playSound(null, CENTER, SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.HOSTILE, 1.8f, .55f);
        ring(world, .5, 80.25, .5, 19, ParticleTypes.EXPLOSION, 96);
    }

    private static void beginAttack(ServerWorld world, ArenaState a) {
        ServerPlayerEntity target = selectTarget(world, a);
        if (target == null) return;

        a.attack = switch (a.phase) {
            case 1 -> Attack.VOID_CLEAVE;
            case 2 -> a.ticks % 2 == 0 ? Attack.SCULK_RING : Attack.VOID_RAIN;
            case 3 -> a.ticks % 2 == 0 ? Attack.NULL_DASH : Attack.GRAVITY_WELL;
            default -> switch ((a.ticks / 42) % 3) {
                case 0 -> Attack.REALITY_TEAR;
                case 1 -> Attack.GRAVITY_WELL;
                default -> Attack.COLLAPSE;
            };
        };
        a.attackTarget = target.getUuid();
        a.attackX = target.getX();
        a.attackY = target.getY();
        a.attackZ = target.getZ();
        a.attackWindup = a.attack.windup;
        a.boss.setVisualState(a.phase, attackVisualId(a.attack), 100, false);
        int phaseDelay = switch (a.phase) {
            case 1 -> 12;
            case 2 -> 10;
            case 3 -> 8;
            default -> 6;
        };
        a.nextAttackTick = a.ticks + a.attack.windup + a.attack.recovery + phaseDelay;
        a.recoveryTicks = a.attack.recovery + phaseDelay;

        for (ServerPlayerEntity p : participants(world, a))
            p.sendMessage(Text.literal("NULL WARDEN // " + a.attack.name), true);

        world.playSound(null, a.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                SoundCategory.HOSTILE, 2.5f, .75f);
    }

    private static ServerPlayerEntity selectTarget(ServerWorld world, ArenaState a) {
        ServerPlayerEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        ServerPlayerEntity rotated = null;
        int targetIndex = Math.floorMod(a.targetRotation, Math.max(1, a.activeParticipants.size()));
        int index = 0;

        for (UUID id : a.activeParticipants) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p == null) continue;
            if (index == targetIndex) rotated = p;

            double d = p.squaredDistanceTo(a.boss);
            if (d < closestDistance) {
                closestDistance = d;
                closest = p;
            }
            index++;
        }

        a.targetRotation++;
        if (rotated != null) return rotated;
        return closest;
    }

    private static ServerPlayerEntity target(ServerWorld world, ArenaState a) {
        if (a.attackTarget != null) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(a.attackTarget);
            if (p != null && a.activeParticipants.contains(p.getUuid())) return p;
        }
        return selectTarget(world, a);
    }

    private static void telegraph(ServerWorld world, ArenaState a) {
        ServerPlayerEntity t = target(world, a);
        if (t == null) return;
        int left = a.attackWindup;
        double tx = a.attackX, ty = a.attackY, tz = a.attackZ;
        switch (a.attack) {
            case VOID_CLEAVE -> {
                Vec3d f = a.boss.getRotationVector().normalize();
                for (int i = 0; i < 20; i++) {
                    double d = 1.5 + i * .25;
                    double spread = (i % 5 - 2) * .35;
                    world.spawnParticles(new DustParticleEffect(
                                    new Vector3f(.65f, .1f, .9f), left < 7 ? 1.8f : 1.1f),
                            a.boss.getX() + f.x * d - f.z * spread,
                            a.boss.getY() + .15,
                            a.boss.getZ() + f.z * d + f.x * spread,
                            1, 0, 0, 0, 0);
                }
            }
            case SCULK_RING -> ring(world, a.boss.getX(), a.boss.getY() + .1, a.boss.getZ(),
                    6, ParticleTypes.SCULK_SOUL, 64);
            case VOID_RAIN -> {
                for (UUID id : a.activeParticipants) {
                    ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
                    if (p != null) ring(world, p.getX(), p.getY() + .1, p.getZ(),
                            left < 7 ? 2.7 : 2.2, ParticleTypes.REVERSE_PORTAL, 36);
                }
            }
            case NULL_DASH -> ring(world, t.getX(), t.getY() + .1, t.getZ(),
                    left < 7 ? 3.5 : 2.5, ParticleTypes.REVERSE_PORTAL, 48);
            case GRAVITY_WELL, REALITY_TEAR, COLLAPSE ->
                    ring(world, t.getX(), t.getY() + .08, t.getZ(), a.attack.radius,
                            left < 7 ? ParticleTypes.EXPLOSION : ParticleTypes.REVERSE_PORTAL,
                            a.attack == Attack.COLLAPSE ? 72 : 48);
            default -> {}
        }
    }

    private static void resolveAttack(ServerWorld world, ArenaState a) {
        ServerPlayerEntity t = target(world, a);
        if (t == null || a.boss == null) return;

        switch (a.attack) {
            case VOID_CLEAVE -> {
                Vec3d f = a.boss.getRotationVector().normalize();
                Vec3d delta = t.getPos().subtract(a.boss.getPos());
                double d = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                double dot = d < .01 ? 1 : (f.x * delta.x + f.z * delta.z) / d;
                if (d <= 5 && dot > .55) t.damage(world.getDamageSources().mobAttack(a.boss), 12);
                shockwave(world, t, 3);
            }
            case SCULK_RING -> {
                for (ServerPlayerEntity p : activePlayers(world, a)) {
                    double d = p.distanceTo(a.boss);
                    if (d > 4 && d < 8) p.damage(world.getDamageSources().mobAttack(a.boss), 13);
                }
                ring(world, a.boss.getX(), a.boss.getY() + .15, a.boss.getZ(),
                        6, ParticleTypes.SCULK_SOUL, 80);
            }
            case VOID_RAIN -> {
                ring(world, a.attackX, a.attackY + .1, a.attackZ,
                        2.75, ParticleTypes.EXPLOSION, 56);
                for (ServerPlayerEntity p : activePlayers(world, a)) {
                    if (p.squaredDistanceTo(a.attackX, p.getY(), a.attackZ) <= 7.56) {
                        p.damage(world.getDamageSources().mobAttack(a.boss), 10);
                    }
                }
            }
            case NULL_DASH -> {
                Vec3d from = a.boss.getPos();
                a.boss.teleport(t.getX(), t.getY(), t.getZ(), false);
                for (ServerPlayerEntity p : activePlayers(world, a)) {
                    if (p.squaredDistanceTo(from.x, from.y, from.z) < 6.25
                            || p.squaredDistanceTo(a.boss) < 12.25)
                        p.damage(world.getDamageSources().mobAttack(a.boss), 16);
                }
                shockwave(world, t, 3.5);
            }
            case GRAVITY_WELL -> {
                pullTowardPoint(world, a, a.attackX, a.attackY, a.attackZ, .62, 12);
                for (ServerPlayerEntity p : activePlayers(world, a))
                    if (p.squaredDistanceTo(a.attackX, p.getY(), a.attackZ) < 12.25)
                        p.damage(world.getDamageSources().mobAttack(a.boss), 12);
                ring(world, a.attackX, a.attackY + .15, a.attackZ,
                        3.5, ParticleTypes.REVERSE_PORTAL, 64);
            }
            case REALITY_TEAR -> {
                for (ServerPlayerEntity p : activePlayers(world, a))
                    if (p.squaredDistanceTo(a.attackX, p.getY(), a.attackZ) < 16)
                        p.damage(world.getDamageSources().mobAttack(a.boss), 14);
                world.playSound(null, BlockPos.ofFloored(a.attackX, a.attackY, a.attackZ),
                        SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.6f, .7f);
            }
            case COLLAPSE -> {
                for (ServerPlayerEntity p : activePlayers(world, a))
                    if (p.squaredDistanceTo(a.attackX, p.getY(), a.attackZ) < 25)
                        p.damage(world.getDamageSources().mobAttack(a.boss), 20);
                pullTowardPoint(world, a, a.attackX, a.attackY, a.attackZ, .82, 15);
                ring(world, a.attackX, a.attackY + .15, a.attackZ, 9,
                        ParticleTypes.EXPLOSION, 96);
            }
            default -> {}
        }
    }

    private static void pullTowardPoint(ServerWorld world, ArenaState a,
                                          double x, double y, double z,
                                          double strength, double radius) {
        for (ServerPlayerEntity p : activePlayers(world, a)) {
            double dx = x - p.getX();
            double dz = z - p.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > .1 && len < radius) {
                double scaled = Math.min(strength, .9) * (1.0 - len / radius);
                p.addVelocity(dx / len * scaled, .08, dz / len * scaled);
                p.velocityModified = true;
            }
        }
    }

    private static void gravity(ServerWorld world, ArenaState a, double strength) {
        pullTowardPoint(world, a, a.boss.getX(), a.boss.getY(), a.boss.getZ(), strength, 15);
    }

    private static void phaseShift(ServerWorld world, ArenaState a) {
        world.playSound(null, a.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_ROAR,
                SoundCategory.HOSTILE, 3.5f, .45f);
        world.spawnParticles(ParticleTypes.EXPLOSION,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 22, 2.2, 2.0, 2.2, .06);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 120, 5, 2.5, 5, .035);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 100, 4, 3, 4, .045);
        String rule = switch (a.phase) {
            case 2 -> "Phase 2: pylon fields pulse. Commit carefully while cleansing.";
            case 3 -> "Phase 3: pylon fields pull you inward. Movement is the price of cleansing.";
            default -> "Phase 4: all pylons resonate faster. Cleanse them before the arena overwhelms you.";
        };
        for (ServerPlayerEntity p : participants(world, a)) {
            p.sendMessage(Text.literal("NULL WARDEN // PHASE " + a.phase), true);
            p.sendMessage(Text.literal("The pylons are feeding it. Sneak beside each one to cleanse it."), false);
            p.sendMessage(Text.literal(rule), false);
        }
    }

    private static void summonEcho(ServerWorld world, ArenaState a) {
        a.echoes.removeIf(id -> {
            Entity e = world.getEntity(id);
            return e == null || !e.isAlive();
        });
        if (a.echoes.size() >= Math.min(3, a.phase)) return;

        WardenEntity echo = EntityType.WARDEN.create(world);
        if (echo == null) return;
        double angle = a.ticks * .045;
        echo.refreshPositionAndAngles(Math.cos(angle) * 11, 81,
                Math.sin(angle) * 11, 0, 0);
        echo.setCustomName(Text.literal("NULL ECHO"));
        echo.setCustomNameVisible(false);
        echo.setPersistent();

        var max = echo.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        var dmg = echo.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (max != null) max.setBaseValue(65);
        if (dmg != null) dmg.setBaseValue(8);
        echo.setHealth(65);
        world.spawnEntity(echo);
        a.echoes.add(echo.getUuid());
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                echo.getX(), echo.getY() + 1, echo.getZ(), 90, .8, 1, .8, .06);
        persist(world, a);
    }

    private static void finish(ServerWorld world, ArenaState a) {
        if (a.defeated) return;
        a.defeated = true;
        a.attack = Attack.NONE;
        a.attackTarget = null;
        a.attackX = a.attackY = a.attackZ = 0;
        a.attackWindup = 0;
        a.activePylons = 0;
        a.victoryTicks = 0;
        a.boss.setHealth(1);
        a.boss.setInvulnerable(true);
        a.boss.setAiDisabled(true);
        a.boss.setVisualState(a.phase, 0, 0, true);
        if (a.bar != null) a.bar.setVisible(false);

        world.playSound(null, a.boss.getBlockPos(), SoundEvents.ENTITY_WARDEN_DEATH,
                SoundCategory.HOSTILE, 5, .45f);
        world.spawnParticles(ParticleTypes.EXPLOSION,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 48, 2, 2, 2, .08);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 420, 5, 4, 5, .1);
        world.spawnParticles(ParticleTypes.SCULK_SOUL,
                a.boss.getX(), a.boss.getY() + 1, a.boss.getZ(), 240, 4, 3, 4, .055);
        for (int r = 3; r <= 12; r += 3) {
            ring(world, a.boss.getX(), 81.0, a.boss.getZ(), r,
                    ParticleTypes.END_ROD, 64);
        }

        rewardPending(world.getServer(), world, a);
        buildReturnPortal(world, a);
        persist(world, a);
    }

    private static void rewardPending(MinecraftServer server, ServerWorld world, ArenaState a) {
        for (UUID id : a.eligiblePlayers) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(id);
            if (p != null && p.getServerWorld() == world) rewardIfEligible(p, a);
        }
        persist(world, a);
    }

    private static void rewardIfEligible(ServerPlayerEntity p, ArenaState a) {
        if (!a.eligiblePlayers.contains(p.getUuid())) return;
        if (!a.rewardedPlayers.add(p.getUuid())) return;
        p.getInventory().offerOrDrop(new ItemStack(ModItems.NULLBLADE));
        p.getInventory().offerOrDrop(new ItemStack(ModItems.NULL_RELIC));
        p.sendMessage(Text.literal("THE NULL WARDEN HAS FALLEN"), false);
        p.sendMessage(Text.literal("A Null Realm reward has been claimed."), false);
    }

    private static void reset(ServerWorld world, ArenaState a) {
        cleanup(world, a);
        removeReturnPortal(world);
        clearSaved(world);
        if (a.bar != null) for (UUID id : a.participants) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p != null) a.bar.removePlayer(p);
        }
        a.boss = null;
        a.bossUuid = null;
        a.bar = null;
        a.participants.clear();
        a.activeParticipants.clear();
        a.rewardedPlayers.clear();
        a.eligiblePlayers.clear();
        a.joinTicks.clear();
        a.echoes.clear();
        a.defeated = false;
        a.returnPortalBuilt = false;
        a.phase = 1;
        a.activePylons = 0;
        a.pylonProgress = new int[4];
        a.attack = Attack.NONE;
        a.attackTarget = null;
    }

    private static void cleanup(ServerWorld world, ArenaState a) {
        if (a.bossUuid != null) {
            Entity e = world.getEntity(a.bossUuid);
            if (e != null) e.discard();
        }
        for (UUID id : a.echoes) {
            Entity e = world.getEntity(id);
            if (e != null) e.discard();
        }
        a.echoes.clear();
    }

    private static void buildArena(ServerWorld world) {
        // A deliberately constructed ritual arena instead of a flat boss platform.
        for (int x = -ARENA_RADIUS; x <= ARENA_RADIUS; x++) {
            for (int z = -ARENA_RADIUS; z <= ARENA_RADIUS; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > ARENA_RADIUS) continue;

                BlockPos floor = new BlockPos(x, 80, z);
                if (d < 7) {
                    world.setBlockState(floor, Blocks.POLISHED_BLACKSTONE.getDefaultState());
                } else if (((Math.abs(x) + Math.abs(z)) % 4) == 0) {
                    world.setBlockState(floor, Blocks.CRYING_OBSIDIAN.getDefaultState());
                } else {
                    world.setBlockState(floor, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                }

                for (int y = 81; y <= 100; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }

        // Raised central ritual dais.
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                if (x * x + z * z <= 36) {
                    world.setBlockState(new BlockPos(x, 81, z),
                            Blocks.POLISHED_BLACKSTONE.getDefaultState());
                    if (x * x + z * z <= 9) {
                        world.setBlockState(new BlockPos(x, 82, z),
                                Blocks.CRYING_OBSIDIAN.getDefaultState());
                    }
                }
            }
        }

        // Four monumental pylon towers with collars and floating caps.
        for (int i = 0; i < 4; i++) {
            BlockPos p = PYLONS[i];

            for (int y = 0; y < 11; y++) {
                world.setBlockState(p.add(0, y, 0),
                        Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                if (y >= 3 && y <= 8) {
                    world.setBlockState(p.add(1, y, 0),
                            Blocks.SCULK.getDefaultState());
                    world.setBlockState(p.add(-1, y, 0),
                            Blocks.SCULK.getDefaultState());
                }
            }

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                        world.setBlockState(p.add(x, 11, z),
                                Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                    }
                }
            }
            world.setBlockState(p.up(12), Blocks.SCULK_CATALYST.getDefaultState());
            world.setBlockState(p.up(13), Blocks.SCULK.getDefaultState());
        }

        // Four outer monuments make the circular boundary read as an actual arena.
        BlockPos[] corners = {
                new BlockPos(19, 81, 19), new BlockPos(-19, 81, 19),
                new BlockPos(-19, 81, -19), new BlockPos(19, 81, -19)
        };
        for (BlockPos base : corners) {
            for (int y = 0; y < 8; y++) {
                world.setBlockState(base.add(0, y, 0), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
                if (y > 1) {
                    world.setBlockState(base.add(1, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
                    world.setBlockState(base.add(-1, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState());
                }
            }
            world.setBlockState(base.up(8), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
            world.setBlockState(base.up(9), Blocks.SCULK_CATALYST.getDefaultState());
        }

        // Cardinal arches frame the boss without closing the sky.
        for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            int dx = dir[0], dz = dir[1];
            for (int side = -3; side <= 3; side++) {
                int x = dx * 22 + (dz != 0 ? side : 0);
                int z = dz * 22 + (dx != 0 ? side : 0);
                world.setBlockState(new BlockPos(x, 81, z), Blocks.REINFORCED_DEEPSLATE.getDefaultState());
                world.setBlockState(new BlockPos(x, 82, z), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
            }
            for (int y = 82; y <= 88; y++) {
                world.setBlockState(new BlockPos(dx * 22, y, dz * 22),
                        Blocks.REINFORCED_DEEPSLATE.getDefaultState());
            }
        }
    }

    private static void buildReturnPortal(ServerWorld world, ArenaState a) {
        if (a.returnPortalBuilt) return;
        for (int x = -1; x <= 1; x++)
            for (int y = 0; y < 4; y++)
                world.setBlockState(RETURN_PORTAL.add(x, y, 0), ModBlocks.VOID_PORTAL.getDefaultState());
        a.returnPortalBuilt = true;
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                13.5, 83, 13.5, 160, 1.5, 2.2, 1.5, .12);
    }

    private static void removeReturnPortal(ServerWorld world) {
        for (int x = -1; x <= 1; x++)
            for (int y = 0; y < 4; y++) {
                BlockPos p = RETURN_PORTAL.add(x, y, 0);
                if (world.getBlockState(p).isOf(ModBlocks.VOID_PORTAL))
                    world.setBlockState(p, Blocks.AIR.getDefaultState());
            }
    }

    private static void ring(ServerWorld world, double x, double y, double z,
                             double radius, ParticleEffect effect, int points) {
        for (int i = 0; i < points; i++) {
            double angle = i * Math.PI * 2 / points;
            world.spawnParticles(effect, x + Math.cos(angle) * radius, y,
                    z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
        }
    }

    private static void shockwave(ServerWorld world, ServerPlayerEntity p, double radius) {
        ring(world, p.getX(), p.getY() + .15, p.getZ(), radius, ParticleTypes.PORTAL, 80);
    }

    private static Set<ServerPlayerEntity> activePlayers(ServerWorld world, ArenaState a) {
        Set<ServerPlayerEntity> result = new HashSet<>();
        for (UUID id : a.activeParticipants) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p != null && p.getServerWorld() == world && p.isAlive()) result.add(p);
        }
        return result;
    }

    private static Set<ServerPlayerEntity> participants(ServerWorld world, ArenaState a) {
        Set<ServerPlayerEntity> result = new HashSet<>();
        for (UUID id : a.participants) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(id);
            if (p != null && p.getServerWorld() == world) result.add(p);
        }
        return result;
    }

    public static void saveReturnPoint(ServerPlayerEntity player, RegistryKey<World> worldKey,
                                       BlockPos pos, float yaw, float pitch) {
        ServerWorld nullRealm = player.getServer().getWorld(
                RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,
                        net.minecraft.util.Identifier.of(dev.qynl.myfirstmod.MyFirstMod.MOD_ID, "null_realm")));
        if (nullRealm == null) return;

        NullWardenState data = saved(nullRealm);
        data.returnPoints.put(player.getUuid(), new NullWardenState.ReturnPointData(
                worldKey.getValue().toString(), pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));
        data.dirty();
    }

    public static NullWardenState.ReturnPointData takeReturnPoint(ServerPlayerEntity player) {
        NullWardenState data = saved(player.getServerWorld());
        NullWardenState.ReturnPointData point = data.returnPoints.remove(player.getUuid());
        data.dirty();
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

        final int windup, recovery;
        final double radius;
        final String name;

        Attack(int windup, int recovery, double radius, String name) {
            this.windup = windup;
            this.recovery = recovery;
            this.radius = radius;
            this.name = name;
        }
    }

    private static final class ArenaState {
        NullWardenEntity boss;
        UUID bossUuid;
        ServerBossBar bar;
        final Set<UUID> participants = new HashSet<>();
        final Set<UUID> activeParticipants = new HashSet<>();
        final Set<UUID> eligiblePlayers = new HashSet<>();
        final Set<UUID> rewardedPlayers = new HashSet<>();
        final Set<UUID> echoes = new HashSet<>();
        final Map<UUID, Integer> joinTicks = new HashMap<>();
        int ticks, intro, phase = 1, idleTicks, nextAttackTick = 40, attackWindup;
        int victoryTicks, targetRotation;
        int hazardTicks, hazardPattern = -1;
        int recoveryTicks;
        int activePylons;
        int[] pylonProgress = new int[4];
        double attackX, attackY, attackZ;
        UUID attackTarget;
        Attack attack = Attack.NONE;
        boolean defeated, returnPortalBuilt;
    }
}