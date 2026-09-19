package dev.qynl.myfirstmod.remembrance;

import dev.qynl.myfirstmod.item.AshenFlaskItem;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.entity.effect.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** Player-owned realm specializations. Switching requires safety and a persistent cooldown. */
public final class PilgrimVows {
    public static boolean choose(ServerPlayerEntity player,int choice) {
        if(!AshenFlaskItem.safeRest(player)) {
            player.sendMessage(Text.translatable("message.myfirstmod.vow_unsafe"),true);return false;
        }
        var state=RealmState.get(player.getServerWorld());var record=state.expedition(player.getUuid());
        long now=player.getServer().getOverworld().getTime();
        if(record.vow==choice || now<record.vowReadyAt) {
            player.sendMessage(Text.translatable("message.myfirstmod.vow_wait"),true);return false;
        }
        record.vow=RemembranceRules.vow(choice);record.vowReadyAt=now+1200;state.markDirty();
        player.sendMessage(Text.translatable("message.myfirstmod.vow_chosen",Text.translatable("vow.myfirstmod."+record.vow)),false);
        return true;
    }
    public static void tick(MinecraftServer server) {
        var world=server.getWorld(VoidPortalManager.NULL_REALM);if(world==null || world.getTime()%20!=0) return;
        var state=RealmState.get(world);
        for(var player:world.getPlayers()) if(player.isAlive() && !player.isSpectator() && !player.isCreative()) {
            int vow=state.expedition(player.getUuid()).vow;
            if(vow==1) {effect(player,StatusEffects.RESISTANCE);effect(player,StatusEffects.SLOWNESS);}
            if(vow==2) {effect(player,StatusEffects.STRENGTH);effect(player,StatusEffects.HUNGER);}
            if(vow==3) {effect(player,StatusEffects.SPEED);effect(player,StatusEffects.WEAKNESS);}
        }
    }
    private static void effect(ServerPlayerEntity player,net.minecraft.registry.entry.RegistryEntry<StatusEffect> effect) {
        player.addStatusEffect(new StatusEffectInstance(effect,60,0,true,false,true));
    }
}
