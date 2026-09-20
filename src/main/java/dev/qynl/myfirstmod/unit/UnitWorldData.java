package dev.qynl.myfirstmod.unit;

import dev.qynl.myfirstmod.faction.Faction;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import java.util.*;

/** Server-authoritative library. Definitions are saved in the world, never in a client screen. */
public final class UnitWorldData extends PersistentState {
    public final Map<String, UnitDefinition> units = new LinkedHashMap<>();
    public final Map<String, Faction> factions = new LinkedHashMap<>();
    public final Map<UUID, String> equipped = new HashMap<>();
    private static final String KEY = "myfirstmod_units";
    public UnitWorldData() { }
    public static UnitWorldData get(MinecraftServer server) { PersistentStateManager p=server.getWorld(World.OVERWORLD).getPersistentStateManager(); return p.getOrCreate(new Type<>(UnitWorldData::new, UnitWorldData::fromNbt, null), KEY); }
    public static UnitWorldData fromNbt(NbtCompound n) { UnitWorldData d=new UnitWorldData(); NbtList us=n.getList("units",10); for(int i=0;i<us.size();i++){ UnitDefinition u=new UnitDefinition(us.getCompound(i)); if(!u.id.isBlank()&&u.entityId!=null)d.units.put(u.id,u); } NbtList fs=n.getList("factions",10); for(int i=0;i<fs.size();i++){ Faction f=new Faction(fs.getCompound(i)); d.factions.put(f.id,f); } return d; }
    @Override public NbtCompound writeNbt(NbtCompound n, RegistryWrapper.WrapperLookup lookup) { NbtList us=new NbtList(); units.values().forEach(u->us.add(u.toNbt())); n.put("units",us); NbtList fs=new NbtList(); factions.values().forEach(f->fs.add(f.toNbt())); n.put("factions",fs); return n; }
    public void saveUnit(UnitDefinition unit) { units.put(unit.id,unit); markDirty(); }
    public String firstUnit(){ return units.keySet().stream().findFirst().orElse(""); }
}
