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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UnitCreatorItem extends Item {
    private static final Map<UUID, String> PLAYER_SPAWN_FACTIONS = new ConcurrentHashMap<>();

    public UnitCreatorItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            UnitWorldData data = UnitWorldData.get(serverPlayer.getServer());

            if (player.isSneaking()) {
                // Shift + Right-Click: Cycle Active Solo Spawn Faction
                List<String> fKeys = new ArrayList<>(data.factions.keySet());
                if (!fKeys.isEmpty()) {
                    String currentF = PLAYER_SPAWN_FACTIONS.getOrDefault(player.getUuid(), "kingdom");
                    int idx = fKeys.indexOf(currentF);
                    String nextF = fKeys.get((idx + 1) % fKeys.size());
                    PLAYER_SPAWN_FACTIONS.put(player.getUuid(), nextF);

                    Faction faction = data.factions.get(nextF);
                    int colorRgb = faction != null ? faction.getParsedColor() : 0x3B82F6;
                    String fName = faction != null ? faction.name : nextF;

                    serverPlayer.sendMessage(
                            Text.literal("⚔ Solo Spawn Faction: ").formatted(Formatting.GOLD, Formatting.BOLD)
                                    .append(Text.literal(fName).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorRgb)).withBold(true))),
                            true // Action bar
                    );
                    dev.qynl.myfirstmod.visual.FloatingCombatText.spawnStatus(serverPlayer.getServerWorld(), player.getX(), player.getY() + 1.2, player.getZ(), "🚩 " + fName.toUpperCase(), Formatting.GOLD);
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 1.5f);
                }
            } else {
                // Right-Click: Open Full Creator & Sliders Dashboard
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
            UnitDefinition soloUnit = new UnitDefinition(unit.toNbt());
            String chosenFaction = PLAYER_SPAWN_FACTIONS.get(player.getUuid());
            if (chosenFaction != null && data.factions.containsKey(chosenFaction)) {
                soloUnit.factionId = chosenFaction;
            }
            return UnitSpawner.spawnAtPlayer(player, soloUnit);
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
            UnitDefinition soloUnit = new UnitDefinition(unit.toNbt());
            String chosenFaction = PLAYER_SPAWN_FACTIONS.get(player.getUuid());
            if (chosenFaction != null && data.factions.containsKey(chosenFaction)) {
                soloUnit.factionId = chosenFaction;
            }
            return UnitSpawner.spawnSquadAtPlayer(player, soloUnit, 5);
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Solo Sandbox Army Commander").formatted(Formatting.GOLD, Formatting.BOLD));
        tooltip.add(Text.literal("Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Open Sliders & Creator Dashboard").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Right-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Cycle Active Spawn Faction").formatted(Formatting.AQUA)));
        tooltip.add(Text.literal("Left-Click on Ground: ").formatted(Formatting.YELLOW).append(Text.literal("Spawn Unit for Active Faction").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Shift + Left-Click: ").formatted(Formatting.YELLOW).append(Text.literal("Spawn Squad (5 Units) for Active Faction").formatted(Formatting.GRAY)));
        tooltip.add(Text.literal("Right-Click Unit: ").formatted(Formatting.YELLOW).append(Text.literal("Inspect Unit Dossier").formatted(Formatting.GRAY)));
        super.appendTooltip(stack, context, tooltip, type);
    }
}
