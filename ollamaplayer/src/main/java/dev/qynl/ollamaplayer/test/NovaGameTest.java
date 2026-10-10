package dev.qynl.ollamaplayer.test;

import dev.qynl.ollamaplayer.companion.Companion;
import dev.qynl.ollamaplayer.companion.NovaBody;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Headless end-to-end checks run by `gradle runGameTest` in CI.
 * Each test spawns a companion on a stone floor and checks what it ends up doing.
 */
public class NovaGameTest implements FabricGameTest {

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1500)
    public void companionMinesLogAndKeepsIt(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        BlockPos logRel = new BlockPos(7, 1, 5);
        ctx.setBlockState(logRel, Blocks.OAK_LOG.getDefaultState());
        BlockPos logAbs = BlockPos.ofFloored(ctx.getAbsolute(Vec3d.ofCenter(logRel)));

        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaTest");
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        companion.startMining("log", 1);

        run(ctx, companion, 1400, () -> world.getBlockState(logAbs).isAir() && count(body, Items.OAK_LOG) >= 1,
                () -> "Companion did not mine+collect the log. pos=" + body.getPos());
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 600)
    public void craftsCraftingTableFromLogs(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaCraft");
        body.getInventory().insertStack(new ItemStack(Items.OAK_LOG, 2));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        String reply = companion.craftGoal("crafting_table", 1);
        System.out.println("NOVA_REPLY " + reply);
        if (reply.startsWith("I can't")) ctx.throwGameTestException(reply);

        run(ctx, companion, 500, () -> count(body, Items.CRAFTING_TABLE) >= 1,
                () -> "Companion did not craft a crafting table from logs. inv=" + body.getInventory().size());
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 600)
    public void craftsIronPickaxeAtNearbyTable(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        ctx.setBlockState(new BlockPos(3, 1, 5), Blocks.CRAFTING_TABLE.getDefaultState());
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaSmith");
        body.getInventory().insertStack(new ItemStack(Items.IRON_INGOT, 3));
        body.getInventory().insertStack(new ItemStack(Items.STICK, 2));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        String reply = companion.craftGoal("iron_pickaxe", 1);
        System.out.println("NOVA_REPLY " + reply);
        if (reply.startsWith("I can't")) ctx.throwGameTestException(reply);

        run(ctx, companion, 500, () -> count(body, Items.IRON_PICKAXE) >= 1,
                () -> "Companion did not craft an iron pickaxe at the table.");
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void wearsBetterArmorFromInventory(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaArmor");
        body.getInventory().insertStack(new ItemStack(Items.IRON_HELMET, 1));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        run(ctx, companion, 120, () -> body.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.IRON_HELMET),
                () -> "Companion did not put on the iron helmet.");
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1200)
    public void smeltsIronWithPlacedFurnace(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaSmelt");
        body.getInventory().insertStack(new ItemStack(Items.RAW_IRON, 2));
        body.getInventory().insertStack(new ItemStack(Items.COAL, 2));
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 8));
        body.getInventory().insertStack(new ItemStack(Items.OAK_PLANKS, 4));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        String reply = companion.craftGoal("iron_ingot", 2);
        System.out.println("NOVA_REPLY " + reply);
        if (reply.startsWith("I can't")) ctx.throwGameTestException(reply);

        run(ctx, companion, 900, () -> count(body, Items.IRON_INGOT) >= 2,
                () -> "Companion did not smelt two iron ingots with a placed furnace.");
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 900)
    public void minesIronOreWithStonePickaxe(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        BlockPos oreRel = new BlockPos(6, 1, 5);
        ctx.setBlockState(oreRel, Blocks.IRON_ORE.getDefaultState());
        BlockPos oreAbs = BlockPos.ofFloored(ctx.getAbsolute(Vec3d.ofCenter(oreRel)));
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaOre");
        body.getInventory().insertStack(new ItemStack(Items.STONE_PICKAXE, 1));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        companion.startMining("iron_ore", 1);

        run(ctx, companion, 800, () -> world.getBlockState(oreAbs).isAir() && count(body, Items.RAW_IRON) >= 1,
                () -> "Companion did not mine the iron ore with its stone pickaxe.");
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 4000)
    public void fullLoopIronPickaxeFromOre(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        BlockPos[] ores = {new BlockPos(6, 1, 5), new BlockPos(8, 1, 3), new BlockPos(9, 1, 8),
                new BlockPos(5, 1, 9), new BlockPos(4, 1, 2)};
        for (BlockPos o : ores) ctx.setBlockState(o, Blocks.IRON_ORE.getDefaultState());
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaLoop");
        body.getInventory().insertStack(new ItemStack(Items.STONE_PICKAXE, 1));
        body.getInventory().insertStack(new ItemStack(Items.COAL, 3));
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 8));
        body.getInventory().insertStack(new ItemStack(Items.OAK_PLANKS, 8));
        body.getInventory().insertStack(new ItemStack(Items.STICK, 2));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        String reply = companion.craftGoal("iron_pickaxe", 1);
        System.out.println("NOVA_REPLY " + reply);
        if (reply.startsWith("I can't")) ctx.throwGameTestException(reply);

        run(ctx, companion, 3800, () -> count(body, Items.IRON_PICKAXE) >= 1,
                () -> "Companion did not finish the iron pickaxe loop. status=" + companion.statusLine()
                        + " | " + companion.describe(null).replace('\n', ' ') + " | " + companion.debugDrops());
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void buildsShelterAroundItself(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(5, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaHut");
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 40));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        BlockPos base = body.getBlockPos();

        String reply = companion.command("shelter", null, 0);
        System.out.println("NOVA_REPLY " + reply);

        boolean walls = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                walls &= world.getBlockState(base.add(dx, 0, dz)).isOf(Blocks.COBBLESTONE);
                walls &= world.getBlockState(base.add(dx, 1, dz)).isOf(Blocks.COBBLESTONE);
            }
            walls &= world.getBlockState(base.add(dx, 2, 0)).isOf(Blocks.COBBLESTONE);
        }
        if (!walls) ctx.throwGameTestException("Shelter walls are missing. reply=" + reply);
        if (companion.mode() != Companion.Mode.STAY) ctx.throwGameTestException("Companion should hold still in the shelter.");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 600)
    public void swimsOutOfWaterPool(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        for (int x = 1; x <= 3; x++) {
            for (int z = 4; z <= 6; z++) {
                ctx.setBlockState(new BlockPos(x, 1, z), Blocks.WATER.getDefaultState());
            }
        }
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaSwim");
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        BlockPos dry = BlockPos.ofFloored(ctx.getAbsolute(Vec3d.ofCenter(new BlockPos(6, 1, 5))));
        companion.walkTo(dry);

        run(ctx, companion, 500, () -> body.getPos().squaredDistanceTo(Vec3d.ofCenter(dry)) < 4.0,
                () -> "Companion did not swim out of the pool. pos=" + body.getPos());
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1500)
    public void cooksRawBeefInsteadOfEatingItRaw(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaCook");
        body.getInventory().insertStack(new ItemStack(Items.BEEF, 1));
        body.getInventory().insertStack(new ItemStack(Items.COAL, 2));
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 8));
        body.getInventory().insertStack(new ItemStack(Items.OAK_PLANKS, 4));
        body.getHungerManager().setFoodLevel(8);
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        run(ctx, companion, 1200, () -> count(body, Items.COOKED_BEEF) >= 1,
                () -> "Companion did not cook its beef. food=" + body.getHungerManager().getFoodLevel()
                        + " raw=" + count(body, Items.BEEF));
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 600)
    public void bridgesAcrossAGap(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        // A two-block gap across the whole floor: the only way over is to lay blocks.
        for (int z = 0; z <= 16; z++) {
            ctx.setBlockState(new BlockPos(5, 0, z), Blocks.AIR.getDefaultState());
            ctx.setBlockState(new BlockPos(6, 0, z), Blocks.AIR.getDefaultState());
        }
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaBridge");
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 8));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        Vec3d goal = ctx.getAbsolute(new Vec3d(9.5, 1, 5.5));
        companion.walkTo(BlockPos.ofFloored(goal));

        // walkTo counts arrival within about 3 blocks; from x=2.5 that can only be reached by crossing the gap.
        run(ctx, companion, 500, () -> Math.hypot(body.getX() - goal.x, body.getZ() - goal.z) < 3.5,
                () -> "Companion did not bridge the gap. rel=" + relative(ctx, body.getPos()) + " goal=" + relative(ctx, goal)
                        + " status=" + companion.statusLine() + " inv=" + count(body, Items.COBBLESTONE));
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 20)
    public void keepsBagAcrossSave(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaSave");
        body.getInventory().insertStack(new ItemStack(Items.DIAMOND, 3));
        body.getInventory().setStack(39, new ItemStack(Items.IRON_HELMET));
        Companion companion = new Companion(body, msg -> { });
        java.util.UUID owner = java.util.UUID.randomUUID();

        dev.qynl.ollamaplayer.companion.CompanionStore.save(world.getServer(), companion, owner);
        net.minecraft.nbt.NbtCompound saved = dev.qynl.ollamaplayer.companion.CompanionStore.read(world.getServer());
        dev.qynl.ollamaplayer.companion.CompanionStore.delete(world.getServer());
        if (saved == null) ctx.throwGameTestException("Nova's state was not written.");

        NovaBody fresh = Companion.createPlayer(world, start, "NovaSaveB");
        dev.qynl.ollamaplayer.companion.CompanionStore.restoreItems(world.getServer(), saved, fresh);
        if (count(fresh, Items.DIAMOND) != 3) ctx.throwGameTestException("Bag did not come back: diamonds=" + count(fresh, Items.DIAMOND));
        if (!fresh.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.IRON_HELMET) && !fresh.getInventory().getStack(39).isOf(Items.IRON_HELMET)) {
            ctx.throwGameTestException("Armor did not come back.");
        }
        if (!owner.equals(saved.getUuid("owner"))) ctx.throwGameTestException("Owner was not saved.");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 600)
    public void pillarsUpTwoBlocks(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        // Nothing to step onto: the goal is two blocks up in open air, so Nova has to lay its own floor.
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaPillar");
        body.getInventory().insertStack(new ItemStack(Items.COBBLESTONE, 8));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));
        Vec3d goal = ctx.getAbsolute(new Vec3d(2.5, 5, 5.5));
        companion.walkTo(BlockPos.ofFloored(goal));

        run(ctx, companion, 500, () -> body.getY() > ctx.getAbsolute(Vec3d.ZERO).y + 2.9,
                () -> "Companion did not pillar up. rel=" + relative(ctx, body.getPos())
                        + " status=" + companion.statusLine() + " cobble=" + count(body, Items.COBBLESTONE));
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
    public void movesSwordIntoHotbar(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaHotbar");
        body.getInventory().setStack(25, new ItemStack(Items.DIAMOND_SWORD));
        Companion companion = new Companion(body, msg -> { });

        run(ctx, companion, 150, () -> {
            for (int h = 0; h < 9; h++) {
                if (body.getInventory().getStack(h).isOf(Items.DIAMOND_SWORD)) return true;
            }
            return false;
        }, () -> "Companion never moved its sword into the hotbar.");
    }

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 400)
    public void stashesBulkBlocksWhenFull(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        floor(ctx);
        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        NovaBody body = Companion.createPlayer(world, start, "NovaStash");
        for (int i = 0; i < 36; i++) body.getInventory().setStack(i, new ItemStack(Items.DIORITE, 64));
        Companion companion = new Companion(body, msg -> System.out.println("NOVA_SAY " + msg));

        // run() is asynchronous, so the take happens inside the done check, on the tick the stash is recorded,
        // while the body is still next to the chest.
        AtomicInteger taken = new AtomicInteger(-1);
        run(ctx, companion, 300, () -> {
            if (companion.stashPos() == null) return false;
            if (taken.get() < 0) taken.set(companion.takeFromStash("diorite", 1));
            if (taken.get() == 0) {
                ctx.throwGameTestException("Companion could not take diorite back from its chest.");
            }
            return true;
        }, () -> "Companion did not stash bulk blocks. diorite=" + count(body, Items.DIORITE));
    }

    /** Pure logic checks: the keyword fallback and the model-reply parser route requests to the right action. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 5)
    public void routesRequestsToActions(TestContext ctx) {
        String[][] cases = {
                {"make an iron pickaxe", "craft", "iron_pickaxe"},
                {"build a shelter", "shelter", null},
                {"gear up", "gear", null},
                {"give me everything", "give", null},
                {"get me 8 cobblestone", "mine", "cobblestone"},
                {"give me 5 iron", "mine", "iron_ore"},
                {"follow me", "follow", null},
                {"stay here", "stay", null},
                {"what are you doing", "status", null},
        };
        for (String[] c : cases) {
            dev.qynl.ollamaplayer.brain.Reply r = dev.qynl.ollamaplayer.brain.Heuristics.reply(c[0]);
            if (!c[1].equals(r.action()) || (c[2] != null && !c[2].equals(r.target()))) {
                ctx.throwGameTestException("'" + c[0] + "' routed to " + r.action() + "/" + r.target() + ", expected " + c[1] + "/" + c[2]);
            }
        }
        dev.qynl.ollamaplayer.brain.Reply ok = dev.qynl.ollamaplayer.brain.Reply.parse(
                "{\"say\":\"On it\",\"action\":\"craft\",\"target\":\"iron_pickaxe\",\"count\":1}");
        if (!"craft".equals(ok.action()) || !"iron_pickaxe".equals(ok.target()) || ok.count() != 1) {
            ctx.throwGameTestException("valid JSON reply parsed wrong: " + ok);
        }
        dev.qynl.ollamaplayer.brain.Reply bogus = dev.qynl.ollamaplayer.brain.Reply.parse(
                "{\"say\":\"hi\",\"action\":\"fly\"}");
        if (!"none".equals(bogus.action())) ctx.throwGameTestException("unknown action was not rejected: " + bogus);
        dev.qynl.ollamaplayer.brain.Reply plain = dev.qynl.ollamaplayer.brain.Reply.parse("Sure thing, friend.");
        if (!"none".equals(plain.action()) || !plain.say().startsWith("Sure")) {
            ctx.throwGameTestException("plain-text reply was not kept as speech: " + plain);
        }
        ctx.complete();
    }

    private static String relative(TestContext ctx, Vec3d abs) {
        Vec3d o = ctx.getAbsolute(Vec3d.ZERO);
        return String.format(java.util.Locale.ROOT, "(%.2f, %.2f, %.2f)", abs.x - o.x, abs.y - o.y, abs.z - o.z);
    }

    private static void floor(TestContext ctx) {
        for (int x = 0; x <= 16; x++) {
            for (int z = 0; z <= 16; z++) {
                ctx.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
            }
        }
    }

    private static int count(NovaBody body, Item item) {
        int n = 0;
        for (int i = 0; i < body.getInventory().size(); i++) {
            ItemStack s = body.getInventory().getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    /** Ticks the companion until {@code done} holds, then completes the test; fails with {@code failMsg} at the limit. */
    private static void run(TestContext ctx, Companion companion, int limit, BooleanSupplier done,
                            java.util.function.Supplier<String> failMsg) {
        AtomicInteger ticks = new AtomicInteger();
        AtomicBoolean finished = new AtomicBoolean();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished.get()) return;
            int t = ticks.incrementAndGet();
            companion.tick(null);
            if (done.getAsBoolean()) {
                finished.set(true);
                ctx.complete();
            } else if (t > limit) {
                finished.set(true);
                ctx.throwGameTestException(failMsg.get() + " (after " + t + " ticks)");
            }
        });
    }
}
