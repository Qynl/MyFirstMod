# Landmarks of the Kingdom — structures catalog (2.0.0-alpha.5)

The Null Realm now carries **seventeen structure families**: nine from earlier milestones
(ruins, shrines, observatories, cathedrals, memorials, monastery, archive, foundry,
capital) plus eight landmark families added here. This page catalogs the new eight.

## The families

| Landmark | Biomes | Rarity (1/n chunks) | Silhouette | Cache |
| --- | --- | --- | --- | --- |
| Veil Watchtower | veil_highlands | 6 | 5×5 nullstone-brick shaft, nine high, door gap, four-step stair, veilstone roof, prism-lamp beacon | `watch_cache` at the top |
| Brine Chapel | drowned_stacks | 8 | 9×9 oxidized-trim walls, blue-glass windows, flooded nave, oxidized altar with lamp | `chapel_cache` underwater; drowned spawner |
| Geode Garden | prism_wastes, shard_spires | 7 | Eight spire-crystal columns, amethyst cluster heart, resonite-ore floor | `geode_cache` beside the ring |
| Slag Camp | cinder_steps, ember_vents | 7 | Campfire, four blackstone seats, hush-plank lean-to with warped-slab roof | two `camp_cache` barrels |
| Caravan Wreck | all eight | 14 | Hush-plank deck, hushwood wheels and corner posts, scattered slabs | two `caravan_cache` chests |
| Echo Fissure | all eight | 26 | 3×7 resonite-lined trench three deep, soul-fire rim | `fissure_cache` (echo shard) + Rift Sentinel spawner |
| Heartwood Circle | hushed_grove | 6 | Eight hushwood trunks, leaf lintels at y+4 | `circle_cache` covered by leaves at the heart |
| Fen Shrine | luminous_fen | 6 | Four prism-lamp corners, moss altar, glass bowl, spore-blossom crown | `shrine_cache` at the steps |

## Guarantees

- **Chunk-contained:** every read and write stays inside the generating chunk; no
  synchronous exploration-time chunk generation.
- **Sanctuary-aware:** nothing starts inside the fixed sanctuary approach reservation.
- **Surface-verified:** each landmark checks its own surface material (region identity)
  and local flatness before placing a single block; otherwise the chunk is left untouched.
- **Real loot:** caches use loot tables with one guaranteed themed roll plus two random
  regional rolls; spawners carry deferred NBT so IDs survive worldgen placement.
- **Persistence:** no block states or world state are involved — landmarks are pure
  geometry plus containers, so saves from any earlier version simply gain them in new
  chunks.

## Verified automatically

- Python: membership matrix across all eight biomes, configured/placed feature files with
  rarity filters, eight loot tables with guaranteed rolls, registration + sanctuary guard
  + loot keys + spawner IDs in source, journal page, gallery plate.
- Smoke: each landmark placed on a flat platform of its own surface block; deterministic
  silhouette blocks, every cache (`data get … ChestItems`) and both spawner IDs asserted.

## Known gaps

- No connected client has found, entered or looted any landmark; rarity values are
  unmeasured in real exploration and may need tuning after playtest.
- Landmarks do not yet feed the Pilgrim Atlas landmark kinds or contracts; that wiring is
  deliberately left to a later pass.
