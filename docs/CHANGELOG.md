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
