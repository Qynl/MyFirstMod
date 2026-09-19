package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/** Party-scoped courts with bounded lifetimes, explicit intermissions and five endgame tiers. */
public final class RealmTrials {
    private static final Map<BlockPos,Trial> ACTIVE=new HashMap<>();
    public static boolean owns(UUID id) {return ACTIVE.values().stream().anyMatch(t->t.mobs.containsKey(id));}
    public static void clear() {ACTIVE.values().forEach(t->t.bar.clearPlayers());ACTIVE.clear();}

    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        ServerWorld world=player.getServerWorld();
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)
                || !world.getBlockState(pos).isOf(ModBlocks.RESONANCE_CORE)) return ActionResult.PASS;
        RealmState state=RealmState.get(world);
        ExpeditionRecord record=state.expedition(player.getUuid());
        record.discoverCourt(pos.asLong());state.markDirty();
        if(player.isSpectator() || player.isCreative()) return message(player,"message.myfirstmod.trial_survival");
        if(ACTIVE.containsKey(pos)) return ActionResult.SUCCESS;
        if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL) return message(player,"message.myfirstmod.trial_peaceful");
        // One enrollment at a time prevents overlapping courts sharing the same party.
        if(ACTIVE.values().stream().anyMatch(t->t.players.contains(player.getUuid())))
            return message(player,"message.myfirstmod.trial_busy");
        long remaining=state.trialCooldowns.getOrDefault(pos.asLong(),0L)-world.getTime();
        if(remaining>0) return message(player,"message.myfirstmod.trial_cooldown",(remaining+19)/20);
        int tier=NullWardenManager.isDefeated(world)?(player.isSneaking()?TrialRules.nextTier(record.highestTier):1):0;
        Trial trial=new Trial(pos.toImmutable(),tier,world.getTime()+12000);
        for(ServerPlayerEntity p:world.getPlayers()) if(eligible(p,pos)
                && ACTIVE.values().stream().noneMatch(t->t.players.contains(p.getUuid()))) trial.players.add(p.getUuid());
        ACTIVE.put(trial.pos,trial);
        state.trialCooldowns.put(pos.asLong(),world.getTime()+1200);state.markDirty();
        tell(world,trial,"message.myfirstmod.trial_tier",tier,Text.translatable("oath.myfirstmod."+trial.oath));
        return ActionResult.SUCCESS;
    }
    private static ActionResult message(ServerPlayerEntity p,String key,Object... args) {
        p.sendMessage(Text.translatable(key,args),true);return ActionResult.SUCCESS;
    }
    private static boolean eligible(ServerPlayerEntity p,BlockPos pos) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.squaredDistanceTo(pos.toCenterPos())<256;
    }
    public static void tick(MinecraftServer server) {
        ServerWorld world=server.getWorld(VoidPortalManager.NULL_REALM);
        if(world==null || world.getTime()%5!=0) return;
        var iterator=ACTIVE.values().iterator();
        while(iterator.hasNext()) {
            Trial t=iterator.next();
            List<ServerPlayerEntity> present=world.getPlayers().stream()
                    .filter(p->t.players.contains(p.getUuid()) && eligible(p,t.pos)).toList();
            updateBar(t,present);
            if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL || world.getTime()>=t.deadline) {
                tell(world,t,"message.myfirstmod.trial_expired");discard(world,t);iterator.remove();continue;
            }
            if(present.isEmpty()) {
                t.hazards.clear();
                if(++t.idle<120) continue;
                discard(world,t);iterator.remove();continue;
            }
            t.idle=0;
            if(!world.isChunkLoaded(t.pos)) continue;
            t.mobs.replaceAll((id,old)->world.getEntity(id) instanceof MobEntity loaded?loaded:old);
            // Dead actors can be removed. Merely unloaded actors never count as kills.
            t.mobs.entrySet().removeIf(entry->!entry.getValue().isAlive());
            if(!t.mobs.isEmpty()) oath(world,t,present);
            else t.hazards.clear();
            if(world.getTime()%20!=0) continue;
            for(UUID id:t.mobs.keySet()) {
                Entity entity=world.getEntity(id);
                if(entity!=null && entity.squaredDistanceTo(t.pos.toCenterPos())>225)
                    entity.refreshPositionAndAngles(t.pos.getX()+3.5,t.pos.getY()+1,t.pos.getZ()+.5,0,0);
            }
            if(!t.mobs.isEmpty()) continue;
            if(--t.rest>0) continue;
            if(t.wave>=TrialRules.waves(t.tier)) {
                complete(world,t,present);t.bar.clearPlayers();iterator.remove();continue;
            }
            t.wave++;
            spawnWave(world,t,present);
            if(t.mobs.isEmpty()) {
                tell(world,t,"message.myfirstmod.trial_blocked");t.bar.clearPlayers();iterator.remove();
            }
            t.rest=5;
        }
    }
    private static void updateBar(Trial t,List<ServerPlayerEntity> players) {
        for(var existing:List.copyOf(t.bar.getPlayers())) if(!players.contains(existing)) t.bar.removePlayer(existing);
        players.forEach(t.bar::addPlayer);
        t.bar.setName(Text.translatable(t.mobs.isEmpty()?"hud.myfirstmod.trial_rest":"hud.myfirstmod.trial_active",
                t.tier,t.wave,TrialRules.waves(t.tier),t.mobs.size(),Text.translatable("oath.myfirstmod."+t.oath)));
        float progress=(Math.max(0,t.wave-1)+(t.spawned==0?0:1-t.mobs.size()/(float)t.spawned))/TrialRules.waves(t.tier);
        t.bar.setPercent(Math.max(0,Math.min(1,progress)));
    }
    private static void spawnWave(ServerWorld world,Trial t,List<ServerPlayerEntity> players) {
        tell(world,t,"message.myfirstmod.trial_wave_count",t.wave,TrialRules.waves(t.tier));
        int count=TrialRules.enemies(t.wave,t.tier,t.players.size());
        for(int i=0;i<count;i++) {
            boolean captain=t.tier>0 && i==0 && t.wave==TrialRules.waves(t.tier);
            MobEntity mob=(i%2==0?ModEntities.RIFT_SENTINEL:ModEntities.SHARDSTALKER).create(world);
            if(mob==null) continue;
            double angle=i*Math.PI*2/count;
            mob.refreshPositionAndAngles(t.pos.getX()+.5+Math.cos(angle)*4,t.pos.getY()+1,
                    t.pos.getZ()+.5+Math.sin(angle)*4,0,0);
            if(!world.isSpaceEmpty(mob)) {mob.discard();continue;}
            var health=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if(health!=null) {health.setBaseValue(health.getBaseValue()*TrialRules.healthMultiplier(t.tier)*(captain?2:1));mob.setHealth(mob.getMaxHealth());}
            if(t.oath==2) {
                var speed=mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if(speed!=null) speed.setBaseValue(speed.getBaseValue()*1.15);
            }
            if(captain) {mob.setCustomName(Text.translatable("entity.myfirstmod.court_captain"));mob.setCustomNameVisible(true);}
            mob.setTarget(players.get(i%players.size()));
            mob.addCommandTag("null_trial");mob.setPersistent();t.mobs.put(mob.getUuid(),mob);
            if(!world.spawnEntity(mob)) t.mobs.remove(mob.getUuid());
        }
        t.spawned=t.mobs.size();
    }
    private static void oath(ServerWorld world,Trial t,List<ServerPlayerEntity> players) {
        if(t.tier==0) return;
        if(t.oath==0) {
            if(t.hazards.isEmpty() && world.getTime()%200==0) {
                players.forEach(p->t.hazards.put(p.getUuid(),p.getPos()));t.detonateAt=world.getTime()+40;
                tell(world,t,"message.myfirstmod.fracture_warning");
            }
            for(Vec3d center:t.hazards.values()) circle(world,center,2.5,ParticleTypes.FLAME);
            if(!t.hazards.isEmpty() && world.getTime()>=t.detonateAt) {
                // Overlapping circles never multiply the damage on a single player.
                for(var p:players) if(t.hazards.values().stream().anyMatch(v->Math.abs(v.y-p.getY())<3
                        && Math.pow(v.x-p.getX(),2)+Math.pow(v.z-p.getZ(),2)<6.25))
                    p.damage(world.getDamageSources().magic(),4+t.tier);
                t.hazards.clear();
            }
        } else if(t.oath==1) {
            for(int i=0;i<2;i++) {
                if(t.wards[i]>=40) continue;
                Vec3d point=t.pos.toCenterPos().add(i==0?-4:4,0,0);
                circle(world,point,1.5,ParticleTypes.END_ROD);
                if(players.stream().anyMatch(p->p.isSneaking() && p.squaredDistanceTo(point)<6.25)) {
                    t.wards[i]+=5;
                    if(t.wards[i]>=40) tell(world,t,"message.myfirstmod.ward_broken");
                }
            }
            if(world.getTime()%60==0 && (t.wards[0]<40 || t.wards[1]<40))
                t.mobs.values().stream().filter(m->!m.isRemoved()).forEach(m->m.heal(1));
        }
    }
    private static void circle(ServerWorld world,Vec3d center,double radius,net.minecraft.particle.ParticleEffect effect) {
        for(int i=0;i<20;i++) {
            double angle=i*Math.PI/10;
            world.spawnParticles(effect,center.x+Math.cos(angle)*radius,center.y+.15,center.z+Math.sin(angle)*radius,1,0,0,0,0);
        }
    }
    private static void complete(ServerWorld world,Trial t,List<ServerPlayerEntity> players) {
        RealmState state=RealmState.get(world);
        // Record completion before dispensing rewards; late arrivals are never eligible.
        for(var p:players) {
            ExpeditionRecord record=state.expedition(p.getUuid());
            record.trials++;record.highestTier=Math.max(record.highestTier,t.tier);
            record.discoverCourt(t.pos.asLong());
            p.getInventory().offerOrDrop(new ItemStack(ModItems.RESONANT_SHARD,TrialRules.shards(t.tier)));
            if(t.tier>=2) p.getInventory().offerOrDrop(new ItemStack(ModItems.ECHO_SIGIL));
            p.addExperience(TrialRules.experience(t.tier));
        }
        state.trialCooldowns.put(t.pos.asLong(),world.getTime()+6000);state.markDirty();
        tell(world,t,"message.myfirstmod.trial_complete_tier",t.tier,TrialRules.shards(t.tier));
    }
    private static void discard(ServerWorld world,Trial t) {
        t.bar.clearPlayers();
        for(UUID id:t.mobs.keySet()) {Entity entity=world.getEntity(id);if(entity!=null) entity.discard();}
    }
    private static void tell(ServerWorld world,Trial t,String key,Object... args) {
        for(UUID id:t.players) {
            var p=world.getServer().getPlayerManager().getPlayer(id);
            if(p!=null && p.getServerWorld()==world) p.sendMessage(Text.translatable(key,args),false);
        }
    }
    private static final class Trial {
        final BlockPos pos; final int tier,oath; final long deadline;
        final Set<UUID> players=new HashSet<>();
        final Map<UUID,MobEntity> mobs=new HashMap<>();
        final Map<UUID,Vec3d> hazards=new HashMap<>();
        final int[] wards=new int[2];
        final ServerBossBar bar=new ServerBossBar(Text.empty(),BossBar.Color.BLUE,BossBar.Style.NOTCHED_10);
        int wave,idle,rest=3,spawned;long detonateAt;
        Trial(BlockPos pos,int tier,long deadline) {
            this.pos=pos;this.tier=tier;this.deadline=deadline;this.oath=tier==0?3:TrialRules.oath(pos.asLong(),tier);
        }
    }
}
