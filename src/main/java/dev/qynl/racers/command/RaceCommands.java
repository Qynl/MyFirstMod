package dev.qynl.racers.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.CarType;
import dev.qynl.racers.car.PaintColors;
import dev.qynl.racers.race.RaceEntry;
import dev.qynl.racers.race.RaceFormat;
import dev.qynl.racers.race.RaceManager;
import dev.qynl.racers.race.RaceSession;
import dev.qynl.racers.stats.StatsManager;
import dev.qynl.racers.track.Track;
import dev.qynl.racers.track.TrackManager;
import dev.qynl.racers.track.TrackPlacement;
import dev.qynl.racers.track.TrackRecorder;
import dev.qynl.racers.util.Cars;
import dev.qynl.racers.util.Stopwatch;

/** {@code /race ...} - everything the mod can do, scriptable. */
public final class RaceCommands {
    private RaceCommands() {
    }

    private static final SuggestionProvider<ServerCommandSource> TRACKS = (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        if (server == null) {
            // tab completion can run without a server behind the source
            return builder.buildFuture();
        }
        return CommandSource.suggestMatching(
                TrackManager.get(server).all().stream().map(track -> track.id).toList(), builder);
    };

    private static final SuggestionProvider<ServerCommandSource> CAR_TYPES = (context, builder) ->
            CommandSource.suggestMatching(
                    java.util.Arrays.stream(CarType.values()).map(type -> type.id).toList(), builder);

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("race")
                        .executes(context -> help(context.getSource()))
                        .then(CommandManager.literal("help").executes(context -> help(context.getSource())))
                        .then(CommandManager.literal("list").executes(context -> list(context.getSource())))
                        // --- driving helpers ---------------------------------
                        .then(CommandManager.literal("car")
                                .executes(context -> giveCar(context.getSource(), "sport", null))
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .suggests(CAR_TYPES)
                                        .executes(context -> giveCar(context.getSource(),
                                                StringArgumentType.getString(context, "type"), null))
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(context -> giveCar(context.getSource(),
                                                        StringArgumentType.getString(context, "type"),
                                                        EntityArgumentType.getPlayer(context, "player"))))))
                        .then(CommandManager.literal("park")
                                .executes(context -> park(context.getSource())))
                        .then(CommandManager.literal("repair")
                                .executes(context -> repair(context.getSource())))
                        .then(CommandManager.literal("ai")
                                .executes(context -> toggleAi(context.getSource(), -1))
                                .then(CommandManager.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(context -> toggleAi(context.getSource(),
                                                com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "on") ? 1 : 0)))
                                .then(CommandManager.argument("skill", DoubleArgumentType.doubleArg(0.2, 1.0))
                                        .executes(context -> setAiSkill(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "skill")))))
                        // --- tracks -------------------------------------------
                        .then(CommandManager.literal("tracks").executes(context -> listTracks(context.getSource())))
                        .then(CommandManager.literal("wand")
                                .requires(RaceCommands::canBuild)
                                .executes(context -> giveWand(context.getSource())))
                        .then(CommandManager.literal("place")
                                .requires(RaceCommands::canBuild)
                                .then(CommandManager.argument("track", StringArgumentType.word())
                                        .suggests(TRACKS)
                                        .executes(context -> place(context.getSource(),
                                                StringArgumentType.getString(context, "track"), 0))
                                        .then(CommandManager.argument("rotation", IntegerArgumentType.integer(0, 359))
                                                .executes(context -> place(context.getSource(),
                                                        StringArgumentType.getString(context, "track"),
                                                        IntegerArgumentType.getInteger(context, "rotation"))))))
                        .then(CommandManager.literal("remove")
                                .requires(RaceCommands::canBuild)
                                .executes(context -> remove(context.getSource())))
                        .then(CommandManager.literal("record")
                                .executes(context -> record(context.getSource(), 7, true))
                                .then(CommandManager.argument("width", IntegerArgumentType.integer(3, 15))
                                        .executes(context -> record(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "width"), true))
                                        .then(CommandManager.argument("barriers",
                                                        com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(context -> record(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "width"),
                                                        com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "barriers"))))))
                        // --- racing ------------------------------------------
                        .then(CommandManager.literal("start")
                                .executes(context -> start(context.getSource(), 0, 3, 0.72))
                                .then(CommandManager.argument("ai", IntegerArgumentType.integer(0, 15))
                                        .executes(context -> start(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ai"), 3, 0.72))
                                        .then(CommandManager.argument("laps", IntegerArgumentType.integer(1, 20))
                                                .executes(context -> start(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "ai"),
                                                        IntegerArgumentType.getInteger(context, "laps"), 0.72))
                                                .then(CommandManager.argument("skill", DoubleArgumentType.doubleArg(0.2, 1.0))
                                                        .executes(context -> start(context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "ai"),
                                                                IntegerArgumentType.getInteger(context, "laps"),
                                                                DoubleArgumentType.getDouble(context, "skill")))))))
                        .then(CommandManager.literal("join").executes(context -> join(context.getSource())))
                        .then(CommandManager.literal("leave").executes(context -> leave(context.getSource())))
                        .then(CommandManager.literal("stop").executes(context -> stop(context.getSource())))
                        .then(CommandManager.literal("info").executes(context -> info(context.getSource())))
                        .then(CommandManager.literal("standings").executes(context -> standings(context.getSource())))
                        .then(CommandManager.literal("stats")
                                .executes(context -> stats(context.getSource(), context.getSource().getPlayer()))
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(context -> stats(context.getSource(),
                                                EntityArgumentType.getPlayer(context, "player")))))
                        .then(CommandManager.literal("top")
                                .executes(context -> top(context.getSource(), null))
                                .then(CommandManager.argument("track", StringArgumentType.word())
                                        .suggests(TRACKS)
                                        .executes(context -> top(context.getSource(),
                                                StringArgumentType.getString(context, "track")))))
                        .then(CommandManager.literal("paint")
                                .then(CommandManager.argument("color", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                                PaintColors.named().keySet(), builder))
                                        .executes(context -> paint(context.getSource(),
                                                StringArgumentType.getString(context, "color")))))
                        .then(CommandManager.literal("drift")
                                .executes(context -> drift(context.getSource())))));
    }

    // ------------------------------------------------------------------
    private static int help(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("FabricRacers - /race commands").formatted(Formatting.GOLD),
                false);
        String[] lines = {
                "/race car [type] - get a car (kart, rally, sport, hyper, f1, truck, drift, hotrod, shadow)",
                "/race park - put the car you are in back into your inventory",
                "/race repair - fix your car",
                "/race ai [on|off|skill 0.2-1.0] - hand your car to the AI",
                "/race tracks | /race wand - list circuits / get the track wand",
                "/race place <track> [rotation] - build a circuit where you stand",
                "/race remove - remove the nearest placed circuit",
                "/race record [width] [barriers] - record a lap and turn it into a circuit",
                "/race start [ai] [laps] [skill] - start a race where you are",
                "/race join | leave | stop | info | standings",
                "/race stats [player] | /race top [track] - records and leaderboards",
                "/race paint <color> | /race drift - cosmetics and the drift challenge",
        };
        for (String line : lines) {
            source.sendFeedback(() -> Text.literal(line).formatted(Formatting.GRAY), false);
        }
        return 1;
    }

    private static int list(ServerCommandSource source) {
        var manager = TrackManager.get(source.getServer());
        source.sendFeedback(() -> Text.literal("Circuits (" + manager.all().size() + "):")
                .formatted(Formatting.GOLD), false);
        for (Track track : manager.all()) {
            source.sendFeedback(() -> Text.literal(" - " + track.id + ": " + track.describe())
                    .formatted(Formatting.GRAY), false);
        }
        source.sendFeedback(() -> Text.literal("Placed: " + manager.placements().size())
                .formatted(Formatting.YELLOW), false);
        for (TrackPlacement placement : manager.placements()) {
            source.sendFeedback(() -> Text.literal(" - " + placement.label()).formatted(Formatting.GRAY),
                    false);
        }
        return 1;
    }

    private static int listTracks(ServerCommandSource source) {
        return list(source);
    }

    private static int giveCar(ServerCommandSource source, String type, ServerPlayerEntity target) {
        ServerPlayerEntity player = target != null ? target : source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Player only."));
            return 0;
        }
        CarType carType = CarType.byId(type);
        var stack = dev.qynl.racers.registry.ModItems.carStack(carType, player);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
        source.sendFeedback(() -> Text.translatable("message.fabricracers.car_given",
                carType.displayName).formatted(Formatting.GREEN), false);
        return 1;
    }

    /** Building circuits edits the world, so it needs creative mode or operator rights. */
    private static boolean canBuild(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        return source.hasPermissionLevel(2) || (player != null && player.isCreative());
    }

    private static int giveWand(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Player only."));
            return 0;
        }
        var stack = new net.minecraft.item.ItemStack(dev.qynl.racers.registry.ModItems.TRACK_WAND);
        player.getInventory().insertStack(stack);
        return 1;
    }

    private static int park(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        player.stopRiding();
        ItemStack parked = parkStack(car);
        if (!player.getInventory().insertStack(parked)) {
            player.dropItem(parked, false);
        }
        car.discard();
        return 1;
    }

    private static ItemStack parkStack(CarEntity car) {
        ItemStack stack = new ItemStack(dev.qynl.racers.registry.ModItems.carItem(car.carType()));
        CarData.apply(stack, car.data().withHealth(car.engineHealth(), car.tyreHealth()));
        return stack;
    }

    private static int repair(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        car.resetHealth();
        source.sendFeedback(() -> Text.translatable("message.fabricracers.repaired")
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int toggleAi(ServerCommandSource source, int value) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        boolean on = value == -1 ? !car.isAiDriving() : value == 1;
        car.setAiDriving(on);
        source.sendFeedback(() -> Text.literal(on ? "AI driver engaged." : "AI driver off.")
                .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int setAiSkill(ServerCommandSource source, double skill) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        car.setAiSkill(skill);
        car.setAiDriving(true);
        source.sendFeedback(() -> Text.literal(String.format("AI skill set to %.2f", skill))
                .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int place(ServerCommandSource source, String trackId, int rotation) {
        ServerPlayerEntity player = source.getPlayer();
        TrackManager manager = TrackManager.get(source.getServer());
        Track track = manager.track(trackId);
        if (track == null) {
            source.sendError(Text.literal("Unknown track: " + trackId));
            return 0;
        }
        if (player == null) {
            source.sendError(Text.literal("Player only (the track is built around you)."));
            return 0;
        }
        BlockPos anchor = player.getBlockPos().offset(player.getHorizontalFacing());
        manager.placeTrack(player, track, anchor, rotation);
        return 1;
    }

    private static int remove(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        TrackManager manager = TrackManager.get(source.getServer());
        if (player == null) {
            source.sendError(Text.literal("Player only."));
            return 0;
        }
        TrackPlacement placement = manager.placementNear(player.getPos(), 300.0);
        if (placement == null) {
            source.sendError(Text.literal("No placed circuit found."));
            return 0;
        }
        manager.removePlacement(player, placement, true);
        return 1;
    }

    private static int record(ServerCommandSource source, int width, boolean barriers) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Player only."));
            return 0;
        }
        TrackManager manager = TrackManager.get(source.getServer());
        TrackRecorder recorder = manager.recorder(player);
        if (recorder.isFinished()) {
            return 0;
        }
        if (recorder.samples() < 2) {
            recorder.setWidth(width);
            recorder.setBarriers(barriers);
            source.sendFeedback(() -> Text.literal("Recording - drive a lap, then run /race record again.")
                    .formatted(Formatting.GREEN), false);
            return 1;
        }
        recorder.stop();
        return 1;
    }

    private static int start(ServerCommandSource source, int ai, int laps, double skill) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        TrackManager manager = TrackManager.get(source.getServer());
        TrackPlacement placement = manager.placementNear(car.getPos(), 160.0);
        if (placement == null) {
            source.sendError(Text.literal("No circuit nearby. Use /race place <track> first."));
            return 0;
        }
        RaceFormat format = RaceFormat.of(laps, ai, skill, true);
        RaceSession session = RaceManager.get(source.getServer())
                .start(player, placement, format, ai, skill);
        if (session == null) {
            return 0;
        }
        source.sendFeedback(() -> Text.literal(session.describe()).formatted(Formatting.GOLD), false);
        return 1;
    }

    private static int join(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        RaceManager races = RaceManager.get(source.getServer());
        RaceSession session = null;
        double best = 160.0;
        for (RaceSession candidate : races.sessions()) {
            if (!candidate.isLive() || candidate.state != RaceSession.State.COUNTDOWN) {
                continue;
            }
            double distance = candidate.placement().distanceTo(player.getPos());
            if (distance < best) {
                best = distance;
                session = candidate;
            }
        }
        if (session == null) {
            source.sendError(Text.literal("No race is waiting on a grid nearby."));
            return 0;
        }
        if (!(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        int slot = session.entries().size();
        Vec3d pos = session.placement().gridPos(slot);
        car.teleportCleanly(pos.x, pos.y, pos.z, session.placement().startYaw());
        car.setControlLocked(true);
        car.setRaceState(1);
        RaceEntry entry = new RaceEntry(car, player.getUuid(), player.getName().getString(), false, 0.0);
        session.addEntry(entry);
        session.announce(player.getName().getString() + " joined the grid");
        return 1;
    }

    private static int leave(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        RaceSession session = RaceManager.get(source.getServer()).sessionFor(player);
        if (session == null) {
            source.sendError(Text.literal("You are not racing."));
            return 0;
        }
        RaceEntry entry = session.entryFor(player);
        if (entry != null) {
            entry.finished = true;
            entry.finishMs = System.currentTimeMillis() - session.raceStartMs;
        }
        player.stopRiding();
        StatsManager.get(source.getServer()).recordWreck(player);
        return 1;
    }

    private static int stop(ServerCommandSource source) {
        RaceManager races = RaceManager.get(source.getServer());
        int stopped = 0;
        for (RaceSession session : new java.util.ArrayList<>(races.sessions())) {
            races.stop(session, "Race stopped by an operator.");
            stopped++;
        }
        int count = stopped;
        source.sendFeedback(() -> Text.literal("Stopped " + count + " race(s).").formatted(Formatting.YELLOW),
                false);
        return 1;
    }

    private static int info(ServerCommandSource source) {
        RaceManager races = RaceManager.get(source.getServer());
        if (races.sessions().isEmpty()) {
            source.sendFeedback(() -> Text.literal("No live races.").formatted(Formatting.GRAY), false);
            return 1;
        }
        for (RaceSession session : races.sessions()) {
            source.sendFeedback(() -> Text.literal(session.describe()).formatted(Formatting.GOLD), false);
        }
        return 1;
    }

    private static int standings(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        RaceSession session = player == null ? null
                : RaceManager.get(source.getServer()).sessionFor(player);
        if (session == null) {
            source.sendFeedback(() -> Text.literal("No race.").formatted(Formatting.GRAY), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal(session.standingsText()).formatted(Formatting.WHITE), false);
        return 1;
    }

    private static int stats(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Text.literal("Player only."));
            return 0;
        }
        StatsManager.get(source.getServer()).sendRecord(target);
        return 1;
    }

    private static int top(ServerCommandSource source, String trackId) {
        StatsManager stats = StatsManager.get(source.getServer());
        if (trackId == null) {
            TrackPlacement placement = null;
            if (source.getPlayer() != null) {
                placement = TrackManager.get(source.getServer())
                        .placementNear(source.getPlayer().getPos(), 200.0);
            }
            trackId = placement != null ? placement.track.id
                    : (TrackManager.get(source.getServer()).all().isEmpty() ? null
                    : TrackManager.get(source.getServer()).all().get(0).id);
        }
        if (trackId == null) {
            source.sendError(Text.literal("No tracks yet."));
            return 0;
        }
        String id = trackId;
        source.sendFeedback(() -> Text.literal("Leaderboard - " + id).formatted(Formatting.GOLD), false);
        int position = 1;
        for (var entry : stats.leaderboard(id)) {
            int place = position++;
            source.sendFeedback(() -> Text.literal(place + ". " + entry.getKey() + "  "
                    + Stopwatch.format(entry.getValue())).formatted(Formatting.WHITE), false);
        }
        return 1;
    }

    private static int paint(ServerCommandSource source, String colorName) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        Integer color = PaintColors.byName(colorName);
        if (color == null) {
            source.sendError(Text.literal("Unknown colour."));
            return 0;
        }
        car.setPaint(color, car.data().secondaryColor);
        source.sendFeedback(() -> Text.literal("Painted " + colorName + ".").formatted(Formatting.GREEN),
                false);
        return 1;
    }

    private static int drift(ServerCommandSource source) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            source.sendError(Text.translatable("race.fabricracers.need_car"));
            return 0;
        }
        source.sendFeedback(() -> Text.literal(String.format("Drift score: %.0f (boost %.0f%%)",
                car.getDriftScore(), car.getBoostCharge() * 100.0)).formatted(Formatting.LIGHT_PURPLE),
                false);
        return 1;
    }
}
