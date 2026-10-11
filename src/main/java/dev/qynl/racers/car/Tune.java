package dev.qynl.racers.car;

import java.util.List;

/**
 * The seven tuning systems a car can be upgraded with. Each level costs more
 * resources (see {@link dev.qynl.racers.shop.PartShop}) and stacks with the
 * car's base {@link CarStats}.
 */
public enum Tune {
    ENGINE("engine", "Engine", 4, "Power and acceleration"),
    GEARBOX("gearbox", "Gearbox", 3, "Top speed and shift speed"),
    BRAKES("brakes", "Brakes", 3, "Braking force and balance"),
    TIRES("tires", "Tyres", 4, "Grip, cornering speed, drift control"),
    AERO("aero", "Aero", 3, "Downforce, high speed grip, less drag"),
    CHASSIS("chassis", "Chassis", 3, "Weight, crash resistance, health"),
    BOOST("boost", "Nitro", 3, "Boost capacity and recharge rate");

    public final String id;
    public final String displayName;
    public final int maxLevel;
    public final String description;

    Tune(String id, String displayName, int maxLevel, String description) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.description = description;
    }

    public static Tune byId(String id) {
        for (Tune tune : values()) {
            if (tune.id.equals(id)) {
                return tune;
            }
        }
        return null;
    }

    public static List<Tune> all() {
        return List.of(values());
    }

    /** Registry-style names used by the villager shop and the item registry. */
    public String itemName() {
        return "tune_" + id;
    }
}
