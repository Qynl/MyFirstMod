package dev.qynl.myfirstmod.client.model;

import dev.qynl.myfirstmod.mob.RiftHeraldEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

/** Original floating-core silhouette, not a reskinned vanilla mob model. */
public final class RiftHeraldModel extends SinglePartEntityModel<RiftHeraldEntity> {
    private final ModelPart root,core,crown;
    private final ModelPart[] shards=new ModelPart[4];
    public RiftHeraldModel(ModelPart root) {
        this.root=root;core=root.getChild("core");crown=root.getChild("crown");
        for(int i=0;i<4;i++) shards[i]=root.getChild("shard"+i);
    }
    public static TexturedModelData data() {
        ModelData data=new ModelData();ModelPartData root=data.getRoot();
        root.addChild("core",ModelPartBuilder.create().uv(0,0).cuboid(-5,-5,-5,10,10,10),ModelTransform.pivot(0,5,0));
        root.addChild("crown",ModelPartBuilder.create().uv(0,24).cuboid(-7,-2,-7,14,2,14)
                .uv(0,42).cuboid(-2,-8,-2,4,6,4),ModelTransform.pivot(0,-4,0));
        for(int i=0;i<4;i++) root.addChild("shard"+i,ModelPartBuilder.create().uv(42,0)
                .cuboid(-2,-8,-2,4,16,4).uv(36,24).cuboid(-3,4,-3,6,5,6),ModelTransform.NONE);
        return TexturedModelData.of(data,64,64);
    }
    @Override public ModelPart getPart() {return root;}
    @Override public void setAngles(RiftHeraldEntity entity,float limbAngle,float limbDistance,float time,float yaw,float pitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        float charge=entity.charge()/40f;
        core.pivotY=5+MathHelper.sin(time*.06f)*1.4f;
        core.yaw=time*.025f;core.roll=MathHelper.sin(time*.04f)*.15f;
        crown.yaw=-time*.014f;crown.pivotY=-4-charge*2;
        for(int i=0;i<4;i++) {
            float angle=time*.018f+i*MathHelper.PI/2;
            float radius=10+(entity.visualAttack()==2?charge*5:charge*2);
            shards[i].pivotX=MathHelper.cos(angle)*radius;
            shards[i].pivotZ=MathHelper.sin(angle)*radius;
            shards[i].pivotY=8+MathHelper.sin(time*.09f+i)*2;
            shards[i].yaw=-angle;shards[i].roll=(i%2==0?1:-1)*(.2f+charge*.35f);
        }
    }
}
