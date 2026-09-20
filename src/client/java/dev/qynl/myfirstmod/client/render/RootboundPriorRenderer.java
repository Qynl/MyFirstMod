package dev.qynl.myfirstmod.client.render;
import dev.qynl.myfirstmod.kingdom.RootboundPriorEntity;
import dev.qynl.myfirstmod.client.model.RootboundPriorModel;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
public final class RootboundPriorRenderer extends MobEntityRenderer<RootboundPriorEntity,RootboundPriorModel>{
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","rootbound_prior"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/rootbound_prior.png");
    public RootboundPriorRenderer(EntityRendererFactory.Context context){super(context,new RootboundPriorModel(context.getPart(LAYER)),1);}
    @Override public Identifier getTexture(RootboundPriorEntity entity){return TEXTURE;}
}
