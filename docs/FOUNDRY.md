# The Ashen Foundry — the chain of seals (2.0.0-alpha.3)

Milestone C completes the dungeon triad and wires the three dungeons together through the
royal seals. Everything below is generated, contract-tested and server-smoke-tested without
a connected game client.

## What was added

- **The Ashen Foundry** (`cinder_steps`, ~1/30 chunks, new chunks only): a 19×17-wall forge
  hall — blackstone brick walls with a magma band at eye level, magma-seamed floor, blast
  furnaces and anvils, two lava channels running beside the walk, a dry entrance corridor,
  magma-cube and Rift Sentinel spawners (deferred NBT), a Waystone at the door, and the
  **Ember Crucible** raised on a dais **inside a 5×5×3 shell of standing fire**.
- **The Quench (one-way):** using the crucible collapses the fire shell to air and cools both
  lava channels to walkable **Slagglass**, opening the vault inside (Cinder Seal, resonite,
  cinder pearls, slagglass, memory shard). `quenched` lives in the block state: it survives
  unloads and restarts, and a quenched crucible only answers *"The quench holds."*
- **Gating:** the crucible answers only to a player whose ledger carries the **Rootbound and
  Drowned** seal attunements. It verifies every chunk of the hall is loaded and that the
  player may modify the block before touching anything.
- **Seal chain:** using any royal seal in hand attunes it **permanently** to that player's
  expedition record (`Seals` bitmask, persisted). Quenching attunes the **Cinder Seal**.
- **Sanctuary Seal Plinth:** on the world's first attunement a plinth is placed at the first
  empty designated plaza spot (never over builds) and thereafter displays the world's seal
  count 0–3 with rising luminance. Its position persists in `RealmState`.
- **Cinderwalk Charm** (Cinder Seal + Slagglass + Resonite Ingot, shapeless): while carried
  anywhere in the inventory, `hot_floor`, `in_fire` and `on_fire` damage are cancelled and
  existing fire is extinguished every two seconds. The vents become terrain, not a threat.
- **Cinder Steps signature:** basalt chimneys topped with smoking campfires plus slag piles,
  so all five young regions carry an authored silhouette.
- **Journal:** *Ashen Foundry* and *Chain of Seals* pages; the seals page now leaves one
  seal sleeping for the next milestone.

## Save behaviour

No existing geometry changes. The foundry and chimneys appear only in **new chunks**;
attunement and plinth state are additive fields with safe defaults for old saves.

## Verified automatically

- **Python (41):** foundry gating to Cinder Steps, chimney signature surface, crucible and
  plinth block-state variants resolving to real textures, slagglass full asset set and
  pickaxe tag, seal/plinth persistence fields, quench gating and one-way source contracts,
  charm recipe/advancement/tooltips, journal pages, deterministic regeneration.
- **JUnit (48):** `FoundryRules` shell ring/offsets and channel geometry, plus all previous
  suites.
- **Dedicated-server smoke (~68 commands):** places the full foundry on a cinderstone
  platform and asserts crucible state, fire-shell lava, channel lava, blast furnace,
  waystone, both spawner IDs and the vault loot table via `data get`; places the chimney
  signature and asserts its basalt column, campfire and slag piles; gives/loot-spawns the
  seal, charm and foundry cache.

## Known gaps (not verified)

- No connected client: the quench moment, the fire shell's read, the plinth's light and the
  charm's feel are unseen by a human; CI never rings the crucible interactively.
- Natural foundry spacing (~1/30 chunks) and chimney density (1/2) unmeasured in real play.
- The fourth seal, its dungeon and the kingdom's capital remain the next milestone.
