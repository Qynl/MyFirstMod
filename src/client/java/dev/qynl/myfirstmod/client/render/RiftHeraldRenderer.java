package dev.qynl.myfirstmod.client.render;

import dev.qynl.myfirstmod.mob.RiftHeraldEntity;
import dev.qynl.myfirstmod.client.model.RiftHeraldModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class RiftHeraldRenderer extends MobEntityRenderer<RiftHeraldEntity,RiftHeraldModel> {
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","rift_herald"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/rift_herald.png");
    public RiftHeraldRenderer(EntityRendererFactory.Context context) {super(context,new RiftHeraldModel(context.getPart(LAYER)),.8f);}
    @Override public Identifier getTexture(RiftHeraldEntity entity) {return TEXTURE;}
    @Override protected int getBlockLight(RiftHeraldEntity entity,BlockPos pos) {return 15;}
}
