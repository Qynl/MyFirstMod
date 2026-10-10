package dev.qynl.ollamaplayer.brain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword understanding used when Ollama is off, slow, or failing. It keeps the companion
 * useful with zero AI, so the mod never feels dead.
 */
public final class Heuristics {
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,2})");
    private static final Pattern MINE_VERB = Pattern.compile("\\b(mine|get|collect|chop|gather|dig|fetch|bring|find)\\b");
    private static final Pattern EAT = Pattern.compile("\\beat\\b");

    private static final String[][] TARGETS = {
            {"cobble", "cobblestone"}, {"stone", "stone"}, {"iron", "iron_ore"}, {"coal", "coal_ore"},
            {"diamond", "diamond_ore"}, {"gold", "gold_ore"}, {"copper", "copper_ore"},
            {"redstone", "redstone_ore"}, {"emerald", "emerald_ore"}, {"wood", "log"}, {"log", "log"},
            {"tree", "log"}, {"sand", "sand"}, {"dirt", "dirt"}, {"gravel", "gravel"}
    };

    private Heuristics() {}

    public static Reply reply(String raw) {
        String t = raw.toLowerCase(Locale.ROOT);

        if (EAT.matcher(t).find() || t.contains("hungry")) return new Reply("Om nom.", "eat", null, 0);
        if (t.contains("follow") || t.contains("come") || t.contains("with me")) return new Reply("On my way!", "follow", null, 0);
        if (t.contains("stay") || t.contains("wait") || t.contains("stop") || t.contains("guard")) {
            return new Reply("Holding here.", "stay", null, 0);
        }
        if (t.contains("status") || t.contains("what are you doing") || t.contains("how are you")) {
            return new Reply("", "status", null, 0);
        }
        if (MINE_VERB.matcher(t).find()) {
            String target = findTarget(t);
            if (target != null) {
                int count = count(t);
                return new Reply("Alright, going for " + count + " " + target.replace('_', ' ') + ".", "mine", target, count);
            }
        }
        return Reply.none("My brain is offline right now. I understand: follow, stay, eat, status, and mine <logs|stone|iron|coal>.");
    }

    private static String findTarget(String t) {
        for (String[] pair : TARGETS) {
            if (t.contains(pair[0])) return pair[1];
        }
        return null;
    }

    private static int count(String t) {
        Matcher m = NUMBER.matcher(t);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return Math.max(1, Math.min(64, n));
        }
        return 8;
    }
}
