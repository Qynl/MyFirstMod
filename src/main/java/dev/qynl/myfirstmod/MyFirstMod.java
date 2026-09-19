package dev.qynl.myfirstmod;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.boss.ModEntities;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.item.NullbladeItem;
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
        dev.qynl.myfirstmod.realm.RealmFeatures.register();
        dev.qynl.myfirstmod.realm.ExpeditionJournal.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            dev.qynl.myfirstmod.realm.RealmTrials.clear();
            dev.qynl.myfirstmod.boss.NullWardenManager.clear();
            VoidPortalManager.clear();
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            dev.qynl.myfirstmod.boss.NullWardenManager.entityLoaded(entity, world);
            if (entity.getCommandTags().contains("null_trial")
                    && !dev.qynl.myfirstmod.realm.RealmTrials.owns(entity.getUuid())) entity.discard();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != net.minecraft.util.Hand.MAIN_HAND) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            ActionResult altar=dev.qynl.myfirstmod.boss.NullWardenManager.interactAltar(serverPlayer,hit.getBlockPos());
            if(altar!=ActionResult.PASS) return altar;
            ActionResult trial = dev.qynl.myfirstmod.realm.RealmTrials.interact(serverPlayer, hit.getBlockPos());
            if (trial != ActionResult.PASS) return trial;
            if (!serverPlayer.getStackInHand(hand).isOf(Items.FLINT_AND_STEEL)) return ActionResult.PASS;
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.REINFORCED_DEEPSLATE)
                    && VoidPortalManager.tryIgnite(serverPlayer, hit.getBlockPos())) return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(VoidPortalManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(NullbladeItem::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.realm.RealmExpedition::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.realm.RealmTrials::tick);
    }
}
