package dev.qynl.myfirstmod.faction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.*;

public final class Faction {
    public String id;
    public String name;
    public String description;
    public String color = "#3B82F6";
    public final Map<String, FactionRelation> relations = new LinkedHashMap<>();
    public final Set<String> perks = new LinkedHashSet<>();

    public Faction(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
    }

    public Faction(String id, String name, String color, String description) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.description = description;
    }

    public Faction(NbtCompound nbt) {
        this.id = nbt.getString("id");
        this.name = nbt.getString("name");
        this.description = nbt.getString("description");
        this.color = nbt.contains("color") ? nbt.getString("color") : "#3B82F6";

        if (nbt.contains("perks")) {
            NbtList perksList = nbt.getList("perks", 8);
            for (int i = 0; i < perksList.size(); i++) {
                perks.add(perksList.getString(i));
            }
        }

        if (nbt.contains("relations")) {
            NbtCompound rels = nbt.getCompound("relations");
            for (String key : rels.getKeys()) {
                try {
                    relations.put(key, FactionRelation.valueOf(rels.getString(key)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", id == null ? "" : id);
        nbt.putString("name", name == null ? "" : name);
        nbt.putString("description", description == null ? "" : description);
        nbt.putString("color", color == null ? "#3B82F6" : color);

        NbtList perksList = new NbtList();
        for (String perk : perks) {
            perksList.add(NbtString.of(perk));
        }
        nbt.put("perks", perksList);

        NbtCompound rels = new NbtCompound();
        for (Map.Entry<String, FactionRelation> entry : relations.entrySet()) {
            rels.putString(entry.getKey(), entry.getValue().name());
        }
        nbt.put("relations", rels);

        return nbt;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.addProperty("color", color);

        JsonArray perksArr = new JsonArray();
        for (String perk : perks) {
            perksArr.add(perk);
        }
        json.add("perks", perksArr);

        JsonObject rels = new JsonObject();
        for (Map.Entry<String, FactionRelation> entry : relations.entrySet()) {
            rels.addProperty(entry.getKey(), entry.getValue().name());
        }
        json.add("relations", rels);

        return json;
    }

    public static Faction fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.has("name") ? json.get("name").getAsString() : id;
        Faction faction = new Faction(id, name);
        if (json.has("description")) {
            faction.description = json.get("description").getAsString();
        }
        if (json.has("color")) {
            faction.color = json.get("color").getAsString();
        }
        if (json.has("perks")) {
            JsonArray arr = json.getAsJsonArray("perks");
            for (int i = 0; i < arr.size(); i++) {
                faction.perks.add(arr.get(i).getAsString());
            }
        }
        if (json.has("relations")) {
            JsonObject rels = json.getAsJsonObject("relations");
            for (String key : rels.keySet()) {
                try {
                    faction.relations.put(key, FactionRelation.valueOf(rels.get(key).getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return faction;
    }

    public boolean isHostileTo(String targetFactionId) {
        if (targetFactionId == null || targetFactionId.equals(this.id)) return false;
        return relations.getOrDefault(targetFactionId, FactionRelation.NEUTRAL) == FactionRelation.HOSTILE;
    }

    public boolean isAlliedWith(String targetFactionId) {
        if (targetFactionId == null) return false;
        if (targetFactionId.equals(this.id)) return true;
        return relations.getOrDefault(targetFactionId, FactionRelation.NEUTRAL) == FactionRelation.ALLIED;
    }

    public boolean hasPerk(String perkId) {
        return perks.contains(perkId);
    }

    public boolean hasPerk(FactionPerk perk) {
        return perk != null && (perks.contains(perk.getId()) || perks.contains(perk.name().toLowerCase()));
    }

    public int getParsedColor() {
        try {
            if (color != null && color.startsWith("#")) {
                return (int) Long.parseLong(color.substring(1), 16);
            }
        } catch (Exception ignored) {
        }
        return 0x3B82F6;
    }
}
