package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.List;

/** A normal written book: works with the vanilla book UI and requires no custom packets. */
public final class ExpeditionJournal {
    private static boolean isJournal(ItemStack stack) {
        return stack.isOf(Items.WRITTEN_BOOK) && stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT)
                .copyNbt().getBoolean("NullJournal");
    }
    public static void register() {
        UseItemCallback.EVENT.register((player,world,hand)->{
            ItemStack stack=player.getStackInHand(hand);
            if(player instanceof ServerPlayerEntity serverPlayer && isJournal(stack)) {
                refresh(serverPlayer,stack);
                serverPlayer.currentScreenHandler.sendContentUpdates();
            }
            return TypedActionResult.pass(stack);
        });
    }
    public static void give(ServerPlayerEntity player) {
        for(int i=0;i<player.getInventory().size();i++) if(isJournal(player.getInventory().getStack(i))) return;
        ItemStack book=new ItemStack(Items.WRITTEN_BOOK);
        NbtCompound data=new NbtCompound();data.putBoolean("NullJournal",true);
        book.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
        refresh(player,book);
        player.getInventory().offerOrDrop(book);
    }
    private static void refresh(ServerPlayerEntity player,ItemStack book) {
        var realm=player.getServer().getWorld(VoidPortalManager.NULL_REALM);
        if(realm==null) return;
        RealmState state=RealmState.get(realm);
        ExpeditionRecord record=state.expedition(player.getUuid());
        List<RawFilteredPair<Text>> pages=new ArrayList<>();
        page(pages,"journal.myfirstmod.progress",record.biomes.size(),record.courts.size(),record.trials,record.highestTier,record.victories);
        page(pages,"journal.myfirstmod.pilgrimage");
        page(pages,"journal.myfirstmod.flask",record.flaskCharges,dev.qynl.myfirstmod.pilgrimage.PilgrimageRules.capacity(record.flaskUpgrades));
        page(pages,"journal.myfirstmod.cathedral",record.cathedralsOpened);
        page(pages,"journal.myfirstmod.funeral");
        page(pages,"journal.myfirstmod.combat");
        page(pages,"journal.myfirstmod.exploration");
        page(pages,"journal.myfirstmod.convergence",record.riftsClosed);
        page(pages,"journal.myfirstmod.attunement");
        page(pages,"journal.myfirstmod.cultivation");
        page(pages,"journal.myfirstmod.resources");
        page(pages,"journal.myfirstmod.wilds_gear");
        page(pages,"journal.myfirstmod.waystones");
        BlockPos bound=BlockPos.fromLong(record.boundWaystone);
        page(pages,"journal.myfirstmod.bound_stone",record.hasWaystone
                ? Text.literal(bound.getX()+", "+bound.getY()+", "+bound.getZ())
                : Text.translatable("journal.myfirstmod.no_stone"));
        page(pages,"journal.myfirstmod.trials");
        page(pages,"journal.myfirstmod.oaths");
        page(pages,"journal.myfirstmod.warden");
        page(pages,"journal.myfirstmod.equipment");
        page(pages,"journal.myfirstmod.endgame");
        // Short pages remain readable at the vanilla book's fixed width.
        List<Long> known=new ArrayList<>(record.courts);
        for(int i=Math.max(0,known.size()-12);i<known.size();i+=3) {
            StringBuilder lines=new StringBuilder();
            for(int j=i;j<Math.min(i+3,known.size());j++) {
                BlockPos pos=BlockPos.fromLong(known.get(j));
                long wait=Math.max(0,(state.trialCooldowns.getOrDefault(known.get(j),0L)-realm.getTime()+19)/20);
                lines.append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ())
                        .append(wait>0?" ["+wait+"s]":" [ready]").append("\n\n");
            }
            page(pages,"journal.myfirstmod.courts",lines.toString());
        }
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT,new WrittenBookContentComponent(
                RawFilteredPair.of("Null Expedition"),"The Threshold Archive",0,pages,true));
    }
    private static void page(List<RawFilteredPair<Text>> pages,String key,Object... args) {
        pages.add(RawFilteredPair.of(Text.translatable(key,args)));
    }
}
