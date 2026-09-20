package dev.qynl.myfirstmod.battle;

import dev.qynl.myfirstmod.ai.UnitSystem;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
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
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.List;

public final class BattleSandbox {
    private BattleSandbox() {}

    public static String activeFactionA = null;
    public static String activeFactionB = null;
    public static boolean battleActive = false;
    private static int battleTickTimer = 0;

    public static void startBattle(ServerPlayerEntity player, String factionAId, String factionBId, int armySize) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        UnitWorldData data = UnitWorldData.get(server);
        Faction factionA = data.factions.get(factionAId);
        Faction factionB = data.factions.get(factionBId);

        if (factionA == null || factionB == null) {
            player.sendMessage(Text.literal("Error: Both factions must exist to start a battle.").formatted(Formatting.RED), false);
            return;
        }

        List<UnitDefinition> unitsA = data.units.values().stream().filter(u -> u.factionId.equalsIgnoreCase(factionAId)).toList();
        List<UnitDefinition> unitsB = data.units.values().stream().filter(u -> u.factionId.equalsIgnoreCase(factionBId)).toList();

        if (unitsA.isEmpty() || unitsB.isEmpty()) {
            player.sendMessage(Text.literal("Error: Both factions must have at least one saved unit.").formatted(Formatting.RED), false);
            return;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos();

        double battleDistance = 32.0;
        Vec3d sideAPos = center.add(-battleDistance / 2.0, 0, 0);
        Vec3d sideBPos = center.add(battleDistance / 2.0, 0, 0);

        // Spawn Side A (facing East -> Yaw 90)
        spawnArmyFormation(world, unitsA, sideAPos, 90.0f, armySize);

        // Spawn Side B (facing West -> Yaw -90)
        spawnArmyFormation(world, unitsB, sideBPos, -90.0f, armySize);

        activeFactionA = factionAId;
        activeFactionB = factionBId;
        battleActive = true;
        battleTickTimer = 0;

        // Sound battle horn
        world.playSound(null, center.x, center.y, center.z, SoundEvents.EVENT_RAID_HORN, SoundCategory.NEUTRAL, 2.0f, 1.0f);

        Text startMsg = Text.literal("⚔ BATTLE COMMENCED: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                .append(Text.literal(factionA.name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionA.getParsedColor())).withBold(true)))
                .append(Text.literal(" VS ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                .append(Text.literal(factionB.name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionB.getParsedColor())).withBold(true)))
                .append(Text.literal(" (" + armySize + " vs " + armySize + ")").formatted(Formatting.GRAY));

        server.getPlayerManager().broadcast(startMsg, false);
    }

    private static void spawnArmyFormation(ServerWorld world, List<UnitDefinition> units, Vec3d basePos, float yaw, int totalCount) {
        int cols = 4;
        for (int i = 0; i < totalCount; i++) {
            UnitDefinition template = units.get(i % units.size());
            int row = i / cols;
            int col = i % cols;

            double rowOffset = row * 2.5;
            if (template.role.equalsIgnoreCase("melee") || template.role.equalsIgnoreCase("tank") || template.role.equalsIgnoreCase("berserker")) {
                rowOffset -= 1.5;
            } else if (template.role.equalsIgnoreCase("medic") || template.role.equalsIgnoreCase("pyrotechnic") || template.role.equalsIgnoreCase("bard") || template.commander) {
                rowOffset += 2.0;
            }

            double offsetX = yaw > 0 ? -rowOffset : rowOffset;
            double offsetZ = (col - (cols / 2.0)) * 2.2;

            Vec3d spawnPos = basePos.add(offsetX, 0, offsetZ);
            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(spawnPos.x), (int) Math.floor(spawnPos.z));
            if (Math.abs(topY - spawnPos.y) < 15.0) {
                spawnPos = new Vec3d(spawnPos.x, topY, spawnPos.z);
            }

            LivingEntity entity = UnitSpawner.spawn(world, template, spawnPos, yaw);
            if (entity != null) {
                entity.getCommandTags().add("battle_mob");
            }
        }
    }

    public static void tickBattleCheck(ServerWorld world) {
        if (!battleActive || world == null || activeFactionA == null || activeFactionB == null) return;

        battleTickTimer++;
        if (battleTickTimer < 20) return; // Wait initial warm-up period
        if (battleTickTimer % 20 != 0) return; // Check once per second

        int countA = countAliveBattleFaction(world, activeFactionA);
        int countB = countAliveBattleFaction(world, activeFactionB);

        if (countA == 0 && countB > 0) {
            announceVictory(world, activeFactionB);
            battleActive = false;
        } else if (countB == 0 && countA > 0) {
            announceVictory(world, activeFactionA);
            battleActive = false;
        } else if (countA == 0 && countB == 0) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                server.getPlayerManager().broadcast(Text.literal("⚔ BATTLE ENDED IN MUTUAL DESTRUCTION! No survivors remain! ⚔").formatted(Formatting.RED, Formatting.BOLD), false);
            }
            battleActive = false;
        }
    }

    public static int countAliveBattleFaction(ServerWorld world, String factionId) {
        if (world == null || factionId == null) return 0;
        int count = 0;
        for (Entity e : world.iterateEntities()) {
            if (e.isAlive() && e.getCommandTags().contains("battle_mob") && factionId.equalsIgnoreCase(UnitSystem.getTagValue(e, "faction:"))) {
                count++;
            }
        }
        return count;
    }

    public static int clearAllBattleMobs(ServerWorld world) {
        if (world == null) return 0;
        int count = 0;
        List<Entity> toRemove = new ArrayList<>();

        for (Entity e : world.iterateEntities()) {
            if (e.getCommandTags().contains("battle_mob") || UnitSystem.getTagValue(e, "unit:") != null) {
                toRemove.add(e);
            }
        }

        for (Entity e : toRemove) {
            world.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
            e.discard();
            count++;
        }

        battleActive = false;
        return count;
    }

    public static int countAliveFaction(ServerWorld world, String factionId) {
        if (world == null || factionId == null) return 0;
        int count = 0;
        for (Entity e : world.iterateEntities()) {
            if (e.isAlive() && factionId.equalsIgnoreCase(UnitSystem.getTagValue(e, "faction:"))) {
                count++;
            }
        }
        return count;
    }

    public static void announceVictory(ServerWorld world, String winningFactionId) {
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

        // Celebration fireworks in the sky
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            world.spawnParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 8.0, p.getZ(), 50, 2.0, 2.0, 2.0, 0.2);
            world.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.2f, 1.0f);
        }
    }
}
