package dev.qynl.myfirstmod.mob;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Crouches, marks a fixed destination, lunges, then exposes its core during recovery. */
public class ShardstalkerEntity extends SpiderEntity {
    private static final TrackedData<Integer> TELL=DataTracker.registerData(ShardstalkerEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private int cooldown=90,charge,dash,recovery;
    private Vec3d mark=Vec3d.ZERO;
    public ShardstalkerEntity(EntityType<? extends SpiderEntity> type,World world) {super(type,world);}
    public static DefaultAttributeContainer.Builder attributes() {
        return SpiderEntity.createSpiderAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,24)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,4).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.3);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) {super.initDataTracker(builder);builder.add(TELL,0);}
    public int tell() {return getDataTracker().get(TELL);}
    @Override public boolean tryAttack(Entity target) {return charge==0 && dash==0 && recovery==0 && super.tryAttack(target);}
    @Override public void tick() {
        super.tick();if(!(getWorld() instanceof ServerWorld world) || !isAlive()) return;
        if(recovery>0) {getNavigation().stop();setVelocity(0,getVelocity().y,0);getDataTracker().set(TELL,-recovery--);return;}
        if(dash>0) {
            getNavigation().stop();
            if(--dash==0) {
                for(var p:world.getPlayers()) if(p.isAlive() && !p.isSpectator() && !p.isCreative() && canSee(p) && squaredDistanceTo(p)<5)
                    p.damage(world.getDamageSources().mobAttack(this),7);
                recovery=28;cooldown=110;setVelocity(0,getVelocity().y,0);
            }
            return;
        }
        if(charge>0) {
            getNavigation().stop();setVelocity(0,getVelocity().y,0);
            if(charge%5==0) for(int i=0;i<20;i++) {
                double a=i*Math.PI/10;world.spawnParticles(ParticleTypes.END_ROD,mark.x+Math.cos(a)*1.8,mark.y+.1,mark.z+Math.sin(a)*1.8,1,0,0,0,0);
            }
            getDataTracker().set(TELL,--charge);
            if(charge==0) {
                Vec3d direction=mark.subtract(getPos()).multiply(1,0,1).normalize();setVelocity(direction.x*.8,.25,direction.z*.8);
                velocityModified=true;dash=10;getDataTracker().set(TELL,40);playSound(SoundEvents.ENTITY_SPIDER_HURT,.9f,.6f);
            }
        } else {
            getDataTracker().set(TELL,0);
            if(--cooldown<=0 && getTarget()!=null && squaredDistanceTo(getTarget())<64 && canSee(getTarget()) && isOnGround()) {
                mark=getTarget().getPos();charge=30;getDataTracker().set(TELL,30);playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,1,.5f);
            }
        }
    }
}
