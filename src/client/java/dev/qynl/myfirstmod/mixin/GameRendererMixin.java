package dev.qynl.myfirstmod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.qynl.myfirstmod.kinetics.client.KineticsClientEffects;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds the Null Kinetics dash/grapple FOV punch on top of the vanilla FOV.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double myfirstmod$kineticsFovKick(double original) {
        return original + KineticsClientEffects.fovKick();
    }
}
