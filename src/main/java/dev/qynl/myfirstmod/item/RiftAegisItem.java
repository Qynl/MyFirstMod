package dev.qynl.myfirstmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Short, deliberate parry pulse. Only hostile-owned projectiles are reflected. */
public final class RiftAegisItem extends Item {
    public RiftAegisItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        ItemStack stack=player.getStackInHand(hand);
        if(player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            player.getItemCooldownManager().set(this,RelicAttunements.cooldown(stack,240));
            boolean reflected=false;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE,60,1));
            var look=player.getRotationVec(1);
            for(ProjectileEntity shot:server.getEntitiesByClass(ProjectileEntity.class,player.getBoundingBox().expand(5),
                    e->e.getOwner() instanceof HostileEntity && e.squaredDistanceTo(player)<25 && player.canSee(e))) {
                var direction=shot.getPos().subtract(player.getEyePos()).normalize();
                if(direction.dotProduct(look)<0) continue;
                reflected=true;shot.setOwner(player);
                shot.setVelocity(look.multiply(Math.max(1.5,Math.min(3,shot.getVelocity().length()))));
                shot.velocityModified=true;
            }
            RelicAttunements.afterCast(player,stack,reflected);
            stack.damage(2,player,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            server.spawnParticles(ParticleTypes.ELECTRIC_SPARK,player.getX(),player.getY()+1,player.getZ(),50,1,.8,1,.04);
            server.playSound(null,player.getBlockPos(),SoundEvents.ITEM_SHIELD_BLOCK,SoundCategory.PLAYERS,1.2f,.7f);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
