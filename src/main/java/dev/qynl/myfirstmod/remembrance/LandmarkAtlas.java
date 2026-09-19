package dev.qynl.myfirstmod.remembrance;

import dev.qynl.myfirstmod.block.ModBlocks;
import dev.qynl.myfirstmod.realm.*;
import dev.qynl.myfirstmod.portal.VoidPortalManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class LandmarkAtlas {
    public static String kind(net.minecraft.block.Block block) {
        if(block==ModBlocks.WAYSTONE) return "waystone";
        if(block==ModBlocks.RESONANCE_CORE) return "court";
        if(block==ModBlocks.RIFT_ANCHOR) return "rift";
        if(block==ModBlocks.MOURNING_RELIQUARY) return "cathedral";
        if(block==ModBlocks.MEMORY_STELE) return "memory";
        return "";
    }
    public static void remember(ServerPlayerEntity player,BlockPos pos) {
        var world=player.getServerWorld();
        if(player.isSpectator() || !world.getRegistryKey().equals(VoidPortalManager.NULL_REALM)) return;
        String type=kind(world.getBlockState(pos).getBlock());if(type.isEmpty()) return;
        var state=RealmState.get(world);state.expedition(player.getUuid()).rememberLandmark(pos.asLong(),type);state.markDirty();
    }
    /** A bounded survey of chunk-center columns only. It never requests an unloaded chunk. */
    public static int survey(ServerPlayerEntity player) {
        var world=player.getServerWorld();int found=0;
        var state=RealmState.get(world);var record=state.expedition(player.getUuid());
        int cx=player.getBlockX()>>4,cz=player.getBlockZ()>>4;
        for(int dx=-4;dx<=4;dx++) for(int dz=-4;dz<=4;dz++) {
            int x=((cx+dx)<<4)+8,z=((cz+dz)<<4)+8;
            if(!world.isChunkLoaded(new BlockPos(x,80,z))) continue;
            for(int y=12;y<=180;y++) {
                var pos=new BlockPos(x,y,z);String type=kind(world.getBlockState(pos).getBlock());
                if(type.isEmpty()) continue;
                if(!record.landmarks.containsKey(pos.asLong())) found++;
                record.rememberLandmark(pos.asLong(),type);
            }
        }
        state.markDirty();return found;
    }
}
