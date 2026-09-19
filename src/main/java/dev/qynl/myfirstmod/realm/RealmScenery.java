package dev.qynl.myfirstmod.realm;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;
/** Low-scale biome identity between major landmarks. All writes remain in this chunk. */
public final class RealmScenery extends Feature<DefaultFeatureConfig> {
    public RealmScenery(){super(DefaultFeatureConfig.CODEC);}
    public static void register(){Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","realm_scenery"),new RealmScenery());}
    private static boolean natural(BlockState state){return state.isOf(ModBlocks.HUSHED_MOSS)||state.isOf(ModBlocks.LUMEN_MOSS)||state.isOf(ModBlocks.PRISMSTONE)||state.isOf(ModBlocks.CINDERSTONE);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        var w=c.getWorld();var random=c.getRandom();int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        if(Math.abs(cx+8)<96&&cz+8>-96&&cz+8<192)return false;
        // Existing landmark roofs/cores occupy the chunk center. Never decorate through them.
        var center=new BlockPos(cx+8,RealmFeatures.surfaceHeight(w,cx+8,cz+8)-1,cz+8);
        if(!natural(w.getBlockState(center)))return false;
        for(int n=0;n<9;n++){
            int x=cx+2+random.nextInt(12),z=cz+2+random.nextInt(12);var p=new BlockPos(x,RealmFeatures.surfaceHeight(w,x,z),z);
            if(p.getY()<20||p.getY()>190||!w.getBlockState(p).isAir())continue;
            var floor=w.getBlockState(p.down());if(!natural(floor))continue;
            Block decoration;
            if(floor.isOf(ModBlocks.HUSHED_MOSS))decoration=n%4==0?ModBlocks.HUSHWOOD:n%3==0?Blocks.BROWN_MUSHROOM:Blocks.MOSS_CARPET;
            else if(floor.isOf(ModBlocks.LUMEN_MOSS))decoration=n%3==0?Blocks.MOSS_BLOCK:Blocks.MOSS_CARPET;
            else if(floor.isOf(ModBlocks.PRISMSTONE))decoration=n%3==0?Blocks.AMETHYST_CLUSTER:Blocks.SMALL_AMETHYST_BUD;
            else decoration=n%3==0?Blocks.POLISHED_BASALT:Blocks.BLACKSTONE_SLAB;
            if(floor.isOf(ModBlocks.LUMEN_MOSS)&&n==0&&pond(w,p))continue;
            w.setBlockState(p,decoration.getDefaultState(),Block.NOTIFY_LISTENERS);

        }
        return true;
    }
    private static boolean pond(net.minecraft.world.StructureWorldAccess w,BlockPos p){
        // Only flat natural ground, with an intact rim and no buried structures.
        for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            var q=p.add(dx,0,dz);var under=w.getBlockState(q.down(2));
            if(!w.getBlockState(q).isAir()||!natural(w.getBlockState(q.down()))
                ||!(natural(under)||under.isOf(ModBlocks.NULLSTONE)||under.isOf(ModBlocks.POLISHED_NULLSTONE)))return false;
        }
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            w.setBlockState(p.add(dx,-2,dz),ModBlocks.LUMEN_MOSS.getDefaultState(),Block.NOTIFY_LISTENERS);
            w.setBlockState(p.add(dx,-1,dz),Blocks.WATER.getDefaultState(),Block.NOTIFY_LISTENERS);
        }
        w.setBlockState(p,Blocks.LILY_PAD.getDefaultState(),Block.NOTIFY_LISTENERS);return true;
    }
}
