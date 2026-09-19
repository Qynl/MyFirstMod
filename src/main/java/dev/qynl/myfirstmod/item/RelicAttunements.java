package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import dev.qynl.myfirstmod.rift.ConvergenceRules;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;

/** One attunement per relic. Its data survives smithing, renaming, and server restarts. */
public final class RelicAttunements {
    public static String kind(ItemStack stack) {
        String value=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getString("NullAttunement");
        return switch(value) {case "vigor","gale","focus"->value;default->"";};
    }
    public static boolean accepts(ItemStack stack) {
        return stack.isOf(ModItems.REQUIEM_GLAIVE)||stack.isOf(ModItems.NULLBLADE)||stack.isOf(ModItems.ASCENDED_NULLBLADE)||stack.isOf(ModItems.PRISM_STAFF)
                ||stack.isOf(ModItems.CINDER_MAUL)||stack.isOf(ModItems.RIFT_AEGIS);
    }
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        if(!player.getServerWorld().getBlockState(pos).isOf(ModBlocks.ATTUNEMENT_FORGE)) return ActionResult.PASS;
        if(player.isSpectator()) return ActionResult.PASS;
        ItemStack gear=player.getMainHandStack(),rune=player.getOffHandStack();
        String choice=rune.isOf(ModItems.VIGOR_RUNE)?"vigor":rune.isOf(ModItems.GALE_RUNE)?"gale":rune.isOf(ModItems.FOCUS_RUNE)?"focus":"";
        if(!accepts(gear)||choice.isEmpty()) return message(player,"message.myfirstmod.forge_hint");
        if(!kind(gear).isEmpty() && !player.isSneaking()) return message(player,"message.myfirstmod.forge_replace");
        if(choice.equals(kind(gear))) return message(player,"message.myfirstmod.forge_same");
        var data=gear.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
        data.putString("NullAttunement",choice);gear.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
        var lore=new ArrayList<Text>();
        for(var line:gear.getOrDefault(DataComponentTypes.LORE,LoreComponent.DEFAULT).lines()) {
            if(line.getContent() instanceof TranslatableTextContent content && content.getKey().startsWith("attunement.myfirstmod.")) continue;
            if(lore.size()<255) lore.add(line);
        }
        lore.add(Text.translatable("attunement.myfirstmod."+choice).formatted(Formatting.AQUA));
        gear.set(DataComponentTypes.LORE,new LoreComponent(lore));
        if(!player.isCreative()) rune.decrement(1);
        return message(player,"message.myfirstmod.forge_success",Text.translatable("attunement.myfirstmod."+choice));
    }
    private static ActionResult message(ServerPlayerEntity player,String key,Object... args) {
        player.sendMessage(Text.translatable(key,args),false);return ActionResult.SUCCESS;
    }
    public static int cooldown(ItemStack stack,int ticks) {return ConvergenceRules.cooldown(ticks,kind(stack).equals("focus"));}
    public static void afterCast(PlayerEntity player,ItemStack stack,boolean successfulHit) {
        if(!(player instanceof ServerPlayerEntity serverPlayer)) return;
        String rune=kind(stack);
        if(rune.isEmpty() || rune.equals("focus")) return;
        var realm=serverPlayer.getServer().getWorld(VoidPortalManager.NULL_REALM);
        if(realm==null) return;
        var state=RealmState.get(realm);var record=state.expedition(player.getUuid());
        // Use one persistent clock and one cooldown per effect, not per stack or dimension.
        long now=serverPlayer.getServer().getOverworld().getTime();
        if(rune.equals("vigor") && successfulHit && now>=record.vigorReadyAt) {
            player.heal(2);record.vigorReadyAt=now+200;state.markDirty();
        } else if(rune.equals("gale") && now>=record.galeReadyAt) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,80,0));
            record.galeReadyAt=now+200;state.markDirty();
        }
    }
}
