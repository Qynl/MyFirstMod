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

public class DruidStaffItem extends Item {
    public DruidStaffItem(Settings settings) {
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
            String factionId = unit != null ? unit.factionId : "villagers";

            // Root and entangle hostile enemies
            List<LivingEntity> enemies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(14.0),
                    e -> e != player && e.isAlive() && FactionManager.isHostile(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            // Nature bloom ring particles: Expanding Triple Floral Array
            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnArcaneCircle(serverWorld, player.getX(), player.getY(), player.getZ(), 8.0, ParticleTypes.COMPOSTER, ParticleTypes.HAPPY_VILLAGER);
            dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnExpandingShockwave(serverWorld, player.getX(), player.getY(), player.getZ(), 14.0, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.COMPOSTER);

            for (LivingEntity enemy : enemies) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 4));
                enemy.damage(serverWorld.getDamageSources().magic(), 6.0f);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnDamage(serverWorld, enemy.getX(), enemy.getBodyY(0.75), enemy.getZ(), 6.0f, false);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(serverWorld, enemy.getX(), enemy.getBodyY(1.1), enemy.getZ(), "🌿 ENTANGLED!", Formatting.GREEN);
                // Entangling roots bursting from floor around enemy
                for (double dy = 0; dy <= 1.5; dy += 0.3) {
                    serverWorld.spawnParticles(ParticleTypes.COMPOSTER, enemy.getX(), enemy.getY() + dy, enemy.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
                }
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, enemy.getX(), enemy.getY() + 0.5, enemy.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
            }

            // Heal surrounding allies
            List<LivingEntity> allies = serverWorld.getEntitiesByClass(LivingEntity.class, player.getBoundingBox().expand(14.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (LivingEntity ally : allies) {
                ally.heal(6.0f);
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1));
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnHeal(serverWorld, ally.getX(), ally.getBodyY(0.7), ally.getZ(), 6.0f);
            }

            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.8f);
            serverWorld.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.3f);

            serverPlayer.sendMessage(
                    Text.literal("🌿 Nature's Grasp: ").formatted(Formatting.GREEN, Formatting.BOLD)
                            .append(Text.literal("Entangled " + enemies.size() + " foes, healed " + allies.size() + " allies.").formatted(Formatting.WHITE)),
                    true
            );

            player.getItemCooldownManager().set(this, 120); // 6s cooldown
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Oakbound Druidic Staff").formatted(Formatting.DARK_GREEN, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Cast Entangling Roots & Healing Surge").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Roots enemies with Slowness V & heals nature allies").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Cooldown: 6s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
