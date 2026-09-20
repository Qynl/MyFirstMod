package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.RealmFeatures;
import net.minecraft.block.*;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;
/** The Drowned Archive: a flooded reading hall whose Tide Bell drains it exactly once.
 * Walkway ring above the water, drowned desks and the vault below it. Chunk-contained. */
public final class ArchiveFeature extends Feature<DefaultFeatureConfig> {
    public ArchiveFeature(){super(DefaultFeatureConfig.CODEC);}
    public static void register(){Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","drowned_archive"),new ArchiveFeature());}
    private static void put(StructureWorldAccess w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),Block.NOTIFY_LISTENERS);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context){
        var w=context.getWorld();var random=context.getRandom();
        int cx=context.getOrigin().getX()&~15,cz=context.getOrigin().getZ()&~15;
        if(Math.abs(cx+8)<96&&cz+8>-96&&cz+8<192)return false;
        var c=new BlockPos(cx+8,RealmFeatures.surfaceHeight(w,cx+8,cz+8)-1,cz+8);
        if(!w.getBlockState(c).isOf(ModBlocks.BRINESILT))return false;
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
            var q=c.add(dx,0,dz);
            if(Math.abs(RealmFeatures.surfaceHeight(w,q.getX(),q.getZ())-c.getY())>1)return false;
            for(int y=1;y<=8;y++)if(!w.getBlockState(q.up(y)).isAir())return false;
        }
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)for(int y=1;y<=7;y++)put(w,c.add(dx,y,dz),Blocks.AIR);
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
            boolean edge=Math.abs(dx)==8||Math.abs(dz)==8;
            put(w,c.add(dx,0,dz),edge?ModBlocks.OXIDIZED_TRIM:((dx+dz)&3)==0?ModBlocks.BRINESILT:Blocks.STONE_BRICKS);
            for(int y=1;y<=7;y++){
                Block b=Blocks.AIR;
                if(edge)b=y>=3&&y<=5&&(Math.abs(dx)==8&&Math.abs(dz)%4<=1||Math.abs(dz)==8&&Math.abs(dx)%4<=1)?Blocks.BLUE_STAINED_GLASS:Blocks.DEEPSLATE_TILES;
                if(dz==8&&Math.abs(dx)<=1&&y<=4)b=Blocks.AIR;
                put(w,c.add(dx,y,dz),b);
            }
            int roof=8+(8-Math.abs(dx))/3;
            put(w,c.add(dx,roof,dz),dz%4==0?ModBlocks.OXIDIZED_TRIM:Blocks.OXIDIZED_COPPER);
        }
        for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)for(int y=1;y<=4;y++)put(w,c.add(dx,y,dz),Blocks.WATER);
        for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)
            if(Math.max(Math.abs(dx),Math.abs(dz))==6)put(w,c.add(dx,5,dz),Blocks.DEEPSLATE_TILE_SLAB);
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){
            put(w,c.add(dx,1,dz),Blocks.BOOKSHELF);
            put(w,c.add(dx,2,dz),Blocks.DARK_OAK_STAIRS);
        }
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int y=1;y<=5;y++)put(w,c.add(dx,y,dz),ModBlocks.OXIDIZED_TRIM);
        put(w,c.up(6),ModBlocks.TIDE_BELL);
        var vault=c.add(0,1,-4);put(w,vault,Blocks.CHEST);
        LootableInventory.setLootTable(w,random,vault,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/archive_cache")));
        spawner(w,c.add(-5,1,5),"drowned");spawner(w,c.add(5,1,-5),"rift_sentinel");
        put(w,c.add(0,1,7),ModBlocks.WAYSTONE);
        return true;
    }
    private static void spawner(StructureWorldAccess w,BlockPos pos,String actor){
        put(w,pos,Blocks.SPAWNER);
        NbtCompound nbt=new NbtCompound();
        nbt.putString("id","minecraft:mob_spawner");
        nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());
        var entity=new NbtCompound();entity.putString("id",actor.contains(":")?actor:"myfirstmod:"+actor);
        if(actor.equals("drowned"))entity.putString("id","minecraft:drowned");
        var data=new NbtCompound();data.put("entity",entity);nbt.put("SpawnData",data);
        nbt.putShort("Delay",(short)120);nbt.putShort("MinSpawnDelay",(short)300);nbt.putShort("MaxSpawnDelay",(short)700);
        nbt.putShort("SpawnCount",(short)1);nbt.putShort("SpawnRange",(short)4);nbt.putShort("MaxNearbyEntities",(short)2);nbt.putShort("RequiredPlayerRange",(short)10);
        if(w instanceof net.minecraft.server.world.ServerWorld live){
            if(live.getBlockEntity(pos) instanceof MobSpawnerBlockEntity blockEntity){blockEntity.getLogic().readNbt(live,pos,nbt);blockEntity.markDirty();}
        } else {
            w.getChunk(pos).addPendingBlockEntityNbt(nbt);
        }
    }
}
