package dev.qynl.ollamaplayer.craft;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hand-written recipe book. Keys are item paths ("iron_ingot") or groups ("#logs", "#planks").
 * Using a table instead of the server's recipe manager keeps the planner simple and predictable.
 */
public final class Recipes {
    public record Ing(String key, int count) {}

    /** One craft: produces {@code outCount} of {@code output}; {@code table} means a crafting table must be within reach. */
    public record Recipe(String output, int outCount, List<Ing> ings, boolean table) {}

    /** A raw resource the companion can mine by block keyword, with the minimum pickaxe tier it needs (0 = none). */
    public record Raw(String mineKeyword, int tier) {}

    public static final String LOGS = "#logs";
    public static final String PLANKS = "#planks";

    private static final Map<String, Recipe> RECIPES = new HashMap<>();
    private static final Map<String, Raw> RAW = new HashMap<>();
    private static final Map<String, String[]> SMELT = new HashMap<>();

    static {
        add(PLANKS, 4, List.of(new Ing(LOGS, 1)), false);
        add("stick", 4, List.of(new Ing(PLANKS, 2)), false);
        add("crafting_table", 1, List.of(new Ing(PLANKS, 4)), false);
        add("chest", 1, List.of(new Ing(PLANKS, 8)), true);
        add("furnace", 1, List.of(new Ing("cobblestone", 8)), true);
        add("torch", 4, List.of(new Ing("coal", 1), new Ing("stick", 1)), false);
        add("shield", 1, List.of(new Ing(PLANKS, 6), new Ing("iron_ingot", 1)), true);
        add("bucket", 1, List.of(new Ing("iron_ingot", 3)), true);

        for (String[] t : new String[][] {
                {"wooden", PLANKS}, {"stone", "cobblestone"}, {"iron", "iron_ingot"}, {"diamond", "diamond"}}) {
            String tier = t[0];
            String mat = t[1];
            add(tier + "_pickaxe", 1, List.of(new Ing(mat, 3), new Ing("stick", 2)), true);
            add(tier + "_axe", 1, List.of(new Ing(mat, 3), new Ing("stick", 2)), true);
            add(tier + "_shovel", 1, List.of(new Ing(mat, 1), new Ing("stick", 2)), true);
            add(tier + "_sword", 1, List.of(new Ing(mat, 2), new Ing("stick", 1)), true);
        }
        for (String[] a : new String[][] {
                {"helmet", "5"}, {"chestplate", "8"}, {"leggings", "7"}, {"boots", "4"}}) {
            add("iron_" + a[0], 1, List.of(new Ing("iron_ingot", Integer.parseInt(a[1]))), true);
            add("diamond_" + a[0], 1, List.of(new Ing("diamond", Integer.parseInt(a[1]))), true);
        }

        RAW.put(LOGS, new Raw("log", 0));
        RAW.put("cobblestone", new Raw("stone", 1));
        RAW.put("coal", new Raw("coal_ore", 1));
        RAW.put("raw_iron", new Raw("iron_ore", 2));
        RAW.put("raw_copper", new Raw("copper_ore", 2));
        RAW.put("raw_gold", new Raw("gold_ore", 3));
        RAW.put("diamond", new Raw("diamond_ore", 3));
        RAW.put("redstone", new Raw("redstone_ore", 2));
        RAW.put("sand", new Raw("sand", 0));

        SMELT.put("iron_ingot", new String[] {"raw_iron"});
        SMELT.put("gold_ingot", new String[] {"raw_gold"});
        SMELT.put("copper_ingot", new String[] {"raw_copper"});
    }

    private Recipes() {}

    private static void add(String out, int n, List<Ing> ings, boolean table) {
        RECIPES.put(out, new Recipe(out, n, ings, table));
    }

    public static Recipe recipe(String key) {
        return RECIPES.get(key);
    }

    public static Raw raw(String key) {
        return RAW.get(key);
    }

    /** Smelting input for an ingot, or null. The fuel is always coal. */
    public static String smeltInput(String ingot) {
        String[] s = SMELT.get(ingot);
        return s == null ? null : s[0];
    }

    public static boolean isSmeltable(String key) {
        return SMELT.containsKey(key);
    }

    /** Tier of a pickaxe-like tool by material prefix: 1 wooden/golden, 2 stone, 3 iron, 4 diamond, 5 netherite. */
    public static int toolTier(ItemStack s) {
        return materialTier(Registries.ITEM.getId(s.getItem()).getPath());
    }

    public static int materialTier(String id) {
        if (id.startsWith("netherite")) return 5;
        if (id.startsWith("diamond")) return 4;
        if (id.startsWith("iron")) return 3;
        if (id.startsWith("stone") || id.startsWith("chain")) return 2;
        if (id.startsWith("wooden") || id.startsWith("golden") || id.startsWith("leather")) return 1;
        return 0;
    }

    /** Whether an inventory stack satisfies an ingredient key. */
    public static boolean matches(ItemStack s, String key) {
        if (s.isEmpty()) return false;
        if (LOGS.equals(key)) return s.isIn(ItemTags.LOGS);
        if (PLANKS.equals(key)) return s.isIn(ItemTags.PLANKS);
        return Registries.ITEM.getId(s.getItem()).getPath().equals(key);
    }

    /** Human-friendly aliases the model or players may use. Returns a recipe or raw key. */
    public static String normalize(String raw) {
        String t = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (t.startsWith("minecraft:")) t = t.substring(10);
        return switch (t) {
            case "wood_pickaxe", "wood_pick" -> "wooden_pickaxe";
            case "wood_axe" -> "wooden_axe";
            case "wood_sword" -> "wooden_sword";
            case "wood_shovel" -> "wooden_shovel";
            case "table", "workbench" -> "crafting_table";
            case "plank", "planks", "wood_planks" -> PLANKS;
            case "log", "logs", "wood", "wood_log" -> LOGS;
            case "pickaxe", "pick" -> "iron_pickaxe";
            case "axe" -> "iron_axe";
            case "sword" -> "iron_sword";
            case "shovel" -> "iron_shovel";
            case "armor", "armour", "gear", "kit" -> "gear";
            case "iron", "iron_ore" -> "iron_ingot";
            case "gold", "gold_ore" -> "gold_ingot";
            case "copper", "copper_ore" -> "copper_ingot";
            case "cobble" -> "cobblestone";
            case "helmet" -> "iron_helmet";
            case "chestplate", "chest_plate" -> "iron_chestplate";
            case "leggings", "pants" -> "iron_leggings";
            case "boots" -> "iron_boots";
            case "coal_ore" -> "coal";
            case "diamond_ore" -> "diamond";
            default -> t;
        };
    }

    /** The full set of items the "gear up" goal produces, in priority order. */
    public static List<String> gearGoal() {
        List<String> out = new ArrayList<>();
        out.add("iron_pickaxe");
        out.add("iron_sword");
        out.add("shield");
        out.add("iron_helmet");
        out.add("iron_chestplate");
        out.add("iron_leggings");
        out.add("iron_boots");
        return out;
    }

    /** Planks type matching a log item path (oak_log -> oak_planks, crimson_stem -> crimson_planks). */
    public static String planksFor(String logPath) {
        if (logPath.endsWith("_log")) return logPath.substring(0, logPath.length() - 4) + "_planks";
        if (logPath.endsWith("_stem")) return logPath.substring(0, logPath.length() - 5) + "_planks";
        return "oak_planks";
    }
}
