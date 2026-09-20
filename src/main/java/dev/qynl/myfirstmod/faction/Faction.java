package dev.qynl.myfirstmod.faction;

import net.minecraft.nbt.NbtCompound;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class Faction {
    public enum Relation { ALLIED, NEUTRAL, HOSTILE }
    public String id, name, description, color = "#6c8cff";
    public final Map<String, Relation> relations = new HashMap<>();
    public final Set<String> perks = new HashSet<>();
    public Faction(String id, String name) { this.id = id; this.name = name; }
    public Faction(NbtCompound n) { id=n.getString("id"); name=n.getString("name"); description=n.getString("description"); color=n.getString("color"); var ps=n.getList("perks",8); for(int i=0;i<ps.size();i++) perks.add(ps.getString(i)); NbtCompound r=n.getCompound("relations"); for(String key:r.getKeys()) try { relations.put(key,Relation.valueOf(r.getString(key))); } catch (IllegalArgumentException ignored) { } }
    public NbtCompound toNbt() { NbtCompound n=new NbtCompound(); n.putString("id",id); n.putString("name",name); n.putString("description",description==null?"":description); n.putString("color",color); var ps=new net.minecraft.nbt.NbtList(); perks.forEach(p->ps.add(net.minecraft.nbt.NbtString.of(p))); n.put("perks",ps); NbtCompound r=new NbtCompound(); relations.forEach((k,v)->r.putString(k,v.name())); n.put("relations",r); return n; }
}
