package dev.qynl.racers.car;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

import dev.qynl.racers.registry.ModBlocks;

/** How the ground under the wheels changes the physics. */
public enum SurfaceType {
    ROAD(1.00, 1.00, 1.00, 1.00, false),
    PIT(1.00, 1.00, 1.00, 1.00, false),
    KERB(0.96, 0.96, 0.88, 1.25, false),
    ROUGH(0.80, 0.82, 0.84, 1.30, false),
    GRASS(0.62, 0.60, 0.66, 1.70, false),
    SAND(0.55, 0.52, 0.58, 2.10, false),
    STONE(0.86, 0.88, 0.90, 1.20, false),
    ICE(0.90, 0.95, 0.12, 0.35, false),
    SLIME(0.88, 0.90, 0.70, 1.45, true),
    WATER(0.45, 0.40, 0.55, 3.20, false),
    AIR(1.0, 1.0, 1.0, 1.0, false);

    /** Multiplies engine power. */
    public final double powerMul;
    /** Multiplies top speed. */
    public final double speedMul;
    /** Multiplies available grip. */
    public final double gripMul;
    /** Multiplies rolling resistance. */
    public final double rollMul;
    public final boolean bouncy;

    SurfaceType(double powerMul, double speedMul, double gripMul, double rollMul, boolean bouncy) {
        this.powerMul = powerMul;
        this.speedMul = speedMul;
        this.gripMul = gripMul;
        this.rollMul = rollMul;
        this.bouncy = bouncy;
    }

    public static SurfaceType of(BlockState state) {
        if (state.isAir()) {
            return AIR;
        }
        var block = state.getBlock();
        if (block == ModBlocks.ASPHALT || block == ModBlocks.ASPHALT_CRACKED
                || block == ModBlocks.ROAD_BLOCK || block == ModBlocks.FINISH_LINE
                || block == ModBlocks.BOOST_PAD || block == ModBlocks.STATION) {
            return ROAD;
        }
        if (block == ModBlocks.PIT_LANE) {
            return PIT;
        }
        if (block == ModBlocks.KERB) {
            return KERB;
        }
        if (block == ModBlocks.FAKE_ICE) {
            return ICE;
        }
        if (block == ModBlocks.FAKE_SLIME) {
            return SLIME;
        }
        if (state.isOf(Blocks.ICE) || state.isOf(Blocks.PACKED_ICE) || state.isOf(Blocks.BLUE_ICE)
                || state.isOf(Blocks.FROSTED_ICE)) {
            return ICE;
        }
        if (state.isOf(Blocks.SLIME_BLOCK)) {
            return SLIME;
        }
        if (state.isOf(Blocks.WATER) || state.isOf(Blocks.KELP)
                || state.isOf(Blocks.SEAGRASS) || state.isOf(Blocks.BUBBLE_COLUMN)) {
            return WATER;
        }
        if (state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.FLOWERS) || state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.FARMLAND)) {
            return GRASS;
        }
        if (state.isIn(BlockTags.SAND) || state.isOf(Blocks.SOUL_SAND) || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.CLAY)) {
            return SAND;
        }
        if (state.isIn(BlockTags.STONE_BRICKS) || state.isIn(BlockTags.BASE_STONE_OVERWORLD)
                || state.isOf(Blocks.STONE) || state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.SMOOTH_STONE) || state.isIn(BlockTags.WOOL)
                || state.isIn(BlockTags.PLANKS) || state.isOf(Blocks.QUARTZ_BLOCK)) {
            return STONE;
        }
        // anything else solid-but-unclassified (concrete, terracotta, ...) is rough
        return ROUGH;
    }
}
