package dev.qynl.myfirstmod.keep;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.*;
import net.minecraft.registry.*;
import net.minecraft.server.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;

/** One shared expedition at a time. World geometry persists; transient encounters never do. */
public final class HollowKeep {
    public static final RegistryKey<World> WORLD=RegistryKey.of(RegistryKeys.WORLD,Identifier.of("myfirstmod","hollow_keep"));
    private static Run run;private static int build=-1;private static long readyAt;
    public static boolean expedition(World world) {return world.getRegistryKey().equals(WORLD)||world.getRegistryKey().equals(VoidPortalManager.NULL_REALM);}
    public static RealmState playerState(ServerPlayerEntity p) {return RealmState.get(p.getServer().getWorld(VoidPortalManager.NULL_REALM));}
    public static boolean enrolled(UUID id) {return run!=null&&run.players.contains(id);}
    public static boolean active() {return run!=null;}
    public static boolean owns(UUID id) {return run!=null&&(run.guards.containsKey(id)||id.equals(run.bossId));}
    public static boolean prepare(MinecraftServer server) {
        var world=server.getWorld(WORLD);if(world==null)return false;
        if(RealmState.get(world).sanctuaryBuilt)return true;
        if(build<0){build=0;tickets(world,true);}return false;
    }
    public static void resetTickets(MinecraftServer server){var world=server.getWorld(WORLD);if(world!=null)tickets(world,false);}
    public static void registerCommands() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(
            net.minecraft.server.command.CommandManager.literal("hollowkeep").requires(source->source.hasPermissionLevel(2))
                .then(net.minecraft.server.command.CommandManager.literal("prepare").executes(ctx->{boolean ready=prepare(ctx.getSource().getServer());ctx.getSource().sendFeedback(()->Text.literal(ready?"KEEP_READY":"KEEP_BUILDING"),false);return 1;}))));
    }
    public static void clear() {if(run!=null)run.bar.clearPlayers();run=null;build=-1;readyAt=0;}
    private static void tickets(ServerWorld world,boolean force) {for(int x=-2;x<=1;x++)for(int z=-2;z<=1;z++)world.setChunkForced(x,z,force);}
    private static ActionResult tell(ServerPlayerEntity p,String key,Object... args) {p.sendMessage(Text.translatable(key,args),false);return ActionResult.SUCCESS;}
    public static ActionResult interact(ServerPlayerEntity p,BlockPos pos) {
        var w=p.getServerWorld();var block=w.getBlockState(pos);
        if(p.isSpectator())return ActionResult.PASS;
        if(block.isOf(ModBlocks.KEEP_GATE)) {
            if(w.getRegistryKey().equals(WORLD)) {exit(p);return ActionResult.SUCCESS;}
            if(!w.getRegistryKey().equals(VoidPortalManager.NULL_REALM))return ActionResult.PASS;
            if(!p.isCreative() && RealmState.get(w).bossClears<1)return tell(p,"message.myfirstmod.keep_locked");
            if(Waystones.combatLocked(p))return tell(p,"message.myfirstmod.trial_busy");
            var target=p.getServer().getWorld(WORLD);if(target==null)return ActionResult.FAIL;
            if(!RealmState.get(target).sanctuaryBuilt) {
                if(build<0){build=0;tickets(target,true);}
                return tell(p,"message.myfirstmod.keep_building");
            }
            if(run!=null&&!enrolled(p.getUuid())&&!p.isCreative())return tell(p,"message.myfirstmod.keep_busy");
            p.teleport(target,.5,65,27.5,180,0);p.fallDistance=0;
            return tell(p,"message.myfirstmod.keep_arrival");
        }
        if(!w.getRegistryKey().equals(WORLD))return ActionResult.PASS;
        if(block.isOf(ModBlocks.KEEP_HEART)) {
            if(run!=null)return tell(p,"message.myfirstmod.keep_busy");
            if(p.isCreative()||w.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL)return tell(p,"message.myfirstmod.trial_survival");
            if(w.getTime()<readyAt)return tell(p,"message.myfirstmod.trial_cooldown",(readyAt-w.getTime()+19)/20);
            run=new Run(w.getTime()+18000);
            for(var ally:w.getPlayers())if(eligible(ally)&&ally.squaredDistanceTo(pos.toCenterPos())<=16*16)run.players.add(ally.getUuid());
            tickets(w,true);announce(w,"message.myfirstmod.keep_begin");return ActionResult.SUCCESS;
        }
        if(block.isOf(ModBlocks.KEEP_WARD)) {
            if(!enrolled(p.getUuid()))return tell(p,"message.myfirstmod.keep_start_first");
            if(run.room>=0||run.boss!=null)return tell(p,"message.myfirstmod.keep_finish_room");
            for(int i=0;i<3;i++)if(pos.equals(new BlockPos(KeepArchitecture.WARDS[i][0],65,KeepArchitecture.WARDS[i][1]))) {
                if((run.seals&(1<<i))!=0)return tell(p,"message.myfirstmod.keep_ward_done");
                if(!spawnRoom(w,i)) {discardGuards();return tell(p,"message.myfirstmod.trial_blocked");}
                run.room=i;announce(w,"message.myfirstmod.keep_room",i+1);return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
    private static boolean eligible(ServerPlayerEntity p){return p.isAlive()&&!p.isCreative()&&!p.isSpectator();}
    private static void exit(ServerPlayerEntity p) {
        var home=p.getServer().getWorld(VoidPortalManager.NULL_REALM);if(home==null)return;
        RealmExpedition.prepare(home);p.teleport(home,.5,81,160.5,180,0);p.fallDistance=0;
    }
    private static boolean spawnRoom(ServerWorld world,int room) {
        int count=2+KeepRules.party(run.players.size());int[] center=KeepArchitecture.WARDS[room];
        for(int i=0;i<count;i++) {
            MobEntity mob=(room==0 || room==2&&i%2==0)?ModEntities.RIFT_SENTINEL.create(world):ModEntities.SHARDSTALKER.create(world);
            if(mob==null)return false;
            double angle=i*Math.PI*2/count;mob.refreshPositionAndAngles(center[0]+.5+Math.cos(angle)*4,65,center[1]+.5+Math.sin(angle)*4,0,0);
            if(!world.isSpaceEmpty(mob)){mob.discard();return false;}
            mob.addCommandTag("hollow_keep");mob.setPersistent();
            run.guards.put(mob.getUuid(),mob);
            if(!world.spawnEntity(mob))return false;
            world.getPlayers().stream().filter(p->enrolled(p.getUuid())&&eligible(p)).min(Comparator.comparingDouble(mob::squaredDistanceTo)).ifPresent(mob::setTarget);
        }
        return true;
    }
    private static boolean spawnBoss(ServerWorld world) {
        var boss=ModEntities.GRAVE_REGENT.create(world);if(boss==null)return false;
        boss.refreshPositionAndAngles(.5,65,.5,180,0);if(!world.isSpaceEmpty(boss)){boss.discard();return false;}
        boss.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(KeepRules.health(run.players.size()));boss.setHealth(boss.getMaxHealth());
        boss.setPersistent();boss.addCommandTag("hollow_keep");run.boss=boss;run.bossId=boss.getUuid();
        if(!world.spawnEntity(boss))return false;
        announce(world,"message.myfirstmod.keep_regent");return true;
    }
    public static void tick(MinecraftServer server) {
        var world=server.getWorld(WORLD);if(world==null)return;
        if(build>=0) {
            KeepArchitecture.buildChunk(world,build%4-2,build/4-2);
            if(++build>=16){RealmState.get(world).sanctuaryBuilt=true;RealmState.get(world).markDirty();build=-1;tickets(world,false);}
        }
        if(world.getTime()%5!=0)return;
        for(var p:world.getPlayers())if(p.isAlive()&&p.getY()<55){p.teleport(world,.5,65,27.5,180,0);p.fallDistance=0;}
        if(run==null)return;
        var present=world.getPlayers().stream().filter(p->enrolled(p.getUuid())&&eligible(p)).toList();
        for(var p:List.copyOf(run.bar.getPlayers()))if(!present.contains(p))run.bar.removePlayer(p);
        present.forEach(run.bar::addPlayer);
        if(present.isEmpty())run.idle+=5;else run.idle=0;
        if(run.idle>=600 || world.getTime()>=run.deadline || world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL) {finish(world,false);return;}
        present.forEach(p->run.participation.merge(p.getUuid(),5,Integer::sum));
        if(run.boss!=null) {
            if(world.getEntity(run.bossId) instanceof GraveRegentEntity actor)run.boss=actor;
            if(run.boss.getHealth()<=0){finish(world,true);return;}
            if(run.boss.isRemoved()){finish(world,false);return;}
            run.bar.setName(Text.translatable("entity.myfirstmod.grave_regent"));run.bar.setPercent(Math.max(0,Math.min(1,run.boss.getHealth()/run.boss.getMaxHealth())));
            if(run.boss.squaredDistanceTo(.5,65,.5)>100)run.boss.refreshPositionAndAngles(.5,65,.5,180,0);
        } else if(run.emergence>0) {
            run.emergence-=5;run.bar.setName(Text.translatable("message.myfirstmod.keep_emergence"));
            if(run.emergence==0&&!spawnBoss(world))finish(world,false);
        } else {
            run.bar.setName(Text.translatable("hud.myfirstmod.keep",Integer.bitCount(run.seals),run.guards.size()));
            run.bar.setPercent(Integer.bitCount(run.seals)/3f);
            if(run.room>=0) {
                // A removed/unloaded actor is a failed expedition, never a victory.
                if(run.guards.values().stream().anyMatch(m->m.isRemoved()&&m.getHealth()>0)){finish(world,false);return;}
                run.guards.entrySet().removeIf(e->e.getValue().getHealth()<=0);
                if(run.guards.isEmpty()) {
                    run.seals|=1<<run.room;run.room=-1;announce(world,"message.myfirstmod.keep_ward_fallen");
                    if(run.seals==7)run.emergence=100;
                } else for(var mob:run.guards.values()) {
                    int[] c=KeepArchitecture.WARDS[run.room];
                    if(mob.squaredDistanceTo(c[0]+.5,65,c[1]+.5)>12*12)mob.refreshPositionAndAngles(c[0]+3.5,65,c[1]+.5,0,0);
                }
            }
        }
    }
    private static void discardGuards(){if(run!=null){run.guards.values().forEach(Entity::discard);run.guards.clear();}}
    private static void finish(ServerWorld world,boolean won) {
        if(won) {
            var home=world.getServer().getWorld(VoidPortalManager.NULL_REALM);var state=RealmState.get(home);
            for(UUID id:run.players)if(run.participation.getOrDefault(id,0)>=200){var r=state.expedition(id);r.pendingKeepRewards=Math.min(64,r.pendingKeepRewards+1);}
            state.markDirty();
        }
        announce(world,won?"message.myfirstmod.keep_victory":"message.myfirstmod.keep_failed");
        discardGuards();if(run.boss!=null)run.boss.discard();run.bar.clearPlayers();run=null;readyAt=world.getTime()+1200;tickets(world,false);
    }
    private static void announce(ServerWorld world,String key,Object... args){for(var p:world.getPlayers())if(enrolled(p.getUuid()))p.sendMessage(Text.translatable(key,args),false);}
    private static final class Run {
        final Set<UUID>players=new HashSet<>();final Map<UUID,Integer>participation=new HashMap<>();final Map<UUID,MobEntity>guards=new HashMap<>();
        final ServerBossBar bar=new ServerBossBar(Text.empty(),BossBar.Color.RED,BossBar.Style.NOTCHED_10);
        final long deadline;int idle,seals,room=-1,emergence;UUID bossId;GraveRegentEntity boss;
        Run(long deadline){this.deadline=deadline;}
    }
}
