package dev.qynl.racers.registry;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Item payloads.
 *
 * <p>Everything the mod stores on an item rides inside the vanilla
 * {@code minecraft:custom_data} component, under namespaced keys. That keeps
 * CarData forward compatible (it is plain NBT), needs no custom component
 * registry, and still syncs to clients automatically.
 */
public final class ModComponents {
    /** Key of the {@link dev.qynl.racers.car.CarData} payload on a car item. */
    public static final String CAR_DATA = "fabricracers:car_data";
    /** Key of the last stopwatch lap time (milliseconds, long). */
    public static final String LAP_TIME = "fabricracers:lap_time";
    /** Key of the track wand's remembered mode (int). */
    public static final String WAND_MODE = "fabricracers:wand_mode";

    private ModComponents() {
    }

    public static void init() {
        // nothing to register: the payload rides in minecraft:custom_data
    }

    /** The whole fabricracers payload of a stack (never null, may be empty). */
    public static NbtCompound payload(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        return component == null ? new NbtCompound() : component.copyNbt();
    }

    public static void write(ItemStack stack, NbtCompound payload) {
        if (payload.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(payload));
        }
    }

    private static NbtCompound section(ItemStack stack, String key) {
        NbtCompound payload = payload(stack);
        return payload.contains(key) ? payload.getCompound(key) : new NbtCompound();
    }

    private static void put(ItemStack stack, String key, NbtCompound value) {
        NbtCompound payload = payload(stack);
        if (value.isEmpty()) {
            payload.remove(key);
        } else {
            payload.put(key, value);
        }
        write(stack, payload);
    }

    // -- car data ----------------------------------------------------------
    public static NbtCompound carData(ItemStack stack) {
        return section(stack, CAR_DATA);
    }

    public static void setCarData(ItemStack stack, NbtCompound nbt) {
        put(stack, CAR_DATA, nbt);
    }

    // -- stopwatch ---------------------------------------------------------
    public static long lapTime(ItemStack stack) {
        NbtCompound payload = payload(stack);
        return payload.contains(LAP_TIME) ? payload.getLong(LAP_TIME) : 0L;
    }

    public static void setLapTime(ItemStack stack, long millis) {
        NbtCompound payload = payload(stack);
        if (millis <= 0L) {
            payload.remove(LAP_TIME);
        } else {
            payload.putLong(LAP_TIME, millis);
        }
        write(stack, payload);
    }

    // -- track wand --------------------------------------------------------
    public static int wandMode(ItemStack stack) {
        NbtCompound payload = payload(stack);
        return payload.contains(WAND_MODE) ? payload.getInt(WAND_MODE) : 0;
    }

    public static void setWandMode(ItemStack stack, int mode) {
        NbtCompound payload = payload(stack);
        if (mode == 0) {
            payload.remove(WAND_MODE);
        } else {
            payload.putInt(WAND_MODE, mode);
        }
        write(stack, payload);
    }
}
