package dev.qynl.myfirstmod.kinetics;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_GLIDE_START;
import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_GLIDE_STOP;
import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_SLIDE_START;
import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_SLIDE_STOP;
import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_WALLRUN_START;
import static dev.qynl.myfirstmod.kinetics.KineticsNetworking.WINDOW_WALLRUN_STOP;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative brain of the Null Kinetics movement suite.
 *
 * <p>The client predicts every ability with {@link KineticMath}, then asks this
 * manager (via {@link KineticsNetworking} payloads) to validate and apply the
 * same motion for real. Windows (wall run / glide / slide) are continuously
 * re-validated here: energy is drained, fall distance is forgiven, and stale
 * windows are closed the moment the player touches the ground.
 */
public final class KineticsManager {
    private static final Map<UUID, KineticsState> STATES = new ConcurrentHashMap<>();
    private static final Identifier FLOW_MODIFIER_ID = Identifier.of(MyFirstMod.MOD_ID, "flow_speed");

    private KineticsManager() {}

    /** Single time base for every window/grace comparison. */
    private static long now(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        return server != null ? server.getTicks() : 0L;
    }

    public static KineticsState getState(ServerPlayerEntity player) {
        return STATES.computeIfAbsent(player.getUuid(), id -> new KineticsState());
    }

    public static void onPlayerJoin(ServerPlayerEntity player) {
        KineticsState state = getState(player);
        state.energy = KineticsConfig.get().maxEnergy;
        state.markDirty();
    }

    public static void onPlayerLeave(UUID id) {
        STATES.remove(id);
    }

    // ------------------------------------------------------------------
    // Main tick
    // ------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        long tick = server.getTicks();
        KineticsConfig cfg = KineticsConfig.get();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            KineticsState state = getState(player);
            ServerWorld world = player.getServerWorld();

            // ---- cooldowns -------------------------------------------------
            if (state.dashCooldown > 0) {
                state.dashCooldown--;
                state.markDirty();
            }
            if (state.airJumpCooldown > 0) state.airJumpCooldown--;
            if (state.slideCooldown > 0) {
                state.slideCooldown--;
                state.markDirty();
            }

            // ---- grounded resets ------------------------------------------
            if (player.isOnGround()) {
                if (state.airJumpsLeft != cfg.airJumps) {
                    state.airJumpsLeft = cfg.airJumps;
                    state.markDirty();
                }
                state.airborneJumpsChain = 0;
                state.wallJumpsChain = 0;
                if (state.wallRunUntil >= 0) {
                    state.wallRunUntil = -1;
                    state.markDirty();
                }
                if (state.glideUntil >= 0) {
                    state.glideUntil = -1;
                    state.markDirty();
                }
                if (state.slideUntil >= 0) {
                    state.slideUntil = -1;
                    state.markDirty();
                }
            }

            // ---- kinetic landing burst -------------------------------------
            boolean windowsActive = state.isWallRunning(tick) || state.isGliding(tick)
                    || state.isSliding(tick) || state.grappleEntityId >= 0
                    || state.hasFallGrace(tick);
            if (player.isOnGround() && !state.wasOnGround
                    && (state.hadKineticWindow || windowsActive)) {
                Vec3d v = player.getVelocity();
                double speed = Math.sqrt(v.x * v.x + v.z * v.z);
                if (speed > 0.45) {
                    int count = Math.min(40, (int) (speed * 24));
                    Vec3d pos = player.getPos();
                    world.spawnParticles(ParticleTypes.CLOUD,
                            pos.x, pos.y + 0.1, pos.z, count, 0.4, 0.06, 0.4, 0.06);
                    world.spawnParticles(ParticleTypes.CRIT,
                            pos.x, pos.y + 0.15, pos.z, count / 2, 0.35, 0.12, 0.35, 0.08);
                    world.playSound(null, player.getBlockPos(),
                            KineticsSounds.event(SoundEvents.ENTITY_BREEZE_LAND),
                            SoundCategory.PLAYERS,
                            Math.min(0.8F, 0.35F + (float) speed * 0.25F), 0.9F);
                }
            }
            state.wasOnGround = player.isOnGround();
            state.hadKineticWindow = windowsActive;

            // ---- window maintenance ---------------------------------------
            tickWallRun(player, state, tick, cfg);
            tickGlide(player, state, world, tick, cfg);
            tickSlide(player, state, tick, cfg);

            // ---- energy ----------------------------------------------------
            tickEnergy(player, state, cfg);

            // ---- flow ------------------------------------------------------
            tickFlow(player, state, cfg);

            // ---- grapple sanity --------------------------------------------
            if (state.grappleEntityId >= 0) {
                Entity hook = world.getEntityById(state.grappleEntityId);
                if (!(hook instanceof NullhookEntity)) {
                    state.grappleEntityId = -1;
                    state.markDirty();
                }
            }

            // ---- fall damage grace ----------------------------------------
            if (state.hasFallGrace(tick)
                    || state.isWallRunning(tick)
                    || state.isGliding(tick)
                    || state.isSliding(tick)
                    || state.grappleEntityId >= 0) {
                player.fallDistance = 0.0F;
            }

            // ---- sync ------------------------------------------------------
            if (state.dirty && tick - state.lastSyncTick >= 2) {
                KineticsNetworking.sendState(player, state);
                state.dirty = false;
                state.lastSyncTick = tick;
            }
        }
    }

    private static void tickWallRun(ServerPlayerEntity player, KineticsState state, long tick, KineticsConfig cfg) {
        if (!state.isWallRunning(tick)) return;
        if (player.isOnGround() || !KineticsWorldUtil.canUseKinetics(player)) {
            state.wallRunUntil = -1;
            state.markDirty();
            return;
        }
        state.energy -= cfg.wallRunEnergyPerTick;
        state.markDirty();
        if (state.energy <= 0.0) {
            state.energy = 0.0;
            state.wallRunUntil = -1;
            return;
        }
        Direction wall = KineticsWorldUtil.findWallDirection(player);
        if (wall == null && tick % 4 == 0) {
            // The client may briefly lose wall contact around block seams.
            // If the wall is really gone for several ticks, cut the window.
            state.wallRunUntil = Math.min(state.wallRunUntil, tick + 4);
        }
        if (tick % 8 == 0) {
            if (wall != null) {
                player.getServerWorld().playSound(
                        null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_BREEZE_SLIDE),
                        SoundCategory.PLAYERS, 0.35F, 1.1F + player.getRandom().nextFloat() * 0.2F);
                Vec3d at = player.getPos().add(
                        wall.getOffsetX() * 0.45, 0.4 + player.getRandom().nextDouble() * 0.4,
                        wall.getOffsetZ() * 0.45);
                player.getServerWorld().spawnParticles(ParticleTypes.SOUL,
                        at.x, at.y, at.z, 2, 0.05, 0.2, 0.05, 0.01);
            }
        }
    }

    private static void tickGlide(ServerPlayerEntity player, KineticsState state, ServerWorld world, long tick, KineticsConfig cfg) {
        if (!state.isGliding(tick)) return;
        if (player.isOnGround() || !KineticsWorldUtil.canUseKinetics(player) || state.energy <= 0.0) {
            state.glideUntil = -1;
            state.markDirty();
            return;
        }
        state.energy -= cfg.glideEnergyPerTick;
        state.totalGlideTicks++;
        state.markDirty();
        if (state.totalGlideTicks == 200) {
            KineticsAdvancements.grant(player, "drifter");
        }
        if (tick % 12 == 0) {
            world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_PHANTOM_FLAP),
                    SoundCategory.PLAYERS, 0.25F, 0.9F);
        }
    }

    private static void tickSlide(ServerPlayerEntity player, KineticsState state, long tick, KineticsConfig cfg) {
        if (!state.isSliding(tick)) return;
        if (player.isOnGround()) {
            // Mirror the client's anti-friction so the server velocity stays
            // in sync with what the client is actually doing.
            Vec3d v = player.getVelocity();
            if (Math.abs(v.x) > 0.05 || Math.abs(v.z) > 0.05) {
                Vec3d restored = KineticMath.slideFrictionTick(player, v, groundSlipperiness(player), cfg);
                player.setVelocity(restored);
                player.velocityModified = true;
            }
        }
    }

    private static void tickEnergy(ServerPlayerEntity player, KineticsState state, KineticsConfig cfg) {
        if (state.regenDelayTicks > 0) {
            state.regenDelayTicks--;
            return;
        }
        double rate = player.isOnGround() ? cfg.energyRegenGround : cfg.energyRegenAir;
        if (state.energy < cfg.maxEnergy) {
            state.energy = Math.min(cfg.maxEnergy, state.energy + rate);
            state.markDirty();
        }
    }

    private static void tickFlow(ServerPlayerEntity player, KineticsState state, KineticsConfig cfg) {
        if (!cfg.flowEnabled) {
            clearFlow(player, state);
            return;
        }
        if (state.flowStacks <= 0) return;
        state.flowTimer--;
        if (state.flowTimer <= 0) {
            state.flowStacks--;
            state.flowTimer = state.flowStacks > 0 ? cfg.flowWindowTicks : 0;
            state.markDirty();
            if (state.flowStacks == 0) {
                clearFlow(player, state);
                return;
            }
        }
        applyFlowModifier(player, state, cfg);
    }

    private static void applyFlowModifier(ServerPlayerEntity player, KineticsState state, KineticsConfig cfg) {
        EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;
        double target = state.flowStacks * cfg.flowSpeedPerStack;
        EntityAttributeModifier existing = attr.getModifier(FLOW_MODIFIER_ID);
        if (existing == null || Math.abs(existing.value() - target) > 1.0E-6) {
            attr.removeModifier(FLOW_MODIFIER_ID);
            attr.addTemporaryModifier(new EntityAttributeModifier(
                    FLOW_MODIFIER_ID, target, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        state.flowModifierApplied = true;
    }

    private static void clearFlow(ServerPlayerEntity player, KineticsState state) {
        if (state.flowModifierApplied) {
            EntityAttributeInstance attr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null) {
                attr.removeModifier(FLOW_MODIFIER_ID);
            }
            state.flowModifierApplied = false;
        }
        if (state.flowStacks != 0) {
            state.flowStacks = 0;
            state.flowTimer = 0;
            state.markDirty();
        }
    }

    // ------------------------------------------------------------------
    // Shared gate for every ability
    // ------------------------------------------------------------------

    private static boolean canAct(ServerPlayerEntity player, KineticsConfig cfg) {
        return cfg.enabled && KineticsWorldUtil.canUseKinetics(player) && !player.getAbilities().flying
                && (!cfg.requireRelic || player.getInventory().contains(new ItemStack(ModItems.NULL_RELIC)));
    }

    /** Effective energy cost after Flow discounts. */
    public static double cost(KineticsState state, double base, KineticsConfig cfg) {
        double reduction = cfg.flowEnabled ? state.flowStacks * cfg.flowCostReductionPerStack : 0.0;
        return base * Math.max(0.25, 1.0 - reduction);
    }

    private static boolean trySpend(ServerPlayerEntity player, KineticsState state, double base, KineticsConfig cfg) {
        double cost = cost(state, base, cfg);
        if (state.energy < cost) {
            player.getServerWorld().playSound(
                    null, player.getBlockPos(), KineticsSounds.event(SoundEvents.BLOCK_NOTE_BLOCK_PLING),
                    SoundCategory.PLAYERS, 0.5F, 0.55F);
            return false;
        }
        state.energy -= cost;
        state.regenDelayTicks = Math.max(state.regenDelayTicks, cfg.energyRegenDelay);
        return true;
    }

    private static void grantFallGrace(KineticsState state, long tick, KineticsConfig cfg) {
        state.fallGraceUntil = tick + cfg.fallDamageGraceTicks;
    }

    // ------------------------------------------------------------------
    // Ability: Void Dash
    // ------------------------------------------------------------------

    public static boolean tryDash(ServerPlayerEntity player) {
        KineticsConfig cfg = KineticsConfig.get();
        KineticsState state = getState(player);
        if (!canAct(player, cfg) || !cfg.dashEnabled || state.dashCooldown > 0) return false;
        if (!trySpend(player, state, cfg.dashEnergy, cfg)) return false;

        state.dashCooldown = cfg.dashCooldownTicks;
        player.setVelocity(KineticMath.dashVelocity(player, cfg));
        player.velocityModified = true;
        grantFallGrace(state, now(player), cfg);
        addFlow(player, state, cfg);
        state.markDirty();

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_ENDERMAN_TELEPORT),
                SoundCategory.PLAYERS, 0.7F, 1.35F);
        world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_BREEZE_JUMP),
                SoundCategory.PLAYERS, 0.5F, 0.7F);
        Vec3d pos = player.getPos();
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 26, 0.35, 0.5, 0.35, 0.16);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 0.9, pos.z, 14, 0.3, 0.4, 0.3, 0.08);

        KineticsAdvancements.grantRoot(player);
        KineticsAdvancements.grant(player, "void_dash");
        return true;
    }

    // ------------------------------------------------------------------
    // Jump intent: wall jump / air jump / slide hop
    // ------------------------------------------------------------------

    public static boolean tryJumpIntent(ServerPlayerEntity player) {
        KineticsConfig cfg = KineticsConfig.get();
        KineticsState state = getState(player);
        if (!canAct(player, cfg)) return false;
        long tick = now(player);

        // Slide hop: jump pressed mid-slide pops a low, momentum-keeping hop.
        // (Not gated by slideCooldown — that spaces consecutive slides, not hops.)
        if (state.isSliding(tick)) {
            if (trySpend(player, state, cfg.slideEnergy * 0.5, cfg)) {
                Vec3d v = player.getVelocity();
                player.setVelocity(v.x, 0.34, v.z);
                player.velocityModified = true;
                state.slideUntil = -1;
                state.slideCooldown = cfg.slideCooldownTicks;
                grantFallGrace(state, tick, cfg);
                addFlow(player, state, cfg);
                state.markDirty();
                player.getServerWorld().playSound(null, player.getBlockPos(),
                        KineticsSounds.event(SoundEvents.ENTITY_BREEZE_LAND), SoundCategory.PLAYERS, 0.6F, 1.4F);
                return true;
            }
            return false;
        }

        if (player.isOnGround()) return false;

        // Wall jump has priority over air jumps — it feels better.
        Direction wall = KineticsWorldUtil.findWallDirection(player);
        if (wall != null && cfg.wallRunEnabled) {
            if (trySpend(player, state, cfg.wallJumpEnergy, cfg)) {
                player.setVelocity(KineticMath.wallJumpVelocity(player, wall, cfg));
                player.velocityModified = true;
                grantFallGrace(state, tick, cfg);
                state.airJumpsLeft = Math.max(state.airJumpsLeft, 1);
                state.airborneJumpsChain++;
                state.wallJumpsChain++;
                if (state.wallJumpsChain >= 3) {
                    KineticsAdvancements.grant(player, "wall_dancer");
                }
                addFlow(player, state, cfg);
                state.markDirty();

                ServerWorld world = player.getServerWorld();
                world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_BREEZE_JUMP),
                        SoundCategory.PLAYERS, 0.8F, 1.45F);
                BlockPos at = player.getBlockPos().offset(wall);
                world.spawnParticles(ParticleTypes.SOUL,
                        at.getX() + 0.5 - wall.getOffsetX() * 0.5, player.getY() + 0.8,
                        at.getZ() + 0.5 - wall.getOffsetZ() * 0.5, 10, 0.2, 0.3, 0.2, 0.03);
                KineticsAdvancements.grantRoot(player);
                return true;
            }
            return false;
        }

        // Air jump.
        if (!cfg.doubleJumpEnabled || state.airJumpsLeft <= 0 || state.airJumpCooldown > 0) return false;
        if (!trySpend(player, state, cfg.airJumpEnergy, cfg)) return false;

        state.airJumpsLeft--;
        state.airJumpCooldown = cfg.airJumpCooldownTicks;
        player.setVelocity(KineticMath.airJumpVelocity(player, cfg));
        player.velocityModified = true;
        grantFallGrace(state, tick, cfg);
        state.airborneJumpsChain++;
        if (state.airborneJumpsChain >= 5) {
            KineticsAdvancements.grant(player, "airwalker");
        }
        addFlow(player, state, cfg);
        state.markDirty();

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_GOAT_LONG_JUMP),
                SoundCategory.PLAYERS, 0.9F, 1.15F);
        Vec3d pos = player.getPos();
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 0.2, pos.z, 16, 0.35, 0.05, 0.35, 0.02);
        world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y - 0.1, pos.z, 6, 0.25, 0.02, 0.25, 0.02);
        KineticsAdvancements.grantRoot(player);
        return true;
    }

    // ------------------------------------------------------------------
    // Windows: wall run / glide / slide
    // ------------------------------------------------------------------

    public static boolean tryWindow(ServerPlayerEntity player, int type) {
        KineticsConfig cfg = KineticsConfig.get();
        KineticsState state = getState(player);
        long tick = now(player);

        switch (type) {
            case WINDOW_WALLRUN_START -> {
                if (!canAct(player, cfg) || !cfg.wallRunEnabled || player.isOnGround()) return false;
                if (state.isWallRunning(tick)) return false;
                Direction wall = KineticsWorldUtil.findWallDirection(player);
                if (wall == null) return false;
                Vec3d v = player.getVelocity();
                if (Math.sqrt(v.x * v.x + v.z * v.z) < cfg.wallRunMinSpeed) return false;
                if (state.energy < cfg.wallRunEnergyPerTick * 10) return false;
                state.wallRunUntil = tick + cfg.wallRunMaxTicks;
                grantFallGrace(state, tick, cfg);
                state.markDirty();
                KineticsAdvancements.grantRoot(player);
                player.getServerWorld().playSound(null, player.getBlockPos(),
                        KineticsSounds.event(SoundEvents.ENTITY_BREEZE_SLIDE), SoundCategory.PLAYERS, 0.6F, 1.2F);
                return true;
            }
            case WINDOW_WALLRUN_STOP -> {
                if (state.wallRunUntil >= 0) {
                    state.wallRunUntil = -1;
                    state.markDirty();
                }
                return true;
            }
            case WINDOW_GLIDE_START -> {
                if (!canAct(player, cfg) || !cfg.glideEnabled || player.isOnGround()) return false;
                if (player.getVelocity().y >= -0.1) return false;
                if (state.energy < cfg.glideEnergyPerTick * 10) return false;
                state.glideUntil = tick + 120; // renewable: the client re-requests while held
                grantFallGrace(state, tick, cfg);
                state.markDirty();
                KineticsAdvancements.grantRoot(player);
                player.getServerWorld().playSound(null, player.getBlockPos(),
                        KineticsSounds.event(SoundEvents.ENTITY_PHANTOM_FLAP), SoundCategory.PLAYERS, 0.5F, 0.85F);
                return true;
            }
            case WINDOW_GLIDE_STOP -> {
                if (state.glideUntil >= 0) {
                    state.glideUntil = -1;
                    state.markDirty();
                }
                return true;
            }
            case WINDOW_SLIDE_START -> {
                if (!canAct(player, cfg) || !cfg.slideEnabled || !player.isOnGround()) return false;
                if (!player.isSprinting() || state.isSliding(tick) || state.slideCooldown > 0) return false;
                if (!trySpend(player, state, cfg.slideEnergy, cfg)) return false;
                Vec3d dir = KineticMath.slideDirection(player);
                if (dir == null) return false;
                Vec3d v = player.getVelocity();
                player.setVelocity(new Vec3d(dir.x * cfg.slideSpeed, Math.min(v.y, 0.0), dir.z * cfg.slideSpeed));
                player.velocityModified = true;
                state.slideUntil = tick + cfg.slideTicks;
                state.slideCooldown = cfg.slideCooldownTicks;
                grantFallGrace(state, tick, cfg);
                addFlow(player, state, cfg);
                state.markDirty();
                KineticsAdvancements.grantRoot(player);

                ServerWorld world = player.getServerWorld();
                world.playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.ENTITY_BREEZE_SLIDE),
                        SoundCategory.PLAYERS, 0.9F, 0.75F);
                Vec3d pos = player.getPos();
                world.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.1, pos.z, 10, 0.3, 0.05, 0.3, 0.04);
                world.spawnParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.2, pos.z, 8, 0.3, 0.1, 0.3, 0.06);
                return true;
            }
            case WINDOW_SLIDE_STOP -> {
                if (state.slideUntil >= 0) {
                    state.slideUntil = -1;
                    state.markDirty();
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------
    // Flow
    // ------------------------------------------------------------------

    private static void addFlow(ServerPlayerEntity player, KineticsState state, KineticsConfig cfg) {
        if (!cfg.flowEnabled) return;
        boolean chained = state.flowTimer > 0;
        state.flowStacks = chained ? Math.min(cfg.flowMaxStacks, state.flowStacks + 1) : 1;
        state.flowTimer = cfg.flowWindowTicks;
        state.markDirty();

        player.getServerWorld().playSound(null, player.getBlockPos(), KineticsSounds.event(SoundEvents.BLOCK_NOTE_BLOCK_PLING),
                SoundCategory.PLAYERS, 0.45F, 0.9F + state.flowStacks * 0.12F);
        if (state.flowStacks >= cfg.flowMaxStacks) {
            KineticsAdvancements.grant(player, "flow_state");
            ServerWorld world = player.getServerWorld();
            Vec3d pos = player.getPos();
            world.spawnParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1.0, pos.z, 24, 0.4, 0.6, 0.4, 0.05);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    public static double groundSlipperiness(ServerPlayerEntity player) {
        BlockPos below = player.getBlockPos().down();
        return player.getServerWorld().getBlockState(below).getBlock().getSlipperiness();
    }

    public static void setEnergy(ServerPlayerEntity player, double value) {
        KineticsState state = getState(player);
        state.energy = Math.max(0.0, Math.min(KineticsConfig.get().maxEnergy, value));
        state.markDirty();
    }

    public static void noteGrapple(ServerPlayerEntity player, double distance) {
        KineticsState state = getState(player);
        if (distance >= 20.0 && !state.grappleAdvancementDone) {
            state.grappleAdvancementDone = true;
            KineticsAdvancements.grant(player, "hooked");
        }
        KineticsAdvancements.grantRoot(player);
    }

    public static void handleGrappleEnd(ServerPlayerEntity player) {
        KineticsState state = getState(player);
        if (state.grappleEntityId >= 0) {
            state.grappleEntityId = -1;
            state.markDirty();
        }
    }
}
