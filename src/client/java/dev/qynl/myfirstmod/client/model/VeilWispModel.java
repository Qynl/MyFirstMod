package dev.qynl.myfirstmod.client.model;
import dev.qynl.myfirstmod.mob.VeilWispEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
/** A luminous core with four drifting veil wings. */
public final class VeilWispModel extends SinglePartEntityModel<VeilWispEntity> {
    private final ModelPart root,left,right,fore,aft;
    public VeilWispModel(ModelPart root){this.root=root;left=root.getChild("left");right=root.getChild("right");fore=root.getChild("fore");aft=root.getChild("aft");}
    public static TexturedModelData data(){
        var d=new ModelData();var r=d.getRoot();
        r.addChild("core",ModelPartBuilder.create().uv(0,0).cuboid(-2,-2,-2,4,4,4).uv(0,8).cuboid(-1,-3,-1,2,1,2),ModelTransform.NONE);
        r.addChild("left",ModelPartBuilder.create().uv(16,0).cuboid(-7,-2,0,7,4,1),ModelTransform.pivot(-2,0,0));
        r.addChild("right",ModelPartBuilder.create().uv(16,0).cuboid(0,-2,0,7,4,1),ModelTransform.pivot(2,0,0));
        r.addChild("fore",ModelPartBuilder.create().uv(16,5).cuboid(0,-2,-7,1,4,7),ModelTransform.pivot(0,0,-2));
        r.addChild("aft",ModelPartBuilder.create().uv(16,5).cuboid(-1,-2,0,1,4,7),ModelTransform.pivot(0,0,2));
        return TexturedModelData.of(d,32,32);
    }
    @Override public ModelPart getPart(){return root;}
    @Override public void setAngles(VeilWispEntity e,float limb,float amount,float time,float yaw,float pitch){
        root.traverse().forEach(ModelPart::resetTransform);
        float flap=MathHelper.sin(time*.18f)*.5f;
        left.roll=flap;right.roll=-flap;fore.pitch=flap*.7f;aft.pitch=-flap*.7f;
        root.pivotY=MathHelper.sin(time*.06f)*1.2f;
    }
}
