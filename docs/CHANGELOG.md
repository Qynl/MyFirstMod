# 2.0.0-alpha.1 — The Kingdom Beyond the Gate

## Gateway

- Ancient City activation now takes three seconds: particles climb both posts, the charge sound rises, and the shard is consumed only when the aperture actually opens.
- Moving 32+ blocks away, changing dimension, dying, or swapping the shard out of that hand cancels the awakening and consumes nothing. At most 16 awakenings run at once; all are cleared on shutdown.
- New worlds build a matching 22×8 realm-side gateway at the sanctuary, with a larger lamp-inlaid plaza and arrival at (0, 81, 172). Existing saves keep their current sanctuary and arrival point.

## Rootbound Monastery

- Added an original 47×18×47 generated structure for the Hushed Grove: chapterhouse, west chapel, two-storey east library with a real stair, cloister colonnade, root garden, two spawners, two caches and an entrance waystone.
- Added the `myfirstmod:rootbound_monastery` structure type, which delegates to vanilla jigsaw placement and refuses start chunks inside the fixed sanctuary approach.
- Added a two-bell rite whose progress is stored in block states, survives restarts, and is rotation-correct for all four template orientations.
- Added the Rootbound Prior: altar-bound, 240-540 scaled health, three telegraphed attacks with a recovery pause, a rotated second crown pulse below half health, and self-cleanup when its heart or actor disappears.
- Defeating the Prior turns the heart into a reliquary and opens the cloister screen as a shortcut.

## Rewards and gear

- Added the Rootbound Seal (one shared per monastery) and the Briarbrand: a narrow four-block thorn sweep for 9 damage, Slowness III, one heart healed on hit, ten-second cooldown, forge-attunable.
- Added a shaped Briarbrand recipe and its recipe-book advancement.

## World and assets

- Hushed Grove chunks can now raise great root arches between landmarks, chunk-contained and only on flat natural moss.
- Added original block textures and 16-variant blockstates for the heart, bells and reliquary, item sprites for the seal and Briarbrand, a 128×128 Prior texture, and a new client model/renderer.
- Documentation plates now show 26 items and six creature previews, and the gateway plate documents the hold-to-awaken behavior.
- Added a journal page, all rite/boss/awakening translations, and docs/KINGDOM.md.

## Validation

- 23 Python resource tests, including a from-scratch NBT decoder asserting the monastery's geometry, puzzle blocks, stair headroom, spawner actors and loot tables.
- 42 JUnit methods; 7 new for bell logic, party scaling, every attack boundary, bell-to-heart rotation invariance and the sanctuary exclusion.
- Dedicated-server smoke places the real template, checks 17 blocks and both NBT payloads, spawns the new loot tables and entities, and runs the custom structure type.
- No connected-player test: the monastery has not been walked, the Prior has not been fought, and the awakening has not been seen by a human.

---

# 1.3.0 — Wilds & Relics

## Dimension

- Added Luminous Fen with glowing moss and shallow, contained pools.
- Replaced new-chunk base terrain/surfaces with a custom Nullstone, moss, Prismstone, and Cinderstone palette.
- Added underground fracture caves bounded above bedrock and below the usual surface.
- Added three ore types with biome-weighted distributions, iron-tier harvesting, Fortune, and Silk Touch drops.
- Hushwood and Hush Leaves supply wood, fiber, and food. Flora positions now vary within chunks instead of always sharing the same center.
- Added permanent waystone shrines with original block art and supply caches.

## Equipment and progression

- Added 21 items: five materials, three foods/drinks, three tools, four armor pieces, two weapons, and four utility items.
- Added a custom Resonite armor material, two worn texture layers, and a full-set realm exploration bonus.
- Added dust-powered, wall-clipped staff attacks; grounded maul slams; a bounded ore survey; stealth; consumable repairs; and interruptible recall.
- Added 15 collectible custom blocks, crafting/smelting recipes, recipe-book unlocks, mining tags, plank/stone crafting compatibility, and a Creative tab.
- Added staff-ammunition and recall HUD readouts. Wrapped all custom item tooltips to keep long ability instructions readable.
- Expanded the live journal and persistent record for the fourth biome and personal bound waystone. Older records default to sanctuary recall.

## Validation

- Expanded resource checks from 11 to 15, including custom block completeness, recipe item references, mining/ore-loot contracts, caves, and armor assets.
- Expanded Java tests from 10 to 12 with waystone round trips, all four biome discoveries, and legacy defaults.
- Extended dedicated-server smoke tests to prepare and place a shrine, inspect its core, roll all three ore drop tables, and roll shrine supplies.
- Smoke fixtures wait for surrounding chunks to load before issuing block/feature commands. RCON command transcripts are saved with the validation artifact.

See [WILDS.md](WILDS.md) for the full equipment guide and deliberate limitations.

---

# 1.2.0 — Echoes

## Content

- Added an Echo Altar beside the arena and consumable Echo Sigils. The first victory permanently unlocks rituals; rematches scale through challenge levels 1–3.
- Rematches reactivate distinct pylon pairs and shorten recovery without shortening attack telegraphs. They reward Warden Crests, shards, and XP.
- Added the Rift Aegis, a craftable parry artifact with forward hostile-projectile reflection and brief resistance.
- Expanded ascended courts to five challenge tiers, four/five waves, named final-wave captains, party boss bars, and five-second wave breaks.
- Added Fracture, Siphon, and Pursuit court oaths: avoid fixed ground eruptions, disable healing wards, or manage faster enemies.
- Added sunken archive vaults with two-wide stairs, columns, a lit centerpiece, and a dedicated treasure table.
- Added a live expedition book, persisted personal progress, biome discovery XP, court coordinates, and recovery time listings.
- Added compass routes for the arena, sanctuary, and nearest ready discovered court.
- Added recipes and advancement unlocks for the journal, sigil, and Aegis, plus a rematch-victory advancement.

## Reliability

- Rewards are queued per player independently of encounter state; starting another ritual cannot erase an offline participant's queued victory loot.
- Failed rematches return to a cleared realm and keep ascended trials unlocked.
- Trials cap enemies per wave at eight and enforce one enrollment per player, a ten-minute lifetime, and cleanup on abandonment/Peaceful mode/server restart.
- Unloaded trial guardians are not treated as defeated enemies.
- Flora will not generate through trial cores or on ruin roofs.
- Existing 1.1 sanctuaries add the new altar once without rebuilding the whole sanctuary. This intentionally updates the X=4..12 / Z=26..34 footprint; back up player builds there.
- Fixed the Warden's Yarn attribute registration and two dimension-codec problems exposed by actual CI: missing `has_ceiling` and an obsolete uniform-int-provider shape.

## Build and tests

- GitHub Actions uses Java 21 / Gradle 8.12, compiles both main and client sources, and produces a remapped mod JAR.
- Added ten JUnit tests for tier bounds, wave counts, multiplayer caps, scaling, deterministic oath selection, saved progress, bounded discoveries, legacy defaults, and reward-mailbox round trips.
- Expanded Python resource tests to eleven, including endgame recipes, journal components, advancement links, and dynamic HUD translations.
- Added a dedicated-server smoke test with isolated world generation, explicit vault placement, custom mob spawning, save, and graceful shutdown.
- CI publishes diagnostics and test reports separately from the installable mod artifact.

## Still needs hands-on verification

Combat tuning, camera/model presentation, GUI readability at different scales, accessibility, full inventory/death/disconnect cases, multiplayer timing, and performance on ordinary hardware. The new creatures continue to use vanilla model rigs; cinematic sequences preserve player camera control rather than using camera rails.
