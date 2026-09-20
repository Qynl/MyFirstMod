package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
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
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ReinforcementBeaconItem extends Item {
    public ReinforcementBeaconItem(Settings settings) {
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

            List<UnitDefinition> factionUnits = data.units.values().stream().filter(u -> u.factionId.equalsIgnoreCase(factionId)).toList();
            if (factionUnits.isEmpty()) {
                factionUnits = data.units.values().stream().toList();
            }

            // Spawn 4 reinforcement units around target point
            Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            int count = Math.min(4, Math.max(1, factionUnits.size()));

            for (int i = 0; i < 4; i++) {
                UnitDefinition def = factionUnits.get(i % factionUnits.size());
                double ox = (i % 2 == 0 ? 1 : -1) * 1.5;
                double oz = (i / 2 == 0 ? 1 : -1) * 1.5;
                Vec3d sPos = center.add(ox, 0, oz);
                UnitSpawner.spawn(world, def, sPos, serverPlayer.getYaw());
            }

            // Epic visual lightning & beacon activation
            world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1.0, center.z, 2, 0.2, 0.2, 0.2, 0.0);
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, center.x, center.y + 1.5, center.z, 40, 0.8, 1.2, 0.8, 0.15);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1.4f, 1.2f);
            world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.0f);

            int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
            String fName = faction != null ? faction.name : "Army";

            serverPlayer.sendMessage(
                    Text.literal("⚡ Reinforcements Deployed: ").formatted(Formatting.GOLD, Formatting.BOLD)
                            .append(Text.literal("4 elite units of ").formatted(Formatting.GRAY))
                            .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb))))
                            .append(Text.literal(" have arrived!").formatted(Formatting.YELLOW)),
                    false
            );

            player.getItemCooldownManager().set(this, 200); // 10 second cooldown
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Reinforcement Drop Beacon").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click on Block: ").formatted(Formatting.YELLOW).append(Text.literal("Deploy 4-unit tactical reinforcement wave").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Cooldown: 10s").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
