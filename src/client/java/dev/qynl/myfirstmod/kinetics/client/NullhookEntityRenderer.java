package dev.qynl.myfirstmod.kinetics.client;

import dev.qynl.myfirstmod.kinetics.NullhookEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Renders the Nullhook: a glowing rope back to the owner plus a slowly
 * spinning wireframe octahedron head whose color reflects the hook state.
 */
public class NullhookEntityRenderer extends EntityRenderer<NullhookEntity> {
    private static final Identifier TEXTURE =
            Identifier.of("minecraft", "textures/block/sculk.png");

    private static final float[][] OCTAHEDRON_VERTICES = {
            {0.0F, 1.0F, 0.0F},   // top
            {1.0F, 0.0F, 0.0F},   // east
            {0.0F, 0.0F, 1.0F},   // south
            {-1.0F, 0.0F, 0.0F},  // west
            {0.0F, 0.0F, -1.0F},  // north
            {0.0F, -1.0F, 0.0F},  // bottom
    };

    private static final int[][] OCTAHEDRON_EDGES = {
            {0, 1}, {0, 2}, {0, 3}, {0, 4},
            {5, 1}, {5, 2}, {5, 3}, {5, 4},
            {1, 2}, {2, 3}, {3, 4}, {4, 1},
    };

    public NullhookEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(NullhookEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(NullhookEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);

        byte state = entity.getHookState();
        float r, g, b;
        switch (state) {
            case NullhookEntity.STATE_ANCHORED -> { r = 0.65F; g = 1.0F; b = 0.95F; }
            case NullhookEntity.STATE_RETRACTING -> { r = 0.55F; g = 0.6F; b = 0.7F; }
            default -> { r = 0.4F; g = 0.9F; b = 0.85F; }
        }

        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer lines = vertexConsumers.getBuffer(RenderLayer.getLines());

        // ---- rope to owner ----------------------------------------------
        PlayerEntity owner = entity.getOwner();
        if (owner != null) {
            Vec3d hookPos = entity.getLerpedPos(tickDelta);
            Vec3d ownerPos = owner.getLerpedPos(tickDelta).add(0.0, 1.35, 0.0);
            Vec3d rel = ownerPos.subtract(hookPos);
            drawLine(lines, entry, 0, 0, 0, (float) rel.x, (float) rel.y, (float) rel.z,
                    r * 0.7F, g * 0.7F, b * 0.7F, 0.9F);
            // A second, shorter strand near the hook for a bit of thickness.
            float len = (float) rel.length();
            if (len > 0.4F) {
                Vec3d dir = rel.multiply(1.0 / len);
                drawLine(lines, entry,
                        dir.x * 0.15F, dir.y * 0.15F, dir.z * 0.15F,
                        (float) rel.x * 0.55F, (float) rel.y * 0.55F, (float) rel.z * 0.55F,
                        r, g, b, 0.5F);
            }
        }

        // ---- spinning octahedron head -----------------------------------
        float spin = (entity.age + tickDelta) * 2.4F;
        float size = state == NullhookEntity.STATE_ANCHORED ? 0.22F : 0.18F;
        float cos = MathHelper.cos(spin);
        float sin = MathHelper.sin(spin);

        for (int[] edge : OCTAHEDRON_EDGES) {
            float[] a = rotate(OCTAHEDRON_VERTICES[edge[0]], cos, sin, size);
            float[] b = rotate(OCTAHEDRON_VERTICES[edge[1]], cos, sin, size);
            drawLine(lines, entry, a[0], a[1], a[2], b[0], b[1], b[2], r, g, b, 1.0F);
        }
    }

    private static float[] rotate(float[] v, float cos, float sin, float size) {
        // Rotate around the Y axis, then scale.
        float x = v[0] * cos + v[2] * sin;
        float z = -v[0] * sin + v[2] * cos;
        return new float[]{x * size, v[1] * size, z * size};
    }

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-5F) len = 1.0F;
        float nx = dx / len;
        float ny = dy / len;
        float nz = dz / len;

        consumer.vertex(entry, x1, y1, z1).color(r, g, b, a).normal(entry, nx, ny, nz);
        consumer.vertex(entry, x2, y2, z2).color(r, g, b, a).normal(entry, nx, ny, nz);
    }
}
