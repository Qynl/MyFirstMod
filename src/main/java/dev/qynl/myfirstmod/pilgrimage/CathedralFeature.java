package dev.qynl.myfirstmod.pilgrimage;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.RealmFeatures;
import net.minecraft.block.*;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** A vaulted nave above two navigable crypt floors. Every write stays in the source chunk. */
public final class CathedralFeature extends Feature<DefaultFeatureConfig> {
    public CathedralFeature() {super(DefaultFeatureConfig.CODEC);}
    public static void register() {Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","mourning_cathedral"),new CathedralFeature());}
    private static void put(StructureWorldAccess w,BlockPos p,Block b) {w.setBlockState(p,b.getDefaultState(),Block.NOTIFY_LISTENERS);}
    @Override public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        var w=context.getWorld();int x=(context.getOrigin().getX()&~15)+8,z=(context.getOrigin().getZ()&~15)+8;
        if(Math.abs(x)<96 && z>-96 && z<192) return false;
        var p=new BlockPos(x,RealmFeatures.surfaceHeight(w,x,z),z);if(p.getY()<30 || p.getY()>160) return false;
        var surface=w.getBlockState(p.down());
        if(!surface.isOf(ModBlocks.HUSHED_MOSS) && !surface.isOf(ModBlocks.LUMEN_MOSS)
                && !surface.isOf(ModBlocks.CINDERSTONE) && !surface.isOf(ModBlocks.PRISMSTONE)) return false;
        for(int dx=-7;dx<=7;dx++) for(int dz=-7;dz<=7;dz++) for(int y=-18;y<=19;y++) {
            boolean edge=Math.abs(dx)==7 || Math.abs(dz)==7;
            boolean floor=y==-18 || y==-17 || y==-9 || y==-1;
            Block b=floor?ModBlocks.NULLSTONE_BRICKS:y<0&&edge?Blocks.DEEPSLATE_BRICKS:Blocks.AIR;
            if(y>=0 && y<10 && edge) b=Blocks.DEEPSLATE_TILES;
            if(y>=0 && y<4 && Math.abs(dx)<=1 && dz==7) b=Blocks.AIR;
            // Tall pointed windows, warm glass against cold masonry.
            if(y>=3 && y<=7 && (Math.abs(dx)==7 && Math.abs(dz)%4<=1 || dz==-7 && Math.abs(dx)<=2))
                b=y==7 && Math.abs(dz)%4==1?Blocks.DEEPSLATE_TILES:Blocks.PURPLE_STAINED_GLASS;
            put(w,p.add(dx,y,dz),b);
        }
        // Sloped, ribbed vault and four exterior pinnacles, not a flat-box roof.
        for(int dx=-7;dx<=7;dx++) for(int dz=-7;dz<=7;dz++) {
            int roof=10+(7-Math.abs(dx))/2;
            put(w,p.add(dx,roof,dz),dz%4==0?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_TILES);
            if(dz%4==0 && Math.abs(dx)<7) put(w,p.add(dx,roof-1,dz),Blocks.POLISHED_BASALT);
        }
        for(int dx:new int[]{-6,6}) for(int dz:new int[]{-6,6}) {
            for(int y=0;y<=15;y++) put(w,p.add(dx,y,dz),y%4==0?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_BRICKS);
            put(w,p.add(dx,16,dz),Blocks.POLISHED_BASALT);put(w,p.add(dx,17,dz),Blocks.SOUL_LANTERN);
        }
        for(int dz:new int[]{-2,2}) for(int dx:new int[]{-3,3}) {
            for(int y=0;y<8;y++) put(w,p.add(dx,y,dz),Blocks.POLISHED_BASALT);
            put(w,p.add(dx,8,dz),Blocks.SOUL_LANTERN);
        }
        // Nave benches and sarcophagi below. Keep the center and stair aisles open.
        for(int dz:new int[]{-1,3}) for(int dx:new int[]{-2,2}) put(w,p.add(dx,0,dz),Blocks.DARK_OAK_STAIRS);
        for(int y:new int[]{-8,-16}) {
            for(int dx:new int[]{-2,2}) for(int dz:new int[]{-3,3}) {
                put(w,p.add(dx,y,dz),Blocks.CHISELED_DEEPSLATE);
                put(w,p.add(dx,y+1,dz),Blocks.DEEPSLATE_TILE_SLAB);
            }
            for(int dx:new int[]{-6,6}) {put(w,p.add(dx,y+3,0),Blocks.CHAIN);put(w,p.add(dx,y+2,0),Blocks.SOUL_LANTERN);}
        }
        // Opposite stair flights: 8 drops each, three clear blocks above every tread.
        for(int step=0;step<8;step++) for(int lane=0;lane<2;lane++) {
            stair(w,p.add(-5+lane,-1-step,-5+step),Direction.NORTH);
            stair(w,p.add(4+lane,-9-step,2-step),Direction.SOUTH);
        }
        for(int[] seal:CathedralRite.SEALS) put(w,p.add(seal[0],seal[1]-16,seal[2]),ModBlocks.FUNERARY_SEAL);
        put(w,p.down(16),ModBlocks.MOURNING_RELIQUARY);
        for(int y:new int[]{-8,-16}) {
            var spawner=p.add(0,y,-5);put(w,spawner,Blocks.SPAWNER);
            var nbt=spawnerData(spawner,y==-8?"rift_sentinel":"shardstalker");
            if(w instanceof net.minecraft.server.world.ServerWorld live) {
                if(live.getBlockEntity(spawner) instanceof MobSpawnerBlockEntity entity) {
                    entity.getLogic().readNbt(live,spawner,nbt);entity.markDirty();
                }
            } else {
                // Never call a live-world block-entity listener from a generation worker:
                // it can request this unfinished chunk and deadlock generation.
                w.getChunk(spawner).addPendingBlockEntityNbt(nbt);
            }
            var chest=p.add(-2,y,5);put(w,chest,Blocks.CHEST);
            LootableInventory.setLootTable(w,context.getRandom(),chest,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/pilgrim_cache")));
        }
        return true;
    }
    private static net.minecraft.nbt.NbtCompound spawnerData(BlockPos pos,String actor) {
        var nbt=new net.minecraft.nbt.NbtCompound();
        nbt.putString("id","minecraft:mob_spawner");nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());
        var entity=new net.minecraft.nbt.NbtCompound();entity.putString("id","myfirstmod:"+actor);
        var data=new net.minecraft.nbt.NbtCompound();data.put("entity",entity);nbt.put("SpawnData",data);
        nbt.putShort("Delay",(short)100);nbt.putShort("MinSpawnDelay",(short)300);nbt.putShort("MaxSpawnDelay",(short)600);
        nbt.putShort("SpawnCount",(short)2);nbt.putShort("MaxNearbyEntities",(short)4);
        nbt.putShort("RequiredPlayerRange",(short)10);nbt.putShort("SpawnRange",(short)3);
        return nbt;
    }
    private static void stair(StructureWorldAccess w,BlockPos p,Direction direction) {
        for(int h=1;h<=3;h++) put(w,p.up(h),Blocks.AIR);
        w.setBlockState(p,Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState().with(StairsBlock.FACING,direction),Block.NOTIFY_LISTENERS);
    }
}
