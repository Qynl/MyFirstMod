# The Null Warden — Wilds & Relics (1.3.0)

A Minecraft **1.21.1 / Fabric / Java 21** exploration and boss mod.

The Null Realm now has its own noise terrain and four custom biomes, an arrival sanctuary, procedural ruin courts, two realm enemies, a four-phase guardian, and a repeatable post-boss progression loop.

> **Development build.** CI compiles the mod and runs automated checks, including a dedicated-server worldgen smoke test. Visual presentation, combat balance, and multiplayer still require hands-on playtesting; see [validation scope](docs/TESTING.md). Back up existing worlds. Use a fresh test world for the complete terrain update: existing chunks never regenerate automatically.

## New in 1.3 — Wilds & Relics

The realm now has its own gathering and equipment progression, not just combat courts:

- **Luminous Fen**, a fourth biome with glowing moss, shallow pools, and luminous growths.
- **Underground fracture caves** between roughly Y=-40 and Y=48, beneath custom Nullstone terrain.
- **Three mineable ores:** Resonite, Prism, and Cinder. Resonite is smelted into ingots; the other ores drop useful magical materials.
- **15 collectible custom blocks:** stone, bricks, moss, Hushwood, planks, foliage, ores, metal storage, and lamps. Original textures and mining/loot tags are included.
- **21 new items**, including a complete custom-textured armor set, three tools, two weapons, surveying/stealth/repair/recall equipment, and expedition food.
- **Waystone shrines:** attune to a discovered shrine and return using a channeled Wayfarer Thread.
- A dedicated Creative tab, recipe unlocks, expanded journal pages, wrapped tooltips, staff-ammunition display, and recall progress bar.

**Existing saves:** new terrain, plants, ores, and shrines appear in newly generated chunks. New stone and climate distribution can create visible seams against old chunks. Back up your world; a fresh world gives the most consistent generation. Existing inventory items, trial progress, and boss progression are retained.

See [the Wilds equipment guide](docs/WILDS.md) for stats, crafting, and controls.

## New in 1.2 — Echoes

- **Replayable Warden rituals:** offer an Echo Sigil at the altar near the arena. Challenge levels increase through three rematches, with reactivating pylon pairs and shorter recovery windows. Original attack warning times remain intact.
- **Five court challenge tiers:** party boss bars, intermissions, final-wave captains, and three different oaths—not only health scaling.
- **Sunken archive vaults:** another dungeon floor, a real staircase, pillars, and a separate treasure cache.
- **Expedition journal:** a vanilla book interface with live statistics, mechanics, crafting help, and remembered court coordinates/cooldowns. Reopen it to refresh.
- **Multi-route compass:** sneak-use cycles the arena, sanctuary, and nearest ready court you have discovered.
- **New equipment:** Echo Sigils, Warden Crests, and a projectile-deflecting Rift Aegis.
- **Persistent reward mailboxes:** an offline participant's boss loot is retained even if another group starts a new ritual.

See [full 1.2 changelog](docs/CHANGELOG.md).

## Your expedition

1. Find an Ancient City's sculk catalyst and a reinforced-deepslate seal.
2. Build/complete the supported **4-block-wide, 5-block-tall frame in the X/Y plane**; ignite its bottom-left corner using flint and steel. The interior must be empty. The existing ignition implementation does not support rotated frames.
3. Arrive at the **Hushed Threshold**, not directly inside a boss fight. A return gate is available immediately.
4. Receive a **Resonance Compass**. Its custom animated needle points toward the Warden arena; holding it shows distance. Follow the lit causeway or explore first.
5. Discover ruin courts and use their **Resonance Cores** to opt into three-wave trials.
6. Approach the arena in Survival or Adventure to awaken the Null Warden.
7. Claim the blade and heart, then return to the courts: defeating the boss permanently unlocks **Ascended Trials** globally in that realm.
8. Forge a Resonance Matrix and awaken your Nullblade at a smithing table.
9. Sneak-use a core to attempt your next challenge tier. Gather Echo Sigils from tier 2+ completions, vaults, or crafting.
10. Offer a sigil at the **Echo Altar (8, 81, 30)**. Rematch victories award Warden Crests for the Rift Aegis.

## A realm of its own

The dimension no longer uses the End's noise settings or biome.

| Biome | Identity |
| --- | --- |
| **Hushed Grove** | Custom moss, harvestable Hushwood and Hush Leaves, luminous crowns, drifting souls |
| **Prism Wastes** | Custom Prismstone, amethyst formations, violet fog, floating motes |
| **Cinder Steps** | Custom Cinderstone terraces, ash, fractured spires |
| **Luminous Fen** | Glowing moss, shallow pools, luminous growths, spore-filled teal fog |

Seeded terrain combines layered ridges with smaller three-dimensional fractures and underground tunnels. Biome-local resource weighting makes Cinder Pearls more common under Cinder Steps and Prism Dust more common elsewhere; all three ore types can occur throughout the realm. Procedural decoration places three ruin layouts: an enclosed **reliquary**, an open **observatory**, and a two-storey **archive** with a sunken treasure vault. Waystone shrines add a separate exploration landmark with an attunement stone and supplies. These are code-generated, chunk-contained features, not copied vanilla structure templates. Courts contain a loot cache and an opt-in trial core. They do not currently integrate with `/locate structure`.

The sanctuary includes a nearby introductory court and a lit approach to the arena. Exploration features are excluded from the protected central approach. Sanctuary construction happens once per world; arena resets rebuild the combat footprint. Do not build a permanent base inside that footprint.

## Realm creatures

- **Rift Sentinel:** armored guardian with a stationary, 1.8-second telegraphed close-range pulse. Retreat beyond the visible ring.
- **Shardstalker:** faster climbing hunter with a sparkling warning before a short-range slowing pulse.

Both have original textures and distinct behavior, but currently reuse vanilla zombie/spider model rigs and base navigation. They spawn in the realm and in trials; boss echoes now use Sentinels rather than full vanilla Wardens.

## The guardian

The existing four-phase encounter has been revised, rather than discarded:

- Five-second awakening sequence, short phase transitions, and a five-second victory sequence.
- Letterboxing and narrative subtitles, with synchronized emergence/fracture poses. These are **in-engine cinematic sequences**, not free-camera cutscenes; you retain movement and camera control. Hide the HUD with F1 to hide the overlay.
- Explicitly locked attack positions. Warnings no longer chase players while damage lands somewhere else.
- A dash that commits to its marked destination, rather than teleporting onto the player at impact.
- Cone and ring boundaries that match their damage areas, plus textual dodge instructions.
- Native Warden AI attacks disabled: no unrelated sonic-boom or melee attacks interrupting the encounter's tells.
- Pylon pulses have advance warnings. Sneaking within the inner three-block pocket grounds their pulse while cleansing; boss attacks still demand attention.
- Opening health scales with nearby players, up to four-player scaling.
- Persistent participant/reward tracking, stale-actor cleanup, and runtime-state cleanup when a server closes.

Peaceful mode does not start the encounter or trials. You can still enter, explore, and return. Creative and spectator players do not start or enroll in combat.

## Rewards worth keeping

### Nullblade

An actual sword attribute profile: **10 attack damage / 1.8 attack speed**, 2,531 durability, fireproof.

- **Hold use, then release:** Soul Rend. Minimum charge 0.6 seconds; fully charged at 1.5 seconds. At full charge, deals 16 ability damage within 6.5 blocks in a forward sweep.
- Soul Rend affects hostile, non-allied mobs within line of sight—not pets, passive mobs, or other players. Successful hits restore up to two hearts total.
- **Sneak-use:** Riftstep, up to six blocks. Checks intermediate collision and safe grounded destinations; does not pass through walls or intentionally land in fluid/off a ledge.
- Shared ability cooldowns prevent swapping between blade tiers to bypass recovery. Abilities consume durability.

### Heart of the Null

A reusable defensive artifact: use for regeneration II, absorption II, and slow falling. **45-second cooldown.** It is not consumed.

### Nullblade, Awakened

Craft a **Resonance Matrix** from eight Resonant Shards surrounding an amethyst shard. At a smithing table combine:

| Template slot | Equipment slot | Addition slot |
| --- | --- | --- |
| Echo shard | Nullblade | Resonance Matrix |

The smithing transform carries the base stack's applicable components forward, including enchantments. The awakened blade has **12 attack damage**, 4,096 durability, a full-charge 24-damage/eight-block Soul Rend, up to three hearts of healing, shorter Rend cooldown, and eight-block Riftstep.

First-encounter rewards are **four shards and 500 XP** per eligible participant. Each player's first victory also grants **Nullblade and Heart**, including newcomers winning their first rematch. Rematches grant **a Warden Crest, eight shards, and 350 XP**. Reward mailboxes are independent of the active fight: offline winners receive their queued rewards when alive in the realm again. A full inventory drops items instead of deleting them.

### Rift Aegis

Craft a **shield + Warden Crest + Resonance Matrix**. Use the Aegis to reflect visible hostile-owned projectiles in the forward hemisphere within five blocks, directing them along your aim, and gain resistance II for three seconds. It has a 12-second cooldown and consumes two of its 768 durability per activation. This is a timed parry artifact, not a hold-to-block vanilla shield.

## After the boss

Courts remain useful after victory:

| Tier | Waves | Shards / player | XP / player |
| --- | --- | --- | --- |
| 0 (before boss) | 3 | 2 | 50 |
| 1 | 4 | 4 | 100 |
| 2 | 5 | 6 | 150 |
| 3 | 5 | 8 | 200 |
| 4 | 5 | 10 | 250 |
| 5 | 5 | 12 | 300 |

Use a core normally for tier 1 after the boss; **sneak-use** to attempt the activating player's next tier, capped at five. Tier 2+ also grants one Echo Sigil per eligible survivor.

The selected court/tier has one oath:

- **Fracture:** fixed circles erupt after two seconds. Move out; overlapping circles do not multiply damage.
- **Siphon:** two visible wards slowly heal enemies. Sneak within their circles for two seconds to ground each ward.
- **Pursuit:** faster hunters. Every ascended court ends with a tougher, named captain.

Nearby eligible players enroll at activation; late arrivals receive no completion loot. One enrollment per player prevents overlapping court farming. A party boss bar shows wave, remaining enemies, tier, and oath. Waves have five-second intermissions. Completed courts recharge for five minutes; abandoned courts reset after 30 seconds, and all trials have a ten-minute maximum duration. Interrupted trials reset on server restart rather than resuming with stale guardians.

The Echo Altar has a three-minute ritual cooldown. Rematch health scales up through challenge level three, recovery becomes shorter, and different pylon pairs reactivate at phase changes. Failed rematches restore the cleared realm state—no free automatic boss respawn and no loss of ascended court access.

### Journal and routes

The sanctuary gives you a **Null Expedition** book. Reopen it to refresh statistics and the most recent known court coordinates. A replacement is crafted from a book and amethyst shard. Discover biomes by visiting; discover courts by interacting with their cores. Each player's saved court list keeps the 128 most recent discoveries, and the journal shows the latest twelve.

A replacement compass uses a compass and echo shard. Sneak-use selects a destination; ordinary use refreshes the selected route. Court mode chooses the nearest discovered court whose cooldown has expired at the time you use the compass. It is not an omniscient dungeon locator or a live activity tracker.

## Build and validation

```sh
# Java 21 must be installed; the Gradle 8.12 wrapper is included.
./gradlew build

# Unit tests (also included in build):
./gradlew test

# Fast data/resource checks, Python 3 only:
python3 -m unittest discover -s tests -v
```

Windows: `gradlew.bat build`. Output: `build/libs/`.

CI runs resource tests, JUnit balancing tests, the Loom build, and a real dedicated-server smoke test for main/arena pushes and pull requests. It uploads the installable JAR separately from test reports. Download the `Null-Warden-<commit>` artifact from a successful [Actions run](https://github.com/Qynl/MyFirstMod/actions/workflows/build.yml).

The smoke test boots Fabric, generates realm chunks, places a ruin, archive vault, and shrine, checks actual ore drops and a shrine loot table, spawns both realm mobs, saves, and shuts down cleanly. It is not a combat bot or a visual/multiplayer test. The smoke-test script owns the disposable `run/ci-smoke` world and overwrites `run/server.properties`/`eula.txt`; never point it at a production server.

Original pixel textures and worldgen resources are reproducible:

```sh
python3 scripts/generate_art.py
python3 scripts/generate_realm_data.py
python3 scripts/generate_loot.py
python3 scripts/generate_wilds.py
```

The Gradle wrapper is sourced from the official Gradle 8.12 repository (Apache-2.0). The mod retains its existing declared MIT license.
