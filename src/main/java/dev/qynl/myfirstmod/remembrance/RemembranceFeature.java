package dev.qynl.myfirstmod.remembrance;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.RealmFeatures;
import net.minecraft.block.*;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Three silhouettes with a shared remembrance interaction; chunk-contained, no live-world calls. */
public final class RemembranceFeature extends Feature<DefaultFeatureConfig> {
    public RemembranceFeature() {super(DefaultFeatureConfig.CODEC);}
    public static void register() {Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","forgotten_memorial"),new RemembranceFeature());}
    private static void put(StructureWorldAccess w,BlockPos p,Block block) {w.setBlockState(p,block.getDefaultState(),Block.NOTIFY_LISTENERS);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        var w=context.getWorld();int x=(context.getOrigin().getX()&~15)+8,z=(context.getOrigin().getZ()&~15)+8;
        if(Math.abs(x)<96 && z>-96 && z<192) return false;
        var p=new BlockPos(x,RealmFeatures.surfaceHeight(w,x,z),z);if(p.getY()<24 || p.getY()>150) return false;
        var floor=w.getBlockState(p.down());
        if(!floor.isOf(ModBlocks.HUSHED_MOSS) && !floor.isOf(ModBlocks.PRISMSTONE) && !floor.isOf(ModBlocks.CINDERSTONE) && !floor.isOf(ModBlocks.LUMEN_MOSS)) return false;
        for(int dx=-5;dx<=5;dx++) for(int dz=-5;dz<=5;dz++) {
            for(int y=-3;y<0;y++) put(w,p.add(dx,y,dz),ModBlocks.NULLSTONE_BRICKS);
            for(int y=0;y<12;y++) put(w,p.add(dx,y,dz),Blocks.AIR);
            put(w,p.add(dx,-1,dz),(Math.abs(dx)+Math.abs(dz))%3==0?ModBlocks.PRISMSTONE:ModBlocks.POLISHED_NULLSTONE);
        }
        int style=context.getRandom().nextInt(3);
        if(style==0) {
            // A broken bell tower, open at ground level.
            for(int dx:new int[]{-3,3}) for(int dz:new int[]{-3,3}) for(int y=0;y<9;y++) put(w,p.add(dx,y,dz),Blocks.DEEPSLATE_BRICKS);
            for(int d=-3;d<=3;d++) {put(w,p.add(d,9,-3),Blocks.CHISELED_DEEPSLATE);put(w,p.add(d,9,3),Blocks.CHISELED_DEEPSLATE);put(w,p.add(-3,9,d),Blocks.CHISELED_DEEPSLATE);}
            for(int y=6;y<=9;y++) put(w,p.up(y),Blocks.CHAIN);
            put(w,p.up(5),ModBlocks.PRISM_LAMP);
        } else if(style==1) {
            // A roofed pilgrim shelter and two benches.
            for(int dx:new int[]{-4,4}) for(int dz:new int[]{-3,3}) for(int y=0;y<4;y++) put(w,p.add(dx,y,dz),ModBlocks.HUSHWOOD);
            for(int dx=-4;dx<=4;dx++) for(int dz=-3;dz<=3;dz++) put(w,p.add(dx,4+(4-Math.abs(dx))/2,dz),ModBlocks.HUSH_PLANKS);
            for(int dz=-2;dz<=2;dz++) {put(w,p.add(-3,0,dz),Blocks.DARK_OAK_STAIRS);put(w,p.add(3,0,dz),Blocks.DARK_OAK_STAIRS);}
            put(w,p.add(0,3,-2),Blocks.SOUL_LANTERN);
        } else {
            // An open grave garden, its crosses facing the central memorial.
            for(int dx:new int[]{-3,3}) for(int dz:new int[]{-3,3}) {
                for(int y=0;y<4;y++) put(w,p.add(dx,y,dz),Blocks.POLISHED_BASALT);
                put(w,p.add(dx-1,2,dz),Blocks.CHISELED_DEEPSLATE);put(w,p.add(dx+1,2,dz),Blocks.CHISELED_DEEPSLATE);
                put(w,p.add(dx,0,dz+1),Blocks.DEEPSLATE_TILE_SLAB);
            }
        }
        put(w,p,ModBlocks.MEMORY_STELE);
        var chest=p.add(2,0,4);put(w,chest,Blocks.CHEST);
        LootableInventory.setLootTable(w,context.getRandom(),chest,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/memorial_cache")));
        return true;
    }
}
