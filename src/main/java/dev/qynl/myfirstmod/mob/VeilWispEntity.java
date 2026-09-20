package dev.qynl.myfirstmod.mob;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
/** A drifting highland mote: no gravity, slow sine wander, harmless and unbreedable. */
public final class VeilWispEntity extends AnimalEntity {
    public VeilWispEntity(EntityType<? extends AnimalEntity> type,World world){super(type,world);setNoGravity(true);}
    public static DefaultAttributeContainer.Builder attributes(){return LivingEntity.createLivingAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH,6).add(EntityAttributes.GENERIC_MOVEMENT_SPEED,.08);}
    @Override public boolean isBreedingItem(ItemStack stack){return false;}
    @Override public PassiveEntity createChild(ServerWorld world,PassiveEntity other){return null;}
    @Override public boolean canImmediatelyDespawn(double distance){return false;}
    @Override public void tick(){
        super.tick();
        setVelocity(Math.sin(age*.05)*.05,Math.sin(age*.03)*.02,Math.cos(age*.04)*.05);
    }
}
