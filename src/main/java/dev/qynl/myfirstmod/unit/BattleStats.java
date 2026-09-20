package dev.qynl.myfirstmod.unit;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;

public final class BattleStats extends PersistentState {
    public final Map<String,Integer> kills=new HashMap<>(), deaths=new HashMap<>();
    public static BattleStats get(MinecraftServer s){PersistentStateManager p=s.getWorld(World.OVERWORLD).getPersistentStateManager();return p.getOrCreate(new Type<>(BattleStats::new,BattleStats::read,null),"myfirstmod_battle_stats");}
    private static BattleStats read(NbtCompound n){BattleStats b=new BattleStats();NbtCompound k=n.getCompound("kills"),d=n.getCompound("deaths");for(String id:k.getKeys())b.kills.put(id,k.getInt(id));for(String id:d.getKeys())b.deaths.put(id,d.getInt(id));return b;}
    @Override public NbtCompound writeNbt(NbtCompound n,RegistryWrapper.WrapperLookup l){NbtCompound k=new NbtCompound(),d=new NbtCompound();kills.forEach(k::putInt);deaths.forEach(d::putInt);n.put("kills",k);n.put("deaths",d);return n;}
    public void kill(String faction){kills.merge(faction,1,Integer::sum);markDirty();}
    public void death(String faction){deaths.merge(faction,1,Integer::sum);markDirty();}
}
