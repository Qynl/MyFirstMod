package dev.qynl.ollamaplayer.companion;

/**
 * The compact situation the model normally sees: a few labelled lines of facts that code already knows,
 * plus a risk level. Bounded in size, so it stays cheap to send however big the world is.
 */
public final class WorldState {
    private WorldState() {}

    /** Everything the state needs, as plain values. */
    public record Facts(float health, int food, int armorPieces, double pickaxeFraction, double bagFraction,
                        int torches, boolean night, boolean underground, double ownerDistance,
                        int hostiles, int valuableNearby, String goal, String mode) {}

    public enum Risk { LOW, MEDIUM, HIGH }

    public static Risk risk(Facts f) {
        if (f.health() < 8 || f.hostiles() >= 3) return Risk.HIGH;
        if (f.health() < 12 || f.hostiles() >= 1 || (f.night() && f.underground())) return Risk.MEDIUM;
        return Risk.LOW;
    }

    public static String render(Facts f) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nova: health ").append(Math.round(f.health())).append("/20, food ").append(f.food())
                .append("/20, armor ").append(f.armorPieces()).append("/4, pickaxe ")
                .append(Math.round(f.pickaxeFraction() * 100)).append("%, bag ")
                .append(Math.round(f.bagFraction() * 100)).append("% full, torches ").append(f.torches()).append(".\n");
        sb.append("Where: ").append(f.underground() ? "underground" : "surface").append(", ")
                .append(f.night() ? "night" : "day").append(". Owner: ")
                .append(f.ownerDistance() < 0 ? "none nearby" : Math.round(f.ownerDistance()) + " blocks away").append(".\n");
        sb.append("Nearby: ").append(f.hostiles()).append(" hostile, ").append(f.valuableNearby()).append(" valuable blocks.\n");
        sb.append("Goal: ").append(f.goal() == null || f.goal().isBlank() ? "none" : f.goal())
                .append(". Mode: ").append(f.mode()).append(".\n");
        sb.append("Risk: ").append(risk(f)).append(".");
        return sb.toString();
    }
}
