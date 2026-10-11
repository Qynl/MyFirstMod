package dev.qynl.racers.car;

import java.util.List;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The nine stock cars. Every car is available as a vehicle item and shares the
 * car entity + renderer, only the numbers and the body spec change.
 */
public enum CarType {
    KART("kart", "Cart Kart", 13.0, 24.0, 11.0, 17.0, 6.0, 34.0, 1.35,
            0.0016, 0.62, 1.18, 1.05, 190.0, "3.1", Formatting.GREEN),
    RALLY("rally", "Rally Rascal", 11.5, 30.0, 13.0, 14.6, 5.2, 33.0, 2.25,
            0.0020, 1.05, 1.14, 0.95, 260.0, "350", Formatting.YELLOW),
    SPORT("sport", "Circuit Sport", 13.5, 36.0, 15.5, 16.4, 5.6, 31.0, 2.45,
            0.0019, 1.00, 1.15, 0.95, 280.0, "GT-R", Formatting.RED),
    HYPER("hyper", "Hyper Nova", 16.5, 42.0, 16.5, 17.4, 5.9, 30.0, 2.60,
            0.0017, 0.92, 1.20, 0.90, 300.0, "V12", Formatting.GOLD),
    F1("f1", "Formula Apex", 18.5, 46.0, 21.0, 20.2, 6.6, 32.0, 3.10,
            0.0026, 0.72, 1.26, 0.75, 320.0, "Apex", Formatting.AQUA),
    TRUCK("truck", "Ram Truck", 10.5, 26.0, 9.0, 10.6, 4.4, 36.0, 3.20,
            0.0024, 1.95, 1.05, 0.85, 460.0, "V8", Formatting.DARK_GREEN),
    DRIFT("drift", "Drift Queen", 12.5, 31.0, 13.5, 13.2, 4.6, 40.0, 2.45,
            0.0020, 1.00, 1.05, 1.45, 270.0, "S15", Formatting.LIGHT_PURPLE),
    HOTROD("hotrod", "Hot Rod", 15.5, 34.0, 9.0, 12.0, 4.4, 36.0, 2.90,
            0.0028, 1.22, 1.10, 1.10, 300.0, "V8 Blown", Formatting.DARK_RED),
    /** Secret car - quick, light, and slightly unnerving. */
    SHADOW("shadow", "Shadow", 19.5, 48.0, 22.0, 21.0, 6.8, 30.0, 2.95,
            0.0015, 0.68, 1.30, 0.80, 340.0, "Null", Formatting.DARK_PURPLE);

    public final String id;
    public final String displayName;
    public final double basePower;
    public final double baseTopSpeed;
    public final double baseBrakes;
    public final double baseGrip;
    public final double baseLatStiffness;
    public final double baseSteer;
    public final double baseWheelBase;
    public final double baseDrag;
    public final double baseMass;
    public final double baseBoostMul;
    public final double driftBias;
    public final double baseHealth;
    public final String engineName;
    public final Formatting color;

    CarType(String id, String displayName, double power, double topSpeed, double brakes,
            double grip, double latStiffness, double steer, double wheelBase, double drag,
            double mass, double boostMul, double driftBias, double health, String engineName,
            Formatting color) {
        this.id = id;
        this.displayName = displayName;
        this.basePower = power;
        this.baseTopSpeed = topSpeed;
        this.baseBrakes = brakes;
        this.baseGrip = grip;
        this.baseLatStiffness = latStiffness;
        this.baseSteer = steer;
        this.baseWheelBase = wheelBase;
        this.baseDrag = drag;
        this.baseMass = mass;
        this.baseBoostMul = boostMul;
        this.driftBias = driftBias;
        this.baseHealth = health;
        this.engineName = engineName;
        this.color = color;
    }

    public static CarType byId(String id) {
        for (CarType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return SPORT;
    }

    public static List<CarType> stock() {
        return List.of(KART, RALLY, SPORT, HYPER, F1, TRUCK, DRIFT, HOTROD);
    }

    public CarSpec spec() {
        return CarSpec.of(id);
    }

    public Text label() {
        return Text.literal(displayName).formatted(color);
    }

    public String itemId() {
        return "car_" + id;
    }

    public String textureName() {
        return id;
    }

    /** Shop price in emeralds, and how many emeralds the owner gets when selling. */
    public int price() {
        return switch (this) {
            case KART -> 3;
            case RALLY, DRIFT -> 6;
            case SPORT, HOTROD -> 8;
            case TRUCK -> 9;
            case HYPER -> 12;
            case F1 -> 16;
            case SHADOW -> 24;
        };
    }
}
