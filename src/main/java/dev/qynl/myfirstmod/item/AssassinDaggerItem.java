package dev.qynl.myfirstmod.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public class AssassinDaggerItem extends Item {
    public AssassinDaggerItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getServerWorld();
            Vec3d look = player.getRotationVector();
            Vec3d targetPos = player.getPos().add(look.x * 6.0, 0.0, look.z * 6.0);

            // Shadow step blink
            serverWorld.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.3, 0.5, 0.3, 0.1);
            player.teleport(serverWorld, targetPos.x, targetPos.y, targetPos.z, Set.of(), player.getYaw(), player.getPitch());
            serverWorld.spawnParticles(ParticleTypes.SMOKE, targetPos.x, targetPos.y + 0.5, targetPos.z, 20, 0.3, 0.5, 0.3, 0.05);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 80, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 2));

            serverWorld.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.4f);

            player.getItemCooldownManager().set(this, 80); // 4s cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Shadowfang Obsidian Dagger").formatted(Formatting.DARK_PURPLE, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Shadow Step Blink (6 Blocks)").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Grants Invisibility & Speed III for ambush critical strikes").formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.literal("Cooldown: 4s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
