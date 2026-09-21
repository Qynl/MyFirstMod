package dev.qynl.myfirstmod.ai;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Active Unit Registry that tracks living modded units in O(1) time
 * rather than iterating through all world entities on every tick.
 */
public final class ActiveUnitManager {
    private static final Map<ServerWorld, Set<MobEntity>> ACTIVE_UNITS = new ConcurrentHashMap<>();

    private ActiveUnitManager() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob && isModUnit(mob)) {
                registerUnit(world, mob);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob) {
                unregisterUnit(world, mob);
            }
        });
    }

    public static boolean isModUnit(Entity entity) {
        if (entity == null) return false;
        for (String tag : entity.getCommandTags()) {
            if (tag.startsWith("unit:") || tag.equals("battle_mob")) {
                return true;
            }
        }
        return false;
    }

    public static void registerUnit(ServerWorld world, MobEntity mob) {
        if (world == null || mob == null) return;
        ACTIVE_UNITS.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet()).add(mob);
    }

    public static void unregisterUnit(ServerWorld world, MobEntity mob) {
        if (world == null || mob == null) return;
        Set<MobEntity> set = ACTIVE_UNITS.get(world);
        if (set != null) {
            set.remove(mob);
            if (set.isEmpty()) {
                ACTIVE_UNITS.remove(world);
            }
        }
    }

    public static Set<MobEntity> getActiveUnits(ServerWorld world) {
        if (world == null) return Collections.emptySet();
        Set<MobEntity> set = ACTIVE_UNITS.get(world);
        if (set == null) return Collections.emptySet();

        // Prune any dead or discarded entities lazily
        set.removeIf(mob -> mob == null || !mob.isAlive() || mob.isRemoved());
        return set;
    }

    public static int getActiveUnitCount(ServerWorld world) {
        return getActiveUnits(world).size();
    }

    public static void clear() {
        ACTIVE_UNITS.clear();
    }
}
