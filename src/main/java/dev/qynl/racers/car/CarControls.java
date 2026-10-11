package dev.qynl.racers.car;

/** One tick of driver input, shared by human and AI drivers. */
public final class CarControls {
    public static final byte BIT_FORWARD = 1;
    public static final byte BIT_BACK = 2;
    public static final byte BIT_LEFT = 4;
    public static final byte BIT_RIGHT = 8;
    public static final byte BIT_HANDBRAKE = 16;
    public static final byte BIT_BOOST = 32;

    public boolean forward;
    public boolean back;
    public boolean left;
    public boolean right;
    public boolean handbrake;
    public boolean boost;
    /** 0..1 analog steering used by the AI (overrides left/right when non-zero). */
    public double analogSteer;
    public double analogThrottle = -1.0;

    public static CarControls none() {
        return new CarControls();
    }

    public static CarControls fromMask(byte mask) {
        CarControls controls = new CarControls();
        controls.applyMask(mask);
        return controls;
    }

    public void applyMask(byte mask) {
        forward = (mask & BIT_FORWARD) != 0;
        back = (mask & BIT_BACK) != 0;
        left = (mask & BIT_LEFT) != 0;
        right = (mask & BIT_RIGHT) != 0;
        handbrake = (mask & BIT_HANDBRAKE) != 0;
        boost = (mask & BIT_BOOST) != 0;
    }

    public byte mask() {
        byte mask = 0;
        if (forward) {
            mask |= BIT_FORWARD;
        }
        if (back) {
            mask |= BIT_BACK;
        }
        if (left) {
            mask |= BIT_LEFT;
        }
        if (right) {
            mask |= BIT_RIGHT;
        }
        if (handbrake) {
            mask |= BIT_HANDBRAKE;
        }
        if (boost) {
            mask |= BIT_BOOST;
        }
        return mask;
    }

    /** Steering input in the -1 (left) .. 1 (right) range. */
    public double steer() {
        if (analogSteer != 0.0) {
            return Math.max(-1.0, Math.min(1.0, analogSteer));
        }
        double value = 0.0;
        if (left) {
            value -= 1.0;
        }
        if (right) {
            value += 1.0;
        }
        return value;
    }

    public double throttle() {
        if (analogThrottle >= 0.0) {
            return Math.max(0.0, Math.min(1.0, analogThrottle));
        }
        return forward ? 1.0 : 0.0;
    }

    public double brake() {
        return back ? 1.0 : 0.0;
    }

    public boolean isEmpty() {
        return !forward && !back && !left && !right && !handbrake && !boost
                && analogSteer == 0.0 && analogThrottle < 0.0;
    }

    @Override
    public String toString() {
        return "CarControls[f=" + forward + " b=" + back + " l=" + left + " r=" + right
                + " hb=" + handbrake + " boost=" + boost + " steer=" + steer() + "]";
    }
}
