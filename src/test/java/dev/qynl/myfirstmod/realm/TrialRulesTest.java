package dev.qynl.myfirstmod.realm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrialRulesTest {
    @Test void tiersAreBounded() {
        assertEquals(0,TrialRules.tier(-10));assertEquals(5,TrialRules.tier(200));
        assertEquals(5,TrialRules.nextTier(Integer.MAX_VALUE));
        assertEquals(1,TrialRules.nextTier(0));assertEquals(5,TrialRules.nextTier(5));
    }
    @Test void waveCountsHaveASensibleCap() {
        assertEquals(3,TrialRules.waves(0));assertEquals(4,TrialRules.waves(1));
        for(int i=2;i<=100;i++) assertEquals(5,TrialRules.waves(i));
    }
    @Test void multiplayerNeverSpawnsAnUnboundedHorde() {
        for(int tier=0;tier<=5;tier++) for(int wave=1;wave<=5;wave++) for(int players=1;players<=100;players++)
            assertTrue(TrialRules.enemies(wave,tier,players)<=8);
        assertEquals(2,TrialRules.enemies(1,0,1));
    }
    @Test void rewardsAndHealthIncreaseTogether() {
        for(int i=1;i<=5;i++) {
            assertTrue(TrialRules.shards(i)>TrialRules.shards(i-1));
            assertTrue(TrialRules.experience(i)>TrialRules.experience(i-1));
            assertTrue(TrialRules.healthMultiplier(i)>TrialRules.healthMultiplier(i-1));
        }
        assertEquals(12,TrialRules.shards(5));assertEquals(2f,TrialRules.healthMultiplier(5));
    }
    @Test void oathsAreDeterministicAndValidForNegativeCoordinates() {
        for(long position:new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE}) for(int tier=1;tier<=5;tier++) {
            int result=TrialRules.oath(position,tier);
            assertTrue(result>=0 && result<3);
            assertEquals(result,TrialRules.oath(position,tier));
        }
    }
}
