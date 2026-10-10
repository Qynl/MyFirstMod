package dev.qynl.ollamaplayer.test;

import dev.qynl.ollamaplayer.companion.Companion;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless end-to-end check run by `gradle runGametest` in CI:
 * a companion walks to a log on open ground, breaks it, and ends up holding it.
 */
public class NovaGameTest implements FabricGameTest {

    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1500)
    public void companionMinesLogAndKeepsIt(TestContext ctx) {
        ServerWorld world = ctx.getWorld();
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                ctx.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
            }
        }
        BlockPos logRel = new BlockPos(7, 1, 5);
        ctx.setBlockState(logRel, Blocks.OAK_LOG.getDefaultState());
        BlockPos logAbs = BlockPos.ofFloored(ctx.getAbsolute(Vec3d.ofCenter(logRel)));

        Vec3d start = ctx.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(2, 1, 5)));
        FakePlayer body = Companion.createPlayer(world, start, "NovaTest");
        Companion companion = new Companion(body, msg -> { });
        companion.startMining("log", 1);

        AtomicInteger ticks = new AtomicInteger();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (ticks.get() < 0) return; // finished already
            int t = ticks.incrementAndGet();
            companion.tick(null);
            if (t % 100 == 0) System.out.println("NOVA_DEBUG t=" + t + " " + companion.debugState());

            boolean logGone = world.getBlockState(logAbs).isAir();
            boolean holdingLog = false;
            for (int i = 0; i < body.getInventory().size(); i++) {
                ItemStack s = body.getInventory().getStack(i);
                if (s.isOf(Items.OAK_LOG)) holdingLog = true;
            }
            if (logGone && holdingLog) {
                ticks.set(-1);
                ctx.complete();
            } else if (t > 1400) {
                ticks.set(-1);
                ctx.throwGameTestException("Companion did not mine+collect the log. logGone=" + logGone
                        + " holdingLog=" + holdingLog + " pos=" + body.getPos());
            }
        });
    }
}
