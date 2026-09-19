package dev.qynl.myfirstmod.client.render;
import dev.qynl.myfirstmod.keep.GraveRegentEntity;
import dev.qynl.myfirstmod.client.model.GraveRegentModel;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
public final class GraveRegentRenderer extends MobEntityRenderer<GraveRegentEntity,GraveRegentModel>{
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","grave_regent"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/grave_regent.png");
    public GraveRegentRenderer(EntityRendererFactory.Context context){super(context,new GraveRegentModel(context.getPart(LAYER)),1);}
    @Override public Identifier getTexture(GraveRegentEntity entity){return TEXTURE;}
}
