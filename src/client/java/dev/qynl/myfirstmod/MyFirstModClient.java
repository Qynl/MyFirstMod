package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.client.CreatorScreen;
import dev.qynl.myfirstmod.client.TacticalHudOverlay;
import dev.qynl.myfirstmod.gui.ModScreenHandlers;
import dev.qynl.myfirstmod.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class MyFirstModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.CREATOR, CreatorScreen::new);
        TacticalHudOverlay.register();

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SyncEquippedUnitPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().currentScreen instanceof CreatorScreen creatorScreen) {
                    creatorScreen.applySyncedUnit(payload);
                }
            });
        });
    }
}
