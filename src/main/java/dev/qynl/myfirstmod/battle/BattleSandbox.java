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
import java.util.Collections;
import java.util.List;

public final class BattleSandbox {
    private BattleSandbox() {}

    public static String activeFactionA = null;
    public static String activeFactionB = null;
    public static boolean battleActive = false;
    private static int battleTickTimer = 0;
    private static int initialCountA = 0;
    private static int initialCountB = 0;

    public static void startBattle(ServerPlayerEntity player, String factionAId, String factionBId, int armySize) {
        startCustomBattle(player, factionAId, "all", armySize, factionBId, "all", armySize, "line", 32.0);
    }

    public static void startCustomBattle(ServerPlayerEntity player,
                                         String factionAId, String unitAId, int countA,
                                         String factionBId, String unitBId, int countB,
                                         String formation, double distance) {
        if (player == null) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;

        UnitWorldData data = UnitWorldData.get(server);
        Faction factionA = data.factions.get(factionAId);
        Faction factionB = data.factions.get(factionBId);

        if (factionA == null) {
            factionA = new Faction(factionAId, factionAId.toUpperCase());
            data.saveFaction(factionA);
        }
        if (factionB == null) {
            factionB = new Faction(factionBId, factionBId.toUpperCase());
            data.saveFaction(factionB);
        }

        List<UnitDefinition> unitsA = resolveUnits(data, factionAId, unitAId);
        List<UnitDefinition> unitsB = resolveUnits(data, factionBId, unitBId);

        if (unitsA.isEmpty() || unitsB.isEmpty()) {
            player.sendMessage(Text.literal("Error: No valid unit templates found for one or both factions.").formatted(Formatting.RED), false);
            return;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos();
        double battleDist = Math.max(12.0, Math.min(80.0, distance));

        countA = Math.max(1, Math.min(100, countA));
        countB = Math.max(1, Math.min(100, countB));
        initialCountA = countA;
        initialCountB = countB;

        String fStyle = formation != null ? formation.toLowerCase() : "line";

        if ("ambush".equals(fStyle)) {
            // Side A in central circle; Side B encircling around
            spawnCircleFormation(world, unitsA, center, countA, 4.0, true);
            spawnCircleFormation(world, unitsB, center, countB, battleDist / 2.0, false);
        } else if ("flank".equals(fStyle)) {
            // Side A standard line facing East; Side B split into two flanking assault wings
            Vec3d sideAPos = center.add(-battleDist / 2.0, 0, 0);
            spawnArmyFormation(world, unitsA, sideAPos, 90.0f, countA);

            int halfB = Math.max(1, countB / 2);
            Vec3d flankNorth = center.add(battleDist / 4.0, 0, -18.0);
            Vec3d flankSouth = center.add(battleDist / 4.0, 0, 18.0);
            spawnArmyFormation(world, unitsB, flankNorth, -45.0f, halfB);
            spawnArmyFormation(world, unitsB, flankSouth, -135.0f, countB - halfB);
        } else {
            // Classic Line vs Line
            Vec3d sideAPos = center.add(-battleDist / 2.0, 0, 0);
            Vec3d sideBPos = center.add(battleDist / 2.0, 0, 0);

            // Spawn Side A (facing East -> Yaw 90)
            spawnArmyFormation(world, unitsA, sideAPos, 90.0f, countA);

            // Spawn Side B (facing West -> Yaw -90)
            spawnArmyFormation(world, unitsB, sideBPos, -90.0f, countB);
        }

        activeFactionA = factionAId;
        activeFactionB = factionBId;
        battleActive = true;
        battleTickTimer = 0;

        // Sound battle horn and spawn particle blast
        world.playSound(null, center.x, center.y, center.z, SoundEvents.EVENT_RAID_HORN, SoundCategory.NEUTRAL, 2.5f, 1.0f);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1, center.z, 2, 0.5, 0.5, 0.5, 0.0);

        String descA = ("all".equalsIgnoreCase(unitAId) ? "Battalion" : unitAId) + " (" + countA + "x)";
        String descB = ("all".equalsIgnoreCase(unitBId) ? "Battalion" : unitBId) + " (" + countB + "x)";

        Text startMsg = Text.literal("⚔ WAR CLASH COMMENCED: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                .append(Text.literal(factionA.name + " " + descA).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionA.getParsedColor())).withBold(true)))
                .append(Text.literal(" ⚡ VS ⚡ ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(true)))
                .append(Text.literal(factionB.name + " " + descB).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionB.getParsedColor())).withBold(true)));

        server.getPlayerManager().broadcast(startMsg, false);
    }

    private static List<UnitDefinition> resolveUnits(UnitWorldData data, String factionId, String unitId) {
        if (unitId != null && !unitId.isBlank() && !"all".equalsIgnoreCase(unitId) && !"mixed".equalsIgnoreCase(unitId)) {
            UnitDefinition single = data.units.get(unitId);
            if (single != null) {
                // Ensure faction tag matches
                UnitDefinition copy = new UnitDefinition(single.toNbt());
                copy.factionId = factionId;
                return List.of(copy);
            }
        }

        List<UnitDefinition> factionUnits = data.units.values().stream()
                .filter(u -> u.factionId.equalsIgnoreCase(factionId))
                .toList();

        if (!factionUnits.isEmpty()) {
            return factionUnits;
        }

        // If no units in faction, take any available units and assign to this faction
        if (!data.units.isEmpty()) {
            List<UnitDefinition> assigned = new ArrayList<>();
            for (UnitDefinition u : data.units.values()) {
                UnitDefinition copy = new UnitDefinition(u.toNbt());
                copy.factionId = factionId;
                assigned.add(copy);
            }
            return assigned;
        }

        return Collections.emptyList();
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

    private static void spawnCircleFormation(ServerWorld world, List<UnitDefinition> units, Vec3d center, int count, double radius, boolean inward) {
        for (int i = 0; i < count; i++) {
            UnitDefinition template = units.get(i % units.size());
            double angle = (2 * Math.PI * i) / count;
            double px = center.x + radius * Math.cos(angle);
            double pz = center.z + radius * Math.sin(angle);

            int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(px), (int) Math.floor(pz));
            Vec3d spawnPos = new Vec3d(px, Math.abs(topY - center.y) < 15.0 ? topY : center.y, pz);

            float yaw = (float) Math.toDegrees(Math.atan2(center.z - pz, center.x - px)) - 90.0f;
            if (!inward) yaw += 180.0f;

            LivingEntity entity = UnitSpawner.spawn(world, template, spawnPos, yaw);
            if (entity != null) {
                entity.getCommandTags().add("battle_mob");
            }
        }
    }

    public static void tickBattleCheck(ServerWorld world) {
        if (!battleActive || world == null || activeFactionA == null || activeFactionB == null) return;

        battleTickTimer++;
        if (battleTickTimer < 20) return; // Warm-up period

        int countA = countAliveBattleFaction(world, activeFactionA);
        int countB = countAliveBattleFaction(world, activeFactionB);

        // Real-time Action Bar HUD update for all players in the server
        if (battleTickTimer % 10 == 0) {
            MinecraftServer server = world.getServer();
            if (server != null) {
                UnitWorldData data = UnitWorldData.get(server);
                Faction fA = data.factions.get(activeFactionA);
                Faction fB = data.factions.get(activeFactionB);
                String nameA = fA != null ? fA.name : activeFactionA;
                String nameB = fB != null ? fB.name : activeFactionB;
                int colA = fA != null ? fA.getParsedColor() : 0x3b82f6;
                int colB = fB != null ? fB.getParsedColor() : 0xef4444;

                Text hudText = Text.literal("⚔ ")
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                        .append(Text.literal(nameA + ": " + countA + " Alive").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colA)).withBold(true)))
                        .append(Text.literal(" ⚡ VS ⚡ ").setStyle(Style.EMPTY.withColor(Formatting.WHITE)))
                        .append(Text.literal(nameB + ": " + countB + " Alive").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colB)).withBold(true)))
                        .append(Text.literal(" ⚔").setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));

                for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                    p.sendMessage(hudText, true); // Actionbar display
                }
            }
        }

        // Check for victory or mutual destruction
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
