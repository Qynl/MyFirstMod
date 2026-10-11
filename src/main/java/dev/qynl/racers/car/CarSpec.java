package dev.qynl.racers.car;

/**
 * Purely visual/physical description of a car body. One renderer/model pair is
 * reused for every car and reshaped with these numbers, which keeps the client
 * model count small while still giving each vehicle a distinct silhouette.
 */
public record CarSpec(float length, float width, float height, float wheelRadius,
                      float wheelBase, float trackWidth, float bodyLift,
                      BodyStyle style, boolean openWheels, boolean cockpitWindow) {

    public enum BodyStyle {
        KART,
        HATCHBACK,
        MUSCLE,
        EXOTIC,
        OPEN_WHEEL,
        TRUCK,
        ROD
    }

    public static CarSpec of(String type) {
        return switch (type) {
            case "kart" -> new CarSpec(2.3F, 1.5F, 0.95F, 0.24F, 1.35F, 1.32F, 0.22F,
                    BodyStyle.KART, false, false);
            case "rally" -> new CarSpec(3.3F, 1.85F, 1.35F, 0.33F, 2.25F, 1.66F, 0.30F,
                    BodyStyle.HATCHBACK, false, true);
            case "sport" -> new CarSpec(3.7F, 1.85F, 1.10F, 0.34F, 2.45F, 1.70F, 0.24F,
                    BodyStyle.MUSCLE, false, true);
            case "hyper" -> new CarSpec(3.9F, 1.95F, 1.00F, 0.35F, 2.60F, 1.80F, 0.20F,
                    BodyStyle.EXOTIC, false, true);
            case "f1" -> new CarSpec(4.2F, 1.95F, 0.95F, 0.34F, 3.10F, 1.78F, 0.16F,
                    BodyStyle.OPEN_WHEEL, true, false);
            case "truck" -> new CarSpec(4.4F, 2.15F, 1.85F, 0.40F, 3.20F, 1.98F, 0.36F,
                    BodyStyle.TRUCK, false, true);
            case "hotrod" -> new CarSpec(3.9F, 1.90F, 1.30F, 0.38F, 2.85F, 1.76F, 0.30F,
                    BodyStyle.ROD, false, true);
            case "drift" -> new CarSpec(3.7F, 1.90F, 1.15F, 0.34F, 2.45F, 1.74F, 0.24F,
                    BodyStyle.MUSCLE, false, true);
            case "shadow" -> new CarSpec(4.1F, 2.00F, 1.05F, 0.36F, 2.95F, 1.86F, 0.18F,
                    BodyStyle.EXOTIC, false, true);
            default -> new CarSpec(3.7F, 1.85F, 1.15F, 0.34F, 2.50F, 1.72F, 0.25F,
                    BodyStyle.MUSCLE, false, true);
        };
    }
}
