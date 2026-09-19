package dev.qynl.myfirstmod.remembrance;
import dev.qynl.myfirstmod.realm.ExpeditionRecord;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RemembranceRulesTest {
    @Test void allSevenContractsHaveIndependentThresholds() {
        for(int i=0;i<7;i++) {assertFalse(RemembranceRules.ready(i,0,0,0,0,0,0,0));assertTrue(RemembranceRules.ready(i,2,3,3,1,2,3,1));}
        assertFalse(RemembranceRules.ready(7,99,99,99,99,99,99,99));
        assertEquals(30,java.util.stream.IntStream.range(0,7).map(RemembranceRules::reward).sum());
    }
    @Test void offeringsAndVowsAreBounded() {
        assertEquals(0,RemembranceRules.vow(-1));assertEquals(3,RemembranceRules.vow(99));
        assertEquals(2,RemembranceRules.price(0));assertEquals(4,RemembranceRules.price(-1));
    }
    @Test void landmarksEvictOldestAndRejectUnknownTypes() {
        var r=new ExpeditionRecord();for(int i=0;i<140;i++) r.rememberLandmark(i,"memory");
        assertEquals(128,r.landmarks.size());assertFalse(r.landmarks.containsKey(0L));assertTrue(r.landmarks.containsKey(139L));
        r.rememberLandmark(999,"invented");assertFalse(r.landmarks.containsKey(999L));
    }
    @Test void legacyAndPersistentRecords() {
        var r=ExpeditionRecord.read(new NbtCompound());assertEquals(0,r.vow);assertEquals(0,r.contractsClaimed);
        r.vow=2;r.vowReadyAt=5000;r.contractsClaimed=65;r.ledgerOffer=3;r.memories.add(-123L);r.rememberLandmark(-123,"memory");
        var copy=ExpeditionRecord.read(r.write());assertEquals(2,copy.vow);assertEquals(5000,copy.vowReadyAt);
        assertEquals(65,copy.contractsClaimed);assertEquals(3,copy.ledgerOffer);assertEquals(r.memories,copy.memories);assertEquals(r.landmarks,copy.landmarks);
    }
    @Test void corruptSavesCannotExpandCollectionsOrMask() {
        var n=new NbtCompound();n.putInt("Vow",99);n.putInt("ContractsClaimed",-1);n.putInt("LedgerOffer",-1);
        n.putLongArray("Memories",java.util.stream.LongStream.range(0,1000).toArray());
        var r=ExpeditionRecord.read(n);assertEquals(3,r.vow);assertEquals(127,r.contractsClaimed);assertEquals(3,r.ledgerOffer);assertEquals(256,r.memories.size());
    }
}
