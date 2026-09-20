package dev.qynl.myfirstmod.realm;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.HashMap;
import java.util.Map;

public final class RealmState extends PersistentState {
    public final java.util.Set<Long> keepForcedChunks=new java.util.HashSet<>();
    public final Map<Long,java.util.UUID> monasteryGuardians=new HashMap<>();
    public boolean sanctuaryBuilt,thresholdBuilt;
    public long plinthPos;
    public int worldSeals;
    public long courtPos;
    public int courtWave;
    public final java.util.List<java.util.UUID> courtActors=new java.util.ArrayList<>();
    public int bossClears, contentVersion;
    public long rematchReadyAt;
    public final Map<java.util.UUID, ExpeditionRecord> expeditions = new HashMap<>();
    public ExpeditionRecord expedition(java.util.UUID player) {
        return expeditions.computeIfAbsent(player, id -> new ExpeditionRecord());
    }
    public final Map<Long,Long> riftCooldowns=new HashMap<>();
    public final Map<Long,Long> trialCooldowns = new HashMap<>();
    public static RealmState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(new Type<>(RealmState::new,
                (nbt, lookup) -> read(nbt), null), "myfirstmod_expedition");
    }
    private static RealmState read(NbtCompound nbt) {
        RealmState state=new RealmState();
        for(long pos:nbt.getLongArray("KeepForcedChunks"))state.keepForcedChunks.add(pos);
        state.sanctuaryBuilt=nbt.getBoolean("SanctuaryBuilt");
        state.thresholdBuilt=nbt.getBoolean("ThresholdBuilt");
        state.plinthPos=nbt.getLong("PlinthPos");
        state.worldSeals=nbt.getInt("WorldSeals")&15;
        state.courtPos=nbt.getLong("CourtPos");
        state.courtWave=Math.max(0,Math.min(3,nbt.getInt("CourtWave")));
        for(long id:nbt.getLongArray("CourtActors"))state.courtActors.add(new java.util.UUID(id>>32,id&0xffffffffL));
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
        NbtList rifts=nbt.getList("Rifts",10);
        for(int i=0;i<rifts.size();i++) {
            var entry=rifts.getCompound(i);state.riftCooldowns.put(entry.getLong("Pos"),entry.getLong("Ready"));
        }
        var guardians=nbt.getList("MonasteryGuardians",10);
        for(int i=0;i<guardians.size();i++){var e=guardians.getCompound(i);if(e.containsUuid("Actor"))state.monasteryGuardians.put(e.getLong("Heart"),e.getUuid("Actor"));}
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLongArray("KeepForcedChunks",keepForcedChunks.stream().mapToLong(Long::longValue).toArray());
        nbt.putBoolean("SanctuaryBuilt",sanctuaryBuilt);
        nbt.putBoolean("ThresholdBuilt",thresholdBuilt);
        nbt.putLong("PlinthPos",plinthPos);
        nbt.putInt("WorldSeals",worldSeals);
        nbt.putLong("CourtPos",courtPos);
        nbt.putInt("CourtWave",courtWave);
        nbt.putLongArray("CourtActors",courtActors.stream().limit(16).mapToLong(id->id.getMostSignificantBits()<<32|id.getLeastSignificantBits()).toArray());
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
        NbtList rifts=new NbtList();
        riftCooldowns.forEach((pos,ready)->{var entry=new NbtCompound();entry.putLong("Pos",pos);entry.putLong("Ready",ready);rifts.add(entry);});
        nbt.put("Rifts",rifts);
        var guardians=new NbtList();monasteryGuardians.forEach((pos,id)->{var e=new NbtCompound();e.putLong("Heart",pos);e.putUuid("Actor",id);guardians.add(e);});nbt.put("MonasteryGuardians",guardians);
        return nbt;
    }
}
