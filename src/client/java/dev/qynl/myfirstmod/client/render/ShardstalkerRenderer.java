package dev.qynl.myfirstmod.client.render;

import dev.qynl.myfirstmod.mob.ShardstalkerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SpiderEntityRenderer;
import net.minecraft.util.Identifier;

public final class ShardstalkerRenderer extends SpiderEntityRenderer<ShardstalkerEntity> {
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/shardstalker.png");
    public ShardstalkerRenderer(EntityRendererFactory.Context context) {super(context);}
    @Override public Identifier getTexture(ShardstalkerEntity entity) {return TEXTURE;}
}
