package dev.qynl.myfirstmod.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/** Dust-powered, wall-clipped precision beam; never hits pets or players. */
public final class PrismStaffItem extends Item {
    public PrismStaffItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        var stack=user.getStackInHand(hand);
        if(user.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            ItemStack fuel=ItemStack.EMPTY;
            for(int i=0;i<user.getInventory().size();i++) if(user.getInventory().getStack(i).isOf(ModItems.PRISM_DUST)) {
                fuel=user.getInventory().getStack(i);break;
            }
            if(fuel.isEmpty() && !user.isCreative()) {
                user.sendMessage(Text.translatable("message.myfirstmod.staff_fuel"),true);return TypedActionResult.fail(stack);
            }
            if(!user.isCreative()) fuel.decrement(1);
            var start=user.getEyePos();
            var end=server.raycast(new RaycastContext(start,start.add(user.getRotationVec(1).multiply(24)),
                    RaycastContext.ShapeType.COLLIDER,RaycastContext.FluidHandling.NONE,user)).getPos();
            HostileEntity closest=null;double best=start.squaredDistanceTo(end);
            for(var enemy:server.getEntitiesByClass(HostileEntity.class,new Box(start,end).expand(1),
                    e->e.isAlive() && !e.isTeammate(user))) {
                var bounds=enemy.getBoundingBox().expand(.15);
                var hit=bounds.contains(start)?java.util.Optional.of(start):bounds.raycast(start,end);
                if(hit.isPresent() && start.squaredDistanceTo(hit.get())<=best) {closest=enemy;best=start.squaredDistanceTo(hit.get());}
            }
            if(closest!=null) {
                if(closest.damage(server.getDamageSources().playerAttack(user),8))
                    closest.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING,100,0));
                end=start.add(end.subtract(start).normalize().multiply(Math.sqrt(best)));
            }
            var direction=end.subtract(start);
            int points=Math.min(48,(int)(direction.length()*2)+1);
            for(int i=0;i<=points;i++) {
                var point=start.add(direction.multiply(i/(double)points));
                server.spawnParticles(ParticleTypes.END_ROD,point.x,point.y,point.z,1,0,0,0,0);
            }
            user.getItemCooldownManager().set(this,30);
            stack.damage(1,user,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            server.playSound(null,user.getBlockPos(),SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,SoundCategory.PLAYERS,1,1.5f);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
