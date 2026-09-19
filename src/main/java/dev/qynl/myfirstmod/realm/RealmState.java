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
    public final Map<Long,Long> trialCooldowns = new HashMap<>();
    public static RealmState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(new Type<>(RealmState::new,
                (nbt, lookup) -> read(nbt), null), "myfirstmod_expedition");
    }
    private static RealmState read(NbtCompound nbt) {
        RealmState state=new RealmState();
        state.sanctuaryBuilt=nbt.getBoolean("SanctuaryBuilt");
        NbtList trials=nbt.getList("Trials",10);
        for(int i=0;i<trials.size();i++) {
            NbtCompound entry=trials.getCompound(i);
            state.trialCooldowns.put(entry.getLong("Pos"),entry.getLong("Ready"));
        }
        return state;
    }
    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putBoolean("SanctuaryBuilt",sanctuaryBuilt);
        NbtList trials=new NbtList();
        trialCooldowns.forEach((pos,time)->{
            NbtCompound entry=new NbtCompound();entry.putLong("Pos",pos);entry.putLong("Ready",time);trials.add(entry);
        });
        nbt.put("Trials",trials);
        return nbt;
    }
}
