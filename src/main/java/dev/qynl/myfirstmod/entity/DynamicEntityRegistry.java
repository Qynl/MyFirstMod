package dev.qynl.myfirstmod.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public final class DynamicEntityRegistry {
    private static final List<Identifier> ALL_ENTITIES = new ArrayList<>();
    private static final List<Identifier> MODDED_ENTITIES = new ArrayList<>();
    private static final List<String> MOUNT_ENTITIES = new ArrayList<>();
    private static boolean initialized = false;

    private static final Set<String> EXCLUDED_ENTITIES = Set.of(
            "minecraft:area_effect_cloud",
            "minecraft:armor_stand",
            "minecraft:item",
            "minecraft:experience_orb",
            "minecraft:marker",
            "minecraft:interaction",
            "minecraft:block_display",
            "minecraft:item_display",
            "minecraft:text_display",
            "minecraft:arrow",
            "minecraft:spectral_arrow",
            "minecraft:trident",
            "minecraft:wind_charge",
            "minecraft:breeze_wind_charge",
            "minecraft:small_fireball",
            "minecraft:fireball",
            "minecraft:dragon_fireball",
            "minecraft:wither_skull",
            "minecraft:shulker_bullet",
            "minecraft:potion",
            "minecraft:experience_bottle",
            "minecraft:eye_of_ender",
            "minecraft:end_crystal",
            "minecraft:tnt",
            "minecraft:falling_block",
            "minecraft:boat",
            "minecraft:chest_boat",
            "minecraft:minecart",
            "minecraft:chest_minecart",
            "minecraft:furnace_minecart",
            "minecraft:tnt_minecart",
            "minecraft:hopper_minecart",
            "minecraft:spawner_minecart",
            "minecraft:command_block_minecart",
            "minecraft:lightning_bolt",
            "minecraft:evoker_fangs",
            "minecraft:fishing_bobber",
            "minecraft:egg",
            "minecraft:snowball",
            "minecraft:firework_rocket",
            "minecraft:leash_knot",
            "minecraft:painting",
            "minecraft:item_frame",
            "minecraft:glow_item_frame",
            "minecraft:ominous_item_spawner"
    );

    private DynamicEntityRegistry() {}

    public static synchronized void ensureInitialized() {
        if (initialized && !ALL_ENTITIES.isEmpty()) return;

        ALL_ENTITIES.clear();
        MODDED_ENTITIES.clear();
        MOUNT_ENTITIES.clear();

        // Always start mounts with empty (None / On Foot)
        MOUNT_ENTITIES.add("");

        // High-priority defaults first for instant intuitive selection
        List<Identifier> priorityEntities = List.of(
                Identifier.of("minecraft", "villager"),
                Identifier.of("minecraft", "skeleton"),
                Identifier.of("minecraft", "zombie"),
                Identifier.of("minecraft", "pillager"),
                Identifier.of("minecraft", "vindicator"),
                Identifier.of("minecraft", "piglin_brute"),
                Identifier.of("minecraft", "piglin"),
                Identifier.of("minecraft", "witch"),
                Identifier.of("minecraft", "evoker"),
                Identifier.of("minecraft", "iron_golem"),
                Identifier.of("minecraft", "wolf"),
                Identifier.of("minecraft", "polar_bear"),
                Identifier.of("minecraft", "panda"),
                Identifier.of("minecraft", "fox"),
                Identifier.of("minecraft", "cat"),
                Identifier.of("minecraft", "breeze"),
                Identifier.of("minecraft", "bogged"),
                Identifier.of("minecraft", "allay"),
                Identifier.of("minecraft", "blaze"),
                Identifier.of("minecraft", "wither_skeleton"),
                Identifier.of("minecraft", "stray"),
                Identifier.of("minecraft", "drowned"),
                Identifier.of("minecraft", "warden"),
                Identifier.of("minecraft", "ravager"),
                Identifier.of("minecraft", "creeper"),
                Identifier.of("minecraft", "spider"),
                Identifier.of("minecraft", "cave_spider"),
                Identifier.of("minecraft", "enderman"),
                Identifier.of("minecraft", "horse"),
                Identifier.of("minecraft", "skeleton_horse"),
                Identifier.of("minecraft", "camel"),
                Identifier.of("minecraft", "sniffer"),
                Identifier.of("minecraft", "strider"),
                Identifier.of("minecraft", "slime"),
                Identifier.of("minecraft", "magma_cube"),
                Identifier.of("minecraft", "ghast"),
                Identifier.of("minecraft", "shulker"),
                Identifier.of("minecraft", "guardian"),
                Identifier.of("minecraft", "elder_guardian"),
                Identifier.of("minecraft", "wither")
        );

        for (Identifier id : priorityEntities) {
            if (Registries.ENTITY_TYPE.getOrEmpty(id).isPresent()) {
                ALL_ENTITIES.add(id);
            }
        }

        // Dynamically discover all other registered EntityTypes from Minecraft AND any installed mods (e.g. Naturalist, Alex's Mobs, etc.)
        for (Identifier id : Registries.ENTITY_TYPE.getIds()) {
            if (EXCLUDED_ENTITIES.contains(id.toString())) continue;
            if (ALL_ENTITIES.contains(id)) continue;

            EntityType<?> type = Registries.ENTITY_TYPE.get(id);
            if (type == null) continue;

            ALL_ENTITIES.add(id);

            if (!id.getNamespace().equals("minecraft")) {
                MODDED_ENTITIES.add(id);
            }
        }

        // Populate mounts: All rideable / beast entities
        List<String> defaultMounts = List.of(
                "minecraft:horse",
                "minecraft:skeleton_horse",
                "minecraft:zombie_horse",
                "minecraft:camel",
                "minecraft:donkey",
                "minecraft:mule",
                "minecraft:llama",
                "minecraft:ravager",
                "minecraft:spider",
                "minecraft:cave_spider",
                "minecraft:wolf",
                "minecraft:strider",
                "minecraft:pig",
                "minecraft:polar_bear"
        );

        for (String m : defaultMounts) {
            Identifier id = Identifier.tryParse(m);
            if (id != null && Registries.ENTITY_TYPE.getOrEmpty(id).isPresent()) {
                if (!MOUNT_ENTITIES.contains(m)) {
                    MOUNT_ENTITIES.add(m);
                }
            }
        }

        // Include any modded entities into Mounts as well (e.g. naturalist:elephant, naturalist:rhino, naturalist:zebra)
        for (Identifier id : MODDED_ENTITIES) {
            String path = id.getPath().toLowerCase();
            if (path.contains("horse") || path.contains("bear") || path.contains("elephant") ||
                path.contains("rhino") || path.contains("zebra") || path.contains("lion") ||
                path.contains("tiger") || path.contains("boar") || path.contains("mammoth") ||
                path.contains("mount") || path.contains("dino") || path.contains("dragon")) {
                if (!MOUNT_ENTITIES.contains(id.toString())) {
                    MOUNT_ENTITIES.add(id.toString());
                }
            }
        }

        initialized = true;
    }

    public static List<Identifier> getAllEntities() {
        ensureInitialized();
        return Collections.unmodifiableList(ALL_ENTITIES);
    }

    public static List<Identifier> getModdedEntities() {
        ensureInitialized();
        return Collections.unmodifiableList(MODDED_ENTITIES);
    }

    public static List<String> getMountEntities() {
        ensureInitialized();
        return Collections.unmodifiableList(MOUNT_ENTITIES);
    }

    public static Identifier cycleEntity(Identifier current, int direction) {
        ensureInitialized();
        if (ALL_ENTITIES.isEmpty()) return Identifier.of("minecraft", "villager");

        int index = ALL_ENTITIES.indexOf(current);
        if (index == -1) {
            index = 0;
        } else {
            index = (index + direction + ALL_ENTITIES.size()) % ALL_ENTITIES.size();
        }
        return ALL_ENTITIES.get(index);
    }

    public static String cycleMount(String current, int direction) {
        ensureInitialized();
        if (MOUNT_ENTITIES.isEmpty()) return "";

        int index = MOUNT_ENTITIES.indexOf(current);
        if (index == -1) {
            index = 0;
        } else {
            index = (index + direction + MOUNT_ENTITIES.size()) % MOUNT_ENTITIES.size();
        }
        return MOUNT_ENTITIES.get(index);
    }

    public static String formatEntityName(Identifier id) {
        if (id == null) return "None";
        String path = id.getPath();
        String readable = Arrays.stream(path.split("_"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));

        if (!id.getNamespace().equals("minecraft")) {
            String modName = Arrays.stream(id.getNamespace().split("_"))
                    .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1))
                    .collect(Collectors.joining(" "));
            return readable + " [" + modName + "]";
        }
        return readable;
    }

    public static String formatMountName(String mount) {
        if (mount == null || mount.isBlank() || mount.equalsIgnoreCase("none")) {
            return "None (On Foot)";
        }
        Identifier id = Identifier.tryParse(mount);
        if (id == null) return mount;
        return formatEntityName(id);
    }

    public static boolean isModded(Identifier id) {
        return id != null && !id.getNamespace().equals("minecraft");
    }
}
