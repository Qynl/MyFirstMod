#!/usr/bin/env python3
"""
FabricRacers track toolkit.

Pure-stdlib helpers used by gen_tracks.py:

  * a tiny NBT writer (vanilla structure-file layout, so the results also load
    in a vanilla structure block)
  * spline sampling + "stamping" of roads / kerbs / walls / verges onto a grid
  * track map preview rendering (so designers can eyeball a layout)
  * emission of the data/fabricracers/tracks/*.json definition files

Structure coordinate contract (mirrored by TrackStructureBuilder.java):

    local y=0  -> solid foundation (dirt/stone) under the road
    local y=1  -> the road surface itself (asphalt, kerbs, lines)
    local y=2  -> car level: checkpoint light strips, barriers, pit wall
    local y=3+ -> everything above: barrier tops, gantries, buildings, trees

The structure is placed with its local y=2 layer on top of the road surface,
i.e. world y = stationBlock.y for car level.
"""

import gzip
import json
import math
import os
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "fabricracers")
STRUCT_DIR = os.path.join(DATA, "structure", "tracks")
TRACK_DIR = os.path.join(DATA, "tracks")
PREVIEW_DIR = os.path.join(ROOT, "tools", "previews")

# --- block name shortcuts --------------------------------------------------
NS = "fabricracers:"
ASPHALT = NS + "asphalt"
ASPHALT_WORN = NS + "asphalt_cracked"
ROAD_LINE = NS + "road_block"
FINISH = NS + "finish_line"
KERB = NS + "kerb"
BARRIER = NS + "safety_barrier"
GATE = NS + "start_gate"
CHECKPOINT = NS + "checkpoint_marker"
PIT = NS + "pit_lane"
STATION = NS + "station"
DIRT = "minecraft:dirt"
GRASS = "minecraft:grass_block"
COARSE = "minecraft:coarse_dirt"
STONE = "minecraft:stone"
SMOOTH = "minecraft:smooth_stone"
QUARTZ = "minecraft:quartz_block"
CONCRETE = {
    "white": "minecraft:white_concrete",
    "black": "minecraft:black_concrete",
    "red": "minecraft:red_concrete",
    "yellow": "minecraft:yellow_concrete",
    "blue": "minecraft:blue_concrete",
    "green": "minecraft:lime_concrete",
    "gray": "minecraft:gray_concrete",
    "orange": "minecraft:orange_concrete",
    "purple": "minecraft:purple_concrete",
    "pink": "minecraft:pink_concrete",
    "cyan": "minecraft:cyan_concrete",
    "lime": "minecraft:lime_concrete",
    "light_blue": "minecraft:light_blue_concrete",
    "brown": "minecraft:brown_concrete",
}
GLASS = "minecraft:glass"
LANTERN = "minecraft:sea_lantern"
IRON = "minecraft:iron_block"
LOG = "minecraft:oak_log"
LEAVES = "minecraft:oak_leaves"
PLANKS = "minecraft:oak_planks"
WOOL = "minecraft:%s_wool"
TERRACOTTA = "minecraft:%s_terracotta"

ROAD_Y = 1          # local y of the road surface blocks
CAR_Y = 2           # local y of car level (checkpoint strips, barriers base)
FOUND_Y = 0         # local y of the foundation layer


# ---------------------------------------------------------------------------
# NBT writer
# ---------------------------------------------------------------------------
TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG = 0, 1, 2, 3, 4
TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING = 5, 6, 7, 8
TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 9, 10, 11


def _nbt_string(name, value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _tag_payload(tag_id, value):
    if tag_id == TAG_BYTE:
        return struct.pack(">b", value)
    if tag_id == TAG_SHORT:
        return struct.pack(">h", value)
    if tag_id == TAG_INT:
        return struct.pack(">i", value)
    if tag_id == TAG_LONG:
        return struct.pack(">q", value)
    if tag_id == TAG_FLOAT:
        return struct.pack(">f", value)
    if tag_id == TAG_DOUBLE:
        return struct.pack(">d", value)
    if tag_id == TAG_STRING:
        return _nbt_string("", value)
    if tag_id == TAG_LIST:
        item_type, items = value
        out = struct.pack(">bi", item_type, len(items))
        for item in items:
            out += _tag_payload(item_type, item)
        return out
    if tag_id == TAG_COMPOUND:
        out = b""
        for key, (sub_id, sub_val) in value.items():
            out += struct.pack(">b", sub_id) + _nbt_string("", key) + _tag_payload(sub_id, sub_val)
        return out + struct.pack(">b", TAG_END)
    if tag_id == TAG_INT_ARRAY:
        return struct.pack(">i", len(value)) + b"".join(struct.pack(">i", v) for v in value)
    raise ValueError("unsupported tag %d" % tag_id)


def _write_file(path, root_compound, gzipped=True):
    data = struct.pack(">b", TAG_COMPOUND) + _nbt_string("", "") + _tag_payload(TAG_COMPOUND, root_compound)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if gzipped:
        # mtime=0 keeps generated structures byte-identical between runs
        with gzip.GzipFile(filename=path, mode="wb", compresslevel=9, mtime=0) as fh:
            fh.write(data)
    else:
        with open(path, "wb") as fh:
            fh.write(data)


def write_structure(path, blocks, size, anchor, author="FabricRacers"):
    """blocks: {(x, y, z): (name, props_dict or None)}"""
    palette = []
    palette_index = {}
    entries = []
    for (x, y, z) in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        name, props = blocks[(x, y, z)]
        key = (name, tuple(sorted((props or {}).items())))
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(key)
        entries.append((palette_index[key], x, y, z))

    pal_list = []
    for name, props in palette:
        comp = {"Name": (TAG_STRING, name)}
        if props:
            comp["Properties"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in props})
        pal_list.append(comp)

    block_list = [
        {
            "state": (TAG_INT, state),
            "pos": (TAG_LIST, (TAG_INT, [x, y, z])),
        }
        for state, x, y, z in entries
    ]

    root = {
        "DataVersion": (TAG_INT, 3953),          # 1.21.1
        "size": (TAG_LIST, (TAG_INT, list(size))),
        "palette": (TAG_LIST, (TAG_COMPOUND, pal_list)),
        "blocks": (TAG_LIST, (TAG_COMPOUND, block_list)),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
        # custom tags (ignored by vanilla, read by TrackStructureBuilder)
        "Author": (TAG_STRING, author),
        "AnchorPos": (TAG_LIST, (TAG_INT, list(anchor))),
    }
    _write_file(path, root)
    return len(entries), len(palette)


# ---------------------------------------------------------------------------
# spline + design helpers
# ---------------------------------------------------------------------------
def catmull_rom_closed(points, samples_per_segment=60):
    """Returns a dense list of (x, z) samples around a closed loop."""
    out = []
    n = len(points)
    for i in range(n):
        p0 = points[(i - 1) % n]
        p1 = points[i]
        p2 = points[(i + 1) % n]
        p3 = points[(i + 2) % n]
        for s in range(samples_per_segment):
            t = s / float(samples_per_segment)
            t2, t3 = t * t, t * t * t
            x = 0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t
                       + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                       + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3)
            z = 0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t
                       + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                       + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
            out.append((x, z))
    return out


def catmull_rom_open(points, samples_per_segment=40):
    """Smooth open polyline through the given points."""
    if len(points) < 2:
        return list(points)
    pts = [points[0]] + list(points) + [points[-1]]
    out = []
    for i in range(1, len(pts) - 2):
        p0, p1, p2, p3 = pts[i - 1], pts[i], pts[i + 1], pts[i + 2]
        for s in range(samples_per_segment):
            t = s / float(samples_per_segment)
            t2, t3 = t * t, t * t * t
            out.append((
                0.5 * ((2 * p1[0]) + (-p0[0] + p2[0]) * t
                       + (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2
                       + (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
                0.5 * ((2 * p1[1]) + (-p0[1] + p2[1]) * t
                       + (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2
                       + (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3),
            ))
    out.append(points[-1])
    return out


def resample(points, spacing=0.25):
    """Resamples a polyline to roughly constant spacing; returns (pts, arc_len)."""
    out = [points[0]]
    arc = [0.0]
    acc = 0.0
    for i in range(1, len(points)):
        x0, z0 = out[-1]
        x1, z1 = points[i]
        d = math.hypot(x1 - x0, z1 - z0)
        if d < 1e-9:
            continue
        while acc + d >= spacing:
            t = (spacing - acc) / d
            x0, z0 = x0 + (x1 - x0) * t, z0 + (z1 - z0) * t
            out.append((x0, z0))
            arc.append(arc[-1] + spacing)
            d = math.hypot(x1 - x0, z1 - z0)
            acc = 0.0
        acc += d
    return out, arc


class Design:
    """Accumulates blocks on an integer grid; later writes NBT + previews."""

    def __init__(self, key, name, short_name):
        self.key = key
        self.name = name
        self.short_name = short_name
        self.blocks = {}
        self.checkpoints = []      # (x, z, yaw)
        self.grid = []             # (x, z)
        self.road_cells = set()
        self.kerb_cells = {}
        self.anchor = (0, CAR_Y, 0)
        self.start_line = (0, 0)
        self.pit = {}

    # -- primitives ------------------------------------------------------
    def set(self, x, y, z, name, **props):
        self.blocks[(int(x), int(y), int(z))] = (name, props or None)

    def box(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def hollow_box(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.set(x, y, z, name, **props)

    def has(self, x, y, z):
        return (int(x), int(y), int(z)) in self.blocks

    # -- road stamping ---------------------------------------------------
    def stamp_road(self, waypoints, width=7, worn=True, kerb=True, wall=True,
                   verge=2, surface=ASPHALT, seed=1):
        half = (width - 1) // 2
        center, arc = resample(catmull_rom_closed(waypoints), 0.25)
        n = len(center)
        for idx, (cx, cz) in enumerate(center):
            nx, nz = center[(idx + 1) % n]
            px, pz = center[(idx - 1) % n]
            tx, tz = nx - px, nz - pz
            tl = math.hypot(tx, tz) or 1.0
            tx, tz = tx / tl, tz / tl
            perp = (-tz, tx)
            s = arc[idx]
            for i in range(-half, half + 1):
                x = int(round(cx + perp[0] * i))
                z = int(round(cz + perp[1] * i))
                self.road_cells.add((x, z))
                worn_here = worn and ((x * 31 + z * 17 + idx) % 23 == 0)
                self.set(x, ROAD_Y, z, ASPHALT_WORN if worn_here else surface)
                self.set(x, FOUND_Y, z, DIRT)
            if kerb:
                for side in (-1, 1):
                    i = side * (half + 1)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    color = "red" if int(s / 1.6) % 2 == 0 else "white"
                    self.kerb_cells[(x, z)] = color
                    self.set(x, ROAD_Y, z, KERB, color=color)
                    self.set(x, FOUND_Y, z, DIRT)
            if wall:
                for side in (-1, 1):
                    i = side * (half + 2)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    self.set(x, CAR_Y, z, BARRIER)
                    self.set(x, CAR_Y + 1, z, BARRIER)
                    self.set(x, FOUND_Y, z, DIRT)
            for side in (-1, 1):
                for extra in range(3, 3 + verge):
                    i = side * (half + extra)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    if (x, z) in self.road_cells or (x, z) in self.kerb_cells:
                        continue
                    if self.has(x, FOUND_Y, z):
                        continue
                    self.set(x, FOUND_Y, z, COARSE if ((x + z) % 7 == 0) else GRASS,
                             **({"snowy": "false"} if (x + z) % 7 else {}))
        return center, arc

    def stamp_path(self, waypoints, width, surface=PIT, track=False, kerb=False,
                   wall=False, verge=0):
        """Stamps an open, non-looping road segment (pit lanes, access roads)."""
        half = (width - 1) // 2
        pts = resample(catmull_rom_open(waypoints, 12), 0.25)[0]
        n = len(pts)
        for idx, (cx, cz) in enumerate(pts):
            ax, az = pts[min(idx + 1, n - 1)]
            bx, bz = pts[max(idx - 1, 0)]
            tx, tz = ax - bx, az - bz
            tl = math.hypot(tx, tz) or 1.0
            perp = (-tz / tl, tx / tl)
            for i in range(-half, half + 1):
                x = int(round(cx + perp[0] * i))
                z = int(round(cz + perp[1] * i))
                if track:
                    self.road_cells.add((x, z))
                self.set(x, ROAD_Y, z, surface)
                self.set(x, FOUND_Y, z, DIRT)
            if kerb:
                for side in (-1, 1):
                    i = side * (half + 1)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    self.kerb_cells[(x, z)] = "red"
                    self.set(x, ROAD_Y, z, KERB, color="red")
            if wall:
                for side in (-1, 1):
                    i = side * (half + 2)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    self.set(x, CAR_Y, z, BARRIER)
                    self.set(x, CAR_Y + 1, z, BARRIER)
            for side in (-1, 1):
                for extra in range(3, 3 + verge):
                    i = side * (half + extra)
                    x = int(round(cx + perp[0] * i))
                    z = int(round(cz + perp[1] * i))
                    if (x, z) in self.road_cells or (x, z) in self.kerb_cells:
                        continue
                    if not self.has(x, FOUND_Y, z):
                        self.set(x, FOUND_Y, z, GRASS, snowy="false")
        return pts

    def clear_column(self, x, z, ys):
        for y in ys:
            self.blocks.pop((int(x), int(y), int(z)), None)

    def cells_with(self, y, name):
        return set((x, z) for (x, yy, z), (nm, _p) in self.blocks.items()
                   if yy == y and nm == name)

    def paint_line_across(self, center, idx, name, checker=True, span=None, **props):
        """Paints a stripe across the road at sample index `idx`."""
        n = len(center)
        nx, nz = center[(idx + 1) % n]
        px, pz = center[(idx - 1) % n]
        tx, tz = nx - px, nz - pz
        tl = math.hypot(tx, tz) or 1.0
        perp = (-tz / tl, tx / tl)
        cx, cz = center[idx]
        for i in range(-(span or 3), (span or 3) + 1):
            x = int(round(cx + perp[0] * i))
            z = int(round(cz + perp[1] * i))
            if (x, z) not in self.road_cells:
                continue
            if checker and (i + idx) % 2 == 0:
                self.set(x, ROAD_Y, z, FINISH)
            else:
                self.set(x, ROAD_Y, z, name, **props)

    def checkpoint(self, center, arc, idx, yaw_from_dir):
        cx, cz = center[idx]
        n = len(center)
        nx, nz = center[(idx + 1) % n]
        px, pz = center[(idx - 1) % n]
        tx, tz = nx - px, nz - pz
        tl = math.hypot(tx, tz) or 1.0
        perp = (-tz / tl, tx / tl)
        for i in range(-4, 5):
            x = int(round(cx + perp[0] * i))
            z = int(round(cz + perp[1] * i))
            if (x, z) not in self.road_cells:
                continue
            self.set(x, CAR_Y, z, CHECKPOINT, lit="false")
        yaw = math.degrees(math.atan2(-tx, tz)) % 360.0
        self.checkpoints.append((round(cx, 1), round(cz, 1), round(yaw, 1)))

    # -- decoration ------------------------------------------------------
    def gantry(self, x, z, width=15, beam_y=CAR_Y + 4, span=9):
        """Start/finish gantry across the road, centred on (x, z)."""
        for side in (-1, 1):
            px = x + side * span
            self.box(px, CAR_Y, z, px, CAR_Y + 4, z, IRON)
        self.box(x - span, beam_y, z, x + span, beam_y, z, CONCRETE["white"])
        self.box(x - span, beam_y + 1, z, x + span, beam_y + 1, z, CONCRETE["black"])
        for i in range(-4, 5):
            if i in (-4, -2, 0, 2, 4):
                continue
            self.set(x + i, beam_y, z, GATE, lit="false")
        for i in (-6, -3, 3, 6):
            self.set(x + i, beam_y - 1, z, CONCRETE["red"])
        self.set(x, beam_y + 2, z, LANTERN)

    def grandstand(self, x, z, length=18, depth=7, facing=1):
        """Simple grandstand along the main straight."""
        for i in range(length):
            for d in range(depth):
                seat = i % 3
                color = ("red", "white", "blue")[seat]
                self.set(x + i, CAR_Y + 1 + d, z + facing * d, WOOL % color)
            self.set(x + i, CAR_Y + 9, z + facing * (depth - 1), CONCRETE["gray"])
        self.box(x, CAR_Y + 2, z + facing * (depth - 1), x + length - 1, CAR_Y + 8,
                 z + facing * (depth - 1), GLASS)
        for i in range(0, length, 4):
            self.set(x + i, CAR_Y + 9, z, CONCRETE["gray"])
        self.box(x, CAR_Y + 10, z, x + length - 1, CAR_Y + 10, z + facing * (depth - 1),
                 CONCRETE["white"])
        self.set(x + length // 2, CAR_Y + 11, z + facing * (depth // 2), LANTERN)

    def pit_building(self, x, z, length=22, depth=6, facing=-1):
        for i in range(length):
            for d in range(depth):
                self.set(x + i, FOUND_Y, z + facing * d, SMOOTH)
                self.set(x + i, ROAD_Y, z + facing * d, PIT if d == 0 else SMOOTH)
        self.box(x, CAR_Y, z, x + length - 1, CAR_Y + 3, z + facing * (depth - 1), QUARTZ)
        for i in range(1, length - 1):
            if i % 3 == 0:
                continue
            self.box(x + i, CAR_Y + 1, z + facing * (depth - 1),
                     x + i, CAR_Y + 2, z + facing * (depth - 1), GLASS)
        self.box(x, CAR_Y + 4, z - (0 if facing > 0 else depth - 1), x + length - 1,
                 CAR_Y + 4, z + facing * (depth - 1), CONCRETE["gray"])
        for i in range(0, length, 4):
            self.set(x + i, CAR_Y + 3, z + facing * (depth - 1), LANTERN)
        for i in range(1, length, 3):      # pit wall between track and pits
            self.set(x + i, CAR_Y, z + facing * -1, BARRIER)
            self.set(x + i, CAR_Y + 1, z + facing * -1, BARRIER)

    def light_poles(self, center, every=44, offset=6, height=5):
        for i in range(0, len(center), every):
            n = len(center)
            nx, nz = center[(i + 1) % n]
            px, pz = center[(i - 1) % n]
            tx, tz = nx - px, nz - pz
            tl = math.hypot(tx, tz) or 1.0
            perp = (-tz / tl, tx / tl)
            for side in (-1,):
                x = int(round(center[i][0] + perp[0] * offset * side))
                z = int(round(center[i][1] + perp[1] * offset * side))
                if self.has(x, CAR_Y, z) and not self.has(x, FOUND_Y, z):
                    continue
                for h in range(CAR_Y, CAR_Y + height):
                    self.set(x, h, z, IRON)
                self.set(x, CAR_Y + height, z, LANTERN)

    def trees(self, cells, rng):
        for (x, z) in cells:
            if self.has(x, CAR_Y, z) or self.has(x, CAR_Y + 1, z):
                continue
            if not self.has(x, FOUND_Y, z):
                continue
            h = 4 + rng.randint(0, 2)
            for y in range(CAR_Y, CAR_Y + h):
                self.set(x, y, z, LOG, axis="y")
            for dx in range(-2, 3):
                for dz in range(-2, 3):
                    for dy in range(-1, 2):
                        if abs(dx) == 2 and abs(dz) == 2 and dy == -1:
                            continue
                        if abs(dx) + abs(dz) + abs(dy) > 4:
                            continue
                        self.set(x + dx, CAR_Y + h + dy, z + dz, LEAVES,
                                 persistent="true", distance="1", waterlogged="false")

    def sponsor_banner(self, x, z, y=3, colors=("red", "white"), text_pattern=None):
        pattern = text_pattern or [
            "1111110", "1000000", "1111100", "1000000",
            "1000000", "1000000", "1111110",
        ]
        for row, line in enumerate(pattern):
            for col, bit in enumerate(line):
                if bit == "1":
                    self.set(x + col, y + (len(pattern) - 1 - row), z, CONCRETE[colors[0]])
                elif bit == "0" and (row + col) % 2 == 0:
                    self.set(x + col, y + (len(pattern) - 1 - row), z, CONCRETE[colors[1]])

    # -- output ----------------------------------------------------------
    def bbox(self):
        xs = [p[0] for p in self.blocks]
        ys = [p[1] for p in self.blocks]
        zs = [p[2] for p in self.blocks]
        return (min(xs), min(ys), min(zs)), (max(xs) + 1, max(ys) + 1, max(zs) + 1)

    def normalize(self):
        """Vanilla structure files cannot contain negative coordinates."""
        (mn, mx) = self.bbox()
        dx, dz = max(0, -mn[0]), max(0, -mn[2])
        if not (dx or dz):
            return
        self.blocks = {(x + dx, y, z + dz): v for (x, y, z), v in self.blocks.items()}
        self.checkpoints = [(x + dx, z + dz, yaw) for (x, z, yaw) in self.checkpoints]
        self.grid = [(x + dx, z + dz) for (x, z) in self.grid]
        self.road_cells = {(x + dx, z + dz) for (x, z) in self.road_cells}
        self.kerb_cells = {(x + dx, z + dz): v for (x, z), v in self.kerb_cells.items()}
        self.anchor = (self.anchor[0] + dx, self.anchor[1], self.anchor[2] + dz)
        self.start_line = (self.start_line[0] + dx, self.start_line[1] + dz)
        shifted = {}
        for key, value in self.pit.items():
            if isinstance(value, (list, tuple)) and len(value) == 2:
                shifted[key] = [value[0] + dx, value[1] + dz]
            elif isinstance(value, (list, tuple)) and len(value) == 3:
                shifted[key] = [value[0] + dx, value[1], value[2] + dz]
            else:
                shifted[key] = value
        self.pit = shifted

    def write(self):
        self.normalize()
        (mn, mx) = self.bbox()
        count, pal = write_structure(
            os.path.join(STRUCT_DIR, "%s.nbt" % self.key), self.blocks, mx, self.anchor
        )
        return count, pal

    def write_json(self, extra=None):
        data = {
            "name": self.name,
            "short_name": self.short_name,
            "structure": "fabricracers:tracks/%s" % self.key,
            "anchor": list(self.anchor),
            "start_line": [self.start_line[0], CAR_Y, self.start_line[1]],
            "pit": self.pit,
            "grid": [[x, CAR_Y, z] for (x, z) in self.grid],
            "checkpoints": [[x, CAR_Y, z, yaw] for (x, z, yaw) in self.checkpoints],
        }
        if extra:
            data.update(extra)
        os.makedirs(TRACK_DIR, exist_ok=True)
        with open(os.path.join(TRACK_DIR, "%s.json" % self.key), "w") as fh:
            json.dump(data, fh, indent=2)
            fh.write("\n")
        return data

    # -- preview ---------------------------------------------------------
    def render_preview(self, scale=5):
        (mn, mx) = self.bbox()
        w, h = mx[0] - mn[0] + 2, mx[2] - mn[2] + 2
        img = [[(26, 26, 30) for _ in range(w)] for _ in range(h)]

        def put(x, z, color):
            px, pz = x - mn[0] + 1, z - mn[2] + 1
            if 0 <= px < w and 0 <= pz < h:
                img[pz][px] = color

        ROAD_COLORS = {
            ASPHALT: (64, 64, 70),
            ASPHALT_WORN: (58, 58, 64),
            ROAD_LINE: (150, 148, 140),
            FINISH: (238, 236, 228),
            KERB: (226, 220, 210),
            PIT: (128, 126, 120),
            NS + "fake_ice": (170, 210, 240),
            NS + "fake_slime": (120, 190, 120),
        }
        grass = set()
        for (x, y, z), (name, props) in self.blocks.items():
            if y == FOUND_Y and name in (GRASS, COARSE, DIRT, STONE, SMOOTH, QUARTZ, PLANKS):
                grass.add((x, z))
        for (x, z) in grass:
            put(x, z, (48, 78, 44))
        for (x, z) in self.road_cells:
            put(x, z, (64, 64, 70))
        for (x, y, z), (name, props) in self.blocks.items():
            if y != ROAD_Y:
                continue
            if name == KERB:
                put(x, z, (226, 220, 210) if props.get("color") == "white" else (204, 58, 52))
            elif name == FINISH:
                put(x, z, (240, 238, 230) if (x + z) % 2 else (40, 40, 46))
            elif name in ROAD_COLORS:
                put(x, z, ROAD_COLORS[name])
        for (x, z, _yaw) in self.checkpoints:
            put(int(x), int(z), (86, 232, 246))
        for (x, y, z), (name, props) in self.blocks.items():
            if y != CAR_Y:
                continue
            if name == CHECKPOINT:
                put(x, z, (86, 232, 246))
            elif name == BARRIER:
                put(x, z, (188, 60, 52))
            elif name == GATE:
                put(x, z, (250, 210, 70))
            elif name == STATION:
                put(x, z, (255, 120, 240))
        for (x, z) in self.grid:
            for dx in (0, 1):
                for dz in (0, 1):
                    put(x + dx, z + dz, (90, 220, 120))
        if self.start_line != (0, 0):
            for dz in range(-4, 5):
                for dx in range(-1, 2):
                    if (dx + dz) % 2 == 0:
                        put(self.start_line[0] + dx, self.start_line[1] + dz, (250, 250, 250))
        put(self.anchor[0], self.anchor[2], (255, 160, 40))

        canvas = Png(w * scale, h * scale)
        for y in range(h * scale):
            for x in range(w * scale):
                canvas.set(x, y, img[y // scale][x // scale])
        os.makedirs(PREVIEW_DIR, exist_ok=True)
        path = os.path.join(PREVIEW_DIR, "%s.png" % self.key)
        canvas.save(path)
        return path, (w, h)


class Png:
    """Minimal RGBA PNG writer."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = bytearray(w * h * 4)

    def set(self, x, y, color):
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        i = (y * self.w + x) * 4
        self.px[i:i + 4] = bytes(color[:3]) + b"\xff"

    def save(self, path):
        raw = b""
        for y in range(self.h):
            raw += b"\x00" + bytes(self.px[y * self.w * 4:(y + 1) * self.w * 4])
        png = b"\x89PNG\r\n\x1a\n"
        png += _png_chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        png += _png_chunk(b"IDAT", zlib.compress(raw, 9))
        png += _png_chunk(b"IEND", b"")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as fh:
            fh.write(png)


def _png_chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(
        ">I", zlib.crc32(tag + data) & 0xFFFFFFFF
    )


def yaw_for_offset(dx, dz):
    """Minecraft yaw for a direction vector (0 = south/+z, 90 = west, 180 = north, 270 = east)."""
    return math.degrees(math.atan2(-dx, dz)) % 360.0
