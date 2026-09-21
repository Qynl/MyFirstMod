package dev.qynl.myfirstmod.battle;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Facade providing simple static endpoints for battle sandbox operations,
 * routing internally to the multi-instance BattleManager.
 */
public final class BattleSandbox {
    private BattleSandbox() {}

    public static void startBattle(ServerPlayerEntity player, String factionAId, String factionBId, int armySize) {
        startCustomBattle(player, factionAId, "all", armySize, factionBId, "all", armySize, "line", 32.0);
    }

    public static void startCustomBattle(ServerPlayerEntity player,
                                         String factionAId, String unitAId, int countA,
                                         String factionBId, String unitBId, int countB,
                                         String formation, double distance) {
        BattleManager.createBattle(player, factionAId, unitAId, countA, factionBId, unitBId, countB, formation, distance);
    }

    public static void tickBattleCheck(ServerWorld world) {
        BattleManager.tickBattles(world);
    }

    public static int clearAllBattleMobs(ServerWorld world) {
        return BattleManager.clearAllBattles(world);
    }

    public static boolean isBattleActive() {
        return BattleManager.hasActiveBattles();
    }
}
