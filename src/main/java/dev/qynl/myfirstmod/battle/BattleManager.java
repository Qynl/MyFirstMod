package dev.qynl.myfirstmod.battle;

import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages concurrent, isolated, multi-instance battles across any world dimensions.
 */
public final class BattleManager {
    private static final Map<UUID, BattleInstance> BATTLES = new ConcurrentHashMap<>();

    private BattleManager() {}

    public static BattleInstance createBattle(ServerPlayerEntity player,
                                              String factionAId, String unitAId, int countA,
                                              String factionBId, String unitBId, int countB,
                                              String formation, double distance) {
        if (player == null || player.getServer() == null) return null;
        MinecraftServer server = player.getServer();
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
            return null;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos();
        double battleDist = Math.max(12.0, Math.min(80.0, distance));

        countA = Math.max(1, Math.min(100, countA));
        countB = Math.max(1, Math.min(100, countB));

        UUID battleId = UUID.randomUUID();
        BattleInstance battle = new BattleInstance(
                battleId, world, factionAId, unitAId, countA, factionBId, unitBId, countB, center, battleDist, formation, world.getTime()
        );

        String fStyle = formation != null ? formation.toLowerCase() : "line";

        if ("ambush".equals(fStyle)) {
            spawnCircleFormation(world, unitsA, center, countA, 4.0, true, battle, true);
            spawnCircleFormation(world, unitsB, center, countB, battleDist / 2.0, false, battle, false);
        } else if ("flank".equals(fStyle)) {
            Vec3d sideAPos = center.add(-battleDist / 2.0, 0, 0);
            spawnArmyFormation(world, unitsA, sideAPos, 90.0f, countA, battle, true);

            int halfB = Math.max(1, countB / 2);
            Vec3d flankNorth = center.add(battleDist / 4.0, 0, -18.0);
            Vec3d flankSouth = center.add(battleDist / 4.0, 0, 18.0);
            spawnArmyFormation(world, unitsB, flankNorth, -45.0f, halfB, battle, false);
            spawnArmyFormation(world, unitsB, flankSouth, -135.0f, countB - halfB, battle, false);
        } else {
            Vec3d sideAPos = center.add(-battleDist / 2.0, 0, 0);
            Vec3d sideBPos = center.add(battleDist / 2.0, 0, 0);

            spawnArmyFormation(world, unitsA, sideAPos, 90.0f, countA, battle, true);
            spawnArmyFormation(world, unitsB, sideBPos, -90.0f, countB, battle, false);
        }

        BATTLES.put(battleId, battle);

        // Sound battle horn and spawn particle blast & battlefield perimeter markers
        world.playSound(null, center.x, center.y, center.z, SoundEvents.EVENT_RAID_HORN, SoundCategory.NEUTRAL, 2.5f, 1.0f);
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1, center.z, 3, 0.5, 0.5, 0.5, 0.0);
        world.spawnParticles(ParticleTypes.FLASH, center.x, center.y + 1.5, center.z, 2, 0.1, 0.1, 0.1, 0.0);

        // Clashing faction perimeter marker ring
        for (int i = 0; i < 36; i++) {
            double angle = (2 * Math.PI * i) / 36.0;
            double px = center.x + Math.cos(angle) * (battleDist / 2.0);
            double pz = center.z + Math.sin(angle) * (battleDist / 2.0);
            world.spawnParticles(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME, px, center.y + 0.3, pz, 1, 0, 0, 0, 0.02);
        }

        String descA = ("all".equalsIgnoreCase(unitAId) ? "Battalion" : unitAId) + " (" + countA + "x)";
        String descB = ("all".equalsIgnoreCase(unitBId) ? "Battalion" : unitBId) + " (" + countB + "x)";

        Text startMsg = Text.literal("⚔ WAR CLASH COMMENCED: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                .append(Text.literal(factionA.name + " " + descA).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionA.getParsedColor())).withBold(true)))
                .append(Text.literal(" ⚡ VS ⚡ ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(true)))
                .append(Text.literal(factionB.name + " " + descB).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionB.getParsedColor())).withBold(true)));

        server.getPlayerManager().broadcast(startMsg, false);
        return battle;
    }

    private static List<UnitDefinition> resolveUnits(UnitWorldData data, String factionId, String unitId) {
        if (unitId != null && !unitId.isBlank() && !"all".equalsIgnoreCase(unitId) && !"mixed".equalsIgnoreCase(unitId)) {
            UnitDefinition single = data.units.get(unitId);
            if (single != null) {
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

    private static void spawnArmyFormation(ServerWorld world, List<UnitDefinition> units, Vec3d basePos, float yaw, int totalCount, BattleInstance battle, boolean isSideA) {
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
                entity.getCommandTags().add("battle_id:" + battle.id);
                if (isSideA) battle.addTroopA(entity);
                else battle.addTroopB(entity);
            }
        }
    }

    private static void spawnCircleFormation(ServerWorld world, List<UnitDefinition> units, Vec3d center, int count, double radius, boolean inward, BattleInstance battle, boolean isSideA) {
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
                entity.getCommandTags().add("battle_id:" + battle.id);
                if (isSideA) battle.addTroopA(entity);
                else battle.addTroopB(entity);
            }
        }
    }

    public static void tickBattles(ServerWorld world) {
        if (world == null || BATTLES.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>();
        for (BattleInstance battle : BATTLES.values()) {
            if (battle.world == world) {
                battle.tick();
                if (!battle.active) {
                    toRemove.add(battle.id);
                }
            }
        }
        for (UUID id : toRemove) {
            BATTLES.remove(id);
        }
    }

    public static int clearAllBattles(ServerWorld world) {
        if (world == null) return 0;
        int total = 0;
        List<UUID> toRemove = new ArrayList<>();

        for (BattleInstance battle : BATTLES.values()) {
            if (battle.world == world) {
                total += battle.clear();
                toRemove.add(battle.id);
            }
        }

        for (UUID id : toRemove) {
            BATTLES.remove(id);
        }
        return total;
    }

    public static boolean hasActiveBattles() {
        return !BATTLES.isEmpty();
    }

    public static Collection<BattleInstance> getActiveBattles() {
        return Collections.unmodifiableCollection(BATTLES.values());
    }
}
