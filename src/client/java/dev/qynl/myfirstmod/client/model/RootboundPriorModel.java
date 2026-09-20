package dev.qynl.myfirstmod.client.model;
import dev.qynl.myfirstmod.kingdom.RootboundPriorEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
/** A bark-robed abbot with a split wooden mask, antler branches and root feet. */
public final class RootboundPriorModel extends SinglePartEntityModel<RootboundPriorEntity> {
    private final ModelPart root,left,right,mask;
    public RootboundPriorModel(ModelPart root){this.root=root;left=root.getChild("left");right=root.getChild("right");mask=root.getChild("mask");}
    public static TexturedModelData data(){
        var data=new ModelData();var r=data.getRoot();
        r.addChild("body",ModelPartBuilder.create().uv(0,28).cuboid(-7,-20,-5,14,24,10).uv(52,28).cuboid(-9,-16,3,18,29,3),ModelTransform.pivot(0,4,0));
        r.addChild("mask",ModelPartBuilder.create().uv(0,0).cuboid(-5,-10,-5,10,10,10).uv(42,0).cuboid(-1,-9,-6,2,10,2)
            .uv(56,0).cuboid(-9,-15,-2,3,13,3).cuboid(6,-15,-2,3,13,3)
            .uv(72,0).cuboid(-13,-16,-1,6,3,2).cuboid(7,-16,-1,6,3,2),ModelTransform.pivot(0,-16,0));
        r.addChild("left",ModelPartBuilder.create().uv(0,68).cuboid(-2,-2,-3,5,23,6).uv(24,68).cuboid(-3,17,-4,7,5,8),ModelTransform.pivot(9,-12,0));
        r.addChild("right",ModelPartBuilder.create().uv(0,68).cuboid(-3,-2,-3,5,23,6).uv(24,68).cuboid(-4,17,-4,7,5,8),ModelTransform.pivot(-9,-12,0));
        r.addChild("root_left",ModelPartBuilder.create().uv(48,68).cuboid(-3,0,-3,6,16,6).uv(76,68).cuboid(-4,13,-8,8,3,12),ModelTransform.pivot(4,8,0));
        r.addChild("root_right",ModelPartBuilder.create().uv(48,68).cuboid(-3,0,-3,6,16,6).uv(76,68).cuboid(-4,13,-8,8,3,12),ModelTransform.pivot(-4,8,0));
        return TexturedModelData.of(data,128,128);
    }
    @Override public ModelPart getPart(){return root;}
    @Override public void setAngles(RootboundPriorEntity e,float limb,float amount,float time,float yaw,float pitch){
        root.traverse().forEach(ModelPart::resetTransform);mask.yaw=yaw*.008f;
        float t=e.charge()/60f;left.roll=-.12f;right.roll=.12f;
        if(e.attack()==1)right.pitch=-1.8f+t*.4f;
        if(e.attack()==2){left.roll=-1.2f;right.roll=1.2f;mask.pitch=-.2f;}
        if(e.attack()==3){left.pitch=-2+t*.3f;right.pitch=-2+t*.3f;}
        left.roll+=MathHelper.sin(time*.04f)*.04f;right.roll-=MathHelper.sin(time*.04f)*.04f;
    }
}
