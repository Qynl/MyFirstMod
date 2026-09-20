package dev.qynl.myfirstmod.realm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LandmarkDiscoveryTest {
    @Test
    public void mapsCacheTablesToLandmarks() {
        Assertions.assertEquals("veil_watchtower", LandmarkDiscovery.landmark("watch_cache"));
        Assertions.assertEquals("brine_chapel", LandmarkDiscovery.landmark("chapel_cache"));
        Assertions.assertEquals("fen_shrine", LandmarkDiscovery.landmark("shrine_cache"));
    }
    @Test
    public void acceptsFullyQualifiedLootTables() {
        Assertions.assertEquals("echo_fissure", LandmarkDiscovery.landmark("myfirstmod:chests/fissure_cache"));
        Assertions.assertEquals("caravan_wreck", LandmarkDiscovery.landmark("myfirstmod:chests/caravan_cache"));
    }
    @Test
    public void ignoresForeignTables() {
        Assertions.assertNull(LandmarkDiscovery.landmark("myfirstmod:chests/waystone_cache"));
        Assertions.assertNull(LandmarkDiscovery.landmark(null));
        Assertions.assertNull(LandmarkDiscovery.landmark(""));
    }
    @Test
    public void coversEveryLandmarkFamily() {
        Assertions.assertEquals(8, LandmarkDiscovery.LANDMARKS.size());
        Assertions.assertEquals(8, LandmarkDiscovery.CACHE_TO_LANDMARK.size());
    }
}
