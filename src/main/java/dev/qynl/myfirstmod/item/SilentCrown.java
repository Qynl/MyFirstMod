package dev.qynl.myfirstmod.item;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
/** Worn, the crown softens the kingdom: no fall breaks the crowned, and the realm's
 * dark becomes sight while they walk it. */
public final class SilentCrown {
    private SilentCrown(){}
    public static boolean worn(ServerPlayerEntity player){return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.SILENT_CROWN);}
    public static boolean shields(ServerPlayerEntity player,DamageSource source){return worn(player)&&source.isOf(DamageTypes.FALL);}
    public static void tick(MinecraftServer server){
        var world=server.getWorld(dev.qynl.myfirstmod.portal.VoidPortalManager.NULL_REALM);
        if(world==null||world.getTime()%20!=0)return;
        for(var player:world.getPlayers()){
            if(!player.isAlive()||player.isSpectator()||!worn(player))continue;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION,300,0,false,false,true));
        }
    }
}
