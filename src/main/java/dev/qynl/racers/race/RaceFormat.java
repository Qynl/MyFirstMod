package dev.qynl.racers.race;

import net.minecraft.util.math.MathHelper;

/** The rules of one race. */
public final class RaceFormat {
    public int laps = 3;
    public int aiOpponents;
    /** 0.35 = relaxed, 0.72 = club racer, 1.0 = alien. */
    public double aiSkill = 0.72;
    public boolean damage = true;
    public boolean boostPads = true;
    public boolean rubberBand = true;
    /** Grid slots taken by the AI rather than by players. */
    public boolean reverseGrid;
    public int countdownTicks = 100;
    public int prizeEmeralds = 6;
    public String name = "Grand Prix";

    public static RaceFormat of(int laps, int aiOpponents, double aiSkill, boolean damage) {
        RaceFormat format = new RaceFormat();
        format.laps = MathHelper.clamp(laps, 1, 20);
        format.aiOpponents = MathHelper.clamp(aiOpponents, 0, 19);
        format.aiSkill = MathHelper.clamp(aiSkill, 0.2, 1.0);
        format.damage = damage;
        return format;
    }

    public String skillName() {
        if (aiSkill >= 0.95) {
            return "Alien";
        }
        if (aiSkill >= 0.8) {
            return "Pro";
        }
        if (aiSkill >= 0.62) {
            return "Club";
        }
        if (aiSkill >= 0.45) {
            return "Rookie";
        }
        return "Sunday driver";
    }

    public String describe() {
        return name + ": " + laps + " laps, " + aiOpponents + " AI (" + skillName()
                + "), damage " + (damage ? "on" : "off") + (rubberBand ? ", rubber band on" : "");
    }
}
