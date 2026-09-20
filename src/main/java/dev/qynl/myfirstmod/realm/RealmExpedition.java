package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.item.ArenaCompassItem;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class RealmExpedition {
    public static final BlockPos ARRIVAL=new BlockPos(0,81,160);
    /** Fresh worlds arrive in front of the realm-side counterpart of the Ancient City gateway. */
    public static final BlockPos THRESHOLD=new BlockPos(0,81,172);
    public static void prepare(ServerWorld world) {
        RealmState state=RealmState.get(world);
        if(state.sanctuaryBuilt) {prepareAltar(world,state);prepareForge(world,state);return;}
        NullWardenManager.prepareArena(world);
        // Lit, railed causeway: entry is always safe regardless of the seed's local terrain.
        for(int z=23;z<=170;z++) for(int x=-3;x<=3;x++) {
            put(world,new BlockPos(x,80,z),Math.abs(x)==3?Blocks.CHISELED_DEEPSLATE:Blocks.DEEPSLATE_TILES);
            for(int y=81;y<=86;y++) put(world,new BlockPos(x,y,z),Blocks.AIR);
            if(Math.abs(x)==3 && z%12!=0) put(world,new BlockPos(x,81,z),Blocks.DEEPSLATE_BRICK_WALL);
            if(Math.abs(x)==3 && z%12==0) put(world,new BlockPos(x,81,z),Blocks.SEA_LANTERN);
        }
        for(int x=-14;x<=14;x++) for(int z=150;z<=178;z++) {
            put(world,new BlockPos(x,79,z),Blocks.DEEPSLATE_BRICKS);
            put(world,new BlockPos(x,80,z),Math.abs(x)==14||z==178?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_BLACKSTONE);
            for(int y=81;y<93;y++) put(world,new BlockPos(x,y,z),Blocks.AIR);
        }
        for(int x:new int[]{-8,8}) for(int z:new int[]{152,168}) {
            for(int y=81;y<87;y++) put(world,new BlockPos(x,y,z),Blocks.CHISELED_DEEPSLATE);
            put(world,new BlockPos(x,87,z),Blocks.SEA_LANTERN);
        }
        // Fresh worlds receive the counterpart of the Ancient City gateway. Never retrofit over old builds.
        for(int x=-10;x<=11;x++)for(int y=80;y<=87;y++){
            var cell=new BlockPos(x,y,175);
            if(x==-10||x==11||y==80||y==87)put(world,cell,Blocks.REINFORCED_DEEPSLATE);
            else world.setBlockState(cell,ModBlocks.VOID_PORTAL.getDefaultState()
                .with(dev.qynl.myfirstmod.block.VoidPortalBlock.AXIS,net.minecraft.util.math.Direction.Axis.X),net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
        for(int z=151;z<=175;z+=4)for(int x:new int[]{-3,3})put(world,new BlockPos(x,80,z),ModBlocks.PRISM_LAMP);
        for(int x:new int[]{-12,12})for(int z:new int[]{154,162,170}){
            put(world,new BlockPos(x,81,z),ModBlocks.HUSHED_MOSS);
            put(world,new BlockPos(x,82,z),ModBlocks.HUSH_LEAVES);
        }
        state.thresholdBuilt=true;
        // Nearby introductory trial court, separate from the safe arrival tile.
        for(int x=14;x<=28;x++) for(int z=145;z<=159;z++) {
            put(world,new BlockPos(x,80,z),Blocks.DEEPSLATE_TILES);
            for(int y=81;y<=85;y++) put(world,new BlockPos(x,y,z),Blocks.AIR);
        }
        for(int x=4;x<15;x++) for(int z=153;z<=156;z++) {
            put(world,new BlockPos(x,80,z),Blocks.DEEPSLATE_TILES);
            for(int y=81;y<=84;y++) put(world,new BlockPos(x,y,z),Blocks.AIR);
        }
        put(world,new BlockPos(21,81,152),ModBlocks.RESONANCE_CORE);
        state.sanctuaryBuilt=true;
        prepareAltar(world,state);
        prepareForge(world,state);
        state.markDirty();
    }
    private static void prepareForge(ServerWorld world,RealmState state) {
        if(state.contentVersion>=6) {
            // An occupied legacy gate site can be cleared by its owner, then upgraded on re-entry.
            BlockPos gate=new BlockPos(-6,81,158);
            if(world.getBlockState(gate).isAir())put(world,gate,ModBlocks.KEEP_GATE);
            return;
        }
        BlockPos pos=new BlockPos(-6,81,160);
        // Upgrade old sanctuaries without overwriting an occupied player block.
        if(world.getBlockState(pos).isAir()) put(world,pos,ModBlocks.ATTUNEMENT_FORGE);
        BlockPos rest=new BlockPos(6,81,160);
        if(world.getBlockState(rest).isAir()) put(world,rest,ModBlocks.WAYSTONE);
        BlockPos ledger=new BlockPos(6,81,158);
        if(world.getBlockState(ledger).isAir()) put(world,ledger,ModBlocks.PILGRIM_LEDGER);
        BlockPos keepGate=new BlockPos(-6,81,158);
        if(world.getBlockState(keepGate).isAir())put(world,keepGate,ModBlocks.KEEP_GATE);
        state.contentVersion=6;state.markDirty();
    }
    private static void prepareAltar(ServerWorld world,RealmState state) {
        if(state.contentVersion>=2) return;
        for(int x=4;x<=12;x++) for(int z=26;z<=34;z++) {
            put(world,new BlockPos(x,80,z),Blocks.CHISELED_DEEPSLATE);
            for(int y=81;y<=86;y++) put(world,new BlockPos(x,y,z),Blocks.AIR);
        }
        for(int x:new int[]{4,12}) for(int z:new int[]{26,34}) {
            for(int y=81;y<85;y++) put(world,new BlockPos(x,y,z),Blocks.CRYING_OBSIDIAN);
            put(world,new BlockPos(x,85,z),Blocks.SOUL_LANTERN);
        }
        put(world,new BlockPos(8,81,30),ModBlocks.ECHO_ALTAR);
        state.contentVersion=2;state.markDirty();
    }
    private static void put(ServerWorld world,BlockPos pos,Block block) {
        world.setBlockState(pos,block.getDefaultState(),Block.NOTIFY_LISTENERS);
    }
    public static void welcome(ServerPlayerEntity player) {
        boolean hasCompass=false;
        for(int i=0;i<player.getInventory().size();i++) if(player.getInventory().getStack(i).isOf(ModItems.ARENA_COMPASS)) hasCompass=true;
        if(!hasCompass) player.getInventory().offerOrDrop(ArenaCompassItem.create());
        ExpeditionJournal.give(player);
        player.sendMessage(Text.translatable("message.myfirstmod.welcome"),false);
        player.sendMessage(Text.translatable("message.myfirstmod.expedition_hint"),false);
    }
    public static void tick(MinecraftServer server) {
        ServerWorld world=server.getWorld(VoidPortalManager.NULL_REALM);
        if(world==null || world.getTime()%20!=0) return;
        for(ServerPlayerEntity p:world.getPlayers()) {
            if(p.isSpectator() || !p.isAlive()) continue;
            ExpeditionRewards.claim(p);
            if(p.age % 100 == 0) {
                var state=RealmState.get(world);
                world.getBiome(p.getBlockPos()).getKey().ifPresent(key -> {
                    if(key.getValue().getNamespace().equals("myfirstmod")
                            && state.expedition(p.getUuid()).biomes.add(key.getValue().toString())) {
                        state.markDirty();
                        p.sendMessage(Text.translatable("message.myfirstmod.biome_discovered",
                                Text.translatable("biome.myfirstmod."+key.getValue().getPath())),false);
                        if(!p.isCreative()) p.addExperience(25);
                    }
                });
            }
            if(p.squaredDistanceTo(.5,81,.5)<20*20) NullWardenManager.approachArena(world,p);
            // A quiet arrival refuge: suppress naturally spawned hostiles, never trial mobs.
            var refuge=p.squaredDistanceTo(ARRIVAL.toCenterPos());
            if(RealmState.get(world).thresholdBuilt)refuge=Math.min(refuge,p.squaredDistanceTo(THRESHOLD.toCenterPos()));
            if(refuge<14*14) {
                world.getEntitiesByClass(net.minecraft.entity.mob.HostileEntity.class,
                        new net.minecraft.util.math.Box(ARRIVAL).union(new net.minecraft.util.math.Box(THRESHOLD)).expand(10),
                        e->!e.getCommandTags().contains("null_trial")).forEach(net.minecraft.entity.Entity::discard);
            }
            var progress=RealmState.get(world).expedition(p.getUuid());
            if(!progress.pilgrimKit && !p.isSpectator()) {
                progress.pilgrimKit=true;RealmState.get(world).markDirty();
                p.getInventory().offerOrDrop(new net.minecraft.item.ItemStack(ModItems.ASHEN_FLASK));
                p.getInventory().offerOrDrop(new net.minecraft.item.ItemStack(ModItems.PILGRIM_STEP));
                p.sendMessage(net.minecraft.text.Text.translatable("message.myfirstmod.pilgrim_kit"),false);
            }
            if(p.age%100==0) world.spawnParticles(ParticleTypes.END_ROD,p.getX(),p.getY()+3,p.getZ(),3,6,2,6,.01);
        }
    }
}
