package dev.qynl.myfirstmod.client.render;
import dev.qynl.myfirstmod.mob.VeilWispEntity;
import dev.qynl.myfirstmod.client.model.VeilWispModel;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
public final class VeilWispRenderer extends MobEntityRenderer<VeilWispEntity,VeilWispModel>{
    public static final EntityModelLayer LAYER=new EntityModelLayer(Identifier.of("myfirstmod","veil_wisp"),"main");
    private static final Identifier TEXTURE=Identifier.of("myfirstmod","textures/entity/veil_wisp.png");
    public VeilWispRenderer(EntityRendererFactory.Context context){super(context,new VeilWispModel(context.getPart(LAYER)),.4f);}
    @Override public Identifier getTexture(VeilWispEntity entity){return TEXTURE;}
}
