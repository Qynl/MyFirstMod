package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.territory.TerritoryManager;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class FactionBannerItem extends Item {
    public FactionBannerItem(Settings settings) {
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
            Faction faction = data.factions.get(factionId);

            if (world.isAir(pos) || world.getBlockState(pos).isReplaceable()) {
                world.setBlockState(pos, Blocks.BLUE_BANNER.getDefaultState(), Block.NOTIFY_ALL);
                TerritoryManager.registerOutpost(factionId, pos);

                // Ascending banner pillar
                dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnOrbitalLightPillar(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 14.0, ParticleTypes.END_ROD, ParticleTypes.HAPPY_VILLAGER);
                dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(world, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, "🚩 OUTPOST ESTABLISHED", Formatting.GOLD);

                // 32-Block Territory Perimeter Ring preview
                for (int i = 0; i < 48; i++) {
                    double angle = (2 * Math.PI * i) / 48.0;
                    double bx = pos.getX() + 0.5 + Math.cos(angle) * 32.0;
                    double bz = pos.getZ() + 0.5 + Math.sin(angle) * 32.0;
                    world.spawnParticles(ParticleTypes.PORTAL, bx, pos.getY() + 0.5, bz, 2, 0.1, 0.2, 0.1, 0.02);
                    if (i % 4 == 0) {
                        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, bx, pos.getY() + 0.8, bz, 2, 0.1, 0.2, 0.1, 0.02);
                    }
                }

                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.4f, 1.2f);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.8f, 1.4f);

                int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
                String fName = faction != null ? faction.name : "Faction";

                serverPlayer.sendMessage(
                        Text.literal("🚩 Faction Outpost Established: ").formatted(Formatting.GOLD, Formatting.BOLD)
                                .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb))))
                                .append(Text.literal(" territory claim active in 32 block radius.").formatted(Formatting.GRAY)),
                        false
                );
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Faction Outpost Standard").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click on Block: ").formatted(Formatting.YELLOW).append(Text.literal("Plant Faction Outpost").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Grants Home Turf buffs to defending troops").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Territory Radius: 32 blocks").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
