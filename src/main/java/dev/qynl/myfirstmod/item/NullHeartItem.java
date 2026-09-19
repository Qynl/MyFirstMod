package dev.qynl.myfirstmod.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** A renewable defensive reward, not a decorative trophy. */
public final class NullHeartItem extends Item {
    public NullHeartItem(Settings settings) {super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        ItemStack stack=player.getStackInHand(hand);
        if(player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(world instanceof ServerWorld server) {
            player.getItemCooldownManager().set(this,900);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,100,1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,240,1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,160,0));
            server.spawnParticles(ParticleTypes.SOUL,player.getX(),player.getY()+1,player.getZ(),40,.6,.6,.6,.02);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
