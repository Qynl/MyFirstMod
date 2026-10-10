package dev.qynl.ollamaplayer;

import dev.qynl.ollamaplayer.command.NovaCommands;
import dev.qynl.ollamaplayer.companion.CompanionManager;
import dev.qynl.ollamaplayer.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OllamaPlayerMod implements ModInitializer {
    public static final String MOD_ID = "ollamaplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.load();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> CompanionManager.INSTANCE.onServerStarted(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> CompanionManager.INSTANCE.onServerStopping());
        ServerTickEvents.END_SERVER_TICK.register(server -> CompanionManager.INSTANCE.tick());

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) ->
                CompanionManager.INSTANCE.onPlayerChat(sender, message.getContent().getString()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                NovaCommands.register(dispatcher));

        LOGGER.info("Ollama Player loaded. Use /nova spawn in game.");
    }
}
