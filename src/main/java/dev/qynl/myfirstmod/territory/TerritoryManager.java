package dev.qynl.myfirstmod.territory;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.FactionManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class TerritoryManager {
    private static final Map<String, BlockPos> OUTPOSTS = new HashMap<>();

    private TerritoryManager() {}

    public static void registerOutpost(String factionId, BlockPos pos) {
        if (factionId == null || pos == null) return;
        OUTPOSTS.put(factionId, pos);
    }

    public static void checkTerritory(ServerWorld world, MobEntity mob) {
        if (mob == null || !mob.isAlive() || mob.age % 40 != 0) return;

        String myFaction = UnitSystem.getTagValue(mob, "faction:");
        if (myFaction == null) return;

        BlockPos outpost = OUTPOSTS.get(myFaction);
        if (outpost != null && mob.squaredDistanceTo(outpost.getX(), outpost.getY(), outpost.getZ()) < 1024.0) { // 32 blocks
            // Home Turf defender buff
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 0, false, false));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, false, false));
        }
    }
}
