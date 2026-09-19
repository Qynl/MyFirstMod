package dev.qynl.myfirstmod.keep;
public final class KeepRules {
    private KeepRules() {}
    public static int party(int players) {return Math.max(1,Math.min(4,players));}
    public static float health(int players) {return 360+120*(party(players)-1);}
    public static boolean cleave(double squared,double dot,double dy) {return squared<=25 && dot>=.4 && Math.abs(dy)<3;}
    public static boolean ring(double radius,double dy) {return radius>2.5 && radius<7.5 && Math.abs(dy)<3;}
    public static boolean cross(double dx,double dz,double dy) {return Math.abs(dy)<3 && Math.abs(dx)<=6 && Math.abs(dz)<=6 && (Math.abs(dx)<1.25 || Math.abs(dz)<1.25);}
}
