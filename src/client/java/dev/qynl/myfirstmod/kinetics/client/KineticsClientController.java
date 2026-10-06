package dev.qynl.myfirstmod.kinetics.client;

import dev.qynl.myfirstmod.kinetics.KineticMath;
import dev.qynl.myfirstmod.kinetics.KineticsConfig;
import dev.qynl.myfirstmod.kinetics.KineticsNetworking;
import dev.qynl.myfirstmod.kinetics.KineticsWorldUtil;
import dev.qynl.myfirstmod.kinetics.NullhookEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * The client brain of Null Kinetics. Runs once per client tick after the
 * player ticks and does three things:
 *
 * <ol>
 *   <li><b>Detect input edges</b> (jump in air, dash key, crouch while sprinting).</li>
 *   <li><b>Predict</b> the exact motion the server will apply using the shared
 *       {@link KineticMath}, so abilities feel instant with zero rubber-banding.</li>
 *   <li><b>Simulate continuous windows</b> (wall run, glide, slide friction,
 *       grapple pull) that the server re-validates every tick.</li>
 * </ol>
 */
public final class KineticsClientController {
    private KineticsClientController() {}

    // Input edges
    private static boolean prevJump = false;
    private static boolean prevSneak = false;
    private static boolean prevDash = false;
    private static boolean prevHudToggle = false;

    // Local window bookkeeping
    private static int wallRunNoWallTicks = 0;
    private static int wallRunRequestCooldown = 0;
    private static int glideHeartbeat = 0;
    private static int glideResendDelay = 0;
    private static int dashTrailTicks = 0;

    public static void tick(MinecraftClient client) {
        KineticsClientEffects.tick();

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.options == null) {
            KineticsClientState.reset();
            return;
        }

        KineticsConfig cfg = KineticsConfig.get();
        KineticsClientState st = KineticsClientState.INSTANCE;

        // Local timers -------------------------------------------------
        if (st.localDashCooldown > 0) st.localDashCooldown--;
        if (st.localSlideCooldown > 0) st.localSlideCooldown--;
        if (st.dashFlashTicks > 0) st.dashFlashTicks--;
        if (st.flowPulseTicks > 0) st.flowPulseTicks--;
        if (wallRunRequestCooldown > 0) wallRunRequestCooldown--;

        // Jump charges refill locally while grounded.
        if (player.isOnGround()) {
            st.localAirJumps = cfg.airJumps;
        }

        boolean jumpHeld = client.options.jumpKey.isPressed();
        boolean sneakHeld = client.options.sneakKey.isPressed();
        boolean dashHeld = KineticsKeybinds.dashKey != null && KineticsKeybinds.dashKey.isPressed();
        boolean hudToggleHeld = KineticsKeybinds.hudToggleKey != null && KineticsKeybinds.hudToggleKey.isPressed();

        if (hudToggleHeld && !prevHudToggle) {
            st.hudVisible = !st.hudVisible;
        }
        prevHudToggle = hudToggleHeld;

        boolean kineticsOk = cfg.enabled && KineticsWorldUtil.canUseKinetics(player)
                && !player.getAbilities().flying && !player.isSpectator();

        // ------------------------------------------------------------------
        // Dash
        // ------------------------------------------------------------------
        if (kineticsOk && dashHeld && !prevDash && st.abilityEnabled(KineticsNetworking.ABILITY_DASH)
                && st.dashCooldownDisplay() <= 0 && st.energy >= cfg.dashEnergy * 0.5) {
            st.localDashCooldown = cfg.dashCooldownTicks;
            st.dashFlashTicks = 8;
            dashTrailTicks = 8;
            KineticsClientEffects.kick(1.0);
            player.setVelocity(KineticMath.dashVelocity(player, cfg));
            playLocal(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.5F, 1.35F);
            playLocal(SoundEvents.ENTITY_BREEZE_JUMP, 0.35F, 0.7F);
            ClientPlayNetworking.send(new KineticsNetworking.DashPayload());
        }
        prevDash = dashHeld;

        // Dash particle trail
        if (dashTrailTicks > 0) {
            dashTrailTicks--;
            Vec3d p = player.getPos();
            player.getWorld().addParticle(ParticleTypes.ELECTRIC_SPARK, false,
                    p.x - player.getVelocity().x * 0.5, p.y + 0.9, p.z - player.getVelocity().z * 0.5,
                    0.0, 0.01, 0.0);
            player.getWorld().addParticle(ParticleTypes.REVERSE_PORTAL, false,
                    p.x, p.y + 0.6, p.z, 0.0, 0.0, 0.0);
        }

        // ------------------------------------------------------------------
        // Jump intent: wall jump / air jump / slide hop
        // ------------------------------------------------------------------
        if (jumpHeld && !prevJump && kineticsOk) {
            if (st.sliding) {
                // Slide hop — keep momentum, pop a little.
                Vec3d v = player.getVelocity();
                player.setVelocity(v.x, 0.34, v.z);
                st.sliding = false;
                st.localSlideCooldown = cfg.slideCooldownTicks;
                playLocal(SoundEvents.ENTITY_BREEZE_LAND, 0.4F, 1.4F);
                ClientPlayNetworking.send(new KineticsNetworking.JumpIntentPayload());
            } else if (!player.isOnGround()) {
                Direction wall = KineticsWorldUtil.findWallDirection(player);
                if (wall != null && st.abilityEnabled(KineticsNetworking.ABILITY_WALL_RUN)) {
                    player.setVelocity(KineticMath.wallJumpVelocity(player, wall, cfg));
                    KineticsClientEffects.kick(0.35);
                    playLocal(SoundEvents.ENTITY_BREEZE_JUMP, 0.6F, 1.45F);
                    ClientPlayNetworking.send(new KineticsNetworking.JumpIntentPayload());
                } else if (st.abilityEnabled(KineticsNetworking.ABILITY_DOUBLE_JUMP)
                        && st.localAirJumps > 0 && st.energy >= cfg.airJumpEnergy * 0.5) {
                    st.localAirJumps--;
                    player.setVelocity(KineticMath.airJumpVelocity(player, cfg));
                    KineticsClientEffects.kick(0.3);
                    playLocal(SoundEvents.ENTITY_GOAT_LONG_JUMP, 0.7F, 1.15F);
                    Vec3d p = player.getPos();
                    for (int i = 0; i < 10; i++) {
                        double angle = i * Math.PI * 2.0 / 10.0;
                        player.getWorld().addParticle(ParticleTypes.SCULK_SOUL, false,
                                p.x + Math.cos(angle) * 0.5, p.y + 0.15, p.z + Math.sin(angle) * 0.5,
                                0.0, 0.02, 0.0);
                    }
                    ClientPlayNetworking.send(new KineticsNetworking.JumpIntentPayload());
                }
            }
        }
        prevJump = jumpHeld;

        // ------------------------------------------------------------------
        // Slide start: crouch tap while sprinting on the ground
        // ------------------------------------------------------------------
        if (kineticsOk && sneakHeld && !prevSneak && player.isOnGround() && player.isSprinting()
                && !st.sliding && st.localSlideCooldown <= 0
                && st.abilityEnabled(KineticsNetworking.ABILITY_SLIDE)
                && st.energy >= cfg.slideEnergy * 0.5) {
            Vec3d dir = KineticMath.slideDirection(player);
            if (dir != null) {
                Vec3d v = player.getVelocity();
                player.setVelocity(new Vec3d(dir.x * cfg.slideSpeed, Math.min(v.y, 0.0), dir.z * cfg.slideSpeed));
                st.sliding = true;
                st.localSlideCooldown = cfg.slideCooldownTicks;
                playLocal(SoundEvents.ENTITY_BREEZE_SLIDE, 0.6F, 0.75F);
                ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                        KineticsNetworking.WINDOW_SLIDE_START));
            }
        }
        prevSneak = sneakHeld;

        // ------------------------------------------------------------------
        // Slide friction (keep momentum against vanilla friction)
        // ------------------------------------------------------------------
        if (st.sliding) {
            if (player.isOnGround()) {
                double slip = player.getWorld()
                        .getBlockState(player.getBlockPos().down())
                        .getBlock().getSlipperiness();
                player.setVelocity(KineticMath.slideFrictionTick(
                        player, player.getVelocity(), slip, cfg));
                if (player.age % 3 == 0) {
                    Vec3d p = player.getPos();
                    player.getWorld().addParticle(ParticleTypes.CLOUD, false,
                            p.x, p.y + 0.1, p.z, 0.0, 0.01, 0.0);
                }
            }
        }

        // ------------------------------------------------------------------
        // Wall run
        // ------------------------------------------------------------------
        if (kineticsOk && st.abilityEnabled(KineticsNetworking.ABILITY_WALL_RUN)) {
            boolean holdingTowardWall = player.input != null && player.input.pressingForward;
            Direction wall = KineticsWorldUtil.findWallDirection(player);
            Vec3d v = player.getVelocity();
            double horizSpeed = Math.sqrt(v.x * v.x + v.z * v.z);

            boolean wants = !player.isOnGround() && wall != null && holdingTowardWall
                    && horizSpeed >= cfg.wallRunMinSpeed && st.energy > cfg.wallRunEnergyPerTick * 10;

            if (wants && !st.wallRunning && wallRunRequestCooldown <= 0) {
                st.wallRunning = true; // optimistic; server confirms or denies via sync
                st.wallRunTicksLeft = cfg.wallRunMaxTicks;
                wallRunNoWallTicks = 0;
                playLocal(SoundEvents.ENTITY_BREEZE_SLIDE, 0.4F, 1.2F);
                ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                        KineticsNetworking.WINDOW_WALLRUN_START));
            }

            if (st.wallRunning) {
                if (!wants && wall == null) {
                    if (++wallRunNoWallTicks > 6) {
                        st.wallRunning = false;
                        ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                                KineticsNetworking.WINDOW_WALLRUN_STOP));
                    }
                } else if (!wants) {
                    // Stopped wanting the wall run (grounded, slow, released
                    // forward) — close the window explicitly.
                    st.wallRunning = false;
                    ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                            KineticsNetworking.WINDOW_WALLRUN_STOP));
                } else {
                    wallRunNoWallTicks = 0;
                    st.wallRunTicksLeft--;
                    player.setVelocity(KineticMath.wallRunTick(player, wall, cfg));
                    if (player.age % 4 == 0) {
                        Vec3d p = player.getPos();
                        player.getWorld().addParticle(ParticleTypes.SOUL, false,
                                p.x + wall.getOffsetX() * 0.45, p.y + 0.4,
                                p.z + wall.getOffsetZ() * 0.45, 0.0, 0.02, 0.0);
                    }
                    if (st.wallRunTicksLeft <= 0) {
                        // Window expired — allow an immediate re-request if
                        // the wall and energy are still there.
                        st.wallRunning = false;
                        wallRunRequestCooldown = 5;
                    }
                }
            }
        } else if (st.wallRunning) {
            st.wallRunning = false;
        }

        // ------------------------------------------------------------------
        // Glide (hold key while falling)
        // ------------------------------------------------------------------
        boolean glideHeld = KineticsKeybinds.glideKey != null && KineticsKeybinds.glideKey.isPressed();
        if (kineticsOk && st.abilityEnabled(KineticsNetworking.ABILITY_GLIDE)) {
            boolean wants = glideHeld && !player.isOnGround() && player.getVelocity().y < -0.1
                    && st.energy > cfg.glideEnergyPerTick * 10;

            if (wants) {
                st.glideTicksHeld++;
                if (glideResendDelay > 0) glideResendDelay--;
                boolean needStart = !st.gliding && glideResendDelay == 0;
                boolean renew = st.gliding && ++glideHeartbeat >= 60;
                if (needStart || renew) {
                    glideHeartbeat = 0;
                    glideResendDelay = 5;
                    if (needStart) {
                        playLocal(SoundEvents.ENTITY_PHANTOM_FLAP, 0.4F, 0.85F);
                    }
                    ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                            KineticsNetworking.WINDOW_GLIDE_START));
                    st.gliding = true;
                }
                player.setVelocity(KineticMath.glideTick(player, cfg));
                if (player.age % 5 == 0) {
                    Vec3d p = player.getPos();
                    player.getWorld().addParticle(ParticleTypes.CLOUD, false,
                            p.x, p.y + 0.4, p.z, 0.0, 0.02, 0.0);
                }
            } else if (st.gliding) {
                st.gliding = false;
                st.glideTicksHeld = 0;
                glideHeartbeat = 0;
                ClientPlayNetworking.send(new KineticsNetworking.WindowPayload(
                        KineticsNetworking.WINDOW_GLIDE_STOP));
            }
        } else if (st.gliding) {
            st.gliding = false;
        }

        // ------------------------------------------------------------------
        // Grapple pull prediction
        // ------------------------------------------------------------------
        if (st.grappleEntityId >= 0 && !player.isOnGround()) {
            Entity entity = player.getWorld().getEntityById(st.grappleEntityId);
            if (entity instanceof NullhookEntity hook && hook.isAnchored()) {
                Vec3d pull = KineticMath.grapplePull(
                        player.getPos().add(0.0, 0.5, 0.0), hook.getPos(), cfg);
                player.setVelocity(pull);
                if (player.age % 3 == 0) {
                    Vec3d p = player.getPos();
                    player.getWorld().addParticle(ParticleTypes.GLOW, false,
                            p.x, p.y + 0.7, p.z, 0.0, 0.0, 0.0);
                }
            }
        }
    }

    private static void playLocal(net.minecraft.sound.SoundEvent sound, float volume, float pitch) {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(sound, pitch, volume));
    }
}
