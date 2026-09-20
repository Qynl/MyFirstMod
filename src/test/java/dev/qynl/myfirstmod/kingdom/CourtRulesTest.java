package dev.qynl.myfirstmod.kingdom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure composition of the Silent Court sessions, without touching registries. */
final class CourtRulesTest {
    @Test
    void threeSessionsEscalate() {
        assertEquals(3,CourtRules.waves());
        String[][] sessions=CourtRules.sessions();
        assertEquals(3,sessions.length);
        assertEquals(2,sessions[0].length);
        assertEquals(3,sessions[1].length);
        assertEquals(4,sessions[2].length);
    }
    @Test
    void everyActorIsAKnownKingdomActor() {
        for(String[] session:CourtRules.sessions())for(String actor:session)
            assertTrue(actor.startsWith("myfirstmod:rift_")||actor.startsWith("myfirstmod:shard"),actor);
    }
    @Test
    void spawnOffsetsStayInsideThePlaza() {
        for(int[] off:CourtRules.offsets()){
            assertTrue(Math.abs(off[0])<=14);
            assertTrue(off[2]>=30&&off[2]<=44);
        }
        assertEquals(3,CourtRules.chestOffset().length);
        assertEquals(37,CourtRules.chestOffset()[2]);
    }
}
