package dev.qynl.myfirstmod.realm;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpeditionRecordTest {
    @Test void pilgrimReservoirPersistsAndOldSavesGetThreeCharges() {
        var old=ExpeditionRecord.read(new NbtCompound());assertEquals(3,old.flaskCharges);assertEquals(0,old.flaskUpgrades);assertFalse(old.pilgrimKit);
        old.flaskCharges=2;old.flaskUpgrades=2;old.pilgrimKit=true;old.cathedralsOpened=4;
        var restored=ExpeditionRecord.read(old.write());assertEquals(2,restored.flaskCharges);assertEquals(2,restored.flaskUpgrades);
        assertTrue(restored.pilgrimKit);assertEquals(4,restored.cathedralsOpened);
        var invalid=new NbtCompound();invalid.putInt("FlaskCharges",999);invalid.putInt("FlaskUpgrades",999);
        assertEquals(5,ExpeditionRecord.read(invalid).flaskCharges);
    }

    @Test void convergenceMailAndTimersSurviveRestart() {
        ExpeditionRecord record=new ExpeditionRecord();
        record.riftsClosed=7;record.pendingRiftCores=3;record.vigorReadyAt=81234;record.galeReadyAt=81555;
        ExpeditionRecord restored=ExpeditionRecord.read(record.write());
        assertEquals(7,restored.riftsClosed);assertEquals(3,restored.pendingRiftCores);
        assertEquals(81234,restored.vigorReadyAt);assertEquals(81555,restored.galeReadyAt);
        var legacy=ExpeditionRecord.read(new NbtCompound());assertEquals(0,legacy.pendingRiftCores);
        var invalid=new NbtCompound();invalid.putInt("PendingRiftCores",999);
        assertEquals(64,ExpeditionRecord.read(invalid).pendingRiftCores);
    }

    @Test void boundWaystoneSurvivesRestartAtNegativeCoordinates() {
        ExpeditionRecord record=new ExpeditionRecord();
        record.hasWaystone=true;
        record.boundWaystone=new net.minecraft.util.math.BlockPos(-312,78,680).asLong();
        record.biomes.add("myfirstmod:luminous_fen");
        record.biomes.add("myfirstmod:hushed_grove");
        record.biomes.add("myfirstmod:prism_wastes");
        record.biomes.add("myfirstmod:cinder_steps");
        ExpeditionRecord restored=ExpeditionRecord.read(record.write());
        assertTrue(restored.hasWaystone);assertEquals(record.boundWaystone,restored.boundWaystone);
        assertEquals(4,restored.biomes.size());
    }
    @Test void legacyJournalDefaultsToSanctuaryRecall() {
        ExpeditionRecord restored=ExpeditionRecord.read(new NbtCompound());
        assertFalse(restored.hasWaystone);
    }

    @Test void progressAndRewardMailboxRoundTrip() {
        ExpeditionRecord record=new ExpeditionRecord();
        record.trials=17;record.highestTier=4;record.victories=3;
        record.pendingNormal=1;record.pendingEcho=2;
        record.biomes.add("myfirstmod:hushed_grove");record.discoverCourt(-71234567L);
        ExpeditionRecord restored=ExpeditionRecord.read(record.write());
        assertEquals(17,restored.trials);assertEquals(4,restored.highestTier);
        assertEquals(3,restored.victories);assertEquals(1,restored.pendingNormal);assertEquals(2,restored.pendingEcho);
        assertEquals(record.biomes,restored.biomes);assertEquals(record.courts,restored.courts);
    }
    @Test void malformedCountersCannotEscapeLimits() {
        NbtCompound nbt=new NbtCompound();
        nbt.putInt("Trials",-4);nbt.putInt("Tier",100);nbt.putInt("Victories",-10);
        nbt.putInt("PendingNormal",Integer.MAX_VALUE);nbt.putInt("PendingEcho",-1);
        ExpeditionRecord record=ExpeditionRecord.read(nbt);
        assertEquals(0,record.trials);assertEquals(5,record.highestTier);assertEquals(0,record.victories);
        assertEquals(64,record.pendingNormal);assertEquals(0,record.pendingEcho);
    }
    @Test void discoveryStorageKeepsNewest128Courts() {
        ExpeditionRecord record=new ExpeditionRecord();
        for(long i=0;i<200;i++) record.discoverCourt(i);
        assertEquals(128,record.courts.size());assertFalse(record.courts.contains(0L));
        assertTrue(record.courts.contains(199L));assertTrue(record.courts.contains(72L));
    }
    @Test void discoveringAnExistingCourtDoesNotEvictAnother() {
        ExpeditionRecord record=new ExpeditionRecord();
        for(long i=0;i<128;i++) record.discoverCourt(i);
        record.discoverCourt(7L);
        assertEquals(128,record.courts.size());assertTrue(record.courts.contains(0L));
    }
    @Test void oldSavesDefaultToAnEmptyMailbox() {
        ExpeditionRecord record=ExpeditionRecord.read(new NbtCompound());
        assertEquals(0,record.pendingNormal);assertEquals(0,record.pendingEcho);
        assertTrue(record.courts.isEmpty());assertTrue(record.biomes.isEmpty());
    }
}
