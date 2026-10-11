package dev.qynl.racers.car;

import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import dev.qynl.racers.registry.ModComponents;

/**
 * Everything a car item carries: which chassis, its colours, its tuning levels,
 * wear, owner and whether the AI is allowed to drive it.
 *
 * <p>Stored as a plain NBT compound data component so the format stays
 * forward/backward compatible.
 */
public final class CarData {
    public final CarType type;
    public final int primaryColor;
    public final int secondaryColor;
    public final int accentColor;
    public final int[] levels;
    public final float engineHealth;
    public final float tyreHealth;
    public final UUID owner;
    public final String ownerName;
    public final boolean aiAllowed;

    public CarData(CarType type, int primaryColor, int secondaryColor, int accentColor,
                   int[] levels, float engineHealth, float tyreHealth, UUID owner,
                   String ownerName, boolean aiAllowed) {
        this.type = type;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
        this.levels = levels.clone();
        this.engineHealth = engineHealth;
        this.tyreHealth = tyreHealth;
        this.owner = owner;
        this.ownerName = ownerName;
        this.aiAllowed = aiAllowed;
    }

    public static CarData fresh(CarType type, PlayerEntity owner) {
        return new CarData(type, PaintColors.defaultPrimary(type), PaintColors.defaultSecondary(type),
                PaintColors.WHITE, new int[Tune.values().length], 1.0F, 1.0F,
                owner == null ? null : owner.getUuid(),
                owner == null ? "" : owner.getName().getString(), true);
    }

    public CarStats stats() {
        return CarStats.of(type, levels);
    }

    public int level(Tune tune) {
        return levels[tune.ordinal()];
    }

    public CarData withLevel(Tune tune, int level) {
        int[] copy = levels.clone();
        copy[tune.ordinal()] = Math.max(0, Math.min(tune.maxLevel, level));
        return new CarData(type, primaryColor, secondaryColor, accentColor, copy,
                engineHealth, tyreHealth, owner, ownerName, aiAllowed);
    }

    public CarData withColors(int primary, int secondary) {
        return new CarData(type, primary, secondary, accentColor, levels, engineHealth,
                tyreHealth, owner, ownerName, aiAllowed);
    }

    public CarData withAccent(int accent) {
        return new CarData(type, primaryColor, secondaryColor, accent, levels, engineHealth,
                tyreHealth, owner, ownerName, aiAllowed);
    }

    public CarData withHealth(float engine, float tyres) {
        return new CarData(type, primaryColor, secondaryColor, accentColor, levels, engine,
                tyres, owner, ownerName, aiAllowed);
    }

    public CarData withAiAllowed(boolean allowed) {
        return new CarData(type, primaryColor, secondaryColor, accentColor, levels,
                engineHealth, tyreHealth, owner, ownerName, allowed);
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("type", type.id);
        nbt.putInt("primary", primaryColor);
        nbt.putInt("secondary", secondaryColor);
        nbt.putInt("accent", accentColor);
        nbt.putIntArray("levels", levels);
        nbt.putFloat("engineHealth", engineHealth);
        nbt.putFloat("tyreHealth", tyreHealth);
        nbt.putString("owner", owner == null ? "" : owner.toString());
        nbt.putString("ownerName", ownerName == null ? "" : ownerName);
        nbt.putBoolean("ai", aiAllowed);
        return nbt;
    }

    public static CarData fromNbt(NbtCompound nbt) {
        CarType type = CarType.byId(nbt.getString("type"));
        int[] levels = nbt.getIntArray("levels");
        if (levels.length != Tune.values().length) {
            int[] fixed = new int[Tune.values().length];
            System.arraycopy(levels, 0, fixed, 0, Math.min(levels.length, fixed.length));
            levels = fixed;
        }
        UUID owner = null;
        String raw = nbt.getString("owner");
        if (!raw.isEmpty()) {
            try {
                owner = UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                owner = null;
            }
        }
        return new CarData(type,
                nbt.contains("primary") ? nbt.getInt("primary") : PaintColors.defaultPrimary(type),
                nbt.contains("secondary") ? nbt.getInt("secondary") : PaintColors.defaultSecondary(type),
                nbt.contains("accent") ? nbt.getInt("accent") : PaintColors.WHITE,
                levels,
                nbt.contains("engineHealth") ? nbt.getFloat("engineHealth") : 1.0F,
                nbt.contains("tyreHealth") ? nbt.getFloat("tyreHealth") : 1.0F,
                owner, nbt.getString("ownerName"), !nbt.contains("ai") || nbt.getBoolean("ai"));
    }

    /** True if the given player may drive/retune this car. */
    public boolean ownedBy(PlayerEntity player) {
        return owner == null || player == null || owner.equals(player.getUuid());
    }

    public Text displayName() {
        return Text.literal(type.displayName).formatted(type.color);
    }

    public Text description() {
        return Text.translatable("item.fabricracers." + type.itemId() + ".desc")
                .formatted(Formatting.GRAY);
    }

    // -- item stack helpers -------------------------------------------------
    public static CarData of(ItemStack stack) {
        NbtCompound nbt = ModComponents.carData(stack);
        return nbt.isEmpty() ? null : fromNbt(nbt);
    }

    public static ItemStack createStack(CarType type, PlayerEntity owner) {
        ItemStack stack = new ItemStack(dev.qynl.racers.registry.ModItems.carItem(type));
        apply(stack, fresh(type, owner));
        return stack;
    }

    public static void apply(ItemStack stack, CarData data) {
        ModComponents.setCarData(stack, data.toNbt());
    }
}
