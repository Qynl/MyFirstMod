package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.command.UnitCommands;
import dev.qynl.myfirstmod.faction.FactionManager;
import dev.qynl.myfirstmod.gui.ModScreenHandlers;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.UnitCreatorItem;
import dev.qynl.myfirstmod.unit.UnitInspector;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class MyFirstMod implements ModInitializer {
    public static final String MOD_ID = "myfirstmod";

    @Override
    public void onInitialize() {
        ModItems.register();
        ModScreenHandlers.register();
        UnitCommands.register();
        UnitSystem.register();

        // Add creator tool and tactical gear to Creative tabs
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(ModItems.UNIT_CREATOR);
            entries.add(ModItems.COMMANDER_HORN);
            entries.add(ModItems.FACTION_BANNER);
            entries.add(ModItems.FACTION_SCEPTER);
            entries.add(ModItems.TACTICAL_WHISTLE);
            entries.add(ModItems.REINFORCEMENT_BEACON);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(ModItems.UNIT_CREATOR);
            entries.add(ModItems.COMMANDER_HORN);
            entries.add(ModItems.FACTION_BANNER);
            entries.add(ModItems.FACTION_SCEPTER);
            entries.add(ModItems.TACTICAL_WHISTLE);
            entries.add(ModItems.REINFORCEMENT_BEACON);
        });

        // Left-click on block with Creator Tool -> spawn unit / squad
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity serverPlayer) {
                if (serverPlayer.isSneaking()) {
                    UnitCreatorItem.spawnSquadEquipped(serverPlayer);
                } else {
                    UnitCreatorItem.spawnEquipped(serverPlayer);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Left-click / Attack entity with Creator Tool OR Friendly Fire Check
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity serverPlayer) {
                if (serverPlayer.isSneaking()) {
                    UnitCreatorItem.spawnSquadEquipped(serverPlayer);
                } else {
                    UnitCreatorItem.spawnEquipped(serverPlayer);
                }
                return ActionResult.SUCCESS;
            }

            // Friendly Fire Protection Check
            if (!world.isClient && player.getServer() != null) {
                UnitWorldData data = UnitWorldData.get(player.getServer());
                if (!data.friendlyFireAllowed) {
                    String attackerFaction = UnitSystem.getTagValue(player, "faction:");
                    String victimFaction = UnitSystem.getTagValue(entity, "faction:");

                    if (attackerFaction != null && victimFaction != null && FactionManager.isAllied(player.getServer(), attackerFaction, victimFaction)) {
                        return ActionResult.FAIL; // Cancel attack between allies
                    }
                }
            }

            return ActionResult.PASS;
        });
    }
}
