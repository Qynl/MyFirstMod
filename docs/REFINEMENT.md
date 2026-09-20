# The Refined Realm — 2.0.0-alpha.6 record

Alpha.6 is a breadth-and-polish pass over everything the realm already had: a complete
builder set, a progression layer over the eight landmark families, and two named elites.

## Builder set (20 pieces)

| Family | Stairs | Slab | Wall |
| --- | --- | --- | --- |
| Nullstone | ✔ | ✔ | ✔ |
| Polished Nullstone | ✔ | ✔ | ✔ |
| Nullstone Bricks | ✔ | ✔ | ✔ |
| Veilstone | ✔ | ✔ | ✔ |
| Prismstone | ✔ | ✔ | ✔ |
| Cinderstone | ✔ | ✔ | ✔ |
| Hush Planks | ✔ | ✔ | — |

- Blockstates follow the vanilla mappings exactly (40 stair variants, 9 multipart wall
  rules, 3 slab variants), so orientation and connections behave like vanilla families.
- Recipes: shaped for every piece (4/6/6 outputs), stonecutting for the 18 stone pieces.
- Drops: self, with slabs yielding two from `type=double`; all pieces survive-explosion
  conditioned like their base blocks.
- Tags: stone pieces join `mineable/pickaxe`, hush pieces join `mineable/axe` and both
  block+item `planks`; every piece appears in the Null Realm creative tab.

## Remembrance (9 advancements)

- Eight task advancements (`myfirstmod:landmarks/<family>`, parent `enter_realm`,
  criterion `minecraft:impossible`, +50 xp) granted when a player **opens** that family's
  cache chest — detected server-side from the chest's loot table via
  `LandmarkDiscovery` (UseBlockCallback), so no command or bookkeeping is needed.
- Goal advancement `cartographer_of_the_null` (+200 xp, loot `chests/wayfarer_bounty`:
  3× memory shard, 2× echo shard, 2× resonite ingot) once all eight are done.
- State lives in vanilla player advancement data: survives saves, per-player on shared
  worlds, visible in the advancements screen; the bounty is paid once per player.

## Named elites

| Elite | Home | Modifications |
| --- | --- | --- |
| The Brine Keeper | Brine Chapel spawner | Trident in main hand (8.5% drop chance), visible aqua name |
| Warden of the Fissure | Echo Fissure spawner | Resistance I + Speed I for 120 s, visible red name |

Both are spawner-borne entity NBT (`hand_items`, `hand_drop_chances`, `active_effects`,
`CustomName`), so they persist with the world and need no runtime code.

## Verification and known gaps

- Python: build-set matrix (blockstates/models/items/loot/recipes/tags/lang), slab double
  drop, remembrance tree and bounty, elite source contracts; JUnit `LandmarkDiscoveryTest`
  covers the pure cache→landmark mapping; smoke round-trips stair/wall states, mines a
  double slab for two drops and reads both elite names from spawner NBT.
- Not verified with a connected client: crafting/placing/climbing any piece, earning
  remembrance advancements by opening caches, elite fight feel, trident drop rate.
