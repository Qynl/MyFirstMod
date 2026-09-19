package dev.qynl.myfirstmod.item;
import dev.qynl.myfirstmod.remembrance.LandmarkAtlas;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import dev.qynl.myfirstmod.realm.RealmState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
public final class PilgrimAtlasItem extends Item {
    public static final String[] KINDS={"waystone","court","rift","cathedral","memory"};
    public PilgrimAtlasItem(Settings settings) {super(settings);}
    public static int mode(ItemStack stack) {return Math.floorMod(stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt().getInt("AtlasMode"),KINDS.length);}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand) {
        var stack=player.getStackInHand(hand);
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || player.getItemCooldownManager().isCoolingDown(this)) return TypedActionResult.fail(stack);
        if(player instanceof ServerPlayerEntity server) {
            int route=mode(stack);
            if(player.isSneaking()) route=(route+1)%KINDS.length;
            else LandmarkAtlas.survey(server);
            var record=RealmState.get(server.getServerWorld()).expedition(player.getUuid());
            // Remove stale markers only when their chunks are already loaded.
            boolean removed=record.landmarks.entrySet().removeIf(e->{var p=BlockPos.fromLong(e.getKey());
                return world.isChunkLoaded(p) && !LandmarkAtlas.kind(world.getBlockState(p).getBlock()).equals(e.getValue());});
            if(removed) RealmState.get(server.getServerWorld()).markDirty();
            String type=KINDS[route];
            var nearest=record.landmarks.entrySet().stream().filter(e->e.getValue().equals(type) && (!type.equals("memory") || !record.memories.contains(e.getKey())))
                .map(e->BlockPos.fromLong(e.getKey())).min(Comparator.comparingDouble(p->p.toCenterPos().squaredDistanceTo(player.getPos())));
            var data=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,NbtComponent.DEFAULT).copyNbt();data.putInt("AtlasMode",route);
            stack.set(DataComponentTypes.CUSTOM_DATA,NbtComponent.of(data));
            if(nearest.isPresent()) {
                var p=nearest.get();stack.set(DataComponentTypes.LODESTONE_TRACKER,new LodestoneTrackerComponent(Optional.of(GlobalPos.create(VoidPortalManager.NULL_REALM,p)),false));
                player.sendMessage(Text.translatable("message.myfirstmod.atlas_target",Text.translatable("landmark.myfirstmod."+type),p.getX(),p.getY(),p.getZ()),false);
            } else {
                stack.remove(DataComponentTypes.LODESTONE_TRACKER);
                player.sendMessage(Text.translatable("message.myfirstmod.atlas_empty",Text.translatable("landmark.myfirstmod."+type)),true);
            }
            player.getItemCooldownManager().set(this,player.isSneaking()?10:100);
        }
        return TypedActionResult.success(stack,world.isClient);
    }
}
