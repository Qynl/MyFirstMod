package dev.qynl.myfirstmod.realm;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import java.util.LinkedHashSet;
import java.util.Set;

/** Per-player expedition progress. Bounded discovery storage keeps saves small. */
public final class ExpeditionRecord {
    public int trials, highestTier, victories, pendingNormal, pendingEcho;
    public final Set<String> biomes = new LinkedHashSet<>();
    public final Set<Long> courts = new LinkedHashSet<>();
    public void discoverCourt(long pos) {
        if (courts.size() >= 128 && !courts.contains(pos)) courts.remove(courts.iterator().next());
        courts.add(pos);
    }
    public static ExpeditionRecord read(NbtCompound nbt) {
        ExpeditionRecord record = new ExpeditionRecord();
        record.trials = Math.max(0, nbt.getInt("Trials"));
        record.highestTier = Math.max(0, Math.min(5, nbt.getInt("Tier")));
        record.victories = Math.max(0, nbt.getInt("Victories"));
        record.pendingNormal = Math.max(0,Math.min(64,nbt.getInt("PendingNormal")));
        record.pendingEcho = Math.max(0,Math.min(64,nbt.getInt("PendingEcho")));
        NbtList biomes = nbt.getList("Biomes", 8);
        for (int i=0; i<Math.min(3, biomes.size()); i++) record.biomes.add(biomes.getString(i));
        long[] courts = nbt.getLongArray("Courts");
        for (int i=Math.max(0,courts.length-128); i<courts.length; i++) record.discoverCourt(courts[i]);
        return record;
    }
    public NbtCompound write() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("PendingNormal",pendingNormal);nbt.putInt("PendingEcho",pendingEcho);
        nbt.putInt("Trials", trials); nbt.putInt("Tier", highestTier); nbt.putInt("Victories", victories);
        NbtList list = new NbtList();
        biomes.forEach(b -> list.add(NbtString.of(b)));
        nbt.put("Biomes", list);
        nbt.putLongArray("Courts", courts.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }
}
