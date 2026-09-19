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

        getContextModel().getPart().render(
                matrices,
                vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissiveNoOutline(NullWardenTexture.GLOW_TEXTURE)),
                0xF000F0,
                OverlayTexture.DEFAULT_UV,
                0xFFFFFFFF
        );
    }
}
