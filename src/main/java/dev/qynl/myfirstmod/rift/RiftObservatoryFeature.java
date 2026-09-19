package dev.qynl.myfirstmod.rift;

import dev.qynl.myfirstmod.MyFirstMod;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.RealmFeatures;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public final class RiftObservatoryFeature extends Feature<DefaultFeatureConfig> {
    public RiftObservatoryFeature() {super(DefaultFeatureConfig.CODEC);}
    public static void register() {
        Registry.register(Registries.FEATURE,Identifier.of(MyFirstMod.MOD_ID,"rift_observatory"),new RiftObservatoryFeature());
    }
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        var world=context.getWorld();int x=(context.getOrigin().getX() & ~15)+8,z=(context.getOrigin().getZ() & ~15)+8;
        if(Math.abs(x)<96 && z>-96 && z<192) return false;
        var p=new BlockPos(x,RealmFeatures.surfaceHeight(world,x,z),z);
        if(p.getY()<20 || p.getY()>160) return false;
        var surface=world.getBlockState(p.down());
        if(!surface.isOf(ModBlocks.HUSHED_MOSS) && !surface.isOf(ModBlocks.LUMEN_MOSS)
                && !surface.isOf(ModBlocks.PRISMSTONE) && !surface.isOf(ModBlocks.CINDERSTONE)) return false;
        for(int dx=-6;dx<=6;dx++) for(int dz=-6;dz<=6;dz++) {
            for(int dy=-3;dy<0;dy++) world.setBlockState(p.add(dx,dy,dz),ModBlocks.NULLSTONE_BRICKS.getDefaultState(),Block.NOTIFY_LISTENERS);
            world.setBlockState(p.add(dx,-1,dz),((dx*dx+dz*dz)%7<2?ModBlocks.PRISMSTONE:ModBlocks.POLISHED_NULLSTONE).getDefaultState(),Block.NOTIFY_LISTENERS);
            for(int dy=0;dy<=13;dy++) world.setBlockState(p.add(dx,dy,dz),Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS);
        }
        for(int dx:new int[]{-5,5}) for(int dz:new int[]{-5,5}) {
            for(int y=0;y<10;y++) world.setBlockState(p.add(dx,y,dz),ModBlocks.NULLSTONE_BRICKS.getDefaultState(),Block.NOTIFY_LISTENERS);
            world.setBlockState(p.add(dx,10,dz),ModBlocks.PRISM_LAMP.getDefaultState(),Block.NOTIFY_LISTENERS);
        }
        for(int i=-5;i<=5;i++) {
            if(i==-1 || i==2) continue;
            world.setBlockState(p.add(i,9,-5),ModBlocks.PRISMSTONE.getDefaultState(),Block.NOTIFY_LISTENERS);
            world.setBlockState(p.add(-5,9,i),ModBlocks.PRISMSTONE.getDefaultState(),Block.NOTIFY_LISTENERS);
        }
        // Three ground-level seals mirror the encounter's visible capture circles.
        for(int[] offset:new int[][]{{-4,-3},{4,-3},{0,5}})
            world.setBlockState(p.add(offset[0],-1,offset[1]),ModBlocks.PRISM_LAMP.getDefaultState(),Block.NOTIFY_LISTENERS);
        world.setBlockState(p,ModBlocks.RIFT_ANCHOR.getDefaultState(),Block.NOTIFY_LISTENERS);
        return true;
    }
}
