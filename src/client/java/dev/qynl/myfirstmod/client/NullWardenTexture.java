package dev.qynl.myfirstmod.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class NullWardenTexture {
    private NullWardenTexture() {}

    public static Identifier register() {
        NativeImage image = new NativeImage(128, 96, false);
        for (int y = 0; y < 96; y++) {
            for (int x = 0; x < 128; x++) {
                double n = Math.sin(x * 0.37) * Math.sin(y * 0.29);
                int base = n > 0.35 ? 24 : 12;
                int blue = n > 0.55 ? 52 : 32;
                int r = base + (x * 3 + y) % 8;
                int g = 10 + (y * 2) % 9;
                int b = blue + (x + y) % 14;
                if (((x * 7 + y * 11) & 31) == 0) { r = 70; g = 12; b = 105; }
                image.setColor(x, y, (255 << 24) | (b << 16) | (g << 8) | r);
            }
        }
        for (int y = 20; y < 76; y++) {
            int x = 64 + (int)(Math.sin(y * 0.42) * 3);
            for (int dx = -1; dx <= 1; dx++) {
                image.setColor(x + dx, y, 0xFF22FFFF);
            }
        }
        NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
        return MinecraftClient.getInstance().getTextureManager()
                .registerDynamicTexture("null_warden", texture);
    }
}
