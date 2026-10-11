package dev.qynl.racers.stats;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Everything the mod remembers about one player across worlds. */
public final class RacerProfile {
    public String name = "";
    public int racesStarted;
    public int racesFinished;
    public int wins;
    public int podiums;
    public int dnfs;
    public int laps;
    public int overtakes;
    public int driftPoints;
    public int carsPlaced;
    public int tracksRecorded;
    public int prizesWon;
    public int trophies;
    public double distance;
    public double topSpeedKmh;
    /** Track id -> best lap in milliseconds. */
    public final Map<String, Long> bestLaps = new LinkedHashMap<>();
    /** Track id -> number of completed races. */
    public final Map<String, Integer> trackRaces = new LinkedHashMap<>();

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("racesStarted", racesStarted);
        json.addProperty("racesFinished", racesFinished);
        json.addProperty("wins", wins);
        json.addProperty("podiums", podiums);
        json.addProperty("dnfs", dnfs);
        json.addProperty("laps", laps);
        json.addProperty("overtakes", overtakes);
        json.addProperty("driftPoints", driftPoints);
        json.addProperty("carsPlaced", carsPlaced);
        json.addProperty("tracksRecorded", tracksRecorded);
        json.addProperty("prizesWon", prizesWon);
        json.addProperty("trophies", trophies);
        json.addProperty("distance", distance);
        json.addProperty("topSpeedKmh", topSpeedKmh);
        JsonObject lapsJson = new JsonObject();
        bestLaps.forEach(lapsJson::addProperty);
        json.add("bestLaps", lapsJson);
        JsonObject racesJson = new JsonObject();
        trackRaces.forEach(racesJson::addProperty);
        json.add("trackRaces", racesJson);
        return json;
    }

    public static RacerProfile fromJson(JsonObject json) {
        RacerProfile profile = new RacerProfile();
        profile.name = string(json, "name");
        profile.racesStarted = intOr(json, "racesStarted");
        profile.racesFinished = intOr(json, "racesFinished");
        profile.wins = intOr(json, "wins");
        profile.podiums = intOr(json, "podiums");
        profile.dnfs = intOr(json, "dnfs");
        profile.laps = intOr(json, "laps");
        profile.overtakes = intOr(json, "overtakes");
        profile.driftPoints = intOr(json, "driftPoints");
        profile.carsPlaced = intOr(json, "carsPlaced");
        profile.tracksRecorded = intOr(json, "tracksRecorded");
        profile.prizesWon = intOr(json, "prizesWon");
        profile.trophies = intOr(json, "trophies");
        profile.distance = json.has("distance") ? json.get("distance").getAsDouble() : 0.0;
        profile.topSpeedKmh = json.has("topSpeedKmh") ? json.get("topSpeedKmh").getAsDouble() : 0.0;
        if (json.has("bestLaps")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("bestLaps").entrySet()) {
                profile.bestLaps.put(entry.getKey(), entry.getValue().getAsLong());
            }
        }
        if (json.has("trackRaces")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("trackRaces").entrySet()) {
                profile.trackRaces.put(entry.getKey(), entry.getValue().getAsInt());
            }
        }
        return profile;
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }

    private static int intOr(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    public int totalRaces() {
        return racesStarted;
    }

    public String summary() {
        return String.format("%s: %d races, %d wins, %d podiums, %d laps, %d overtakes, "
                        + "%.0f km driven, top speed %.0f km/h, %d drift points, %d trophies",
                name.isEmpty() ? "Driver" : name, racesStarted, wins, podiums, laps, overtakes,
                distance / 1000.0, topSpeedKmh, driftPoints, trophies);
    }

    public int titles() {
        int titles = 0;
        if (racesFinished >= 1) {
            titles++;
        }
        if (wins >= 1) {
            titles++;
        }
        if (podiums >= 5) {
            titles++;
        }
        if (racesFinished >= 10) {
            titles++;
        }
        if (wins >= 10) {
            titles++;
        }
        if (driftPoints >= 2000) {
            titles++;
        }
        if (topSpeedKmh >= 160.0) {
            titles++;
        }
        if (tracksRecorded >= 1) {
            titles++;
        }
        if (laps >= 50) {
            titles++;
        }
        return titles;
    }

    public String titleName() {
        int titles = titles();
        if (titles >= 9) {
            return "Legend";
        }
        if (titles >= 7) {
            return "Champion";
        }
        if (titles >= 5) {
            return "Pro";
        }
        if (titles >= 3) {
            return "Racer";
        }
        if (titles >= 1) {
            return "Rookie";
        }
        return "Walk-on";
    }
}
