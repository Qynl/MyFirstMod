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

/** Resources and landmarks share no mutable generation state and never cross their chunk. */
public final class WildsFeatures {
    public static void register() {
        Registry.register(Registries.FEATURE,Identifier.of(MyFirstMod.MOD_ID,"realm_resources"),new Resources());
        Registry.register(Registries.FEATURE,Identifier.of(MyFirstMod.MOD_ID,"waystone_shrine"),new Shrine());
    }
    private static void put(StructureWorldAccess world,BlockPos pos,Block block) {
        world.setBlockState(pos,block.getDefaultState(),Block.NOTIFY_LISTENERS);
    }
    private static final class Resources extends Feature<DefaultFeatureConfig> {
        Resources() {super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            var world=context.getWorld();var random=context.getRandom();
            int cx=context.getOrigin().getX() & ~15,cz=context.getOrigin().getZ() & ~15;
            int surface=world.getTopY(Heightmap.Type.WORLD_SURFACE_WG,cx+8,cz+8);
            int ceiling=Math.min(64,surface-6);
            if(ceiling<=-24) return false;
            String biome=world.getBiome(new BlockPos(cx+8,surface,cz+8)).getKey()
                    .map(key->key.getValue().getPath()).orElse("");
            for(int vein=0;vein<12;vein++) {
                BlockPos origin=new BlockPos(cx+3+random.nextInt(10),-32+random.nextInt(ceiling+33),cz+3+random.nextInt(10));
                int roll=random.nextInt(10);
                Block ore=roll<5?ModBlocks.RESONITE_ORE:
                        roll<8?(biome.equals("cinder_steps")?ModBlocks.CINDER_ORE:ModBlocks.PRISM_ORE):
                                (random.nextBoolean()?ModBlocks.PRISM_ORE:ModBlocks.CINDER_ORE);
                for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) for(int z=-1;z<=1;z++) {
                    if(Math.abs(x)+Math.abs(y)+Math.abs(z)>2 || random.nextInt(4)==0) continue;
                    BlockPos pos=origin.add(x,y,z);
                    if(world.getBlockState(pos).isOf(ModBlocks.NULLSTONE)) put(world,pos,ore);
                }
            }
            return true;
        }
    }
    private static final class Shrine extends Feature<DefaultFeatureConfig> {
        Shrine() {super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
            var world=context.getWorld();
            int x=(context.getOrigin().getX() & ~15)+8,z=(context.getOrigin().getZ() & ~15)+8;
            if(Math.abs(x)<96 && z>-96 && z<192) return false;
            BlockPos p=new BlockPos(x,world.getTopY(Heightmap.Type.WORLD_SURFACE_WG,x,z),z);
            if(p.getY()<20 || p.getY()>160) return false;
            var ground=world.getBlockState(p.down());
            if(!(ground.isOf(ModBlocks.HUSHED_MOSS) || ground.isOf(ModBlocks.LUMEN_MOSS)
                    || ground.isOf(ModBlocks.CINDERSTONE) || ground.isOf(ModBlocks.PRISMSTONE))) return false;
            for(int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) {
                if(dx*dx+dz*dz>36) continue;
                for(int dy=-3;dy<0;dy++) put(world,p.add(dx,dy,dz),ModBlocks.NULLSTONE_BRICKS);
                put(world,p.add(dx,-1,dz),(Math.abs(dx)+Math.abs(dz))%3==0?ModBlocks.PRISMSTONE:ModBlocks.POLISHED_NULLSTONE);
                for(int dy=0;dy<=7;dy++) put(world,p.add(dx,dy,dz),Blocks.AIR);
            }
            for(int[] corner:new int[][]{{-4,-4},{4,-4},{-4,4},{4,4}}) {
                int height=4+context.getRandom().nextInt(3);
                for(int y=0;y<height;y++) put(world,p.add(corner[0],y,corner[1]),ModBlocks.NULLSTONE_BRICKS);
                put(world,p.add(corner[0],height,corner[1]),ModBlocks.PRISM_LAMP);
            }
            // A fractured arch frames the waystone without obstructing its landing pads.
            for(int dx=-4;dx<=4;dx++) if(dx!=1) put(world,p.add(dx,6,-4),ModBlocks.PRISMSTONE);
            put(world,p,ModBlocks.WAYSTONE);
            BlockPos cache=p.add(3,0,3);put(world,cache,Blocks.CHEST);
            LootableInventory.setLootTable(world,context.getRandom(),cache,
                    RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of(MyFirstMod.MOD_ID,"chests/waystone_cache")));
            return true;
        }
    }
}
