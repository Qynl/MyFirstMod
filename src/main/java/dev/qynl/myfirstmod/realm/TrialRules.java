package dev.qynl.myfirstmod.realm;

/** Pure balancing rules, deliberately separated from Minecraft for unit tests. */
public final class TrialRules {
    private TrialRules() {}
    public static int tier(int value) { return Math.max(0, Math.min(5,value)); }
    public static int waves(int tier) { return 3 + Math.min(2,tier(tier)); }
    public static int enemies(int wave,int tier,int players) {
        return Math.min(8,1+Math.max(1,wave)+(tier(tier)>0?1:0)+Math.max(0,Math.min(4,players)-1));
    }
    public static float healthMultiplier(int tier) { return 1 + .2f*tier(tier); }
    public static int shards(int tier) { return 2+2*tier(tier); }
    public static int experience(int tier) { return 50+50*tier(tier); }
    public static int nextTier(int best) { return Math.min(5,tier(best)+1); }
    public static int oath(long position,int tier) { return Math.floorMod(Long.hashCode(position)+tier,3); }
}
