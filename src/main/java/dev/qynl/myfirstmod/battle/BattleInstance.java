package dev.qynl.myfirstmod.battle;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an isolated, instance-based battlefield clash between two faction armies.
 */
public class BattleInstance {
    public final UUID id;
    public final ServerWorld world;
    public final String factionA;
    public final String factionB;
    public final String unitA;
    public final String unitB;
    public final int initialCountA;
    public final int initialCountB;
    public final Vec3d center;
    public final double distance;
    public final String formation;
    public final long startTick;

    public final Set<UUID> troopsA = ConcurrentHashMap.newKeySet();
    public final Set<UUID> troopsB = ConcurrentHashMap.newKeySet();

    public int tickTimer = 0;
    public boolean active = true;

    public BattleInstance(UUID id, ServerWorld world, String factionA, String unitA, int countA,
                          String factionB, String unitB, int countB,
                          Vec3d center, double distance, String formation, long startTick) {
        this.id = id;
        this.world = world;
        this.factionA = factionA;
        this.unitA = unitA;
        this.initialCountA = countA;
        this.factionB = factionB;
        this.unitB = unitB;
        this.initialCountB = countB;
        this.center = center;
        this.distance = distance;
        this.formation = formation;
        this.startTick = startTick;
    }

    public void addTroopA(LivingEntity entity) {
        if (entity != null) {
            troopsA.add(entity.getUuid());
        }
    }

    public void addTroopB(LivingEntity entity) {
        if (entity != null) {
            troopsB.add(entity.getUuid());
        }
    }

    public int countAliveA() {
        troopsA.removeIf(uuid -> {
            Entity e = world.getEntity(uuid);
            return e == null || !e.isAlive();
        });
        return troopsA.size();
    }

    public int countAliveB() {
        troopsB.removeIf(uuid -> {
            Entity e = world.getEntity(uuid);
            return e == null || !e.isAlive();
        });
        return troopsB.size();
    }

    public void tick() {
        if (!active || world == null) return;

        tickTimer++;
        if (tickTimer < 20) return; // Warmup period

        int aliveA = countAliveA();
        int aliveB = countAliveB();

        // Broadcast action bar status every 10 ticks
        if (tickTimer % 10 == 0) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                UnitWorldData data = UnitWorldData.get(server);
                Faction fA = data.factions.get(factionA);
                Faction fB = data.factions.get(factionB);
                String nameA = fA != null ? fA.name : factionA;
                String nameB = fB != null ? fB.name : factionB;
                int colA = fA != null ? fA.getParsedColor() : 0x3b82f6;
                int colB = fB != null ? fB.getParsedColor() : 0xef4444;

                Text hudText = Text.literal("⚔ ")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                        .append(Text.literal(nameA + ": " + aliveA + " Alive").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colA)).withBold(true)))
                        .append(Text.literal(" ⚡ VS ⚡ ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                        .append(Text.literal(nameB + ": " + aliveB + " Alive").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colB)).withBold(true)))
                        .append(Text.literal(" ⚔").setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    if (p.getServerWorld() == world && p.squaredDistanceTo(center) < 16384.0) { // Within 128m
                        p.sendMessage(hudText, true);
                    }
                }
            }
        }

        // Check for victory
        if (aliveA == 0 && aliveB > 0) {
            announceVictory(factionB);
            active = false;
        } else if (aliveB == 0 && aliveA > 0) {
            announceVictory(factionA);
            active = false;
        } else if (aliveA == 0 && aliveB == 0) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                server.getPlayerManager().broadcast(Text.literal("⚔ BATTLE ENDED IN MUTUAL DESTRUCTION! No survivors remain! ⚔").formatted(Formatting.RED, Formatting.BOLD), false);
            }
            active = false;
        }
    }

    private void announceVictory(String winningFactionId) {
        MinecraftServer server = world.getServer();
        if (server == null || winningFactionId == null) return;

        UnitWorldData data = UnitWorldData.get(server);
        Faction winner = data.factions.get(winningFactionId);
        if (winner == null) return;

        BattleStats.get(server).recordWin(winningFactionId);

        server.getPlayerManager().broadcast(Text.literal("🏆 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 🏆").formatted(Formatting.GOLD), false);
        server.getPlayerManager().broadcast(
                Text.literal("             BATTLE VICTOR: ").formatted(Formatting.YELLOW, Formatting.BOLD)
                        .append(Text.literal(winner.name.toUpperCase()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(winner.getParsedColor())).withBold(true))),
                false
        );
        server.getPlayerManager().broadcast(Text.literal("      Total Kills Recorded: " + BattleStats.get(server).getKills(winningFactionId)).formatted(Formatting.AQUA), false);
        server.getPlayerManager().broadcast(Text.literal("🏆 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 🏆").formatted(Formatting.GOLD), false);

        // Celebration orbital beams and fireworks
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getServerWorld() == world) {
                dev.qynl.myfirstmod.visual.CombatVisualEffects.spawnOrbitalLightPillar(world, p.getX(), p.getY(), p.getZ(), 10.0, ParticleTypes.TOTEM_OF_UNDYING, ParticleTypes.FIREWORK);
                world.spawnParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 8.0, p.getZ(), 60, 3.0, 2.5, 3.0, 0.25);
                world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 4.0, p.getZ(), 40, 2.0, 1.5, 2.0, 0.15);
                world.spawnParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 6.0, p.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.5f, 1.0f);
                world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 2.0f, 1.0f);
            }
        }
    }

    public int clear() {
        int removed = 0;
        for (UUID id : troopsA) {
            Entity e = world.getEntity(id);
            if (e != null && e.isAlive()) {
                world.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                e.discard();
                removed++;
            }
        }
        for (UUID id : troopsB) {
            Entity e = world.getEntity(id);
            if (e != null && e.isAlive()) {
                world.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
                e.discard();
                removed++;
            }
        }
        active = false;
        troopsA.clear();
        troopsB.clear();
        return removed;
    }
}
