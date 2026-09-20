package dev.qynl.myfirstmod.kingdom;
import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.boss.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
/** Altar-bound keeper. Snapshot tells, visible recovery and a second, rotated crown pulse below half health. */
public final class RootboundPriorEntity extends HostileEntity {
    private static final TrackedData<Integer> ATTACK=DataTracker.registerData(RootboundPriorEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CHARGE=DataTracker.registerData(RootboundPriorEntity.class,TrackedDataHandlerRegistry.INTEGER);
    private final ServerBossBar bar=new ServerBossBar(Text.translatable("entity.myfirstmod.rootbound_prior"),BossBar.Color.GREEN,BossBar.Style.PROGRESS);
    private BlockPos heart;private int rest=60,step,absent;private boolean enraged;
    private Vec3d aim=new Vec3d(0,0,1);private final List<Vec3d> seeds=new ArrayList<>();
    public RootboundPriorEntity(EntityType<? extends HostileEntity> type,World world){super(type,world);}
    public static DefaultAttributeContainer.Builder attributes(){return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,240).add(EntityAttributes.GENERIC_ARMOR,6).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,1).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,0);}
    @Override protected void initDataTracker(DataTracker.Builder b){super.initDataTracker(b);b.add(ATTACK,0);b.add(CHARGE,0);}
    public int attack(){return getDataTracker().get(ATTACK);}public int charge(){return getDataTracker().get(CHARGE);}
    public void bind(BlockPos pos){heart=pos.toImmutable();}
    @Override public boolean canImmediatelyDespawn(double d){return false;}
    @Override public void writeCustomDataToNbt(NbtCompound n){super.writeCustomDataToNbt(n);if(heart!=null)n.putLong("MonasteryHeart",heart.asLong());}
    @Override public void readCustomDataFromNbt(NbtCompound n){super.readCustomDataFromNbt(n);if(n.contains("MonasteryHeart"))heart=BlockPos.fromLong(n.getLong("MonasteryHeart"));}
    @Override public void tick(){
        super.tick();if(!(getWorld() instanceof ServerWorld w)||!isAlive())return;
        if(heart!=null){
            var owner=RealmState.get(w).monasteryGuardians.get(heart.asLong());
            if(!getUuid().equals(owner)||!w.getBlockState(heart).isOf(ModBlocks.ROOT_HEART)){bar.clearPlayers();discard();return;}
            setPosition(heart.getX()+.5,heart.getY()+1,heart.getZ()+.5);setVelocity(Vec3d.ZERO);
        }
        var players=w.getPlayers().stream().filter(p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&squaredDistanceTo(p)<24*24).toList();
        for(var p:new ArrayList<>(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);
        for(var p:players)bar.addPlayer(p);bar.setPercent(Math.max(0,getHealth()/getMaxHealth()));
        if(players.isEmpty()){
            getDataTracker().set(ATTACK,0);getDataTracker().set(CHARGE,0);rest=60;
            if(heart!=null&&++absent>=600){RealmState.get(w).monasteryGuardians.remove(heart.asLong());RealmState.get(w).markDirty();bar.clearPlayers();discard();}return;
        }
        absent=0;
        if(attack()==0){
            if(--rest>0)return;
            enraged=getHealth()<getMaxHealth()/2;
            var target=players.get(Math.floorMod(step,players.size()));aim=target.getPos().subtract(getPos()).multiply(1,0,1).normalize();
            seeds.clear();for(var p:players.stream().limit(4).toList())seeds.add(p.getPos());
            if(enraged){seeds.add(target.getPos().add(3,0,0));seeds.add(target.getPos().add(-3,0,0));}
            getDataTracker().set(ATTACK,1+step++%3);getDataTracker().set(CHARGE,60);
            setYaw((float)(Math.toDegrees(Math.atan2(aim.z,aim.x))-90));setBodyYaw(getYaw());setHeadYaw(getYaw());
        }
        int left=charge()-1;getDataTracker().set(CHARGE,left);
        boolean diagonal=attack()==2&&enraged&&left<20;
        bar.setName(Text.translatable("prior.myfirstmod.attack."+attack()));
        if(left%4==0){
            if(attack()==1)for(int i=0;i<=9;i++)for(double side:new double[]{-1.15,1.15})point(w,getPos().add(aim.x*i-aim.z*side,-.8,aim.z*i+aim.x*side));
            if(attack()==2)for(int i=3;i<=7;i++)for(int arm=0;arm<4;arm++){double a=arm*Math.PI/2+(diagonal?Math.PI/4:0);point(w,getPos().add(Math.cos(a)*i,-.8,Math.sin(a)*i));}
            if(attack()==3)for(var seed:seeds)for(int i=0;i<24;i++)point(w,seed.add(Math.cos(i*Math.PI/12)*2.2,.1,Math.sin(i*Math.PI/12)*2.2));
        }
        if(left==0||(attack()==2&&enraged&&left==20)){
            for(var p:players){
                var d=p.getPos().subtract(getPos());boolean hit=switch(attack()){
                    case 1->MonasteryRules.lane(d.x*aim.x+d.z*aim.z,-d.x*aim.z+d.z*aim.x,d.y);
                    case 2->MonasteryRules.crown(d.x,d.z,d.y,diagonal);
                    default->seeds.stream().anyMatch(s->MonasteryRules.seed(p.getX()-s.x,p.getZ()-s.z,p.getY()-s.y));};
                if(hit&&canSee(p))p.damage(w.getDamageSources().mobAttack(this),enraged?12:9);
            }
            w.spawnParticles(ParticleTypes.COMPOSTER,getX(),getY()+1,getZ(),40,1,1,1,.04);
            playSound(net.minecraft.sound.SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,1,.8f);
        }
        if(left==0){getDataTracker().set(ATTACK,0);rest=65;bar.setName(Text.translatable("prior.myfirstmod.recovery"));}
    }
    private void point(ServerWorld w,Vec3d p){w.spawnParticles(ParticleTypes.HAPPY_VILLAGER,p.x,p.y,p.z,1,0,0,0,0);}
    @Override public void onDeath(DamageSource source){
        if(getWorld() instanceof ServerWorld w&&heart!=null){
            var data=RealmState.get(w);var state=w.getBlockState(heart);
            if(getUuid().equals(data.monasteryGuardians.get(heart.asLong()))&&state.isOf(ModBlocks.ROOT_HEART)){
                w.setBlockState(heart,ModBlocks.ROOT_RELIQUARY.getDefaultState().with(MonasteryBlock.FACING,state.get(MonasteryBlock.FACING)),net.minecraft.block.Block.NOTIFY_ALL);
                data.monasteryGuardians.remove(heart.asLong());data.markDirty();
                for(int x=-1;x<=1;x++)for(int y=0;y<3;y++){
                    var p=Monastery.relative(heart,state.get(MonasteryBlock.FACING),x,y,-17);
                    if(w.getBlockState(p).isOf(net.minecraft.block.Blocks.IRON_BARS))w.setBlockState(p,net.minecraft.block.Blocks.AIR.getDefaultState(),net.minecraft.block.Block.NOTIFY_ALL);
                }
            }
        }
        bar.clearPlayers();super.onDeath(source);
    }
    @Override public void remove(RemovalReason reason){bar.clearPlayers();super.remove(reason);}
}
