# 1.6 — Remembrance

The consolidated player and developer guide is now the [main README](../README.md).

## Added

- Three forgotten-memorial layouts with supply chests, original Memory Stele art, and six lore passages.
- Memory Shards: one shard and 25 XP per player per newly remembered stele, plus seven one-time milestone contracts totaling 30 shards.
- A craftable Pilgrim Ledger and safe sanctuary migration at `(6,81,158)`. Claim contract rewards or buy four supply bundles.
- Iron, Embers, and Mist vows, with paired positive/negative effects. A Silence seal clears the specialization. Changes require a safe waystone and share a persistent 60-second cooldown.
- A craftable Pilgrim Atlas with five categories, loaded-column surveys, remembered sites, stale-loaded-marker cleanup, and a directional HUD. Unread memorial routing excludes already rewarded coordinates.
- A Survival crafting route to four reinforced-deepslate frame blocks: eight polished deepslate around one echo shard.
- Recipes/unlocks, deterministic original assets, journal pages, save defaults/bounds, resource/persistence/rule tests, and server smoke fixtures.

## Persistence and limits

Memorial reward records stop accepting new coordinates at 256; they never evict an old rewarded coordinate. Atlas records keep 128 landmarks and evict their oldest stored entry. Contract claims use a seven-bit mask. Invalid vow/offer values and oversized save collections are bounded on read. All existing progression fields remain.

Vow effects last at most three seconds after the realm stops refreshing them. They follow ordinary vanilla status-effect stacking and only refresh for living Survival/Adventure players in the realm. The active vow is player-owned, not gear-owned; swapping seals cannot stack persistent choices.

Memorials and other new terrain features require new chunks. Crafting and existing-stat contracts work without regenerating a world. The ledger migration does not overwrite an occupied block; craft one if needed.

## Validation limits

Automation covers build/resource integrity, rule boundaries, record round-trips and bounds, and real server worldgen/loot/item fixtures. Connected-client visual checks, atlas use, ledger transactions, multiplayer claims, and combat balance still require manual testing. No additional custom mob, boss, soundtrack, global map, or free-camera cinematic is claimed for this release.
