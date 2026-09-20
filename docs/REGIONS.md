# The Wider Kingdom — eight regions (2.0.0-alpha.2)

Milestone B grows the Null Realm from four routed biomes to **eight**, each with its own
terrain shaping, surface material, fog, particles, ambient loop, spawn table and authored
landmark. Everything here was generated, contract-tested and server-smoke-tested without a
connected game client.

## What was added

- **Eight routed biomes.** Climate bands in the multi-noise source now tile the whole axis:
  Grove, Veil Highlands, Luminous Fen, Drowned Stacks, Prism Wastes, Shard Spires,
  Cinder Steps, Ember Vents.
- **Per-region terrain shaping.** The noise router gained climate-masked terms:
  quantized **terraces** in the Stacks, vanilla **steep ridges** gated to the Spires, a
  **lifted plateau** (+~24 blocks of density) for the Highlands and **damped basins/strata**
  for the Vents — distinct silhouettes, not recoloured flats.
- **New region materials** (original generated textures, full block assets, mineable tags,
  loot): Brinesilt, Veilstone, Vent Basalt, Spire Crystal, Oxidized Trim.
- **Region signatures feature.** One chunk-contained feature switches on the surface block:
  drowned **colonnades + slab walkways + brine pools** (Stacks), **floating veil islands**
  with hanging roots and a hushwood crown (Highlands), tapering **crystal spires** with
  amethyst crowns (Spires), basalt **chimneys around lava pockets** with magma rims and
  soul fire (Vents). It never starts inside the sanctuary reservation.
- **The Drowned Archive** (Stacks only, ~1/34 chunks): a 17×17 flooded reading hall —
  deepslate walls with blue-glass windows, oxidized-copper roof, dry entrance corridor and
  patrol ring, slab walkway above the water, drowned desks and the vault below it, drowned
  + Rift Sentinel spawners (deferred NBT), Waystone at the entrance, and the **Tide Bell**
  on the central dais.
- **One-shot tide drain.** Ringing the bell turns the hall's water (bell−5 … bell−2,
  radius 5) to air **exactly once**; the `drained` state lives in the block state and
  survives restarts. The bell verifies every chunk in the box is loaded and the player may
  modify the block before touching anything, and it says so plainly if not.
- **Progression:** the vault's **Drowned Seal** crafts (with Prism Lamp + Resonite Ingot)
  the **Tide Lantern** — 40s Water Breathing + 20s Night Vision, 90s cooldown, stack of one.
- **Veil Wisp:** passive ambient mote (Highlands + Spires creature spawns, weight 12) —
  no gravity, sine drift, original 32×32 texture and model, spawns only on veilstone or
  prismstone, never breeds, empty loot table.
- **Journal:** *Eight Regions* and *Royal Seals* pages; the seals thread names two seals
  and leaves two more for later milestones.

## Save behaviour

Changing climate bands reshapes routing for **new chunks only**. Already-generated terrain,
the sanctuary, the monastery and existing player records are untouched. Upgrade in place;
explore outward for the new regions.

## Verified automatically

- **Python (33):** band tiling across the whole axis, per-biome surface rules, signatures
  only in the four new biomes, archive only in the Stacks, drowned/wisp spawn gates, full
  asset+loot+lang coverage for the five new blocks, tide-bell drained variants resolving to
  real textures, archive/tide message keys (including the `%s` drain count), lantern recipe
  + advancement, journal pages, deterministic regeneration.
- **JUnit (45):** `ArchiveRules` drain-zone geometry (radius 5, bell−5…bell−2, hall square)
  plus all previous suites.
- **Dedicated-server smoke (~52 commands):** places each region signature on a flat
  platform of its own surface block and asserts the deterministic results (pool water and
  rim, island at surface+24 with roots and crown, crystal column, lava pocket with magma
  rim and soul fire); places a full Archive and asserts bell state, water, walkway slab,
  dais trim, wall/window/roof blocks, entrance air, desk, waystone, both spawner IDs and
  the vault loot table via `data get`; summons the wisp and gives/loot-spawns the seal,
  lantern and archive cache.

## Known gaps (not verified)

- No connected client: drain feel, island sightlines, terrace readability, wisp animation
  and all fog/particle choices are unseen by a human.
- The bell drain is not exercised interactively by CI (the block state and geometry are).
- Natural spacing/frequency of the Archive (~1/34 chunks) and signature density (1/2)
  are unmeasured in real exploration.
- The Ashen Foundry (third seal) and cross-region seal gating are **Milestone C**.
