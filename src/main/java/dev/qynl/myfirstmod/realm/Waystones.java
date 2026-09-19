package dev.qynl.myfirstmod.realm;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

public final class Waystones {
    private Waystones() {}
    public static ActionResult interact(ServerPlayerEntity player,BlockPos pos) {
        var world=player.getServerWorld();
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || !world.getBlockState(pos).isOf(ModBlocks.WAYSTONE))
            return ActionResult.PASS;
        if(player.isSpectator()) return ActionResult.PASS;
        var state=RealmState.get(world);var record=state.expedition(player.getUuid());
        record.boundWaystone=pos.asLong();record.hasWaystone=true;state.markDirty();
        player.sendMessage(Text.translatable("message.myfirstmod.waystone_bound",pos.getX(),pos.getY(),pos.getZ()),false);
        return ActionResult.SUCCESS;
    }
    public static boolean combatLocked(ServerPlayerEntity player) {
        return dev.qynl.myfirstmod.boss.NullWardenManager.isEncounterActive(player.getServerWorld())
                && player.squaredDistanceTo(.5,81,.5)<48*48 || RealmTrials.isEnrolled(player.getUuid()) || dev.qynl.myfirstmod.rift.RealmRifts.isEnrolled(player.getUuid());
    }
    public static boolean recall(ServerPlayerEntity player) {
        var world=player.getServerWorld();
        if(!world.getRegistryKey().equals(VoidPortalManager.NULL_REALM) || combatLocked(player)) return false;
        var record=RealmState.get(world).expedition(player.getUuid());
        boolean useStone=record.hasWaystone && !player.isSneaking();
        BlockPos anchor=useStone?BlockPos.fromLong(record.boundWaystone):RealmExpedition.ARRIVAL;
        if(!world.getWorldBorder().contains(anchor)) return false;
        world.getChunk(anchor);
        if(useStone && !world.getBlockState(anchor).isOf(ModBlocks.WAYSTONE)) return false;
        // Search a small supported landing area; never overwrite a player's blocks to teleport.
        for(int[] offset:new int[][]{{2,0},{-2,0},{0,2},{0,-2},{0,0}}) {
            var feet=anchor.add(offset[0],0,offset[1]);
            var below=feet.down();
            if(!world.getBlockState(below).isSolidBlock(world,below) || !world.getFluidState(below).isEmpty()) continue;
            var box=player.getBoundingBox().offset(feet.toBottomCenterPos().subtract(player.getPos()));
            if(!world.getWorldBorder().contains(box) || !world.isSpaceEmpty(player,box) || world.containsFluid(box)) continue;
            if(world.getBlockState(below).isOf(net.minecraft.block.Blocks.MAGMA_BLOCK)
                    || world.getBlockState(feet).isIn(net.minecraft.registry.tag.BlockTags.FIRE)) continue;
            player.teleport(world,feet.getX()+.5,feet.getY(),feet.getZ()+.5,player.getYaw(),player.getPitch());
            player.fallDistance=0;return true;
        }
        return false;
    }
}
