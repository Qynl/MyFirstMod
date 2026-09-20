package dev.qynl.myfirstmod.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.qynl.myfirstmod.faction.Faction;
import dev.qynl.myfirstmod.unit.UnitDefinition;
import dev.qynl.myfirstmod.unit.UnitSystem;
import dev.qynl.myfirstmod.unit.UnitWorldData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Administrative and datapack-friendly editing surface for the same saved library used by the GUI. */
public final class UnitCommands {
    private UnitCommands() {}
    public static void register() { CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(CommandManager.literal("unit")
        .then(CommandManager.literal("list").executes(c -> list(c.getSource())))
        .then(CommandManager.literal("create").then(CommandManager.argument("id",StringArgumentType.word()).then(CommandManager.argument("entity",IdentifierArgumentType.identifier()).then(CommandManager.argument("name",StringArgumentType.greedyString()).executes(c -> create(c.getSource(),StringArgumentType.getString(c,"id"),IdentifierArgumentType.getIdentifier(c,"entity"),StringArgumentType.getString(c,"name")))))))
        .then(CommandManager.literal("delete").then(CommandManager.argument("id",StringArgumentType.word()).executes(c -> delete(c.getSource(),StringArgumentType.getString(c,"id")))))
        .then(CommandManager.literal("spawn").then(CommandManager.argument("id",StringArgumentType.word()).executes(c -> spawn(c.getSource(),StringArgumentType.getString(c,"id")))))
        .then(CommandManager.literal("set").then(CommandManager.argument("id",StringArgumentType.word()).then(CommandManager.literal("health").then(CommandManager.argument("value",FloatArgumentType.floatArg(1,1024)).executes(c -> setHealth(c.getSource(),StringArgumentType.getString(c,"id"),FloatArgumentType.getFloat(c,"value"))))).then(CommandManager.literal("damage").then(CommandManager.argument("value",FloatArgumentType.floatArg(0,1024)).executes(c -> setDamage(c.getSource(),StringArgumentType.getString(c,"id"),FloatArgumentType.getFloat(c,"value"))))).then(CommandManager.literal("faction").then(CommandManager.argument("value",StringArgumentType.word()).executes(c -> setFaction(c.getSource(),StringArgumentType.getString(c,"id"),StringArgumentType.getString(c,"value")))))))
        .then(CommandManager.literal("faction").then(CommandManager.literal("create").then(CommandManager.argument("id",StringArgumentType.word()).then(CommandManager.argument("name",StringArgumentType.greedyString()).executes(c -> faction(c.getSource(),StringArgumentType.getString(c,"id"),StringArgumentType.getString(c,"name")))))))); }
    private static int list(ServerCommandSource s){var d=UnitWorldData.get(s.getServer());s.sendFeedback(()->Text.literal("Units ("+d.units.size()+"): "+String.join(", ",d.units.keySet())),false);return d.units.size();}
    private static int create(ServerCommandSource s,String id,Identifier entity,String name){var d=UnitWorldData.get(s.getServer());if(!net.minecraft.registry.Registries.ENTITY_TYPE.getOrEmpty(entity).isPresent()){s.sendError(Text.literal("Unknown entity type: "+entity));return 0;}if(d.units.containsKey(id)){s.sendError(Text.literal("A unit with that id already exists."));return 0;}d.saveUnit(new UnitDefinition(id,name,entity));s.sendFeedback(()->Text.literal("Created unit "+id),true);return 1;}
    private static int delete(ServerCommandSource s,String id){var d=UnitWorldData.get(s.getServer());if(d.units.remove(id)==null){s.sendError(Text.literal("Unknown unit: "+id));return 0;}d.markDirty();return 1;}
    private static int spawn(ServerCommandSource s,String id){var u=UnitWorldData.get(s.getServer()).units.get(id);if(u==null||s.getEntity()==null){s.sendError(Text.literal("Unknown unit or command source is not an entity."));return 0;}if(!(s.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity p)||!UnitSystem.spawn(p,u)){s.sendError(Text.literal("This entity type cannot be spawned as a unit."));return 0;}return 1;}
    private static int setHealth(ServerCommandSource s,String id,float value){var u=UnitWorldData.get(s.getServer()).units.get(id);if(u==null)return 0;u.maxHealth=value;UnitWorldData.get(s.getServer()).markDirty();return 1;}
    private static int setDamage(ServerCommandSource s,String id,float value){var u=UnitWorldData.get(s.getServer()).units.get(id);if(u==null)return 0;u.attackDamage=value;UnitWorldData.get(s.getServer()).markDirty();return 1;}
    private static int setFaction(ServerCommandSource s,String id,String faction){var d=UnitWorldData.get(s.getServer());var u=d.units.get(id);if(u==null)return 0;u.factionId=faction;d.factions.putIfAbsent(faction,new Faction(faction,faction));d.markDirty();return 1;}
    private static int faction(ServerCommandSource s,String id,String name){var d=UnitWorldData.get(s.getServer());d.factions.putIfAbsent(id,new Faction(id,name));d.markDirty();return 1;}
}
