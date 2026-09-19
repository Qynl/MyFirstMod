package dev.qynl.myfirstmod.keep;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
public final class GraveRegentEntity extends HostileEntity {
    private static final TrackedData<Integer> ATTACK=DataTracker.registerData(GraveRegentEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE=DataTracker.registerData(GraveRegentEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private int rest=70,step;private Vec3d mark=Vec3d.ZERO,facing=Vec3d.ZERO;
    public GraveRegentEntity(EntityType<? extends HostileEntity> type,World world) {super(type,world);}
    public static DefaultAttributeContainer.Builder attributes() {return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,360).add(EntityAttributes.GENERIC_ARMOR,8).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0);}
    @Override protected void initDataTracker(DataTracker.Builder b) {super.initDataTracker(b);b.add(ATTACK,0);b.add(CHARGE,0);}
    public int attack() {return getDataTracker().get(ATTACK);} public int charge() {return getDataTracker().get(CHARGE);}
    @Override public boolean canImmediatelyDespawn(double distance) {return false;}
    @Override public void tick() {
        super.tick();if(!(getWorld() instanceof ServerWorld w) || !isAlive()) return;
        setVelocity(0,getVelocity().y,0);
        var players=w.getPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&squaredDistanceTo(p)<28*28
            && (!getCommandTags().contains("hollow_keep") || HollowKeep.enrolled(p.getUuid()))).toList();
        if(players.isEmpty()) {getDataTracker().set(ATTACK,0);getDataTracker().set(CHARGE,0);rest=60;return;}
        if(attack()==0) {
            if(--rest>0) return;
            mark=players.get(Math.floorMod(step,players.size())).getPos();facing=mark.subtract(getPos()).multiply(1,0,1).normalize();
            getDataTracker().set(ATTACK,1+step++%3);getDataTracker().set(CHARGE,40);
            float yaw=(float)(Math.toDegrees(Math.atan2(facing.z,facing.x))-90);setYaw(yaw);setBodyYaw(yaw);setHeadYaw(yaw);
            for(var p:players) p.sendMessage(net.minecraft.text.Text.translatable("regent.myfirstmod.attack."+attack()),true);
        }
        int left=charge()-1;getDataTracker().set(CHARGE,left);
        if(left%4==0) {
            if(attack()==1) {double angle=Math.atan2(facing.z,facing.x);for(int i=-5;i<=5;i++) point(w,getPos().add(Math.cos(angle+i*.22)*5,.15,Math.sin(angle+i*.22)*5));}
            if(attack()==2) for(double r:new double[]{2.5,7.5}) for(int i=0;i<32;i++) point(w,getPos().add(Math.cos(i*Math.PI/16)*r,.15,Math.sin(i*Math.PI/16)*r));
            if(attack()==3) for(int i=-6;i<=6;i++) {point(w,mark.add(i,.15,0));point(w,mark.add(0,.15,i));}
        }
        if(left>0) return;
        for(var p:players) {
            var d=p.getPos().subtract(getPos());var m=p.getPos().subtract(mark);
            boolean hit=switch(attack()) {case 1->KeepRules.cleave(d.horizontalLengthSquared(),d.multiply(1,0,1).normalize().dotProduct(facing),d.y);case 2->KeepRules.ring(d.horizontalLength(),d.y);default->KeepRules.cross(m.x,m.z,m.y);};
            if(hit&&canSee(p)) p.damage(w.getDamageSources().mobAttack(this),getHealth()<getMaxHealth()/2?12:10);
        }
        w.spawnParticles(ParticleTypes.SOUL,getX(),getY()+1,getZ(),35,1.5,1,1.5,.03);
        playSound(net.minecraft.sound.SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,1,.6f);
        getDataTracker().set(ATTACK,0);rest=getHealth()<getMaxHealth()/2?45:70;
    }
    private void point(ServerWorld w,Vec3d p) {w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y,p.z,1,0,0,0,0);}
}
