package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import java.util.Optional;

public final class ArenaCompassItem extends CompassItem {
    public ArenaCompassItem(Settings settings) { super(settings); }
    public static ItemStack create() {
        ItemStack stack=new ItemStack(ModItems.ARENA_COMPASS);
        bind(stack);
        return stack;
    }
    public static void bind(ItemStack stack) {
        stack.set(DataComponentTypes.LODESTONE_TRACKER,new LodestoneTrackerComponent(
                Optional.of(GlobalPos.create(VoidPortalManager.NULL_REALM,new BlockPos(0,80,0))),false));
    }
    @Override public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack=player.getStackInHand(hand);
        if(!world.isClient) {
            bind(stack);
            player.sendMessage(world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)
                    ? Text.translatable("message.myfirstmod.compass_distance",(int)Math.hypot(player.getX(),player.getZ()))
                    : Text.translatable("message.myfirstmod.compass_dormant"),true);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
