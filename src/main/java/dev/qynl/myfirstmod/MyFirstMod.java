package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.NullbladeItem;
import dev.qynl.myfirstmod.kinetics.KineticsCommands;
import dev.qynl.myfirstmod.kinetics.KineticsConfig;
import dev.qynl.myfirstmod.kinetics.KineticsManager;
import dev.qynl.myfirstmod.kinetics.KineticsNetworking;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyFirstMod implements ModInitializer {
    public static final String MOD_ID = "myfirstmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        KineticsConfig.load();

        ModBlocks.register();
        ModItems.register();
        ModEntities.register();

        // Creative tab entries for the kinetics gear and boss rewards.
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(ModItems.NULLHOOK));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(ModItems.NULLBLADE));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(ModItems.NULL_RELIC));

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != net.minecraft.util.Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!serverPlayer.getStackInHand(hand).isOf(Items.FLINT_AND_STEEL)) return ActionResult.PASS;
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.REINFORCED_DEEPSLATE)
                    && VoidPortalManager.tryIgnite(serverPlayer, hit.getBlockPos())) return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        // Null Kinetics — movement suite.
        KineticsNetworking.register();
        KineticsCommands.register();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                KineticsManager.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                KineticsManager.onPlayerLeave(handler.player.getUuid()));

        ServerTickEvents.END_SERVER_TICK.register(VoidPortalManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(NullbladeItem::tick);
        ServerTickEvents.END_SERVER_TICK.register(KineticsManager::tick);
    }
}
