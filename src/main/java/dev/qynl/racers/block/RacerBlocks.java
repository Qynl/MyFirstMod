package dev.qynl.racers.block;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * All of the mod's building blocks.
 *
 * <p>Most of them are plain decorative blocks used to paint a circuit (asphalt,
 * kerbs, barriers, pit lane, ...). The interesting ones implement the little
 * bits of gameplay: checkpoint markers light up while a race is running, the
 * start gantry is animated, boost pads launch cars forward, and fake ice/slime
 * behave exactly like their names suggest.
 */
public final class RacerBlocks {
    private RacerBlocks() {
    }

    public enum KerbColor implements StringIdentifiable {
        RED("red"),
        WHITE("white");

        private final String name;

        KerbColor(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    /** Plain tarmac. */
    public static class Asphalt extends Block {
        public Asphalt(Settings settings) {
            super(settings);
        }
    }

    /** Tarmac with a painted centre line. */
    public static class RoadLine extends Block {
        public RoadLine(Settings settings) {
            super(settings);
        }
    }

    /** Checkered start/finish line. */
    public static class FinishLine extends Block {
        public FinishLine(Settings settings) {
            super(settings);
        }
    }

    /** Pit lane tarmac - no lap timing, but full grip. */
    public static class PitLane extends Block {
        public PitLane(Settings settings) {
            super(settings);
        }
    }

    /** Red/white kerb. */
    public static class Kerb extends Block {
        public static final EnumProperty<KerbColor> COLOR =
                EnumProperty.of("color", KerbColor.class);

        public Kerb(Settings settings) {
            super(settings);
            setDefaultState(getDefaultState().with(COLOR, KerbColor.RED));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(COLOR);
        }
    }

    /** Armco / concrete safety barrier. */
    public static class SecurityBarrier extends Block {
        public SecurityBarrier(Settings settings) {
            super(settings);
        }
    }

    /** Animated start gantry block with its own lights. */
    public static class StartGate extends Block {
        public StartGate(Settings settings) {
            super(settings);
        }
    }

    /** Checkpoint light strip. Lights up while a race is live. */
    public static class CheckpointMarker extends Block {
        public static final BooleanProperty LIT = Properties.LIT;

        public CheckpointMarker(Settings settings) {
            super(settings);
            setDefaultState(getDefaultState().with(LIT, false));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            builder.add(LIT);
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            return getDefaultState().with(LIT, false);
        }

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
        }
    }

    /** Pit box marker: shows where a car can be serviced. */
    public static class Station extends Block {
        public Station(Settings settings) {
            super(settings);
        }
    }

    /** Boost pad: gives any car that drives over it a burst of speed. */
    public static class BoostPad extends Block {
        public BoostPad(Settings settings) {
            super(settings);
        }

        @Override
        public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
            super.onSteppedOn(world, pos, state, entity);
        }

        @Override
        public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
            if (!world.isClient && entity instanceof dev.qynl.racers.car.CarEntity car) {
                car.hitBoostPad(pos);
            }
        }
    }

    /** Decor only - slippery in looks and in physics. */
    public static class FakeIce extends Block {
        public FakeIce(Settings settings) {
            super(settings);
        }
    }

    /** Bouncy decor. */
    public static class FakeSlime extends Block {
        public FakeSlime(Settings settings) {
            super(settings);
        }

        @Override
        public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
            if (entity instanceof dev.qynl.racers.car.CarEntity car) {
                car.hitBounceBlock(0.55);
                return;
            }
            super.onLandedUpon(world, state, pos, entity, fallDistance);
        }
    }

    // -- settings helpers ---------------------------------------------------
    public static Settings tarmac() {
        return Block.Settings.create().strength(1.2F, 6.0F)
                .sounds(BlockSoundGroup.STONE).mapColor(net.minecraft.block.MapColor.BLACK);
    }

    public static Settings barrier() {
        return Block.Settings.create().strength(2.5F, 12.0F).requiresTool()
                .sounds(BlockSoundGroup.IRON);
    }

    public static Settings decor() {
        return Block.Settings.create().strength(1.0F, 6.0F).sounds(BlockSoundGroup.STONE);
    }
}
