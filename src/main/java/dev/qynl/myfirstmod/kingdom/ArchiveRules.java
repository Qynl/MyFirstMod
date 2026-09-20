package dev.qynl.myfirstmod.kingdom;
/** Pure geometry of the flooded archive hall, relative to the Tide Bell on its dais. */
public final class ArchiveRules {
    private ArchiveRules(){}
    public static int radius(){return 5;}
    public static int floorOffset(){return -5;}
    public static int ceilOffset(){return -2;}
    public static boolean inHall(int dx,int dz){return Math.max(Math.abs(dx),Math.abs(dz))<=radius();}
}
