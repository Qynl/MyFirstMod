package dev.qynl.myfirstmod.kinetics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * {@code /kinetics} — inspect and control the Null Kinetics movement suite.
 *
 * <ul>
 *   <li>{@code /kinetics info} — show your energy, Flow and window status</li>
 *   <li>{@code /kinetics reload} — reload the config (op)</li>
 *   <li>{@code /kinetics toggle <ability>} — enable/disable an ability globally (op)</li>
 *   <li>{@code /kinetics energy <amount>} — set your energy (op)</li>
 * </ul>
 */
public final class KineticsCommands {
    private static final List<String> ABILITIES = List.of(
            "all", "dash", "double_jump", "wall_run", "glide", "slide", "grapple", "flow");

    private KineticsCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                KineticsCommands.registerTree(dispatcher));
    }

    private static void registerTree(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("kinetics")
                .then(CommandManager.literal("info")
                        .executes(KineticsCommands::info))
                .then(CommandManager.literal("reload")
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(KineticsCommands::reload))
                .then(CommandManager.literal("energy")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.0, 1000.0))
                                .executes(KineticsCommands::energy)))
                .then(CommandManager.literal("toggle")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.argument("ability", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    CommandSource.suggestMatching(ABILITIES, builder);
                                    return builder.buildFuture();
                                })
                                .executes(KineticsCommands::toggle))));
    }

    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players have kinetics."));
            return 0;
        }
        KineticsConfig cfg = KineticsConfig.get();
        KineticsState state = KineticsManager.getState(player);
        long tick = player.getServer() != null ? player.getServer().getTicks() : 0L;

        source.sendFeedback(() -> Text.literal("")
                .append(Text.literal("Null Kinetics ").formatted(Formatting.AQUA))
                .append(Text.literal(cfg.enabled ? "online" : "disabled").formatted(
                        cfg.enabled ? Formatting.GREEN : Formatting.RED)), false);
        source.sendFeedback(() -> Text.literal("  Energy: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(String.format("%.0f / %.0f", state.energy, cfg.maxEnergy))
                        .formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> Text.literal("  Flow: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(state.flowStacks + " / " + cfg.flowMaxStacks)
                        .formatted(Formatting.LIGHT_PURPLE)), false);
        source.sendFeedback(() -> Text.literal("  Air jumps: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(state.airJumpsLeft + " / " + cfg.airJumps)
                        .formatted(Formatting.AQUA)), false);
        source.sendFeedback(() -> Text.literal("  Dash: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(state.dashCooldown > 0
                        ? "cooling (" + state.dashCooldown + "t)" : "ready")
                        .formatted(state.dashCooldown > 0 ? Formatting.YELLOW : Formatting.GREEN)), false);
        source.sendFeedback(() -> Text.literal("  Active: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(describeWindows(state, tick)).formatted(Formatting.AQUA)), false);
        return 1;
    }

    private static String describeWindows(KineticsState state, long tick) {
        StringBuilder sb = new StringBuilder();
        if (state.isWallRunning(tick)) sb.append("wall-run ");
        if (state.isGliding(tick)) sb.append("glide ");
        if (state.isSliding(tick)) sb.append("slide ");
        if (state.grappleEntityId >= 0) sb.append("grapple ");
        return sb.isEmpty() ? "nothing (style pending)" : sb.toString().trim();
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        KineticsConfig.load();
        context.getSource().sendFeedback(() -> Text.literal("Null Kinetics config reloaded.")
                .formatted(Formatting.AQUA), true);
        return 1;
    }

    private static int energy(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendError(Text.literal("Only players have kinetics."));
            return 0;
        }
        double amount = DoubleArgumentType.getDouble(context, "amount");
        KineticsManager.setEnergy(player, amount);
        context.getSource().sendFeedback(() -> Text.literal(
                        "Energy set to " + String.format("%.0f", amount) + ".")
                .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int toggle(CommandContext<ServerCommandSource> context) {
        String ability = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "ability");
        KineticsConfig cfg = KineticsConfig.get();
        switch (ability) {
            case "all" -> cfg.enabled = !cfg.enabled;
            case "dash" -> cfg.dashEnabled = !cfg.dashEnabled;
            case "double_jump" -> cfg.doubleJumpEnabled = !cfg.doubleJumpEnabled;
            case "wall_run" -> cfg.wallRunEnabled = !cfg.wallRunEnabled;
            case "glide" -> cfg.glideEnabled = !cfg.glideEnabled;
            case "slide" -> cfg.slideEnabled = !cfg.slideEnabled;
            case "grapple" -> cfg.grappleEnabled = !cfg.grappleEnabled;
            case "flow" -> cfg.flowEnabled = !cfg.flowEnabled;
            default -> {
                context.getSource().sendError(Text.literal("Unknown ability: " + ability));
                return 0;
            }
        }
        KineticsConfig.save();
        boolean enabled = isEnabled(cfg, ability);
        context.getSource().sendFeedback(() -> Text.literal("Null Kinetics: " + ability + " is now "
                        + (enabled ? "enabled" : "disabled") + ".")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED), true);
        return 1;
    }

    private static boolean isEnabled(KineticsConfig cfg, String ability) {
        return switch (ability) {
            case "all" -> cfg.enabled;
            case "dash" -> cfg.dashEnabled;
            case "double_jump" -> cfg.doubleJumpEnabled;
            case "wall_run" -> cfg.wallRunEnabled;
            case "glide" -> cfg.glideEnabled;
            case "slide" -> cfg.slideEnabled;
            case "grapple" -> cfg.grappleEnabled;
            case "flow" -> cfg.flowEnabled;
            default -> false;
        };
    }
}
