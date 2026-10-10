package dev.qynl.ollamaplayer.brain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.qynl.ollamaplayer.OllamaPlayerMod;
import dev.qynl.ollamaplayer.companion.Companion;
import dev.qynl.ollamaplayer.companion.CompanionManager;
import dev.qynl.ollamaplayer.config.ModConfig;
import dev.qynl.ollamaplayer.ollama.OllamaClient;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * Connects chat and idle moments to the language model and applies its decisions on the server thread.
 * One request in flight at a time; if the model is unavailable, {@link Heuristics} takes over.
 */
public final class Brain {
    private static final int HISTORY_LINES = 12;

    private final CompanionManager manager;
    private final OllamaClient llm = new OllamaClient();
    private final Deque<String> history = new ArrayDeque<>();
    private final ConcurrentLinkedQueue<Queued> replies = new ConcurrentLinkedQueue<>();
    private record Queued(boolean idle, Reply reply) {}
    private final Random random = new Random();
    private final JsonObject responseFormat = buildResponseFormat();

    private volatile boolean inFlight;
    private String pendingText;
    private String pendingWho;
    private int chatterTimer = 20 * 60;

    public Brain(CompanionManager manager) {
        this.manager = manager;
        llm.discoverAsync();
    }

    public OllamaClient llm() {
        return llm;
    }

    public synchronized void remember(String line) {
        history.addLast(line);
        while (history.size() > HISTORY_LINES) history.removeFirst();
    }

    /** Server thread. Called for every chat line on the server. */
    public void onChat(ServerPlayerEntity sender, String text) {
        String who = sender.getName().getString();
        remember(who + ": " + text);

        String name = ModConfig.get().companionName;
        if (!text.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) return;
        if (manager.companion() == null) {
            manager.speak("I'm not out here yet. Someone run /nova spawn.");
            return;
        }
        String cleaned = Pattern.compile(Pattern.quote(name), Pattern.CASE_INSENSITIVE).matcher(text).replaceAll("").trim();
        if (cleaned.isEmpty()) cleaned = "(just called your name)";
        request("player", who, cleaned);
    }

    /** Server thread. Applies finished replies and fires idle chatter. */
    public void tick(@Nullable Companion c, @Nullable ServerPlayerEntity owner) {
        Queued q;
        while ((q = replies.poll()) != null) {
            apply(q, c);
        }

        if (pendingText != null && !inFlight) {
            String text = pendingText;
            String who = pendingWho;
            pendingText = null;
            request("player", who, text);
        }

        if (c == null || owner == null || inFlight) return;
        if (--chatterTimer > 0) return;
        ModConfig cfg = ModConfig.get();
        int min = Math.max(20, cfg.chatterMinSeconds);
        int max = Math.max(min, cfg.chatterMaxSeconds);
        chatterTimer = (min + random.nextInt(max - min + 1)) * 20;
        if (c.player().squaredDistanceTo(owner) > 24 * 24) return;
        request("idle", "", "");
    }

    private void apply(Queued q, @Nullable Companion c) {
        Reply r = q.reply();
        if (r.say() != null && !r.say().isBlank()) {
            manager.speak(r.say());
        }
        if (c == null || "none".equals(r.action())) return;
        // Idle thoughts may chat and may pick up mining while following, but never override an explicit order.
        if (q.idle() && (c.mode() != Companion.Mode.FOLLOW || !"mine".equals(r.action()))) return;
        String note = c.command(r.action(), r.target(), r.count());
        if (note != null) manager.speak(note);
    }

    private void request(String trigger, String who, String text) {
        ModConfig cfg = ModConfig.get();
        boolean player = "player".equals(trigger);

        if (!cfg.useLlm || !llm.isReady()) {
            llm.discoverAsync();
            if (player) replies.add(new Queued(false, Heuristics.reply(text)));
            return;
        }
        if (inFlight) {
            if (player) {
                pendingText = text;
                pendingWho = who;
            }
            return;
        }

        inFlight = true;
        List<JsonObject> messages = buildMessages(trigger, who, text);
        llm.chat(messages, responseFormat).whenComplete((content, err) -> {
            inFlight = false;
            boolean idle = !player;
            if (err != null) {
                OllamaPlayerMod.LOGGER.info("Brain request failed: {}", err.getMessage());
                if (player) replies.add(new Queued(false, Heuristics.reply(text)));
                return;
            }
            Reply reply = Reply.parse(content);
            if (player && reply.say().isBlank() && "none".equals(reply.action())) {
                reply = Heuristics.reply(text);
            }
            replies.add(new Queued(idle, reply));
        });
    }

    private List<JsonObject> buildMessages(String trigger, String who, String text) {
        ModConfig cfg = ModConfig.get();
        String name = cfg.companionName;
        String system = cfg.persona + "\n\n"
                + "You control your body with the 'action' field. Allowed actions:\n"
                + "- none: keep doing what you are doing.\n"
                + "- follow: walk back to your owner and stay close.\n"
                + "- stay: stop and guard this spot.\n"
                + "- mine: collect blocks. Put a keyword in 'target' (log, stone, cobblestone, coal_ore, iron_ore, "
                + "diamond_ore, sand, dirt) and how many in 'count'.\n"
                + "- craft: make an item, e.g. target 'stone_pickaxe', 'furnace', 'iron_helmet', 'torch'. Gathers what is missing, places a crafting table or furnace if needed, and smelts ores.\n"
                + "- gear: work toward a full iron kit (iron pickaxe, sword, shield, helmet, chestplate, leggings, boots).\n"
                + "- eat: eat food from your inventory.\n"
                + "- status: report what you are doing.\n"
                + "Reply ONLY with JSON: {\"say\": \"<chat line, empty to stay silent>\", \"action\": \"<allowed action>\", "
                + "\"target\": \"<keyword or empty>\", \"count\": <number>}.\n"
                + "Never claim an action you did not take. Never invent items you do not have. You are " + name + ".";

        Companion c = manager.companion();
        String state = c == null ? "You are not currently in the world." : c.describe(manager.owner());
        StringBuilder user = new StringBuilder();
        user.append("Your state:\n").append(state).append("\nRecent chat:\n");
        synchronized (this) {
            for (String h : history) user.append(h).append('\n');
        }
        user.append('\n');
        if ("player".equals(trigger)) {
            user.append(who).append(" just said to you: \"").append(text).append("\"\nRespond to them and act if asked.");
        } else {
            user.append("Nobody spoke to you. Say one short in-character remark about what you are doing or seeing, "
                    + "or choose a sensible next action. Use action none if nothing needs to change.");
        }

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", system);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", user.toString());
        return List.of(sys, usr);
    }

    private static JsonObject buildResponseFormat() {
        JsonObject props = new JsonObject();
        props.add("say", typed("string"));
        JsonObject action = typed("string");
        JsonArray enumValues = new JsonArray();
        for (String a : Reply.ACTIONS) enumValues.add(a);
        action.add("enum", enumValues);
        props.add("action", action);
        props.add("target", typed("string"));
        props.add("count", typed("integer"));

        JsonArray required = new JsonArray();
        required.add("say");
        required.add("action");

        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        schema.add("required", required);
        return schema;
    }

    private static JsonObject typed(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        return o;
    }
}
