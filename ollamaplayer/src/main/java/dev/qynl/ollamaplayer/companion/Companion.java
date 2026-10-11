package dev.qynl.ollamaplayer.companion;

import com.mojang.authlib.GameProfile;
import dev.qynl.ollamaplayer.craft.Planner;
import dev.qynl.ollamaplayer.craft.Recipes;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The body and short-term behaviour of the companion: follow, guard, fight, mine, eat, pick up drops.
 * Everything here is deterministic. The language model only chooses which of these to do.
 */
public final class Companion {
    public enum Mode { FOLLOW, STAY, MINE }

    private static final Set<String> JUNK = Set.of("dirt", "gravel", "andesite", "diorite", "granite", "tuff",
            "netherrack", "rotten_flesh", "cobbled_deepslate", "sand");
    private static final String[] RARE = {"diamond", "emerald", "ancient_debris", "gold_ore", "iron_ore", "lapis", "redstone_ore", "coal_ore"};

    private final NovaBody player;
    private final Consumer<String> say;
    private final Navigator nav = new Navigator();

    {
        nav.setBridger(this::bridgeAt);
    }

    private Mode mode = Mode.FOLLOW;
    private int replanTimer;
    private int eatCooldown;
    private int hurtCooldown;
    private boolean wasFighting;
    private boolean sawEnemy;
    private float lastHealth = 20f;

    private String mineTarget;
    private int mineWanted;
    private int mined;
    private BlockPos mineBlock;
    private float mineProgress;
    private int mineFailures;
    private int swingTimer;
    private final Set<Long> badBlocks = new HashSet<>();
    private int explorations;
    private int digs;
    private boolean digging;
    private final java.util.Random random = new java.util.Random();
    private boolean exploring;
    private int exploreFails;
    private BlockPos exploreGoal;

    private final Set<java.util.UUID> ignoredDrops = new HashSet<>();
    private java.util.UUID chasedDrop;
    private int chaseTicks;

    private final Deque<Planner.Step> plan = new ArrayDeque<>();
    private List<String> planTargets = List.of();
    private int planCount;
    private int replans;
    private String planGoal;
    private boolean planMining;
    private int shieldTimer;
    private boolean huntingLogged;
    private int cookCooldown;
    private int dropGrace;
    private final Set<java.util.UUID> claimed = new HashSet<>();
    private Vec3d lastEnemyPos;
    private BlockPos recoverAt;
    private int recoverTimer;
    private int equipTimer;

    public Companion(NovaBody player, Consumer<String> say) {
        this.player = player;
        this.say = say;
    }

    /** Creates a fresh fake player with a unique profile (avoids FakePlayer's instance cache). */
    public static NovaBody createPlayer(ServerWorld world, Vec3d pos, String name) {
        NovaBody p = new NovaBody(world, new GameProfile(UUID.randomUUID(), name));
        p.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
        world.spawnEntity(p);
        return p;
    }

    public NovaBody player() {
        return player;
    }

    public Mode mode() {
        return mode;
    }

    private final Experience experience = new Experience();

    public Experience experience() {
        return experience;
    }

    public void setMode(Mode m) {
        mode = m;
        nav.clear();
    }

    // ------------------------------------------------------------------ tick

    /** One server tick of decisions. The body's physics run in NovaBody#tick. */
    public void tick(@Nullable ServerPlayerEntity owner) {
        if (owner != null) lastOwner = owner;
        think(owner);
    }

    private void think(@Nullable ServerPlayerEntity owner) {
        ServerWorld world = player.getServerWorld();
        if (replanTimer > 0) replanTimer--;
        if (eatCooldown > 0) eatCooldown--;
        if (hurtCooldown > 0) hurtCooldown--;

        pickUpItems(world);

        float hp = player.getHealth();
        if (hp < 7 && lastHealth >= 7 && hurtCooldown == 0) {
            say.accept("I'm badly hurt!");
            hurtCooldown = 200;
            event("HURT");
            // Learn from it: if this happened in a fight, the lesson is to leave earlier.
            if (nearestEnemy(world, 16) != null) {
                experience.record("badly hurt in a fight", Experience.RETREAT_EARLY);
            }
        }
        lastHealth = hp;

        if (player.getHungerManager().getFoodLevel() <= 14 && eatCooldown == 0) {
            eatFood();
        }
        // Player relationship: if the owner is badly hurt and Nova is too, restock first rather than
        // blindly following into the fight.
        // New area: a long move away from where Nova last looked around. Event only, never a timer.
        if (++areaTimer >= 40) {
            areaTimer = 0;
            int cx = player.getBlockX() >> 4;
            int cz = player.getBlockZ() >> 4;
            if (areaX == Integer.MIN_VALUE) {
                areaX = cx;
                areaZ = cz;
            } else if (Math.abs(cx - areaX) >= 4 || Math.abs(cz - areaZ) >= 4) {
                areaX = cx;
                areaZ = cz;
                event("NEW_AREA");
            }
        }
        if (ownerDangerTimer > 0) ownerDangerTimer--;
        if (owner != null && owner.getHealth() < 8 && player.getHealth() < 8 && ownerDangerTimer == 0) {
            say.accept("I'm too low on health to keep up. I'll restock and catch up.");
            ownerDangerTimer = 600;
            eatFood();
        }

        LivingEntity enemy = nearestEnemy(world, 16);
        // Defend the owner: whatever just hit them, unless it is another player, becomes the target.
        if (owner != null) {
            LivingEntity hitter = owner.getAttacker();
            if (hitter != null && hitter.isAlive() && !(hitter instanceof ServerPlayerEntity)
                    && hitter.squaredDistanceTo(owner) < 144.0 && owner.age - owner.getLastAttackedTime() < 100) {
                if (enemy == null || enemy.squaredDistanceTo(owner) > 16.0) enemy = hitter;
            }
        }
        if (enemy != null && !sawEnemy && player.distanceTo(enemy) < 10) {
            event("ENEMY_APPEARED");
            say.accept("Heads up, " + Registries.ENTITY_TYPE.getId(enemy.getType()).getPath().replace('_', ' ') + "!");
        }
        sawEnemy = enemy != null;

        torchIfDark(world);
        if (enemy == null && mode != Mode.STAY) stashIfFull(world);
        if (enemy == null && ++hotbarTimer >= 40) {
            hotbarTimer = 0;
            organizeHotbar();
        }

        if (enemy != null) {
            wasFighting = true;
            if (avoidCreeper(enemy)) return;
            if (hp <= 10 && eatCooldown == 0 && eatFood()) {
                return;
            }
            // Deterministic judgement: fight, keep the position, or retreat to the owner.
            Tactics.Stance stance = Tactics.decide(situationFor(enemy, owner, world));
            if (stance == Tactics.Stance.RETREAT && owner != null) {
                planTo(world, ownerGoal(owner));
                nav.tick(player, 0.18);
            } else if (stance == Tactics.Stance.AVOID && player.distanceTo(enemy) >= 4.0 && tryShoot(enemy, world)) {
                // Keeping the range advantage with the bow.
            } else {
                fight(enemy, world);
            }
            return;
        }
        if (wasFighting) {
            wasFighting = false;
            nav.clear();
            if (lastEnemyPos != null) claimDropsNear(world, lastEnemyPos, 5.0);
            lastEnemyPos = null;
        }

        if (mode != Mode.STAY && collectDrops(world)) {
            return;
        }

        if (recoverAt != null && enemy == null) {
            if (--recoverTimer <= 0 || player.getPos().squaredDistanceTo(recoverAt.toCenterPos()) < 9.0) {
                recoverAt = null;
                nav.clear();
            } else {
                // A fixed target only needs planning once: replanning mid-jump would start from the wrong cell.
                if (!nav.hasPath() || (replanTimer == 0 && player.isOnGround())) {
                    BlockPos target = recoverAt;
                    planTo(world, new Pathfinder.Goal(
                            p -> p.getSquaredDistance(target) <= 4.0,
                            p -> p.getSquaredDistance(target)));
                    replanTimer = 20;
                }
                if (nav.tick(player, 0.14) == Navigator.Result.FAILED) {
                    recoverAt = null;
                }
                return;
            }
        }

        if (enemy == null && plan.isEmpty() && !planMining && needsFood() && cookCooldown == 0) {
            // Hungry with raw meat and coal in the bag: cook it in a furnace before hunting more.
            String meat = firstRawMeat();
            if (meat != null && countInv("coal") > 0) {
                craftGoal("cooked_" + meat, 1);
                cookCooldown = 100;
            }
        }
        if (cookCooldown > 0) cookCooldown--;

        if (enemy == null && plan.isEmpty() && !planMining && needsFood() && !hasFood()) {
            LivingEntity animal = nearestAnimal(world, 24);
            if (animal != null) {
                if (!huntingLogged) {
                    say.accept("I'm hungry. Going hunting.");
                    huntingLogged = true;
                }
                fight(animal, world);
                return;
            }
        }
        if (!needsFood()) huntingLogged = false;

        if (!plan.isEmpty() || planMining) {
            if (runPlan(world)) return;
        }

        if (++equipTimer % 20 == 0) {
            equipBest();
            dropJunk();
        }

        switch (mode) {
            case FOLLOW -> follow(owner, world);
            case STAY -> {
                nav.clear();
                stopHorizontal();
            }
            case MINE -> mine(world);
        }
    }

    // ------------------------------------------------------------------ commands from the brain

    /** Executes a high-level action. Returns an optional line to say in chat. */
    public String command(String action, @Nullable String target, int count) {
        switch (action) {
            case "follow" -> {
                cancelPlan();
                setMode(Mode.FOLLOW);
            }
            case "stay" -> {
                cancelPlan();
                setMode(Mode.STAY);
            }
            case "mine" -> {
                if (target == null || target.isBlank()) return "Mine what, exactly?";
                String why = MiningTrip.postponeReason(player.getHealth(), player.getHungerManager().getFoodLevel(),
                        bestPickaxeFraction(), bagFraction());
                if (why != null && why.startsWith("I need to eat") && eatFood()) {
                    why = MiningTrip.postponeReason(player.getHealth(), player.getHungerManager().getFoodLevel(),
                            bestPickaxeFraction(), bagFraction());
                }
                if (why != null) return "Not yet: " + why + ". I'll go once that's sorted.";
                startMining(target, count);
            }
            case "craft" -> {
                if (target == null || target.isBlank()) return "Craft what, exactly?";
                return craftGoal(Recipes.normalize(target), count);
            }
            case "gear" -> {
                return craftGoal("gear", 1);
            }
            case "shelter" -> {
                return buildShelter();
            }
            case "give" -> {
                return giveToOwner();
            }
            case "stash" -> {
                String msg = doStash(player.getServerWorld());
                return msg != null ? msg : "I have nothing spare to stash.";
            }
            case "diamond_gear" -> {
                return craftGoal("diamond_gear", 1);
            }
            case "eat" -> {
                if (!eatFood()) return "I've got nothing to eat right now.";
            }
            case "status" -> {
                return statusLine();
            }
            default -> { }
        }
        return null;
    }

    /** Durability left on the best pickaxe in the bag, 0..1. 1 when there is no pickaxe to judge. */
    private double bestPickaxeFraction() {
        double best = -1;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (!(st.getItem() instanceof net.minecraft.item.PickaxeItem) || st.getMaxDamage() <= 0) continue;
            best = Math.max(best, (double) (st.getMaxDamage() - st.getDamage()) / st.getMaxDamage());
        }
        return best < 0 ? 1.0 : best;
    }

    /** Share of the 36 main inventory slots in use, 0..1. */
    private double bagFraction() {
        int used = 0;
        for (int i = 0; i < 36; i++) if (!player.getInventory().getStack(i).isEmpty()) used++;
        return used / 36.0;
    }

    private int ownerDangerTimer;
    private int areaX = Integer.MIN_VALUE;
    private int areaZ = Integer.MIN_VALUE;
    private int areaTimer;

    /** Events the strategist may want to hear about, drained by the brain. Capped so nothing grows unbounded. */
    private final java.util.ArrayDeque<String> events = new java.util.ArrayDeque<>();

    private void event(String e) {
        if (events.size() < 8) events.addLast(e);
    }

    @Nullable
    public String pollEvent() {
        return events.pollFirst();
    }

    public void startMining(String target, int count) {
        cancelPlan();
        beginMine(normalizeTarget(target), count);
    }

    private void beginMine(String keyword, int count) {
        mineTarget = keyword;
        mineWanted = Math.max(1, Math.min(64, count <= 0 ? 8 : count));
        mined = 0;
        mineFailures = 0;
        mineBlock = null;
        mineProgress = 0;
        explorations = 0;
        exploring = false;
        digs = 0;
        digging = false;
        badBlocks.clear();
        setMode(Mode.MINE);
    }

    private void cancelPlan() {
        plan.clear();
        planMining = false;
        planGoal = null;
    }

    /** After a death, walk back to where the stuff was dropped. */
    public void recoverAt(BlockPos where) {
        recoverAt = where;
        recoverTimer = 600;
        claimDropsNear(player.getServerWorld(), where.toCenterPos(), 6.0);
        say.accept("I'll go back for my stuff at " + where.toShortString() + ".");
    }

    /** Plans and starts crafting (or gearing up) toward the target. Returns a chat line for the outcome. */
    public String craftGoal(String target, int count) {
        boolean gear = target.equals("gear") || target.equals("diamond_gear");
        List<String> targets = target.equals("gear") ? Recipes.gearGoal()
                : target.equals("diamond_gear") ? Recipes.diamondGoal() : List.of(target);
        if (mode == Mode.MINE && plan.isEmpty()) setMode(Mode.FOLLOW);
        int n = gear ? 1 : Math.max(1, Math.min(64, count <= 0 ? 1 : count));
        ServerWorld world = player.getServerWorld();
        boolean table = findNearby(world, Blocks.CRAFTING_TABLE, 4) != null;
        boolean furnace = findNearby(world, Blocks.FURNACE, 4) != null;
        Planner.Result res = Planner.plan(targets, n, this::countInv, table, furnace, pickTier());
        if (res.failed()) {
            return "I can't make that: " + res.reason() + ".";
        }
        if (res.steps().isEmpty()) {
            return "I already have that.";
        }
        cancelPlan();
        plan.addAll(res.steps());
        planTargets = targets;
        planCount = n;
        replans = 0;
        planGoal = target.equals("gear") ? "a full iron kit" : target.equals("diamond_gear") ? "a full diamond kit"
                : n + " " + target.replace('_', ' ');
        int crafts = 0;
        for (Planner.Step st : res.steps()) if (st.kind() == Planner.Kind.CRAFT) crafts++;
        return "On it: " + planGoal + " (" + res.steps().size() + " steps, " + crafts + " crafts).";
    }

    /** Developer diagnostics; not shown to players. */
    public String debugState() {
        Vec3d v = player.getVelocity();
        String drops = nearbyDrops(player.getServerWorld());
        return String.format(Locale.ROOT,
                "mode=%s pos=(%.2f,%.2f,%.2f) vel=(%.3f,%.3f,%.3f) onGround=%s hasPath=%s mineBlock=%s progress=%.3f mined=%d/%d failures=%d drops=[%s] inv=[%s]",
                mode, player.getX(), player.getY(), player.getZ(), v.x, v.y, v.z, player.isOnGround(),
                nav.hasPath(), mineBlock, mineProgress, mined, mineWanted, mineFailures, drops, inventorySummary());
    }

    public String statusLine() {
        String what = switch (mode) {
            case FOLLOW -> "following you";
            case STAY -> "guarding this spot";
            case MINE -> "mining " + mineTarget + " (" + mined + "/" + mineWanted + ")";
        };
        if (planGoal != null) what = "working on " + planGoal + (planMining ? ", mining " + mineTarget : "");
        return String.format(Locale.ROOT, "Health %.0f/20, food %d/20, %s.",
                player.getHealth(), player.getHungerManager().getFoodLevel(), what);
    }

    /** Ground truth about the body, given to the language model as its observation. */
    public String describe(@Nullable ServerPlayerEntity owner) {
        ServerWorld world = player.getServerWorld();
        StringBuilder sb = new StringBuilder();
        sb.append("- Health ").append(Math.round(player.getHealth())).append("/20, food ")
                .append(player.getHungerManager().getFoodLevel()).append("/20.\n");
        sb.append("- Current task: ").append(statusLine()).append('\n');
        ItemStack held = player.getMainHandStack();
        sb.append("- Holding: ").append(held.isEmpty() ? "nothing" : held.getName().getString()).append(".\n");
        sb.append("- Inventory: ").append(inventorySummary()).append(".\n");
        sb.append("- Wearing: ").append(wornSummary()).append(".\n");
        java.util.List<String> lessons = experience.top(3);
        if (!lessons.isEmpty()) sb.append("- Lessons learned: ").append(String.join("; ", lessons)).append(".\n");
        sb.append("- Stash chest: ").append(stashPos == null ? "none yet"
                : "at " + stashPos.toShortString() + ", " + Math.round(Math.sqrt(player.getBlockPos().getSquaredDistance(stashPos))) + " blocks away").append(".\n");
        LivingEntity enemy = nearestEnemy(world, 16);
        sb.append("- Hostile mobs nearby: ").append(enemy == null ? "none"
                : Registries.ENTITY_TYPE.getId(enemy.getType()).getPath() + " about "
                + Math.round(player.distanceTo(enemy)) + " blocks away").append(".\n");
        sb.append("- Blocks around you: ").append(nearbyBlocks(world)).append(".\n");
        if (owner == null) {
            sb.append("- Your owner is not online.\n");
        } else {
            sb.append("- Your owner ").append(owner.getName().getString()).append(" is ")
                    .append(Math.round(player.distanceTo(owner))).append(" blocks away.\n");
        }
        long t = world.getTimeOfDay() % 24000L;
        sb.append("- It is ").append(t >= 13000 && t < 23000 ? "night" : "day").append(".\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------ behaviours

    private void follow(@Nullable ServerPlayerEntity owner, ServerWorld world) {
        if (owner == null) {
            nav.clear();
            stopHorizontal();
            return;
        }
        Vec3d op = owner.getPos();
        double d = player.getPos().distanceTo(op);
        if (d > 35) {
            teleportNear(owner);
            return;
        }
        if (d <= 3.0) {
            nav.clear();
            stopHorizontal();
            faceTo(owner.getEyePos());
            return;
        }
        if (replanTimer == 0 || !nav.hasPath()) {
            planTo(world, ownerGoal(owner));
            replanTimer = 15;
        }
        Navigator.Result r = nav.tick(player, d > 8 ? 0.16 : 0.12);
        if (r == Navigator.Result.FAILED) {
            stopHorizontal();
            if (d > 12) teleportNear(owner);
        }
    }

    private Pathfinder.Goal ownerGoal(ServerPlayerEntity owner) {
        Vec3d op = owner.getPos();
        return new Pathfinder.Goal(
                p -> Math.hypot(p.getX() + 0.5 - op.x, p.getZ() + 0.5 - op.z) <= 2.5 && Math.abs(p.getY() - op.y) <= 2,
                p -> Math.hypot(p.getX() + 0.5 - op.x, p.getZ() + 0.5 - op.z));
    }

    /** The numbers the combat judgement needs, read from the body and the world. */
    private Tactics.Situation situationFor(LivingEntity enemy, @Nullable ServerPlayerEntity owner, ServerWorld world) {
        int enemies = world.getEntitiesByClass(net.minecraft.entity.mob.HostileEntity.class,
                player.getBoundingBox().expand(12), LivingEntity::isAlive).size();
        int armor = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!player.getEquippedStack(slot).isEmpty()) armor++;
        }
        ItemStack hand = player.getMainHandStack();
        boolean sword = hand.getItem() instanceof net.minecraft.item.SwordItem || hand.getItem() instanceof net.minecraft.item.AxeItem;
        int retreatBonus = Math.min(3, experience.count(Experience.RETREAT_EARLY));
        return new Tactics.Situation(
                Registries.ENTITY_TYPE.getId(enemy.getType()).getPath(),
                player.distanceTo(enemy), Math.max(1, enemies), player.getHealth(),
                sword, armor, player.getOffHandStack().getItem() instanceof ShieldItem,
                owner == null ? -1.0 : player.distanceTo(owner), retreatBonus);
    }

    private void fight(LivingEntity enemy, ServerWorld world) {
        equipWeapon();
        lastEnemyPos = enemy.getPos();
        double d = player.distanceTo(enemy);
        faceTo(enemy.getEyePos());
        if (d >= 4.0 && tryShoot(enemy, world)) return;
        if (d > 2.6) {
            Vec3d ep = enemy.getPos();
            if (replanTimer == 0 || !nav.hasPath()) {
                planTo(world, new Pathfinder.Goal(
                        p -> Math.hypot(p.getX() + 0.5 - ep.x, p.getZ() + 0.5 - ep.z) <= 2.0 && Math.abs(p.getY() - ep.y) <= 2,
                        p -> Math.hypot(p.getX() + 0.5 - ep.x, p.getZ() + 0.5 - ep.z)));
                replanTimer = 8;
            }
            if (nav.tick(player, 0.16) == Navigator.Result.FAILED) stopHorizontal();
        } else {
            nav.clear();
            stopHorizontal();
            boolean ready = player.getAttackCooldownProgress(0.5F) >= 1.0F;
            boolean shield = player.getOffHandStack().getItem() instanceof ShieldItem;
            if (shield && !ready && d <= 3.5) {
                if (!player.isBlocking()) player.setCurrentHand(Hand.OFF_HAND);
            } else if (player.isBlocking()) {
                player.clearActiveItem();
            }
            if (ready) {
                player.attack(enemy);
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private int hotbarTimer;
    private int stashCooldown;

    private static final Set<String> KEEP_BLOCKS = Set.of("crafting_table", "furnace", "chest", "torch");

    /**
     * When the bag is nearly full, bulk blocks go into a chest placed nearby. The table, furnace,
     * torches and a reserve of cobblestone and dirt for bridges stay in the bag.
     */
    private void stashIfFull(ServerWorld world) {
        if (stashCooldown > 0) {
            stashCooldown--;
            return;
        }
        PlayerInventory inv = player.getInventory();
        int used = 0;
        for (int i = 0; i < 36; i++) {
            if (!inv.getStack(i).isEmpty()) used++;
        }
        if (used < 30) return;
        String msg = doStash(world);
        // A failed attempt (no room, full chest) is reported, then retried much less often.
        boolean failed = msg == null || msg.startsWith("My ");
        stashCooldown = failed ? 1200 : 200;
        if (msg != null) say.accept(msg);
    }

    /**
     * Puts the spare blocks into Nova's stash chest, placing one first if needed. Returns the message
     * to say, or null when there was nothing to stash.
     */
    @Nullable
    public String doStash(ServerWorld world) {
        PlayerInventory inv = player.getInventory();
        int bulk = 0;
        for (int i = 0; i < 36; i++) bulk += stashable(inv.getStack(i));
        if (bulk == 0) return null;

        BlockPos spot = null;
        net.minecraft.inventory.Inventory chest = null;
        if (stashPos != null && world.getBlockState(stashPos).isOf(Blocks.CHEST)
                && player.getBlockPos().getSquaredDistance(stashPos) <= 36.0
                && world.getBlockEntity(stashPos) instanceof net.minecraft.inventory.Inventory existing) {
            spot = stashPos;
            chest = existing;
        } else {
            spot = findChestSpot(world);
            if (spot == null) return "My bag is nearly full and there is no room for a chest.";
            world.setBlockState(spot, Blocks.CHEST.getDefaultState());
            if (!(world.getBlockEntity(spot) instanceof net.minecraft.inventory.Inventory placed)) {
                world.removeBlock(spot, false);
                return "My chest would not open here.";
            }
            chest = placed;
        }
        int moved = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            int extra = stashable(s);
            if (extra <= 0) continue;
            ItemStack rest = s.copyWithCount(extra);
            insertIntoChest(chest, rest);
            int stored = extra - rest.getCount();
            s.decrement(stored);
            moved += stored;
        }
        chest.markDirty();
        stashPos = spot;
        if (moved > 0) experience.record("bag nearly full", Experience.STASH_FIRST);
        if (moved == 0) return "My stash chest is full.";
        return "Stashed " + moved + " blocks in a chest at " + spot.toShortString() + ".";
    }

    /** The one chest Nova placed itself. Village chests are never touched. */
    @Nullable
    private BlockPos stashPos;

    @Nullable
    public BlockPos stashPos() {
        return stashPos;
    }

    public void setStashPos(@Nullable BlockPos pos) {
        stashPos = pos;
    }

    private static int chestCount(net.minecraft.inventory.Inventory chest, String key) {
        int n = 0;
        for (int j = 0; j < chest.size(); j++) {
            ItemStack st = chest.getStack(j);
            if (Recipes.matches(st, key)) n += st.getCount();
        }
        return n;
    }

    /**
     * Moves up to {@code count} of {@code key} from the stash into the bag. Only works when the body is
     * within a few blocks of the chest. Returns how many were moved.
     */
    public int takeFromStash(String key, int count) {
        if (stashPos == null || player.getBlockPos().getSquaredDistance(stashPos) > 9.0) return 0;
        if (!(player.getWorld().getBlockEntity(stashPos) instanceof net.minecraft.inventory.Inventory chest)) {
            stashPos = null;
            return 0;
        }
        PlayerInventory inv = player.getInventory();
        int need = count;
        for (int j = 0; j < chest.size() && need > 0; j++) {
            ItemStack st = chest.getStack(j);
            if (!Recipes.matches(st, key)) continue;
            ItemStack moved = st.split(Math.min(need, st.getCount()));
            need -= moved.getCount();
            inv.insertStack(moved);
            if (!moved.isEmpty()) {
                // The bag was too full for all of it: put the rest back.
                if (chest.getStack(j).isEmpty()) chest.setStack(j, moved);
                else chest.getStack(j).increment(moved.getCount());
                need += moved.getCount();
            }
        }
        chest.markDirty();
        return count - need;
    }

    /** 1 = walking to the stash, 2 = fetched everything, 0 = nothing in the stash, mine instead. */
    private int tryFetch(ServerWorld world, Planner.Step s) {
        if (stashPos == null || !world.getBlockState(stashPos).isOf(Blocks.CHEST)) return 0;
        if (!(world.getBlockEntity(stashPos) instanceof net.minecraft.inventory.Inventory chest)
                || chestCount(chest, s.key()) < s.count()) {
            return 0;
        }
        if (player.getBlockPos().getSquaredDistance(stashPos) > 9.0) {
            BlockPos st = stashPos;
            if (replanTimer == 0 || !nav.hasPath()) {
                replanTimer = 15;
                if (!planTo(world, new Pathfinder.Goal(
                        p -> p.getSquaredDistance(st) <= 6.0,
                        p -> p.getSquaredDistance(st)))) {
                    return 0;
                }
            }
            return nav.tick(player, 0.14) == Navigator.Result.FAILED ? 0 : 1;
        }
        nav.clear();
        int got = takeFromStash(s.key(), s.count());
        if (got > 0) say.accept("Fetched " + got + " " + s.key().replace('_', ' ') + " from my chest.");
        return got >= s.count() ? 2 : 0;
    }

    /** How many items of this stack can go into the chest; 0 for anything the plans may still need. */
    private static int stashable(ItemStack s) {
        if (s.isEmpty() || !(s.getItem() instanceof BlockItem)) return 0;
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        if (KEEP_BLOCKS.contains(id)) return 0;
        int keep = (id.equals("cobblestone") || id.equals("dirt")) ? 32 : 0;
        return Math.max(0, s.getCount() - keep);
    }

    private static void insertIntoChest(net.minecraft.inventory.Inventory chest, ItemStack rest) {
        for (int j = 0; j < chest.size() && !rest.isEmpty(); j++) {
            ItemStack cur = chest.getStack(j);
            if (cur.isEmpty()) {
                chest.setStack(j, rest.copy());
                rest.setCount(0);
            } else if (ItemStack.areItemsAndComponentsEqual(cur, rest)) {
                int add = Math.min(cur.getMaxCount() - cur.getCount(), rest.getCount());
                cur.increment(add);
                rest.decrement(add);
            }
        }
    }

    @Nullable
    private BlockPos findChestSpot(ServerWorld world) {
        BlockPos origin = player.getBlockPos();
        for (int ring = 2; ring <= 3; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos p = origin.add(dx, 0, dz);
                    BlockPos below = p.down();
                    if (!world.getBlockState(p).isAir() || !world.getBlockState(below).isSolidBlock(world, below)) continue;
                    if (hasDropAt(world, p) || touchesStation(world, p)) continue;
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Equipping only looks at the hotbar, so the best sword, pickaxe and food are moved there,
     * displacing whatever is least useful in the first nine slots.
     */
    private void organizeHotbar() {
        PlayerInventory inv = player.getInventory();
        promote(inv, s -> s.getItem() instanceof SwordItem, s -> 10 + Recipes.toolTier(s));
        promote(inv, s -> s.getItem() instanceof PickaxeItem, s -> Recipes.toolTier(s));
        promote(inv, s -> s.get(DataComponentTypes.FOOD) != null, s -> s.get(DataComponentTypes.FOOD).nutrition());
    }

    /** Moves the best matching stack from the main bag into the hotbar, if it beats what is there. */
    private static void promote(PlayerInventory inv, java.util.function.Predicate<ItemStack> match,
                                java.util.function.ToIntFunction<ItemStack> score) {
        int bestIdx = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 9; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !match.test(s)) continue;
            int sc = score.applyAsInt(s);
            if (sc > bestScore) {
                bestScore = sc;
                bestIdx = i;
            }
        }
        if (bestIdx < 0) return;
        int hotbarBest = Integer.MIN_VALUE;
        int victim = -1;
        int victimScore = Integer.MAX_VALUE;
        for (int h = 0; h < 9; h++) {
            ItemStack s = inv.getStack(h);
            if (!s.isEmpty() && match.test(s)) hotbarBest = Math.max(hotbarBest, score.applyAsInt(s));
            // Victim order: empty slots, then plain junk (blocks), then other gear. Never the stack we are promoting for.
            boolean useful = !s.isEmpty() && (s.isDamageable() || s.get(DataComponentTypes.FOOD) != null);
            int junk = s.isEmpty() ? -1 : match.test(s) ? 100 : useful ? 75 : 50;
            if (junk < victimScore) {
                victimScore = junk;
                victim = h;
            }
        }
        if (bestScore <= hotbarBest || victim < 0) return;
        ItemStack moving = inv.getStack(bestIdx);
        inv.setStack(bestIdx, inv.getStack(victim));
        inv.setStack(victim, moving);
    }

    private int shotCooldown;

    /** Fires an arrow at ranged mobs when Nova carries a bow and arrows. Returns true if it shot this tick. */
    private boolean tryShoot(LivingEntity enemy, ServerWorld world) {
        if (shotCooldown > 0) {
            shotCooldown--;
            return false;
        }
        boolean ranged = enemy instanceof net.minecraft.entity.mob.SkeletonEntity
                || enemy instanceof net.minecraft.entity.mob.PillagerEntity
                || enemy instanceof net.minecraft.entity.mob.WitchEntity
                || enemy instanceof net.minecraft.entity.mob.BlazeEntity;
        double d = player.distanceTo(enemy);
        if (!ranged || d > 15.0 || countInv("bow") < 1 || countInv("arrow") < 1 || !player.canSee(enemy)) {
            return false;
        }
        Vec3d from = player.getEyePos();
        Vec3d to = enemy.getEyePos();
        // Aim a little high to compensate for arrow drop over distance.
        double dx = to.x - from.x, dy = to.y - from.y + d * 0.06, dz = to.z - from.z;
        net.minecraft.entity.projectile.ArrowEntity arrow = new net.minecraft.entity.projectile.ArrowEntity(
                world, player, new ItemStack(Items.ARROW), null);
        arrow.setVelocity(dx, dy, dz, 2.8F, 0.0F);
        arrow.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
        world.spawnEntity(arrow);
        consume("arrow", 1);
        player.swingHand(Hand.MAIN_HAND);
        shotCooldown = 25;
        return true;
    }

    private void mine(ServerWorld world) {
        if (exploring && walkExplore(world)) return;

        if (mineBlock != null && digging && world.getBlockState(mineBlock).isAir()) {
            digging = false;
            mineBlock = null;
            mineProgress = 0;
        }
        if (mineBlock == null || (!digging && !isTarget(world.getBlockState(mineBlock)))) {
            mineBlock = null;
            mineProgress = 0;
            digging = false;
            if (!findTarget(world)) {
                if (explorations < 6) {
                    if (startExplore(world)) return;
                    if (!player.isOnGround()) return;
                }
                if (canDigDown(world)) {
                    mineBlock = player.getBlockPos().down();
                    digging = true;
                    digs++;
                } else {
                    finishMining();
                    return;
                }
            }
        }

        Vec3d center = mineBlock.toCenterPos();
        if (player.getEyePos().distanceTo(center) > 4.3) {
            if (replanTimer == 0 || !nav.hasPath()) {
                boolean ok = planTo(world, new Pathfinder.Goal(
                        p -> eyeAt(p).distanceTo(center) <= 3.8,
                        p -> eyeAt(p).distanceTo(center)));
                replanTimer = 20;
                if (!ok) {
                    markBad();
                    return;
                }
            }
            if (nav.tick(player, 0.13) == Navigator.Result.FAILED) {
                markBad();
            }
            return;
        }

        nav.clear();
        stopHorizontal();
        faceTo(center);
        BlockState st = world.getBlockState(mineBlock);
        if (mineProgress == 0) equipTool(st);
        float delta = st.calcBlockBreakingDelta(player, world, mineBlock);
        if (delta <= 0) {
            markBad();
            return;
        }
        mineProgress += delta;
        if (++swingTimer % 6 == 0) player.swingHand(Hand.MAIN_HAND);
        if (mineProgress >= 1.0F) {
            String id = blockId(st);
            boolean wasDig = digging;
            BlockPos brokeAt = mineBlock;
            world.breakBlock(mineBlock, true, player);
            claimDropsNear(world, brokeAt.toCenterPos(), 2.0);
            if (!wasDig) mined++;
            digging = false;
            mineBlock = null;
            mineProgress = 0;
            if (wasDig) return;
            for (String rare : RARE) {
                if (id.contains(rare)) {
                    say.accept("Found " + id.replace('_', ' ') + "!");
                    break;
                }
            }
            if (mined >= mineWanted) finishMining();
        }
    }

    private static boolean isDeepOre(String t) {
        return t.equals("diamond_ore") || t.equals("gold_ore") || t.equals("redstone_ore") || t.equals("emerald_ore");
    }

    /** Digging straight down one block at a time: the body falls into the hole, so there is no fall damage. */
    private boolean canDigDown(ServerWorld world) {
        if (mineTarget == null || !isDeepOre(mineTarget) || digs >= 60) return false;
        if (player.getBlockY() <= -54 || !player.isOnGround()) return false;
        BlockPos below = player.getBlockPos().down();
        BlockState bs = world.getBlockState(below);
        if (bs.isAir() || !bs.getFluidState().isEmpty() || bs.getHardness(world, below) < 0) return false;
        return !nearLiquid(world, player.getBlockPos().down(2)) && !nearLiquid(world, below);
    }

    private static boolean nearLiquid(ServerWorld world, BlockPos c) {
        for (BlockPos p : BlockPos.iterate(c.add(-1, -1, -1), c.add(1, 1, 1))) {
            if (!world.getFluidState(p).isEmpty()) return true;
        }
        return false;
    }

    private boolean startExplore(ServerWorld world) {
        if (!player.isOnGround()) return false;
        BlockPos c = player.getBlockPos();
        for (int attempt = 0; attempt < 3; attempt++) {
            explorations++;
            double ang = random.nextDouble() * Math.PI * 2;
            int dist = 22 + random.nextInt(10);
            exploreGoal = new BlockPos(c.getX() + (int) (Math.cos(ang) * dist), c.getY(),
                    c.getZ() + (int) (Math.sin(ang) * dist));
            if (planTo(world, exploreGoal())) {
                exploring = true;
                replanTimer = 40;
                return true;
            }
        }
        return false;
    }

    private Pathfinder.Goal exploreGoal() {
        final int gx = exploreGoal.getX();
        final int gy = exploreGoal.getY();
        final int gz = exploreGoal.getZ();
        return new Pathfinder.Goal(
                p -> Math.abs(p.getX() - gx) <= 2 && Math.abs(p.getZ() - gz) <= 2 && Math.abs(p.getY() - gy) <= 4,
                p -> Math.hypot(p.getX() - gx, p.getZ() - gz));
    }

    /** Walks toward the exploration point. Returns true while still walking. */
    private boolean walkExplore(ServerWorld world) {
        if (exploreGoal == null) {
            exploring = false;
            return false;
        }
        double dx = player.getX() - (exploreGoal.getX() + 0.5);
        double dz = player.getZ() - (exploreGoal.getZ() + 0.5);
        if (Math.hypot(dx, dz) <= 3.0) {
            exploring = false;
            return false;
        }
        if (replanTimer == 0 || !nav.hasPath()) {
            replanTimer = 40;
            if (!planTo(world, exploreGoal())) {
                exploring = false;
                return false;
            }
        }
        if (nav.tick(player, 0.14) == Navigator.Result.FAILED) {
            exploring = false;
            return false;
        }
        return true;
    }

    /** Walks to the nearest item drop within reach so it lands in the inventory. Returns true while busy. */
    private boolean collectDrops(ServerWorld world) {
        Box box = player.getBoundingBox().expand(8.0, 3.0, 8.0);
        ItemEntity target = null;
        double best = Double.MAX_VALUE;
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (ignoredDrops.contains(e.getUuid()) || !claimed.contains(e.getUuid())) continue;
            double d = player.squaredDistanceTo(e);
            if (d < best) {
                best = d;
                target = e;
            }
        }
        if (target == null) return false;
        if (target.getUuid().equals(chasedDrop)) {
            if (++chaseTicks > 200) {
                ignoredDrops.add(target.getUuid()); // probably cannot be picked up (full inventory)
                chasedDrop = null;
                chaseTicks = 0;
                return false;
            }
        } else {
            chasedDrop = target.getUuid();
            chaseTicks = 0;
        }
        if (best < 2.25 && mineBlock == null) {
            // Already on top of it: pickUpItems() takes it on this tick.
            return false;
        }
        if (replanTimer == 0 || !nav.hasPath()) {
            Vec3d ip = target.getPos();
            planTo(world, new Pathfinder.Goal(
                    p -> Math.hypot(p.getX() + 0.5 - ip.x, p.getZ() + 0.5 - ip.z) <= 0.7 && Math.abs(p.getY() - ip.y) <= 1.5,
                    p -> Math.hypot(p.getX() + 0.5 - ip.x, p.getZ() + 0.5 - ip.z)));
            replanTimer = 10;
        }
        if (nav.tick(player, 0.13) != Navigator.Result.MOVING) {
            return false;
        }
        return true;
    }

    /** Marks item drops near a point as ours, so only drops this companion caused are fetched from afar. */
    private void claimDropsNear(ServerWorld world, Vec3d center, double radius) {
        Box box = new Box(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            claimed.add(e.getUuid());
        }
    }

    /** Diagnostics: nearby drops, ignored drop count, and the body's position. */
    public String debugDrops() {
        ServerWorld w = player.getServerWorld();
        StringBuilder cells = new StringBuilder();
        for (ItemEntity e : w.getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(8, 3, 8), ItemEntity::isAlive)) {
            BlockPos c = e.getBlockPos();
            cells.append(" cell[").append(blockId(w.getBlockState(c))).append('/')
                 .append(blockId(w.getBlockState(c.down()))).append('/').append(blockId(w.getBlockState(c.up()))).append(']');
        }
        StringBuilder map = new StringBuilder();
        BlockPos b = player.getBlockPos();
        for (int y = 1; y >= -1; y--) {
            map.append(" y").append(y).append(':');
            for (int z = -3; z <= 3; z++) {
                for (int x = -3; x <= 3; x++) {
                    String id = blockId(w.getBlockState(b.add(x, y, z)));
                    map.append(id.length() > 3 ? id.substring(0, 3) : id).append(' ');
                }
                map.append('|');
            }
        }
        return map + " " + cells + " " + nearbyDrops(player.getServerWorld()) + " ignored=" + ignoredDrops.size()
                + " body=" + String.format(Locale.ROOT, "%.2f,%.2f,%.2f", player.getX(), player.getY(), player.getZ());
    }

    /** Counts item drops near the body, for diagnostics. */
    public String nearbyDrops(ServerWorld world) {
        Box box = player.getBoundingBox().expand(8.0, 3.0, 8.0);
        StringBuilder sb = new StringBuilder();
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getStack().getCount()).append("x").append(e.getStack().getItem())
              .append(" at ").append(String.format(Locale.ROOT, "%.1f,%.1f,%.1f", e.getX(), e.getY(), e.getZ()));
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private void finishMining() {
        if (mined > 0) say.accept("Done. Got " + mined + " " + mineTarget + ".");
        else say.accept("I can't find any " + mineTarget + " around here.");
        mineBlock = null;
        setMode(Mode.FOLLOW);
    }

    private void markBad() {
        if (mineBlock != null) badBlocks.add(mineBlock.asLong());
        mineBlock = null;
        mineProgress = 0;
        nav.clear();
        if (++mineFailures >= 10) finishMining();
    }

    private boolean findTarget(ServerWorld world) {
        BlockPos c = player.getBlockPos();
        List<BlockPos> hits = new ArrayList<>();
        for (BlockPos p : BlockPos.iterate(c.add(-24, -10, -24), c.add(24, 10, 24))) {
            if (badBlocks.contains(p.asLong())) continue;
            if (isTarget(world.getBlockState(p))) hits.add(p.toImmutable());
        }
        hits.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(c)));
        for (int i = 0; i < Math.min(4, hits.size()); i++) {
            BlockPos b = hits.get(i);
            Vec3d center = b.toCenterPos();
            if (planTo(world, new Pathfinder.Goal(
                    p -> eyeAt(p).distanceTo(center) <= 3.8,
                    p -> eyeAt(p).distanceTo(center)))) {
                mineBlock = b;
                replanTimer = 20;
                return true;
            }
            badBlocks.add(b.asLong());
        }
        return false;
    }

    // ------------------------------------------------------------------ planned work

    /** Runs the head of the plan. Returns true if this tick was used (walking, placing, crafting). */
    private boolean runPlan(ServerWorld world) {
        if (planMining) {
            if (mode == Mode.MINE) return false;
            planMining = false;
            if (mined < mineWanted) {
                abort("I couldn't find enough " + mineTarget.replace('_', ' ') + " nearby.");
                return true;
            }
        }
        if (plan.isEmpty()) {
            if (planGoal != null) {
                say.accept("Got everything for " + planGoal + ".");
                planGoal = null;
            }
            return false;
        }
        Planner.Step s = plan.peek();
        switch (s.kind()) {
            case FAIL -> {
                abort("I can't do that: " + s.key() + ".");
                return true;
            }
            case MINE -> {
                int fetched = tryFetch(world, s);
                if (fetched == 1) return true;
                plan.poll();
                if (fetched == 2) return true;
                beginMine(s.key(), s.count());
                planMining = true;
                return false;
            }
            case PLACE_TABLE -> {
                plan.poll();
                placeStation(world, Blocks.CRAFTING_TABLE, "crafting_table");
                return true;
            }
            case PLACE_FURNACE -> {
                plan.poll();
                placeStation(world, Blocks.FURNACE, "furnace");
                return true;
            }
            case CRAFT -> {
                Recipes.Recipe r = Recipes.recipe(s.key());
                if (r != null && r.table() && findNearby(world, Blocks.CRAFTING_TABLE, 4) == null) {
                    walkToStation(world, Blocks.CRAFTING_TABLE, "crafting table");
                    return true;
                }
                if (waitForDrops(world)) return true;
                plan.poll();
                doCraft(s);
                return true;
            }
            case SMELT -> {
                if (findNearby(world, Blocks.FURNACE, 4) == null) {
                    walkToStation(world, Blocks.FURNACE, "furnace");
                    return true;
                }
                if (waitForDrops(world)) return true;
                plan.poll();
                doSmelt(s.key(), s.count());
                return true;
            }
        }
        return true;
    }

    /** Gives a freshly dropped stack a few ticks to be picked up before a step needs it. */
    private boolean waitForDrops(ServerWorld world) {
        if (dropGrace >= 60) return false;
        Box box = player.getBoundingBox().expand(8.0, 4.0, 8.0);
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (!ignoredDrops.contains(e.getUuid()) && claimed.contains(e.getUuid())) {
                dropGrace++;
                return true;
            }
        }
        dropGrace = 0;
        return false;
    }

    private void abort(String reason) {
        cancelPlan();
        if (mode == Mode.MINE) setMode(Mode.FOLLOW);
        say.accept(reason);
    }

    private void walkToStation(ServerWorld world, Block block, String label) {
        BlockPos st = findNearby(world, block, 24);
        if (st == null) {
            abort("I need a " + label + " nearby, and I can't find one.");
            return;
        }
        if (replanTimer == 0 || !nav.hasPath()) {
            replanTimer = 15;
            if (!planTo(world, new Pathfinder.Goal(
                    p -> p.getSquaredDistance(st) <= 6.0,
                    p -> p.getSquaredDistance(st)))) {
                abort("I can't get to the " + label + ".");
                return;
            }
        }
        if (nav.tick(player, 0.14) == Navigator.Result.FAILED) {
            abort("I can't get to the " + label + ".");
        }
    }

    /** Re-plans the current goal from what the inventory holds now. Used when an item went missing mid-plan. */
    private void replan(ServerWorld world, String why) {
        if (replans >= 3 || planTargets.isEmpty()) {
            abort(why);
            return;
        }
        replans++;
        Planner.Result res = Planner.plan(planTargets, planCount, this::countInv,
                findNearby(world, Blocks.CRAFTING_TABLE, 4) != null, findNearby(world, Blocks.FURNACE, 4) != null, pickTier());
        cancelPlan();
        if (res.failed() || res.steps().isEmpty()) {
            abort(why);
            return;
        }
        plan.addAll(res.steps());
        planTargets = planTargets;
        planGoal = planGoal == null ? "more materials" : planGoal;
        say.accept(why + " Going back for more.");
    }

    private void doCraft(Planner.Step s) {
        Recipes.Recipe r = Recipes.recipe(s.key());
        if (r == null) {
            abort("I don't know how to make " + s.key() + ".");
            return;
        }
        int crafts = s.count();
        for (Recipes.Ing ing : r.ings()) {
            if (countInv(ing.key()) < crafts * ing.count()) {
                replan(player.getServerWorld(), "I'm short on " + ing.key().replace('#', ' ') + ".");
                return;
            }
        }
        String outPath = r.output();
        if (Recipes.PLANKS.equals(outPath)) {
            outPath = "oak_planks";
            PlayerInventory inv = player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack st = inv.getStack(i);
                if (Recipes.matches(st, Recipes.LOGS)) {
                    outPath = Recipes.planksFor(Registries.ITEM.getId(st.getItem()).getPath());
                    break;
                }
            }
        }
        for (Recipes.Ing ing : r.ings()) consume(ing.key(), crafts * ing.count());
        int total = crafts * r.outCount();
        give(new ItemStack(itemOf(outPath), total));
        player.swingHand(Hand.MAIN_HAND);
        if (!outPath.endsWith("planks") && !outPath.equals("stick")) {
            say.accept("Made " + total + " " + outPath.replace('_', ' ') + ".");
        }
    }

    private void doSmelt(String ingot, int n) {
        String raw = Recipes.smeltInput(ingot);
        if (countInv(raw) < n || countInv("coal") < n) {
            replan(player.getServerWorld(), "I need " + n + " " + raw.replace('_', ' ') + " and " + n + " coal to smelt.");
            return;
        }
        consume(raw, n);
        consume("coal", n);
        give(new ItemStack(itemOf(ingot), n));
        player.swingHand(Hand.MAIN_HAND);
        say.accept("Smelted " + n + " " + ingot.replace('_', ' ') + ".");
    }

    /**
     * Places a station two blocks away from the body, never orthogonally touching another station,
     * so stations cannot wall the body in.
     */
    private void placeStation(ServerWorld world, Block block, String itemPath) {
        if (countInv(itemPath) < 1) {
            abort("I have no " + itemPath.replace('_', ' ') + " to place.");
            return;
        }
        BlockPos origin = player.getBlockPos();
        for (int ring = 2; ring <= 3; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos p = origin.add(dx, 0, dz);
                    BlockPos below = p.down();
                    if (!world.getBlockState(p).isAir() || !world.getBlockState(below).isSolidBlock(world, below)) continue;
                    if (hasDropAt(world, p) || touchesStation(world, p)) continue;
                    world.setBlockState(p, block.getDefaultState());
                    consume(itemPath, 1);
                    player.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
        abort("There is no room to place the " + itemPath.replace('_', ' ') + " here.");
    }

    private static boolean touchesStation(ServerWorld world, BlockPos p) {
        for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos n = p.offset(d);
            if (world.getBlockState(n).isOf(Blocks.CRAFTING_TABLE) || world.getBlockState(n).isOf(Blocks.FURNACE)) {
                return true;
            }
        }
        return false;
    }

    /** True if an item entity lies in this block cell (placing a block there would trap the item). */
    private static boolean hasDropAt(ServerWorld world, BlockPos p) {
        return !world.getEntitiesByClass(ItemEntity.class, new Box(p), ItemEntity::isAlive).isEmpty();
    }

    private void give(ItemStack stack) {
        ItemStack left = stack.copy();
        player.getInventory().insertStack(left);
        if (!left.isEmpty()) player.dropItem(left, false);
    }

    private void consume(String key, int n) {
        PlayerInventory inv = player.getInventory();
        int left = n;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack st = inv.getStack(i);
            if (!Recipes.matches(st, key)) continue;
            int take = Math.min(left, st.getCount());
            st.decrement(take);
            left -= take;
        }
    }

    private int countInv(String key) {
        PlayerInventory inv = player.getInventory();
        int total = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (Recipes.matches(st, key)) total += st.getCount();
        }
        return total;
    }

    private int pickTier() {
        PlayerInventory inv = player.getInventory();
        int best = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (st.getItem() instanceof PickaxeItem) best = Math.max(best, Recipes.toolTier(st));
        }
        return best;
    }

    @Nullable
    private BlockPos findNearby(ServerWorld world, Block block, int radius) {
        BlockPos c = player.getBlockPos();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.iterate(c.add(-radius, -radius, -radius), c.add(radius, radius, radius))) {
            if (!world.getBlockState(p).isOf(block)) continue;
            double d = p.getSquaredDistance(c);
            if (d < bestD) {
                bestD = d;
                best = p.toImmutable();
            }
        }
        return best;
    }

    private static Item itemOf(String path) {
        return Registries.ITEM.get(Identifier.of("minecraft", path));
    }

    /** Puts on better armor from the inventory, moves a shield to the off hand, and keeps the best sword in reach. */
    private void equipBest() {
        PlayerInventory inv = player.getInventory();
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            ItemStack cur = player.getEquippedStack(slot);
            int curTier = cur.isEmpty() ? -1 : armorTier(cur);
            int bestIdx = -1;
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.getItem() instanceof ArmorItem a && a.getSlotType() == slot) {
                    int t = armorTier(s);
                    if (t > curTier) {
                        curTier = t;
                        bestIdx = i;
                    }
                }
            }
            if (bestIdx >= 0) {
                ItemStack cand = inv.getStack(bestIdx).copy();
                player.equipStack(slot, cand);
                inv.setStack(bestIdx, cur.isEmpty() ? ItemStack.EMPTY : cur.copy());
                say.accept("Wearing " + cand.getName().getString() + ".");
            }
        }
        if (player.getOffHandStack().isEmpty()) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                if (s.getItem() instanceof ShieldItem) {
                    player.equipStack(EquipmentSlot.OFFHAND, s.copy());
                    inv.setStack(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    /** Keeps two slots free by dropping filler blocks. The dropped stacks are ignored so they are not picked up again. */
    private void dropJunk() {
        PlayerInventory inv = player.getInventory();
        int empty = 0;
        for (int i = 0; i < 36; i++) if (inv.getStack(i).isEmpty()) empty++;
        if (empty >= 2) return;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !JUNK.contains(Registries.ITEM.getId(s.getItem()).getPath())) continue;
            ItemEntity dropped = player.dropItem(s.copy(), false);
            if (dropped != null) ignoredDrops.add(dropped.getUuid());
            inv.setStack(i, ItemStack.EMPTY);
            return;
        }
    }

    private static int armorTier(ItemStack s) {
        return Recipes.materialTier(Registries.ITEM.getId(s.getItem()).getPath());
    }

    private String wornSummary() {
        StringBuilder sb = new StringBuilder();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(Registries.ITEM.getId(s.getItem()).getPath());
        }
        if (!player.getOffHandStack().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("off-hand ").append(Registries.ITEM.getId(player.getOffHandStack().getItem()).getPath());
        }
        return sb.length() == 0 ? "no armor" : sb.toString();
    }

    // ------------------------------------------------------------------ survival extras

    private int torchCooldown;
    private ServerPlayerEntity lastOwner;

    /** Drops every item in the bag at the owner's feet, keeping worn armor and the tools in hand. */
    public String giveToOwner() {
        ServerPlayerEntity owner = lastOwner;
        if (owner == null || owner.isRemoved() || owner.getWorld() != player.getWorld()) {
            return "Your owner isn't close enough to take anything.";
        }
        PlayerInventory inv = player.getInventory();
        int moved = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            ItemEntity dropped = owner.dropItem(s.copy(), false);
            if (dropped != null) {
                dropped.setOwner(owner.getUuid());
                ignoredDrops.add(dropped.getUuid());
            }
            moved += s.getCount();
            inv.setStack(i, ItemStack.EMPTY);
        }
        return moved == 0 ? "I'm not carrying anything." : "Here, " + owner.getName().getString() + ". " + moved + " items.";
    }

    /** Places a torch at the body's feet when it is underground and has torches; keeps caves from filling with mobs. */
    private void torchIfDark(ServerWorld world) {
        if (torchCooldown > 0) {
            torchCooldown--;
            return;
        }
        BlockPos feet = player.getBlockPos();
        if (feet.getY() > 50 || !world.getBlockState(feet).isAir()) return;
        BlockPos below = feet.down();
        if (!world.getBlockState(below).isSolidBlock(world, below)) return;
        if (countInv("torch") < 1) return;
        if (!world.getBlockState(feet.up()).isAir()) return;
        world.setBlockState(feet, Blocks.TORCH.getDefaultState());
        consume("torch", 1);
        torchCooldown = 40;
    }

    /** Backs away from a creeper that is about to blow, instead of trading hits with it. */
    private boolean avoidCreeper(LivingEntity enemy) {
        if (!(enemy instanceof net.minecraft.entity.mob.CreeperEntity) || player.squaredDistanceTo(enemy) > 6.25) {
            return false;
        }
        double dx = player.getX() - enemy.getX();
        double dz = player.getZ() - enemy.getZ();
        double len = Math.max(0.001, Math.hypot(dx, dz));
        player.setVelocity(dx / len * 0.2, player.getVelocity().y, dz / len * 0.2);
        player.velocityModified = true;
        faceTo(enemy.getEyePos());
        return true;
    }

    private boolean needsFood() {
        return player.getHungerManager().getFoodLevel() <= 16;
    }

    private boolean hasRawFood() {
        return firstRawMeat() != null;
    }

    /** Raw meat id in the inventory, or null. */
    @Nullable
    private String firstRawMeat() {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            String id = Registries.ITEM.getId(inv.getStack(i).getItem()).getPath();
            if (id.equals("beef") || id.equals("porkchop") || id.equals("chicken") || id.equals("mutton")) return id;
        }
        return null;
    }

    private boolean hasFood() {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).get(DataComponentTypes.FOOD) != null) return true;
        }
        return false;
    }

    @Nullable
    private LivingEntity nearestAnimal(ServerWorld world, double range) {
        Box box = player.getBoundingBox().expand(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (AnimalEntity a : world.getEntitiesByClass(AnimalEntity.class, box, LivingEntity::isAlive)) {
            double d = player.squaredDistanceTo(a);
            if (d < bestDist) {
                bestDist = d;
                best = a;
            }
        }
        return best;
    }

    /** Walls the body into a 3x3 hut (two walls high, flat roof) using blocks from the inventory. */
    private String buildShelter() {
        ServerWorld world = player.getServerWorld();
        BlockPos base = player.getBlockPos();
        List<BlockPos> spots = new ArrayList<>();
        for (int y = 0; y <= 2; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean interior = dx == 0 && dz == 0 && y <= 1;
                    if (interior) continue;
                    BlockPos p = base.add(dx, y, dz);
                    if (world.getBlockState(p).isAir() || world.getBlockState(p).isReplaceable()) spots.add(p);
                }
            }
        }
        PlayerInventory inv = player.getInventory();
        int available = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock().getDefaultState().isFullCube(world, base)) {
                available += s.getCount();
            }
        }
        if (available < spots.size()) {
            return "I need about " + (spots.size() - available) + " more blocks for a shelter.";
        }
        int placed = 0;
        for (BlockPos p : spots) {
            Block block = null;
            for (int i = 0; i < inv.size() && block == null; i++) {
                ItemStack s = inv.getStack(i);
                if (s.getItem() instanceof BlockItem bi && bi.getBlock().getDefaultState().isFullCube(world, p)) {
                    block = bi.getBlock();
                    s.decrement(1);
                }
            }
            if (block == null) break;
            if (hasDropAt(world, p)) continue; // never entomb a dropped item inside a wall
            world.setBlockState(p, block.getDefaultState());
            placed++;
        }
        cancelPlan();
        nav.clear();
        setMode(Mode.STAY);
        player.swingHand(Hand.MAIN_HAND);
        return "Shelter built with " + placed + " blocks. I'll hold here. Tell me to follow when you want me out.";
    }

    // ------------------------------------------------------------------ helpers

    private boolean planTo(ServerWorld world, Pathfinder.Goal goal) {
        boolean canBridge = countInv("cobblestone") + countInv("dirt") > 0;
        long t0 = System.nanoTime();
        List<BlockPos> path = Pathfinder.find(world, player.getBlockPos(), goal, canBridge);
        dev.qynl.ollamaplayer.brain.Metrics.SHARED.path(System.nanoTime() - t0);
        if (path == null) {
            nav.clear();
            return false;
        }
        nav.setPath(path);
        return true;
    }

    /** Fills an air cell on the route with a block from the bag. Called by the navigator. */
    private boolean bridgeAt(BlockPos at) {
        ServerWorld world = player.getServerWorld();
        if (countInv("cobblestone") >= 1) {
            consume("cobblestone", 1);
            world.setBlockState(at, Blocks.COBBLESTONE.getDefaultState());
            return true;
        }
        if (countInv("dirt") >= 1) {
            consume("dirt", 1);
            world.setBlockState(at, Blocks.DIRT.getDefaultState());
            return true;
        }
        return false;
    }

    private void teleportNear(ServerPlayerEntity owner) {
        Vec3d o = owner.getPos();
        player.refreshPositionAndAngles(o.x + 1.5, o.y, o.z + 1.5, player.getYaw(), 0f);
        player.setVelocity(0, 0, 0);
        player.velocityModified = true;
        nav.clear();
    }

    private void stopHorizontal() {
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x * 0.3, v.y, v.z * 0.3);
        player.velocityModified = true;
    }

    private void faceTo(Vec3d target) {
        Vec3d e = player.getEyePos();
        double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
        double h = Math.hypot(dx, dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        player.setYaw(yaw);
        player.setHeadYaw(yaw);
        player.setPitch(pitch);
    }

    private static Vec3d eyeAt(BlockPos p) {
        return new Vec3d(p.getX() + 0.5, p.getY() + 1.62, p.getZ() + 0.5);
    }

    private void pickUpItems(ServerWorld world) {
        Box box = player.getBoundingBox().expand(3.0);
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            ItemStack s = e.getStack().copy();
            player.getInventory().insertStack(s);
            if (s.isEmpty()) e.discard();
            else e.setStack(s);
        }
    }

    /** Eats the best food in the bag: cooked food first, raw meat only when close to starving. */
    private boolean eatFood() {
        PlayerInventory inv = player.getInventory();
        boolean starving = player.getHungerManager().getFoodLevel() <= 6;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < inv.size(); i++) {
                ItemStack s = inv.getStack(i);
                FoodComponent food = s.get(DataComponentTypes.FOOD);
                if (food == null) continue;
                boolean raw = RAW_FOOD.contains(Registries.ITEM.getId(s.getItem()).getPath());
                if (pass == 0 && raw) continue;
                if (pass == 1 && !(raw && starving)) continue;
                player.getHungerManager().add(food.nutrition(), food.saturation());
                s.decrement(1);
                eatCooldown = 30;
                player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private static final Set<String> RAW_FOOD = Set.of(
            "beef", "porkchop", "chicken", "mutton", "rabbit", "cod", "salmon", "rotten_flesh");

    /** Sends the body to a block position on its own (used by tests and by the brain's move actions). */
    public void walkTo(BlockPos target) {
        recoverAt = target;
        recoverTimer = 400;
        nav.clear();
    }

    private void equipWeapon() {
        PlayerInventory inv = player.getInventory();
        int pick = -1;
        int bestScore = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            int score;
            if (s.getItem() instanceof SwordItem) score = 10 + Recipes.toolTier(s);
            else if (s.getItem() instanceof AxeItem) score = Recipes.toolTier(s);
            else continue;
            if (score > bestScore) {
                bestScore = score;
                pick = i;
            }
        }
        if (pick >= 0) inv.selectedSlot = pick;
    }

    private void equipTool(BlockState st) {
        PlayerInventory inv = player.getInventory();
        int best = -1;
        float bestSpeed = 0f;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty() || !s.isSuitableFor(st)) continue;
            float speed = s.getMiningSpeedMultiplier(st);
            // A tool about to break is the last choice, not the first: it would vanish mid-job.
            if (s.isDamageable() && s.getMaxDamage() - s.getDamage() <= 3) speed *= 0.01f;
            if (best < 0 || speed > bestSpeed) {
                best = i;
                bestSpeed = speed;
            }
        }
        if (best >= 0) inv.selectedSlot = best;
    }

    @Nullable
    private LivingEntity nearestEnemy(ServerWorld world, double range) {
        Box box = player.getBoundingBox().expand(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        // Raiders (pillagers, witches, vindicators) are not HostileEntity subclasses, so check both.
        for (MobEntity m : world.getEntitiesByClass(MobEntity.class, box, LivingEntity::isAlive)) {
            if (!(m instanceof HostileEntity || m instanceof RaiderEntity)) continue;
            double d = player.squaredDistanceTo(m);
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    private boolean isTarget(BlockState st) {
        if (mineTarget == null || st.isAir()) return false;
        String id = blockId(st);
        return id.equals(mineTarget) || id.endsWith("_" + mineTarget)
                || (mineTarget.endsWith("ore") && id.contains(mineTarget));
    }

    private static String blockId(BlockState st) {
        return Registries.BLOCK.getId(st.getBlock()).getPath();
    }

    static String normalizeTarget(String raw) {
        String t = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (t.endsWith("s") && !t.endsWith("ss") && t.length() > 3) t = t.substring(0, t.length() - 1);
        return switch (t) {
            case "wood", "tree", "lumber" -> "log";
            case "cobble" -> "cobblestone";
            case "iron" -> "iron_ore";
            case "coal" -> "coal_ore";
            case "diamond" -> "diamond_ore";
            case "gold" -> "gold_ore";
            case "copper" -> "copper_ore";
            case "redstone" -> "redstone_ore";
            case "emerald" -> "emerald_ore";
            default -> t;
        };
    }

    private String inventorySummary() {
        Map<String, Integer> counts = new TreeMap<>();
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            counts.merge(Registries.ITEM.getId(s.getItem()).getPath(), s.getCount(), Integer::sum);
        }
        if (counts.isEmpty()) return "empty";
        StringBuilder sb = new StringBuilder();
        counts.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(v).append(' ').append(k);
        });
        return sb.toString();
    }

    private String nearbyBlocks(ServerWorld world) {
        BlockPos c = player.getBlockPos();
        Map<String, Integer> counts = new TreeMap<>();
        for (BlockPos p : BlockPos.iterate(c.add(-5, -3, -5), c.add(5, 3, 5))) {
            BlockState st = world.getBlockState(p);
            if (st.isAir()) continue;
            counts.merge(blockId(st), 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> top = new ArrayList<>(counts.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(6, top.size()); i++) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(top.get(i).getKey());
        }
        return sb.length() == 0 ? "nothing" : sb.toString();
    }
}
