package dev.qynl.ollamaplayer.ollama;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qynl.ollamaplayer.OllamaPlayerMod;
import dev.qynl.ollamaplayer.config.ModConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal, dependency-free client for Ollama's HTTP API (/api/tags and /api/chat).
 * All network work happens on a daemon thread; the game thread never blocks on it.
 */
public final class OllamaClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ollamaplayer-llm");
        t.setDaemon(true);
        return t;
    });

    private volatile String model;
    private volatile long lastDiscovery;
    private volatile String status = "not checked yet";

    public boolean isReady() {
        return model != null;
    }

    public String model() {
        return model;
    }

    public String status() {
        return status;
    }

    /** Looks up installed models in the background. Throttled so a dead Ollama is not hammered. */
    public void discoverAsync() {
        long now = System.currentTimeMillis();
        if (now - lastDiscovery < 15_000L) return;
        lastDiscovery = now;
        POOL.execute(this::discover);
    }

    private void discover() {
        ModConfig cfg = ModConfig.get();
        try {
            if (!"auto".equalsIgnoreCase(cfg.model)) {
                model = cfg.model;
                status = "using configured model " + cfg.model;
                return;
            }
            JsonObject tags = getJson(cfg.ollamaHost + "/api/tags");
            JsonArray models = tags.has("models") ? tags.getAsJsonArray("models") : new JsonArray();
            String best = null;
            long bestSize = -1;
            String smallest = null;
            long smallestSize = Long.MAX_VALUE;
            for (JsonElement el : models) {
                JsonObject m = el.getAsJsonObject();
                String name = m.get("name").getAsString();
                long size = m.has("size") ? m.get("size").getAsLong() : 0L;
                if (name.contains("embed")) continue;
                if (size < smallestSize) {
                    smallestSize = size;
                    smallest = name;
                }
                if (size <= cfg.ollamaMaxModelBytes && size > bestSize) {
                    bestSize = size;
                    best = name;
                }
            }
            if (best == null) best = smallest;
            model = best;
            status = best == null ? "Ollama is running but has no models. Run: ollama pull <model>" : "auto-selected " + best;
        } catch (Exception e) {
            model = null;
            status = "Ollama unreachable at " + cfg.ollamaHost + " (" + e.getMessage() + ")";
            OllamaPlayerMod.LOGGER.info("Ollama discovery failed: {}", e.getMessage());
        }
    }

    /**
     * Sends a non-streaming chat request. If {@code format} is non-null it is passed as Ollama's
     * structured-output JSON schema, which works on any model (no tool-calling support needed).
     * Returns the assistant message content.
     */
    public CompletableFuture<String> chat(List<JsonObject> messages, JsonObject format) {
        String m = model;
        if (m == null) {
            discoverAsync();
            return CompletableFuture.failedFuture(new IllegalStateException("Ollama model not ready"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doChat(m, messages, format);
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, POOL).whenComplete((content, err) -> {
            if (err != null) {
                status = "chat failed: " + err.getMessage();
                model = null; // re-discover next time (handles model removed / Ollama restarted)
            }
        });
    }

    private String doChat(String m, List<JsonObject> messages, JsonObject format) throws IOException, InterruptedException {
        ModConfig cfg = ModConfig.get();
        JsonObject body = new JsonObject();
        body.addProperty("model", m);
        body.addProperty("stream", false);
        body.addProperty("keep_alive", "10m");
        JsonArray arr = new JsonArray();
        for (JsonObject msg : messages) arr.add(msg);
        body.add("messages", arr);
        if (format != null) body.add("format", format);
        JsonObject opts = new JsonObject();
        opts.addProperty("num_ctx", cfg.numCtx);
        opts.addProperty("temperature", 0.7);
        opts.addProperty("num_predict", 220);
        body.add("options", opts);

        HttpRequest req = HttpRequest.newBuilder(URI.create(cfg.ollamaHost + "/api/chat"))
                .timeout(Duration.ofSeconds(cfg.timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("Ollama HTTP " + res.statusCode() + ": " + res.body());
        }
        JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
        status = "ok (" + m + ")";
        return root.getAsJsonObject("message").get("content").getAsString();
    }

    private static JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(4)).GET().build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IOException("HTTP " + res.statusCode());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }
}
