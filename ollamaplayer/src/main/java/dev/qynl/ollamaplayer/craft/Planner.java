package dev.qynl.ollamaplayer.craft;

import dev.qynl.ollamaplayer.craft.Recipes.Ing;
import dev.qynl.ollamaplayer.craft.Recipes.Raw;
import dev.qynl.ollamaplayer.craft.Recipes.Recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Turns "I want an iron pickaxe" into an ordered list of steps: mine what is missing, place a
 * crafting table or furnace if none is available, craft, smelt. Works on a simulated inventory
 * so it never plans with items it does not have.
 */
public final class Planner {

    public enum Kind { MINE, CRAFT, SMELT, PLACE_TABLE, PLACE_FURNACE, FAIL }

    public record Step(Kind kind, String key, int count) {
        @Override
        public String toString() {
            return kind + "(" + key + "x" + count + ")";
        }
    }

    private static final int MAX_STEPS = 400;

    private final ToIntFunction<String> invCount;
    private final Map<String, Integer> sim = new HashMap<>();
    private final List<Step> steps = new ArrayList<>();
    private final boolean tableNear;
    private final boolean furnaceNear;
    private int pickTier;
    private boolean failed;
    private String failReason;
    private boolean tableSet;
    private boolean furnaceSet;

    private Planner(ToIntFunction<String> invCount, boolean tableNear, boolean furnaceNear, int pickTier) {
        this.invCount = invCount;
        this.tableNear = tableNear;
        this.furnaceNear = furnaceNear;
        this.pickTier = pickTier;
    }

    /**
     * @param invCount   how many of an ingredient key the inventory holds (groups supported)
     * @param pickTier   best pickaxe tier currently held (0 none, 1 wood, 2 stone, 3 iron, 4 diamond)
     */
    public static Result plan(List<String> targets, int count, ToIntFunction<String> invCount,
                              boolean tableNear, boolean furnaceNear, int pickTier) {
        Planner p = new Planner(invCount, tableNear, furnaceNear, pickTier);
        for (String t : targets) {
            if (p.failed) break;
            p.need(t, count);
            if (p.steps.size() > MAX_STEPS) {
                p.fail("that is too much to plan in one go");
                break;
            }
        }
        return new Result(p.steps, p.failed, p.failReason);
    }

    public record Result(List<Step> steps, boolean failed, String reason) {
        public Deque<Step> deque() {
            return new ArrayDeque<>(steps);
        }
    }

    private void fail(String reason) {
        if (!failed) {
            failed = true;
            failReason = reason;
            steps.add(new Step(Kind.FAIL, reason, 0));
        }
    }

    private int have(String key) {
        Integer v = sim.get(key);
        if (v == null) {
            v = invCount.applyAsInt(key);
            sim.put(key, v);
        }
        return v;
    }

    private static boolean isEquipment(String key) {
        return key.endsWith("pickaxe") || key.endsWith("_axe") || key.endsWith("_sword")
                || key.endsWith("shovel") || key.endsWith("helmet") || key.endsWith("chestplate")
                || key.endsWith("leggings") || key.endsWith("boots") || key.equals("shield");
    }

    /** Make sure at least {@code n} of {@code key} exist in the simulated inventory after the plan runs. */
    private void need(String key, int n) {
        if (failed || n <= 0) return;
        int have = have(key);
        if (have >= n) {
            if (!isEquipment(key)) sim.put(key, have - n);
            return;
        }
        int short_ = n - have;
        sim.put(key, 0);

        if (Recipes.isSmeltable(key)) {
            String raw = Recipes.smeltInput(key);
            need(raw, short_);
            need("coal", short_);
            ensureStation("furnace");
            steps.add(new Step(Kind.SMELT, key, short_));
            return;
        }

        Raw raw = Recipes.raw(key);
        if (raw != null) {
            if (raw.tier() > 0) ensureTool(raw.tier());
            if (failed) return;
            steps.add(new Step(Kind.MINE, raw.mineKeyword(), short_));
            return;
        }

        Recipe r = Recipes.recipe(key);
        if (r == null) {
            fail("I don't know how to get " + key.replace('_', ' '));
            return;
        }
        int crafts = (short_ + r.outCount() - 1) / r.outCount();
        if (r.table()) ensureStation("crafting_table");
        if (failed) return;
        for (Ing ing : r.ings()) {
            need(ing.key(), crafts * ing.count());
            if (failed) return;
        }
        steps.add(new Step(Kind.CRAFT, key, crafts));
        sim.put(key, crafts * r.outCount() - short_);
    }

    private void ensureTool(int tier) {
        if (pickTier >= tier) return;
        String name = switch (tier) {
            case 1 -> "wooden_pickaxe";
            case 2 -> "stone_pickaxe";
            case 3 -> "iron_pickaxe";
            default -> "diamond_pickaxe";
        };
        need(name, 1);
        pickTier = Math.max(pickTier, tier);
    }

    private void ensureStation(String station) {
        if (station.equals("crafting_table")) {
            if (tableNear || tableSet) return;
            need("crafting_table", 1);
            if (failed) return;
            steps.add(new Step(Kind.PLACE_TABLE, "crafting_table", 1));
            tableSet = true;
        } else {
            if (furnaceNear || furnaceSet) return;
            need("furnace", 1);
            if (failed) return;
            steps.add(new Step(Kind.PLACE_FURNACE, "furnace", 1));
            furnaceSet = true;
        }
    }
}
