package dev.qynl.myfirstmod.territory;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Manages persistent faction outposts and home-turf defensive territory zones.
 */
public final class TerritoryManager {
    private TerritoryManager() {}

    public static void registerOutpost(MinecraftServer server, String factionId, BlockPos pos) {
        if (server == null || factionId == null || pos == null) return;
        UnitWorldData data = UnitWorldData.get(server);
        data.outposts.put(factionId, pos);
        data.markDirty();
    }

    public static BlockPos getOutpost(MinecraftServer server, String factionId) {
        if (server == null || factionId == null) return null;
        return UnitWorldData.get(server).outposts.get(factionId);
    }

    public static void checkTerritory(ServerWorld world, MobEntity mob) {
        if (mob == null || !mob.isAlive() || mob.age % 40 != 0 || world.getServer() == null) return;

        String myFaction = UnitSystem.getTagValue(mob, "faction:");
        if (myFaction == null) return;

        BlockPos outpost = getOutpost(world.getServer(), myFaction);
        if (outpost != null && mob.squaredDistanceTo(outpost.getX(), outpost.getY(), outpost.getZ()) < 1024.0) { // 32 blocks
            // Home Turf defender buff
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0, false, false));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, false, false));
        }
    }
}
