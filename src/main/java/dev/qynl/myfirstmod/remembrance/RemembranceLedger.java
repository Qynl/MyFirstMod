package dev.qynl.myfirstmod.remembrance;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.item.ModItems;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class RemembranceLedger {
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        var world=player.getServerWorld();var block=world.getBlockState(pos);
        if(player.isSpectator() || !world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) return ActionResult.PASS;
        var state=RealmState.get(world);
        if(block.isOf(ModBlocks.MEMORY_STELE)) {
            var record=state.expedition(player.getUuid());
            if(record.memories.contains(pos.asLong()) || record.memories.size()>=256) {
                player.sendMessage(Text.translatable("message.myfirstmod.memory_known"),true);return ActionResult.SUCCESS;
            }
            record.memories.add(pos.asLong());state.markDirty();
            player.getInventory().offerOrDrop(new ItemStack(ModItems.MEMORY_SHARD));player.addExperience(25);
            int story=Math.floorMod(pos.asLong(),6);
            player.sendMessage(Text.translatable("memory.myfirstmod."+story),false);
            player.sendMessage(Text.translatable("message.myfirstmod.memory_found",record.memories.size()),true);
            return ActionResult.SUCCESS;
        }
        if(!block.isOf(ModBlocks.PILGRIM_LEDGER)) return ActionResult.PASS;
        var record=state.expedition(player.getUuid());
        if(player.isSneaking()) {
            record.ledgerOffer=(record.ledgerOffer+1)%4;state.markDirty();
        } else if(player.getMainHandStack().isOf(ModItems.MEMORY_SHARD)) {
            int cost=RemembranceRules.price(record.ledgerOffer);var payment=player.getMainHandStack();
            if(payment.getCount()<cost) {player.sendMessage(Text.translatable("message.myfirstmod.ledger_cost",cost),true);return ActionResult.SUCCESS;}
            Item[] goods={ModItems.REPAIR_KIT,ModItems.PRISM_DUST,ModItems.ECHO_SIGIL,ModItems.RESONITE_INGOT};
            int[] amounts={2,24,2,12};
            if(!player.isCreative()) payment.decrement(cost);
            player.getInventory().offerOrDrop(new ItemStack(goods[record.ledgerOffer],amounts[record.ledgerOffer]));
            player.sendMessage(Text.translatable("message.myfirstmod.ledger_bought"),true);
        } else {
            int total=0;
            for(int i=0;i<RemembranceRules.CONTRACTS;i++) {
                boolean claimed=(record.contractsClaimed&(1<<i))!=0;
                boolean ready=RemembranceRules.ready(i,record.biomes.size(),record.memories.size(),record.trials,record.cathedralsOpened,record.riftsClosed,record.highestTier,record.victories);
                if(!claimed && ready) {record.contractsClaimed|=1<<i;total+=RemembranceRules.reward(i);}
                player.sendMessage(Text.translatable("message.myfirstmod.contract_status",Text.translatable("contract.myfirstmod."+i),
                        Text.translatable(claimed?"contract.myfirstmod.claimed":ready?"contract.myfirstmod.complete":"contract.myfirstmod.incomplete")),false);
            }
            if(total>0) {state.markDirty();player.getInventory().offerOrDrop(new ItemStack(ModItems.MEMORY_SHARD,total));}
        }
        player.sendMessage(Text.translatable("message.myfirstmod.ledger_offer",Text.translatable("offer.myfirstmod."+record.ledgerOffer),RemembranceRules.price(record.ledgerOffer)),false);
        return ActionResult.SUCCESS;
    }
}
