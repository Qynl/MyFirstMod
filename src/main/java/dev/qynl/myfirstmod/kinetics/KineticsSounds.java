package dev.qynl.myfirstmod.kinetics;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

/**
 * Adapts {@code SoundEvents} constants to plain {@link SoundEvent}s.
 *
 * <p>Since 1.20.5 some {@code SoundEvents} constants are
 * {@code RegistryEntry<SoundEvent>} (note blocks, trident throws, …) while most
 * are plain {@code SoundEvent}s. These overloads accept either shape so call
 * sites never have to care which is which.
 */
public final class KineticsSounds {
    private KineticsSounds() {}

    public static SoundEvent event(SoundEvent sound) {
        return sound;
    }

    public static SoundEvent event(RegistryEntry<SoundEvent> sound) {
        return sound.value();
    }
}
