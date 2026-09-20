package dev.qynl.myfirstmod.faction;

import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.server.MinecraftServer;

public final class FactionManager {
    private FactionManager() {}

    public static Faction getFaction(MinecraftServer server, String factionId) {
        if (server == null || factionId == null || factionId.isBlank()) return null;
        return UnitWorldData.get(server).factions.get(factionId);
    }

    public static boolean isHostile(MinecraftServer server, String f1, String f2) {
        if (f1 == null || f2 == null || f1.equalsIgnoreCase(f2)) return false;
        Faction faction = getFaction(server, f1);
        if (faction != null) {
            return faction.isHostileTo(f2);
        }
        return false;
    }

    public static boolean isAllied(MinecraftServer server, String f1, String f2) {
        if (f1 == null || f2 == null) return false;
        if (f1.equalsIgnoreCase(f2)) return true;
        Faction faction = getFaction(server, f1);
        if (faction != null) {
            return faction.isAlliedWith(f2);
        }
        return false;
    }

    public static FactionRelation getRelation(MinecraftServer server, String f1, String f2) {
        if (f1 == null || f2 == null) return FactionRelation.NEUTRAL;
        if (f1.equalsIgnoreCase(f2)) return FactionRelation.ALLIED;
        Faction faction = getFaction(server, f1);
        if (faction != null) {
            return faction.relations.getOrDefault(f2, FactionRelation.NEUTRAL);
        }
        return FactionRelation.NEUTRAL;
    }

    public static boolean hasPerk(MinecraftServer server, String factionId, FactionPerk perk) {
        Faction faction = getFaction(server, factionId);
        return faction != null && faction.hasPerk(perk);
    }

    public static int getFactionColor(MinecraftServer server, String factionId) {
        Faction faction = getFaction(server, factionId);
        return faction != null ? faction.getParsedColor() : 0x94A3B8;
    }
}
