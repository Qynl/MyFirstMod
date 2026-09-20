package dev.qynl.myfirstmod.realm;
import dev.qynl.myfirstmod.block.ModBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.util.FeatureContext;
/** Eight authored landmark families, one per region mood: watchtower, brine chapel,
 * geode garden, slag camp, caravan wreck, echo fissure, heartwood circle, fen shrine.
 * Every landmark is chunk-contained and never starts inside the sanctuary approach. */
public final class KingdomLandmarks {
    private KingdomLandmarks(){}
    public static void register(){
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","veil_watchtower"),new VeilWatchtower());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","brine_chapel"),new BrineChapel());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","geode_garden"),new GeodeGarden());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","slag_camp"),new SlagCamp());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","caravan_wreck"),new CaravanWreck());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","echo_fissure"),new EchoFissure());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","heartwood_circle"),new HeartwoodCircle());
        Registry.register(Registries.FEATURE,Identifier.of("myfirstmod","fen_shrine"),new FenShrine());
    }
    static boolean reserved(int cx,int cz){return Math.abs(cx+8)<96&&cz+8>-96&&cz+8<192;}
    static void put(StructureWorldAccess w,BlockPos p,Block b){w.setBlockState(p,b.getDefaultState(),Block.NOTIFY_LISTENERS);}
    static boolean flat(StructureWorldAccess w,BlockPos surface,int r){
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            var q=surface.add(dx,0,dz);
            if(Math.abs(RealmFeatures.surfaceHeight(w,q.getX(),q.getZ())-surface.getY())>1)return false;
            if(!w.getBlockState(q).isAir())return false;
        }
        return true;
    }
    static void chest(FeatureContext<DefaultFeatureConfig> c,StructureWorldAccess w,BlockPos pos,String table){
        put(w,pos,Blocks.CHEST);
        LootableInventory.setLootTable(w,c.getRandom(),pos,RegistryKey.of(RegistryKeys.LOOT_TABLE,Identifier.of("myfirstmod","chests/"+table)));
    }
    static void spawner(StructureWorldAccess w,BlockPos pos,String actor){spawner(w,pos,actor,null);}
    static void spawner(StructureWorldAccess w,BlockPos pos,String actor,NbtCompound elite){
        put(w,pos,Blocks.SPAWNER);
        NbtCompound nbt=new NbtCompound();
        nbt.putString("id","minecraft:mob_spawner");
        nbt.putInt("x",pos.getX());nbt.putInt("y",pos.getY());nbt.putInt("z",pos.getZ());
        var entity=new NbtCompound();entity.putString("id",actor);
        if(elite!=null)for(var key:elite.getKeys())entity.put(key,elite.get(key));
        var data=new NbtCompound();data.put("entity",entity);nbt.put("SpawnData",data);
        nbt.putShort("Delay",(short)140);nbt.putShort("MinSpawnDelay",(short)320);nbt.putShort("MaxSpawnDelay",(short)720);
        nbt.putShort("SpawnCount",(short)1);nbt.putShort("SpawnRange",(short)3);nbt.putShort("MaxNearbyEntities",(short)2);nbt.putShort("RequiredPlayerRange",(short)10);
        if(w instanceof net.minecraft.server.world.ServerWorld live){
            if(live.getBlockEntity(pos) instanceof MobSpawnerBlockEntity blockEntity){blockEntity.getLogic().readNbt(live,pos,nbt);blockEntity.markDirty();}
        } else {
            w.getChunk(pos).addPendingBlockEntityNbt(nbt);
        }
    }
    /** Named elites: a trident-throwing keeper and a hardened warden. Souls-like pressure, small arenas. */
    static NbtCompound brineKeeper(){
        var elite=new NbtCompound();
        elite.putString("CustomName","{\"text\":\"The Brine Keeper\",\"color\":\"aqua\",\"italic\":false}");
        elite.putByte("CustomNameVisible",(byte)1);
        var hands=new NbtList();
        var trident=new NbtCompound();trident.putString("id","minecraft:trident");trident.putInt("count",1);
        hands.add(trident);hands.add(new NbtCompound());
        elite.put("hand_items",hands);
        var chances=new NbtList();chances.add(NbtFloat.of(0.085f));chances.add(NbtFloat.of(0f));
        elite.put("hand_drop_chances",chances);
        return elite;
    }
    static NbtCompound fissureWarden(){
        var elite=new NbtCompound();
        elite.putString("CustomName","{\"text\":\"Warden of the Fissure\",\"color\":\"red\",\"italic\":false}");
        elite.putByte("CustomNameVisible",(byte)1);
        var effects=new NbtList();
        for(String[] e:new String[][]{{"minecraft:resistance","0"},{"minecraft:speed","0"}}){
            var effect=new NbtCompound();effect.putString("id",e[0]);
            effect.putByte("amplifier",(byte)Integer.parseInt(e[1]));
            effect.putInt("duration",2400);effect.putBoolean("show_particles",false);
            effects.add(effect);
        }
        elite.put("active_effects",effects);
        return elite;
    }
    static BlockPos origin(FeatureContext<DefaultFeatureConfig> c){
        int cx=c.getOrigin().getX()&~15,cz=c.getOrigin().getZ()&~15;
        return new BlockPos(cx+8,RealmFeatures.surfaceHeight(w(c),cx+8,cz+8)-1,cz+8);
    }
    static StructureWorldAccess w(FeatureContext<DefaultFeatureConfig> c){return c.getWorld();}

    /** A tall lookout over the floating islands: stair, beacon lamp, cache at the top. */
    public static final class VeilWatchtower extends Feature<DefaultFeatureConfig> {
        public VeilWatchtower(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!w.getBlockState(o).isOf(ModBlocks.VEILSTONE)||!flat(w,o.up(),2))return false;
            var s=o.up();
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){
                boolean ring=Math.max(Math.abs(dx),Math.abs(dz))==2;
                for(int y=0;y<9;y++){
                    boolean door=dz==2&&dx==0&&y<=1;
                    put(w,s.add(dx,y,dz),ring&&!door?ModBlocks.NULLSTONE_BRICKS:Blocks.AIR);
                }
            }
            put(w,s.add(1,2,1),Blocks.STONE_BRICK_STAIRS);put(w,s.add(-1,4,1),Blocks.STONE_BRICK_STAIRS);
            put(w,s.add(-1,6,-1),Blocks.STONE_BRICK_STAIRS);put(w,s.add(1,8,-1),Blocks.STONE_BRICK_STAIRS);
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)put(w,s.add(dx,9,dz),ModBlocks.VEILSTONE);
            put(w,s.add(0,10,0),ModBlocks.PRISM_LAMP);
            chest(c,w,s.add(0,10,-1),"watch_cache");
            return true;
        }
    }
    /** A flooded roadside chapel: water nave, oxidized altar, drowned keeper. */
    public static final class BrineChapel extends Feature<DefaultFeatureConfig> {
        public BrineChapel(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!w.getBlockState(o).isOf(ModBlocks.BRINESILT)||!flat(w,o.up(),4))return false;
            var s=o.up();
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
                boolean edge=Math.abs(dx)==4||Math.abs(dz)==4;
                for(int y=0;y<4;y++){
                    Block b=Blocks.AIR;
                    if(edge)b=(y==2&&(dx==0&&Math.abs(dz)==4||dz==0&&Math.abs(dx)==4))?Blocks.BLUE_STAINED_GLASS:ModBlocks.OXIDIZED_TRIM;
                    put(w,s.add(dx,y,dz),b);
                }
            }
            for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int y=0;y<2;y++)put(w,s.add(dx,y,dz),Blocks.WATER);
            for(int y=0;y<2;y++)put(w,s.add(0,y,-2),ModBlocks.OXIDIZED_TRIM);
            put(w,s.add(0,2,-2),ModBlocks.PRISM_LAMP);
            chest(c,w,s.add(2,0,2),"chapel_cache");
            spawner(w,s.add(-3,0,-3),"minecraft:drowned");
            return true;
        }
    }
    /** A burst geode: crystal ring, amethyst heart, resonite floor, half-buried cache. */
    public static final class GeodeGarden extends Feature<DefaultFeatureConfig> {
        public GeodeGarden(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!w.getBlockState(o).isOf(ModBlocks.PRISMSTONE)||!flat(w,o.up(),4))return false;
            var s=o.up();
            for(int[] d:new int[][]{{3,0},{2,2},{0,3},{-2,2},{-3,0},{-2,-2},{0,-3},{2,-2}})
                for(int y=0;y<4;y++)put(w,s.add(d[0],y,d[1]),ModBlocks.SPIRE_CRYSTAL);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)put(w,s.add(dx,-1,dz),ModBlocks.RESONITE_ORE);
            put(w,s,Blocks.AMETHYST_CLUSTER);
            chest(c,w,s.add(4,0,0),"geode_cache");
            return true;
        }
    }
    /** A cinder pilgrim camp: fire, seats, lean-to and supply barrels. */
    public static final class SlagCamp extends Feature<DefaultFeatureConfig> {
        public SlagCamp(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            var floor=w.getBlockState(o);
            if(reserved(o.getX(),o.getZ())||!(floor.isOf(ModBlocks.CINDERSTONE)||floor.isOf(ModBlocks.VENT_BASALT))||!flat(w,o.up(),3))return false;
            var s=o.up();
            put(w,s,Blocks.CAMPFIRE);
            for(int[] d:new int[][]{{1,1},{-1,1},{-1,-1},{1,-1}})put(w,s.add(d[0],0,d[1]),Blocks.BLACKSTONE);
            for(int dx=-2;dx<=2;dx++)for(int y=0;y<3;y++)put(w,s.add(dx,y,-2),ModBlocks.HUSH_PLANKS);
            for(int dx=-2;dx<=2;dx++)put(w,s.add(dx,3,-2),Blocks.WARPED_SLAB);
            chest(c,w,s.add(-2,0,0),"camp_cache");
            chest(c,w,s.add(2,0,1),"camp_cache");
            return true;
        }
    }
    /** A broken caravan: plank deck, wood wheels, two spilled caches. */
    public static final class CaravanWreck extends Feature<DefaultFeatureConfig> {
        public CaravanWreck(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!RealmScenery.natural(w.getBlockState(o))||!flat(w,o.up(),4))return false;
            var s=o.up();
            for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)put(w,s.add(dx,0,dz),ModBlocks.HUSH_PLANKS);
            put(w,s.add(2,-1,2),ModBlocks.HUSHWOOD);put(w,s.add(-2,-1,-2),ModBlocks.HUSHWOOD);
            for(int dx=-2;dx<=2;dx+=2)for(int dz=-1;dz<=1;dz+=2)put(w,s.add(dx,1,dz),ModBlocks.HUSHWOOD);
            chest(c,w,s.add(-1,1,0),"caravan_cache");
            chest(c,w,s.add(1,1,1),"caravan_cache");
            put(w,s.add(4,0,2),Blocks.WARPED_SLAB);put(w,s.add(-4,0,-2),Blocks.WARPED_SLAB);
            return true;
        }
    }
    /** A rift scar: resonite-lined trench, echo cache and a sentinel in the dark. */
    public static final class EchoFissure extends Feature<DefaultFeatureConfig> {
        public EchoFissure(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!RealmScenery.natural(w.getBlockState(o))||!flat(w,o.up(),3))return false;
            var s=o.up();
            for(int dx=-1;dx<=1;dx++)for(int dz=-3;dz<=3;dz++)for(int y=-3;y<=-1;y++){
                boolean wall=Math.abs(dx)==1||Math.abs(dz)==3;
                put(w,s.add(dx,y,dz),y==-3||wall?ModBlocks.RESONITE_ORE:Blocks.AIR);
            }
            put(w,s.add(1,0,0),Blocks.SOUL_FIRE);put(w,s.add(-1,0,0),Blocks.SOUL_FIRE);
            chest(c,w,s.add(0,-2,0),"fissure_cache");
            spawner(w,s.add(0,-2,2),"myfirstmod:rift_sentinel");
            return true;
        }
    }
    /** An old grove ring: eight trunks, leaf lintels, a covered cache at the heart. */
    public static final class HeartwoodCircle extends Feature<DefaultFeatureConfig> {
        public HeartwoodCircle(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!w.getBlockState(o).isOf(ModBlocks.HUSHED_MOSS)||!flat(w,o.up(),3))return false;
            var s=o.up();
            for(int[] d:new int[][]{{3,0},{2,2},{0,3},{-2,2},{-3,0},{-2,-2},{0,-3},{2,-2}}){
                for(int y=0;y<4;y++)put(w,s.add(d[0],y,d[1]),ModBlocks.HUSHWOOD);
                put(w,s.add(d[0],4,d[1]),ModBlocks.HUSH_LEAVES);
            }
            chest(c,w,s,"circle_cache");
            put(w,s.add(0,1,0),ModBlocks.HUSH_LEAVES);
            return true;
        }
    }
    /** A fen light-shrine: lamp corners, moss altar, glass bowl and a spore crown. */
    public static final class FenShrine extends Feature<DefaultFeatureConfig> {
        public FenShrine(){super(DefaultFeatureConfig.CODEC);}
        @Override public boolean generate(FeatureContext<DefaultFeatureConfig> c){
            var w=w(c);var o=origin(c);
            if(reserved(o.getX(),o.getZ())||!w.getBlockState(o).isOf(ModBlocks.LUMEN_MOSS)||!flat(w,o.up(),2))return false;
            var s=o.up();
            for(int[] d:new int[][]{{2,2},{-2,2},{-2,-2},{2,-2}})put(w,s.add(d[0],1,d[1]),ModBlocks.PRISM_LAMP);
            put(w,s,ModBlocks.LUMEN_MOSS);put(w,s.add(0,1,0),Blocks.GLASS);
            put(w,s.add(0,2,0),Blocks.SPORE_BLOSSOM);
            chest(c,w,s.add(0,0,3),"shrine_cache");
            return true;
        }
    }
}
