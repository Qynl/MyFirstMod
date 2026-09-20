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
/** The Ashen Foundry: a forge hall behind a shell of standing fire. Channels of lava run
 * beside the walk; the vault sleeps inside the fire shell until a worthy pilgrim quenches.
 * Chunk-contained like every other generated hall. */
public final class FoundryFeature extends Feature<DefaultFeatureConfig> {
    public FoundryFeature(){super(DefaultFeatureConfig.CODEC);}
    public static void register(){Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","ashen_foundry"),new FoundryFeature());}
    private static void put(StructureWorldAccess w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),Block.NOTIFY_LISTENERS);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context){
        var w=context.getWorld();
        int cx=context.getOrigin().getX()&~15,cz=context.getOrigin().getZ()&~15;
        if(Math.abs(cx+8)<96&&cz+8>-96&&cz+8<192)return false;
        var c=new BlockPos(cx+8,RealmFeatures.surfaceHeight(w,cx+8,cz+8)-1,cz+8);
        if(!w.getBlockState(c).isOf(ModBlocks.CINDERSTONE))return false;
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++){
            var q=c.add(dx,0,dz);
            if(Math.abs(RealmFeatures.surfaceHeight(w,q.getX(),q.getZ())-c.getY())>1)return false;
            for(int y=1;y<=8;y++)if(!w.getBlockState(q.up(y)).isAir())return false;
        }
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++)for(int y=1;y<=7;y++)put(w,c.add(dx,y,dz),Blocks.AIR);
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++){
            boolean edge=Math.abs(dx)==9||Math.abs(dz)==9;
            put(w,c.add(dx,0,dz),(dx+dz)%5==0?Blocks.MAGMA_BLOCK:Blocks.BLACKSTONE);
            for(int y=1;y<=7;y++){
                Block b=Blocks.AIR;
                if(edge)b=y==2?Blocks.MAGMA_BLOCK:Blocks.POLISHED_BLACKSTONE_BRICKS;
                if(dz==9&&Math.abs(dx)<=1&&y<=4)b=Blocks.AIR;
                put(w,c.add(dx,y,dz),b);
            }
            put(w,c.add(dx,8+(9-Math.abs(dx))/3,dz),dz%4==0?Blocks.POLISHED_BLACKSTONE:Blocks.BLACKSTONE);
        }
        for(int row:new int[]{-FoundryRules.channelRow(),FoundryRules.channelRow()})
            for(int dx=-FoundryRules.channelHalfLength();dx<=FoundryRules.channelHalfLength();dx++)
                put(w,c.add(dx,1,row),Blocks.LAVA);
        for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)
            if(FoundryRules.inShell(dx,dz))for(int y=FoundryRules.shellFloorOffset();y<=FoundryRules.shellCeilOffset();y++)
                put(w,c.add(dx,y+4,dz),Blocks.LAVA);
        for(int y=1;y<=3;y++)put(w,c.up(y),Blocks.POLISHED_BLACKSTONE);
        put(w,c.up(4),ModBlocks.EMBER_CRUCIBLE);
        var vault=c.add(1,1,0);put(w,vault,Blocks.CHEST);
        LootableInventory.setLootTable(w,context.getRandom(),vault,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/foundry_cache")));
        for(int dx:new int[]{-5,5})for(int dz:new int[]{-5,5})put(w,c.add(dx,1,dz),Blocks.BLAST_FURNACE);
        put(w,c.add(6,1,0),Blocks.ANVIL);put(w,c.add(-6,1,0),Blocks.ANVIL);
        spawner(w,c.add(-6,1,6),"minecraft:magma_cube");spawner(w,c.add(6,1,-6),"myfirstmod:rift_sentinel");
        put(w,c.add(0,1,8),ModBlocks.WAYSTONE);
        return true;
    }
    private static void spawner(StructureWorldAccess w,BlockPos pos,String actor){
        put(w,pos,Blocks.SPAWNER);
        NbtCompound nbt=new NbtCompound();
        nbt.putString("id","minecraft:mob_spawner");
        nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());
        var entity=new NbtCompound();entity.putString("id",actor);
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
