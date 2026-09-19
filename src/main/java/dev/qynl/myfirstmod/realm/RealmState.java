package dev.qynl.myfirstmod.realm;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.Map;

public final class RealmState extends PersistentState {
    public boolean sanctuaryBuilt;
    public int bossClears, contentVersion;
    public long rematchReadyAt;
    public final Map<java.util.UUID, ExpeditionRecord> expeditions = new HashMap<>();
    public ExpeditionRecord expedition(java.util.UUID player) {
        return expeditions.computeIfAbsent(player, id -> new ExpeditionRecord());
    }
    public final Map<Long,Long> trialCooldowns = new HashMap<>();
    public static RealmState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(new Type<>(RealmState::new,
                (nbt, lookup) -> read(nbt), null), "myfirstmod_expedition");
    }
    private static RealmState read(NbtCompound nbt) {
        RealmState state=new RealmState();
        state.sanctuaryBuilt=nbt.getBoolean("SanctuaryBuilt");
        state.contentVersion=nbt.getInt("ContentVersion");
        state.bossClears=Math.max(0,nbt.getInt("BossClears"));
        state.rematchReadyAt=nbt.getLong("RematchReadyAt");
        NbtList records=nbt.getList("Expeditions",10);
        for(int i=0;i<records.size();i++) {
            NbtCompound entry=records.getCompound(i);
            if(entry.containsUuid("Player")) state.expeditions.put(entry.getUuid("Player"),ExpeditionRecord.read(entry));
        }
        NbtList trials=nbt.getList("Trials",10);
        for(int i=0;i<trials.size();i++) {
            NbtCompound entry=trials.getCompound(i);
            state.trialCooldowns.put(entry.getLong("Pos"),entry.getLong("Ready"));
        }
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putBoolean("SanctuaryBuilt",sanctuaryBuilt);
        nbt.putInt("BossClears",bossClears);
        nbt.putInt("ContentVersion",contentVersion);
        nbt.putLong("RematchReadyAt",rematchReadyAt);
        NbtList records=new NbtList();
        expeditions.forEach((id,record)->{NbtCompound entry=record.write();entry.putUuid("Player",id);records.add(entry);});
        nbt.put("Expeditions",records);
        NbtList trials=new NbtList();
        trialCooldowns.forEach((pos,time)->{
            NbtCompound entry=new NbtCompound();entry.putLong("Pos",pos);entry.putLong("Ready",time);trials.add(entry);
        });
        nbt.put("Trials",trials);
        return nbt;
    }
}
