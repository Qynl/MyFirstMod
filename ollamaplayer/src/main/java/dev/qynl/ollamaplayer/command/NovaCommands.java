package dev.qynl.ollamaplayer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.qynl.ollamaplayer.companion.CompanionManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class NovaCommands {
    private NovaCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("nova")
                .then(literal("spawn").executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) return 0;
                    CompanionManager.INSTANCE.spawn(p);
                    return 1;
                }))
                .then(literal("despawn").executes(ctx -> {
                    CompanionManager.INSTANCE.despawn();
                    return 1;
                }))
                .then(literal("status").executes(ctx -> {
                    feedback(ctx.getSource(), CompanionManager.INSTANCE.status());
                    return 1;
                }))
                .then(literal("follow").executes(ctx -> act(ctx.getSource(), "follow", null, 0)))
                .then(literal("stay").executes(ctx -> act(ctx.getSource(), "stay", null, 0)))
                .then(literal("eat").executes(ctx -> act(ctx.getSource(), "eat", null, 0)))
                .then(literal("mine")
                        .then(argument("what", StringArgumentType.word())
                                .executes(ctx -> act(ctx.getSource(), "mine", StringArgumentType.getString(ctx, "what"), 8))
                                .then(argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> act(ctx.getSource(), "mine",
                                                StringArgumentType.getString(ctx, "what"),
                                                IntegerArgumentType.getInteger(ctx, "count"))))))
                .then(literal("say")
                        .then(argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    CompanionManager.INSTANCE.onPlayerChat(p,
                                            "Nova, " + StringArgumentType.getString(ctx, "text"));
                                    return 1;
                                }))));
    }

    private static int act(ServerCommandSource source, String action, String target, int count) {
        if (CompanionManager.INSTANCE.companion() == null) {
            feedback(source, "Nova is not spawned. Use /nova spawn first.");
            return 0;
        }
        String note = CompanionManager.INSTANCE.companion().command(action, target, count);
        if (note != null) CompanionManager.INSTANCE.speak(note);
        return 1;
    }

    private static void feedback(ServerCommandSource source, String text) {
        source.sendFeedback(() -> Text.literal(text), false);
    }
}
