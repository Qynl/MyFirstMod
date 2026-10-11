package dev.qynl.racers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import dev.qynl.racers.car.CarControls;
import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.util.Stopwatch;

/**
 * Everything the client keeps in memory: the latest HUD snapshot, the list of
 * circuits the server offers, the tuning sheet, the player's profile, plus the
 * driving input and camera handling.
 */
public final class ClientRaceState {
    private ClientRaceState() {
    }

    // --- hud -----------------------------------------------------------------
    public static ModNetworking.Hud hud;
    public static long hudTime;
    public static boolean hudEnabled = true;

    // --- tracks --------------------------------------------------------------
    public record TrackInfo(String id, String name, int sizeX, int sizeY, int sizeZ,
                            int checkpoints, int laps, int width, boolean stock) {
    }

    public record PlacementInfo(String track, BlockPos anchor, int rotation) {
    }

    public static final List<TrackInfo> tracks = new ArrayList<>();
    public static final List<PlacementInfo> placements = new ArrayList<>();
    public static BlockPos wandAnchor;
    public static int wandRotation;

    // --- garage --------------------------------------------------------------
    public static CarData garageData;
    public static int garageEntityId = -1;
    public static int garageHealth = 100;
    public static boolean garageAi;
    public static NbtCompound garageExtra = new NbtCompound();

    // --- profile -------------------------------------------------------------
    public static NbtCompound profile = new NbtCompound();
    public static String notifyMessage = "";
    public static long notifyTime;
    public static int recordState;

    private static int lastVehicleId = -1;
    private static boolean thirdPersonForced;

    // ------------------------------------------------------------------
    // networking
    // ------------------------------------------------------------------
    public static void setHud(ModNetworking.Hud payload) {
        hud = payload;
        hudTime = System.currentTimeMillis();
    }

    public static void handle(ModNetworking.ServerAction payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        switch (payload.action()) {
            case ModNetworking.ACT_TRACK_LIST -> {
                tracks.clear();
                NbtCompound data = payload.data();
                NbtList list = data.getList("tracks", 10);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound entry = list.getCompound(i);
                    tracks.add(new TrackInfo(entry.getString("id"), entry.getString("name"),
                            entry.getInt("sx"), entry.getInt("sy"), entry.getInt("sz"),
                            entry.getInt("cps"), entry.getInt("laps"), entry.getInt("width"),
                            entry.getBoolean("stock")));
                }
                if (data.contains("anchor")) {
                    NbtList anchor = data.getList("anchor", 3);
                    wandAnchor = new BlockPos(anchor.getInt(0), anchor.getInt(1), anchor.getInt(2));
                    wandRotation = data.getInt("rotation");
                }
                if (!tracks.isEmpty() && client.currentScreen == null) {
                    client.setScreen(new dev.qynl.racers.client.screen.TrackSelectScreen(Text
                            .translatable("screen.fabricracers.tracks")));
                }
            }
            case ModNetworking.ACT_PLACEMENTS -> {
                placements.clear();
                NbtList list = payload.data().getList("list", 10);
                for (int i = 0; i < list.size(); i++) {
                    NbtCompound entry = list.getCompound(i);
                    placements.add(new PlacementInfo(entry.getString("track"),
                            new BlockPos(entry.getInt("x"), entry.getInt("y"), entry.getInt("z")),
                            entry.getInt("rot")));
                }
            }
            case ModNetworking.ACT_GARAGE -> {
                garageData = CarData.fromNbt(payload.data().getCompound("car"));
                garageEntityId = payload.entityId();
                garageHealth = payload.data().getInt("health");
                garageAi = payload.data().getBoolean("ai");
                garageExtra = payload.data().getCompound("extra");
                if (client.currentScreen == null) {
                    client.setScreen(new dev.qynl.racers.client.screen.GarageScreen());
                }
            }
            case ModNetworking.ACT_PROFILE -> profile = payload.data();
            case ModNetworking.ACT_NOTIFY -> {
                if (!"garage".equals(payload.text())) {
                    notifyMessage = payload.text();
                    notifyTime = System.currentTimeMillis();
                }
            }
            case ModNetworking.ACT_PLACE_RESULT -> {
                notifyMessage = payload.text();
                notifyTime = System.currentTimeMillis();
            }
            case ModNetworking.ACT_RECORD_STATE -> {
                recordState = payload.a();
                notifyMessage = payload.text();
                notifyTime = System.currentTimeMillis();
            }
            default -> {
            }
        }
    }

    public static void send(int action, int entityId, int a, int b, String text) {
        if (MinecraftClient.getInstance().getNetworkHandler() == null) {
            return;
        }
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new ModNetworking.ClientAction(action, entityId, a, b, text));
    }

    public static void requestGarage() {
        if (MinecraftClient.getInstance().player != null
                && MinecraftClient.getInstance().player.getVehicle() instanceof CarEntity car) {
            send(ModNetworking.REQ_GARAGE, car.getId(), 0, 0, "");
        }
    }

    // ------------------------------------------------------------------
    // per tick: input + camera
    // ------------------------------------------------------------------
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        while (FabricRacersClient.openGarage.wasPressed()) {
            requestGarage();
        }
        while (FabricRacersClient.openTracks.wasPressed()) {
            if (!tracks.isEmpty()) {
                client.setScreen(new dev.qynl.racers.client.screen.TrackSelectScreen(
                        Text.translatable("screen.fabricracers.tracks")));
            } else {
                client.setScreen(new dev.qynl.racers.client.screen.RacePanelScreen(
                        Text.translatable("screen.fabricracers.panel")));
            }
        }
        while (FabricRacersClient.toggleHud.wasPressed()) {
            hudEnabled = !hudEnabled;
            notifyMessage = hudEnabled ? "HUD on" : "HUD off";
            notifyTime = System.currentTimeMillis();
        }

        // ---- driving -----------------------------------------------------
        if (player.getVehicle() instanceof CarEntity car) {
            CarControls controls = new CarControls();
            controls.forward = client.options.forwardKey.isPressed();
            controls.back = client.options.backKey.isPressed();
            controls.left = client.options.leftKey.isPressed();
            controls.right = client.options.rightKey.isPressed();
            controls.handbrake = FabricRacersClient.handbrake.isPressed() || client.options.sneakKey.isPressed();
            controls.boost = FabricRacersClient.boost.isPressed();
            if (client.getNetworkHandler() != null) {
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new ModNetworking.CarControl(car.getId(), controls.mask()));
            }
            // chase camera: keep the player looking where the car is going
            if (player.getUuid().equals(car.data().owner) || car.driverName().isEmpty()
                    || car.driverName().equals(player.getName().getString())) {
                float delta = MathHelper.wrapDegrees(car.getYaw() - player.getYaw());
                player.setYaw(player.getYaw() + delta * 0.4F);
                if (player == client.player) {
                    player.prevYaw += MathHelper.wrapDegrees((player.getYaw() - player.prevYaw)) * 0.6F;
                }
            }
            if (lastVehicleId != car.getId()) {
                lastVehicleId = car.getId();
                thirdPersonForced = false;
            }
            if (!thirdPersonForced) {
                thirdPersonForced = true;
                if (client.options.getPerspective() == Perspective.FIRST_PERSON) {
                    client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                }
            }
        } else {
            lastVehicleId = -1;
            thirdPersonForced = false;
        }
    }

    // ------------------------------------------------------------------
    // helpers for the HUD + screens
    // ------------------------------------------------------------------
    public static boolean hudFresh() {
        return hud != null && System.currentTimeMillis() - hudTime < 1500L;
    }

    public static long stopwatchElapsed() {
        Stopwatch stopwatch = Stopwatch.items();
        return stopwatch.running() ? stopwatch.elapsed(System.currentTimeMillis()) : 0L;
    }

    public static CarEntity nearbyCar(MinecraftClient client) {
        if (client.player == null) {
            return null;
        }
        if (client.player.getVehicle() instanceof CarEntity car) {
            return car;
        }
        return dev.qynl.racers.util.Cars.carNear(client.player.getWorld(), client.player.getPos(), 6.0);
    }

    public static void notify(String message) {
        notifyMessage = message;
        notifyTime = System.currentTimeMillis();
    }

    public static String formatMs(int millis) {
        return Stopwatch.format(millis);
    }

    public static String color(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static Text localRaceLine() {
        if (!hudFresh() || hud.state() < 0) {
            return Text.translatable("screen.fabricracers.no_race").formatted(Formatting.GRAY);
        }
        return Text.literal(Optional.ofNullable(hud.trackName()).orElse("")).formatted(Formatting.GOLD);
    }
}
