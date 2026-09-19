package dev.qynl.myfirstmod.rift;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.mob.RiftHeraldEntity;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import dev.qynl.myfirstmod.realm.RealmTrials;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Opt-in rifts: ground three anchors, survive the emergence, defeat the Herald. */
public final class RealmRifts {
    private static final Map<BlockPos,Rift> ACTIVE=new HashMap<>();
    private static final int[][] ANCHORS={{-4,-3},{4,-3},{0,5}};
    public static boolean isEnrolled(UUID player) {return ACTIVE.values().stream().anyMatch(r->r.players.contains(player));}
    public static boolean owns(UUID actor) {return ACTIVE.values().stream().anyMatch(r->r.guards.containsKey(actor)||actor.equals(r.heraldId));}
    public static boolean isHeraldParticipant(UUID actor,UUID player) {
        return ACTIVE.values().stream().anyMatch(r->actor.equals(r.heraldId) && r.players.contains(player));
    }
    public static void clear() {ACTIVE.values().forEach(r->r.bar.clearPlayers());ACTIVE.clear();}
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        ServerWorld world=player.getServerWorld();
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || !world.getBlockState(pos).isOf(ModBlocks.RIFT_ANCHOR))
            return ActionResult.PASS;
        if(player.isSpectator() || player.isCreative()) return message(player,"message.myfirstmod.trial_survival");
        if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL) return message(player,"message.myfirstmod.trial_peaceful");
        if(dev.qynl.myfirstmod.keep.HollowKeep.enrolled(player.getUuid()) || ACTIVE.containsKey(pos) || isEnrolled(player.getUuid()) || RealmTrials.isEnrolled(player.getUuid())
                || NullWardenManager.isEncounterActive(world) && player.squaredDistanceTo(.5,81,.5)<48*48)
            return message(player,"message.myfirstmod.trial_busy");
        RealmState state=RealmState.get(world);
        long wait=state.riftCooldowns.getOrDefault(pos.asLong(),0L)-world.getTime();
        if(wait>0) return message(player,"message.myfirstmod.trial_cooldown",(wait+19)/20);
        Rift rift=new Rift(pos.toImmutable(),world.getTime()+12000);
        for(var p:world.getPlayers()) if(eligible(p,pos) && !dev.qynl.myfirstmod.keep.HollowKeep.enrolled(p.getUuid()) && !isEnrolled(p.getUuid()) && !RealmTrials.isEnrolled(p.getUuid()))
            rift.players.add(p.getUuid());
        ACTIVE.put(rift.pos,rift);
        state.riftCooldowns.put(pos.asLong(),world.getTime()+1200);state.markDirty();
        tell(world,rift,"message.myfirstmod.rift_begin");
        return ActionResult.SUCCESS;
    }
    private static ActionResult message(ServerPlayerEntity player,String key,Object... args) {
        player.sendMessage(Text.translatable(key,args),true);return ActionResult.SUCCESS;
    }
    private static boolean eligible(ServerPlayerEntity player,BlockPos pos) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator() && player.squaredDistanceTo(pos.toCenterPos())<24*24;
    }
    public static void tick(MinecraftServer server) {
        var world=server.getWorld(VoidPortalManager.NULL_REALM);
        if(world==null || world.getTime()%5!=0) return;
        var iterator=ACTIVE.values().iterator();
        while(iterator.hasNext()) {
            Rift r=iterator.next();
            var present=world.getPlayers().stream().filter(p->r.players.contains(p.getUuid()) && eligible(p,r.pos)).toList();
            for(var p:List.copyOf(r.bar.getPlayers())) if(!present.contains(p)) r.bar.removePlayer(p);
            present.forEach(r.bar::addPlayer);
            if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL || world.getTime()>=r.deadline
                    || world.isChunkLoaded(r.pos) && !world.getBlockState(r.pos).isOf(ModBlocks.RIFT_ANCHOR)) {
                tell(world,r,"message.myfirstmod.rift_failed");discard(world,r);iterator.remove();continue;
            }
            if(present.isEmpty()) {
                if((r.idle+=5)>=600) {discard(world,r);iterator.remove();}
                continue;
            }
            r.idle=0;r.elapsed+=5;
            present.forEach(p->r.participation.merge(p.getUuid(),5,Integer::sum));
            r.guards.replaceAll((id,old)->world.getEntity(id) instanceof MobEntity mob?mob:old);
            r.guards.entrySet().removeIf(e->!e.getValue().isAlive());
            for(var guard:r.guards.values()) if(!guard.isRemoved() && guard.squaredDistanceTo(r.pos.toCenterPos())>18*18)
                guard.refreshPositionAndAngles(r.pos.getX()+3.5,r.pos.getY()+1,r.pos.getZ()+.5,0,0);
            if(r.phase==0) {
                int grounded=0;
                for(int i=0;i<3;i++) {
                    Vec3d point=anchor(r.pos,i);
                    if(r.charge[i]<60) {
                        boolean grounding=present.stream().anyMatch(p->p.isSneaking() && p.squaredDistanceTo(point)<6.25);
                        r.charge[i]=ConvergenceRules.channelProgress(r.charge[i],grounding);
                        ring(world,point,1.6);
                        if(r.charge[i]==60) tell(world,r,"message.myfirstmod.rift_grounded",i+1);
                    }
                    if(r.charge[i]==60) grounded++;
                }
                r.bar.setName(Text.translatable("hud.myfirstmod.rift_anchors",grounded));
                r.bar.setPercent((r.charge[0]+r.charge[1]+r.charge[2])/180f);
                if(grounded==3) {
                    r.phase=1;r.emergence=60;
                    r.guards.values().forEach(Entity::discard);r.guards.clear();
                    tell(world,r,"message.myfirstmod.herald_emerging");
                } else if(r.elapsed==5 || r.elapsed%160==0) spawnGuards(world,r,present);
            } else if(r.phase==1) {
                r.emergence-=5;
                r.bar.setName(Text.translatable("hud.myfirstmod.rift_emerging"));
                ring(world,r.pos.toCenterPos().add(0,1,0),1+r.emergence*.06);
                if(r.emergence<=0 && !spawnHerald(world,r)) {
                    tell(world,r,"message.myfirstmod.trial_blocked");discard(world,r);iterator.remove();
                }
            } else {
                if(world.getEntity(r.heraldId) instanceof RiftHeraldEntity loaded) r.herald=loaded;
                // An unloaded actor is never considered a kill.
                if(r.herald!=null && r.herald.getHealth()<=0) {
                    reward(world,r);discard(world,r);iterator.remove();continue;
                }
                if(r.herald==null || r.herald.isRemoved()) continue;
                if(r.herald.squaredDistanceTo(r.pos.toCenterPos())>12*12)
                    r.herald.refreshPositionAndAngles(r.pos.getX()+.5,r.pos.getY()+1,r.pos.getZ()+.5,0,0);
                r.bar.setColor(BossBar.Color.PURPLE);
                r.bar.setName(Text.translatable("entity.myfirstmod.rift_herald"));
                r.bar.setPercent(Math.max(0,Math.min(1,r.herald.getHealth()/r.herald.getMaxHealth())));
            }
        }
    }
    private static Vec3d anchor(BlockPos pos,int i) {return pos.toCenterPos().add(ANCHORS[i][0],0,ANCHORS[i][1]);}
    private static void spawnGuards(ServerWorld world,Rift r,List<ServerPlayerEntity> players) {
        int limit=ConvergenceRules.guardians(r.players.size());
        for(int i=0;i<2 && r.guards.size()<limit;i++) {
            var mob=ModEntities.RIFT_SENTINEL.create(world);if(mob==null) return;
            mob.refreshPositionAndAngles(r.pos.getX()+(i==0?-4.5:5.5),r.pos.getY()+1,r.pos.getZ()+.5,0,0);
            if(!world.isSpaceEmpty(mob)) {mob.discard();continue;}
            mob.setTarget(players.get(i%players.size()));mob.setPersistent();mob.addCommandTag("null_rift");
            r.guards.put(mob.getUuid(),mob);
            if(!world.spawnEntity(mob)) r.guards.remove(mob.getUuid());
        }
    }
    private static boolean spawnHerald(ServerWorld world,Rift r) {
        var herald=ModEntities.RIFT_HERALD.create(world);if(herald==null) return false;
        herald.refreshPositionAndAngles(r.pos.getX()+.5,r.pos.getY()+1,r.pos.getZ()+.5,0,0);
        if(!world.isSpaceEmpty(herald)) {herald.discard();return false;}
        var health=herald.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if(health!=null) health.setBaseValue(ConvergenceRules.heraldHealth(r.players.size()));
        herald.setHealth(herald.getMaxHealth());herald.setPersistent();herald.addCommandTag("null_rift");
        r.herald=herald;r.heraldId=herald.getUuid();
        if(!world.spawnEntity(herald)) {r.herald=null;r.heraldId=null;return false;}
        r.phase=2;return true;
    }
    private static void reward(ServerWorld world,Rift r) {
        RealmState state=RealmState.get(world);
        for(UUID id:r.players) if(r.participation.getOrDefault(id,0)>=100) {
            var record=state.expedition(id);
            record.riftsClosed++;record.pendingRiftCores=Math.min(64,record.pendingRiftCores+1);
        }
        state.riftCooldowns.put(r.pos.asLong(),world.getTime()+12000);state.markDirty();
        world.spawnParticles(ParticleTypes.END_ROD,r.pos.getX()+.5,r.pos.getY()+2,r.pos.getZ()+.5,80,3,2,3,.05);
        tell(world,r,"message.myfirstmod.rift_closed");
    }
    private static void discard(ServerWorld world,Rift r) {
        r.bar.clearPlayers();
        r.guards.keySet().forEach(id->{var actor=world.getEntity(id);if(actor!=null) actor.discard();});
        if(r.heraldId!=null) {var actor=world.getEntity(r.heraldId);if(actor!=null) actor.discard();}
    }
    private static void ring(ServerWorld world,Vec3d center,double radius) {
        for(int i=0;i<20;i++) {
            double angle=i*Math.PI/10;
            world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,center.x+Math.cos(angle)*radius,center.y+.05,center.z+Math.sin(angle)*radius,1,0,0,0,0);
        }
    }
    private static void tell(ServerWorld world,Rift r,String key,Object... args) {
        for(UUID id:r.players) {var p=world.getServer().getPlayerManager().getPlayer(id);
            if(p!=null && p.getServerWorld()==world) p.sendMessage(Text.translatable(key,args),false);}
    }
    private static final class Rift {
        final BlockPos pos;final long deadline;
        final Set<UUID> players=new HashSet<>();final Map<UUID,Integer> participation=new HashMap<>();
        final Map<UUID,MobEntity> guards=new HashMap<>();final int[] charge=new int[3];
        final ServerBossBar bar=new ServerBossBar(Text.empty(),BossBar.Color.BLUE,BossBar.Style.NOTCHED_10);
        int phase,idle,elapsed,emergence;UUID heraldId;RiftHeraldEntity herald;
        Rift(BlockPos pos,long deadline) {this.pos=pos;this.deadline=deadline;}
    }
}
