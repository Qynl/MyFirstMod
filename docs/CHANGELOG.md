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
