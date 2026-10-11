package dev.qynl.racers.registry;

/**
 * Sound registry.
 *
 * <p>FabricRacers deliberately reuses vanilla sound events (minecarts for the
 * engine, anvils for the wrench, fireworks for the podium) so the mod ships
 * without a single binary sound asset. Custom .ogg files can be dropped in
 * later and registered here.
 */
public final class ModSounds {
    private ModSounds() {
    }

    public static void init() {
        // no custom sounds yet - see the class comment
    }
}
