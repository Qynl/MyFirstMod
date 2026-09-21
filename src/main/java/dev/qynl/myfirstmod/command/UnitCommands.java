package dev.qynl.myfirstmod.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.qynl.myfirstmod.battle.BattleSandbox;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.faction.FactionRelation;
import dev.qynl.myfirstmod.unit.BattleStats;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSpawner;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class UnitCommands {
    private UnitCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            // /unit commands
            dispatcher.register(CommandManager.literal("unit")
                    .then(CommandManager.literal("list").executes(c -> listUnits(c.getSource())))
                    .then(CommandManager.literal("stats").executes(c -> showStats(c.getSource())))
                    .then(CommandManager.literal("create")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .then(CommandManager.argument("entity", IdentifierArgumentType.identifier())
                                            .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                                    .executes(c -> createUnit(c.getSource(), StringArgumentType.getString(c, "id"), IdentifierArgumentType.getIdentifier(c, "entity"), StringArgumentType.getString(c, "name")))))))
                    .then(CommandManager.literal("delete")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .executes(c -> deleteUnit(c.getSource(), StringArgumentType.getString(c, "id")))))
                    .then(CommandManager.literal("duplicate")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .executes(c -> duplicateUnit(c.getSource(), StringArgumentType.getString(c, "id")))))
                    .then(CommandManager.literal("spawn")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .executes(c -> spawnUnit(c.getSource(), StringArgumentType.getString(c, "id"), 1, null))
                                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 100))
                                            .executes(c -> spawnUnit(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "count"), null))
                                            .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                                                    .executes(c -> spawnUnit(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "count"), Vec3ArgumentType.getVec3(c, "pos")))))))
                    .then(CommandManager.literal("squad")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .executes(c -> spawnSquad(c.getSource(), StringArgumentType.getString(c, "id"), 5))
                                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 50))
                                            .executes(c -> spawnSquad(c.getSource(), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "count"))))))
                    .then(CommandManager.literal("equip")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .then(CommandManager.argument("slot", StringArgumentType.word())
                                            .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                                    .executes(c -> equipUnit(c.getSource(), StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "slot"), IdentifierArgumentType.getIdentifier(c, "item")))))))
                    .then(CommandManager.literal("inventory")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                            .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                                    .executes(c -> addInventory(c.getSource(), StringArgumentType.getString(c, "id"), IdentifierArgumentType.getIdentifier(c, "item"), IntegerArgumentType.getInteger(c, "count")))))))
                    .then(CommandManager.literal("set")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .then(CommandManager.literal("health").then(CommandManager.argument("value", FloatArgumentType.floatArg(1, 10000)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "health", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("damage").then(CommandManager.argument("value", FloatArgumentType.floatArg(0, 10000)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "damage", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("speed").then(CommandManager.argument("value", FloatArgumentType.floatArg(0.01f, 5.0f)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "speed", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("armor").then(CommandManager.argument("value", FloatArgumentType.floatArg(0, 100)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "armor", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("role").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "role", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("rank").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "rank", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("squad").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "squad", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("faction").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "faction", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("commander").then(CommandManager.argument("value", BoolArgumentType.bool()).executes(c -> setBoolProp(c.getSource(), StringArgumentType.getString(c, "id"), "commander", BoolArgumentType.getBool(c, "value")))))
                                    .then(CommandManager.literal("retreat").then(CommandManager.argument("value", FloatArgumentType.floatArg(0, 0.99f)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "retreat", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("scale").then(CommandManager.argument("value", FloatArgumentType.floatArg(0.1f, 10.0f)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "scale", FloatArgumentType.getFloat(c, "value")))))
                                    .then(CommandManager.literal("entity").then(CommandManager.argument("value", IdentifierArgumentType.identifier()).executes(c -> setEntityProp(c.getSource(), StringArgumentType.getString(c, "id"), IdentifierArgumentType.getIdentifier(c, "value")))))
                                    .then(CommandManager.literal("mount").then(CommandManager.argument("value", StringArgumentType.string()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "mount", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("aura").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "aura", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("death").then(CommandManager.argument("value", StringArgumentType.word()).executes(c -> setStringProp(c.getSource(), StringArgumentType.getString(c, "id"), "death", StringArgumentType.getString(c, "value")))))
                                    .then(CommandManager.literal("heal-range").then(CommandManager.argument("value", FloatArgumentType.floatArg(1, 64)).executes(c -> setAttribute(c.getSource(), StringArgumentType.getString(c, "id"), "heal-range", FloatArgumentType.getFloat(c, "value")))))))
            );

            // /faction commands
            dispatcher.register(CommandManager.literal("faction")
                    .then(CommandManager.literal("list").executes(c -> listFactions(c.getSource())))
                    .then(CommandManager.literal("create")
                            .then(CommandManager.argument("id", StringArgumentType.word())
                                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                            .executes(c -> createFaction(c.getSource(), StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "name"))))))
                    .then(CommandManager.literal("relation")
                            .then(CommandManager.argument("from", StringArgumentType.word())
                                    .then(CommandManager.argument("to", StringArgumentType.word())
                                            .then(CommandManager.argument("relation", StringArgumentType.word())
                                                    .executes(c -> setRelation(c.getSource(), StringArgumentType.getString(c, "from"), StringArgumentType.getString(c, "to"), StringArgumentType.getString(c, "relation")))))))
                    .then(CommandManager.literal("perk")
                            .then(CommandManager.argument("faction", StringArgumentType.word())
                                    .then(CommandManager.argument("perk", StringArgumentType.word())
                                            .executes(c -> addPerk(c.getSource(), StringArgumentType.getString(c, "faction"), StringArgumentType.getString(c, "perk"))))))
            );

            // /battle commands
            var battleCustomDistance = CommandManager.argument("distance", DoubleArgumentType.doubleArg(10.0, 80.0))
                    .executes(c -> startCustomBattle(c.getSource(),
                            StringArgumentType.getString(c, "factionA"),
                            StringArgumentType.getString(c, "unitA"),
                            IntegerArgumentType.getInteger(c, "countA"),
                            StringArgumentType.getString(c, "factionB"),
                            StringArgumentType.getString(c, "unitB"),
                            IntegerArgumentType.getInteger(c, "countB"),
                            StringArgumentType.getString(c, "formation"),
                            DoubleArgumentType.getDouble(c, "distance")));

            var battleCustomFormation = CommandManager.argument("formation", StringArgumentType.word())
                    .executes(c -> startCustomBattle(c.getSource(),
                            StringArgumentType.getString(c, "factionA"),
                            StringArgumentType.getString(c, "unitA"),
                            IntegerArgumentType.getInteger(c, "countA"),
                            StringArgumentType.getString(c, "factionB"),
                            StringArgumentType.getString(c, "unitB"),
                            IntegerArgumentType.getInteger(c, "countB"),
                            StringArgumentType.getString(c, "formation"), 32.0))
                    .then(battleCustomDistance);

            var battleCustomCountB = CommandManager.argument("countB", IntegerArgumentType.integer(1, 100))
                    .executes(c -> startCustomBattle(c.getSource(),
                            StringArgumentType.getString(c, "factionA"),
                            StringArgumentType.getString(c, "unitA"),
                            IntegerArgumentType.getInteger(c, "countA"),
                            StringArgumentType.getString(c, "factionB"),
                            StringArgumentType.getString(c, "unitB"),
                            IntegerArgumentType.getInteger(c, "countB"),
                            "line", 32.0))
                    .then(battleCustomFormation);

            var battleCustom = CommandManager.literal("custom")
                    .then(CommandManager.argument("factionA", StringArgumentType.word())
                            .then(CommandManager.argument("unitA", StringArgumentType.word())
                                    .then(CommandManager.argument("countA", IntegerArgumentType.integer(1, 100))
                                            .then(CommandManager.argument("factionB", StringArgumentType.word())
                                                    .then(CommandManager.argument("unitB", StringArgumentType.word())
                                                            .then(battleCustomCountB))))));

            dispatcher.register(CommandManager.literal("battle")
                    .then(CommandManager.literal("start")
                            .then(CommandManager.argument("faction1", StringArgumentType.word())
                                    .then(CommandManager.argument("faction2", StringArgumentType.word())
                                            .executes(c -> startBattle(c.getSource(), StringArgumentType.getString(c, "faction1"), StringArgumentType.getString(c, "faction2"), 8))
                                            .then(CommandManager.argument("size", IntegerArgumentType.integer(1, 100))
                                                    .executes(c -> startBattle(c.getSource(), StringArgumentType.getString(c, "faction1"), StringArgumentType.getString(c, "faction2"), IntegerArgumentType.getInteger(c, "size")))))))
                    .then(battleCustom)
                    .then(CommandManager.literal("preset")
                            .then(CommandManager.argument("presetName", StringArgumentType.word())
                                    .executes(c -> startPresetBattle(c.getSource(), StringArgumentType.getString(c, "presetName")))))
                    .then(CommandManager.literal("spectate").executes(c -> toggleSpectate(c.getSource())))
                    .then(CommandManager.literal("clear").executes(c -> clearBattle(c.getSource())))
                    .then(CommandManager.literal("stats").executes(c -> showStats(c.getSource())))
                    .then(CommandManager.literal("reset").executes(c -> resetStats(c.getSource())))
            );
        });
    }

    private static int listUnits(ServerCommandSource s) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        s.sendFeedback(() -> Text.literal("Saved Units (" + data.units.size() + "): ").formatted(Formatting.GOLD)
                .append(Text.literal(String.join(", ", data.units.keySet())).formatted(Formatting.WHITE)), false);
        return data.units.size();
    }

    private static int showStats(ServerCommandSource s) {
        BattleStats b = BattleStats.get(s.getServer());
        s.sendFeedback(() -> Text.literal("=== Battle Statistics ===").formatted(Formatting.GOLD), false);
        s.sendFeedback(() -> Text.literal("Kills: " + b.kills.toString()).formatted(Formatting.GREEN), false);
        s.sendFeedback(() -> Text.literal("Deaths: " + b.deaths.toString()).formatted(Formatting.RED), false);
        s.sendFeedback(() -> Text.literal("Damage Dealt: " + b.damageDealt.toString()).formatted(Formatting.YELLOW), false);
        return 1;
    }

    private static int resetStats(ServerCommandSource s) {
        BattleStats.get(s.getServer()).reset();
        s.sendFeedback(() -> Text.literal("Battle statistics have been reset.").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int createUnit(ServerCommandSource s, String id, Identifier entity, String name) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        if (!Registries.ENTITY_TYPE.getOrEmpty(entity).isPresent()) {
            s.sendError(Text.literal("Unknown entity type: " + entity));
            return 0;
        }
        if (data.units.containsKey(id)) {
            s.sendError(Text.literal("Unit with id '" + id + "' already exists."));
            return 0;
        }
        data.saveUnit(new UnitDefinition(id, name, entity));
        s.sendFeedback(() -> Text.literal("Successfully created unit: " + name + " (" + id + ")").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int deleteUnit(ServerCommandSource s, String id) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        if (!data.deleteUnit(id)) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }
        s.sendFeedback(() -> Text.literal("Deleted unit: " + id).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int duplicateUnit(ServerCommandSource s, String id) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition copy = data.duplicateUnit(id);
        if (copy == null) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }
        s.sendFeedback(() -> Text.literal("Duplicated unit to: " + copy.id).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int spawnUnit(ServerCommandSource s, String id, int count, Vec3d pos) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition unit = data.units.get(id);
        if (unit == null) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }

        ServerPlayerEntity player = s.getPlayer();
        if (player == null && pos == null) {
            s.sendError(Text.literal("Position must be specified when executed from console."));
            return 0;
        }

        Vec3d targetPos = pos != null ? pos : player.getPos().add(player.getRotationVector().multiply(2.0));
        float yaw = player != null ? player.getYaw() : 0.0f;

        for (int i = 0; i < count; i++) {
            UnitSpawner.spawn(s.getWorld(), unit, targetPos.add(i * 0.8, 0, 0), yaw);
        }

        s.sendFeedback(() -> Text.literal("Spawned " + count + "x " + unit.name).formatted(Formatting.GREEN), true);
        return count;
    }

    private static int spawnSquad(ServerCommandSource s, String id, int count) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition unit = data.units.get(id);
        if (unit == null) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }
        if (s.getPlayer() == null) {
            s.sendError(Text.literal("Must be executed by a player."));
            return 0;
        }
        UnitSpawner.spawnSquadAtPlayer(s.getPlayer(), unit, count);
        s.sendFeedback(() -> Text.literal("Spawned squad of " + count + "x " + unit.name).formatted(Formatting.GREEN), true);
        return count;
    }

    private static int equipUnit(ServerCommandSource s, String id, String slotStr, Identifier itemId) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition unit = data.units.get(id);
        if (unit == null) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }
        var item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
        if (item == null) {
            s.sendError(Text.literal("Unknown item ID: " + itemId));
            return 0;
        }
        try {
            EquipmentSlot slot = EquipmentSlot.valueOf(slotStr.toUpperCase());
            unit.equipment.put(slot, new ItemStack(item));
            data.markDirty();
            s.sendFeedback(() -> Text.literal("Equipped " + itemId + " in " + slot.getName() + " for " + id).formatted(Formatting.GREEN), true);
            return 1;
        } catch (IllegalArgumentException e) {
            s.sendError(Text.literal("Invalid slot: " + slotStr + " (Use HEAD, CHEST, LEGS, FEET, MAINHAND, OFFHAND)"));
            return 0;
        }
    }

    private static int addInventory(ServerCommandSource s, String id, Identifier itemId, int count) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition unit = data.units.get(id);
        if (unit == null) {
            s.sendError(Text.literal("Unknown unit ID: " + id));
            return 0;
        }
        var item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
        if (item == null) {
            s.sendError(Text.literal("Unknown item ID: " + itemId));
            return 0;
        }
        unit.inventory.add(new ItemStack(item, count));
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Added " + count + "x " + itemId + " to inventory of " + id).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setAttribute(ServerCommandSource s, String id, String attr, float val) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition u = data.units.get(id);
        if (u == null) return 0;
        switch (attr) {
            case "health" -> u.maxHealth = val;
            case "damage" -> u.attackDamage = val;
            case "speed" -> u.movementSpeed = val;
            case "armor" -> u.armor = val;
            case "retreat" -> u.retreatHealth = val;
            case "heal-range" -> u.healRange = val;
            case "scale" -> u.scale = val;
        }
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Updated " + attr + " of " + id + " to " + val).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setEntityProp(ServerCommandSource s, String id, Identifier entity) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition u = data.units.get(id);
        if (u == null) return 0;
        if (!Registries.ENTITY_TYPE.getOrEmpty(entity).isPresent()) {
            s.sendError(Text.literal("Unknown entity type: " + entity));
            return 0;
        }
        u.entityId = entity;
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Updated entity type of " + id + " to " + entity).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setStringProp(ServerCommandSource s, String id, String prop, String val) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition u = data.units.get(id);
        if (u == null) return 0;
        switch (prop) {
            case "role" -> u.role = val;
            case "rank" -> u.rank = val;
            case "squad" -> u.squad = val;
            case "mount" -> u.mount = val;
            case "aura" -> u.particleAura = val;
            case "death" -> u.deathAction = val;
            case "faction" -> {
                u.factionId = val;
                data.factions.putIfAbsent(val, new Faction(val, val));
            }
        }
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Updated " + prop + " of " + id + " to " + val).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setBoolProp(ServerCommandSource s, String id, String prop, boolean val) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        UnitDefinition u = data.units.get(id);
        if (u == null) return 0;
        if ("commander".equals(prop)) {
            u.commander = val;
        }
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Updated " + prop + " of " + id + " to " + val).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int listFactions(ServerCommandSource s) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        s.sendFeedback(() -> Text.literal("Registered Factions (" + data.factions.size() + "): ").formatted(Formatting.GOLD)
                .append(Text.literal(String.join(", ", data.factions.keySet())).formatted(Formatting.WHITE)), false);
        return data.factions.size();
    }

    private static int createFaction(ServerCommandSource s, String id, String name) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        data.saveFaction(new Faction(id, name));
        s.sendFeedback(() -> Text.literal("Created faction: " + name + " (" + id + ")").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setRelation(ServerCommandSource s, String from, String to, String relStr) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        Faction f1 = data.factions.get(from);
        Faction f2 = data.factions.get(to);
        if (f1 == null || f2 == null) {
            s.sendError(Text.literal("Both factions must exist."));
            return 0;
        }
        try {
            FactionRelation rel = FactionRelation.valueOf(relStr.toUpperCase());
            f1.relations.put(to, rel);
            data.markDirty();
            s.sendFeedback(() -> Text.literal("Set relation " + from + " -> " + to + " as " + rel.getDisplayName()).formatted(Formatting.GREEN), true);
            return 1;
        } catch (IllegalArgumentException e) {
            s.sendError(Text.literal("Relation must be ALLIED, NEUTRAL, or HOSTILE."));
            return 0;
        }
    }

    private static int addPerk(ServerCommandSource s, String factionId, String perkName) {
        UnitWorldData data = UnitWorldData.get(s.getServer());
        Faction faction = data.factions.get(factionId);
        if (faction == null) {
            s.sendError(Text.literal("Unknown faction: " + factionId));
            return 0;
        }
        faction.perks.add(perkName.toLowerCase());
        data.markDirty();
        s.sendFeedback(() -> Text.literal("Added perk '" + perkName + "' to faction " + faction.name).formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int startBattle(ServerCommandSource s, String faction1, String faction2, int size) {
        ServerPlayerEntity player = s.getPlayer();
        if (player == null) {
            s.sendError(Text.literal("Must be executed by a player in the world."));
            return 0;
        }
        BattleSandbox.startBattle(player, faction1, faction2, size);
        return 1;
    }

    private static int startCustomBattle(ServerCommandSource s, String factionA, String unitA, int countA, String factionB, String unitB, int countB, String formation, double distance) {
        ServerPlayerEntity player = s.getPlayer();
        if (player == null) {
            s.sendError(Text.literal("Must be executed by a player in the world."));
            return 0;
        }
        BattleSandbox.startCustomBattle(player, factionA, unitA, countA, factionB, unitB, countB, formation, distance);
        return 1;
    }

    private static int startPresetBattle(ServerCommandSource s, String presetName) {
        ServerPlayerEntity player = s.getPlayer();
        if (player == null) {
            s.sendError(Text.literal("Must be executed by a player in the world."));
            return 0;
        }
        String p = presetName.toLowerCase();
        switch (p) {
            case "titan_vs_swarm" -> BattleSandbox.startCustomBattle(player, "villagers", "village_titan", 1, "raiders", "raider_berserker", 25, "ambush", 24.0);
            case "cavalry_charge" -> BattleSandbox.startCustomBattle(player, "kingdom", "royal_cavalry", 12, "undead", "undead_cavalry", 12, "line", 40.0);
            case "pitched_battle" -> BattleSandbox.startCustomBattle(player, "kingdom", "all", 16, "raiders", "all", 16, "line", 32.0);
            case "undead_siege" -> BattleSandbox.startCustomBattle(player, "villagers", "all", 10, "undead", "all", 30, "ambush", 28.0);
            case "paladin_crusade" -> BattleSandbox.startCustomBattle(player, "kingdom", "paladin_crusader", 8, "undead", "undead_archer", 24, "line", 30.0);
            case "dragoon_skirmish" -> BattleSandbox.startCustomBattle(player, "raiders", "desert_dragoon", 10, "kingdom", "royal_archer", 15, "flank", 35.0);
            default -> s.sendError(Text.literal("Unknown preset. Available: titan_vs_swarm, cavalry_charge, pitched_battle, undead_siege, paladin_crusade, dragoon_skirmish"));
        }
        return 1;
    }

    private static int toggleSpectate(ServerCommandSource s) {
        ServerPlayerEntity player = s.getPlayer();
        if (player == null) {
            s.sendError(Text.literal("Must be executed by a player in the world."));
            return 0;
        }

        if (player.isSpectator()) {
            player.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
            s.sendFeedback(() -> Text.literal("Exited Spectator Mode.").formatted(Formatting.YELLOW), true);
        } else {
            player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
            s.sendFeedback(() -> Text.literal("Entered Battle Spectator Mode! Fly freely to observe the battlefield.").formatted(Formatting.AQUA, Formatting.BOLD), true);
        }
        return 1;
    }

    private static int clearBattle(ServerCommandSource s) {
        int count = BattleSandbox.clearAllBattleMobs(s.getWorld());
        s.sendFeedback(() -> Text.literal("Cleared " + count + " battle units from the world.").formatted(Formatting.GREEN), true);
        return count;
    }
}
