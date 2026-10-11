package dev.qynl.racers.client.render;

import org.joml.Matrix4f;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.MathHelper;

import dev.qynl.racers.car.CarSpec;
import dev.qynl.racers.car.CarType;

/**
 * Hand written car geometry.
 *
 * <p>Instead of box-UV models the mod uses a small custom mesh whose faces are
 * mapped onto the hand painted 64x64 car atlas (see tools/gen_textures.py and
 * the layout constants below). That keeps the cars a single quad batch each and
 * lets the body be tinted with the player's paint while decals stay textured.
 *
 * <p>Model space: +Z is forward, +X is the car's left, +Y is up. All numbers are
 * in blocks.
 */
public final class CarMesh {
    // --- atlas regions (must match tools/gen_textures.py) -------------------
    public static final Uv BODY_TOP = new Uv(0, 0, 32, 16);
    public static final Uv BODY_SIDE = new Uv(0, 16, 32, 32);
    public static final Uv BODY_FRONT = new Uv(0, 32, 16, 44);
    public static final Uv BODY_BACK = new Uv(16, 32, 32, 44);
    public static final Uv GLASS = new Uv(32, 32, 48, 48);
    public static final Uv DETAIL = new Uv(48, 32, 64, 48);
    public static final Uv WHEEL_SIDE = new Uv(32, 0, 48, 16);
    public static final Uv WHEEL_TREAD = new Uv(32, 16, 48, 32);
    public static final Uv WHEEL_HUB = new Uv(48, 0, 64, 16);
    public static final Uv EXTRA = new Uv(48, 16, 64, 32);
    public static final Uv ICON = new Uv(0, 48, 32, 64);
    public static final Uv TIRE_ICON = new Uv(32, 48, 48, 64);

    /** A rectangle of the atlas in 0..1 texture space. */
    public record Uv(float u0, float v0, float u1, float v1) {
        public Uv shrink(float amount) {
            return new Uv(u0 + amount, v0 + amount, u1 - amount, v1 - amount);
        }

        public Uv sub(float x0, float y0, float x1, float y1, float width, float height) {
            float w = u1 - u0;
            float h = v1 - v0;
            return new Uv(u0 + w * (x0 / width), v0 + h * (y0 / height),
                    u0 + w * (x1 / width), v0 + h * (y1 / height));
        }
    }

    private CarMesh() {
    }

    // ------------------------------------------------------------------
    // primitives
    // ------------------------------------------------------------------
    /** A box with one texture rectangle per face. */
    public static void box(VertexConsumer vc, Matrix4f matrix, float x0, float y0, float z0,
                           float x1, float y1, float z1, float red, float green, float blue,
                           float alpha, int light, Uv top, Uv bottom, Uv south, Uv north,
                           Uv east, Uv west) {
        // top (+Y)
        quad(vc, matrix, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1,
                top, red, green, blue, alpha, light, 0.0F, 1.0F, 0.0F);
        // bottom (-Y)
        quad(vc, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0,
                bottom, red, green, blue, alpha, light, 0.0F, -1.0F, 0.0F);
        // south / front (+Z)
        quad(vc, matrix, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1,
                south, red, green, blue, alpha, light, 0.0F, 0.0F, 1.0F);
        // north / back (-Z)
        quad(vc, matrix, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0,
                north, red, green, blue, alpha, light, 0.0F, 0.0F, -1.0F);
        // east / left (+X)
        quad(vc, matrix, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0,
                east, red, green, blue, alpha, light, 1.0F, 0.0F, 0.0F);
        // west / right (-X)
        quad(vc, matrix, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1,
                west, red, green, blue, alpha, light, -1.0F, 0.0F, 0.0F);
    }

    /** A box with the same rectangle on every face. */
    public static void boxAll(VertexConsumer vc, Matrix4f matrix, float x0, float y0, float z0,
                              float x1, float y1, float z1, float red, float green, float blue,
                              float alpha, int light, Uv uv) {
        box(vc, matrix, x0, y0, z0, x1, y1, z1, red, green, blue, alpha, light,
                uv, uv, uv, uv, uv, uv);
    }

    private static void quad(VertexConsumer vc, Matrix4f matrix, float x0, float y0, float z0,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, Uv uv, float red, float green,
                             float blue, float alpha, int light, float nx, float ny, float nz) {
        // texture V grows downwards, so the first vertex pair sits at uv.v1 of the rect
        vertex(vc, matrix, x0, y0, z0, uv.u0(), uv.v1(), red, green, blue, alpha, light, nx, ny, nz);
        vertex(vc, matrix, x1, y1, z1, uv.u0(), uv.v0(), red, green, blue, alpha, light, nx, ny, nz);
        vertex(vc, matrix, x2, y2, z2, uv.u1(), uv.v0(), red, green, blue, alpha, light, nx, ny, nz);
        vertex(vc, matrix, x3, y3, z3, uv.u1(), uv.v1(), red, green, blue, alpha, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix, float x, float y, float z,
                               float u, float v, float red, float green, float blue, float alpha,
                               int light, float nx, float ny, float nz) {
        vc.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .texture(u, v)
                .overlay(net.minecraft.client.render.OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(nx, ny, nz);
    }

    /** A wheel: an octagonal prism aligned along X, with tread, sidewall and hub. */
    public static void wheel(VertexConsumer vc, Matrix4f matrix, float x, float y, float z,
                             float radius, float width, float spin, int light) {
        int sides = 8;
        float half = width / 2.0F;
        float[] sy = new float[sides];
        float[] sz = new float[sides];
        for (int i = 0; i < sides; i++) {
            double angle = (i / (double) sides) * Math.PI * 2.0 + spin;
            sy[i] = (float) Math.sin(angle) * radius;
            sz[i] = (float) Math.cos(angle) * radius;
        }
        float tu0 = WHEEL_TREAD.u0();
        float tu1 = WHEEL_TREAD.u1();
        float tv0 = WHEEL_TREAD.v0();
        float tv1 = WHEEL_TREAD.v1();
        float su0 = WHEEL_SIDE.u0();
        float sv0 = WHEEL_SIDE.v0();
        float sw = WHEEL_SIDE.u1() - WHEEL_SIDE.u0();
        float sh = WHEEL_SIDE.v1() - WHEEL_SIDE.v0();
        float hu0 = WHEEL_HUB.u0();
        float hv0 = WHEEL_HUB.v0();
        float hw = WHEEL_HUB.u1() - WHEEL_HUB.u0();
        float hh = WHEEL_HUB.v1() - WHEEL_HUB.v0();

        for (int i = 0; i < sides; i++) {
            int j = (i + 1) % sides;
            float uA = tu0 + (tu1 - tu0) * (i / (float) sides);
            float uB = tu0 + (tu1 - tu0) * ((i + 1) / (float) sides);
            float ny = (sy[i] + sy[j]) / (2.0F * radius);
            float nz = (sz[i] + sz[j]) / (2.0F * radius);
            // tread band
            vertex(vc, matrix, x - half, y + sy[i], z + sz[i], uA, tv1, 1, 1, 1, 1, light, ny, nz);
            vertex(vc, matrix, x + half, y + sy[i], z + sz[i], uA, tv0, 1, 1, 1, 1, light, ny, nz);
            vertex(vc, matrix, x + half, y + sy[j], z + sz[j], uB, tv0, 1, 1, 1, 1, light, ny, nz);
            vertex(vc, matrix, x - half, y + sy[j], z + sz[j], uB, tv1, 1, 1, 1, 1, light, ny, nz);

            float sideU = 0.5F, sideV = 0.5F;
            float edgeU = 0.5F + 0.5F * (sz[i] / radius);
            float edgeV = 0.5F - 0.5F * (sy[i] / radius);
            float edgeU2 = 0.5F + 0.5F * (sz[j] / radius);
            float edgeV2 = 0.5F - 0.5F * (sy[j] / radius);
            // outer sidewall (rim art, projected like a disc)
            vertex(vc, matrix, x + half, y, z, su0 + sw * sideU, sv0 + sh * sideV, 1, 1, 1, 1, light, 1, 0, 0);
            vertex(vc, matrix, x + half, y + sy[i], z + sz[i], su0 + sw * edgeU, sv0 + sh * edgeV,
                    1, 1, 1, 1, light, 1, 0, 0);
            vertex(vc, matrix, x + half, y + sy[j], z + sz[j], su0 + sw * edgeU2, sv0 + sh * edgeV2,
                    1, 1, 1, 1, light, 1, 0, 0);
            vertex(vc, matrix, x + half, y, z, su0 + sw * sideU, sv0 + sh * sideV, 1, 1, 1, 1, light, 1, 0, 0);
            // inner face (hub)
            vertex(vc, matrix, x - half, y + sy[j], z + sz[j], hu0 + hw * edgeU2, hv0 + hh * edgeV2,
                    1, 1, 1, 1, light, -1, 0, 0);
            vertex(vc, matrix, x - half, y + sy[i], z + sz[i], hu0 + hw * edgeU, hv0 + hh * edgeV,
                    1, 1, 1, 1, light, -1, 0, 0);
            vertex(vc, matrix, x - half, y, z, hu0 + hw * 0.5F, hv0 + hh * 0.5F, 1, 1, 1, 1, light, -1, 0, 0);
            vertex(vc, matrix, x - half, y, z, hu0 + hw * 0.5F, hv0 + hh * 0.5F, 1, 1, 1, 1, light, -1, 0, 0);
        }
    }

    // ------------------------------------------------------------------
    // full car
    // ------------------------------------------------------------------
    /**
     * Draws one whole car.
     *
     * @param bodyColor  tint applied to the body panels (the player's paint)
     * @param trimColor  tint applied to the decal/trim panels
     * @param steer       front wheel steer angle in degrees
     * @param spin       wheel spin in radians
     * @param showDriver draw the AI driver + helmet
     */
    public static void car(VertexConsumer vc, Matrix4f matrix, CarType type, CarSpec spec,
                           int bodyColor, int trimColor, float roll, float pitch,
                           float steer, float spin, boolean showDriver, int light,
                           float damage) {
        float half = spec.width() / 2.0F;
        float length = spec.length() / 2.0F;
        float wheelRadius = spec.wheelRadius();
        float wheelWidth = Math.max(0.16F, spec.width() * 0.13F);
        float lift = spec.bodyLift();

        float br = ((bodyColor >> 16) & 0xFF) / 255.0F;
        float bg = ((bodyColor >> 8) & 0xFF) / 255.0F;
        float bb = (bodyColor & 0xFF) / 255.0F;
        float tr = ((trimColor >> 16) & 0xFF) / 255.0F;
        float tg = ((trimColor >> 8) & 0xFF) / 255.0F;
        float tb = (trimColor & 0xFF) / 255.0F;

        // --- wheels first (they sit under the body) ----------------------
        float axleFront = length - wheelRadius - 0.05F;
        float axleRear = -length + wheelRadius + 0.05F;
        float axleY = wheelRadius;
        float trackHalf = half - wheelWidth * 0.55F;
        float frontSteer = (float) Math.toRadians(steer);

        wheelWithSteer(vc, matrix, -trackHalf, axleY, axleFront, wheelRadius, wheelWidth, spin,
                frontSteer, light);
        wheelWithSteer(vc, matrix, trackHalf, axleY, axleFront, wheelRadius, wheelWidth, spin,
                frontSteer, light);
        wheelWithSteer(vc, matrix, -trackHalf, axleY, axleRear, wheelRadius, wheelWidth, spin, 0.0F, light);
        wheelWithSteer(vc, matrix, trackHalf, axleY, axleRear, wheelRadius, wheelWidth, spin, 0.0F, light);
        if (spec.style() == CarSpec.BodyStyle.TRUCK) {
            wheelWithSteer(vc, matrix, -trackHalf, axleY, axleRear + wheelRadius * 2.2F, wheelRadius,
                    wheelWidth, spin, 0.0F, light);
            wheelWithSteer(vc, matrix, trackHalf, axleY, axleRear + wheelRadius * 2.2F, wheelRadius,
                    wheelWidth, spin, 0.0F, light);
        }

        // --- body --------------------------------------------------------
        float bodyTop = lift + spec.height() * 0.62F;
        float hullBottom = wheelRadius * 0.55F;
        box(vc, matrix, -half, hullBottom, -length, half, bodyTop, length,
                br, bg, bb, 1.0F, light, BODY_TOP, BODY_SIDE, BODY_FRONT, BODY_BACK,
                BODY_SIDE, BODY_SIDE);

        // --- cabin / glass ------------------------------------------------
        if (spec.cockpitWindow()) {
            float cabinFront = length * (spec.style() == CarSpec.BodyStyle.TRUCK ? 0.55F : 0.15F);
            float cabinBack = -length * 0.55F;
            float cabinTop = bodyTop + spec.height() * 0.42F;
            float inset = half * 0.88F;
            box(vc, matrix, -inset, bodyTop - 0.02F, cabinBack, inset, cabinTop, cabinFront,
                    br, bg, bb, 1.0F, light, BODY_TOP, BODY_SIDE, GLASS, GLASS, GLASS, GLASS);
        } else {
            // open wheel racer: airbox + head rest
            boxAll(vc, matrix, -half * 0.34F, bodyTop, -length * 0.42F, half * 0.34F,
                    bodyTop + spec.height() * 0.34F, -length * 0.05F, tr, tg, tb, 1.0F, light, EXTRA);
        }

        // --- front wing / splitter ----------------------------------------
        if (spec.openWheels()) {
            boxAll(vc, matrix, -half * 1.05F, wheelRadius * 0.55F, length * 0.92F,
                    half * 1.05F, wheelRadius * 0.85F, length * 1.06F, tr, tg, tb, 1.0F, light, EXTRA);
            boxAll(vc, matrix, -half * 0.75F, bodyTop + spec.height() * 0.45F, -length * 1.02F,
                    half * 0.75F, bodyTop + spec.height() * 0.6F, -length * 0.86F, tr, tg, tb, 1.0F,
                    light, EXTRA);
        }

        // --- spoiler (from the aero tuning level) -------------------------
        float spoiler = lift + spec.height() * 0.95F;
        boxAll(vc, matrix, -half * 0.98F, spoiler, -length * 0.98F, half * 0.98F, spoiler + 0.12F,
                -length * 0.80F, 0.16F, 0.16F, 0.18F, 1.0F, light, EXTRA);

        // --- lights -------------------------------------------------------
        float lightGlow = damage > 0.5F ? 0.35F : 1.0F;
        boxAll(vc, matrix, -half * 0.94F, lift + spec.height() * 0.22F, length - 0.02F,
                -half * 0.5F, lift + spec.height() * 0.38F, length + 0.02F,
                1.0F, 0.95F * lightGlow, 0.7F * lightGlow, 1.0F, light, DETAIL);
        boxAll(vc, matrix, half * 0.5F, lift + spec.height() * 0.22F, length - 0.02F,
                half * 0.94F, lift + spec.height() * 0.38F, length + 0.02F,
                1.0F, 0.95F * lightGlow, 0.7F * lightGlow, 1.0F, light, DETAIL);

        // --- exhausts / boost flame ---------------------------------------
        float exhaustY = wheelRadius * 0.8F;
        boxAll(vc, matrix, -half * 0.6F, exhaustY - 0.09F, -length - 0.12F,
                -half * 0.35F, exhaustY + 0.03F, -length + 0.06F, 0.75F, 0.75F, 0.78F, 1.0F,
                light, EXTRA);
        boxAll(vc, matrix, half * 0.35F, exhaustY - 0.09F, -length - 0.12F,
                half * 0.6F, exhaustY + 0.03F, -length + 0.06F, 0.75F, 0.75F, 0.78F, 1.0F,
                light, EXTRA);

        // --- driver (AI cars only: human drivers are rendered by Minecraft) -
        if (showDriver) {
            float seat = bodyTop;
            float headY = seat + 0.30F;
            boxAll(vc, matrix, -0.16F, seat - 0.02F, -0.2F, 0.16F, seat + 0.26F, 0.18F,
                    0.22F, 0.24F, 0.32F, 1.0F, light, DETAIL);
            boxAll(vc, matrix, -0.14F, headY + 0.16F, -0.16F, 0.14F, headY + 0.44F, 0.14F,
                    0.90F, 0.90F, 0.94F, 1.0F, light, EXTRA);
            // visor
            boxAll(vc, matrix, -0.12F, headY + 0.26F, 0.13F, 0.12F, headY + 0.38F, 0.16F,
                    0.20F, 0.45F, 0.85F, 1.0F, light, GLASS);
        }
    }

    private static void wheelWithSteer(VertexConsumer vc, Matrix4f matrix, float x, float y, float z,
                                       float radius, float width, float spin, float steer, int light) {
        org.joml.Matrix4f wheel = new org.joml.Matrix4f(matrix);
        wheel.translate(x, y, z);
        if (steer != 0.0F) {
            wheel.rotate(steer, 0.0F, 1.0F, 0.0F);
        }
        wheel(vc, wheel, 0.0F, 0.0F, 0.0F, radius, width, spin, light);
    }
}
