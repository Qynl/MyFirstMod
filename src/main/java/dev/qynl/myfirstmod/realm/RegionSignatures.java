package dev.qynl.myfirstmod.realm;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;
/** One authored silhouette per young biome: drowned colonnades, floating veil islands,
 * crystal spires and smoking vents. Every read and write stays inside the source chunk. */
public final class RegionSignatures extends Feature<DefaultFeatureConfig> {
    public RegionSignatures(){super(DefaultFeatureConfig.CODEC);}
    public static void register(){Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","region_signatures"),new RegionSignatures());}
    private static boolean natural(BlockState s){return s.isOf(ModBlocks.BRINESILT)||s.isOf(ModBlocks.VEILSTONE)||s.isOf(ModBlocks.VENT_BASALT)||s.isOf(ModBlocks.PRISMSTONE)||s.isOf(ModBlocks.CINDERSTONE);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
        var w=c.getWorld();var random=c.getRandom();int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        if(Math.abs(cx+8)<96&&cz+8>-96&&cz+8<192)return false;
        var floor=new BlockPos(cx+8,RealmFeatures.surfaceHeight(w,cx+8,cz+8)-1,cz+8);
        var state=w.getBlockState(floor);if(!natural(state))return false;
        var surface=floor.up();
        if(state.isOf(ModBlocks.BRINESILT))colonnade(w,surface,random);
        else if(state.isOf(ModBlocks.VEILSTONE))island(w,surface,random);
        else if(state.isOf(ModBlocks.VENT_BASALT))vents(w,surface,random);
        else if(state.isOf(ModBlocks.CINDERSTONE))chimney(w,surface,random);
        else spire(w,surface,random);
        return true;
    }
    private static void put(StructureWorldAccess w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),Block.NOTIFY_LISTENERS);}
    /** Flat, clear, natural-floored ground of the given half-width around a surface position. */
    private static boolean flat(StructureWorldAccess w,BlockPos surface,int r){
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            var q=surface.add(dx,0,dz);
            if(Math.abs(RealmFeatures.surfaceHeight(w,q.getX(),q.getZ())-surface.getY())>1)return false;
            if(!w.getBlockState(q).isAir()||!natural(w.getBlockState(q.down())))return false;
        }
        return true;
    }
    private static BlockPos at(StructureWorldAccess w,BlockPos surface,int dx,int dz){
        var q=surface.add(dx,0,dz);return q.up(RealmFeatures.surfaceHeight(w,q.getX(),q.getZ())-surface.getY());
    }
    private static void colonnade(StructureWorldAccess w,BlockPos s,Random random){
        for(int n=0;n<2;n++){
            var p=at(w,s,-4+random.nextInt(9),-4+random.nextInt(9));
            int height=4+random.nextInt(5);
            for(int y=0;y<height;y++)put(w,p.up(y),y==height-1?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_TILES);
        }
        var walk=at(w,s,-5,2+random.nextInt(3));
        for(int i=0;i<7;i++){
            var q=walk.east(i);put(w,q,Blocks.POLISHED_BLACKSTONE_SLAB);
            if(i%3==0)put(w,q.up(1),Blocks.SEA_LANTERN);
        }
        if(flat(w,s,2))for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            boolean rim=Math.max(Math.abs(dx),Math.abs(dz))==2;
            if(!rim)put(w,s.add(dx,-1,dz),ModBlocks.BRINESILT);
            put(w,s.add(dx,0,dz),rim?ModBlocks.BRINESILT:Blocks.WATER);
        }
    }
    private static void island(StructureWorldAccess w,BlockPos s,Random random){
        int r=4+random.nextInt(3),y=24;
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            int d=dx*dx+dz*dz;if(d>r*r)continue;
            put(w,s.add(dx,y,dz),ModBlocks.VEILSTONE);
            put(w,s.add(dx,y-1,dz),ModBlocks.NULLSTONE);
            if(d<=(r-1)*(r-1))put(w,s.add(dx,y-2,dz),ModBlocks.NULLSTONE);
            if(d>(r-2)*(r-2))put(w,s.add(dx,y-3,dz),Blocks.HANGING_ROOTS);
        }
        for(int t=1;t<=3;t++)put(w,s.add(0,y+t,0),ModBlocks.HUSHWOOD);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int t=4;t<=5;t++)put(w,s.add(dx,y+t,dz),ModBlocks.HUSH_LEAVES);
        put(w,s.add(r-1,y+1,0),ModBlocks.PRISM_LAMP);
    }
    private static void spire(StructureWorldAccess w,BlockPos s,Random random){
        int height=8+random.nextInt(9);
        for(int y=0;y<height;y++)put(w,s.up(y),y>height-3?ModBlocks.PRISMSTONE:ModBlocks.SPIRE_CRYSTAL);
        put(w,s.up(height),Blocks.AMETHYST_CLUSTER);
        for(int n=0;n<3;n++)put(w,s.up(3+random.nextInt(Math.max(1,height-4))).north(n%2==0?1:-1),Blocks.SMALL_AMETHYST_BUD);
    }
    private static void chimney(StructureWorldAccess w,BlockPos s,Random random){
        for(int y=0;y<6;y++)put(w,s.up(y),Blocks.POLISHED_BASALT);
        put(w,s.up(6),Blocks.CAMPFIRE);
        for(var d:new BlockPos[]{new BlockPos(2,0,0),new BlockPos(-2,0,0),new BlockPos(0,0,2),new BlockPos(0,0,-2)})put(w,s.add(d),Blocks.BLACKSTONE);
    }
    private static void vents(StructureWorldAccess w,BlockPos s,Random random){
        for(int n=0;n<3;n++){
            var p=at(w,s,-5+random.nextInt(11),-5+random.nextInt(11));
            int height=3+random.nextInt(4);
            for(int y=0;y<height;y++)put(w,p.up(y),Blocks.POLISHED_BASALT);
        }
        if(flat(w,s,2))for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
            int d=Math.max(Math.abs(dx),Math.abs(dz));
            if(d<=1)put(w,s.add(dx,0,dz),Blocks.LAVA);
            else if(d==2)put(w,s.add(dx,0,dz),Blocks.MAGMA_BLOCK);
        }
        put(w,s.add(2,1,0),Blocks.SOUL_FIRE);
    }
}
