package dev.qynl.myfirstmod.keep;
import dev.qynl.myfirstmod.realm.ExpeditionRecord;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class KeepRulesTest {
    @Test void boundedParty(){assertEquals(360,KeepRules.health(0));assertEquals(720,KeepRules.health(99));}
    @Test void cleaveHasRearAndHeightSafety(){assertTrue(KeepRules.cleave(25,.4,0));assertFalse(KeepRules.cleave(26,1,0));assertFalse(KeepRules.cleave(1,-1,0));assertFalse(KeepRules.cleave(1,1,3));}
    @Test void ringAndCrossBoundaries(){assertFalse(KeepRules.ring(2.5,0));assertFalse(KeepRules.ring(7.5,0));assertTrue(KeepRules.ring(4,0));assertTrue(KeepRules.cross(1,6,0));assertFalse(KeepRules.cross(2,2,0));assertFalse(KeepRules.cross(0,7,0));}
    @Test void mailboxSurvivesRestartAndOldSaves(){var r=ExpeditionRecord.read(new NbtCompound());assertEquals(0,r.keepClears);r.pendingKeepRewards=2;r.keepClears=3;var copy=ExpeditionRecord.read(r.write());assertEquals(2,copy.pendingKeepRewards);assertEquals(3,copy.keepClears);var n=new NbtCompound();n.putInt("PendingKeepRewards",999);assertEquals(64,ExpeditionRecord.read(n).pendingKeepRewards);}
}
