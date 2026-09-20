package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.command.UnitCommands;
import dev.qynl.myfirstmod.gui.ModScreenHandlers;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.UnitCreatorItem;
import dev.qynl.myfirstmod.unit.UnitSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class MyFirstMod implements ModInitializer {
    public static final String MOD_ID = "myfirstmod";
    @Override public void onInitialize() {
        ModItems.register(); ModScreenHandlers.register(); UnitCommands.register(); UnitSystem.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(ModItems.UNIT_CREATOR));
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient && hand == net.minecraft.util.Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity server) { UnitCreatorItem.spawnEquipped(server); return ActionResult.SUCCESS; }
            return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && hand == net.minecraft.util.Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity server) { UnitCreatorItem.spawnEquipped(server); return ActionResult.SUCCESS; }
            return ActionResult.PASS;
        });
    }
}
