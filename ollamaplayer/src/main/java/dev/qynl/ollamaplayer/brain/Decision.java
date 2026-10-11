package dev.qynl.ollamaplayer.brain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A structured strategic decision from the model. Every field is validated: unknown names,
 * out-of-range priorities and unknown steps are replaced with safe values, and anything that is not
 * JSON at all becomes {@link #SAFE}. The model never controls the body directly.
 */
public record Decision(String name, String reason, double priority, List<String> steps) {
    public static final Set<String> NAMES = Set.of("NONE", "FOLLOW", "HOLD", "MINE", "RESTOCK", "FIGHT",
            "RETREAT", "CRAFT_GEAR", "EXPLORE_CAVE", "RETURN_HOME");
    public static final Set<String> STEPS = Set.of("eat", "stash", "follow", "stay", "gear");
    public static final Decision SAFE = new Decision("NONE", "", 0.0, List.of());

    public static Decision parse(String content) {
        if (content == null) return SAFE;
        try {
            JsonElement el = JsonParser.parseString(content.trim());
            if (!el.isJsonObject()) return SAFE;
            JsonObject o = el.getAsJsonObject();
            String name = str(o, "decision");
            name = name == null ? "NONE" : name.toUpperCase(Locale.ROOT);
            if (!NAMES.contains(name)) name = "NONE";
            String reason = str(o, "reason");
            if (reason == null) reason = "";
            if (reason.length() > 200) reason = reason.substring(0, 200);
            double priority = 0.0;
            if (o.has("priority") && o.get("priority").isJsonPrimitive() && o.get("priority").getAsJsonPrimitive().isNumber()) {
                priority = o.get("priority").getAsDouble();
                if (Double.isNaN(priority)) priority = 0.0;
                priority = Math.max(0.0, Math.min(1.0, priority));
            }
            List<String> steps = new ArrayList<>();
            if (o.has("next_steps") && o.get("next_steps").isJsonArray()) {
                JsonArray arr = o.getAsJsonArray("next_steps");
                for (JsonElement e : arr) {
                    if (steps.size() >= 5) break;
                    if (e.isJsonPrimitive()) {
                        String s = e.getAsString();
                        if (STEPS.contains(s)) steps.add(s);
                    }
                }
            }
            return new Decision(name, reason, priority, List.copyOf(steps));
        } catch (RuntimeException e) {
            return SAFE;
        }
    }

    /** Maps the decision onto one of the body's existing actions. Unmapped decisions keep doing what they do. */
    public String toAction() {
        return switch (name) {
            case "MINE" -> "mine";
            case "FOLLOW", "RETURN_HOME", "RETREAT" -> "follow";
            case "HOLD" -> "stay";
            case "CRAFT_GEAR" -> "gear";
            case "RESTOCK" -> "stash";
            default -> "none";
        };
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
