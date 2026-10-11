package dev.qynl.ollamaplayer.companion;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic checks that raise events for the strategist: low supplies, and a worthwhile resource
 * in view. They only decide whether the model should be asked; they never move the body.
 */
public final class Needs {
    private Needs() {}

    public static List<String> restockReasons(int food, int torches, double pickaxeFraction, double bagFraction) {
        List<String> r = new ArrayList<>();
        if (food < 12) r.add("food is low");
        if (torches < 4) r.add("few torches left");
        if (pickaxeFraction < 0.25) r.add("pickaxe nearly broken");
        if (bagFraction > 0.85) r.add("bag nearly full");
        return r;
    }

    /** A cluster of valuable blocks close by, with a low-risk situation, is worth a look. */
    public static boolean worthInvestigating(int valuableNearby, WorldState.Risk risk) {
        return valuableNearby >= 6 && risk == WorldState.Risk.LOW;
    }
}
