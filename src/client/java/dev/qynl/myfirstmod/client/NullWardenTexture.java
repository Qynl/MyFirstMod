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

        // Layered Null-forged material palette. Broad regions stay readable at
        // game distance, while fine seams provide close-up detail.
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                int r = 7, g = 9, b = 13;
                int band = (x / 16 + y / 12) & 3;
                switch (band) {
                    case 1 -> { r = 13; g = 16; b = 22; }
                    case 2 -> { r = 19; g = 22; b = 29; }
                    case 3 -> { r = 10; g = 13; b = 18; }
                }

                // Subtle vertical grain keeps large armor plates from reading flat.
                int grain = (x * 13 + y * 7) & 15;
                if (grain == 0 || grain == 1) {
                    r += 5; g += 6; b += 8;
                }

                // Occasional cold mineral flecks.
                if ((x * 17 + y * 29) % 137 == 0) {
                    r = 31; g = 39; b = 47;
                }

                image.setColor(x, y, argb(255, r, g, b));
            }
        }

        // Distinct panel fields: chest/torso, limbs, head and mantle each use
        // a different value range so the silhouette remains legible.
        fillRect(image, 0, 0, 42, 32, 255, 15, 17, 23);
        fillRect(image, 43, 0, 43, 32, 255, 9, 12, 18);
        fillRect(image, 87, 0, 41, 32, 255, 20, 19, 28);
        fillRect(image, 0, 32, 31, 28, 255, 11, 14, 20);
        fillRect(image, 32, 32, 32, 28, 255, 18, 20, 27);
        fillRect(image, 65, 32, 31, 28, 255, 12, 15, 21);
        fillRect(image, 97, 32, 31, 28, 255, 16, 18, 25);
        fillRect(image, 0, 61, 42, 35, 255, 12, 15, 21);
        fillRect(image, 43, 61, 43, 35, 255, 22, 22, 28);
        fillRect(image, 87, 61, 41, 35, 255, 8, 11, 16);

        // Hard armor boundaries. Broken segments imply overlapping plates.
        int[][] seams = {
                {2, 31, 39, 31}, {44, 30, 82, 30}, {88, 29, 125, 29},
                {3, 59, 27, 59}, {35, 61, 62, 61}, {68, 58, 94, 58},
                {99, 62, 125, 62}, {4, 94, 38, 94}, {47, 91, 81, 91},
                {90, 95, 124, 95}
        };
        for (int i = 0; i < seams.length; i++) {
            int[] s = seams[i];
            paintLine(image, s[0], s[1], s[2], s[3],
                    i % 2 == 0 ? 4 : 30, i % 2 == 0 ? 7 : 34, i % 2 == 0 ? 11 : 42);
        }

        // Signature fractured armor seams, intentionally asymmetric.
        int[][] fractures = {
                {10, 8, 20, 15}, {27, 5, 35, 18}, {47, 22, 57, 14},
                {67, 6, 76, 13}, {82, 17, 91, 10}, {98, 34, 115, 26},
                {6, 42, 17, 36}, {24, 48, 34, 59}, {43, 43, 53, 37},
                {57, 52, 69, 64}, {74, 45, 83, 54}, {96, 56, 113, 49},
                {9, 73, 22, 66}, {35, 69, 46, 82}, {63, 76, 77, 68},
                {85, 78, 96, 88}, {105, 71, 121, 79}
        };
        for (int i = 0; i < fractures.length; i++) {
            int[] f = fractures[i];
            paintLine(image, f[0], f[1], f[2], f[3], 43, 13, 55);
            paintLine(image, f[0], f[1] + 1, f[2], f[3] + 1, 18, 24, 31);
            if ((i & 1) == 0) {
                paintLine(image, f[2], f[3], f[2] + 2, f[3] + 4, 26, 78, 84);
            }
        }

        // Raised plate highlights: narrow, offset strips rather than outlines.
        int[][] highlights = {
                {4, 3, 18, 3}, {48, 4, 61, 4}, {91, 4, 105, 4},
                {5, 35, 18, 35}, {38, 34, 54, 34}, {71, 34, 83, 34},
                {8, 64, 20, 64}, {49, 64, 62, 64}, {92, 66, 107, 66}
        };
        for (int[] h : highlights) {
            paintLine(image, h[0], h[1], h[2], h[3], 48, 53, 61);
        }

        // Small cyan stress points make energy leak through the fractures.
        for (int i = 0; i < 14; i++) {
            int x = 7 + (i * 29) % 114;
            int y = 7 + (i * 19) % 84;
            fillRect(image, x, y, 1 + (i & 1), 1, 255, 35, 148, 160);
        }

        // Core, eye, crown and horn UV islands.
        paintGlowGlyph(image, 0, 60, 28, 10, 33, 236, 255);
        fillRect(image, 0, 104, 8, 2, 255, 39, 214, 229);
        fillRect(image, 10, 104, 4, 1, 255, 98, 242, 255);
        paintGlowGlyph(image, 96, 0, 32, 22, 96, 42, 255);
        paintGlowGlyph(image, 52, 35, 10, 20, 39, 194, 210);

        // Dark secondary material on the lower atlas prevents accidental
        // bright sampling on newly added fragment faces.
        fillRect(image, 0, 108, 128, 20, 255, 4, 6, 10);

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
