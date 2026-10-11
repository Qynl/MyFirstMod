package dev.qynl.racers.net;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.race.RaceSession;
import dev.qynl.racers.track.Track;
import dev.qynl.racers.track.TrackManager;
import dev.qynl.racers.track.TrackPlacement;

/**
 * All packets in one place.
 *
 * <p>Four payloads cover everything: car controls (20/s while driving), a
 * generic client->server action envelope, a generic server->client answer, and
 * the race HUD snapshot.
 */
public final class ModNetworking {
    // --- actions -----------------------------------------------------------
    public static final int ACT_TRACK_LIST = 1;
    public static final int ACT_PLACEMENTS = 2;
    public static final int ACT_GARAGE = 3;
    public static final int ACT_NOTIFY = 4;
    public static final int ACT_PLACE_RESULT = 5;
    public static final int ACT_PROFILE = 6;
    public static final int ACT_RACE_SETUP = 7;
    public static final int ACT_RECORD_STATE = 8;
    public static final int ACT_CAR_LIST = 9;

    // --- client requests ---------------------------------------------------
    public static final int REQ_GARAGE = 1;
    public static final int REQ_TUNE = 2;
    public static final int REQ_PAINT = 3;
    public static final int REQ_AI_TOGGLE = 4;
    public static final int REQ_PLACE_TRACK = 5;
    public static final int REQ_REMOVE_TRACK = 6;
    public static final int REQ_HOST_RACE = 7;
    public static final int REQ_JOIN_RACE = 8;
    public static final int REQ_LEAVE_RACE = 9;
    public static final int REQ_STATS = 10;
    public static final int REQ_RECORD_TRACK = 11;
    public static final int REQ_SPECTATE = 12;
    public static final int REQ_REPAIR = 13;

    private ModNetworking() {
    }

    // ------------------------------------------------------------------
    // payload records
    // ------------------------------------------------------------------
    /** Driver input, sent every tick while a player is behind the wheel. */
    public record CarControl(int entityId, int mask) implements net.minecraft.network.packet.CustomPayload {
        public static final Id<CarControl> ID = new Id<>(FabricRacers.id("car_control"));
        public static final Codec<CarControl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("entity").forGetter(CarControl::entityId),
                Codec.INT.fieldOf("mask").forGetter(CarControl::mask)
        ).apply(instance, CarControl::new));

        @Override
        public Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return ID;
        }
    }

    /** Generic client request envelope. */
    public record ClientAction(int action, int entityId, int a, int b, String text)
            implements net.minecraft.network.packet.CustomPayload {
        public static final Id<ClientAction> ID = new Id<>(FabricRacers.id("client_action"));
        public static final Codec<ClientAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("action").forGetter(ClientAction::action),
                Codec.INT.fieldOf("entity").forGetter(ClientAction::entityId),
                Codec.INT.fieldOf("a").forGetter(ClientAction::a),
                Codec.INT.fieldOf("b").forGetter(ClientAction::b),
                Codec.STRING.fieldOf("text").forGetter(ClientAction::text)
        ).apply(instance, ClientAction::new));

        @Override
        public Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return ID;
        }

        public static ClientAction of(int action, int entityId, int a, int b) {
            return new ClientAction(action, entityId, a, b, "");
        }
    }

    /** Generic server answer: a code, four numbers, a message and a data blob. */
    public record ServerAction(int action, int entityId, int a, int b, float f, String text,
                               NbtCompound data)
            implements net.minecraft.network.packet.CustomPayload {
        public static final Id<ServerAction> ID = new Id<>(FabricRacers.id("server_action"));
        public static final Codec<ServerAction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("action").forGetter(ServerAction::action),
                Codec.INT.fieldOf("entity").forGetter(ServerAction::entityId),
                Codec.INT.fieldOf("a").forGetter(ServerAction::a),
                Codec.INT.fieldOf("b").forGetter(ServerAction::b),
                Codec.FLOAT.fieldOf("f").forGetter(ServerAction::f),
                Codec.STRING.fieldOf("text").forGetter(ServerAction::text),
                NbtCompound.CODEC.fieldOf("data").forGetter(ServerAction::data)
        ).apply(instance, ServerAction::new));

        @Override
        public Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return ID;
        }

        public static ServerAction of(int action, int entityId, int a, int b, int f, String text) {
            return new ServerAction(action, entityId, a, b, f, text, new NbtCompound());
        }
    }

    /** The race HUD: everything the overlay needs for one player, 5x per second. */
    public record Hud(int state, int countdown, int lap, int laps, int position, int total,
                      int timeMs, int lapTimeMs, int bestLapMs, int speedKmh, double boost,
                      int driftScore, int gapAheadMs, int gapBehindMs, int flags,
                      String trackName, String message, String standings, String carName)
            implements net.minecraft.network.packet.CustomPayload {
        public static final Id<Hud> ID = new Id<>(FabricRacers.id("hud"));
        public static final Codec<Hud> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("state").forGetter(Hud::state),
                Codec.INT.fieldOf("countdown").forGetter(Hud::countdown),
                Codec.INT.fieldOf("lap").forGetter(Hud::lap),
                Codec.INT.fieldOf("laps").forGetter(Hud::laps),
                Codec.INT.fieldOf("position").forGetter(Hud::position),
                Codec.INT.fieldOf("total").forGetter(Hud::total),
                Codec.INT.fieldOf("timeMs").forGetter(Hud::timeMs),
                Codec.INT.fieldOf("lapTimeMs").forGetter(Hud::lapTimeMs),
                Codec.INT.fieldOf("bestLapMs").forGetter(Hud::bestLapMs),
                Codec.INT.fieldOf("speed").forGetter(Hud::speedKmh),
                Codec.DOUBLE.fieldOf("boost").forGetter(Hud::boost),
                Codec.INT.fieldOf("drift").forGetter(Hud::driftScore),
                Codec.INT.fieldOf("gapAhead").forGetter(Hud::gapAheadMs),
                Codec.INT.fieldOf("gapBehind").forGetter(Hud::gapBehindMs),
                Codec.INT.fieldOf("flags").forGetter(Hud::flags),
                Codec.STRING.fieldOf("track").forGetter(Hud::trackName),
                Codec.STRING.fieldOf("message").forGetter(Hud::message),
                Codec.STRING.fieldOf("standings").forGetter(Hud::standings),
                Codec.STRING.fieldOf("car").forGetter(Hud::carName)
        ).apply(instance, Hud::new));

        @Override
        public Id<? extends net.minecraft.network.packet.CustomPayload> getId() {
            return ID;
        }

        public static final int FLAG_DRIFTING = 1;
        public static final int FLAG_BOOSTING = 2;
        public static final int FLAG_OFF_TRACK = 4;
        public static final int FLAG_DAMAGED = 8;
        public static final int FLAG_FINISHED = 16;

        public boolean flag(int flag) {
            return (flags & flag) != 0;
        }
    }

    // ------------------------------------------------------------------
    // registration
    // ------------------------------------------------------------------
    public static void init() {
        PayloadTypeRegistry.playC2S().register(CarControl.ID, CarControl.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientAction.ID, ClientAction.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerAction.ID, ServerAction.CODEC);
        PayloadTypeRegistry.playS2C().register(Hud.ID, Hud.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CarControl.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            var world = player.getServerWorld();
            var entity = world.getEntityById(payload.entityId());
            if (entity instanceof CarEntity car && player.getVehicle() == car) {
                car.setDriverInput(dev.qynl.racers.car.CarControls.fromMask((byte) payload.mask()));
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(ClientAction.ID, (payload, context) ->
                ServerActions.handle(context.player(), payload));
    }

    // ------------------------------------------------------------------
    // server -> client helpers
    // ------------------------------------------------------------------
    public static void send(ServerPlayerEntity player, ServerAction action) {
        ServerPlayNetworking.send(player, action);
    }

    public static void notify(ServerPlayerEntity player, String message) {
        send(player, ServerAction.of(ACT_NOTIFY, 0, 0, 0, 0, message));
    }

    public static void sendHud(ServerPlayerEntity player, Hud hud) {
        ServerPlayNetworking.send(player, hud);
    }

    /** Sends the list of tracks the player may build (for the track wand screen). */
    public static void sendTrackList(ServerPlayerEntity player, TrackManager manager, BlockPos anchor) {
        NbtCompound data = new NbtCompound();
        NbtList list = new NbtList();
        for (Track track : manager.all()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("id", track.id);
            entry.putString("name", track.name);
            entry.putInt("sx", track.size[0]);
            entry.putInt("sy", track.size[1]);
            entry.putInt("sz", track.size[2]);
            entry.putInt("cps", track.checkpointCount());
            entry.putInt("laps", track.defaultLaps);
            entry.putInt("width", track.width);
            entry.putBoolean("stock", track.stock);
            list.add(entry);
        }
        data.put("tracks", list);
        NbtList anchorList = new NbtList();
        anchorList.add(net.minecraft.nbt.NbtInt.of(anchor.getX()));
        anchorList.add(net.minecraft.nbt.NbtInt.of(anchor.getY()));
        anchorList.add(net.minecraft.nbt.NbtInt.of(anchor.getZ()));
        data.put("anchor", anchorList);
        data.putInt("rotation", manager.suggestedRotation(player));
        send(player, new ServerAction(ACT_TRACK_LIST, 0, 0, 0, 0.0F, "", data));
    }

    /** Sends every placed circuit so the client can draw them + show a map. */
    public static void sendPlacements(ServerPlayerEntity player, TrackManager manager) {
        NbtCompound data = new NbtCompound();
        NbtList list = new NbtList();
        for (TrackPlacement placement : manager.placements()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("track", placement.track.id);
            entry.putInt("x", placement.anchor.getX());
            entry.putInt("y", placement.anchor.getY());
            entry.putInt("z", placement.anchor.getZ());
            entry.putInt("rot", placement.rotation);
            list.add(entry);
        }
        data.put("list", list);
        send(player, new ServerAction(ACT_PLACEMENTS, 0, 0, 0, 0.0F, "", data));
    }

    /** Sends the car's tuning sheet to the garage screen. */
    public static void sendGarage(ServerPlayerEntity player, CarEntity car, NbtCompound extra) {
        NbtCompound data = new NbtCompound();
        data.put("car", car.data().toNbt());
        data.putInt("health", (int) (car.healthFraction() * 100.0F));
        data.putString("driver", car.driverName());
        data.putBoolean("ai", car.isAiDriving());
        if (extra != null) {
            data.put("extra", extra);
        }
        send(player, new ServerAction(ACT_GARAGE, car.getId(), 0, 0, 0.0F, "", data));
    }

    /** Sends the player's racing record. */
    public static void sendProfile(ServerPlayerEntity player, NbtCompound profile) {
        send(player, new ServerAction(ACT_PROFILE, 0, 0, 0, 0.0F, "", profile));
    }

    /** Tells every player in the race something (chat + HUD banner). */
    public static void broadcastRace(MinecraftServer server, RaceSession session, String message) {
        for (ServerPlayerEntity player : session.viewers()) {
            notify(player, message);
        }
    }

    public static void sendRaceSetup(ServerPlayerEntity player, NbtCompound data) {
        send(player, new ServerAction(ACT_RACE_SETUP, 0, 0, 0, 0.0F, "", data));
    }

    public static void sendRecordState(ServerPlayerEntity player, int state, String name) {
        send(player, ServerAction.of(ACT_RECORD_STATE, 0, state, 0, 0, name));
    }

    /** Convenience used by commands that run on a server without any player. */
    public static List<ServerPlayerEntity> players(MinecraftServer server) {
        return server.getPlayerManager().getPlayerList();
    }
}
