package dev.qynl.myfirstmod.item;

import java.util.List;

import dev.qynl.myfirstmod.arsenal.GraspManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Grasp of the Null: a sculk-veined gauntlet of borrowed Warden strength.
 *
 * <p>Use to seize the creature you are looking at (sneak-use to rip a block
 * out of the world instead) and suspend it in front of you. Use again to hurl
 * it — impacts hit harder the faster the victim was flying, thrown blocks land
 * and place themselves, and thrown creepers arrive primed. Sneak-use while
 * holding to set the victim down gently, if mercy is your thing.
 */
public class NullGraspItem extends Item {
    private static final int THROW_COOLDOWN_TICKS = 14;
    private static final int GRAB_COOLDOWN_TICKS = 10;
    private static final int DROP_COOLDOWN_TICKS = 6;
    private static final int WHIFF_COOLDOWN_TICKS = 5;

    public NullGraspItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (world.isClient) {
            return TypedActionResult.success(stack, true);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        if (GraspManager.isHolding(player)) {
            if (player.isSneaking()) {
                GraspManager.releaseGently(player);
                user.getItemCooldownManager().set(this, DROP_COOLDOWN_TICKS);
            } else {
                GraspManager.throwHeld(player);
                stack.damage(1, player,
                        hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                user.getItemCooldownManager().set(this, THROW_COOLDOWN_TICKS);
            }
            return TypedActionResult.success(stack, false);
        }

        boolean grabbed = player.isSneaking()
                ? GraspManager.tryGrabBlock(player)
                : GraspManager.tryGrabEntity(player);
        if (grabbed) {
            user.getItemCooldownManager().set(this, GRAB_COOLDOWN_TICKS);
            return TypedActionResult.success(stack, false);
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_SCULK_SENSOR_CLICKING,
                SoundCategory.PLAYERS, 0.5F, 1.8F);
        user.getItemCooldownManager().set(this, WHIFF_COOLDOWN_TICKS);
        return TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.myfirstmod.null_grasp.tip0").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.null_grasp.tip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.null_grasp.tip2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.null_grasp.tip3")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }
}
