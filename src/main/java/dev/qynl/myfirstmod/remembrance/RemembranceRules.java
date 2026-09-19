package dev.qynl.myfirstmod.remembrance;

public final class RemembranceRules {
    public static final int CONTRACTS=7, CONTRACT_MASK=(1<<CONTRACTS)-1;
    private RemembranceRules() {}
    public static int vow(int value) {return Math.max(0,Math.min(3,value));}
    public static int reward(int contract) {return new int[]{2,3,4,4,5,6,6}[Math.floorMod(contract,CONTRACTS)];}
    public static boolean ready(int contract,int biomes,int memories,int trials,int cathedrals,int rifts,int tier,int victories) {
        return switch(contract) {case 0->biomes>=2;case 1->memories>=3;case 2->trials>=3;case 3->cathedrals>=1;
            case 4->rifts>=2;case 5->tier>=3;case 6->victories>=1;default->false;};
    }
    public static int price(int offer) {return new int[]{2,2,3,4}[Math.floorMod(offer,4)];}
}
