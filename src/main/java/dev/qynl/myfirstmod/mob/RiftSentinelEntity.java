package dev.qynl.myfirstmod.mob;

import dev.qynl.myfirstmod.pilgrimage.PilgrimageRules;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** A committed greatblade cleave. Circle behind it, or interrupt with a heavy hit. */
public class RiftSentinelEntity extends ZombieEntity {
    private static final TrackedData<Integer> TELL=DataTracker.registerData(RiftSentinelEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private int cooldown=75,charge,recovery;
    private Vec3d facing=Vec3d.ZERO;
    public RiftSentinelEntity(EntityType<? extends ZombieEntity> type,World world) {super(type,world);}
    public static DefaultAttributeContainer.Builder attributes() {
        return ZombieEntity.createZombieAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,36)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,5).add(EntityAttributes.GENERIC_ARMOR,6)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.23);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {super.initDataTracker(builder);builder.add(TELL,0);}
    public int tell() {return getDataTracker().get(TELL);}
    @Override public void setBaby(boolean baby) {super.setBaby(false);}
    @Override protected boolean burnsInDaylight() {return false;}
    @Override public boolean tryAttack(Entity target) {return charge==0 && recovery==0 && super.tryAttack(target);}
    @Override public boolean damage(DamageSource source,float amount) {
        boolean hit=super.damage(source,amount);
        if(hit && isAlive() && charge>0 && amount>=6 && source.getAttacker() instanceof net.minecraft.entity.player.PlayerEntity) {
            charge=0;recovery=35;cooldown=100;getDataTracker().set(TELL,-35);
            playSound(SoundEvents.BLOCK_ANVIL_LAND,.45f,1.5f);
            if(getWorld() instanceof ServerWorld world) world.spawnParticles(ParticleTypes.CRIT,getX(),getY()+1.5,getZ(),18,.4,.5,.4,.1);
        }
        return hit;
    }
    @Override public void tick() {
        super.tick();if(!(getWorld() instanceof ServerWorld world) || !isAlive()) return;
        if(recovery>0) {getNavigation().stop();setVelocity(0,getVelocity().y,0);getDataTracker().set(TELL,-recovery--);return;}
        if(charge>0) {
            getNavigation().stop();setVelocity(0,getVelocity().y,0);
            float yaw=(float)(Math.toDegrees(Math.atan2(facing.z,facing.x))-90);setYaw(yaw);setHeadYaw(yaw);setBodyYaw(yaw);
            if(charge%4==0) {
                double angle=Math.atan2(facing.z,facing.x);
                for(int i=-6;i<=6;i++) {
                    double a=angle+i*.2;
                    world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,getX()+Math.cos(a)*3.5,getY()+.15,getZ()+Math.sin(a)*3.5,1,0,0,0,0);
                }
            }
            getDataTracker().set(TELL,--charge);
            if(charge==0) {
                for(var player:world.getPlayers()) {
                    Vec3d d=player.getPos().subtract(getPos());
                    if(player.isAlive() && !player.isSpectator() && !player.isCreative() && canSee(player)
                            && PilgrimageRules.inCleave(d.horizontalLengthSquared(),d.multiply(1,0,1).normalize().dotProduct(facing),d.y)) {
                        player.damage(world.getDamageSources().mobAttack(this),9);
                        player.takeKnockback(.6,getX()-player.getX(),getZ()-player.getZ());
                    }
                }
                recovery=30;cooldown=100;playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,1,.6f);
            }
        } else {
            getDataTracker().set(TELL,0);
            if(--cooldown<=0 && getTarget()!=null && canSee(getTarget()) && squaredDistanceTo(getTarget())<30) {
                facing=getTarget().getPos().subtract(getPos()).multiply(1,0,1).normalize();charge=32;
                getDataTracker().set(TELL,charge);playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,.8f,.7f);
            }
        }
    }
}
