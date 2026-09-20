# 2.0 alpha — The Kingdom Beyond the Gate

Milestone A of the 2.0 plan: a **generated dungeon with its own keeper**, a gateway that now
*awakens* instead of snapping open, and a realm-side counterpart of the Ancient City monument.
Everything from 1.8 remains unchanged, including the 22×8 city frame matcher.

This document is the implementation and verification record. See [TESTING.md](TESTING.md) for
what is still unperformed.

---

## 1. The three-second awakening

`VoidPortalManager.tryIgnite` no longer opens a gate immediately. It registers an `Opening`
(world, frame, player UUID, hand, age) keyed by the frame origin's `GlobalPos`:

| Stage | Behavior |
| --- | --- |
| Use an Echo Shard on a valid frame block | Nothing is written, nothing is consumed. `message.myfirstmod.gate_charging` |
| Every 4 ticks | Reverse-portal particles crawl up both frame posts, `upto = 1 + age * 22 / 60` blocks high |
| Every 20 ticks | Rising respawn-anchor charge sound (`.5 + age/80` pitch) |
| Age 60 (3 s) | The full 20×6 aperture opens; the shard is consumed in Survival |
| Player moves 32+ blocks away, leaves the world, dies, or swaps the shard out of that hand | The opening is cancelled, `message.myfirstmod.gate_cancelled`, **no shard is consumed** |
| Frame becomes invalid before completion | Re-validation runs; failure cancels and refunds nothing because nothing was spent |

Constraints: at most 16 concurrent awakenings (oldest are not evicted; new ones are refused),
entries are removed on server stop, and completion re-runs every permission check that
`tryIgnite` already performed — protected or obstructed apertures still refuse to open.

The `/nullgate x y z` operator fixture still opens a frame immediately. That is deliberate: it
tests geometry without a player, and it is documented as bypassing the Survival checks.

## 2. The realm-side counterpart gateway

New worlds build a matching **22×8 reinforced-deepslate frame with a 20×6 portal** across the
south end of the sanctuary plaza at `z = 175`, `x = -10..11`, `y = 80..87`, axis `x`:

- The plaza grows from 21×21 to 29×29 (`x -14..14`, `z 150..178`) and clears up to `y = 92`.
- Arrival moves to **(0, 81, 172)** so players land facing their own gateway; the recorded
  Overworld return point is unchanged and still survives restarts.
- Prism-lamp floor inlays line the causeway every four blocks; moss and leaf planters flank it.
- The hostile-suppression refuge now covers the union of the old `(0,81,160)` and new
  `(0,81,172)` arrival boxes, and only applies when `thresholdBuilt` is true for the new point.

**Old saves are not retrofitted.** `RealmState.thresholdBuilt` is written only during a first
build; worlds that already have a sanctuary keep their 1.7/1.8 arrival at `(0, 81, 160)` and
their existing small return gate, and no block in them is touched.

## 3. Rootbound Monastery — generated structure

`data/myfirstmod/structure/rootbound_monastery.nbt` is an original **47×18×47** template
(39,762 explicit blocks, DataVersion 3955) written by `scripts/generate_kingdom.py` with a
hand-rolled NBT encoder. It is not a copied vanilla structure.

| Space | Footprint (local) | Contents |
| --- | --- | --- |
| North chapterhouse | `x 14..32, z 2..20`, wall height 9, ribbed hushwood roof to `y 13` | **Rootbound Heart** at `(23,1,10)`, four root pillars, boss floor |
| West chapel | `x 2..12, z 22..40`, height 6 | **Cloister Bell** at `(7,1,26)`, Rift Sentinel spawner `(5,1,35)`, cache `(9,1,24)` |
| East library | `x 34..44, z 22..40`, height 10 | Gallery floor at `y 6`, **second bell** `(39,7,26)`, six-step two-wide stair, bookshelves, Shardstalker spawner `(42,7,24)`, cache `(36,7,30)` |
| Cloister | `x 16..30, z 24..40` | Colonnade with prism lamps, walled root garden, iron-bar screen at `z 27` |
| South approach | `z 46`, `x 21..25` | Five-wide doorway, entrance **Waystone** at `(23,1,43)` |

Victory shortcut: defeating the Prior removes the three iron bars at `(22..24, 1..3, 27)`,
linking the boss room straight to the cloister and the exit.

### Generation rules

- **Structure type:** `myfirstmod:rootbound_monastery` (`MonasteryStructure`). It delegates to
  vanilla `JigsawStructure` placement — `JigsawStructure` is `final` in 1.21.1, so it is
  composed, not extended — after refusing any start chunk inside
  `|x| < 160 && -160 < z < 256`, the fixed sanctuary approach. `getValidStructurePosition` is
  public in Yarn 1.21.1 and is reused so biome filtering stays vanilla behavior.
- **Biomes:** `#myfirstmod:has_rootbound_monastery` = Hushed Grove only.
- **Set:** `random_spread`, spacing 32, separation 16, salt 2074301 → at most one monastery per
  32×32 chunk region, at least 16 chunks from another.
- `terrain_adaptation: beard_thin`, `project_start_to_heightmap: WORLD_SURFACE_WG`; the
  structure step is `surface_structures`, which runs before `vegetal_decoration`. The template
  replaces the ground with stone bricks and polished nullstone, and realm flora only plants on
  moss/prismstone/cinderstone, so trees do not grow through the floor.
- **New chunks only.** Existing generated terrain never receives a monastery.

### The two-bell rite

Both bells carry `bells` (0..3) and `facing`; progress lives in the block state, so it survives
unloads, restarts, and template rotation (`MonasteryBlock.rotate`/`mirror`).

1. Right-click a bell → it resolves the heart 16 blocks to its left/right, 16 forward, and
   (for the library bell) 6 down, using `Monastery.relative`, which is rotation-correct.
2. `MonasteryRules.ringBell(current, bell)` ORs in `1` for the west bell and `2` for the east.
   The heart opens at `3`; already-rung bells are idempotent and never regress.
3. A rung bell reports `monastery.myfirstmod.bell`; a spent one reports `.spent`.
4. With `bells = 3`, right-clicking the heart wakes the Prior. Otherwise it repeats the riddle.

Permissions are checked with `World.canPlayerModifyAt` before any write, Peaceful is refused
with a message, and the spawn volume is verified with `isSpaceEmpty` — an obstructed heart
reports `.blocked` and writes nothing.

## 4. The Rootbound Prior

`RootboundPriorEntity` (1.4×3, altar-bound, 240 base health + 100 per extra eligible player,
capped at four, `MonasteryRules.health`). It never moves, never despawns immediately, and
discards itself if its heart is gone, its UUID no longer matches `RealmState.monasteryGuardians`,
or no eligible player has been within 48 blocks for 30 seconds.

Every attack has a 60-tick (3 s) tell with ground particles every 4 ticks, an action-bar name,
then a resolution, then a visible **65-tick recovery pause** during which the bar reads
`prior.myfirstmod.recovery`:

| Attack | Telegraph | Damage rule (`MonasteryRules`) | Safe response |
| --- | --- | --- | --- |
| 1 — Root Lash | A 10-block lane, 2.3 wide, from the heart toward the target | `lane(forward 0..9, \|side\| ≤ 1.15, \|dy\| < 3)` | Step out of the marked lane |
| 2 — Thorn Crown | Four arms, radius 3..7 | `crown(9 ≤ r² ≤ 49, min(\|x\|,\|z\|) ≤ 0.85)` | Stand in the centre pocket, in a gap between arms, or outside radius 7 |
| 2 (below half health) | The axis-aligned ring resolves **20 ticks early**, then rotated arms are shown for one second and resolve at the end of the tell | `crown(..., diagonal = false)` then `crown(..., diagonal = true)` | Centre pocket or outside radius 7 survives both pulses; arm gaps are only safe for the first |
| 3 — Seedfall | 2.2-block circles under up to four players (six when enraged) | `seed(r² ≤ 4.84, \|dy\| < 3)` | Leave your own circle |

Damage is 9, or 12 below half health, and requires `canSee`, so the four root pillars are real
partial cover. The boss is pinned to the heart each tick and its velocity is zeroed.

Persistence: `heart` is saved in the entity NBT (`MonasteryHeart`), and
`RealmState.monasteryGuardians` maps heart position → actor UUID. `Monastery.tick` re-checks
that pairing once per second for nearby players and clears a stale entry after **six** seconds
of the actor being absent, so a `/kill`ed or unloaded Prior does not permanently lock the heart.
Entries are only evaluated while a player is within 48 blocks, and everything is cleared on
server stop.

On death the heart becomes the **Rootbound Reliquary** (facing preserved), the guardian entry is
removed, and the cloister screen opens.

## 5. Reward: one seal, one weapon

The reliquary is consumed on use and grants the **Rootbound Seal** (shared, one per monastery —
the same contract the cathedral reliquary already uses), 3 Resonite Ingots, 1 Dusk Fiber and
250 XP.

**Briarbrand** (`BriarbrandItem`, 1,800 durability, fireproof, epic): hold one second, release a
narrow cone sweep — 4 blocks, `dot ≥ 0.65`, 9 damage, Slowness III for 3 s, one heart healed on
a successful hit, 3 durability, 10-second cooldown, hostile-only targeting that skips teammates
and passive mobs. Base melee is 7 damage at 1.6 attacks/second. It is attunable at the
Attunement Forge like the other active relics (`RelicAttunements.cooldown/afterCast`), so Focus
shortens its cooldown and Vigor/Gale apply on cast.

Recipe: two Resonite Ingots above a Dusk Fiber + **Rootbound Seal** + Hushwood, unlocked by
picking up a seal. The seal has no other use and cannot be farmed: each monastery holds one.

## 6. Hushed Grove identity

`RealmScenery` now raises a **root arch** on a quarter of grove chunks whose centre is natural
hushed moss: two 5-tall hushwood trunks at `x ± 5`, a spanning beam, and a 11×5 leaf canopy.
The arch is only built when every column in an 13×5 neighbourhood is natural moss within two
blocks of the centre height, and every write stays inside the source chunk, so it cannot tear
ravines, landmark roofs, or neighbouring chunks.

## 7. Assets and documentation

- Original 16×16 block textures and 16-variant blockstates for the heart, bell and reliquary
  (lit states read from `bells`).
- Original 32×32 item sprites for the seal and the Briarbrand, a 128×128 bark/antler entity
  texture, and `RootboundPriorModel` — a split wooden mask, antler branches, bark robe and two
  root feet — with a matching renderer and model layer.
- Journal page `journal.myfirstmod.monastery`, all boss/bell/reward messages, and both
  awakening messages are translated; the journal registers the page after the gateway page.
- `scripts/generate_gallery.py` now shows 26 item textures and six creature previews, and the
  gateway plate documents the hold-to-awaken behavior and the realm-side counterpart.
- `docs/images/*.svg` remain generated, deterministic, labeled previews — **not screenshots**.

## 8. Validation status

Automated: 23 Python resource tests (including a from-scratch NBT decoder that re-reads the
monastery template and asserts every puzzle block, stair headroom, spawner actor, chest loot
table, open boss lane, crown arm and pillar position), 42 JUnit methods (7 new for bell
combination, party scaling, all three damage boundaries, both bell→heart rotations in all four
facings, and the sanctuary exclusion), plus a dedicated-server smoke test that forceloads a
5×5 chunk area, places the real template, asserts 17 blocks and both spawner/chest NBT payloads,
spawns both loot tables, summons the Prior and both new items, and executes
`/place structure myfirstmod:rootbound_monastery` through the custom structure type.

Not performed: no connected client, so no human has walked the monastery, rung a bell, fought
the Prior, claimed the seal, crafted the Briarbrand, or seen the awakening particles. Structure
*spacing* in natural generation, temple-to-temple travel time, difficulty balance, and
multiplayer behavior are unverified. Placement is asserted only where `/locate biome` finds an
actual grove chunk; a declined placement is logged, not failed.
