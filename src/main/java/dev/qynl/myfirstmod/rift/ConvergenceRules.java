package dev.qynl.myfirstmod.rift;

/** Pure encounter rules shared by the controller and unit tests. */
public final class ConvergenceRules {
    private ConvergenceRules() {}
    public static int partySize(int players) {return Math.max(1,Math.min(4,players));}
    public static int guardians(int players) {return 2+partySize(players);}
    public static float heraldHealth(int players) {return 180+70*(partySize(players)-1);}
    public static boolean ringHits(double horizontalDistance,double verticalDistance) {
        return Math.abs(verticalDistance)<3 && horizontalDistance>3 && horizontalDistance<6;
    }
    public static boolean burstHits(double horizontalSquared,double verticalDistance) {
        return Math.abs(verticalDistance)<3 && horizontalSquared<=6.25;
    }
    public static int cooldown(int ticks,boolean focus) {return focus?Math.max(20,ticks*4/5):ticks;}
    public static int channelProgress(int current,boolean grounding) {
        return grounding?Math.min(60,current+5):Math.max(0,current-2);
    }
}
