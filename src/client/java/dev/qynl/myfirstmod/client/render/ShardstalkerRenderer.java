package dev.qynl.myfirstmod.client.render;
import dev.qynl.myfirstmod.mob.ShardstalkerEntity;
import dev.qynl.myfirstmod.client.model.ShardstalkerModel;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
public final class ShardstalkerRenderer extends MobEntityRenderer<ShardstalkerEntity,ShardstalkerModel> {
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","shardstalker"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/shardstalker_pilgrimage.png");
    public ShardstalkerRenderer(EntityRendererFactory.Context context) {super(context,new ShardstalkerModel(context.getPart(LAYER)),.6f);}
    @Override public Identifier getTexture(ShardstalkerEntity entity) {return TEXTURE;}
}
