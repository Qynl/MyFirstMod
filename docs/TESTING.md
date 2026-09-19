# 1.8 validation additions

The Ancient City gate adds five JUnit methods covering both axes, every outline click, every missing frame block, all 120 possible interior obstructions, already-open/partial gates, and old-small-frame rejection. Resource tests cover the axis-specific membrane, animated texture, retired recipe, biome scenery/ambience, and reproducible illustrated documentation.

The dedicated-server smoke uses the actual three vanilla city-center templates in all four rotations (12 fixtures), finalizes their top-center jigsaw as worldgen does, rejects a chest-obstructed opening without partial writes, then checks the actual portal block states and intact outline. This exercises the shared matcher and builder through an operator fixture command—not player item consumption or a connected-player teleport.

Manual checks still outstanding: Survival activation in naturally generated cities; main/offhand item consumption; protected/obstructed frames; entry/return and server restarts; blocked return landings; animated membrane appearance; terrain traversal and decoration across seeds; old-save boundaries; client performance; and multiplayer. The README uses real item textures and clearly labeled geometric model previews, **not gameplay screenshots**. See [Ancient Threshold details](ANCIENT_CITY.md).

---

# 1.7 validation additions

The Keep tests cover party-health clamps, cleave/ring/cross safe boundaries, persistent reward mail, dimension/resource closure, and the actual incremental fortress constructor through permission-gated `/hollowkeep prepare`. Dedicated-server checks inspect all ward blocks, the Expedition Heart, entrance gate and tower, and summon the Regent and glaive.

Manual checklist (not yet performed): full solo and multiplayer runs; late entry and enrolled rejoin; deaths/disconnects/abandonment; a restart in each chamber and during Regent emergence; all three attacks and second-half cadence; armor/vows/flask state across dimensions; obstructed spawn/landing areas; first and repeated reward claims including offline participants; glaive charge, absorption, and attunement; rendering and HUD at multiple GUI scales. No connected-player full-clear or visual playtest is claimed.

---

# 1.6 validation additions

Remembrance adds contract thresholds/rewards, save defaults and round-trips, bounded landmark eviction and rewarded-memory archives, vow/offer bounds, recipe/resource checks, and dedicated-server memorial/ledger/loot/item fixtures.

Manual checklist: read one stele twice and with two players; reconnect and retry; claim multiple eligible contracts and verify no repeat payout; buy with exact/insufficient payment and full inventory; change each vow at a safe/unsafe waystone and across restarts; leave the realm and wait for effects to expire; survey unloaded chunk borders; consume a cathedral and refresh its atlas marker; verify left/right bearings at cardinal headings; check a 1.5 save's sanctuary migration; activate the Ancient City portal (the 1.6 handcrafted route is retired in 1.8). **These connected-player checks have not been performed in the sandbox.**

---

# 1.5 validation additions

The Pilgrimage update adds pure tests for rite order/reset, bounded flask capacity and persistence, and forward-cleave safe boundaries; resource tests for cathedral placement, seal states, models/textures and deterministic generation; and dedicated-server fixtures for all three cathedral floors, custom spawner IDs, pilgrim loot, and item registration.

Manual release checklist: navigate both stair flights in Survival; try the rite out of order and reload mid-rite; have two players attempt the same reward; drink/rest while damaged; swap/craft multiple flasks; upgrade at capacity cap; dodge toward walls, water and ledges; stagger a knight; sidestep a stalker; check Warden melee-vs-ranged recovery damage; inspect animations and HUD at small GUI sizes. **These manual checks have not been performed in this sandbox.**

---

# Validation / release gate

## Automated validation

- Local Python resource suite: **21 tests**.
- `git diff --check`: clean.
- Java builds run in **GitHub Actions** because this workspace has no Java installation.
- CI compiles main/client sources, runs **35 JUnit tests**, creates the remapped JAR, and executes `scripts/ci_server_smoke.py`.
- The smoke test launches a real Fabric dedicated server, loads the custom dimension codecs, generates chunks, places a ruin, forced archive vault, and shrine, checks shrine placement and three ore drop tables, spawns both realm mobs, saves, and shuts down. Logs and JUnit reports are uploaded as `Validation-<commit>`; the mod is in `Null-Warden-<commit>`.

Check the final Actions run for the exact revision under test. Compilation is not a playtest, and the smoke test does not simulate a connected player or a complete boss/trial encounter.

The initial CI attempt caught a Warden attribute API mismatch. The first dedicated-server test then caught two dimension-codec issues that JSON-only tests cannot detect. Both are corrected; regression assertions were added for the dimension fields.

## Required before release

### Build and data loading

- [ ] Run `./gradlew clean build` with Java 21.
- [ ] Launch a fresh 1.21.1 Fabric client with Fabric API. Verify models, attributes, textures, and recipe loading.
- [ ] Start a dedicated server with the same JAR. Confirm no client classes load server-side.
- [ ] Verify no registry, feature, dimension codec, loot-table, advancement, or model warnings.
- [ ] Run `/reload` and restart the server after the first realm visit.

### Arrival / exploration

- [ ] Ignite the documented frame in an Ancient City and confirm exact origin-dimension return behavior.
- [ ] Verify sanctuary and approach safety on at least three seeds, including steep terrain.
- [ ] Check the automatic compass, a full inventory, repeated entries, death, and the replacement recipe.
- [ ] Face all four directions at four arena-relative positions. The needle must point at X=0/Z=0.
- [ ] Confirm dormant behavior outside the realm and the distance overlay in both hands.
- [ ] Find all four biomes and all three ruin layouts. Check entrance clearance, archive passages, cache loot, and natural enemy spawning.
- [ ] Explore far from spawn; profile generation, particles, and server tick time. The initial sanctuary/arena build is synchronous and requires profiling.
- [ ] Verify existing generated chunks remain intact outside the rebuilt central footprint. Test upgrades only on a backup.

### Trials

- [ ] Activate the introductory court, complete all three waves, and check shard/XP counts.
- [ ] Obstruct every spawn location: no free completion reward should be issued.
- [ ] Test two players: both nearby at activation, one joining late, one leaving, and one dying.
- [ ] Leave for 30 seconds, unload/reload the court, and restart mid-wave. No orphan guardians or duplicate rewards.
- [ ] Switching to Peaceful cancels the trial; restoring difficulty permits play after the cooldown.
- [ ] Verify persisted cooldowns and the five-minute recharge after success.
- [ ] Defeat the Warden; the next trial must use ascended health/count/rewards.

### Warden

- [ ] Survival approach starts the fight; Creative, Spectator, and Peaceful do not.
- [ ] Awakening, transitions, and victory sequences play fully without locking controls; F1 hides overlays.
- [ ] Confirm the boss has no native Warden AI attacks.
- [ ] Compare all seven tells with their actual damage areas. Dodge after target lock, especially dash and rain.
- [ ] Cleanse pylons while sneaking; the inner pocket protects from pylon pulses but not boss attacks.
- [ ] Verify the boss remains damageable after intro/transition/restart and is not trapped by its own arena.
- [ ] Confirm the fatal hit starts the finale rather than vanilla 20-tick death removal.
- [ ] Test deaths, disconnects, chunk unloads, retreat, restart, and re-entry during every phase and the finale.
- [ ] Confirm eligible players receive one reward set, including with full inventory and reconnecting after victory.
- [ ] Check multiplayer health scaling and no cross-save static state when opening a second integrated-server world.

### Equipment

- [ ] Inspect both sword damage/speed/durability attributes.
- [ ] Tap release before 0.6 seconds: no charged attack or cooldown. Charge for 1.5 seconds: full strength.
- [ ] Test line of sight, teammate exclusion, hostile-only targeting, healing caps, and shared cooldowns.
- [ ] Riftstep into walls, low ceilings, water, lava, cliffs, borders, and unloaded terrain; it must fail or stop safely.
- [ ] Test both hands and both weapon tiers, including breaking on ability use.
- [ ] Verify Heart effect durations and 45-second cooldown in multiplayer.
- [ ] Smith an enchanted/damaged/renamed blade. Verify the output's components and increased max durability behave correctly.
- [ ] Confirm recipe unlocks and all four advancements.

## Deliberate limits

This is a substantial development iteration, not a finished expansion. The new mobs use vanilla model rigs; courts are small procedural layouts, not sprawling jigsaw dungeons; cinematics preserve the player's camera rather than providing scripted camera rails. The Warden now supports opt-in ritual rematches, but there is no scripted free-camera cutscene system or sprawling jigsaw structure network. No runtime performance or balance claims are made until the checklist is exercised.

## Echoes-specific regression checklist

- [ ] Load a 1.1 save backup. Verify the altar appears once, pending first-victory rewards migrate, and unrelated builds remain intact.
- [ ] Offer a sigil before first victory / in Peaceful / during another fight: no item consumption or duplicate boss.
- [ ] Win or abandon a rematch, restart at each phase, and verify cleared-realm progression remains unlocked.
- [ ] Have one eligible player disconnect before victory. Start another ritual; the absent player's previous reward must still arrive on re-entry.
- [ ] Attempt tiers 1–5, including mixed-progress parties. Check wave count, shard/XP rewards, sigils, and the captain.
- [ ] Test overlapping Fracture circles, paused empty courts, Siphon grounding duration, and Pursuit speed.
- [ ] Open and reopen a journal in either hand and outside the realm. Check translated page layout and updated player-specific values.
- [ ] Discover multiple courts. Cycle compass routes, put the nearest court on cooldown, and refresh the route.
- [ ] Reflect skeleton arrows with the Aegis. Player-owned projectiles, projectiles behind the player, and projectiles behind walls must not be reflected.
- [ ] Enter an archive vault naturally. Walk the entire staircase both ways; check overhead clearance and loot.

## Wilds & Relics-specific playtest checklist

- [ ] Find all four biomes across multiple seeds, inspect cave height/depth transitions, and check for feature overlaps or chunk-border writes.
- [ ] Mine every ore with hand, stone, iron, and diamond-tier tools. Verify correct-tool requirements, Fortune, and Silk Touch behavior.
- [ ] Make a furnace from Nullstone, use Hushwood as fuel, smelt Resonite, and smelt Prismstone into glass.
- [ ] Harvest fiber/berries from leaves, eat stew, and drink tonic with full and partially filled inventories. Check returned containers and Creative behavior.
- [ ] Equip every Resonite armor piece. Inspect both worn texture layers, durability, repair ingredients, enchantments, and realm-only set bonuses.
- [ ] Shoot the staff through doorways and at walls, allies, pets, players, and enemies touching the player. Verify ammunition, nearest-hit selection, cooldowns, and glowing.
- [ ] Slam the maul while grounded/in the air; check range, line of sight, teammate filtering, and durability.
- [ ] Survey near unloaded chunk boundaries and outside the realm. Only existing realm ore blocks should be reported, and no chunks should generate.
- [ ] Use repair kits in both hands on damaged, undamaged, foreign, stacked, and nearly broken items.
- [ ] Bind different waystones with two players. Test per-player destinations, restart persistence, sanctuary fallback, blocked landings, damage interruption, and combat locks.
- [ ] Verify recall does not clear player blocks, land in fluid, or consume its cooldown after a failed landing.
- [ ] Inspect every new inventory icon, the Creative tab, all journal pages, ammo/recall HUD, and wrapped tooltips at small and large GUI scales.
- [ ] Upgrade a backup of a 1.2 world. Existing records and inventory must load; new ores/shrines only appear in new chunks. Terrain seams are expected.
