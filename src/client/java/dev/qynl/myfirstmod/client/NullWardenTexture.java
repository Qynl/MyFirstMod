package dev.qynl.myfirstmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class NullWardenTexture {
    public static Identifier TEXTURE;
    public static Identifier GLOW_TEXTURE;
    private NullWardenTexture() {}

    public static Identifier register() {
        return registerTexture(buildBaseTexture(), "null_warden");
    }

    public static Identifier registerGlow() {
        return registerTexture(buildGlowTexture(), "null_warden_glow");
    }

    private static Identifier registerTexture(NativeImage image, String name) {
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        return MinecraftClient.getInstance().getTextureManager().registerDynamicTexture(name, texture);
    }

    private static NativeImage buildBaseTexture() {
        NativeImage image = new NativeImage(128, 128, false);

        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                int r = 8;
                int g = 10;
                int b = 15;

                int panel = ((x / 4) + (y / 4)) & 3;
                if (panel == 1) {
                    r = 12;
                    g = 15;
                    b = 22;
                } else if (panel == 2) {
                    r = 15;
                    g = 18;
                    b = 27;
                }

                if ((x + y * 2) % 29 == 0) {
                    r = 18;
                    g = 28;
                    b = 34;
                }

                if ((x * 7 + y * 3) % 113 == 0) {
                    r = 43;
                    g = 14;
                    b = 58;
                }

                image.setColor(x, y, argb(255, r, g, b));
            }
        }

        // Deliberate sculk-like seams and angular fracture lines.
        for (int i = 0; i < 9; i++) {
            int x0 = 6 + i * 13;
            int y0 = 7 + (i * 17) % 78;
            paintLine(image, x0, y0, x0 + 5, y0 + 4, 30, 74, 82);
            paintLine(image, x0 + 5, y0 + 4, x0 + 8, y0 + 1, 17, 43, 54);
        }

        // Controlled violet cracks, concentrated around armor edges.
        for (int i = 0; i < 14; i++) {
            int x0 = 4 + (i * 19) % 120;
            int y0 = 5 + (i * 23) % 86;
            paintLine(image, x0, y0, x0 + 3, y0 + 2, 63, 18, 82);
        }

        // Cyan core glyph in the UV region used by the chest core.
        paintGlowGlyph(image, 0, 60, 28, 10, 33, 236, 255);

        // Signature narrow eye slit.
        fillRect(image, 0, 104, 8, 2, 255, 39, 214, 229);
        fillRect(image, 10, 104, 4, 1, 255, 98, 242, 255);

        // Head/crown accent regions.
        paintGlowGlyph(image, 96, 0, 32, 22, 96, 42, 255);
        paintGlowGlyph(image, 52, 35, 10, 20, 39, 194, 210);

        return image;
    }

    private static NativeImage buildGlowTexture() {
        NativeImage image = new NativeImage(128, 128, true);

        // Transparent by default. Only the model's core, crown and horn UV islands glow.
        fillRect(image, 0, 60, 28, 10, 255, 38, 245, 255);
        fillRect(image, 0, 104, 8, 2, 255, 48, 235, 255);
        fillRect(image, 10, 104, 4, 1, 255, 120, 255, 255);
        fillRect(image, 20, 60, 6, 6, 255, 84, 255, 255);
        fillRect(image, 36, 60, 2, 2, 255, 255, 255, 255);

        fillRect(image, 96, 0, 32, 22, 210, 80, 40, 255);
        fillRect(image, 52, 35, 10, 20, 210, 34, 235, 255);
        fillRect(image, 108, 60, 20, 11, 170, 35, 220, 255);

        return image;
    }

    private static void paintGlowGlyph(NativeImage image, int x, int y, int w, int h, int r, int g, int b) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                int dx = Math.abs(px - cx);
                int dy = Math.abs(py - cy);
                if (dx + dy <= Math.max(2, Math.min(w, h) / 3)) {
                    image.setColor(px, py, argb(255, r, g, b));
                }
            }
        }
    }

    private static void paintLine(NativeImage image, int x0, int y0, int x1, int y1, int r, int g, int b) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            if (x0 >= 0 && x0 < 128 && y0 >= 0 && y0 < 128) {
                image.setColor(x0, y0, argb(255, r, g, b));
            }
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void fillRect(NativeImage image, int x, int y, int w, int h, int a, int r, int g, int b) {
        for (int py = y; py < y + h && py < 128; py++) {
            for (int px = x; px < x + w && px < 128; px++) {
                image.setColor(px, py, argb(a, r, g, b));
            }
        }
    }

    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
