package dev.qynl.myfirstmod.kingdom;
/** Pure combat boundaries, sized to the chapterhouse interior (17x17, centre at the heart, ring radius 3-7).
 * Vertical checks stop attacks leaking between floors or through the vaulted roof. */
public final class MonasteryRules {
    private MonasteryRules(){}
    public static int health(int players){return 240+100*(Math.max(1,Math.min(4,players))-1);}
    public static int ringBell(int current,int bell){if(bell!=0&&bell!=1)return current&3;return (current&3)|(bell==0?1:2);}
    /** The sanctuary approach is a fixed, player-built space; worldgen must never claim it. */
    public static boolean reserved(int x,int z){return Math.abs((long)x)<160&&z>-160&&z<256;}
    public static boolean lane(double forward,double sideways,double height){return forward>=0&&forward<=9&&Math.abs(sideways)<=1.15&&Math.abs(height)<3;}
    public static boolean seed(double x,double z,double height){return x*x+z*z<=4.84&&Math.abs(height)<3;}
    public static boolean crown(double x,double z,double height,boolean diagonal){
        if(diagonal){double a=(x+z)/Math.sqrt(2);z=(z-x)/Math.sqrt(2);x=a;}
        double distance=x*x+z*z;
        return distance>=9&&distance<=49&&Math.min(Math.abs(x),Math.abs(z))<=.85&&Math.abs(height)<3;
    }
}
