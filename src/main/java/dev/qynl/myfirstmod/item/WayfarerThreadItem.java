package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.Waystones;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public final class WayfarerThreadItem extends Item {
    public WayfarerThreadItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || player.getItemCooldownManager().isCoolingDown(this))
            return TypedActionResult.fail(stack);
        if(player instanceof ServerPlayerEntity server && (Waystones.combatLocked(server) || player.hurtTime>0)) {
            player.sendMessage(Text.translatable("message.myfirstmod.recall_locked"),true);return TypedActionResult.fail(stack);
        }
        player.setCurrentHand(hand);return TypedActionResult.consume(stack);
    }
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user) {return 60;}
    @Override public UseAction getUseAction(ItemStack stack) {return UseAction.BOW;}
    @Override public void usageTick(World world,LivingEntity user,ItemStack stack,int remaining) {
        if(user.hurtTime>0) {user.stopUsingItem();return;}
        if(world instanceof ServerWorld server && remaining%10==0)
            server.spawnParticles(ParticleTypes.REVERSE_PORTAL,user.getX(),user.getY()+1,user.getZ(),6,.4,.6,.4,.02);
    }
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user) {
        if(user instanceof ServerPlayerEntity player) {
            boolean success=player.hurtTime==0 && Waystones.recall(player);
            if(success) player.getItemCooldownManager().set(this,1200);
            player.sendMessage(Text.translatable(success?"message.myfirstmod.recalled":"message.myfirstmod.recall_blocked"),true);
        }
        return stack;
    }
}
