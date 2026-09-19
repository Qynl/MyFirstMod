package dev.qynl.myfirstmod.client.model;

import dev.qynl.myfirstmod.boss.NullWardenEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class NullWardenModel extends SinglePartEntityModel<NullWardenEntity> {
    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart core;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;

    public NullWardenModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.head = torso.getChild("head");
        this.core = torso.getChild("core");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftHorn = head.getChild("left_horn");
        this.rightHorn = head.getChild("right_horn");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();

        r.addChild("torso", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-7, -12, -4, 14, 18, 8)
                        .uv(0, 28).cuboid(-9, -9, -3, 18, 10, 6, new Dilation(1.0f)),
                ModelTransform.pivot(0, 4, 0));

        ModelPartData t = r.getChild("torso");
        t.addChild("head", ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-5, -7, -5, 10, 9, 10)
                        .uv(52, 19).cuboid(-3, 2, -4, 6, 3, 8),
                ModelTransform.pivot(0, -12, 0));

        t.addChild("core", ModelPartBuilder.create()
                        .uv(0, 48).cuboid(-4, -4, -5, 8, 8, 2)
                        .uv(20, 48).cuboid(-3, -3, -6, 6, 6, 2),
                ModelTransform.pivot(0, 0, -1));

        t.addChild("left_arm", ModelPartBuilder.create()
                        .uv(68, 32).cuboid(0, -3, -3, 6, 15, 6)
                        .uv(92, 32).cuboid(3, 8, -4, 5, 8, 8),
                ModelTransform.pivot(8, -5, 0));
        t.addChild("right_arm", ModelPartBuilder.create()
                        .uv(68, 32).cuboid(-6, -3, -3, 6, 15, 6)
                        .uv(92, 32).cuboid(-8, 8, -4, 5, 8, 8),
                ModelTransform.pivot(-8, -5, 0));

        r.addChild("left_leg", ModelPartBuilder.create()
                        .uv(0, 66).cuboid(-4, 0, -4, 7, 17, 8)
                        .uv(30, 66).cuboid(-5, 14, -5, 9, 5, 10),
                ModelTransform.pivot(4, 10, 0));
        r.addChild("right_leg", ModelPartBuilder.create()
                        .uv(0, 66).cuboid(-3, 0, -4, 7, 17, 8)
                        .uv(30, 66).cuboid(-4, 14, -5, 9, 5, 10),
                ModelTransform.pivot(-4, 10, 0));

        ModelPartData h = t.getChild("head");
        h.addChild("left_horn", ModelPartBuilder.create()
                        .uv(52, 35).cuboid(0, -8, -2, 3, 10, 4)
                        .uv(52, 49).cuboid(2, -12, -1, 2, 5, 2),
                ModelTransform.pivot(4, -2, 0));
        h.addChild("right_horn", ModelPartBuilder.create()
                        .uv(52, 35).cuboid(-3, -8, -2, 3, 10, 4)
                        .uv(52, 49).cuboid(-4, -12, -1, 2, 5, 2),
                ModelTransform.pivot(-4, -2, 0));

        return TexturedModelData.of(data, 128, 96);
    }

    @Override
    public ModelPart getPart() { return root; }

    @Override
    public void setAngles(NullWardenEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        head.yaw = headYaw * ((float)Math.PI / 180F) * 0.55f;
        head.pitch = headPitch * ((float)Math.PI / 180F) * 0.45f;

        float walk = MathHelper.cos(limbAngle * 0.65f) * limbDistance * 0.55f;
        leftLeg.pitch = walk;
        rightLeg.pitch = -walk;
        leftArm.pitch = -walk * 0.45f;
        rightArm.pitch = walk * 0.45f;

        float hover = MathHelper.sin(animationProgress * 0.08f) * 0.08f;
        torso.pivotY += hover;
        leftHorn.roll = MathHelper.sin(animationProgress * 0.045f) * 0.08f;
        rightHorn.roll = -leftHorn.roll;
        core.pitch = MathHelper.sin(animationProgress * 0.12f) * 0.05f;
    }
}
