package dev.qynl.racers.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.block.RacerBlocks;

/** Every block the mod adds, plus a lookup table used by the structure builder. */
public final class ModBlocks {
    private static final Map<String, Block> BY_NAME = new LinkedHashMap<>();

    public static final Block ASPHALT = register("asphalt",
            new RacerBlocks.Asphalt(RacerBlocks.tarmac()));
    public static final Block ASPHALT_CRACKED = register("asphalt_cracked",
            new RacerBlocks.Asphalt(RacerBlocks.tarmac().strength(1.0F, 5.0F)));
    public static final Block ROAD_BLOCK = register("road_block",
            new RacerBlocks.RoadLine(RacerBlocks.tarmac()));
    public static final Block FINISH_LINE = register("finish_line",
            new RacerBlocks.FinishLine(RacerBlocks.tarmac().strength(1.4F, 8.0F)));
    public static final Block PIT_LANE = register("pit_lane",
            new RacerBlocks.PitLane(RacerBlocks.tarmac().sounds(BlockSoundGroup.STONE)));
    public static final Block KERB = register("kerb",
            new RacerBlocks.Kerb(RacerBlocks.decor()));
    public static final Block SAFETY_BARRIER = register("safety_barrier",
            new RacerBlocks.SecurityBarrier(RacerBlocks.barrier()));
    public static final Block START_GATE = register("start_gate",
            new RacerBlocks.StartGate(RacerBlocks.barrier().luminance(state -> 7)));
    public static final Block CHECKPOINT_MARKER = register("checkpoint_marker",
            new RacerBlocks.CheckpointMarker(RacerBlocks.decor()
                    .luminance(state -> state.get(RacerBlocks.CheckpointMarker.LIT) ? 9 : 2)));
    public static final Block STATION = register("station",
            new RacerBlocks.Station(RacerBlocks.decor().luminance(state -> 5)));
    public static final Block BOOST_PAD = register("boost_pad",
            new RacerBlocks.BoostPad(RacerBlocks.tarmac()
                    .sounds(BlockSoundGroup.METAL).luminance(state -> 10)));
    public static final Block FAKE_ICE = register("fake_ice",
            new RacerBlocks.FakeIce(AbstractBlock.Settings.create().strength(0.6F)
                    .sounds(BlockSoundGroup.GLASS).slipperiness(0.989F)
                    .nonOpaque().mapColor(net.minecraft.block.MapColor.LIGHT_BLUE)));
    public static final Block FAKE_SLIME = register("fake_slime",
            new RacerBlocks.FakeSlime(AbstractBlock.Settings.create().strength(0.4F)
                    .sounds(BlockSoundGroup.SLIME).slipperiness(0.8F)
                    .nonOpaque().mapColor(net.minecraft.block.MapColor.LIME)));

    private static Block register(String name, Block block) {
        BY_NAME.put(name, block);
        return Registry.register(Registries.BLOCK, FabricRacers.id(name), block);
    }

    /** Returns the mod block registered under {@code name}, or null. */
    public static Block byName(String name) {
        return BY_NAME.get(name);
    }

    public static Map<String, Block> all() {
        return BY_NAME;
    }

    public static void init() {
        // class-loading side effect
    }
}
