package dev.qynl.myfirstmod.rift;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConvergenceRulesTest {
    @Test void boundedPartyScaling() {
        assertEquals(180,ConvergenceRules.heraldHealth(0));
        assertEquals(390,ConvergenceRules.heraldHealth(99));
        assertEquals(6,ConvergenceRules.guardians(99));
    }
    @Test void safeRingBoundaries() {
        assertFalse(ConvergenceRules.ringHits(3,0));assertFalse(ConvergenceRules.ringHits(6,0));
        assertTrue(ConvergenceRules.ringHits(4,2));assertFalse(ConvergenceRules.ringHits(4,3));
        assertTrue(ConvergenceRules.burstHits(6.25,0));assertFalse(ConvergenceRules.burstHits(7,0));
    }
    @Test void focusAndChannelBounds() {
        assertEquals(80,ConvergenceRules.cooldown(100,true));assertEquals(20,ConvergenceRules.cooldown(10,true));
        assertEquals(100,ConvergenceRules.cooldown(100,false));
        assertEquals(60,ConvergenceRules.channelProgress(59,true));assertEquals(0,ConvergenceRules.channelProgress(1,false));
        assertEquals(28,ConvergenceRules.channelProgress(30,false));
    }
}
