package dev.qynl.racers.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import dev.qynl.racers.FabricRacers;
import dev.qynl.racers.car.CarData;
import dev.qynl.racers.car.CarType;
import dev.qynl.racers.car.PaintColors;
import dev.qynl.racers.car.Tune;
import dev.qynl.racers.item.CarItem;
import dev.qynl.racers.item.ToolItems;

/** Every item: cars, tools, trophies, paint cans, upgrade parts and blocks. */
public final class ModItems {
    private static final Map<String, Item> BY_NAME = new LinkedHashMap<>();
    private static final Map<CarType, Item> CAR_ITEMS = new LinkedHashMap<>();
    private static final Map<Tune, Item> TUNE_PARTS = new LinkedHashMap<>();
    private static final Map<String, Item> PAINT_ITEMS = new LinkedHashMap<>();

    // --- cars --------------------------------------------------------------
    public static final Item CAR_KART = car(CarType.KART);
    public static final Item CAR_RALLY = car(CarType.RALLY);
    public static final Item CAR_SPORT = car(CarType.SPORT);
    public static final Item CAR_HYPER = car(CarType.HYPER);
    public static final Item CAR_F1 = car(CarType.F1);
    public static final Item CAR_TRUCK = car(CarType.TRUCK);
    public static final Item CAR_DRIFT = car(CarType.DRIFT);
    public static final Item CAR_HOTROD = car(CarType.HOTROD);
    public static final Item CAR_SHADOW = car(CarType.SHADOW);

    // --- tools -------------------------------------------------------------
    public static final Item CARROT_KEY = register("carrot_key",
            new ToolItems.CarKey(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));
    public static final Item WRENCH = register("wrench",
            new ToolItems.Wrench(new Item.Settings().maxCount(1).maxDamage(220)));
    public static final Item TRACK_WAND = register("track_wand",
            new ToolItems.TrackWand(new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item STOPWATCH = register("stopwatch",
            new ToolItems.Stopwatch(new Item.Settings().maxCount(1)));
    public static final Item TROPHY_GOLD = register("trophy_gold",
            new ToolItems.Trophy("gold", new Item.Settings().maxCount(1).rarity(Rarity.RARE)));
    public static final Item TROPHY_SILVER = register("trophy_silver",
            new ToolItems.Trophy("silver", new Item.Settings().maxCount(16)));
    public static final Item TROPHY_BRONZE = register("trophy_bronze",
            new ToolItems.Trophy("bronze", new Item.Settings().maxCount(16)));
    public static final Item RACE_PASS = register("race_pass",
            new ToolItems.RacePass(new Item.Settings().maxCount(1)));

    // --- paint -------------------------------------------------------------
    public static final Item PAINT_WHITE = paint("white", PaintColors.WHITE);
    public static final Item PAINT_BLACK = paint("black", PaintColors.BLACK);
    public static final Item PAINT_RED = paint("red", PaintColors.RED);
    public static final Item PAINT_ORANGE = paint("orange", PaintColors.ORANGE);
    public static final Item PAINT_YELLOW = paint("yellow", PaintColors.YELLOW);
    public static final Item PAINT_GREEN = paint("green", PaintColors.GREEN);
    public static final Item PAINT_BLUE = paint("blue", PaintColors.BLUE);
    public static final Item PAINT_PURPLE = paint("purple", PaintColors.PURPLE);

    // --- tuning parts ------------------------------------------------------
    public static final Item TUNE_ENGINE = part(Tune.ENGINE);
    public static final Item TUNE_GEARBOX = part(Tune.GEARBOX);
    public static final Item TUNE_BRAKES = part(Tune.BRAKES);
    public static final Item TUNE_TIRES = part(Tune.TIRES);
    public static final Item TUNE_AERO = part(Tune.AERO);
    public static final Item TUNE_CHASSIS = part(Tune.CHASSIS);
    public static final Item TUNE_BOOST = part(Tune.BOOST);

    // --- block items -------------------------------------------------------
    public static final Item BLOCK_ASPHALT_ITEM = blockItem("asphalt", ModBlocks.ASPHALT);
    public static final Item BLOCK_ASPHALT_CRACKED_ITEM = blockItem("asphalt_cracked", ModBlocks.ASPHALT_CRACKED);
    public static final Item BLOCK_ROAD_BLOCK_ITEM = blockItem("road_block", ModBlocks.ROAD_BLOCK);
    public static final Item BLOCK_FINISH_LINE_ITEM = blockItem("finish_line", ModBlocks.FINISH_LINE);
    public static final Item BLOCK_PIT_LANE_ITEM = blockItem("pit_lane", ModBlocks.PIT_LANE);
    public static final Item BLOCK_KERB_ITEM = blockItem("kerb", ModBlocks.KERB);
    public static final Item BLOCK_SAFETY_BARRIER_ITEM = blockItem("safety_barrier", ModBlocks.SAFETY_BARRIER);
    public static final Item BLOCK_START_GATE_ITEM = blockItem("start_gate", ModBlocks.START_GATE);
    public static final Item BLOCK_CHECKPOINT_MARKER_ITEM = blockItem("checkpoint_marker", ModBlocks.CHECKPOINT_MARKER);
    public static final Item BLOCK_STATION_ITEM = blockItem("station", ModBlocks.STATION);
    public static final Item BLOCK_BOOST_PAD_ITEM = blockItem("boost_pad", ModBlocks.BOOST_PAD);
    public static final Item BLOCK_FAKE_ICE_ITEM = blockItem("fake_ice", ModBlocks.FAKE_ICE);
    public static final Item BLOCK_FAKE_SLIME_ITEM = blockItem("fake_slime", ModBlocks.FAKE_SLIME);

    public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP,
            FabricRacers.id("racing"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.fabricracers.racing"))
                    .icon(() -> new ItemStack(CAR_SPORT))
                    .entries((context, entries) -> {
                        for (CarType type : CarType.values()) {
                            entries.add(new ItemStack(CAR_ITEMS.get(type)));
                        }
                        entries.add(WRENCH);
                        entries.add(TRACK_WAND);
                        entries.add(STOPWATCH);
                        entries.add(CARROT_KEY);
                        entries.add(RACE_PASS);
                        entries.add(TROPHY_GOLD);
                        entries.add(TROPHY_SILVER);
                        entries.add(TROPHY_BRONZE);
                        PAINT_ITEMS.values().forEach(entries::add);
                        TUNE_PARTS.values().forEach(entries::add);
                        entries.add(BLOCK_ASPHALT_ITEM);
                        entries.add(BLOCK_ASPHALT_CRACKED_ITEM);
                        entries.add(BLOCK_ROAD_BLOCK_ITEM);
                        entries.add(BLOCK_FINISH_LINE_ITEM);
                        entries.add(BLOCK_PIT_LANE_ITEM);
                        entries.add(BLOCK_KERB_ITEM);
                        entries.add(BLOCK_SAFETY_BARRIER_ITEM);
                        entries.add(BLOCK_START_GATE_ITEM);
                        entries.add(BLOCK_CHECKPOINT_MARKER_ITEM);
                        entries.add(BLOCK_STATION_ITEM);
                        entries.add(BLOCK_BOOST_PAD_ITEM);
                        entries.add(BLOCK_FAKE_ICE_ITEM);
                        entries.add(BLOCK_FAKE_SLIME_ITEM);
                    })
                    .build());

    private ModItems() {
    }

    // ------------------------------------------------------------------
    private static Item car(CarType type) {
        Item item = register(type.itemId(), new CarItem(type, new Item.Settings()
                .maxCount(1).rarity(type == CarType.SHADOW ? Rarity.EPIC : Rarity.UNCOMMON)));
        CAR_ITEMS.put(type, item);
        return item;
    }

    private static Item part(Tune tune) {
        String name = switch (tune) {
            case ENGINE -> "engine_turbo";
            case GEARBOX -> "gearbox_dct";
            case BRAKES -> "brakes_carbon";
            case TIRES -> "tires_street";
            case AERO -> "aero_wing";
            case CHASSIS -> "chassis_plate";
            case BOOST -> "boost_nitro";
        };
        Item item = register(name, new ToolItems.TunePart(tune, new Item.Settings().maxCount(64)));
        TUNE_PARTS.put(tune, item);
        return item;
    }

    private static Item paint(String name, int color) {
        Item item = register("paint_" + name, new ToolItems.PaintCan(color, new Item.Settings().maxCount(16)));
        PAINT_ITEMS.put(name, item);
        return item;
    }

    private static Item blockItem(String name, Block block) {
        return register(name, new BlockItem(block, new Item.Settings()));
    }

    private static Item register(String name, Item item) {
        BY_NAME.put(name, item);
        return Registry.register(Registries.ITEM, FabricRacers.id(name), item);
    }

    public static Item byName(String name) {
        return BY_NAME.get(name);
    }

    public static Item carItem(CarType type) {
        return CAR_ITEMS.get(type);
    }

    /** The upgrade part item for a tuning system. */
    public static Item partItem(Tune tune) {
        return TUNE_PARTS.get(tune);
    }

    public static Item paintItem(String name) {
        return PAINT_ITEMS.get(name);
    }

    public static Map<CarType, Item> carItems() {
        return CAR_ITEMS;
    }

    /** A ready to drive car, owned by {@code owner}. */
    public static ItemStack carStack(CarType type, PlayerEntity owner) {
        ItemStack stack = new ItemStack(carItem(type));
        CarData.apply(stack, CarData.fresh(type, owner));
        return stack;
    }

    public static void init() {
        // The mod has its own creative tab; also mirror the headline items into
        // the vanilla tools tab (ItemGroups' keys are private in this version).
        ItemGroupEvents.modifyEntriesEvent(net.minecraft.registry.RegistryKey.of(
                        net.minecraft.registry.RegistryKeys.ITEM_GROUP,
                        net.minecraft.util.Identifier.ofVanilla("tools")))
                .register(entries -> {
                    entries.add(CAR_SPORT);
                    entries.add(WRENCH);
                });
    }
}
