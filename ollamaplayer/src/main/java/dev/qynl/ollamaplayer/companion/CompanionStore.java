package dev.qynl.ollamaplayer.companion;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves Nova's body (bag, armor, place, health, hunger, mode, owner) into the world folder, so
 * she comes back with her stuff after a server restart. One file per world.
 */
public final class CompanionStore {
    private static final String FILE = "ollamaplayer-companion.nbt";

    private CompanionStore() {}

    private static Path file(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(FILE);
    }

    /** Writes the companion's state. Failures are logged and ignored: saving must never break play. */
    public static void save(MinecraftServer server, Companion companion, @Nullable java.util.UUID owner) {
        try {
            NovaBody body = companion.player();
            NbtCompound root = new NbtCompound();
            root.putString("name", body.getGameProfile().getName());
            root.putString("world", body.getServerWorld().getRegistryKey().getValue().toString());
            root.putDouble("x", body.getX());
            root.putDouble("y", body.getY());
            root.putDouble("z", body.getZ());
            root.putString("mode", companion.mode().name());
            root.putFloat("health", body.getHealth());
            root.putInt("food", body.getHungerManager().getFoodLevel());
            root.putInt("selected", body.getInventory().selectedSlot);
            if (owner != null) root.putUuid("owner", owner);
            if (companion.stashPos() != null) root.putLong("stash", companion.stashPos().asLong());

            RegistryOps<NbtElement> ops = server.getRegistryManager().getOps(NbtOps.INSTANCE);
            PlayerInventory inv = body.getInventory();
            NbtList items = new NbtList();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isEmpty()) continue;
                final int slot = i;
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(el -> {
                    NbtCompound entry = new NbtCompound();
                    entry.putInt("slot", slot);
                    entry.put("item", el);
                    items.add(entry);
                });
            }
            root.put("items", items);
            NbtIo.writeCompressed(root, file(server));
        } catch (IOException | RuntimeException e) {
            dev.qynl.ollamaplayer.OllamaPlayerMod.LOGGER.warn("Could not save Nova's state", e);
        }
    }

    /** Reads the saved state, or null when there is none (or it can't be read). */
    @Nullable
    public static NbtCompound read(MinecraftServer server) {
        Path p = file(server);
        if (!Files.exists(p)) return null;
        try {
            return NbtIo.readCompressed(p, NbtSizeTracker.ofUnlimitedBytes());
        } catch (IOException | RuntimeException e) {
            dev.qynl.ollamaplayer.OllamaPlayerMod.LOGGER.warn("Could not read Nova's saved state", e);
            return null;
        }
    }

    /** Restores the saved bag into a fresh body. */
    public static void restoreItems(MinecraftServer server, NbtCompound root, NovaBody body) {
        RegistryOps<NbtElement> ops = server.getRegistryManager().getOps(NbtOps.INSTANCE);
        NbtList items = root.getList("items", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < items.size(); i++) {
            NbtCompound entry = items.getCompound(i);
            int slot = entry.getInt("slot");
            if (slot < 0 || slot >= body.getInventory().size() || !entry.contains("item")) continue;
            ItemStack stack = ItemStack.CODEC.parse(ops, entry.get("item")).result().orElse(ItemStack.EMPTY);
            body.getInventory().setStack(slot, stack);
        }
        body.getInventory().selectedSlot = root.getInt("selected");
    }

    public static void delete(MinecraftServer server) {
        try {
            Files.deleteIfExists(file(server));
        } catch (IOException e) {
            dev.qynl.ollamaplayer.OllamaPlayerMod.LOGGER.warn("Could not delete Nova's saved state", e);
        }
    }
}
