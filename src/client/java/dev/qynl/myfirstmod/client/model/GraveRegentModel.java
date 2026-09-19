package dev.qynl.myfirstmod.client.model;
import dev.qynl.myfirstmod.keep.GraveRegentEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
/** A crowned, suspended funeral monarch: mantle, split robes and an oversized ritual staff. */
public final class GraveRegentModel extends SinglePartEntityModel<GraveRegentEntity> {
    private final ModelPart root,head,left,right,mantle;
    public GraveRegentModel(ModelPart root){this.root=root;head=root.getChild("head");left=root.getChild("left");right=root.getChild("right");mantle=root.getChild("mantle");}
    public static TexturedModelData data(){
        var d=new ModelData();var r=d.getRoot();
        r.addChild("body",ModelPartBuilder.create().uv(0,24).cuboid(-6,-8,-4,12,22,8).uv(0,58).cuboid(-7,13,-4,6,10,8).cuboid(1,13,-4,6,10,8),ModelTransform.NONE);
        var h=r.addChild("head",ModelPartBuilder.create().uv(0,0).cuboid(-5,-11,-5,10,11,10).uv(48,0).cuboid(-7,-13,-7,14,3,14),ModelTransform.pivot(0,-8,0));
        for(int i=-1;i<=1;i++)h.addChild("crown"+i,ModelPartBuilder.create().uv(108,0).cuboid(-1,-8,-1,2,8,2),ModelTransform.pivot(i*5,-13,-6));
        r.addChild("left",ModelPartBuilder.create().uv(48,40).cuboid(-2,-2,-3,5,22,6),ModelTransform.pivot(8,-6,0));
        r.addChild("right",ModelPartBuilder.create().uv(48,40).cuboid(-3,-2,-3,5,22,6).uv(80,40).cuboid(-2,-15,-6,2,43,2).uv(96,40).cuboid(-5,-20,-8,8,8,6),ModelTransform.pivot(-8,-6,0));
        r.addChild("mantle",ModelPartBuilder.create().uv(48,88).cuboid(-12,-3,3,24,29,2),ModelTransform.pivot(0,-7,0));
        return TexturedModelData.of(d,128,128);
    }
    @Override public ModelPart getPart(){return root;}
    @Override public void setAngles(GraveRegentEntity e,float limb,float distance,float time,float yaw,float pitch){
        root.traverse().forEach(ModelPart::resetTransform);root.pivotY=MathHelper.sin(time*.04f)*.65f;
        head.yaw=yaw*.008f;mantle.pitch=.08f+MathHelper.sin(time*.045f)*.025f;
        float charge=e.charge()/40f;right.pitch=e.attack()==0?-.15f:-1.8f+charge*.6f;left.roll=-.2f;
        if(e.attack()==2){left.roll=-1.25f;right.roll=1.25f;}if(e.attack()==3){head.pitch=.25f;left.pitch=-1.2f;}
    }
}
