package dev.qynl.myfirstmod.gui;

import dev.qynl.myfirstmod.MyFirstMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<CreatorScreenHandler> CREATOR = Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MyFirstMod.MOD_ID,"creator"), new ScreenHandlerType<>(CreatorScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static void register() {}
}
