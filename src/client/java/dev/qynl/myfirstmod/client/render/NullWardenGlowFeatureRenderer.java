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

        int phase = entity.getVisualPhase();
        int attack = entity.getVisualAttack();
        float attackProgress = entity.getVisualAttackProgress();

        // The core is always alive, but the emissive layer reacts to encounter
        // state instead of sitting at a constant brightness.
        float pulse = 0.82f + 0.18f *
                (float) Math.sin((entity.age + animationProgress) * (0.14f + phase * 0.012f));

        float attackSurge = attack == 0
                ? 0.0f
                : 0.10f + 0.18f * (1.0f - Math.abs(attackProgress - 0.5f) * 2.0f);

        float defeatFade = entity.isVisualDefeated() ? 0.58f : 1.0f;
        float intensity = Math.min(1.0f, Math.max(0.45f, (pulse + attackSurge) * defeatFade));

        int rgb = switch (phase) {
            case 2 -> 0xE66CFF;
            case 3 -> 0xB94CFF;
            case 4 -> 0xFFFFFF;
            default -> 0x62F5FF;
        };

        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 255) * intensity)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 255) * intensity)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * intensity)));

        // Keep the emissive pass visually integrated with the armor. The texture
        // controls exactly which UV islands emit; this tint only changes their
        // encounter color and brightness.
        int alpha = entity.isVisualDefeated() ? 190 : 255;
        int color = (alpha << 24) | (r << 16) | (g << 8) | b;

        getContextModel().getPart().render(
                matrices,
                vertexConsumers.getBuffer(
                        RenderLayer.getEntityTranslucentEmissiveNoOutline(
                                NullWardenTexture.GLOW_TEXTURE)),
                0xF000F0,
                OverlayTexture.DEFAULT_UV,
                color
        );
    }
}
