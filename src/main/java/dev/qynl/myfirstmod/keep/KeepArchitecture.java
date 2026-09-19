package dev.qynl.myfirstmod.keep;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
/** A 63x63 fortress, constructed one chunk per server tick in a dedicated reserved dimension. */
public final class KeepArchitecture {
    public static final int[][] WARDS={{-20,0},{20,0},{0,-20}};
    public static void buildChunk(ServerWorld world,int cx,int cz) {
        world.getChunk(cx,cz);
        for(int x=cx*16;x<cx*16+16;x++) for(int z=cz*16;z<cz*16+16;z++) {
            if(Math.abs(x)>31 || Math.abs(z)>31) continue;
            for(int y=63;y<=88;y++) {
                Block b=Blocks.AIR;
                if(y==63) b=ModBlocks.NULLSTONE_BRICKS;
                if(y==64) b=(Math.abs(x)%8==0 || Math.abs(z)%8==0)?ModBlocks.PRISMSTONE:ModBlocks.POLISHED_NULLSTONE;
                if(Math.max(Math.abs(x),Math.abs(z))==31 && y<=72) b=Blocks.DEEPSLATE_BRICKS;
                if(Math.max(Math.abs(x),Math.abs(z))==31 && y==73 && (x+z)%2==0) b=Blocks.CHISELED_DEEPSLATE;
                // Great hall, with four wide doorways and high ribs.
                boolean edge=Math.max(Math.abs(x),Math.abs(z))==10;
                boolean door=(Math.abs(x)<=2 || Math.abs(z)<=2) && y<=69;
                if(edge && y>=65 && y<=77 && !door) b=y>=71 && (Math.abs(x)+Math.abs(z))%4<2?Blocks.PURPLE_STAINED_GLASS:Blocks.DEEPSLATE_TILES;
                if(y==78+(10-Math.abs(x))/3 && Math.abs(x)<=10 && Math.abs(z)<=10 && z%5==0) b=Blocks.CHISELED_DEEPSLATE;
                for(int[] c:WARDS) {
                    int dx=x-c[0],dz=z-c[1];
                    boolean wall=Math.max(Math.abs(dx),Math.abs(dz))==7;
                    if(wall && y>=65 && y<=72 && !((Math.abs(dx)<=1 || Math.abs(dz)<=1)&&y<=68)) b=Blocks.DEEPSLATE_BRICKS;
                    if(y==73 && Math.abs(dx)<=7 && Math.abs(dz)<=7 && (dx%4==0 || dz%4==0)) b=Blocks.POLISHED_BASALT;
                    if(Math.abs(dx)==5 && Math.abs(dz)==5 && y==65) b=Blocks.SOUL_LANTERN;
                    if(dx==0 && dz==0 && y==65) b=ModBlocks.KEEP_WARD;
                }
                if(Math.abs(x)>=27 && Math.abs(x)<=29 && Math.abs(z)>=27 && Math.abs(z)<=29 && y<=84) b=Blocks.DEEPSLATE_TILES;
                if(Math.abs(x)==28 && Math.abs(z)==28 && y==85) b=ModBlocks.PRISM_LAMP;
                if(x==0 && z==28 && y==65) b=ModBlocks.KEEP_GATE;
                if(x==4 && z==28 && y==65) b=ModBlocks.WAYSTONE;
                if(x==0 && z==22 && y==65) b=ModBlocks.KEEP_HEART;
                world.setBlockState(new BlockPos(x,y,z),b.getDefaultState(),Block.NOTIFY_LISTENERS);
            }
        }
    }
}
