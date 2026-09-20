package dev.qynl.myfirstmod.kingdom;
/** Pure composition of the Silent Court's three sessions, relative to the Crown Gate. */
public final class CourtRules {
    private CourtRules(){}
    public static int waves(){return 3;}
    public static String[][] sessions(){return new String[][]{
        {"myfirstmod:rift_sentinel","myfirstmod:rift_sentinel"},
        {"myfirstmod:shardstalker","myfirstmod:shardstalker","myfirstmod:rift_herald"},
        {"myfirstmod:rift_sentinel","myfirstmod:rift_herald","myfirstmod:shardstalker","myfirstmod:rift_sentinel"}};}
    public static int[][] offsets(){return new int[][]{{-14,1,30},{14,1,30},{-14,1,44},{14,1,44},{-6,1,37},{6,1,37}};}
    public static int[] chestOffset(){return new int[]{0,2,37};}
    public static int offsetFor(int session,int slot){return (session*2+slot)%offsets().length;}
}
