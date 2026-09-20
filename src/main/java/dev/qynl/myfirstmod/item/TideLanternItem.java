package dev.qynl.myfirstmod.item;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.World;
/** A drowned-archive relic: personal water breathing and dark-water sight on a long cooldown. */
public final class TideLanternItem extends Item {
    public TideLanternItem(Settings settings){super(settings);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        var stack=player.getStackInHand(hand);
        if(player.getItemCooldownManager().isCoolingDown(this))return TypedActionResult.fail(stack);
        if(!world.isClient){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING,800,0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,400,0));
            player.getItemCooldownManager().set(this,1800);
            world.playSound(null,player.getBlockPos(),SoundEvents.ENTITY_PLAYER_SPLASH,SoundCategory.PLAYERS,1,.7f);
        }
        return TypedActionResult.success(stack);
    }
}
