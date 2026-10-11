package dev.qynl.ollamaplayer.companion;

import net.minecraft.nbt.NbtElement;
import dev.qynl.ollamaplayer.OllamaPlayerMod;
import dev.qynl.ollamaplayer.brain.Brain;
import dev.qynl.ollamaplayer.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Owns the single companion: spawning, death and respawn, following the owner across dimensions,
 * and the server-thread tick that drives body and brain.
 */
public final class CompanionManager {
    public static final CompanionManager INSTANCE = new CompanionManager();

    private MinecraftServer server;
    private Companion companion;
    private UUID ownerId;
    private int respawnTimer = -1;
    private BlockPos lastDeath;
    private int errorStreak;
    private ServerWorld lastDeathWorld;
    private final Brain brain = new Brain(this);

    private CompanionManager() {}

    private int saveTimer;

    public void onServerStarted(MinecraftServer s) {
        server = s;
        restoreSaved();
    }

    public void onServerStopping() {
        if (companion != null) CompanionStore.save(server, companion, ownerId);
        removeCompanion();
        server = null;
    }

    /** Brings Nova back after a restart, with the bag and place she had when the server stopped. */
    private void restoreSaved() {
        net.minecraft.nbt.NbtCompound root = CompanionStore.read(server);
        if (root == null || companion != null) return;
        try {
            ServerWorld world = server.getWorld(net.minecraft.registry.RegistryKey.of(
                    net.minecraft.registry.RegistryKeys.WORLD, net.minecraft.util.Identifier.of(root.getString("world"))));
            if (world == null) world = server.getOverworld();
            Vec3d pos = new Vec3d(root.getDouble("x"), root.getDouble("y"), root.getDouble("z"));
            NovaBody body = Companion.createPlayer(world, pos, root.getString("name"));
            CompanionStore.restoreItems(server, root, body);
            body.setHealth(Math.max(1.0F, root.getFloat("health")));
            body.getHungerManager().setFoodLevel(root.getInt("food"));
            companion = new Companion(body, this::speak);
            companion.setMode(parseMode(root.getString("mode")));
            if (root.contains("lessons")) companion.experience().load(root.getList("lessons", NbtElement.COMPOUND_TYPE));
            if (root.contains("stash")) companion.setStashPos(net.minecraft.util.math.BlockPos.fromLong(root.getLong("stash")));
            if (root.containsUuid("owner")) ownerId = root.getUuid("owner");
            speak("I'm back. Who wants to go mining?");
        } catch (RuntimeException e) {
            OllamaPlayerMod.LOGGER.warn("Could not restore Nova; she will need /nova spawn", e);
            companion = null;
        }
    }

    private static Companion.Mode parseMode(String name) {
        try {
            return Companion.Mode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Companion.Mode.FOLLOW;
        }
    }

    @Nullable
    public Companion companion() {
        return companion;
    }

    @Nullable
    public ServerPlayerEntity owner() {
        if (server == null || ownerId == null) return null;
        return server.getPlayerManager().getPlayer(ownerId);
    }

    public Brain brain() {
        return brain;
    }

    public void spawn(ServerPlayerEntity owner) {
        removeCompanion();
        ownerId = owner.getUuid();
        respawnTimer = -1;
        saveTimer = 0;
        ServerWorld world = owner.getServerWorld();
        Vec3d pos = owner.getPos().add(2, 0, 0);
        companion = new Companion(Companion.createPlayer(world, pos, name()), this::speak);
        speak("Hey! I'm here. Talk to me by saying my name in chat.");
    }

    public void despawn() {
        if (companion == null) return;
        speak("Heading out. Later!");
        removeCompanion();
        if (server != null) CompanionStore.delete(server);
    }

    public void onPlayerChat(ServerPlayerEntity sender, String text) {
        brain.onChat(sender, text);
    }

    /** Called when any player joins: if it's the owner coming back, Nova greets them. */
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (companion != null && player.getUuid().equals(ownerId)) {
            speak("Welcome back, " + player.getName().getString() + "!");
        }
    }

    /** Called on the server thread every tick. */
    public void tick() {
        if (server == null) return;
        ServerPlayerEntity owner = owner();

        if (companion == null) {
            if (respawnTimer > 0 && --respawnTimer == 0 && owner != null) {
                spawn(owner);
                speak("Back. That was rough.");
                if (lastDeath != null && lastDeathWorld == owner.getServerWorld() && companion != null) {
                    companion.recoverAt(lastDeath);
                }
                lastDeath = null;
            }
        } else {
            NovaBody body = companion.player();
            if (++saveTimer >= 600) {
                saveTimer = 0;
                CompanionStore.save(server, companion, ownerId);
            }
            if (body.getHealth() <= 0.0F || body.isRemoved()) {
                handleDeath(body);
            } else {
                if (owner != null && owner.getServerWorld() != body.getServerWorld()) {
                    relocate(owner);
                }
                if (companion != null) {
                    try {
                        long t0 = System.nanoTime();
                        companion.tick(owner);
                        brain.metrics().tick(System.nanoTime() - t0);
                        errorStreak = 0;
                    } catch (RuntimeException e) {
                        // Never let a bug in the companion take the whole server down.
                        errorStreak++;
                        OllamaPlayerMod.LOGGER.error("Companion tick failed ({} in a row)", errorStreak, e);
                        if (errorStreak >= 20) {
                            speak("Something in my head broke. I'm going back to my owner.");
                            removeCompanion();
                            respawnTimer = 60;
                            errorStreak = 0;
                        }
                    }
                }
            }
        }
        try {
            brain.tick(companion, owner);
        } catch (RuntimeException e) {
            OllamaPlayerMod.LOGGER.error("Brain tick failed", e);
        }
    }

    /** Used by the brain and the body to talk as Nova. */
    public void speak(String text) {
        if (text == null || text.isBlank()) return;
        brain.remember(name() + ": " + text);
        if (server == null) return;
        server.getPlayerManager().broadcast(
                Text.literal("<" + name() + "> " + text).formatted(Formatting.AQUA), false);
    }

    public String status() {
        if (companion == null) return "Not spawned. Use /nova spawn.";
        String stash = companion.stashPos() == null ? "" : " Stash chest at " + companion.stashPos().toShortString() + ".";
        return companion.statusLine() + stash + " Model: " + brain.llm().status() + " "
                + brain.metrics().summary(System.nanoTime());
    }

    private void handleDeath(NovaBody body) {
        BlockPos where = body.getBlockPos();
        lastDeath = where;
        lastDeathWorld = body.getServerWorld();
        body.getInventory().dropAll();
        speak("I died at " + where.toShortString() + "! My stuff should be on the ground there.");
        removeCompanion();
        CompanionStore.delete(server);
        respawnTimer = 100; // back at the owner after 5 seconds, empty-handed
    }

    private void relocate(ServerPlayerEntity owner) {
        NovaBody old = companion.player();
        Companion.Mode mode = companion.mode();
        ServerWorld world = owner.getServerWorld();
        NovaBody fresh = Companion.createPlayer(world, owner.getPos().add(2, 0, 0), name());
        for (int i = 0; i < old.getInventory().size(); i++) {
            fresh.getInventory().setStack(i, old.getInventory().getStack(i).copy());
        }
        fresh.getInventory().selectedSlot = old.getInventory().selectedSlot;
        old.discard();
        companion = new Companion(fresh, this::speak);
        companion.setMode(mode);
        speak("Following you over.");
    }

    private void removeCompanion() {
        if (companion != null) {
            companion.player().discard();
            companion = null;
        }
    }

    private String name() {
        return ModConfig.get().companionName;
    }
}
