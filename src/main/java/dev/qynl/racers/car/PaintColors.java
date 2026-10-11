package dev.qynl.racers.car;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.Formatting;

/** The paint palette: every car keeps three colours (body, secondary, accent). */
public final class PaintColors {
    public static final int WHITE = 0xF2F2F0;
    public static final int BLACK = 0x2A2A30;
    public static final int RED = 0xDC3B36;
    public static final int ORANGE = 0xF07A28;
    public static final int YELLOW = 0xF2C42A;
    public static final int LIME = 0x4FD05A;
    public static final int GREEN = 0x189454;
    public static final int CYAN = 0x3CC8DC;
    public static final int LIGHT_BLUE = 0x5B9CE8;
    public static final int BLUE = 0x2C5CC4;
    public static final int PURPLE = 0x8C3EBE;
    public static final int MAGENTA = 0xEC6CC4;
    public static final int PINK = 0xF090B8;
    public static final int BROWN = 0x8A5A2C;
    public static final int GRAY = 0x8E8E96;
    public static final int LIGHT_GRAY = 0xC8C8CE;

    private static final Map<String, Integer> NAMED = new LinkedHashMap<>();
    private static final Map<Integer, Formatting> CHAT = new LinkedHashMap<>();

    static {
        NAMED.put("white", WHITE);
        NAMED.put("black", BLACK);
        NAMED.put("red", RED);
        NAMED.put("orange", ORANGE);
        NAMED.put("yellow", YELLOW);
        NAMED.put("lime", LIME);
        NAMED.put("green", GREEN);
        NAMED.put("cyan", CYAN);
        NAMED.put("light_blue", LIGHT_BLUE);
        NAMED.put("blue", BLUE);
        NAMED.put("purple", PURPLE);
        NAMED.put("magenta", MAGENTA);
        NAMED.put("pink", PINK);
        NAMED.put("brown", BROWN);
        NAMED.put("gray", GRAY);
        NAMED.put("light_gray", LIGHT_GRAY);

        CHAT.put(WHITE, Formatting.WHITE);
        CHAT.put(BLACK, Formatting.DARK_GRAY);
        CHAT.put(RED, Formatting.RED);
        CHAT.put(ORANGE, Formatting.GOLD);
        CHAT.put(YELLOW, Formatting.YELLOW);
        CHAT.put(LIME, Formatting.GREEN);
        CHAT.put(GREEN, Formatting.DARK_GREEN);
        CHAT.put(CYAN, Formatting.DARK_AQUA);
        CHAT.put(LIGHT_BLUE, Formatting.AQUA);
        CHAT.put(BLUE, Formatting.BLUE);
        CHAT.put(PURPLE, Formatting.DARK_PURPLE);
        CHAT.put(MAGENTA, Formatting.LIGHT_PURPLE);
        CHAT.put(PINK, Formatting.LIGHT_PURPLE);
        CHAT.put(BROWN, Formatting.GOLD);
        CHAT.put(GRAY, Formatting.GRAY);
        CHAT.put(LIGHT_GRAY, Formatting.GRAY);
    }

    private PaintColors() {
    }

    public static Map<String, Integer> named() {
        return NAMED;
    }

    public static Integer byName(String name) {
        return NAMED.get(name);
    }

    public static Formatting chat(int rgb) {
        Formatting closest = Formatting.WHITE;
        double best = Double.MAX_VALUE;
        for (Map.Entry<Integer, Formatting> entry : CHAT.entrySet()) {
            double d = distance(entry.getKey(), rgb);
            if (d < best) {
                best = d;
                closest = entry.getValue();
            }
        }
        return closest;
    }

    private static double distance(int a, int b) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return Math.pow(ar - br, 2) + Math.pow(ag - bg, 2) + Math.pow(ab - bb, 2);
    }

    /** Next colour in the palette (used by the paint can item). */
    public static int next(int rgb, int direction) {
        Object[] values = NAMED.values().toArray();
        int index = 0;
        for (int i = 0; i < values.length; i++) {
            if ((Integer) values[i] == rgb) {
                index = i;
                break;
            }
        }
        index = Math.floorMod(index + direction, values.length);
        return (Integer) values[index];
    }

    public static String nameOf(int rgb) {
        for (Map.Entry<String, Integer> entry : NAMED.entrySet()) {
            if (entry.getValue() == rgb) {
                return entry.getKey().replace('_', ' ');
            }
        }
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    public static int defaultPrimary(CarType type) {
        return switch (type) {
            case KART -> ORANGE;
            case RALLY -> LIGHT_BLUE;
            case SPORT -> RED;
            case HYPER -> YELLOW;
            case F1 -> GREEN;
            case TRUCK -> BROWN;
            case DRIFT -> MAGENTA;
            case HOTROD -> 0x902C3C;
            case SHADOW -> 0x26243A;
        };
    }

    public static int defaultSecondary(CarType type) {
        return switch (type) {
            case KART -> YELLOW;
            case RALLY -> WHITE;
            case SPORT -> BLACK;
            case HYPER, F1 -> WHITE;
            case TRUCK -> GRAY;
            case DRIFT -> 0x342E3C;
            case HOTROD -> 0xCECEC6;
            case SHADOW -> PURPLE;
        };
    }
}
