package dev.qynl.ollamaplayer.companion;

import java.util.Locale;

/**
 * Deterministic combat judgement: fight, keep distance, or retreat. Pure arithmetic over numbers the
 * game already knows, so it runs every tick for free and can be unit-tested without a world.
 *
 * <p>Score = our strength minus the threat of what is near. Creepers and ranged mobs count for more
 * because they punish standing still; a low health pool lowers our strength quickly.
 */
public final class Tactics {
    private Tactics() {}

    public enum Stance { FIGHT, AVOID, RETREAT }

    /** Snapshot of the fight, all plain numbers. */
    public record Situation(String enemyType, double enemyDistance, int enemyCount, float health,
                            boolean hasSword, int armorPieces, boolean hasShield,
                            double ownerDistance, int retreatBonus) {}

    /** Rough threat of one mob of this type, 0..10. */
    public static double threat(String enemyType) {
        return switch (enemyType.toLowerCase(Locale.ROOT)) {
            case "creeper" -> 6.0;          // one hit can be huge, and it hides its approach
            case "skeleton", "stray", "pillager", "witch" -> 3.0;  // ranged: punishes standing still
            case "zombie", "husk", "drowned", "spider", "cave_spider" -> 2.0;
            case "enderman" -> 2.5;
            case "ravager", "vindicator", "ghast", "blaze" -> 5.0;
            case "warden", "wither_skeleton", "wither" -> 9.0;
            default -> 2.0;
        };
    }

    /** How capable Nova is right now, roughly 0..3. */
    public static double strength(float health, boolean hasSword, int armorPieces, boolean hasShield) {
        double base = Math.max(0.0, health) / 20.0;
        double weapon = hasSword ? 1.5 : 0.7;
        double armor = 1.0 + 0.08 * Math.min(4, armorPieces);
        double shield = hasShield ? 1.15 : 1.0;
        return base * weapon * armor * shield * 2.0;
    }

    /** The retreat line: at or below this health, Nova stops trading hits. */
    public static float retreatHealth(int retreatBonus) {
        return 7.0F + Math.max(0, Math.min(3, retreatBonus));
    }

    public static Stance decide(Situation s) {
        double dist = Math.max(1.0, s.enemyDistance());
        // Closer and more numerous enemies weigh more.
        double danger = threat(s.enemyType()) * Math.max(1, s.enemyCount()) * (8.0 / (dist + 4.0));
        double score = strength(s.health(), s.hasSword(), s.armorPieces(), s.hasShield()) - danger;

        if (s.health() <= retreatHealth(s.retreatBonus())) {
            return Stance.RETREAT;
        }
        if (score >= 0) return Stance.FIGHT;
        // Too dangerous to trade; AVOID and RETREAT both fall back to the owner when there is one.
        if (score < -4.0) return Stance.RETREAT;
        return Stance.AVOID;
    }
}
