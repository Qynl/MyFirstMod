package dev.qynl.racers.car;

/**
 * The numbers the physics actually run on. Base values come from the car type,
 * every tuning level then multiplies them.
 *
 * <p>Units are Minecraft-ish metric: blocks (== metres) per second, m/s^2 for
 * accelerations, degrees for angles.
 */
public record CarStats(double power, double topSpeed, double brakeForce, double grip,
                       double latStiffness, double maxSteerDeg, double wheelbase,
                       double drag, double mass, double boostMul, double driftBias,
                       double maxHealth) {

    public static CarStats of(CarType type, int[] levels) {
        double engine = 1.0 + 0.115 * lv(levels, Tune.ENGINE);
        double gearbox = 1.0 + 0.055 * lv(levels, Tune.GEARBOX);
        double brakes = 1.0 + 0.11 * lv(levels, Tune.BRAKES);
        double tires = 1.0 + 0.075 * lv(levels, Tune.TIRES);
        double aero = 1.0 + 0.07 * lv(levels, Tune.AERO);
        double chassis = 1.0 + 0.06 * lv(levels, Tune.CHASSIS);
        double boost = 1.0 + 0.25 * lv(levels, Tune.BOOST);

        double weight = type.baseMass * (1.0 - 0.03 * lv(levels, Tune.CHASSIS));
        return new CarStats(
                type.basePower * engine,
                type.baseTopSpeed * gearbox * (1.0 + 0.02 * lv(levels, Tune.ENGINE)),
                type.baseBrakes * brakes,
                type.baseGrip * tires * (1.0 + 0.5 * aero) * (1.0 - 0.02 * lv(levels, Tune.CHASSIS)),
                type.baseLatStiffness * tires * (1.0 + 0.4 * aero),
                type.baseSteer,
                type.baseWheelBase,
                type.baseDrag * (1.0 - 0.12 * lv(levels, Tune.AERO)),
                weight,
                type.baseBoostMul * boost,
                type.driftBias,
                type.baseHealth * (1.0 + 0.18 * lv(levels, Tune.CHASSIS)));
    }

    private static int lv(int[] levels, Tune tune) {
        if (levels == null || tune.ordinal() >= levels.length) {
            return 0;
        }
        return Math.max(0, Math.min(tune.maxLevel, levels[tune.ordinal()]));
    }

    /** Top speed in km/h, for the HUD. */
    public double topSpeedKmh() {
        return topSpeed * 3.6;
    }

    /** 0..1 rating used by the garage screen bars. */
    public double rating(int index) {
        return switch (index) {
            case 0 -> clamp01(power / 30.0);
            case 1 -> clamp01(topSpeed / 60.0);
            case 2 -> clamp01(brakeForce / 32.0);
            case 3 -> clamp01(grip / 28.0);
            default -> clamp01(maxHealth / 400.0);
        };
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
