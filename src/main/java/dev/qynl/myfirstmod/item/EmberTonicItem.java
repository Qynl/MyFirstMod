package dev.qynl.myfirstmod.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public final class EmberTonicItem extends Item {
    public EmberTonicItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand) {
        user.setCurrentHand(hand);return TypedActionResult.consume(user.getStackInHand(hand));
    }
    @Override public int getMaxUseTime(ItemStack stack,LivingEntity user) {return 32;}
    @Override public UseAction getUseAction(ItemStack stack) {return UseAction.DRINK;}
    @Override public ItemStack finishUsing(ItemStack stack,World world,LivingEntity user) {
        if(!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,1200,0));
            if(user instanceof PlayerEntity player && !player.isCreative()) {
                stack.decrement(1);
                if(stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return stack;
    }
}
