package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.boss.NullWardenManager;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Opt-in, three-wave courts. Interrupted trials reset; cooldowns survive restarts. */
public final class RealmTrials {
    private static final Map<BlockPos, Trial> ACTIVE=new HashMap<>();
    public static boolean owns(UUID id) { return ACTIVE.values().stream().anyMatch(t->t.mobs.containsKey(id)); }
    public static void clear() { ACTIVE.clear(); }
    public static ActionResult interact(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world=player.getServerWorld();
        if (!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)
                || !world.getBlockState(pos).isOf(ModBlocks.RESONANCE_CORE)) return ActionResult.PASS;
        if (player.isSpectator() || player.isCreative()) {
            player.sendMessage(Text.translatable("message.myfirstmod.trial_survival"),true);
            return ActionResult.SUCCESS;
        }
        if (ACTIVE.containsKey(pos)) return ActionResult.SUCCESS;
        if (world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL) {
            player.sendMessage(Text.translatable("message.myfirstmod.trial_peaceful"),true);
            return ActionResult.SUCCESS;
        }
        RealmState state=RealmState.get(world);
        long remaining=state.trialCooldowns.getOrDefault(pos.asLong(),0L)-world.getTime();
        if (remaining>0) {
            player.sendMessage(Text.translatable("message.myfirstmod.trial_cooldown",(remaining+19)/20),true);
            return ActionResult.SUCCESS;
        }
        Trial trial=new Trial(pos.toImmutable(),NullWardenManager.isDefeated(world));
        for (ServerPlayerEntity p:world.getPlayers()) if (eligible(p,pos)) trial.players.add(p.getUuid());
        ACTIVE.put(trial.pos,trial);
        state.trialCooldowns.put(pos.asLong(),world.getTime()+1200);
        state.markDirty();
        tell(world,trial,trial.ascended ? "message.myfirstmod.trial_ascended" : "message.myfirstmod.trial_begin");
        return ActionResult.SUCCESS;
    }
    private static boolean eligible(ServerPlayerEntity p, BlockPos pos) {
        return p.isAlive() && !p.isSpectator() && !p.isCreative() && p.squaredDistanceTo(pos.toCenterPos())<256;
    }
    public static void tick(MinecraftServer server) {
        ServerWorld world=server.getWorld(VoidPortalManager.NULL_REALM);
        if(world==null || world.getTime()%20!=0) return;
        var iterator=ACTIVE.values().iterator();
        while(iterator.hasNext()) {
            Trial t=iterator.next();
            if(world.getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL) {
                discard(world,t);iterator.remove();continue;
            }
            boolean present=world.getPlayers().stream().anyMatch(p->t.players.contains(p.getUuid()) && eligible(p,t.pos));
            if(!present) {
                if(++t.idle<30) continue;
                discard(world,t);iterator.remove();continue;
            }
            t.idle=0;
            // Missing entities cannot award immediate victory after a chunk unload: wait for a loaded court.
            if(!world.isChunkLoaded(t.pos)) continue;
            t.mobs.replaceAll((id, old) -> world.getEntity(id) instanceof MobEntity loaded ? loaded : old);
            t.mobs.entrySet().removeIf(entry -> !entry.getValue().isAlive());
            for(UUID id:t.mobs.keySet()) {
                Entity e=world.getEntity(id);
                if(e!=null && e.squaredDistanceTo(t.pos.toCenterPos())>225)
                    e.refreshPositionAndAngles(t.pos.getX()+3.5,t.pos.getY()+1,t.pos.getZ()+.5,0,0);
            }
            world.spawnParticles(ParticleTypes.END_ROD,t.pos.getX()+.5,t.pos.getY()+1.4,t.pos.getZ()+.5,4,.4,.3,.4,.01);
            if(!t.mobs.isEmpty()) continue;
            if(--t.rest>0) continue;
            if(t.wave>=3) {
                for(UUID id:t.players) {
                    ServerPlayerEntity p=server.getPlayerManager().getPlayer(id);
                    if(p!=null && p.getServerWorld()==world && eligible(p,t.pos)) {
                        p.getInventory().offerOrDrop(new ItemStack(ModItems.RESONANT_SHARD,t.ascended?4:2));
                        p.addExperience(t.ascended?120:50);
                    }
                }
                tell(world,t,"message.myfirstmod.trial_complete");
                RealmState state=RealmState.get(world);
                state.trialCooldowns.put(t.pos.asLong(),world.getTime()+6000);state.markDirty();
                iterator.remove();continue;
            }
            t.wave++;
            tell(world,t,"message.myfirstmod.trial_wave",t.wave);
            int count=Math.min(6,1+t.wave+(t.ascended?1:0)+Math.max(0,t.players.size()-1));
            for(int i=0;i<count;i++) {
                MobEntity mob=(i%2==0?ModEntities.RIFT_SENTINEL:ModEntities.SHARDSTALKER).create(world);
                if(mob==null) continue;
                double angle=i*Math.PI*2/count;
                mob.refreshPositionAndAngles(t.pos.getX()+.5+Math.cos(angle)*4,t.pos.getY()+1,
                        t.pos.getZ()+.5+Math.sin(angle)*4,0,0);
                if(!world.isSpaceEmpty(mob)) { mob.discard(); continue; }
                if(t.ascended) {
                    var health=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
                    if(health!=null) {health.setBaseValue(health.getBaseValue()*1.5);mob.setHealth(mob.getMaxHealth());}
                }
                mob.addCommandTag("null_trial");mob.setPersistent();t.mobs.put(mob.getUuid(),mob);
                if(!world.spawnEntity(mob)) t.mobs.remove(mob.getUuid());
            }
            // A blocked court is not a free reward dispenser.
            if(t.mobs.isEmpty()) { tell(world,t,"message.myfirstmod.trial_blocked");iterator.remove(); }
            t.rest=4;
        }
    }
    private static void discard(ServerWorld world,Trial t) {
        for(UUID id:t.mobs.keySet()) {Entity entity=world.getEntity(id);if(entity!=null) entity.discard();}
    }
    private static void tell(ServerWorld world,Trial t,String key,Object... args) {
        for(UUID id:t.players) {
            var p=world.getServer().getPlayerManager().getPlayer(id);
            if(p!=null && p.getServerWorld()==world) p.sendMessage(Text.translatable(key,args),false);
        }
    }
    private static final class Trial {
        final BlockPos pos;final boolean ascended;
        final Set<UUID> players=new HashSet<>();
        final Map<UUID,MobEntity> mobs=new HashMap<>();
        int wave,idle,rest=3;
        Trial(BlockPos pos,boolean ascended) {this.pos=pos;this.ascended=ascended;}
    }
}
