package dev.qynl.myfirstmod.pilgrimage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PilgrimageRulesTest {
    @Test void capacityIsBounded() {
        assertEquals(3,PilgrimageRules.capacity(-1));assertEquals(4,PilgrimageRules.capacity(1));assertEquals(5,PilgrimageRules.capacity(999));
        assertEquals(5,PilgrimageRules.charges(999,2));assertEquals(0,PilgrimageRules.charges(-1,0));
    }
    @Test void riteRequiresDrownedCrownedNameless() {
        int progress=0;for(int seal:new int[]{1,0,2}) progress=PilgrimageRules.advance(progress,seal);
        assertEquals(3,progress);assertEquals(-1,PilgrimageRules.expectedSeal(progress));
    }
    @Test void wrongSealResetsAndRepeatedSealDoesNotAdvance() {
        assertEquals(0,PilgrimageRules.advance(0,0));assertEquals(0,PilgrimageRules.advance(1,1));assertEquals(0,PilgrimageRules.advance(2,0));
    }
    @Test void cleaveHasSafeRearAndHeightBoundaries() {
        assertTrue(PilgrimageRules.inCleave(12.25,.35,0));assertFalse(PilgrimageRules.inCleave(12.26,1,0));
        assertFalse(PilgrimageRules.inCleave(1,-1,0));assertFalse(PilgrimageRules.inCleave(1,1,2.5));
    }
}
