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

                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.5, 0.8, 0.5, 0.05);
                world.spawnParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.4, 0.4, 0.4, 0.1);
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.2f);

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
