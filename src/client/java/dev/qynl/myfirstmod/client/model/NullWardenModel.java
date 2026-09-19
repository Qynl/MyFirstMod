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
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart core;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart shoulderLeft;
    private final ModelPart shoulderRight;
    private final ModelPart forearmLeft;
    private final ModelPart forearmRight;
    private final ModelPart waist;
    private final ModelPart mantle;
    private final ModelPart chestPlate;
    private final ModelPart ribLeft;
    private final ModelPart ribRight;
    private final ModelPart jaw;
    private final ModelPart backSpine;
    private final ModelPart spineTip;
    private final ModelPart leftHip;
    private final ModelPart rightHip;
    private final ModelPart crown;
    private final ModelPart shardLeft;
    private final ModelPart shardRight;

    public NullWardenModel(ModelPart root) {
        this.root = root;
        this.torso = root.getChild("torso");
        this.neck = torso.getChild("neck");
        this.head = torso.getChild("head");
        this.core = torso.getChild("core");
        this.leftArm = torso.getChild("left_arm");
        this.rightArm = torso.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftHorn = head.getChild("left_horn");
        this.rightHorn = head.getChild("right_horn");
        this.shoulderLeft = torso.getChild("shoulder_left");
        this.shoulderRight = torso.getChild("shoulder_right");
        this.forearmLeft = leftArm.getChild("forearm_left");
        this.forearmRight = rightArm.getChild("forearm_right");
        this.waist = torso.getChild("waist");
        this.mantle = torso.getChild("mantle");
        this.chestPlate = torso.getChild("chest_plate");
        this.ribLeft = torso.getChild("rib_left");
        this.ribRight = torso.getChild("rib_right");
        this.jaw = head.getChild("jaw");
        this.backSpine = torso.getChild("back_spine");
        this.spineTip = torso.getChild("spine_tip");
        this.leftHip = root.getChild("left_hip");
        this.rightHip = root.getChild("right_hip");
        this.crown = head.getChild("crown");
        this.shardLeft = torso.getChild("shard_left");
        this.shardRight = torso.getChild("shard_right");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();

        r.addChild("torso", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-7, -12, -4, 14, 18, 8)
                        .uv(0, 28).cuboid(-9, -9, -3, 18, 10, 6, new Dilation(1.0f))
                        .uv(0, 48).cuboid(-6, -1, -5, 12, 9, 3),
                ModelTransform.pivot(0, 4, 0));

        ModelPartData t = r.getChild("torso");

        t.addChild("neck", ModelPartBuilder.create()
                        .uv(40, 0).cuboid(-4, -4, -3, 8, 6, 6)
                        .uv(40, 12).cuboid(-5, -2, -4, 10, 4, 8, new Dilation(0.5f)),
                ModelTransform.pivot(0, -11, 0));

        t.addChild("head", ModelPartBuilder.create()
                        .uv(52, 0).cuboid(-5, -7, -5, 10, 9, 10)
                        .uv(52, 19).cuboid(-3, 2, -4, 6, 3, 8)
                        .uv(76, 0).cuboid(-6, -4, -6, 12, 5, 2),
                ModelTransform.pivot(0, -12, 0));

        t.addChild("core", ModelPartBuilder.create()
                        .uv(0, 60).cuboid(-4, -4, -5, 8, 8, 2)
                        .uv(20, 60).cuboid(-3, -3, -6, 6, 6, 2)
                        .uv(36, 60).cuboid(-1, -1, -7, 2, 2, 2),
                ModelTransform.pivot(0, 0, -1));

        t.addChild("chest_plate", ModelPartBuilder.create()
                        .uv(0, 72).cuboid(-7, -8, -5, 14, 9, 2)
                        .uv(32, 72).cuboid(-5, 0, -6, 10, 4, 2)
                        .uv(56, 72).cuboid(-3, 3, -5, 6, 3, 2),
                ModelTransform.pivot(0, 0, 0));

        t.addChild("rib_left", ModelPartBuilder.create()
                        .uv(0, 82).cuboid(0, -3, -6, 7, 3, 2)
                        .uv(0, 88).cuboid(1, 1, -6, 6, 2, 2),
                ModelTransform.pivot(-1, 0, 0));

        t.addChild("rib_right", ModelPartBuilder.create()
                        .uv(16, 82).cuboid(-7, -3, -6, 7, 3, 2)
                        .uv(16, 88).cuboid(-7, 1, -6, 6, 2, 2),
                ModelTransform.pivot(1, 0, 0));

        t.addChild("back_spine", ModelPartBuilder.create()
                        .uv(32, 82).cuboid(-2, -9, 4, 4, 15, 4)
                        .uv(44, 82).cuboid(-3, -2, 6, 6, 4, 3),
                ModelTransform.pivot(0, 0, 0));

        t.addChild("spine_tip", ModelPartBuilder.create()
                        .uv(56, 82).cuboid(-2, -5, 5, 4, 8, 4)
                        .uv(68, 82).cuboid(-1, -8, 6, 2, 4, 2),
                ModelTransform.pivot(0, 6, 0));

        t.addChild("shoulder_left", ModelPartBuilder.create()
                        .uv(0, 92).cuboid(-2, -3, -5, 10, 6, 10)
                        .uv(24, 92).cuboid(6, -1, -4, 5, 3, 8),
                ModelTransform.pivot(7, -6, 0));

        t.addChild("shoulder_right", ModelPartBuilder.create()
                        .uv(0, 92).cuboid(-8, -3, -5, 10, 6, 10)
                        .uv(24, 92).cuboid(-11, -1, -4, 5, 3, 8),
                ModelTransform.pivot(-7, -6, 0));

        t.addChild("waist", ModelPartBuilder.create()
                        .uv(40, 48).cuboid(-8, -1, -5, 16, 4, 10)
                        .uv(40, 56).cuboid(-6, 2, -6, 12, 3, 12),
                ModelTransform.pivot(0, 7, 0));

        t.addChild("mantle", ModelPartBuilder.create()
                        .uv(76, 18).cuboid(-9, -5, 3, 18, 12, 3)
                        .uv(76, 33).cuboid(-7, 6, 3, 14, 7, 3),
                ModelTransform.pivot(0, -4, 0));

        t.addChild("left_arm", ModelPartBuilder.create()
                        .uv(68, 48).cuboid(0, -3, -3, 6, 15, 6)
                        .uv(92, 48).cuboid(3, 7, -4, 5, 8, 8),
                ModelTransform.pivot(8, -5, 0));

        t.addChild("right_arm", ModelPartBuilder.create()
                        .uv(68, 48).cuboid(-6, -3, -3, 6, 15, 6)
                        .uv(92, 48).cuboid(-8, 7, -4, 5, 8, 8),
                ModelTransform.pivot(-8, -5, 0));

        ModelPartData la = t.getChild("left_arm");
        la.addChild("forearm_left", ModelPartBuilder.create()
                        .uv(108, 48).cuboid(0, -1, -4, 5, 8, 8)
                        .uv(108, 60).cuboid(1, 5, -5, 3, 5, 10),
                ModelTransform.pivot(3, 8, 0));

        ModelPartData ra = t.getChild("right_arm");
        ra.addChild("forearm_right", ModelPartBuilder.create()
                        .uv(108, 48).cuboid(-5, -1, -4, 5, 8, 8)
                        .uv(108, 60).cuboid(-4, 5, -5, 3, 5, 10),
                ModelTransform.pivot(-3, 8, 0));

        r.addChild("left_leg", ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-4, 0, -4, 7, 17, 8)
                        .uv(30, 38).cuboid(-5, 14, -5, 9, 5, 10)
                        .uv(0, 56).cuboid(-3, 4, -5, 5, 8, 2),
                ModelTransform.pivot(4, 10, 0));

        r.addChild("right_leg", ModelPartBuilder.create()
                        .uv(0, 38).cuboid(-3, 0, -4, 7, 17, 8)
                        .uv(30, 38).cuboid(-4, 14, -5, 9, 5, 10)
                        .uv(0, 56).cuboid(-2, 4, -5, 5, 8, 2),
                ModelTransform.pivot(-4, 10, 0));

        r.addChild("left_hip", ModelPartBuilder.create()
                        .uv(76, 48).cuboid(-5, -2, -5, 10, 5, 10),
                ModelTransform.pivot(4, 9, 0));

        r.addChild("right_hip", ModelPartBuilder.create()
                        .uv(76, 48).cuboid(-5, -2, -5, 10, 5, 10),
                ModelTransform.pivot(-4, 9, 0));

        ModelPartData h = t.getChild("head");

        h.addChild("left_horn", ModelPartBuilder.create()
                        .uv(52, 35).cuboid(0, -8, -2, 3, 10, 4)
                        .uv(52, 49).cuboid(2, -12, -1, 2, 5, 2)
                        .uv(58, 49).cuboid(3, -15, -1, 2, 4, 2),
                ModelTransform.pivot(4, -2, 0));

        h.addChild("right_horn", ModelPartBuilder.create()
                        .uv(52, 35).cuboid(-3, -8, -2, 3, 10, 4)
                        .uv(52, 49).cuboid(-4, -12, -1, 2, 5, 2)
                        .uv(58, 49).cuboid(-5, -15, -1, 2, 4, 2),
                ModelTransform.pivot(-4, -2, 0));

        h.addChild("jaw", ModelPartBuilder.create()
                        .uv(76, 0).cuboid(-4, 1, -5, 8, 4, 10)
                        .uv(76, 42).cuboid(-3, 4, -4, 6, 2, 8),
                ModelTransform.pivot(0, 0, 0));

        h.addChild("crown", ModelPartBuilder.create()
                        .uv(96, 0).cuboid(-2, -10, -2, 4, 5, 4)
                        .uv(96, 12).cuboid(-7, -7, -1, 5, 3, 3)
                        .uv(108, 12).cuboid(2, -7, -1, 5, 3, 3)
                        .uv(96, 18).cuboid(-2, -14, -1, 4, 5, 2),
                ModelTransform.pivot(0, 0, 0));

        t.addChild("shard_left", ModelPartBuilder.create()
                        .uv(116, 0).cuboid(-2, -3, -1, 4, 7, 2),
                ModelTransform.of(-10, -3, 1, 0, 0, -0.25f));

        t.addChild("shard_right", ModelPartBuilder.create()
                        .uv(116, 10).cuboid(-2, -3, -1, 4, 7, 2),
                ModelTransform.of(10, -3, 2, 0, 0, 0.25f));

        return TexturedModelData.of(data, 128, 96);
    }

    @Override
    public ModelPart getPart() {
        return root;
    }

    @Override
    public void setAngles(NullWardenEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);

        head.yaw = headYaw * ((float) Math.PI / 180F) * 0.55f;
        head.pitch = headPitch * ((float) Math.PI / 180F) * 0.45f;

        float walk = MathHelper.cos(limbAngle * 0.65f) * limbDistance * 0.55f;
        leftLeg.pitch = walk;
        rightLeg.pitch = -walk;
        leftArm.pitch = -walk * 0.45f;
        rightArm.pitch = walk * 0.45f;
        forearmLeft.pitch = walk * 0.18f;
        forearmRight.pitch = -walk * 0.18f;

        float healthRatio = entity.getMaxHealth() <= 0.0f ? 1.0f : entity.getHealth() / entity.getMaxHealth();
        float phase = healthRatio > 0.75f ? 1.0f
                : healthRatio > 0.50f ? 2.0f
                : healthRatio > 0.20f ? 3.0f
                : 4.0f;

        float pulseSpeed = 0.075f + phase * 0.012f;
        float pulse = MathHelper.sin(animationProgress * pulseSpeed);
        float fastPulse = MathHelper.sin(animationProgress * (0.16f + phase * 0.02f));

        float hover = MathHelper.sin(animationProgress * 0.055f) * 0.10f;
        torso.pivotY += hover;
        leftHip.pivotY += hover * 0.35f;
        rightHip.pivotY += hover * 0.35f;

        leftHorn.roll = MathHelper.sin(animationProgress * 0.045f) * (0.07f + phase * 0.012f);
        rightHorn.roll = -leftHorn.roll;
        shoulderLeft.roll = MathHelper.sin(animationProgress * 0.050f) * 0.035f;
        shoulderRight.roll = -shoulderLeft.roll;

        forearmLeft.roll = pulse * 0.025f;
        forearmRight.roll = -pulse * 0.025f;

        waist.yaw = MathHelper.sin(animationProgress * 0.032f) * 0.045f;
        mantle.pitch = MathHelper.sin(animationProgress * 0.050f) * 0.045f;
        chestPlate.pitch = pulse * 0.018f;
        ribLeft.pitch = -pulse * 0.025f;
        ribRight.pitch = pulse * 0.025f;
        jaw.pitch = MathHelper.sin(animationProgress * 0.095f) * 0.022f;

        crown.roll = MathHelper.sin(animationProgress * 0.047f) * 0.035f;
        crown.pitch = fastPulse * 0.018f;

        backSpine.yaw = MathHelper.sin(animationProgress * 0.040f) * 0.045f;
        spineTip.yaw = -backSpine.yaw * 1.5f;

        shardLeft.pitch = MathHelper.sin(animationProgress * 0.060f) * 0.18f;
        shardLeft.roll += MathHelper.sin(animationProgress * 0.041f) * 0.06f;
        shardRight.pitch = -shardLeft.pitch;
        shardRight.roll += MathHelper.sin(animationProgress * 0.041f + 1.5f) * 0.06f;

        core.pitch = pulse * (0.055f + phase * 0.012f);
        core.yaw = fastPulse * 0.035f;

        if (entity.hurtTime > 0) {
            float hit = MathHelper.sin(entity.hurtTime * 0.65f) * 0.08f;
            torso.roll += hit;
            head.roll -= hit * 0.6f;
        }
    }
}
