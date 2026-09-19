package dev.qynl.myfirstmod.mob;

import dev.qynl.myfirstmod.rift.ConvergenceRules;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A stationary astral construct: every damaging attack is committed and telegraphed. */
public final class RiftHeraldEntity extends HostileEntity {
    private static final TrackedData<Integer> ATTACK=DataTracker.registerData(RiftHeraldEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE=DataTracker.registerData(RiftHeraldEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private Vec3d mark=Vec3d.ZERO;
    private int rest=80, step;
    public RiftHeraldEntity(EntityType<? extends HostileEntity> type,World world) {super(type,world);}
    public static DefaultAttributeContainer.Builder attributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,180)
                .add(EntityAttributes.GENERIC_ARMOR,4).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);builder.add(ATTACK,0);builder.add(CHARGE,0);
    }
    public int visualAttack() {return getDataTracker().get(ATTACK);}
    public int charge() {return getDataTracker().get(CHARGE);}
    @Override public boolean canImmediatelyDespawn(double distance) {return false;}
    @Override public void tick() {
        super.tick();
        if(!(getWorld() instanceof ServerWorld world) || !isAlive()) return;
        setVelocity(0,getVelocity().y,0);
        var players=world.getPlayers().stream().filter(p->p.isAlive() && !p.isCreative() && !p.isSpectator()
                && squaredDistanceTo(p)<24*24 && (!getCommandTags().contains("null_rift")
                        || dev.qynl.myfirstmod.rift.RealmRifts.isHeraldParticipant(getUuid(),p.getUuid()))).toList();
        if(players.isEmpty()) {getDataTracker().set(ATTACK,0);getDataTracker().set(CHARGE,0);rest=60;return;}
        if(visualAttack()==0) {
            if(--rest>0) return;
            var target=players.get(Math.floorMod(step,players.size()));
            mark=target.getPos();
            int attack=++step%2==0?2:1;
            getDataTracker().set(ATTACK,attack);getDataTracker().set(CHARGE,40);
            playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,1.2f,attack==1?1.3f:.7f);
            for(var player:players) player.sendMessage(net.minecraft.text.Text.translatable("herald.myfirstmod.attack."+attack),true);
            return;
        }
        int left=charge()-1;getDataTracker().set(CHARGE,left);
        if(left%5==0) {
            if(visualAttack()==1) ring(world,mark,2.5);
            else {ring(world,getPos().add(0,-1,0),3);ring(world,getPos().add(0,-1,0),6);}
        }
        if(left>0) return;
        for(var player:players) {
            boolean hit=visualAttack()==1
                    ? ConvergenceRules.burstHits(Math.pow(player.getX()-mark.x,2)+Math.pow(player.getZ()-mark.z,2),player.getY()-mark.y)
                    : ConvergenceRules.ringHits(Math.hypot(player.getX()-getX(),player.getZ()-getZ()),player.getY()-getY());
            if(hit && canSee(player)) player.damage(world.getDamageSources().mobAttack(this),visualAttack()==1?8:10);
        }
        Vec3d impact=visualAttack()==1?mark:getPos();
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,impact.x,impact.y+.3,impact.z,28,1,.2,1,.04);
        playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK,1.4f,.6f);
        getDataTracker().set(ATTACK,0);rest=getHealth()<getMaxHealth()/2?55:80;
    }
    private void ring(ServerWorld world,Vec3d pos,double radius) {
        for(int i=0;i<28;i++) {
            double angle=i*Math.PI/14;
            world.spawnParticles(ParticleTypes.END_ROD,pos.x+Math.cos(angle)*radius,pos.y+.15,pos.z+Math.sin(angle)*radius,1,0,0,0,0);
        }
    }
}
