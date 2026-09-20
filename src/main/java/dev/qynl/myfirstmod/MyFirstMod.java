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
        net.fabricmc.fabric.api.event.interact.v1.UseItemCallback.register((player,world,hand)->{
            if(!world.isClient&&player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer){
                ActionResult seal=dev.qynl.myfirstmod.kingdom.SealChain.useSeal(serverPlayer);
                if(seal!=ActionResult.PASS)return net.minecraft.util.TypedActionResult.success(serverPlayer.getStackInHand(hand));
            }
            return net.minecraft.util.TypedActionResult.pass(player.getStackInHand(hand));
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->!(entity instanceof net.minecraft.server.network.ServerPlayerEntity player&&dev.qynl.myfirstmod.kingdom.SealChain.charmShields(player,source)));
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        dev.qynl.myfirstmod.kingdom.MonasteryStructure.register();
        dev.qynl.myfirstmod.realm.RealmScenery.register();
        dev.qynl.myfirstmod.realm.RegionSignatures.register();
        dev.qynl.myfirstmod.kingdom.ArchiveFeature.register();
        dev.qynl.myfirstmod.kingdom.FoundryFeature.register();
        VoidPortalManager.registerGateCommand();
        dev.qynl.myfirstmod.keep.HollowKeep.registerCommands();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(dev.qynl.myfirstmod.keep.HollowKeep::resetTickets);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(dev.qynl.myfirstmod.keep.HollowKeep::resetTickets);
        dev.qynl.myfirstmod.remembrance.RemembranceFeature.register();
        dev.qynl.myfirstmod.pilgrimage.CathedralFeature.register();
        dev.qynl.myfirstmod.realm.RealmFeatures.register();
        dev.qynl.myfirstmod.realm.WildsFeatures.register();
        dev.qynl.myfirstmod.rift.RiftObservatoryFeature.register();
        dev.qynl.myfirstmod.realm.ExpeditionJournal.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            dev.qynl.myfirstmod.keep.HollowKeep.clear();
            dev.qynl.myfirstmod.realm.RealmTrials.clear();
            dev.qynl.myfirstmod.rift.RealmRifts.clear();
            dev.qynl.myfirstmod.boss.NullWardenManager.clear();
            VoidPortalManager.clear();
            dev.qynl.myfirstmod.kingdom.Monastery.clear();
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            dev.qynl.myfirstmod.boss.NullWardenManager.entityLoaded(entity, world);
            if(entity.getCommandTags().contains("hollow_keep")&&!dev.qynl.myfirstmod.keep.HollowKeep.owns(entity.getUuid()))entity.discard();
            if(entity.getCommandTags().contains("null_rift") && !dev.qynl.myfirstmod.rift.RealmRifts.owns(entity.getUuid())) entity.discard();
            if (entity.getCommandTags().contains("null_trial")
                    && !dev.qynl.myfirstmod.realm.RealmTrials.owns(entity.getUuid())) entity.discard();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if(hand!=net.minecraft.util.Hand.MAIN_HAND){
                if(serverPlayer.getStackInHand(hand).isOf(Items.ECHO_SHARD)&&world.getBlockState(hit.getBlockPos()).isOf(Blocks.REINFORCED_DEEPSLATE)
                    &&VoidPortalManager.tryIgnite(serverPlayer,hit.getBlockPos(),hand))return ActionResult.SUCCESS;
                return ActionResult.PASS;
            }
            ActionResult quench=dev.qynl.myfirstmod.kingdom.FoundryQuench.interact(serverPlayer,hit.getBlockPos());
            if(quench!=ActionResult.PASS)return quench;
            ActionResult tide=dev.qynl.myfirstmod.kingdom.ArchiveTides.interact(serverPlayer,hit.getBlockPos());
            if(tide!=ActionResult.PASS)return tide;
            ActionResult monastery=dev.qynl.myfirstmod.kingdom.Monastery.interact(serverPlayer,hit.getBlockPos());
            if(monastery!=ActionResult.PASS)return monastery;
            ActionResult keep=dev.qynl.myfirstmod.keep.HollowKeep.interact(serverPlayer,hit.getBlockPos());
            if(keep!=ActionResult.PASS)return keep;
            dev.qynl.myfirstmod.remembrance.LandmarkAtlas.remember(serverPlayer,hit.getBlockPos());
            ActionResult ledger=dev.qynl.myfirstmod.remembrance.RemembranceLedger.interact(serverPlayer,hit.getBlockPos());
            if(ledger!=ActionResult.PASS) return ledger;
            ActionResult rite=dev.qynl.myfirstmod.pilgrimage.CathedralRite.interact(serverPlayer,hit.getBlockPos());
            if(rite!=ActionResult.PASS) return rite;
            ActionResult harvest=dev.qynl.myfirstmod.block.HushNurseryBlock.harvest(serverPlayer,hit.getBlockPos());
            if(harvest!=ActionResult.PASS) return harvest;
            ActionResult forge=dev.qynl.myfirstmod.item.RelicAttunements.interact(serverPlayer,hit.getBlockPos());
            if(forge!=ActionResult.PASS) return forge;
            ActionResult rift=dev.qynl.myfirstmod.rift.RealmRifts.interact(serverPlayer,hit.getBlockPos());
            if(rift!=ActionResult.PASS) return rift;
            ActionResult waystone=dev.qynl.myfirstmod.realm.Waystones.interact(serverPlayer,hit.getBlockPos());
            if(waystone!=ActionResult.PASS) return waystone;
            ActionResult altar=dev.qynl.myfirstmod.boss.NullWardenManager.interactAltar(serverPlayer,hit.getBlockPos());
            if(altar!=ActionResult.PASS) return altar;
            ActionResult trial = dev.qynl.myfirstmod.realm.RealmTrials.interact(serverPlayer, hit.getBlockPos());
            if (trial != ActionResult.PASS) return trial;
            if (!serverPlayer.getStackInHand(hand).isOf(Items.ECHO_SHARD)) return ActionResult.PASS;
            if (world.getBlockState(hit.getBlockPos()).isOf(Blocks.REINFORCED_DEEPSLATE)
                    && VoidPortalManager.tryIgnite(serverPlayer, hit.getBlockPos(), hand)) return ActionResult.SUCCESS;
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.kingdom.Monastery::tick);
        ServerTickEvents.END_SERVER_TICK.register(VoidPortalManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.keep.HollowKeep::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.remembrance.PilgrimVows::tick);
        ServerTickEvents.END_SERVER_TICK.register(NullbladeItem::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.item.ResoniteArmor::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.item.AshenFlaskItem::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.realm.RealmExpedition::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.realm.RealmTrials::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.kingdom.SealChain::tick);
        ServerTickEvents.END_SERVER_TICK.register(dev.qynl.myfirstmod.rift.RealmRifts::tick);
    }
}
