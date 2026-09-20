package dev.qynl.myfirstmod.kingdom;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure geometry of the Ashen Foundry hall, without touching registries. */
final class FoundryRulesTest {
    @Test
    void fireShellWrapsTheDais() {
        assertEquals(2,FoundryRules.shellRadius());
        assertEquals(-3,FoundryRules.shellFloorOffset());
        assertEquals(-1,FoundryRules.shellCeilOffset());
        assertTrue(FoundryRules.shellFloorOffset()<FoundryRules.shellCeilOffset());
    }
    @Test
    void shellIsTheRingNotTheDisc() {
        assertTrue(FoundryRules.inShell(2,0));
        assertTrue(FoundryRules.inShell(-2,2));
        assertFalse(FoundryRules.inShell(1,1));
        assertFalse(FoundryRules.inShell(3,0));
        assertFalse(FoundryRules.inShell(0,0));
    }
    @Test
    void channelsRunBesideTheWalk() {
        assertEquals(4,FoundryRules.channelRow());
        assertEquals(7,FoundryRules.channelHalfLength());
        assertTrue(FoundryRules.channelRow()>FoundryRules.shellRadius());
    }
}
