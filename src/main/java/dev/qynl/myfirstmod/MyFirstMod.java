package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.NullbladeItem;
import dev.qynl.myfirstmod.item.UnitCreatorItem;
import dev.qynl.myfirstmod.gui.ModScreenHandlers;
import dev.qynl.myfirstmod.unit.UnitSystem;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public class MyFirstMod implements ModInitializer {
    public static final String MOD_ID = "myfirstmod";

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModScreenHandlers.register();
        UnitSystem.register();

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient && hand == net.minecraft.util.Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity sp) {
                UnitCreatorItem.spawnEquipped(sp); return ActionResult.SUCCESS;
            } return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && hand == net.minecraft.util.Hand.MAIN_HAND && player.getStackInHand(hand).isOf(ModItems.UNIT_CREATOR) && player instanceof ServerPlayerEntity sp) {
                UnitCreatorItem.spawnEquipped(sp); return ActionResult.SUCCESS;
            } return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != net.minecraft.util.Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (!serverPlayer.getStackInHand(hand).isOf(Items.FLINT_AND_STEEL)) return ActionResult.PASS;
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.REINFORCED_DEEPSLATE)
                    && VoidPortalManager.tryIgnite(serverPlayer, hit.getBlockPos())) return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(VoidPortalManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(NullbladeItem::tick);
    }
}
