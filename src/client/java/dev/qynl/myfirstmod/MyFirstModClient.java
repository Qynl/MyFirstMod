package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.client.CreatorScreen;
import dev.qynl.myfirstmod.client.TacticalHudOverlay;
import dev.qynl.myfirstmod.gui.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class MyFirstModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CREATOR, CreatorScreen::new);
        TacticalHudOverlay.register();
    }
}
