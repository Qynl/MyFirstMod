package dev.qynl.racers.util;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

/**
 * Bridges the two shapes {@code SoundEvents} constants come in across 1.21
 * builds: plain {@link SoundEvent} and {@link RegistryEntry} wrappers. The
 * overload is resolved at compile time, so both worlds type check.
 */
public final class Sounds {
    private Sounds() {
    }

    public static SoundEvent of(SoundEvent sound) {
        return sound;
    }

    public static SoundEvent of(RegistryEntry<SoundEvent> entry) {
        return entry.value();
    }
}
