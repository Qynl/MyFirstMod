package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.gui.CreatorScreenHandler;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitInspector;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class UnitCreatorItem extends Item {
    public UnitCreatorItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            if (player.isSneaking()) {
                // Shift + Right-Click: Cycle equipped unit
                UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());
                String nextUnitId = data.cycleEquippedUnit(player.getUuid());
                UnitDefinition unit = data.units.get(nextUnitId);

                if (unit != null) {
                    Faction faction = data.factions.get(unit.factionId);
                    int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;

                    serverPlayer.sendMessage(
                            Text.literal("Equipped: ").formatted(Formatting.GRAY)
                                    .append(Text.literal(unit.name).formatted(Formatting.WHITE, Formatting.BOLD))
                                    .append(Text.literal(" [" + (faction != null ? faction.name : "Independent") + "]")
                                            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)))),
                            true // Action bar
                    );
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK, SoundCategory.PLAYERS, 0.6f, 1.2f);
                }
            } else {
                // Right-Click: Open Creator GUI
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, inv, p) -> new CreatorScreenHandler(syncId, inv),
                        Text.translatable("screen.myfirstmod.creator")
                ));
            }
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        if (!player.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            if (UnitSystem.getTagValue(entity, "unit:") != null) {
                UnitInspector.inspectUnit(serverPlayer, entity);
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }

    public static boolean spawnEquipped(ServerPlayerEntity player) {
        if (player.getServer() == null) return false;
        UnitWorldData data = UnitWorldData.get(player.getServer());
        String equippedId = data.getEquippedUnit(player.getUuid());
        UnitDefinition unit = data.units.get(equippedId);

        if (unit != null) {
            return UnitSpawner.spawnAtPlayer(player, unit);
        } else {
            player.sendMessage(Text.literal("No unit equipped! Right-click to open creator.").formatted(Formatting.RED), true);
            return false;
        }
    }

    public static boolean spawnSquadEquipped(ServerPlayerEntity player) {
        if (player.getServer() == null) return false;
        UnitWorldData data = UnitWorldData.get(player.getServer());
        String equippedId = data.getEquippedUnit(player.getUuid());
        UnitDefinition unit = data.units.get(equippedId);

        if (unit != null) {
            return UnitSpawner.spawnSquadAtPlayer(player, unit, 5);
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Unit & Faction Sandbox Tool").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Open Creator Dashboard").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Right-Click Unit: ").formatted(Formatting.YELLOW).append(Text.literal("Inspect Unit Dossier").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Left-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Spawn Equipped Unit").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Quick-Cycle Unit").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Left-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Spawn Squad (5 Units)").formatted(Formatting.GRAY)));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
