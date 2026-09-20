package dev.qynl.myfirstmod.kingdom;
/** Pure geometry of the Ashen Foundry hall, relative to the Ember Crucible on its dais. */
public final class FoundryRules {
    private FoundryRules(){}
    public static int shellRadius(){return 2;}
    public static int shellFloorOffset(){return -3;}
    public static int shellCeilOffset(){return -1;}
    public static int channelRow(){return 4;}
    public static int channelHalfLength(){return 7;}
    public static boolean inShell(int dx,int dz){return Math.max(Math.abs(dx),Math.abs(dz))==shellRadius();}
}
