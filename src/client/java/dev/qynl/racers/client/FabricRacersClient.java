package dev.qynl.racers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import dev.qynl.racers.client.hud.RaceHud;
import dev.qynl.racers.client.render.CarRenderer;
import dev.qynl.racers.net.ModNetworking;
import dev.qynl.racers.registry.ModEntities;

/** Client bootstrap: renderers, key binds, HUD and the network receiver. */
public class FabricRacersClient implements ClientModInitializer {
    public static KeyBinding openGarage;
    public static KeyBinding openTracks;
    public static KeyBinding handbrake;
    public static KeyBinding boost;
    public static KeyBinding toggleHud;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.PLAYER_CAR, CarRenderer::new);
        EntityRendererRegistry.register(ModEntities.AI_CAR, CarRenderer::new);

        openGarage = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabricracers.garage", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G,
                "category.fabricracers.racing"));
        openTracks = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabricracers.tracks", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_T,
                "category.fabricracers.racing"));
        handbrake = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabricracers.handbrake", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE,
                "category.fabricracers.racing"));
        boost = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabricracers.boost", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL,
                "category.fabricracers.racing"));
        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabricracers.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H,
                "category.fabricracers.racing"));

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ServerAction.ID,
                (payload, context) -> context.client().execute(
                        () -> ClientRaceState.handle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.Hud.ID,
                (payload, context) -> context.client().execute(
                        () -> ClientRaceState.setHud(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(ClientRaceState::tick);
        HudRenderCallback.EVENT.register(RaceHud::render);
    }
}
