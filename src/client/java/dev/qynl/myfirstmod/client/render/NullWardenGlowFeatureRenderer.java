package dev.qynl.myfirstmod.client.render;

import dev.qynl.myfirstmod.boss.NullWardenEntity;
import dev.qynl.myfirstmod.client.NullWardenTexture;
import dev.qynl.myfirstmod.client.model.NullWardenModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.util.math.MatrixStack;

public class NullWardenGlowFeatureRenderer extends FeatureRenderer<NullWardenEntity, NullWardenModel> {
    public NullWardenGlowFeatureRenderer(FeatureRendererContext<NullWardenEntity, NullWardenModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       NullWardenEntity entity, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (NullWardenTexture.GLOW_TEXTURE == null) {
            return;
        }

        float pulse = 0.78f + 0.22f * (float) Math.sin((entity.age + animationProgress) * 0.16f);
        int phase = entity.getVisualPhase();
        int rgb = switch (phase) {
            case 2 -> 0xE66CFF;
            case 3 -> 0xB94CFF;
            case 4 -> 0xFFFFFF;
            default -> 0x62F5FF;
        };
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 255) * pulse)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 255) * pulse)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * pulse)));
        int color = (r << 16) | (g << 8) | b;

        getContextModel().getPart().render(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissiveNoOutline(NullWardenTexture.GLOW_TEXTURE)),
                0xF000F0,
                OverlayTexture.DEFAULT_UV,
                0xFF000000 | color
        );
    }
}
