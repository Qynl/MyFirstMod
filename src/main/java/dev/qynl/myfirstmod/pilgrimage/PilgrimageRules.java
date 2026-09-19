package dev.qynl.myfirstmod.pilgrimage;

/** Pure rules for the shared flask reservoir and the cathedral's funerary rite. */
public final class PilgrimageRules {
    private PilgrimageRules() {}
    public static int capacity(int upgrades) {return 3+Math.max(0,Math.min(2,upgrades));}
    public static int charges(int charges,int upgrades) {return Math.max(0,Math.min(capacity(upgrades),charges));}
    // The drowned (crypt), the crowned (nave), then the nameless (ossuary).
    public static int expectedSeal(int progress) {return switch(progress) {case 0->1;case 1->0;case 2->2;default->-1;};}
    public static int advance(int progress,int seal) {return seal==expectedSeal(progress)?progress+1:0;}
    public static boolean inCleave(double distanceSquared,double facingDot,double height) {
        return distanceSquared<=12.25 && facingDot>=.35 && Math.abs(height)<2.5;
    }
}
