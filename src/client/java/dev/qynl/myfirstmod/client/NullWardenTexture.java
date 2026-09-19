package dev.qynl.myfirstmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class NullWardenTexture {
    private NullWardenTexture() {}

    public static Identifier register() {
        NativeImage image = new NativeImage(128, 96, false);

        // Deliberately painted pixel-art palette instead of procedural noise:
        // void-black armor, sculk teal seams, and a controlled violet energy glow.
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                int r = 9, g = 11, b = 16;

                boolean armor = ((x / 8 + y / 8) & 1) == 0;
                if (armor) {
                    r = 13; g = 16; b = 23;
                }

                // Sculk-like edge seams.
                if ((x % 16 == 0 || y % 16 == 0) && (x + y) % 32 < 18) {
                    r = 11; g = 35; b = 43;
                }

                // Violet fracture lines.
                if ((x * 3 + y * 5) % 47 == 0 || (x * 5 - y * 2 + 128) % 61 == 0) {
                    r = 54; g = 15; b = 76;
                }

                image.setColor(x, y, (255 << 24) | (b << 16) | (g << 8) | r);
            }
        }

        // Chest/core energy glyph.
        for (int y = 44; y <= 55; y++) {
            int half = 1 + Math.max(0, 5 - Math.abs(50 - y));
            for (int x = 64 - half; x <= 64 + half; x++) {
                image.setColor(x, y, 0xFF22FFFF);
            }
        }

        // Crown and horn highlights.
        for (int x = 48; x < 80; x++) {
            if ((x & 3) == 0) {
                image.setColor(x, 9, 0xFF8A2BE2);
            }
        }

        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        return MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture("null_warden", texture);
    }
}
