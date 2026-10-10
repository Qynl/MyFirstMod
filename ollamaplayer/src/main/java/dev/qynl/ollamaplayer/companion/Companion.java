package dev.qynl.ollamaplayer.companion;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    private static final String[] RARE = {"diamond", "emerald", "ancient_debris", "gold_ore", "iron_ore", "lapis", "redstone_ore", "coal_ore"};

    private final FakePlayer player;
    private final Consumer<String> say;
    private final Navigator nav = new Navigator();

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

    public Companion(FakePlayer player, Consumer<String> say) {
        this.player = player;
        this.say = say;
    }

    /** Creates a fresh fake player with a unique profile (avoids FakePlayer's instance cache). */
    public static FakePlayer createPlayer(ServerWorld world, Vec3d pos, String name) {
        FakePlayer p = FakePlayer.get(world, new GameProfile(UUID.randomUUID(), name));
        p.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0f, 0f);
        world.spawnEntity(p);
        return p;
    }

    public FakePlayer player() {
        return player;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode m) {
        mode = m;
        nav.clear();
    }

    // ------------------------------------------------------------------ tick

    public void tick(@Nullable ServerPlayerEntity owner) {
        ServerWorld world = player.getServerWorld();
        if (replanTimer > 0) replanTimer--;
        if (eatCooldown > 0) eatCooldown--;
        if (hurtCooldown > 0) hurtCooldown--;

        pickUpItems(world);

        float hp = player.getHealth();
        if (hp < 7 && lastHealth >= 7 && hurtCooldown == 0) {
            say.accept("I'm badly hurt!");
            hurtCooldown = 200;
        }
        lastHealth = hp;

        if (player.getHungerManager().getFoodLevel() <= 14 && eatCooldown == 0) {
            eatFood();
        }

        LivingEntity enemy = nearestEnemy(world, 16);
        if (enemy != null && !sawEnemy && player.distanceTo(enemy) < 10) {
            say.accept("Heads up, " + Registries.ENTITY_TYPE.getId(enemy.getType()).getPath().replace('_', ' ') + "!");
        }
        sawEnemy = enemy != null;

        if (enemy != null) {
            wasFighting = true;
            if (hp <= 7 && owner != null) {
                // Too hurt to trade hits: retreat toward the owner.
                planTo(world, ownerGoal(owner));
                nav.tick(player, 0.18);
            } else {
                fight(enemy, world);
            }
            return;
        }
        if (wasFighting) {
            wasFighting = false;
            nav.clear();
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
            case "follow" -> setMode(Mode.FOLLOW);
            case "stay" -> setMode(Mode.STAY);
            case "mine" -> {
                if (target == null || target.isBlank()) return "Mine what, exactly?";
                startMining(target, count);
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

    public void startMining(String target, int count) {
        mineTarget = normalizeTarget(target);
        mineWanted = Math.max(1, Math.min(64, count <= 0 ? 8 : count));
        mined = 0;
        mineFailures = 0;
        mineBlock = null;
        mineProgress = 0;
        badBlocks.clear();
        setMode(Mode.MINE);
    }

    public String statusLine() {
        String what = switch (mode) {
            case FOLLOW -> "following you";
            case STAY -> "guarding this spot";
            case MINE -> "mining " + mineTarget + " (" + mined + "/" + mineWanted + ")";
        };
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

    private void fight(LivingEntity enemy, ServerWorld world) {
        equipWeapon();
        double d = player.distanceTo(enemy);
        faceTo(enemy.getEyePos());
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
            if (player.getAttackCooldownProgress(0.5F) >= 1.0F) {
                player.attack(enemy);
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private void mine(ServerWorld world) {
        if (mineBlock == null || !isTarget(world.getBlockState(mineBlock))) {
            mineBlock = null;
            mineProgress = 0;
            if (!findTarget(world)) {
                finishMining();
                return;
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
            world.breakBlock(mineBlock, true, player);
            mined++;
            mineBlock = null;
            mineProgress = 0;
            for (String rare : RARE) {
                if (id.contains(rare)) {
                    say.accept("Found " + id.replace('_', ' ') + "!");
                    break;
                }
            }
            if (mined >= mineWanted) finishMining();
        }
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
        for (BlockPos p : BlockPos.iterate(c.add(-16, -6, -16), c.add(16, 6, 16))) {
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

    // ------------------------------------------------------------------ helpers

    private boolean planTo(ServerWorld world, Pathfinder.Goal goal) {
        List<BlockPos> path = Pathfinder.find(world, player.getBlockPos(), goal);
        if (path == null) {
            nav.clear();
            return false;
        }
        nav.setPath(path);
        return true;
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
        Box box = player.getBoundingBox().expand(1.5);
        for (ItemEntity e : world.getEntitiesByClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            ItemStack s = e.getStack().copy();
            player.getInventory().insertStack(s);
            if (s.isEmpty()) e.discard();
            else e.setStack(s);
        }
    }

    private boolean eatFood() {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            FoodComponent food = s.get(DataComponentTypes.FOOD);
            if (food != null) {
                player.getHungerManager().add(food.nutrition(), food.saturation());
                s.decrement(1);
                eatCooldown = 30;
                player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private void equipWeapon() {
        PlayerInventory inv = player.getInventory();
        int pick = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getStack(i);
            if (s.getItem() instanceof SwordItem) {
                pick = i;
                break;
            }
            if (pick < 0 && s.getItem() instanceof AxeItem) pick = i;
        }
        if (pick >= 0) inv.selectedSlot = pick;
    }

    private void equipTool(BlockState st) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isSuitableFor(st)) {
                inv.selectedSlot = i;
                return;
            }
        }
    }

    @Nullable
    private LivingEntity nearestEnemy(ServerWorld world, double range) {
        Box box = player.getBoundingBox().expand(range);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (HostileEntity h : world.getEntitiesByClass(HostileEntity.class, box, LivingEntity::isAlive)) {
            double d = player.squaredDistanceTo(h);
            if (d < bestDist) {
                bestDist = d;
                best = h;
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
