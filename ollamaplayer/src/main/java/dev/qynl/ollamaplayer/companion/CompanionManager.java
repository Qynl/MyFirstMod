package dev.qynl.ollamaplayer.companion;

import dev.qynl.ollamaplayer.brain.Brain;
import dev.qynl.ollamaplayer.config.ModConfig;
import net.fabricmc.fabric.api.entity.FakePlayer;
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
    private final Brain brain = new Brain(this);

    private CompanionManager() {}

    public void onServerStarted(MinecraftServer s) {
        server = s;
    }

    public void onServerStopping() {
        removeCompanion();
        server = null;
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
        ServerWorld world = owner.getServerWorld();
        Vec3d pos = owner.getPos().add(2, 0, 0);
        companion = new Companion(Companion.createPlayer(world, pos, name()), this::speak);
        speak("Hey! I'm here. Talk to me by saying my name in chat.");
    }

    public void despawn() {
        if (companion == null) return;
        speak("Heading out. Later!");
        removeCompanion();
    }

    public void onPlayerChat(ServerPlayerEntity sender, String text) {
        brain.onChat(sender, text);
    }

    /** Called on the server thread every tick. */
    public void tick() {
        if (server == null) return;
        ServerPlayerEntity owner = owner();

        if (companion == null) {
            if (respawnTimer > 0 && --respawnTimer == 0 && owner != null) {
                spawn(owner);
                speak("Back. That was rough.");
            }
        } else {
            FakePlayer body = companion.player();
            if (body.getHealth() <= 0.0F || body.isRemoved()) {
                handleDeath(body);
            } else {
                if (owner != null && owner.getServerWorld() != body.getServerWorld()) {
                    relocate(owner);
                }
                if (companion != null) companion.tick(owner);
            }
        }
        brain.tick(companion, owner);
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
        return companion.statusLine() + " Model: " + brain.llm().status();
    }

    private void handleDeath(FakePlayer body) {
        BlockPos where = body.getBlockPos();
        speak("I died at " + where.toShortString() + "! My stuff should be on the ground there.");
        removeCompanion();
        respawnTimer = 100; // back at the owner after 5 seconds, empty-handed
    }

    private void relocate(ServerPlayerEntity owner) {
        FakePlayer old = companion.player();
        Companion.Mode mode = companion.mode();
        ServerWorld world = owner.getServerWorld();
        FakePlayer fresh = Companion.createPlayer(world, owner.getPos().add(2, 0, 0), name());
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
