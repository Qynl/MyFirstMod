package dev.qynl.myfirstmod.client.render;

import dev.qynl.myfirstmod.boss.NullWardenEntity;
import dev.qynl.myfirstmod.client.model.NullWardenModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class NullWardenRenderer extends MobEntityRenderer<NullWardenEntity, NullWardenModel> {
    public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(
            Identifier.of("myfirstmod", "null_warden"), "main");
    public static Identifier TEXTURE;

    public NullWardenRenderer(EntityRendererFactory.Context context) {
        super(context, new NullWardenModel(context.getPart(MODEL_LAYER)), 1.15f);
    }

    @Override
    public Identifier getTexture(NullWardenEntity entity) {
        return TEXTURE;
    }
}
