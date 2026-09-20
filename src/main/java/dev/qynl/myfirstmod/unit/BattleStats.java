package dev.qynl.myfirstmod.unit;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BattleStats extends PersistentState {
    public final Map<String, Integer> kills = new LinkedHashMap<>();
    public final Map<String, Integer> deaths = new LinkedHashMap<>();
    public final Map<String, Float> damageDealt = new LinkedHashMap<>();
    public final Map<String, Integer> battlesWon = new LinkedHashMap<>();

    private static final String KEY = "myfirstmod_battle_stats";

    public static BattleStats get(MinecraftServer server) {
        if (server == null) return new BattleStats();
        PersistentStateManager psm = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return psm.getOrCreate(new Type<>(BattleStats::new, BattleStats::readNbt, null), KEY);
    }

    public static BattleStats readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        BattleStats stats = new BattleStats();
        if (nbt.contains("kills")) {
            NbtCompound k = nbt.getCompound("kills");
            for (String key : k.getKeys()) stats.kills.put(key, k.getInt(key));
        }
        if (nbt.contains("deaths")) {
            NbtCompound d = nbt.getCompound("deaths");
            for (String key : d.getKeys()) stats.deaths.put(key, d.getInt(key));
        }
        if (nbt.contains("damage")) {
            NbtCompound dmg = nbt.getCompound("damage");
            for (String key : dmg.getKeys()) stats.damageDealt.put(key, dmg.getFloat(key));
        }
        if (nbt.contains("wins")) {
            NbtCompound w = nbt.getCompound("wins");
            for (String key : w.getKeys()) stats.battlesWon.put(key, w.getInt(key));
        }
        return stats;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound k = new NbtCompound();
        kills.forEach(k::putInt);
        nbt.put("kills", k);

        NbtCompound d = new NbtCompound();
        deaths.forEach(d::putInt);
        nbt.put("deaths", d);

        NbtCompound dmg = new NbtCompound();
        damageDealt.forEach(dmg::putFloat);
        nbt.put("damage", dmg);

        NbtCompound w = new NbtCompound();
        battlesWon.forEach(w::putInt);
        nbt.put("wins", w);

        return nbt;
    }

    public void recordKill(String faction) {
        if (faction == null) return;
        kills.merge(faction, 1, Integer::sum);
        markDirty();
    }

    public void recordDeath(String faction) {
        if (faction == null) return;
        deaths.merge(faction, 1, Integer::sum);
        markDirty();
    }

    public void recordDamage(String faction, float amount) {
        if (faction == null || amount <= 0) return;
        damageDealt.merge(faction, amount, Float::sum);
        markDirty();
    }

    public void recordWin(String faction) {
        if (faction == null) return;
        battlesWon.merge(faction, 1, Integer::sum);
        markDirty();
    }

    public void reset() {
        kills.clear();
        deaths.clear();
        damageDealt.clear();
        battlesWon.clear();
        markDirty();
    }

    public int getKills(String faction) {
        return kills.getOrDefault(faction, 0);
    }

    public int getDeaths(String faction) {
        return deaths.getOrDefault(faction, 0);
    }

    public float getDamage(String faction) {
        return damageDealt.getOrDefault(faction, 0.0f);
    }
}
