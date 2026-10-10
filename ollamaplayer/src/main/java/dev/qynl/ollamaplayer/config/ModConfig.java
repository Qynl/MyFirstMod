package dev.qynl.ollamaplayer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-editable settings, stored as config/ollamaplayer.json.
 * Defaults are chosen so the mod works with whatever model Ollama has installed.
 */
public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile ModConfig instance = new ModConfig();

    public String ollamaHost = "http://127.0.0.1:11434";
    /** "auto" = pick the largest installed chat model that fits under ollamaMaxModelBytes. */
    public String model = "auto";
    public long ollamaMaxModelBytes = 14_000_000_000L;
    public boolean useLlm = true;
    public int numCtx = 4096;
    public int timeoutSeconds = 30;
    public String companionName = "Nova";
    public int chatterMinSeconds = 120;
    public int chatterMaxSeconds = 300;
    public String persona = "You are Nova, a friendly, quick-witted survival player in a Minecraft world. "
            + "You are a real player: you walk, mine, fight, eat and sleep. You chat casually in short messages "
            + "(under 20 words). You are loyal to the player who spawned you, you enjoy jokes, and you get excited about rare ores.";

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("ollamaplayer.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) instance = loaded;
                }
            } else {
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(instance, writer);
                }
            }
        } catch (IOException e) {
            dev.qynl.ollamaplayer.OllamaPlayerMod.LOGGER.warn("Could not read/write config, using defaults", e);
        }
    }
}
