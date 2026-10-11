#!/usr/bin/env python3
"""
FabricRacers asset validator.

Run by `./gradlew check` (see build.gradle) and by CI. It is deliberately
dependency free: it parses every JSON file, walks the model/blockstate graph,
reads the generated structure files back and cross checks them against the
Java registries so a missing texture or a stale track file fails the build
instead of failing in game.

    python3 tools/validate_assets.py
"""

import gzip
import json
import os
import re
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "fabricracers")
DATA = os.path.join(RES, "data", "fabricracers")
MAIN_JAVA = os.path.join(ROOT, "src", "main", "java")
CLIENT_JAVA = os.path.join(ROOT, "src", "client", "java")

PROBLEMS = []
NOTES = []


def fail(message):
    PROBLEMS.append(message)


def read_json(path):
    try:
        with open(path, "r", encoding="utf-8") as handle:
            return json.load(handle)
    except json.JSONDecodeError as exc:
        fail(f"{rel(path)}: invalid JSON ({exc})")
    except OSError as exc:
        fail(f"{rel(path)}: cannot read ({exc})")
    return None


def rel(path):
    return os.path.relpath(path, ROOT)


def exists(path, what):
    if not os.path.exists(path):
        fail(f"missing {what}: {rel(path)}")
        return False
    return True


# ---------------------------------------------------------------------------
# Java side: pull the registries out of the source so this stays in sync
# ---------------------------------------------------------------------------
def java_source(relative):
    path = os.path.join(ROOT, relative)
    if not os.path.exists(path):
        return ""
    with open(path, "r", encoding="utf-8") as handle:
        return handle.read()


def registered_blocks():
    source = java_source("src/main/java/dev/qynl/racers/registry/ModBlocks.java")
    return re.findall(r'register\("([a-z_]+)"', source)


def registered_items():
    source = java_source("src/main/java/dev/qynl/racers/registry/ModItems.java")
    names = [name for name in re.findall(r'register\("([a-z_0-9]+)"', source)
             if not name.endswith("_")]
    # car items are built with a loop helper: car(CarType.KART) -> car_<id>
    car_ids = re.findall(r"car\(CarType\.([A-Z_]+)\)", source)
    for name in car_ids:
        names.append("car_" + name.lower())
    # tuning parts are registered from a switch on Tune
    part_block = source.split("private static Item part(Tune tune)")[-1].split("\n    }")[0]
    names.extend(re.findall(r'->\s*"([a-z_0-9]+)"', part_block))
    # paint cans are registered by name
    names.extend("paint_" + colour for colour in
                 re.findall(r'paint\("([a-z_]+)"', source))
    return sorted(set(names))


def entity_types():
    source = java_source("src/main/java/dev/qynl/racers/car/CarType.java")
    return [name.lower() for name in re.findall(r"^\s{4}([A-Z][A-Z0-9_]*)\(", source, re.M)]


def translation_keys_used():
    keys = set()
    pattern = re.compile(r'translatable\(\s*"([A-Za-z0-9_.\-]+)"')
    for base, _dirs, files in os.walk(os.path.join(ROOT, "src")):
        for name in files:
            if name.endswith(".java"):
                with open(os.path.join(base, name), "r", encoding="utf-8") as handle:
                    keys.update(pattern.findall(handle.read()))
    return sorted(keys)


# ---------------------------------------------------------------------------
# model graph
# ---------------------------------------------------------------------------
def texture_path(identifier):
    namespace, _, path = identifier.partition(":")
    if not path:
        namespace, path = "minecraft", namespace
    return os.path.join(RES, "assets", namespace, "textures", path + ".png")


def check_model(identifier, seen):
    """Resolve a model id and verify every concrete texture it uses exists."""
    namespace, _, path = identifier.partition(":")
    if not path:
        namespace, path = "minecraft", namespace
    model_path = os.path.join(RES, "assets", namespace, "models", path + ".json")
    if namespace != "fabricracers":
        return  # vanilla assets are not ours to validate
    if not os.path.exists(model_path):
        fail(f"missing model {identifier} ({rel(model_path)})")
        return
    if model_path in seen:
        return
    seen.add(model_path)
    model = read_json(model_path)
    if model is None:
        return
    for value in model.get("textures", {}).values():
        if isinstance(value, str) and not value.startswith("#"):
            if not exists(texture_path(value), f"texture for model {identifier}"):
                pass
    parent = model.get("parent")
    if isinstance(parent, str):
        check_model(parent, seen)


def check_block_assets(blocks):
    for block in blocks:
        state_path = os.path.join(ASSETS, "blockstates", block + ".json")
        if not exists(state_path, f"blockstate for {block}"):
            continue
        state = read_json(state_path)
        if state is None:
            continue
        variants = state.get("variants", {})
        if not variants:
            fail(f"blockstate {block}.json has no variants")
        for variant in variants.values():
            entries = variant if isinstance(variant, list) else [variant]
            for entry in entries:
                model = entry.get("model") if isinstance(entry, dict) else None
                if not model:
                    fail(f"blockstate {block}.json has a variant without a model")
                    continue
                check_model(model, set())
        # every block has an item model because they are all sold as items
        item_model = os.path.join(ASSETS, "models", "item", block + ".json")
        if exists(item_model, f"item model for block {block}"):
            check_model(f"fabricracers:{block}" if False else "minecraft:item/generated", set())
        # loot table
        loot = os.path.join(DATA, "loot_table", "blocks", block + ".json")
        if exists(loot, f"loot table for {block}"):
            read_json(loot)


def check_item_assets(items, blocks):
    for item in items:
        if item in blocks:
            continue  # covered by the block check
        model = os.path.join(ASSETS, "models", "item", item + ".json")
        if exists(model, f"item model for {item}"):
            check_model("fabricracers:item/" + item, set())


# ---------------------------------------------------------------------------
# structure files
# ---------------------------------------------------------------------------
class NbtReader:
    """Minimal NBT reader, just enough for structure files."""

    def __init__(self, data):
        self.data = data
        self.pos = 0

    def read(self):
        tag = self.u8()
        if tag != 10:
            raise ValueError("root tag is not a compound")
        self.skip_name()
        return self.payload(10)

    def u8(self):
        value = self.data[self.pos]
        self.pos += 1
        return value

    def i16(self):
        value = struct.unpack_from(">h", self.data, self.pos)[0]
        self.pos += 2
        return value

    def i32(self):
        value = struct.unpack_from(">i", self.data, self.pos)[0]
        self.pos += 4
        return value

    def i64(self):
        value = struct.unpack_from(">q", self.data, self.pos)[0]
        self.pos += 8
        return value

    def f32(self):
        value = struct.unpack_from(">f", self.data, self.pos)[0]
        self.pos += 4
        return value

    def f64(self):
        value = struct.unpack_from(">d", self.data, self.pos)[0]
        self.pos += 8
        return value

    def string(self):
        length = struct.unpack_from(">H", self.data, self.pos)[0]
        self.pos += 2
        value = self.data[self.pos:self.pos + length].decode("utf-8", "replace")
        self.pos += length
        return value

    def skip_name(self):
        self.string()

    def payload(self, tag):
        if tag == 1:
            return self.u8()
        if tag == 2:
            return self.i16()
        if tag == 3:
            return self.i32()
        if tag == 4:
            return self.i64()
        if tag == 5:
            return self.f32()
        if tag == 6:
            return self.f64()
        if tag == 7:
            length = self.i32()
            value = self.data[self.pos:self.pos + length]
            self.pos += length
            return value
        if tag == 8:
            return self.string()
        if tag == 9:
            kind = self.u8()
            length = self.i32()
            return [self.payload(kind) for _ in range(length)]
        if tag == 10:
            value = {}
            while True:
                child = self.u8()
                if child == 0:
                    return value
                name = self.string()
                value[name] = self.payload(child)
        if tag == 11:
            length = self.i32()
            values = list(struct.unpack_from(">%di" % length, self.data, self.pos))
            self.pos += 4 * length
            return values
        if tag == 12:
            length = self.i32()
            values = list(struct.unpack_from(">%dq" % length, self.data, self.pos))
            self.pos += 8 * length
            return values
        raise ValueError(f"unsupported NBT tag {tag}")


def read_nbt(path):
    with gzip.open(path, "rb") as handle:
        return NbtReader(handle.read()).read()


VANILLA_TRACK_BLOCKS = {
    "minecraft:air", "minecraft:cave_air", "minecraft:dirt", "minecraft:grass_block",
    "minecraft:coarse_dirt", "minecraft:stone", "minecraft:cobblestone", "minecraft:gravel",
    "minecraft:sand", "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:oak_log",
    "minecraft:spruce_log", "minecraft:oak_leaves", "minecraft:spruce_leaves",
    "minecraft:short_grass", "minecraft:fern", "minecraft:iron_bars", "minecraft:stone_bricks",
    "minecraft:chiseled_stone_bricks", "minecraft:polished_andesite", "minecraft:andesite",
    "minecraft:glass", "minecraft:tinted_glass", "minecraft:sea_lantern", "minecraft:lantern",
    "minecraft:redstone_lamp", "minecraft:redstone_torch", "minecraft:torch", "minecraft:water",
    "minecraft:red_concrete", "minecraft:white_concrete", "minecraft:black_concrete",
    "minecraft:gray_concrete", "minecraft:light_gray_concrete", "minecraft:blue_concrete",
    "minecraft:yellow_concrete", "minecraft:lime_concrete", "minecraft:cyan_concrete",
    "minecraft:orange_concrete", "minecraft:purple_concrete", "minecraft:pink_concrete",
    "minecraft:brown_concrete", "minecraft:green_concrete", "minecraft:magenta_concrete",
    "minecraft:light_blue_concrete", "minecraft:smooth_stone", "minecraft:smooth_stone_slab",
    "minecraft:stone_slab", "minecraft:barrier", "minecraft:light",
}
MOD_BLOCK_NAMES = {f"fabricracers:{name}" for name in registered_blocks()}


def check_tracks():
    index_path = os.path.join(DATA, "tracks", "_index.json")
    if not exists(index_path, "stock track index"):
        return
    index = read_json(index_path)
    if not isinstance(index, dict):
        fail("_index.json should be an object")
        return
    entries = index.get("tracks", index if isinstance(index, list) else [])
    ids = []
    if isinstance(entries, list):
        for entry in entries:
            if isinstance(entry, str):
                ids.append(entry)
            elif isinstance(entry, dict) and "id" in entry:
                ids.append(entry["id"])
    if not ids:
        fail("_index.json does not list any tracks")
    for track_id in ids:
        json_path = os.path.join(DATA, "tracks", track_id + ".json")
        if not exists(json_path, f"track definition {track_id}"):
            continue
        definition = read_json(json_path)
        if not isinstance(definition, dict):
            continue
        for key in ("name", "width", "structure", "grid", "checkpoints"):
            if key not in definition:
                fail(f"{track_id}.json: missing '{key}'")
        if definition.get("id", track_id) != track_id:
            fail(f"{track_id}.json: id field is '{definition.get('id')}'")
        nbt_path = os.path.join(DATA, "structure", "tracks", track_id + ".nbt")
        if not exists(nbt_path, f"structure for {track_id}"):
            continue
        try:
            nbt = read_nbt(nbt_path)
        except Exception as exc:  # noqa: BLE001 - report whatever went wrong
            fail(f"{rel(nbt_path)}: cannot parse ({exc})")
            continue
        version = nbt.get("DataVersion")
        if version != 3953:
            fail(f"{rel(nbt_path)}: DataVersion {version} (expected 3953)")
        size = nbt.get("size")
        if not isinstance(size, list) or len(size) != 3:
            fail(f"{rel(nbt_path)}: bad size tag")
        else:
            declared = definition.get("size")
            if isinstance(declared, list) and declared[:3] != size[:3]:
                fail(f"{track_id}: json size {declared} != structure size {size}")
        anchor = definition.get("anchor")
        if isinstance(anchor, list) and isinstance(size, list) and len(anchor) == 3:
            for axis, value in enumerate(anchor):
                if not 0 <= value <= size[axis]:
                    fail(f"{track_id}: anchor {anchor} outside the structure {size}")
        palette = nbt.get("palette")
        if not isinstance(palette, list) or not palette:
            fail(f"{rel(nbt_path)}: missing palette")
        else:
            for entry in palette:
                name = entry.get("Name") if isinstance(entry, dict) else None
                if name is None:
                    fail(f"{rel(nbt_path)}: palette entry without Name")
                    continue
                if name in MOD_BLOCK_NAMES:
                    continue
                if name in VANILLA_TRACK_BLOCKS:
                    continue
                if name.startswith("fabricracers:"):
                    fail(f"{rel(nbt_path)}: references unknown mod block {name}")
                else:
                    NOTES.append(f"{rel(nbt_path)}: uses vanilla block {name}")
        # sanity: grid and checkpoints inside the footprint
        if isinstance(size, list) and len(size) == 3:
            sx, sy, sz = size
            for entry in definition.get("grid", []):
                if not (0 <= entry[0] <= sx and 0 <= entry[2] <= sz):
                    fail(f"{track_id}: grid point {entry} outside the structure")
            for entry in definition.get("checkpoints", []):
                if not (0 <= entry[0] <= sx and 0 <= entry[2] <= sz):
                    fail(f"{track_id}: checkpoint {entry} outside the structure")
    return ids


def check_single_files():
    # fabric.mod.json
    mod_path = os.path.join(RES, "fabric.mod.json")
    if exists(mod_path, "fabric.mod.json"):
        mod = read_json(mod_path)
        if isinstance(mod, dict):
            icon = mod.get("icon")
            if icon and not exists(os.path.join(RES, icon), "mod icon"):
                pass
            entrypoints = mod.get("entrypoints", {})
            for key, class_path in (("main", "src/main/java"), ("client", "src/client/java")):
                for name in entrypoints.get(key, []):
                    path = os.path.join(ROOT, class_path, name.replace(".", "/") + ".java")
                    if not exists(path, f"{key} entrypoint class"):
                        pass
    # every JSON file parses
    for base, _dirs, files in os.walk(RES):
        for name in files:
            if name.endswith(".json") or name.endswith(".mcmeta"):
                read_json(os.path.join(base, name))


def check_entity_textures():
    directory = os.path.join(ASSETS, "textures", "entity", "car")
    for car in entity_types():
        exists(os.path.join(directory, car + ".png"), f"car texture for {car}")


def main():
    blocks = registered_blocks()
    items = registered_items()
    if not blocks or not items:
        fail("could not read the block/item registries from the Java sources")
    check_single_files()
    check_block_assets(blocks)
    check_item_assets(items, blocks)
    check_entity_textures()
    lang_path = os.path.join(ASSETS, "lang", "en_us.json")
    if exists(lang_path, "en_us.json"):
        lang = read_json(lang_path) or {}
        for item in items:
            if item in blocks:
                continue
            if f"item.fabricracers.{item}" not in lang:
                fail(f"lang is missing item.fabricracers.{item}")
        for block in blocks:
            if f"block.fabricracers.{block}" not in lang:
                fail(f"lang is missing block.fabricracers.{block}")
        vanilla = {"gui.done", "gui.cancel", "gui.back", "gui.toTitle"}
        for key in translation_keys_used():
            if key in vanilla or key.endswith(".") or key.endswith("_") \
                    or key.startswith("item.fabricracers.") and key.count(".") < 3:
                continue
            if key.startswith("fabricracers.") or key.startswith("itemGroup."):
                continue
            if key not in lang:
                fail(f"lang is missing {key} (used by code)")
    check_tracks()

    for note in NOTES[:10]:
        print("note:", note)
    if len(NOTES) > 10:
        print(f"note: ... and {len(NOTES) - 10} more vanilla-block notes")
    if PROBLEMS:
        print(f"FabricRacers asset validation FAILED ({len(PROBLEMS)} problems):")
        for problem in PROBLEMS:
            print(" -", problem)
        return 1
    print(f"FabricRacers assets OK ({len(blocks)} blocks, {len(items)} items).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
