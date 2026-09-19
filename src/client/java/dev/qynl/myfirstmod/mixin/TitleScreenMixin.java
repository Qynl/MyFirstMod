package dev.qynl.myfirstmod.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Mutable
    @Shadow
    private SplashRenderer splash;

    @Shadow
    private boolean fading;

    static {
    }

    private void myFirstMod$clean() {
        splash = null;
    }
}
