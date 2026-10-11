package dev.qynl.ollamaplayer.vision;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * A small top-down picture of the terrain around Nova, drawn from block map colours. This is the
 * "visual" input for the model: rendered by code from world data, not a screenshot, so it costs a few
 * hundred block lookups. Nova's own position is marked magenta.
 */
public final class Minimap {
    public static final int RADIUS = 16;

    private Minimap() {}

    public static String renderBase64(ServerWorld world, BlockPos center) throws IOException {
        int size = RADIUS * 2 + 1;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                int rgb = world.getBlockState(p).getMapColor(world, p).color & 0xFFFFFF;
                if (dx == 0 && dz == 0) rgb = 0xFF00FF;
                img.setRGB(dx + RADIUS, dz + RADIUS, rgb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
