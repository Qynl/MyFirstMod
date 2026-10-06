package dev.qynl.myfirstmod.kinetics;

import dev.qynl.myfirstmod.MyFirstMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Custom payloads for the Null Kinetics movement suite.
 *
 * <p>Client → server packets are <b>intents</b> ("I want to dash"); the server
 * re-validates everything and applies the authoritative motion. Server →
 * client packets carry a compact state snapshot used by the HUD.
 */
public final class KineticsNetworking {
    private KineticsNetworking() {}

    // ------------------------------------------------------------------
    // Payloads
    // ------------------------------------------------------------------

    /** Client pressed the dash key. */
    public record DashPayload() implements CustomPayload {
        public static final DashPayload INSTANCE = new DashPayload();
        public static final CustomPayload.Id<DashPayload> ID =
                new CustomPayload.Id<>(Identifier.of(MyFirstMod.MOD_ID, "dash"));
        public static final PacketCodec<RegistryByteBuf, DashPayload> CODEC =
                PacketCodec.unit(INSTANCE);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Client pressed jump while airborne (or while sliding) — server decides semantics. */
    public record JumpIntentPayload() implements CustomPayload {
        public static final JumpIntentPayload INSTANCE = new JumpIntentPayload();
        public static final CustomPayload.Id<JumpIntentPayload> ID =
                new CustomPayload.Id<>(Identifier.of(MyFirstMod.MOD_ID, "jump_intent"));
        public static final PacketCodec<RegistryByteBuf, JumpIntentPayload> CODEC =
                PacketCodec.unit(INSTANCE);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Start/stop a continuous movement window (wall run, glide, slide). */
    public record WindowPayload(int type) implements CustomPayload {
        public static final CustomPayload.Id<WindowPayload> ID =
                new CustomPayload.Id<>(Identifier.of(MyFirstMod.MOD_ID, "window"));
        public static final PacketCodec<RegistryByteBuf, WindowPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.VAR_INT, WindowPayload::type, WindowPayload::new);

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Snapshot of the authoritative kinetics state, for the client HUD + prediction. */
    public record KineticsStatePayload(
            float energy,
            float maxEnergy,
            int airJumps,
            int dashCooldown,
            int flowStacks,
            int flags,
            int grappleEntityId,
            int abilityMask
    ) implements CustomPayload {
        public static final CustomPayload.Id<KineticsStatePayload> ID =
                new CustomPayload.Id<>(Identifier.of(MyFirstMod.MOD_ID, "state"));
        public static final PacketCodec<RegistryByteBuf, KineticsStatePayload> CODEC =
                CustomPayload.codecOf(
                        (payload, buf) -> {
                            buf.writeFloat(payload.energy());
                            buf.writeFloat(payload.maxEnergy());
                            buf.writeVarInt(payload.airJumps());
                            buf.writeVarInt(payload.dashCooldown());
                            buf.writeVarInt(payload.flowStacks());
                            buf.writeVarInt(payload.flags());
                            buf.writeVarInt(payload.grappleEntityId());
                            buf.writeVarInt(payload.abilityMask());
                        },
                        buf -> new KineticsStatePayload(
                                buf.readFloat(),
                                buf.readFloat(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt(),
                                buf.readVarInt()));

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // Window ids used by the WindowPayload packet.
    public static final int WINDOW_WALLRUN_START = 0;
    public static final int WINDOW_WALLRUN_STOP = 1;
    public static final int WINDOW_GLIDE_START = 2;
    public static final int WINDOW_GLIDE_STOP = 3;
    public static final int WINDOW_SLIDE_START = 4;
    public static final int WINDOW_SLIDE_STOP = 5;

    // Flag bits for the state payload.
    public static final int FLAG_WALL_RUNNING = 1;
    public static final int FLAG_GLIDING = 2;
    public static final int FLAG_SLIDING = 4;
    public static final int FLAG_GRAPPLING = 8;

    // Ability mask bits.
    public static final int ABILITY_DASH = 1;
    public static final int ABILITY_DOUBLE_JUMP = 2;
    public static final int ABILITY_WALL_RUN = 4;
    public static final int ABILITY_GLIDE = 8;
    public static final int ABILITY_SLIDE = 16;
    public static final int ABILITY_GRAPPLE = 32;
    public static final int ABILITY_FLOW = 64;

    // ------------------------------------------------------------------
    // Registration (called from the common initializer)
    // ------------------------------------------------------------------

    public static void register() {
        PayloadTypeRegistry.playC2S().register(DashPayload.ID, DashPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(JumpIntentPayload.ID, JumpIntentPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WindowPayload.ID, WindowPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(KineticsStatePayload.ID, KineticsStatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(DashPayload.ID, (payload, context) ->
                context.server().execute(() -> KineticsManager.tryDash(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(JumpIntentPayload.ID, (payload, context) ->
                context.server().execute(() -> KineticsManager.tryJumpIntent(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(WindowPayload.ID, (payload, context) ->
                context.server().execute(() -> KineticsManager.tryWindow(context.player(), payload.type())));
    }

    // ------------------------------------------------------------------
    // State sync
    // ------------------------------------------------------------------

    public static void sendState(ServerPlayerEntity player, KineticsState state) {
        KineticsConfig cfg = KineticsConfig.get();
        long tick = player.getServer() != null ? player.getServer().getTicks() : 0L;

        int flags = 0;
        if (state.isWallRunning(tick)) flags |= FLAG_WALL_RUNNING;
        if (state.isGliding(tick)) flags |= FLAG_GLIDING;
        if (state.isSliding(tick)) flags |= FLAG_SLIDING;
        if (state.grappleEntityId >= 0) flags |= FLAG_GRAPPLING;

        int mask = 0;
        if (cfg.enabled) {
            if (cfg.dashEnabled) mask |= ABILITY_DASH;
            if (cfg.doubleJumpEnabled) mask |= ABILITY_DOUBLE_JUMP;
            if (cfg.wallRunEnabled) mask |= ABILITY_WALL_RUN;
            if (cfg.glideEnabled) mask |= ABILITY_GLIDE;
            if (cfg.slideEnabled) mask |= ABILITY_SLIDE;
            if (cfg.grappleEnabled) mask |= ABILITY_GRAPPLE;
            if (cfg.flowEnabled) mask |= ABILITY_FLOW;
        }

        ServerPlayNetworking.send(player, new KineticsStatePayload(
                (float) state.energy,
                (float) cfg.maxEnergy,
                state.airJumpsLeft,
                state.dashCooldown,
                state.flowStacks,
                flags,
                state.grappleEntityId,
                mask));
    }
}
