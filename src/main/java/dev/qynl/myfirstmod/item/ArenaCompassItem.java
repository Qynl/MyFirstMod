package dev.qynl.myfirstmod.item;

import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import dev.qynl.myfirstmod.realm.RealmExpedition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import java.util.Comparator;
import java.util.Optional;

public final class ArenaCompassItem extends CompassItem {
    public ArenaCompassItem(Settings settings) { super(settings); }
    public static ItemStack create() {
        ItemStack stack=new ItemStack(ModItems.ARENA_COMPASS);
        bind(stack,new BlockPos(0,80,0));
        return stack;
    }
    public static int mode(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT)
                .copyNbt().getInt("Route"),3);
    }
    public static BlockPos target(ItemStack stack) {
        var tracker=stack.get(DataComponentTypes.LODESTONE_TRACKER);
        return tracker==null ? new BlockPos(0,80,0) : tracker.target().map(GlobalPos::pos).orElse(new BlockPos(0,80,0));
    }
    private static void bind(ItemStack stack,BlockPos target) {
        stack.set(DataComponentTypes.LODESTONE_TRACKER,new LodestoneTrackerComponent(
                Optional.of(GlobalPos.create(VoidPortalManager.NULL_REALM,target)),false));
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        ItemStack stack=player.getStackInHand(hand);
        if(player instanceof ServerPlayerEntity serverPlayer) {
            if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) {
                player.sendMessage(Text.translatable("message.myfirstmod.compass_dormant"),true);
                return TypedActionResult.success(stack,false);
            }
            int route=mode(stack);
            if(player.isSneaking()) route=(route+1)%3;
            BlockPos destination=new BlockPos(0,80,0);
            if(route==1) destination=RealmExpedition.ARRIVAL;
            if(route==2) {
                var state=RealmState.get(serverPlayer.getServerWorld());
                Optional<BlockPos> court=state.expedition(player.getUuid()).courts.stream()
                        .filter(p->state.trialCooldowns.getOrDefault(p,0L)<=world.getTime())
                        .map(BlockPos::fromLong).min(Comparator.comparingDouble(p->p.toCenterPos().squaredDistanceTo(player.getPos())));
                if(court.isEmpty()) {
                    player.sendMessage(Text.translatable("message.myfirstmod.no_courts"),true);
                    route=0;
                } else destination=court.get();
            }
            var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();
            data.putInt("Route",route);stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
            bind(stack,destination);
            player.sendMessage(Text.translatable("message.myfirstmod.route",Text.translatable("route.myfirstmod."+route),
                    (int)Math.hypot(destination.getX()-player.getX(),destination.getZ()-player.getZ())),true);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
