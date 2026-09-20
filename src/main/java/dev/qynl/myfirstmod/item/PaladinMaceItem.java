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

public class PaladinMaceItem extends Item {
    public PaladinMaceItem(Settings settings) {
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

            // Holy Radiant Burst on enemies
            List<LivingEntity> enemies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(10.0),
                    e -> e != player && e.isAlive() && FactionManager.isHostile(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (LivingEntity enemy : enemies) {
                float smiteDmg = enemy.isUndead() ? 18.0f : 10.0f;
                enemy.damage(serverWorld.getDamageSources().magic(), smiteDmg);
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, enemy.getX(), enemy.getY() + 1.0, enemy.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
            }

            // Shielding on allies
            List<LivingEntity> allies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(12.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (LivingEntity ally : allies) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 300, 1));
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0));
                serverWorld.spawnParticles(ParticleTypes.ENCHANTED_HIT, ally.getX(), ally.getBodyY(0.6), ally.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            }

            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.2f, 1.4f);
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_TRIDENT_THUNDER.value(), SoundCategory.PLAYERS, 1.0f, 1.2f);

            serverPlayer.sendMessage(
                    Text.literal("⚡ Sunforge Smite: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("Smote " + enemies.size() + " foes with holy light!").formatted(Formatting.WHITE)),
                    true
            );

            player.getItemCooldownManager().set(this, 100); // 5s cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Sunforge Paladin Warhammer").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Unleash Holy Radiant Smite").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Deals bonus holy damage to Undead + grants Absorption to allies").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Cooldown: 5s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
