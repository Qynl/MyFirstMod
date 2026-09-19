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
        Registry.register(Registries.FEATURE, Identifier.of(MyFirstMod.MOD_ID, "realm_vault"), new Ruins(2));
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
        private final int forcedStyle;
        Ruins() { this(-1); }
        Ruins(int forcedStyle) { super(DefaultFeatureConfig.CODEC);this.forcedStyle=forcedStyle; }
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            var world = context.getWorld();
            BlockPos p = center(context);
            if (reserved(p) || p.getY() < 20 || p.getY() > 180) return false;
            int style = forcedStyle>=0?forcedStyle:context.getRandom().nextInt(3);
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
            if(style==2) buildVault(world,p,context);
            return true;
        }
        private void buildVault(StructureWorldAccess world,BlockPos p,FeatureContext<DefaultFeatureConfig> context) {
            // A second, sunken archive storey. All writes remain inside this feature's chunk.
            for(int x=-6;x<=6;x++) for(int z=-6;z<=6;z++) {
                place(world,p.add(x,-10,z),Blocks.DEEPSLATE_TILES);
                for(int y=-9;y<=-2;y++) {
                    boolean wall=Math.abs(x)==6 || Math.abs(z)==6;
                    place(world,p.add(x,y,z),wall?Blocks.DEEPSLATE_BRICKS:Blocks.AIR);
                }
            }
            for(int x:new int[]{-2,2}) for(int z:new int[]{-3,3}) {
                for(int y=-9;y<=-3;y++) place(world,p.add(x,y,z),Blocks.CHISELED_DEEPSLATE);
                place(world,p.add(x,-2,z),Blocks.SOUL_LANTERN);
            }
            // A real staircase rather than a lethal drop or a ladder with no support.
            for(int z=-4;z<=4;z++) {
                int y=z-5;
                for(int x=-5;x<=-4;x++) {
                    for(int clear=1;clear<=3;clear++) place(world,p.add(x,y+clear,z),Blocks.AIR);
                    world.setBlockState(p.add(x,y,z),Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState()
                            .with(net.minecraft.block.StairsBlock.FACING,net.minecraft.util.math.Direction.SOUTH),Block.NOTIFY_LISTENERS);
                }
            }
            BlockPos treasure=p.add(4,-9,-4);
            place(world,treasure,Blocks.CHEST);
            LootableInventory.setLootTable(world,context.getRandom(),treasure,
                    RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of(MyFirstMod.MOD_ID,"chests/vault_cache")));
            place(world,p.add(0,-9,0),Blocks.CRYING_OBSIDIAN);
            place(world,p.add(0,-8,0),Blocks.AMETHYST_BLOCK);
            place(world,p.add(0,-7,0),Blocks.SEA_LANTERN);
        }
    }

    private static final class Flora extends Feature<DefaultFeatureConfig> {
        Flora() { super(DefaultFeatureConfig.CODEC); }
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            var world = context.getWorld();
            int px=(context.getOrigin().getX() & ~15)+4+context.getRandom().nextInt(8);
            int pz=(context.getOrigin().getZ() & ~15)+4+context.getRandom().nextInt(8);
            BlockPos p=new BlockPos(px,world.getTopY(Heightmap.Type.WORLD_SURFACE_WG,px,pz),pz);
            if (reserved(p)) return false;
            int height = 3 + context.getRandom().nextInt(7);
            var surface=world.getBlockState(p.down());
            // Decoration must not grow through a trial core or on an archive roof.
            if(!surface.isOf(ModBlocks.PRISMSTONE) && !surface.isOf(ModBlocks.HUSHED_MOSS)
                    && !surface.isOf(ModBlocks.CINDERSTONE) && !surface.isOf(ModBlocks.LUMEN_MOSS)) return false;
            boolean crystal = surface.isOf(ModBlocks.PRISMSTONE);
            boolean grove = surface.isOf(ModBlocks.HUSHED_MOSS);
            if(surface.isOf(ModBlocks.LUMEN_MOSS)) {
                for(int dx=-2;dx<=2;dx++) for(int dz=-2;dz<=2;dz++) {
                    place(world,p.add(dx,-2,dz),ModBlocks.LUMEN_MOSS);
                    place(world,p.add(dx,-1,dz),Math.abs(dx)==2||Math.abs(dz)==2?ModBlocks.LUMEN_MOSS:Blocks.WATER);
                }
                for(int side:new int[]{-3,3}) {
                    place(world,p.add(side,0,0),ModBlocks.HUSHWOOD);
                    place(world,p.add(side,1,0),ModBlocks.PRISM_LAMP);
                    place(world,p.add(side,2,0),ModBlocks.HUSH_LEAVES);
                }
                return true;
            }
            for (int y=0;y<height;y++) {
                place(world,p.up(y),crystal ? Blocks.AMETHYST_BLOCK : grove ? ModBlocks.HUSHWOOD : ModBlocks.CINDERSTONE);
                if (y < height/2) for (int d : new int[]{-1,1})
                    place(world,p.add(d,y,0),crystal ? ModBlocks.PRISMSTONE : grove ? ModBlocks.HUSHWOOD : ModBlocks.CINDERSTONE);
            }
            if (grove) {
                for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) if (Math.abs(x)+Math.abs(z)<=4)
                    place(world,p.add(x,height-1,z), ModBlocks.HUSH_LEAVES);
                place(world,p.up(height),ModBlocks.PRISM_LAMP);
            } else place(world,p.up(height),Blocks.SEA_LANTERN);
            return true;
        }
    }
}
