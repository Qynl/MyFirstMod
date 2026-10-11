package dev.qynl.ollamaplayer.companion;

/**
 * Deterministic go/no-go check before a mining trip. Returns the reason to postpone, or null to go.
 * Pure arithmetic so it is cheap and testable.
 */
public final class MiningTrip {
    private MiningTrip() {}

    /**
     * @param health       current health, 0..20
     * @param food         food level, 0..20
     * @param toolFraction durability left on the best pickaxe, 0..1 (1 if none is needed)
     * @param bagFraction  share of the 36 main slots in use, 0..1
     */
    public static String postponeReason(float health, int food, double toolFraction, double bagFraction) {
        if (health < 10) return "my health is too low for a mining trip";
        if (food < 12) return "I need to eat before a mining trip";
        if (toolFraction < 0.2) return "my pickaxe is nearly broken";
        if (bagFraction > 0.9) return "my bag is nearly full";
        return null;
    }
}
