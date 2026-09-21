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

public class BardLuteItem extends Item {
    public BardLuteItem(Settings settings) {
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
            String factionId = unit != null ? unit.factionId : "arcane";

            List<LivingEntity> allies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(18.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            // Harmonic musical wave particle rings
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                for (double r = 4.0; r <= 16.0; r += 4.0) {
                    double px = player.getX() + r * Math.cos(angle);
                    double pz = player.getZ() + r * Math.sin(angle);
                    serverWorld.spawnParticles(ParticleTypes.NOTE, px, player.getY() + 0.5, pz, 1, (i % 24) / 24.0, 0, 0, 0.5);
                }
            }

            for (LivingEntity ally : allies) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 240, 1));
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 240, 0));
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 0));
                serverWorld.spawnParticles(ParticleTypes.NOTE, ally.getX(), ally.getBodyY(0.7), ally.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
            }

            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1.8f, 1.2f);
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5f, 1.5f);

            serverPlayer.sendMessage(
                    Text.literal("🎶 Harmonic War Hymn: ").formatted(Formatting.AQUA, Formatting.BOLD)
                            .append(Text.literal("Inspired " + allies.size() + " allied warriors.").formatted(Formatting.WHITE)),
                    true
            );

            player.getItemCooldownManager().set(this, 100); // 5s cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Harmonic War Lute").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Strum Inspiring Battle Hymn").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Grants Speed II, Resistance I & Regeneration to allies in 18 blocks").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Cooldown: 5s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
