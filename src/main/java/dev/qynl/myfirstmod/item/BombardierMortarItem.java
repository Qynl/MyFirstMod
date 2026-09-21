package dev.qynl.myfirstmod.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.TntEntity;
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

public class BombardierMortarItem extends Item {
    public BombardierMortarItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getServerWorld();
            Vec3d look = player.getRotationVector();

            TntEntity tnt = new TntEntity(serverWorld, player.getX() + look.x, player.getEyeY() + look.y, player.getZ() + look.z, player);
            tnt.setVelocity(look.x * 1.6, look.y * 1.6 + 0.25, look.z * 1.6);
            tnt.setFuse(30); // 1.5 second air-burst fuse
            serverWorld.spawnEntity(tnt);

            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnDirectionalCone(serverWorld, player.getEyePos(), look, 3.0, 30.0, ParticleTypes.FLAME, 25);
            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnDirectionalCone(serverWorld, player.getEyePos(), look, 4.0, 35.0, ParticleTypes.CAMPFIRE_COSY_SMOKE, 20);
            serverWorld.spawnParticles(ParticleTypes.LAVA, player.getX() + look.x, player.getEyeY(), player.getZ() + look.z, 6, 0.2, 0.2, 0.2, 0.05);

            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.4f, 1.5f);

            player.getItemCooldownManager().set(this, 60); // 3s cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Handheld Siege Mortar").formatted(Formatting.RED, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Fire Ballistic Airburst Mortar Shell").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Cracks enemy lines with heavy explosive splash damage").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Cooldown: 3s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
