package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.kinetics.KineticsConfig;
import dev.qynl.myfirstmod.kinetics.KineticsManager;
import dev.qynl.myfirstmod.kinetics.KineticsState;
import dev.qynl.myfirstmod.kinetics.NullhookEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * The Nullhook — a wrist-mounted void grapple.
 *
 * <p>Right-click fires the hook; right-click again (or sneak) while hooked
 * releases it. The pull itself is simulated on the server by
 * {@link NullhookEntity} and mirrored by the client for zero-lag feel.
 */
public class NullhookItem extends Item {
    public NullhookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        // Second click releases an active hook.
        if (world instanceof ServerWorld serverWorld && user instanceof ServerPlayerEntity player) {
            KineticsState state = KineticsManager.getState(player);
            if (state.grappleEntityId >= 0) {
                Entity hook = serverWorld.getEntityById(state.grappleEntityId);
                if (hook instanceof NullhookEntity nullhook) {
                    nullhook.startRetracting();
                    user.getItemCooldownManager().set(this, 6);
                    return TypedActionResult.success(stack);
                }
                KineticsManager.handleGrappleEnd(player);
            }

            if (fireHook(serverWorld, player, state)) {
                user.getItemCooldownManager().set(this, 8);
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.fail(stack);
        }

        // Client: assume success for a snappy swing; the server decides for real.
        return TypedActionResult.success(stack, world.isClient);
    }

    private boolean fireHook(ServerWorld world, ServerPlayerEntity player, KineticsState state) {
        KineticsConfig cfg = KineticsConfig.get();
        if (!cfg.enabled || !cfg.grappleEnabled) return false;
        if (!dev.qynl.myfirstmod.kinetics.KineticsWorldUtil.canUseKinetics(player)) return false;
        if (player.getAbilities().flying) return false;
        if (state.energy < cfg.grappleEnergy) {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                    SoundCategory.PLAYERS, 0.5F, 0.55F);
            return false;
        }

        state.energy -= cfg.grappleEnergy;
        state.regenDelayTicks = Math.max(state.regenDelayTicks, cfg.energyRegenDelay);
        state.markDirty();

        NullhookEntity hook = new NullhookEntity(world, player);
        world.spawnEntity(hook);

        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW,
                SoundCategory.PLAYERS, 0.7F, 1.5F);
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.myfirstmod.nullhook.tooltip.1").formatted(Formatting.DARK_AQUA));
        tooltip.add(Text.translatable("item.myfirstmod.nullhook.tooltip.2").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("item.myfirstmod.nullhook.tooltip.3").formatted(Formatting.DARK_GRAY));
    }
}
