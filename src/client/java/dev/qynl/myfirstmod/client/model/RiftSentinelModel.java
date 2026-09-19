package dev.qynl.myfirstmod.client.model;
import dev.qynl.myfirstmod.mob.RiftSentinelEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Original horned greatblade knight silhouette, with committed attack/recovery poses. */
public final class RiftSentinelModel extends SinglePartEntityModel<RiftSentinelEntity> {
    private final ModelPart root,head,body,leftArm,rightArm,leftLeg,rightLeg;
    public RiftSentinelModel(ModelPart root) {
        this.root=root;head=root.getChild("head");body=root.getChild("body");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");
        leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");
    }
    public static TexturedModelData data() {
        var data=new ModelData();var r=data.getRoot();
        r.addChild("head",ModelPartBuilder.create().uv(0,0).cuboid(-4,-8,-4,8,8,8)
            .uv(34,0).cuboid(-6,-13,-1,2,8,2).cuboid(4,-13,-1,2,8,2),ModelTransform.pivot(0,0,0));
        r.addChild("body",ModelPartBuilder.create().uv(0,18).cuboid(-5,0,-3,10,12,6)
            .uv(40,20).cuboid(-4,8,-4,8,8,2),ModelTransform.NONE);
        r.addChild("left_arm",ModelPartBuilder.create().uv(64,0).cuboid(-1,-2,-3,6,6,6)
            .uv(64,14).cuboid(0,4,-2,4,9,4).uv(84,0).cuboid(3,0,-4,2,14,8),ModelTransform.pivot(5,1,0));
        r.addChild("right_arm",ModelPartBuilder.create().uv(64,0).cuboid(-5,-2,-3,6,6,6)
            .uv(64,14).cuboid(-4,4,-2,4,9,4).uv(100,0).cuboid(-3,10,-17,2,2,21)
            .uv(82,26).cuboid(-5,9,-5,6,4,2),ModelTransform.pivot(-5,1,0));
        r.addChild("left_leg",ModelPartBuilder.create().uv(0,40).cuboid(-2,0,-2,4,12,4),ModelTransform.pivot(2.5f,12,0));
        r.addChild("right_leg",ModelPartBuilder.create().uv(20,40).cuboid(-2,0,-2,4,12,4),ModelTransform.pivot(-2.5f,12,0));
        return TexturedModelData.of(data,256,64);
    }
    @Override public ModelPart getPart() {return root;}
    @Override public void setAngles(RiftSentinelEntity e,float walk,float amount,float time,float yaw,float pitch) {
        root.traverse().forEach(ModelPart::resetTransform);head.yaw=yaw*MathHelper.RADIANS_PER_DEGREE;head.pitch=pitch*MathHelper.RADIANS_PER_DEGREE;
        leftLeg.pitch=MathHelper.cos(walk*.6662f)*amount;rightLeg.pitch=-leftLeg.pitch;
        leftArm.pitch=-leftLeg.pitch*.5f;rightArm.pitch=.1f+leftLeg.pitch*.35f;
        if(e.tell()>0) {float t=1-e.tell()/32f;rightArm.pitch=-2.1f-t*.4f;rightArm.roll=-.3f;body.yaw=-.25f;leftArm.pitch=-.8f;}
        if(e.tell()<0) {rightArm.pitch=-.6f;head.pitch=.4f;body.pitch=.13f;}
    }
}
