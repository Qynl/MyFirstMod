package dev.qynl.racers.net;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.PaintColors;
import dev.qynl.racers.car.Tune;
import dev.qynl.racers.race.RaceEntry;
import dev.qynl.racers.race.RaceFormat;
import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.race.RaceSession;
import dev.qynl.racers.shop.PartShop;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.Track;
import dev.qynl.racers.track.TrackManager;
import dev.qynl.racers.track.TrackPlacement;
import dev.qynl.racers.util.Cars;

/** Handles every button press and menu action coming from a client. */
public final class ServerActions {
    private ServerActions() {
    }

    public static void handle(ServerPlayerEntity player, ModNetworking.ClientAction action) {
        switch (action.action()) {
            case ModNetworking.REQ_GARAGE -> openGarage(player, action.entityId());
            case ModNetworking.REQ_TUNE -> tune(player, action.entityId(), action.a());
            case ModNetworking.REQ_PAINT -> paint(player, action.entityId(), action.a(), action.b());
            case ModNetworking.REQ_AI_TOGGLE -> toggleAi(player, action.entityId(), action.a());
            case ModNetworking.REQ_PLACE_TRACK -> placeTrack(player, action.text(), action.a(), action.b());
            case ModNetworking.REQ_REMOVE_TRACK -> removeTrack(player, action.a(), action.b(), action.text());
            case ModNetworking.REQ_HOST_RACE -> hostRace(player, action);
            case ModNetworking.REQ_JOIN_RACE -> joinRace(player);
            case ModNetworking.REQ_LEAVE_RACE -> leaveRace(player);
            case ModNetworking.REQ_STATS -> StatsManager.get(player.server).sendRecord(player);
            case ModNetworking.REQ_RECORD_TRACK -> toggleRecording(player);
            case ModNetworking.REQ_SPECTATE -> spectate(player);
            case ModNetworking.REQ_REPAIR -> repair(player);
            default -> {
            }
        }
    }

    private static CarEntity carOf(ServerPlayerEntity player, int entityId) {
        if (player.getServerWorld().getEntityById(entityId) instanceof CarEntity car) {
            return car;
        }
        if (player.getVehicle() instanceof CarEntity car) {
            return car;
        }
        return Cars.carNear(player.getServerWorld(), player.getPos(), 4.0);
    }

    private static void openGarage(ServerPlayerEntity player, int entityId) {
        CarEntity car = carOf(player, entityId);
        if (car == null) {
            ModNetworking.notify(player, "No car nearby to tune.");
            return;
        }
        NbtCompound extra = new NbtCompound();
        for (Tune tune : Tune.values()) {
            extra.putInt(tune.id, PartShop.costFor(tune, car.data().level(tune) + 1));
            extra.putInt(tune.id + "_owned", countParts(player,
                    dev.qynl.racers.registry.ModItems.part(tune)));
        }
        ModNetworking.sendGarage(player, car, extra);
    }

    private static void tune(ServerPlayerEntity player, int entityId, int tuneIndex) {
        CarEntity car = carOf(player, entityId);
        if (car == null || tuneIndex < 0 || tuneIndex >= Tune.values().length) {
            return;
        }
        if (!car.data().ownedBy(player)) {
            ModNetworking.notify(player, "That is not your car.");
            return;
        }
        Tune tune = Tune.values()[tuneIndex];
        int level = car.data().level(tune);
        if (level >= tune.maxLevel) {
            ModNetworking.notify(player, tune.displayName + " is already maxed out.");
            return;
        }
        int cost = PartShop.costFor(tune, level + 1);
        if (player.isCreative() || countParts(player, dev.qynl.racers.registry.ModItems.part(tune)) >= cost) {
            if (!player.isCreative()) {
                consumeParts(player, dev.qynl.racers.registry.ModItems.part(tune), cost);
            }
            int[] levels = car.levels();
            levels[tune.ordinal()] = level + 1;
            car.setLevels(levels);
            car.resetHealth();
            player.sendMessage(Text.translatable("message.fabricracers.tuned",
                    tune.displayName, level + 1, tune.maxLevel).formatted(Formatting.GREEN), true);
            openGarage(player, car.getId());
        } else {
            ModNetworking.notify(player, "You need " + cost + "x " + tune.displayName
                    + " part (you have " + countParts(player,
                    dev.qynl.racers.registry.ModItems.part(tune)) + ").");
        }
    }

    private static void paint(ServerPlayerEntity player, int entityId, int slot, int colorIndex) {
        CarEntity car = carOf(player, entityId);
        if (car == null || !car.data().ownedBy(player)) {
            return;
        }
        int color = PaintColors.named().containsValue(colorIndex) ? colorIndex
                : PaintColors.next(slot == 0 ? car.data().primaryColor : car.data().secondaryColor, 1);
        if (slot == 0) {
            car.setPaint(color, car.data().secondaryColor);
        } else {
            car.setPaint(car.data().primaryColor, color);
        }
        openGarage(player, car.getId());
    }

    private static void toggleAi(ServerPlayerEntity player, int entityId, int value) {
        CarEntity car = carOf(player, entityId);
        if (car == null || !car.data().ownedBy(player)) {
            return;
        }
        if (car.isRidden() && car.getDriver() != player) {
            ModNetworking.notify(player, "Someone is driving that car.");
            return;
        }
        boolean want = value == 1;
        car.setAiDriving(want);
        car.setAiSkill(want ? Math.max(car.aiSkill(), 0.7) : car.aiSkill());
        ModNetworking.notify(player, want ? "AI driver: engaged." : "AI driver: off.");
        openGarage(player, car.getId());
    }

    private static void placeTrack(ServerPlayerEntity player, String trackId, int rotation, int mode) {
        TrackManager manager = TrackManager.get(player.server);
        Track track = manager.track(trackId);
        if (track == null) {
            ModNetworking.notify(player, "Unknown track: " + trackId);
            return;
        }
        BlockPos anchor = manager.wandAnchor(player);
        if (anchor == null) {
            anchor = player.getBlockPos();
        }
        if (mode == 1) {
            anchor = anchor.offset(player.getHorizontalFacing());
        }
            manager.placeTrack(player, track, anchor, rotation);
    }

    private static void removeTrack(ServerPlayerEntity player, int x, int y, int z, String text) {
        TrackManager manager = TrackManager.get(player.server);
        BlockPos pos = new BlockPos(x, y, z);
        TrackPlacement placement = manager.placementAt(pos);
        if (placement == null) {
            placement = manager.placementNear(player.getPos(), 200.0);
        }
        if (placement == null) {
            ModNetworking.notify(player, "No placed track nearby.");
            return;
        }
        for (RaceSession session : RaceManager.get(player.server).sessions()) {
            if (session.placement() == placement) {
                ModNetworking.notify(player, "A race is running on that track.");
                return;
            }
        }
        manager.removePlacement(player, placement, true);
    }

    private static void hostRace(ServerPlayerEntity player, ModNetworking.ClientAction action) {
        RaceManager races = RaceManager.get(player.server);
        TrackManager tracks = TrackManager.get(player.server);
        if (races.sessionFor(player) != null) {
            ModNetworking.notify(player, "You are already in a race. /race leave first.");
            return;
        }
        if (!(player.getVehicle() instanceof CarEntity car)) {
            ModNetworking.notify(player, "Get in a car first, then start the race.");
            return;
        }
        TrackPlacement placement = tracks.placementNear(car.getPos(), 140.0);
        if (placement == null) {
            ModNetworking.notify(player, "Drive closer to a circuit (or place one with the wand).");
            return;
        }
        RaceFormat format = RaceFormat.of(action.a(), action.b() & 0xFF,
                Math.max(0.2, (action.b() >> 8) / 100.0), true);
        format.name = "Grand Prix";
        races.start(player, placement, format, format.aiOpponents, format.aiSkill);
    }

    private static void joinRace(ServerPlayerEntity player) {
        RaceManager races = RaceManager.get(player.server);
        if (races.sessionFor(player) != null) {
            return;
        }
        if (!(player.getVehicle() instanceof CarEntity car)) {
            ModNetworking.notify(player, "Get in a car first.");
            return;
        }
        RaceSession best = null;
        double bestDistance = 120.0;
        for (RaceSession session : races.sessions()) {
            if (!session.isLive() || session.state != RaceSession.State.COUNTDOWN) {
                continue;
            }
            double distance = session.placement().distanceTo(car.getPos());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = session;
            }
        }
        if (best == null) {
            ModNetworking.notify(player, "No race is on the grid near you.");
            return;
        }
        RaceEntry entry = new RaceEntry(car, player.getUuid(), player.getName().getString(), false, 0.0);
        int slot = best.entries().size();
        Vec3d pos = best.placement().gridPos(slot);
        car.teleportCleanly(pos.x, pos.y, pos.z, best.placement().startYaw());
        car.setControlLocked(true);
        car.setRaceState(1);
        best.addEntry(entry);
        best.announce(player.getName().getString() + " joined the grid");
        StatsManager.get(player.server).get(player).racesStarted++;
        StatsManager.get(player.server).markDirty();
    }

    private static void leaveRace(ServerPlayerEntity player) {
        RaceManager races = RaceManager.get(player.server);
        RaceSession session = races.sessionFor(player);
        if (session == null) {
            ModNetworking.notify(player, "You are not in a race.");
            return;
        }
        RaceEntry entry = session.entryFor(player);
        if (entry != null && !entry.finished) {
            entry.finished = true;
            entry.finishMs = System.currentTimeMillis() - session.raceStartMs;
            StatsManager.get(player.server).recordWreck(player);
        }
        player.stopRiding();
        if (player.getVehicle() instanceof CarEntity car) {
            car.setAiDriving(true);
        }
        ModNetworking.notify(player, "You left the race.");
    }

    private static void toggleRecording(ServerPlayerEntity player) {
        TrackManager manager = TrackManager.get(player.server);
        TrackRecorder recorder = manager.recorder(player);
        if (recorder.samples() > 0 && !recorder.isFinished()) {
            recorder.stop();
            return;
        }
        if (recorder.samples() > 0) {
            manager.stopRecording(player);
            manager.recorder(player);
        }
        ModNetworking.sendRecordState(player, 1, "0 samples");
        ModNetworking.notify(player, "Recording. Drive a lap, then use the wand again.");
    }

    private static void spectate(ServerPlayerEntity player) {
        RaceManager races = RaceManager.get(player.server);
        RaceSession best = null;
        double bestDistance = 200.0;
        for (RaceSession session : races.sessions()) {
            if (!session.isLive()) {
                continue;
            }
            double distance = session.placement().distanceTo(player.getPos());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = session;
            }
        }
        if (best == null) {
            ModNetworking.notify(player, "No live race nearby.");
            return;
        }
        best.addSpectator(player);
        ModNetworking.notify(player, "Spectating " + best.placement().track.name);
    }

    private static void repair(ServerPlayerEntity player) {
        if (player.getVehicle() instanceof CarEntity car) {
            car.resetHealth();
            player.sendMessage(Text.translatable("message.fabricracers.repaired")
                    .formatted(Formatting.GREEN), true);
        }
    }

    private static int countParts(ServerPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeParts(ServerPlayerEntity player, net.minecraft.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            var stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                int take = Math.min(remaining, stack.getCount());
                stack.decrement(take);
                remaining -= take;
            }
        }
    }

    /** Used by commands: spawn a car for a player next to themselves. */
    public static CarEntity spawnCarFor(ServerPlayerEntity player, CarData data) {
        var world = player.getServerWorld();
        Vec3d look = Vec3d.fromPolar(0.0F, player.getYaw()).multiply(3.0);
        Vec3d spawn = player.getPos().add(look);
        double ground = Cars.groundHeight(world, spawn);
        if (Double.isNaN(ground)) {
            return null;
        }
        dev.qynl.racers.car.PlayerCarEntity car = new dev.qynl.racers.car.PlayerCarEntity(
                dev.qynl.racers.registry.ModEntities.PLAYER_CAR, world);
        car.initFromData(data);
        car.refreshPositionAndAngles(spawn.x, ground, spawn.z, player.getYaw(), 0.0F);
        if (!world.spawnEntity(car)) {
            return null;
        }
        return car;
    }
}
