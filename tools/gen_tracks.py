#!/usr/bin/env python3
"""
FabricRacers - stock track generator.

    python3 tools/gen_tracks.py

Builds the five stock circuits as vanilla structure files
(src/main/resources/data/fabricracers/structure/tracks/*.nbt), together with
their definitions (data/fabricracers/tracks/*.json) and top-down preview maps
(tools/previews/*.png) used for design review.

Design rules:
  * canonical design space, track starts heading EAST (+x) from the anchor
  * everything lives at x >= 0, z >= 0 so the files are vanilla-legal
  * local y: 0 = foundation, 1 = road surface, 2 = car level, 3+ = scenery
"""

import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from track_lib import (  # noqa: E402
    ASPHALT, BARRIER, CAR_Y, CHECKPOINT, CONCRETE, COARSE, Design, DIRT, FINISH,
    FOUND_Y, GATE, GLASS, GRASS, IRON, LANTERN, LEAVES, LOG, NS, PIT, PLANKS,
    QUARTZ, ROAD_LINE, ROAD_Y, SMOOTH, STATION, STONE, WOOL, yaw_for_offset,
)

LAPS = {"asphalt_park": 2, "kart_circuit": 3, "speed_oval": 2,
        "chaos_junction": 2, "drift_arena": 3}


def place_grid(design, center, arc, slots=12, spacing=3.0, start_s=3.5, lane=1.7):
    """Staggered 2-lane grid on the start straight."""
    total = len(center)
    for i in range(slots):
        s = start_s + spacing * i
        idx = 0
        for j in range(total):
            if arc[j] >= s:
                idx = j
                break
        cx, cz = center[idx]
        nx, nz = center[(idx + 1) % total]
        px, pz = center[(idx - 1) % total]
        tx, tz = nx - px, nz - pz
        tl = math.hypot(tx, tz) or 1.0
        perp = (-tz / tl, tx / tl)
        off = lane if i % 2 == 0 else -lane
        x = int(round(cx + perp[0] * off))
        z = int(round(cz + perp[1] * off))
        design.grid.append((x, z))
    return design.grid


def dist_to_grid(cx, cz, grid_cells):
    return min(math.hypot(cx - x, cz - z) for (x, z) in grid_cells)


def build_track(key, name, short_name, waypoints, width, groups, laps=None,
                cp_count=10, grid_slots=12, grandstands=(), extra=None,
                pit_spec=None, tree_density=0.05, seed=7):
    design = Design(key, name, short_name)
    center, arc = design.stamp_road(waypoints, width=width)
    total = len(center)
    lap_len = arc[-1] + math.hypot(center[0][0] - center[-1][0], center[0][1] - center[-1][1])
    rng = random.Random(seed)

    # --- grid ------------------------------------------------------------
    place_grid(design, center, arc, slots=grid_slots)
    design.anchor = (design.grid[0][0], CAR_Y, design.grid[0][1])

    # --- start / finish line --------------------------------------------
    half = (width - 1) // 2
    line_s = 3.5 + 3.0 * grid_slots + 3.5
    line_idx = min(range(total), key=lambda j: abs(arc[j] - line_s))
    design.paint_line_across(center, line_idx, ASPHALT, checker=True, span=half)
    cx, cz = center[line_idx]
    design.start_line = (int(round(cx)), int(round(cz)))
    design.gantry(int(round(cx)), int(round(cz)), span=half + 3,
                  beam_y=CAR_Y + 4)

    # --- checkpoints (index 0 == the start line) -------------------------
    design.checkpoint(center, arc, line_idx, None)
    for k in range(1, cp_count):
        s = (line_s + k * (lap_len - 1.0) / cp_count) % lap_len
        idx = min(range(total), key=lambda j: abs(arc[j] - s))
        design.checkpoint(center, arc, idx, None)

    # --- pit lane --------------------------------------------------------
    if pit_spec is not None:
        build_pit(design, pit_spec, center, arc, line_idx, width)

    # --- decoration ------------------------------------------------------
    for (kind, args, pos, kwargs) in groups:
        if kind == "grandstand":
            design.grandstand(*args, **kwargs)
        elif kind == "banner":
            design.sponsor_banner(*args, **kwargs)
        elif kind == "box":
            design.box(*args)
        elif kind == "hollow":
            design.hollow_box(*args)
        elif kind == "set":
            design.set(*args)
    design.light_poles(center, every=max(30, int(lap_len / 6)))
    candidates = [
        (x, z) for (x, y, z), (nm, _p) in design.blocks.items()
        if y == FOUND_Y and nm in (GRASS, COARSE)
        and not design.has(x, CAR_Y, z) and not design.has(x, CAR_Y + 1, z)
    ]
    rng.shuffle(candidates)
    design.trees(candidates[:int(len(candidates) * tree_density)], rng)

    # --- JSON ------------------------------------------------------------
    count, pal = design.write()
    extra_json = {"width": width, "default_laps": laps or LAPS.get(key, 2),
                  "lap_length": int(lap_len)}
    if extra:
        extra_json.update(extra)
    data = design.write_json(extra_json)
    path, size = design.render_preview()
    print("%-16s blocks=%-6d palette=%-3d size=%s laps=%s cps=%d grid=%d bbox_px=%s"
          % (key, count, pal, design.bbox()[1], extra_json["default_laps"],
             len(design.checkpoints), len(design.grid), size))
    print("                 json=%s preview=%s" % (os.path.relpath(
        os.path.join("src", "main", "resources", "data", "fabricracers", "tracks",
                     "%s.json" % key)), os.path.relpath(path)))
    return design


def build_pit(design, spec, center, arc, line_idx, width):
    """A pit lane parallel to the main straight, merging with it at both ends."""
    x0, x1, z_off = spec
    main_z = center[line_idx][1]
    pit_z = main_z + z_off
    lane_half = 2                       # 4-wide lane

    # waypoints: leave the straight, run parallel, rejoin further down
    wp = [
        (x0 - 10.0, main_z + 1.0),
        (x0 - 4.0, main_z + 2.0),
        (x0, pit_z - 0.5),
        (x0 + 6.0, pit_z),
        ((x0 + x1) / 2.0, pit_z),
        (x1 - 6.0, pit_z),
        (x1, pit_z - 0.5),
        (x1 + 5.0, main_z + 2.0),
        (x1 + 11.0, main_z + 1.0),
    ]
    design.stamp_path(wp, width=lane_half * 2, surface=PIT)

    # open the barrier wherever the pit lane touches the main straight
    pit_cells = design.cells_with(ROAD_Y, PIT)
    for (x, z) in pit_cells:
        for dz in (-1, 0, 1):
            design.clear_column(x, z + dz, (CAR_Y, CAR_Y + 1))
            design.clear_column(x + 1, z + dz, (CAR_Y, CAR_Y + 1))
    # ...and across the whole merge aprons
    for x in list(range(x0 - 11, x0 + 8)) + list(range(x1 - 7, x1 + 12)):
        for dz in range(0, z_off + 3):
            design.clear_column(x, int(main_z) + dz, (CAR_Y, CAR_Y + 1))

    # low pit wall on the track side of the lane, with gaps at both ends
    xs = sorted(set(x for (x, _z) in pit_cells))
    for x in xs:
        if x < x0 + 2 or x > x1 - 2:
            continue                      # merge aprons stay open
        if x % 9 == 0:
            continue                      # crew access gaps
        zz = min(z for (xx, z) in pit_cells if xx == x)
        design.set(x, CAR_Y, int(main_z) + z_off - lane_half - 1, BARRIER)
        design.set(x, CAR_Y + 1, int(main_z) + z_off - lane_half - 1, BARRIER)

    # pit building + garage doors behind the lane
    build_max = max(z for (_x, z) in pit_cells)
    px0, px1 = xs[0] + 1, xs[-1] - 1
    design.pit_building(px0 + 1, build_max + 4, length=max(10, px1 - px0 - 1), depth=5,
                        facing=-1)

    # pit box marker (the "station" block) in the middle of the lane
    bx = (px0 + px1) // 2
    bz = min(z for (x, z) in pit_cells if x == bx)
    design.set(bx, ROAD_Y, bz, STATION)
    design.clear_column(bx, bz, (CAR_Y, CAR_Y + 1))
    design.pit = {
        "entry": [int(x0), int(pit_z)],
        "exit": [int(x1), int(pit_z)],
        "box": [int(bx), ROAD_Y, int(bz)],
    }


# ---------------------------------------------------------------------------
# the circuits
# ---------------------------------------------------------------------------
def asphalt_park():
    wp = [
        (16, 6), (60, 6),                 # main straight
        (72, 12), (78, 24),               # T1
        (78, 46),                         # back straight
        (74, 58), (62, 66),               # T2
        (38, 66),                         # second straight
        (26, 62), (22, 52),               # T3
        (22, 38),                         # infield straight (north)
        (24, 28), (18, 24),               # T4 left
        (10, 27), (6, 20),                # T5 hairpin kit
        (8, 12), (14, 7),                 # T6 back onto the start straight
    ]
    groups = [
        ("grandstand", (86, 30), {}, {"length": 16, "depth": 6, "facing": 1}),
        ("grandstand", (34, 74), {}, {"length": 14, "depth": 5, "facing": -1}),
        ("banner", (20, 1), {}, {"y": CAR_Y + 2, "colors": ("red", "white")}),
        ("banner", (58, 73), {}, {"y": CAR_Y + 2, "colors": ("blue", "white")}),
        # paddock buildings behind the pit lane
        ("box", (30, CAR_Y, 30, 40, CAR_Y + 4, 38, QUARTZ), {}, {}),
        ("hollow", (30, CAR_Y + 1, 30, 40, CAR_Y + 2, 38, GLASS), {}, {}),
        ("set", (35, CAR_Y + 5, 34, LANTERN), {}, {}),
    ]
    return build_track("asphalt_park", "Asphalt Park", "ASP", wp, 7,
                       groups, laps=2, cp_count=11, grid_slots=12,
                       pit_spec=(24, 60, 9))


def kart_circuit():
    wp = [
        (10, 6), (30, 6),                 # start straight
        (38, 9), (41, 16),                # T1
        (39, 23),                         # T2
        (32, 27), (26, 24), (20, 27),     # chicane
        (13, 23),                         # T3
        (9, 16), (11, 9),                 # T4 + merge
    ]
    groups = [
        ("grandstand", (16, 1), {}, {"length": 10, "depth": 4, "facing": 1}),
        ("banner", (30, 34), {}, {"y": CAR_Y + 2, "colors": ("yellow", "black")}),
        ("box", (46, CAR_Y, 10, 50, CAR_Y + 3, 14, SMOOTH), {}, {}),
        ("set", (48, CAR_Y + 4, 12, LANTERN), {}, {}),
    ]
    return build_track("kart_circuit", "Kart Circuit", "KRT", wp, 5,
                       groups, laps=3, cp_count=8, grid_slots=8,
                       tree_density=0.08, seed=11)


def speed_oval():
    wp = [
        (16, 8), (84, 8),                 # long main straight
        (94, 12), (100, 22),              # T1 sweep
        (100, 50),                        # long back straight (south)
        (94, 62), (84, 68),               # T2 sweep
        (30, 68),                         # second straight (west)
        (16, 64), (8, 52),                # T3
        (8, 20),                          # long straight north
        (16, 8),                          # T4: back onto the main straight
    ]
    groups = [
        ("grandstand", (30, 78), {}, {"length": 24, "depth": 6, "facing": -1}),
        ("grandstand", (30, 1), {}, {"length": 24, "depth": 6, "facing": 1}),
        ("banner", (60, 1), {}, {"y": CAR_Y + 2, "colors": ("green", "white")}),
        ("banner", (70, 78), {}, {"y": CAR_Y + 2, "colors": ("black", "yellow")}),
        ("box", (40, CAR_Y, 24, 52, CAR_Y + 5, 34, QUARTZ), {}, {}),
        ("hollow", (40, CAR_Y + 1, 24, 52, CAR_Y + 3, 34, GLASS), {}, {}),
        ("set", (46, CAR_Y + 6, 29, LANTERN), {}, {}),
    ]
    return build_track("speed_oval", "Bonneville Oval", "OVL", wp, 11,
                       groups, laps=2, cp_count=8, grid_slots=10,
                       tree_density=0.03, seed=23)


def chaos_junction():
    wp = [
        (16, 36), (34, 36),                # start straight (crossed later!)
        (48, 32), (56, 22),                # up to the top lobe
        (50, 12), (34, 10),                # top
        (22, 18), (20, 28),                # coming back down (left side)
        (28, 38),                          # THE crossing
        (40, 50), (52, 58),                # into the bottom lobe
        (54, 70), (42, 76),                # bottom
        (28, 74), (18, 66),                # bottom left
        (12, 54), (12, 44),                # left side heading north
        (16, 36),                          # merge back onto the start straight
    ]
    groups = [
        ("grandstand", (46, 84), {}, {"length": 16, "depth": 5, "facing": -1}),
        ("banner", (40, 4), {}, {"y": CAR_Y + 2, "colors": ("orange", "white")}),
        ("banner", (6, 52), {}, {"y": CAR_Y + 2, "colors": ("purple", "white")}),
        ("set", (28, CAR_Y + 3, 38, IRON), {}, {}),
        ("set", (28, CAR_Y + 4, 38, LANTERN), {}, {}),
        ("box", (60, CAR_Y, 32, 66, CAR_Y + 3, 38, SMOOTH), {}, {}),
        ("set", (63, CAR_Y + 4, 35, LANTERN), {}, {}),
    ]
    return build_track("chaos_junction", "Chaos Junction", "CHS", wp, 7,
                       groups, laps=2, cp_count=10, grid_slots=10,
                       tree_density=0.06, seed=31)


def drift_arena():
    """An open skid pad with a painted ring, slalom stripes and tyre stacks."""
    design = Design("drift_arena", "Drift Arena", "DRF")
    w, d = 60, 44
    for x in range(0, w):
        for z in range(0, d):
            design.set(x, FOUND_Y, z, COARSE if (x + z) % 9 == 0 else GRASS,
                       **({"snowy": "false"} if (x + z) % 9 else {}))
    for x in range(1, w - 1):
        for z in range(1, d - 1):
            design.road_cells.add((x, z))
            design.set(x, ROAD_Y, z, ASPHALT)
    for x in range(w):
        for z in (0, d - 1):
            design.set(x, CAR_Y, z, BARRIER)
            design.set(x, CAR_Y + 1, z, BARRIER)
    for z in range(d):
        for x in (0, w - 1):
            design.set(x, CAR_Y, z, BARRIER)
            design.set(x, CAR_Y + 1, z, BARRIER)
    # opening in the north wall (entrance)
    for x in range(24, 34):
        for y in (CAR_Y, CAR_Y + 1):
            if (x, y, 0) in design.blocks:
                del design.blocks[(x, y, 0)]

    # painted donut rings
    for (cx, cz, r, block) in ((22, 22, 13, FINISH), (44, 22, 8, ROAD_LINE)):
        for x in range(w):
            for z in range(d):
                radius = math.hypot(x - cx, z - cz)
                if abs(radius - r) < 0.6:
                    design.set(x, ROAD_Y, z, block if (x + z) % 2 else ASPHALT)
    # slalom stripes
    for i in range(6):
        x = 30 + i * 4
        for z in range(6, d - 6):
            if i % 2 == 0:
                design.set(x, ROAD_Y, z, ASPHALT)
            else:
                design.set(x, ROAD_Y, z, PIT if z % 3 else ASPHALT)
    # tyre stacks
    for (tx, tz) in ((14, 12), (14, 32), (34, 12), (34, 32), (52, 16), (52, 28)):
        for y in range(CAR_Y, CAR_Y + 3):
            design.set(tx, y, tz, CONCRETE["black"])
        design.set(tx, CAR_Y + 3, tz, CONCRETE["white"])
    # grid along the west side, heading south->then east loop
    for i in range(8):
        design.grid.append((12 + (i % 2) * 3, 6 + i * 2))
    design.anchor = (design.grid[0][0], CAR_Y, design.grid[0][1])
    # a simple ring for laps: checkpoints around the donut
    design.start_line = (18, 10)
    for i in range(8):
        angle = i * 45.0
        cx = 30 + 22 * math.cos(math.radians(angle))
        cz = 22 + 16 * math.sin(math.radians(angle))
        design.checkpoints.append((round(cx, 1), round(cz, 1),
                                   round((90 - angle) % 360, 1)))
    design.gantry(30, 0, span=6, beam_y=CAR_Y + 4)
    rng = random.Random(5)
    design.light_poles([(30 + 24 * math.cos(math.radians(a)), 22 + 18 * math.sin(math.radians(a)))
                        for a in range(0, 360, 60)], every=1, offset=0, height=5)
    count, pal = design.write()
    data = design.write_json({"width": 1, "default_laps": LAPS["drift_arena"],
                              "arena": True})
    path, size = design.render_preview()
    print("%-16s blocks=%-6d palette=%-3d size=%s laps=%s cps=%d grid=%d bbox_px=%s"
          % ("drift_arena", count, pal, design.bbox()[1], data["default_laps"],
             len(design.checkpoints), len(design.grid), size))
    print("                 json=%s preview=%s" % (
        os.path.relpath(os.path.join("src", "main", "resources", "data",
                                     "fabricracers", "tracks", "drift_arena.json")),
        os.path.relpath(path)))
    return design


def write_index(designs):
    """Writes _index.json: the list of stock circuits shipped inside the jar."""
    import json
    path = os.path.join(TRACK_DIR if "TRACK_DIR" in globals() else
                        os.path.join("src", "main", "resources", "data",
                                     "fabricracers", "tracks"), "_index.json")
    payload = {"tracks": sorted(design.key for design in designs)}
    with open(path, "w") as fh:
        json.dump(payload, fh, indent=2)
        fh.write("\n")
    print("index -> %s (%s)" % (path, ", ".join(payload["tracks"])))


if __name__ == "__main__":
    write_index([asphalt_park(), kart_circuit(), speed_oval(), chaos_junction(),
                 drift_arena()])
