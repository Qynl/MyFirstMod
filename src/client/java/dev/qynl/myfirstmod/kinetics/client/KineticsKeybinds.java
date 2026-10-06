package dev.qynl.myfirstmod.kinetics.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybinds for the Null Kinetics suite.
 *
 * <ul>
 *   <li><b>V</b> — Void Dash</li>
 *   <li><b>C</b> — Null Drift (hold to glide)</li>
 *   <li><b>J</b> — toggle the Kinetics HUD</li>
 * </ul>
 *
 * <p>Air jumps use the jump key, wall runs trigger automatically while
 * sprinting into a wall, and sliding is a crouch tap while sprinting.
 */
public final class KineticsKeybinds {
    private KineticsKeybinds() {}

    public static KeyBinding dashKey;
    public static KeyBinding glideKey;
    public static KeyBinding hudToggleKey;

    public static void register() {
        dashKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myfirstmod.void_dash", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V,
                "category.myfirstmod.kinetics"));
        glideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myfirstmod.null_drift", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C,
                "category.myfirstmod.kinetics"));
        hudToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myfirstmod.hud_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J,
                "category.myfirstmod.kinetics"));
    }
}
