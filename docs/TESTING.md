# Validation / release gate

## Run in this workspace

- `python3 -m unittest discover -s tests -v`: **8 passing tests**.
- `git diff --check`: clean.
- `./gradlew build --no-daemon`: **blocked** — no Java executable / JAVA_HOME in the sandbox.
- Attempts to download Java/Gradle and contact Fabric Maven failed with TLS/network errors. Official wrapper files were obtainable via GitHub's API.

The Python tests check JSON parsing, custom worldgen references, biome feature stages, item translations, model/texture references, compass frames/PNG headers, 1.21.1 resource paths, smithing contracts, and deterministic regeneration. They **do not compile Java, run Minecraft's codecs, verify gameplay, or benchmark worldgen**.

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
- [ ] Find all three biomes and all three ruin layouts. Check entrance clearance, archive passages, cache loot, and natural enemy spawning.
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

This is a substantial development iteration, not a finished expansion. The new mobs use vanilla model rigs; courts are small procedural layouts, not sprawling jigsaw dungeons; cinematics preserve the player's camera rather than providing scripted camera rails. The Warden remains a one-time realm boss; replayability currently comes from repeatable courts and weapon progression. No runtime performance or balance claims are made until the checklist is exercised.
