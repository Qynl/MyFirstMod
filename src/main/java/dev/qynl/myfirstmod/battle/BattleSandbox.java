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

import java.util.ArrayList;
import java.util.List;

public final class BattleSandbox {
    private BattleSandbox() {}

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

        double battleDistance = 25.0;
        Vec3d sideAPos = center.add(-battleDistance / 2.0, 0, 0);
        Vec3d sideBPos = center.add(battleDistance / 2.0, 0, 0);

        // Spawn Side A (facing Side B -> Yaw 90)
        spawnArmyFormation(world, unitsA, sideAPos, 90.0f, armySize);

        // Spawn Side B (facing Side A -> Yaw -90)
        spawnArmyFormation(world, unitsB, sideBPos, -90.0f, armySize);

        // Sound battle horn
        world.playSound(null, center.x, center.y, center.z, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.NEUTRAL, 2.0f, 1.0f);

        Text startMsg = Text.literal("⚔ BATTLE COMMENCED: ")
                .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true))
                .append(Text.literal(factionA.name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionA.getParsedColor())).withBold(true)))
                .append(Text.literal(" VS ").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)))
                .append(Text.literal(factionB.name).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(factionB.getParsedColor())).withBold(true)));

        server.getPlayerManager().broadcast(startMsg, false);
    }

    private static void spawnArmyFormation(ServerWorld world, List<UnitDefinition> units, Vec3d basePos, float yaw, int totalCount) {
        int cols = 4;
        for (int i = 0; i < totalCount; i++) {
            UnitDefinition template = units.get(i % units.size());
            int row = i / cols;
            int col = i % cols;

            double offsetX = (row) * (yaw > 0 ? -2.0 : 2.0);
            double offsetZ = (col - (cols / 2.0)) * 2.0;

            Vec3d spawnPos = basePos.add(offsetX, 0, offsetZ);
            LivingEntity entity = UnitSpawner.spawn(world, template, spawnPos, yaw);
            if (entity != null) {
                entity.getCommandTags().add("battle_mob");
            }
        }
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
}
