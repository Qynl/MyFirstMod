package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class FactionScepterItem extends Item {
    public FactionScepterItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getWorld().isClient) return ActionResult.SUCCESS;

        ServerWorld world = (ServerWorld) context.getWorld();
        BlockPos pos = context.getBlockPos().offset(context.getSide());

        if (player instanceof ServerPlayerEntity serverPlayer) {
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());
            String equippedId = data.getEquippedUnit(player.getUuid());
            var unit = data.units.get(equippedId);
            String factionId = unit != null ? unit.factionId : "kingdom";

            // Spawn waypoint marker particles & sound
            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 25, 0.4, 0.8, 0.4, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.6, 0.3, 0.08);
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 1.6f);

            // Command nearby troops to march to this waypoint
            List<MobEntity> troops = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(36.0),
                    e -> e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (MobEntity troop : troops) {
                troop.getNavigation().startMovingTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.2);
                world.spawnParticles(ParticleTypes.PORTAL, troop.getX(), troop.getY() + 0.5, troop.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
            }

            serverPlayer.sendMessage(
                    Text.literal("📍 Tactical Waypoint Set: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal(troops.size() + " allied troops marching to position.").formatted(Formatting.AQUA)),
                    true // Action bar
            );
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            ServerWorld world = serverPlayer.getServerWorld();
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

            String equippedId = data.getEquippedUnit(player.getUuid());
            var unit = data.units.get(equippedId);
            String factionId = unit != null ? unit.factionId : "kingdom";

            // Mark target with glowing and focus fire
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0, false, false));
            world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.5), target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
            world.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.8f, 1.8f);

            List<MobEntity> troops = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(36.0),
                    e -> e != target && e.isAlive() && FactionManager.isAllied(serverPlayer.getServer(), factionId, UnitSystem.getTagValue(e, "faction:")));

            for (MobEntity troop : troops) {
                troop.setTarget(target);
                troop.getNavigation().startMovingTo(target, 1.35);
            }

            serverPlayer.sendMessage(
                    Text.literal("🎯 Priority Target Designated: ").formatted(Formatting.RED, Formatting.BOLD)
                            .append(Text.literal(target.getName().getString()).formatted(Formatting.WHITE, Formatting.BOLD))
                            .append(Text.literal(" (" + troops.size() + " troops focusing target)").formatted(Formatting.YELLOW)),
                    true // Action bar
            );
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Faction Commander's Scepter").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click on Block: ").formatted(Formatting.YELLOW).append(Text.literal("Set Tactical Waypoint").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Right-Click on Enemy: ").formatted(Formatting.RED).append(Text.literal("Designate Focus Target").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Command Radius: 36 blocks").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
