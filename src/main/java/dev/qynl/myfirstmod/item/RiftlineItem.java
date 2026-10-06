package dev.qynl.myfirstmod.item;

import java.util.List;

import dev.qynl.myfirstmod.arsenal.RiftAnchorEntity;
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
 * The Riftline: a void grapple woven from echo shards.
 *
 * <p>Use to hurl a rift anchor. If it bites into a block, the void reels you
 * toward it with momentum you keep on release — recast mid-flight to chain
 * swings, sneak to cut the line. If it strikes a creature, the pull reverses
 * and drags <em>them</em> to <em>you</em>.
 */
public class RiftlineItem extends Item {
    private static final int CAST_COOLDOWN_TICKS = 8;
    private static final float ANCHOR_SPEED = 3.0F;

    public RiftlineItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        user.getItemCooldownManager().set(this, CAST_COOLDOWN_TICKS);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 0.9F, 0.55F);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1.0F, 0.6F);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            RiftAnchorEntity anchor = new RiftAnchorEntity(world, serverPlayer);
            anchor.setVelocity(serverPlayer, serverPlayer.getPitch(), serverPlayer.getYaw(),
                    0.0F, ANCHOR_SPEED, 0.0F);
            world.spawnEntity(anchor);
            stack.damage(1, serverPlayer,
                    hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.myfirstmod.riftline.tip0").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.riftline.tip1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.riftline.tip2").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.riftline.tip3")
                .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }
}
