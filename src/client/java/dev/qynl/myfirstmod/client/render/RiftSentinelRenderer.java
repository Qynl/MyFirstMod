package dev.qynl.myfirstmod.client.render;
import dev.qynl.myfirstmod.mob.RiftSentinelEntity;
import dev.qynl.myfirstmod.client.model.RiftSentinelModel;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
public final class RiftSentinelRenderer extends MobEntityRenderer<RiftSentinelEntity,RiftSentinelModel> {
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","rift_sentinel"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/rift_sentinel_pilgrimage.png");
    public RiftSentinelRenderer(EntityRendererFactory.Context context) {super(context,new RiftSentinelModel(context.getPart(LAYER)),.6f);}
    @Override public Identifier getTexture(RiftSentinelEntity entity) {return TEXTURE;}
}
