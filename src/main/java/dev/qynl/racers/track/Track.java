package dev.qynl.racers.track;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.math.MathHelper;

/**
 * A circuit definition, stored as JSON (stock tracks ship inside the jar, player
 * made ones live in {@code <world>/fabricracers/tracks/}).
 *
 * <p>All coordinates are "local": 0,0 is the corner of the structure file and
 * the road always starts heading +X. A {@link TrackPlacement} turns them into
 * world coordinates.
 */
public final class Track {
    public final String id;
    public final String name;
    public final String shortName;
    public final String structure;
    public final String author;
    public final int width;
    public final int defaultLaps;
    public final int[] size;
    public final List<double[]> grid = new ArrayList<>();
    public final List<double[]> checkpoints = new ArrayList<>();
    public double[] pitEntry;
    public double[] pitExit;
    public double[] pitBox;
    public final boolean stock;

    private Track(String id, String name, String shortName, String structure, String author,
                  int width, int defaultLaps, int[] size, boolean stock) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.structure = structure;
        this.author = author;
        this.width = Math.max(3, width);
        this.defaultLaps = Math.max(1, defaultLaps);
        this.size = size;
        this.stock = stock;
    }

    public static Track fromJson(String id, JsonObject json, int[] size, boolean stock) {
        Track track = new Track(id,
                string(json, "name", id),
                string(json, "short_name", id.substring(0, Math.min(3, id.length())).toUpperCase()),
                string(json, "structure", "fabricracers:tracks/" + id),
                string(json, "author", stock ? "FabricRacers" : "player"),
                intOr(json, "width", 7),
                intOr(json, "default_laps", 2),
                size == null ? new int[]{1, 1, 1} : size,
                stock);
        if (json.has("grid")) {
            for (JsonElement element : json.getAsJsonArray("grid")) {
                JsonArray point = element.getAsJsonArray();
                track.grid.add(new double[]{num(point, 0), num(point, 2)});
            }
        }
        if (json.has("checkpoints")) {
            for (JsonElement element : json.getAsJsonArray("checkpoints")) {
                JsonArray point = element.getAsJsonArray();
                track.checkpoints.add(new double[]{num(point, 0), num(point, 2),
                        point.size() > 3 ? num(point, 3) : 0.0});
            }
        }
        if (json.has("pit") && json.get("pit").isJsonObject()) {
            JsonObject pit = json.getAsJsonObject("pit");
            track.pitEntry = pitArray(pit, "entry");
            track.pitExit = pitArray(pit, "exit");
            track.pitBox = pitArray(pit, "box");
        }
        return track;
    }

    private static double[] pitArray(JsonObject pit, String key) {
        if (!pit.has(key)) {
            return null;
        }
        JsonArray array = pit.getAsJsonArray(key);
        double[] out = new double[array.size()];
        for (int i = 0; i < array.size(); i++) {
            out[i] = array.get(i).getAsDouble();
        }
        return out;
    }

    private static double num(JsonArray array, int index) {
        return index < array.size() ? array.get(index).getAsDouble() : 0.0;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    public int checkpointCount() {
        return checkpoints.size();
    }

    public double halfWidth() {
        return (width - 1) / 2.0;
    }

    public int lapLength() {
        double total = 0.0;
        for (int i = 0; i < checkpoints.size(); i++) {
            double[] a = checkpoints.get(i);
            double[] b = checkpoints.get((i + 1) % checkpoints.size());
            total += Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
        return (int) Math.round(total);
    }

    public String sizeLabel() {
        return size[0] + "x" + size[2];
    }

    public int gridSlots() {
        return Math.max(4, grid.size());
    }

    public String describe() {
        return name + " (" + sizeLabel() + ", " + checkpointCount() + " checkpoints, ~"
                + lapLength() + " blocks per lap)";
    }

    public int suggestLaps(double desiredLength) {
        int lap = Math.max(60, lapLength());
        return MathHelper.clamp((int) Math.round(desiredLength / lap), 1, 12);
    }
}
