package dev.qynl.ollamaplayer.brain;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Set;

/** A decoded decision: what to say and which single action to take. */
public record Reply(String say, String action, String target, int count) {
    public static final Set<String> ACTIONS = Set.of("none", "follow", "stay", "mine", "eat", "status");

    public static Reply none(String say) {
        return new Reply(say, "none", null, 0);
    }

    /** Parses the model's JSON. If the model ignored the format, treat the whole text as speech. */
    public static Reply parse(String content) {
        try {
            JsonObject o = JsonParser.parseString(content.trim()).getAsJsonObject();
            String say = str(o, "say");
            String action = str(o, "action");
            if (action == null || !ACTIONS.contains(action)) action = "none";
            String target = str(o, "target");
            if (target != null && target.isBlank()) target = null;
            int count = o.has("count") && o.get("count").isJsonPrimitive() ? o.get("count").getAsInt() : 0;
            return new Reply(say == null ? "" : say, action, target, count);
        } catch (Exception e) {
            String text = content.trim();
            if (text.length() > 200) text = text.substring(0, 200);
            return none(text);
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
