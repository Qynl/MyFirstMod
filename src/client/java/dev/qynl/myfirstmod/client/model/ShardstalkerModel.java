package dev.qynl.myfirstmod.client.model;
import dev.qynl.myfirstmod.mob.ShardstalkerEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
/** Six bladed limbs, split mandibles, and a jagged exposed crystal spine. */
public final class ShardstalkerModel extends SinglePartEntityModel<ShardstalkerEntity> {
    private final ModelPart root,core,head;
    private final ModelPart[] legs=new ModelPart[6];
    public ShardstalkerModel(ModelPart root) {this.root=root;core=root.getChild("core");head=root.getChild("head");for(int i=0;i<6;i++) legs[i]=root.getChild("leg"+i);}
    public static TexturedModelData data() {
        var data=new ModelData();var r=data.getRoot();
        r.addChild("core",ModelPartBuilder.create().uv(0,0).cuboid(-5,-4,-6,10,7,13)
                .uv(48,0).cuboid(-2,-11,-4,4,8,4).cuboid(-2,-8,3,4,6,4),ModelTransform.pivot(0,16,1));
        r.addChild("head",ModelPartBuilder.create().uv(0,24).cuboid(-4,-3,-5,8,6,6)
                .uv(32,24).cuboid(-4,0,-10,2,3,7).cuboid(2,0,-10,2,3,7),ModelTransform.pivot(0,17,-5));
        for(int i=0;i<6;i++) {
            int side=i<3?-1:1;
            r.addChild("leg"+i,ModelPartBuilder.create().uv(64,0).cuboid(side<0?-10:0,-1,-1,10,2,2)
                    .uv(64,8).cuboid(side<0?-11:9,0,-1,2,8,2),ModelTransform.pivot(side*4,16,(i%3-1)*5));
        }
        return TexturedModelData.of(data,128,64);
    }
    @Override public ModelPart getPart() {return root;}
    @Override public void setAngles(ShardstalkerEntity e,float walk,float amount,float time,float yaw,float pitch) {
        root.traverse().forEach(ModelPart::resetTransform);head.yaw=yaw*.008f;
        for(int i=0;i<6;i++) {float side=i<3?-1:1;legs[i].yaw=(i%3-1)*side*.4f+MathHelper.sin(walk*1.3f+i*2)*amount*.5f;legs[i].roll=side*(.15f+MathHelper.cos(walk*1.3f+i*2)*amount*.18f);}
        core.pivotY+=MathHelper.sin(time*.09f)*.3f;
        if(e.tell()>0 && e.tell()<=30) {core.pivotY+=2;head.pivotY+=2;core.pitch=-.12f;}
        if(e.tell()==40) {core.pitch=-.3f;for(int i=0;i<6;i++) legs[i].roll=(i<3?-1:1)*.65f;}
        if(e.tell()<0) {core.roll=.12f;head.pitch=.3f;}
    }
}
