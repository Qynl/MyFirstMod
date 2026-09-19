package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Small, chunk-contained structures: no synchronous exploration-time chunk generation. */
public final class RealmFeatures {
    public static void register() {
        Registry.register(Registries.FEATURE, Identifier.of(MyFirstMod.MOD_ID, "realm_ruins"), new Ruins());
        Registry.register(Registries.FEATURE, Identifier.of(MyFirstMod.MOD_ID, "realm_flora"), new Flora());
    }

    private static void place(StructureWorldAccess world, BlockPos pos, Block block) {
        world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private static BlockPos center(FeatureContext<DefaultFeatureConfig> context) {
        int x = (context.getOrigin().getX() & ~15) + 8;
        int z = (context.getOrigin().getZ() & ~15) + 8;
        return new BlockPos(x, context.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z), z);
    }

    private static boolean reserved(BlockPos p) {
        return Math.abs(p.getX()) < 96 && p.getZ() > -96 && p.getZ() < 192;
    }

    private static final class Ruins extends Feature<DefaultFeatureConfig> {
        Ruins() { super(DefaultFeatureConfig.CODEC); }
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            var world = context.getWorld();
            BlockPos p = center(context);
            if (reserved(p) || p.getY() < 20 || p.getY() > 180) return false;
            int style = context.getRandom().nextInt(3);
            Block wall = style == 0 ? Blocks.DEEPSLATE_TILES : style == 1 ? Blocks.POLISHED_BASALT : Blocks.POLISHED_BLACKSTONE_BRICKS;
            // Reliquary, open observatory, and buried archive share an accessible trial court.
            for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                for (int y = -3; y < 0; y++) place(world, p.add(x,y,z), Blocks.DEEPSLATE_BRICKS);
                place(world, p.add(x,-1,z), (Math.abs(x) == 5 || Math.abs(z) == 5) ? Blocks.CHISELED_DEEPSLATE : wall);
                for (int y = 0; y <= 6; y++) {
                    boolean edge = Math.abs(x) == 6 || Math.abs(z) == 6;
                    boolean door = (Math.abs(x) <= 1 || Math.abs(z) <= 1) && y < 3;
                    boolean pillar = Math.abs(x) == 6 && Math.abs(z) == 6;
                    boolean solid = style == 1 ? pillar : edge && !door;
                    place(world,p.add(x,y,z), solid || (style == 2 && y == 6) ? wall : Blocks.AIR);
                }
            }
            // Archive shelves divide two chambers without blocking the cardinal entrances.
            if (style == 2) for (int x : new int[]{-3,3}) for (int z = -4; z <= 4; z++) {
                if (Math.abs(z) < 2) continue;
                for (int y=0;y<3;y++) place(world,p.add(x,y,z),Blocks.CHISELED_BOOKSHELF);
            }
            for (int x : new int[]{-5,5}) for (int z : new int[]{-5,5}) {
                place(world,p.add(x,0,z),Blocks.CRYING_OBSIDIAN);
                place(world,p.add(x,1,z),Blocks.SOUL_LANTERN);
                place(world,p.add(x,7,z),Blocks.AMETHYST_BLOCK);
            }
            place(world,p,ModBlocks.RESONANCE_CORE);
            BlockPos chest = p.add(4,0,4);
            place(world,chest,Blocks.CHEST);
            LootableInventory.setLootTable(world,context.getRandom(),chest,
                    RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of(MyFirstMod.MOD_ID,"chests/realm_cache")));
            return true;
        }
    }

    private static final class Flora extends Feature<DefaultFeatureConfig> {
        Flora() { super(DefaultFeatureConfig.CODEC); }
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            BlockPos p = center(context);
            if (reserved(p)) return false;
            var world = context.getWorld();
            int height = 3 + context.getRandom().nextInt(7);
            boolean crystal = world.getBlockState(p.down()).isOf(Blocks.CALCITE);
            boolean grove = world.getBlockState(p.down()).isOf(Blocks.SCULK);
            for (int y=0;y<height;y++) {
                place(world,p.up(y),crystal ? Blocks.AMETHYST_BLOCK : Blocks.BASALT);
                if (y < height/2) for (int d : new int[]{-1,1})
                    place(world,p.add(d,y,0),crystal ? Blocks.CALCITE : Blocks.SMOOTH_BASALT);
            }
            if (grove) {
                for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) if (Math.abs(x)+Math.abs(z)<=4)
                    place(world,p.add(x,height-1,z), ((x+z)&1)==0 ? Blocks.WARPED_WART_BLOCK : Blocks.SCULK);
                place(world,p.up(height),Blocks.SHROOMLIGHT);
            } else place(world,p.up(height),Blocks.SEA_LANTERN);
            return true;
        }
    }
}
