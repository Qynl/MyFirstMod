package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.item.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import java.util.Set;
import java.util.UUID;

/** Reward mailboxes survive a new ritual, restart, or a participant disconnecting at victory. */
public final class ExpeditionRewards {
    public static void enqueue(ServerWorld world,Set<UUID> players,boolean rematch) {
        RealmState state=RealmState.get(world);
        for(UUID id:players) {
            var record=state.expedition(id);
            if(rematch) record.pendingEcho=Math.min(64,record.pendingEcho+1);
            else record.pendingNormal=Math.min(64,record.pendingNormal+1);
        }
        state.markDirty();
    }
    public static void claim(ServerPlayerEntity player) {
        RealmState state=RealmState.get(player.getServerWorld());
        var record=state.expedition(player.getUuid());
        int normal=record.pendingNormal,echo=record.pendingEcho;
        if(record.pendingRiftCores>0) {
            int cores=record.pendingRiftCores;record.pendingRiftCores=0;state.markDirty();
            offer(player,ModItems.ASTRAL_CORE,cores);offer(player,ModItems.RESONANT_SHARD,cores*4);
            player.addExperience(cores*120);
            player.sendMessage(Text.translatable("message.myfirstmod.rift_mail",cores),false);
        }
        if(normal+echo==0) return;
        boolean first=record.victories==0;
        // Clear the mailbox before effects; callbacks cannot recursively claim it again.
        record.pendingNormal=0;record.pendingEcho=0;record.victories+=normal+echo;state.markDirty();
        if(first) {
            offer(player,ModItems.NULLBLADE,1);offer(player,ModItems.NULL_RELIC,1);
        }
        offer(player,ModItems.RESONANT_SHARD,normal*4+echo*8);
        offer(player,ModItems.WARDEN_CREST,echo);
        player.addExperience(normal*500+echo*350);
        player.sendMessage(Text.translatable("message.myfirstmod.reward_mail",normal+echo,normal*4+echo*8,echo),false);
        if(first) player.sendMessage(Text.translatable("message.myfirstmod.first_relics"),false);
        player.sendMessage(Text.translatable("message.myfirstmod.after_victory"),false);
    }
    private static void offer(ServerPlayerEntity player,Item item,int count) {
        while(count>0) {int size=Math.min(item.getMaxCount(),count);player.getInventory().offerOrDrop(new ItemStack(item,size));count-=size;}
    }
}
