package dev.qynl.racers.track;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import dev.qynl.racers.FabricRacers;

/**
 * Reads and writes structure files in the vanilla layout (palette + block list).
 *
 * <p>Deliberately hand rolled: the mod needs exact control over palette
 * remapping (repainting a whole circuit) and it must work with the files the
 * Python track generator produces, plus the ones players create by driving.
 */
public final class TrackStructure {
    /** Local Y of the road surface blocks inside a track structure. */
    public static final int ROAD_Y = 1;
    /** Local Y where cars stand (the structure is placed anchor + this). */
    public static final int CAR_Y = 2;

    private final int[] size;
    private final List<Entry> entries;
    private final NbtCompound raw;

    public record Entry(int x, int y, int z, String name, Map<String, String> properties) {
    }

    private TrackStructure(int[] size, List<Entry> entries, NbtCompound raw) {
        this.size = size;
        this.entries = entries;
        this.raw = raw;
    }

    public int[] size() {
        return size;
    }

    public List<Entry> entries() {
        return entries;
    }

    public NbtCompound rawNbt() {
        return raw;
    }

    public static TrackStructure read(InputStream stream) throws IOException {
        NbtCompound root = NbtIo.readCompressed(stream, NbtSizeTracker.ofUnlimitedBytes());
        return fromNbt(root);
    }

    @SuppressWarnings("unchecked")
    public static TrackStructure fromNbt(NbtCompound root) {
        NbtList sizeList = root.getList("size", NbtElement.INT_TYPE);
        int[] size = new int[]{
                sizeList.size() > 0 ? sizeList.getInt(0) : 1,
                sizeList.size() > 1 ? sizeList.getInt(1) : 1,
                sizeList.size() > 2 ? sizeList.getInt(2) : 1};

        List<Map<String, String>> palette = new ArrayList<>();
        NbtList paletteList = root.getList("palette", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < paletteList.size(); i++) {
            NbtCompound entry = paletteList.getCompound(i);
            Map<String, String> props = new HashMap<>();
            if (entry.contains("Properties")) {
                NbtCompound properties = entry.getCompound("Properties");
                for (String key : properties.getKeys()) {
                    props.put(key, properties.getString(key));
                }
            }
            props.put("__name", entry.getString("Name"));
            palette.add(props);
        }

        List<Entry> entries = new ArrayList<>();
        NbtList blocks = root.getList("blocks", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < blocks.size(); i++) {
            NbtCompound block = blocks.getCompound(i);
            NbtList pos = block.getList("pos", NbtElement.INT_TYPE);
            int state = block.getInt("state");
            if (state < 0 || state >= palette.size()) {
                continue;
            }
            Map<String, String> props = palette.get(state);
            String name = props.get("__name");
            Map<String, String> clean = new HashMap<>(props);
            clean.remove("__name");
            entries.add(new Entry(pos.getInt(0), pos.getInt(1), pos.getInt(2), name, clean));
        }
        return new TrackStructure(size, entries, root);
    }

    /** Turns "minecraft:stone_slab" + {"type":"top"} into a real block state. */
    public static BlockState parseState(String name, Map<String, String> properties) {
        Identifier identifier = Identifier.tryParse(name);
        if (identifier == null || !Registries.BLOCK.containsId(identifier)) {
            return null;
        }
        Block block = Registries.BLOCK.get(identifier);
        BlockState state = block.getDefaultState();
        if (properties != null) {
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                Property<?> property = state.getBlock().getStateManager().getProperty(entry.getKey());
                if (property == null) {
                    continue;
                }
                state = withProperty(state, property, entry.getValue());
            }
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withProperty(BlockState state, Property property, String value) {
        Optional<?> parsed = property.parse(value);
        if (parsed.isEmpty()) {
            return state;
        }
        return state.with(property, (Comparable) parsed.get());
    }

    /**
     * Places the whole structure into the world.
     *
     * @param mirror the block-symmetry handler, or null for a straight build
     * @param progress receives (placed, total) so the caller can report progress
     */
    public int place(World world, BlockPos anchor, int rotation, BlockMirror mirror,
                     java.util.function.BiConsumer<Integer, Integer> progress) {
        double[] shift = shiftFor(rotation, size);
        int placed = 0;
        int total = entries.size();
        for (Entry entry : entries) {
            double[] rotated = rotate(entry.x(), entry.z(), rotation);
            int x = (int) Math.round(anchor.getX() + rotated[0] + shift[0]);
            int y = anchor.getY() + entry.y();
            int z = (int) Math.round(anchor.getZ() + rotated[1] + shift[1]);
            String name = entry.name();
            Map<String, String> props = entry.properties();
            if (mirror != null) {
                String mapped = mirror.map(name);
                if (mapped != null) {
                    name = mapped;
                }
                props = mirror.mapProperties(name, props);
            }
            BlockState state = parseState(name, props);
            if (state == null) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
            placed++;
            if (progress != null && placed % 512 == 0) {
                progress.accept(placed, total);
            }
        }
        if (progress != null) {
            progress.accept(placed, total);
        }
        return placed;
    }

    /** Repaint hook: swaps block names or properties (e.g. red wool -> blue wool). */
    public interface BlockMirror {
        String map(String blockName);

        default Map<String, String> mapProperties(String blockName, Map<String, String> properties) {
            return properties;
        }
    }

    public static double[] rotate(double x, double z, int rotation) {
        return switch (((rotation % 360) + 360) % 360) {
            case 90 -> new double[]{-z, x};
            case 180 -> new double[]{-x, -z};
            case 270 -> new double[]{z, -x};
            default -> new double[]{x, z};
        };
    }

    public static double[] shiftFor(int rotation, int[] size) {
        int sx = Math.max(1, size[0]);
        int sz = Math.max(1, size[2]);
        return switch (((rotation % 360) + 360) % 360) {
            case 90 -> new double[]{sz - 1.0, 0.0};
            case 180 -> new double[]{sx - 1.0, sz - 1.0};
            case 270 -> new double[]{0.0, sx - 1.0};
            default -> new double[]{0.0, 0.0};
        };
    }

    /** Writes the raw structure back out (verbatim, keeps custom tags). */
    public void writeCompressed(java.io.OutputStream out) throws IOException {
        NbtIo.writeCompressed(raw, out);
    }

    /**
     * Removes a placed copy: only blocks that still match the structure are
     * cleared, so player edits and buildings next to a track survive.
     */
    public int clear(World world, BlockPos anchor, int rotation) {
        double[] shift = shiftFor(rotation, size);
        int cleared = 0;
        for (Entry entry : entries) {
            double[] rotated = rotate(entry.x(), entry.z(), rotation);
            BlockPos pos = new BlockPos((int) Math.round(anchor.getX() + rotated[0] + shift[0]),
                    anchor.getY() + entry.y(),
                    (int) Math.round(anchor.getZ() + rotated[1] + shift[1]));
            BlockState current = world.getBlockState(pos);
            Identifier currentId = Registries.BLOCK.getId(current.getBlock());
            if (currentId.toString().equals(entry.name())
                    || currentId.getNamespace().equals("fabricracers")) {
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_LISTENERS);
                cleared++;
            }
        }
        return cleared;
    }

    /** Loads a structure from a world folder first, then from the jar. */
    public static TrackStructure load(java.nio.file.Path worldDir, String id) {
        java.nio.file.Path file = worldDir.resolve(id + ".nbt");
        try {
            if (java.nio.file.Files.exists(file)) {
                try (InputStream in = java.nio.file.Files.newInputStream(file)) {
                    return read(in);
                }
            }
            InputStream in = TrackStructure.class.getResourceAsStream(
                    "/data/fabricracers/structure/tracks/" + id + ".nbt");
            if (in != null) {
                try (InputStream stream = in) {
                    return read(stream);
                }
            }
        } catch (IOException exception) {
            FabricRacers.LOGGER.error("Could not read structure for track {}", id, exception);
        }
        return null;
    }

    /** Writes a structure file (used by the drive-in track recorder). */
    public static void write(java.nio.file.Path file, StructureWriter writer) throws IOException {
        java.nio.file.Files.createDirectories(file.getParent());
        try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(file)) {
            writer.writeTo(out);
        }
    }

    public interface StructureWriter {
        void writeTo(java.io.OutputStream out) throws IOException;
    }

    /** Minimal vanilla-compatible structure NBT writer. */
    public static final class Builder {
        private final Map<Long, Entry> blocks = new HashMap<>();
        private final int[] size;

        public Builder(int sizeX, int sizeY, int sizeZ) {
            this.size = new int[]{sizeX, sizeY, sizeZ};
        }

        public void set(int x, int y, int z, String name, String... properties) {
            if (x < 0 || y < 0 || z < 0 || x >= size[0] || y >= size[1] || z >= size[2]) {
                return;
            }
            Map<String, String> props = new HashMap<>();
            for (int i = 0; i + 1 < properties.length; i += 2) {
                props.put(properties[i], properties[i + 1]);
            }
            blocks.put(((long) x << 40) | ((long) z << 20) | y, new Entry(x, y, z, name, props));
        }

        public void box(int x0, int y0, int z0, int x1, int y1, int z1, String name) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                        set(x, y, z, name);
                    }
                }
            }
        }

        public int size() {
            return blocks.size();
        }

        public NbtCompound toNbt() {
            List<Map<String, String>> palette = new ArrayList<>();
            Map<Map<String, String>, Integer> index = new HashMap<>();
            NbtList blockList = new NbtList();
            List<Entry> ordered = new ArrayList<>(blocks.values());
            ordered.sort((a, b) -> a.y() != b.y() ? Integer.compare(a.y(), b.y())
                    : (a.z() != b.z() ? Integer.compare(a.z(), b.z()) : Integer.compare(a.x(), b.x())));
            for (Entry entry : ordered) {
                Map<String, String> key = new HashMap<>(entry.properties());
                key.put("__name", entry.name());
                Integer state = index.get(key);
                if (state == null) {
                    state = palette.size();
                    index.put(key, state);
                    palette.add(key);
                }
                NbtCompound block = new NbtCompound();
                block.putInt("state", state);
                NbtList pos = new NbtList();
                pos.add(net.minecraft.nbt.NbtInt.of(entry.x()));
                pos.add(net.minecraft.nbt.NbtInt.of(entry.y()));
                pos.add(net.minecraft.nbt.NbtInt.of(entry.z()));
                block.put("pos", pos);
                blockList.add(block);
            }
            NbtList paletteList = new NbtList();
            for (Map<String, String> entry : palette) {
                NbtCompound compound = new NbtCompound();
                compound.putString("Name", entry.get("__name"));
                NbtCompound props = new NbtCompound();
                for (Map.Entry<String, String> prop : entry.entrySet()) {
                    if (!prop.getKey().equals("__name")) {
                        props.putString(prop.getKey(), prop.getValue());
                    }
                }
                if (!props.isEmpty()) {
                    compound.put("Properties", props);
                }
                paletteList.add(compound);
            }
            NbtCompound root = new NbtCompound();
            root.putInt("DataVersion", 3953);
            NbtList sizeList = new NbtList();
            sizeList.add(net.minecraft.nbt.NbtInt.of(size[0]));
            sizeList.add(net.minecraft.nbt.NbtInt.of(size[1]));
            sizeList.add(net.minecraft.nbt.NbtInt.of(size[2]));
            root.put("size", sizeList);
            root.put("palette", paletteList);
            root.put("blocks", blockList);
            root.put("entities", new NbtList());
            root.putString("Author", "FabricRacers");
            return root;
        }

        public void writeTo(java.io.OutputStream out) throws IOException {
            NbtIo.writeCompressed(toNbt(), out);
        }

        /** Convenience for reading back a written structure in tests. */
        public TrackStructure build() {
            return TrackStructure.fromNbt(toNbt());
        }
    }

    /** NBT io needs a random access writer in some versions; this keeps it simple. */
    public static DataInputStream streamOf(byte[] data) {
        return new DataInputStream(new java.io.ByteArrayInputStream(data));
    }
}
