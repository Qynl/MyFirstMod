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
        BlockPos[] ores = {new BlockPos(6, 1, 5), new BlockPos(8, 1, 3), new BlockPos(9, 1, 8)};
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

    private static void floor(TestContext ctx) {
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
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
