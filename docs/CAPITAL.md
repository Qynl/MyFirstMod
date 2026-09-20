# The Silent Capital — coronation (2.0.0-alpha.4)

Milestone D closes the 2.0 kingdom: one unique capital, one final gate, one last seal.
Everything below is generated, contract-tested and server-smoke-tested without a connected
game client.

## What was added

- **The Silent Capital:** a **unique** 63×63 citadel at **(0, -352)** in the Null Realm.
  `CapitalStructure` is a custom structure type that only ever returns a start position in
  chunk **(0, -22)**, then delegates to vanilla jigsaw for the template drop (rigid,
  `beard_thin` adaptation). Layout: crenellated ring walls, south gate gap, lamp-lined
  boulevard, two veil gardens with bushes and prism lamps, a plaza ring with benches, a
  throne dais with seat/back/arms, a colonnaded palace (two column rows under a polished
  nullstone roof over prismstone floors), a sentinel spawner in the palace, and a Waystone
  just inside the gate.
- **The Crown Gate** (`phase` 0/1/2, luminance rises with phase): sealed by default. Using
  it with **Rootbound + Drowned + Cinder attuned** opens the court; using it afterwards only
  reports the session or the verdict. Phase persists in the block state.
- **The Silent Court:** three sessions — 2 sentinels; 2 shardstalkers + 1 herald; then
  4 mixed actors — spawned at fixed plaza offsets around the gate. `CourtOfSeals` tracks
  actor UUIDs in `RealmState` (bounded to 16), advances sessions when none are alive, and
  on completion sets phase 2 and places the **throne treasury chest** (loot
  `crown_cache`: Crown Seal, resonite blocks, memory shards, echo shard) on the dais.
  One treasury per world.
- **Crown Seal:** fourth attunable seal (`Seals` bitmask now 4 bits; plinth counts 0–4 with
  a fifth texture). Using it in hand attunes it like the others.
- **Crown of the Silent Court** (Crown Seal + Resonite Block, shapeless; helmet): while
  worn, `fall` damage is cancelled (ALLOW_DAMAGE) and Night Vision refreshes every second
  while in the Null Realm. Falls stop being tax; the dark stops being blind.
- **Journal:** *Silent Capital* (with the coordinates) and *Coronation* pages.

## Save behaviour

The capital appears when chunk (0, -22) is first generated — **existing terrain never
changes**. Court state (`CourtPos`, `CourtWave`, `CourtActors`) and the fourth seal bit are
additive fields with safe defaults for old saves. An interrupted court resumes from its
persisted wave on reload; orphaned actor IDs are simply treated as dead.

## Verified automatically

- **Python (48):** the template NBT is decoded in tests — size 63×10×63, palette contents,
  gate at (31,1,1) with `phase=0`, waystone, throne back, palace roof, colonnade column and
  palace spawner NBT; structure/pool/set JSONs and the 8-biome tag; the structure type's
  fixed-chunk guard; gate block-state textures; court gating/phase/treasury source
  contracts; `RealmState` court persistence; crown item/recipe/tooltip; journal pages.
- **JUnit (51):** `CourtRules` session sizes (2/3/4), actor IDs and plaza offset bounds,
  plus all previous suites.
- **Dedicated-server smoke (~78 commands):** `/place template` of the full capital at a
  test platform asserting gate phase, waystone, throne, roof, column and spawner ID via
  `data get`; gives/loot-spawns crown seal, crown and crown cache.

## Known gaps (not verified)

- No connected client: the capital's silhouette on the horizon, the court's feel and the
  crown's wear are unseen by a human; CI never opens the gate or fights a session.
- The fixed chunk may sit in any climate band — the capital is a ruin in whatever region
  grew around it; that is intended, but unmeasured visually.
- Multiplayer court etiquette (shared treasury, late arrivals) is untested beyond the
  single-player smoke path.
