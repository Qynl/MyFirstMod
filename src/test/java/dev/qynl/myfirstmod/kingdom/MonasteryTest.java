package dev.qynl.myfirstmod.kingdom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MonasteryTest {
    @Test void partyScalingIsBounded(){
        assertEquals(240,MonasteryRules.health(1));assertEquals(340,MonasteryRules.health(2));
        assertEquals(540,MonasteryRules.health(4));assertEquals(240,MonasteryRules.health(0));
        assertEquals(540,MonasteryRules.health(99));
    }
    @Test void bellsCombineAndNeverRegress(){
        assertEquals(0,MonasteryRules.ringBell(0,-1));
        assertEquals(1,MonasteryRules.ringBell(0,0));assertEquals(2,MonasteryRules.ringBell(0,1));
        assertEquals(3,MonasteryRules.ringBell(1,1));assertEquals(3,MonasteryRules.ringBell(2,0));
        assertEquals(3,MonasteryRules.ringBell(3,0));assertEquals(3,MonasteryRules.ringBell(3,1));
    }
    @Test void lashLeavesASafeLaneEdge(){
        assertTrue(MonasteryRules.lane(9,1.15,2.9));assertTrue(MonasteryRules.lane(0,0,0));
        assertFalse(MonasteryRules.lane(9.01,0,0));assertFalse(MonasteryRules.lane(-.5,0,0));
        assertFalse(MonasteryRules.lane(6,1.2,0));assertFalse(MonasteryRules.lane(6,0,3));
    }
    @Test void seedfallHasAClearOutside(){
        assertTrue(MonasteryRules.seed(0,0,0));assertTrue(MonasteryRules.seed(2.1,0,2.9));
        assertFalse(MonasteryRules.seed(2.3,0,0));assertFalse(MonasteryRules.seed(0,0,3));
        assertFalse(MonasteryRules.seed(-4,4,0));
    }
    @Test void crownHasGapsACenterAndASecondRotation(){
        assertTrue(MonasteryRules.crown(3,0,0,false));assertTrue(MonasteryRules.crown(7,0,2.9,false));
        assertFalse(MonasteryRules.crown(2.9,0,0,false));assertFalse(MonasteryRules.crown(7.1,0,0,false));
        assertFalse(MonasteryRules.crown(4,4,0,false));assertFalse(MonasteryRules.crown(3,0,3,false));
        assertTrue(MonasteryRules.crown(3,3,0,true));assertFalse(MonasteryRules.crown(3,3,0,false));
        assertTrue(MonasteryRules.crown(4,4,0,true));assertFalse(MonasteryRules.crown(6,6,0,true));
        assertFalse(MonasteryRules.crown(0,0,0,true));
    }
    @Test void bothBellsResolveToTheSameHeartInEveryRotation(){
        for(var facing:Direction.Type.HORIZONTAL) {
            var heart=new BlockPos(400,81,-240);
            BlockPos west=Monastery.relative(heart,facing,-16,0,-16);
            BlockPos east=Monastery.relative(heart,facing,16,6,-16);
            assertEquals(heart,Monastery.relative(west,facing,16,0,16),facing.getName());
            assertEquals(heart,Monastery.relative(east,facing,-16,-6,16),facing.getName());
            assertNotEquals(west,east);
        }
    }
    @Test void theSanctuaryApproachStaysClearOfNewMonasteries(){
        assertTrue(MonasteryRules.reserved(0,160));
        assertTrue(MonasteryRules.reserved(-159,255));
        assertFalse(MonasteryRules.reserved(160,160));
        assertFalse(MonasteryRules.reserved(0,256));
        assertFalse(MonasteryRules.reserved(0,-160));
        assertFalse(MonasteryRules.reserved(3000,3000));
    }
}
