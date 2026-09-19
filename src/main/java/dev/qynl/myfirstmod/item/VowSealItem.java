package dev.qynl.myfirstmod.item;
import dev.qynl.myfirstmod.remembrance.PilgrimVows;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
public final class VowSealItem extends Item {
    private final int vow;
    public VowSealItem(Settings settings,int vow) {super(settings);this.vow=vow;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(player instanceof ServerPlayerEntity server && PilgrimVows.choose(server,vow)) {
            if(!player.isCreative()) stack.decrement(1);
            return TypedActionResult.success(stack,false);
        }
        return world.isClient?TypedActionResult.success(stack,true):TypedActionResult.fail(stack);
    }
}
