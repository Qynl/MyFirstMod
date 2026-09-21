package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.world.World;

import java.util.List;

public class HealingStaffItem extends Item {
    public HealingStaffItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld serverWorld = serverPlayer.getServerWorld();
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

            String equippedId = data.getEquippedUnit(player.getUuid());
            var unit = data.units.get(equippedId);
            String factionId = unit != null ? unit.factionId : "kingdom";

            // Find all allied units in 16 blocks
            List<LivingEntity> allies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(16.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            // Expanding radiant healing ring VFX: Celestial Restoration Wave
            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnOrbitalLightPillar(serverWorld, player.getX(), player.getY(), player.getZ(), 7.0, ParticleTypes.END_ROD, ParticleTypes.HEART);
            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnExpandingShockwave(serverWorld, player.getX(), player.getY(), player.getZ(), 16.0, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.HEART);

            for (LivingEntity ally : allies) {
                ally.heal(8.0f);
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1));
                ally.removeStatusEffect(StatusEffects.POISON);
                ally.removeStatusEffect(StatusEffects.WITHER);
                ally.removeStatusEffect(StatusEffects.SLOWNESS);

                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnHeal(serverWorld, ally.getX(), ally.getBodyY(0.7), ally.getZ(), 8.0f);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(serverWorld, ally.getX(), ally.getBodyY(1.1), ally.getZ(), "💖 RESTORED!", Formatting.GREEN);

                serverWorld.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 0.5, ally.getZ(), 10, 0.4, 0.5, 0.4, 0.05);
            }

            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.4f, 1.3f);
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.6f);

            serverPlayer.sendMessage(
                    Text.literal("💖 Divine Restoration: ").formatted(Formatting.GREEN, Formatting.BOLD)
                            .append(Text.literal("Healed " + allies.size() + " allied warriors.").formatted(Formatting.WHITE)),
                    true
            );

            player.getItemCooldownManager().set(this, 100); // 5 second cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Grand Healer's Divine Staff").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Cast massive restoration wave (8 HP + Regen II)").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Cleanses all negative debuffs in 16 blocks").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Cooldown: 5s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
