package dev.qynl.racers.client.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Matrix4f;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.CarEntity;
import dev.qynl.racers.car.CarSpec;

/**
 * Renders any car: body panels tinted with the owner's paint, wheels that steer
 * and spin, a driver for AI cars and a dimmed front light once the engine is
 * damaged.
 */
public class CarRenderer extends EntityRenderer<CarEntity> {
    private final Map<UUID, float[]> state = new HashMap<>();

    public CarRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 1.2F;
    }

    @Override
    public Identifier getTexture(CarEntity entity) {
        return FabricRacers.id("textures/entity/car/" + entity.carType().textureName() + ".png");
    }

    @Override
    public void render(CarEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        CarSpec spec = entity.carType().spec();
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        float[] spin = state.computeIfAbsent(entity.getUuid(), id -> new float[3]);
        float speed = (float) entity.getSpeed();

        // wheel spin: integrate the travelled distance
        float radius = spec.wheelRadius();
        spin[0] += speed * tickDelta * 0.05F / Math.max(0.1F, radius);
        if (spin[0] > (float) (Math.PI * 2.0)) {
            spin[0] -= (float) (Math.PI * 2.0);
        }
        // body roll follows the slip angle, pitch follows acceleration
        float targetRoll = MathHelper.clamp((float) (-entity.getSlip() * entity.getSteerFactor() * 0.34F),
                -0.2F, 0.2F);
        float targetPitch = MathHelper.clamp(spin[1] * 0.0F + (spin[2] - speed) * -0.006F, -0.16F, 0.16F);
        spin[2] = speed;
        spin[1] += (targetRoll - spin[1]) * 0.2F;
        float roll = spin[1];
        float pitch = targetPitch;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll * 57.2958F));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * 57.2958F));

        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(getTexture(entity)));
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int bodyColor = entity.data().primaryColor;
        int trimColor = entity.data().secondaryColor;
        boolean showDriver = entity.isAiDriving();
        float damage = 1.0F - entity.healthFraction();

        CarMesh.car(consumer, matrix, entity.carType(), spec,
                bodyColor, trimColor, roll, pitch,
                (float) (entity.getSteerFactor() * 30.0), spin[0], showDriver, light, damage);
        matrices.pop();

        // boost flame: a separate translucent pass so it blooms over the road
        if (entity.isBoosting()) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
            VertexConsumer flame = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentCull(
                    FabricRacers.id("textures/entity/car/flame.png")));
            Matrix4f flameMatrix = matrices.peek().getPositionMatrix();
            float wobble = (float) Math.sin((entity.getWorld().getTime() + tickDelta) * 0.9) * 0.06F;
            CarMesh.boxAll(flame, flameMatrix,
                    -0.26F - wobble, 0.22F, -spec.length() / 2.0F - 0.9F - wobble * 2.0F,
                    0.26F + wobble, 0.52F, -spec.length() / 2.0F - 0.2F,
                    1.0F, 0.62F, 0.18F, 0.75F, light, CarMesh.EXTRA);
            matrices.pop();
        }
        spin[2] = speed;
    }
}
