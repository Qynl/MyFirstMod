package dev.qynl.myfirstmod.kingdom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure geometry of the flooded archive hall, without touching registries. */
final class ArchiveRulesTest {
    @Test
    void drainZoneSitsUnderTheBell() {
        assertEquals(5,ArchiveRules.radius());
        assertEquals(-5,ArchiveRules.floorOffset());
        assertEquals(-2,ArchiveRules.ceilOffset());
        assertTrue(ArchiveRules.floorOffset()<ArchiveRules.ceilOffset());
        assertTrue(ArchiveRules.ceilOffset()<0);
    }
    @Test
    void hallIsTheChebyshevSquareAroundTheDais() {
        assertTrue(ArchiveRules.inHall(0,0));
        assertTrue(ArchiveRules.inHall(5,5));
        assertTrue(ArchiveRules.inHall(-5,4));
        assertFalse(ArchiveRules.inHall(6,0));
        assertFalse(ArchiveRules.inHall(5,-6));
    }
    @Test
    void drainZoneStaysInsideTheHall() {
        for(int dx=-ArchiveRules.radius();dx<=ArchiveRules.radius();dx++)
            for(int dz=-ArchiveRules.radius();dz<=ArchiveRules.radius();dz++)
                assertTrue(ArchiveRules.inHall(dx,dz));
    }
}
