# The Null Warden — Resonance update

A Minecraft **1.21.1 / Fabric / Java 21** exploration and boss mod.

The Null Realm now has its own noise terrain and three custom biomes, an arrival sanctuary, procedural ruin courts, two realm enemies, a four-phase guardian, and a repeatable post-boss progression loop.

> **Development build, not a tested release.** Resource tests pass; this change has not yet been compiled or playtested in Minecraft. See [validation and playtest checklist](docs/TESTING.md). Back up existing worlds. Use a fresh test world to see the new terrain: already-generated End-style chunks do not regenerate automatically.

## Your expedition

1. Find an Ancient City's sculk catalyst and a reinforced-deepslate seal.
2. Build/complete the supported **4-block-wide, 5-block-tall frame in the X/Y plane**; ignite its bottom-left corner using flint and steel. The interior must be empty. The existing ignition implementation does not support rotated frames.
3. Arrive at the **Hushed Threshold**, not directly inside a boss fight. A return gate is available immediately.
4. Receive a **Resonance Compass**. Its custom animated needle points toward the Warden arena; holding it shows distance. Follow the lit causeway or explore first.
5. Discover ruin courts and use their **Resonance Cores** to opt into three-wave trials.
6. Approach the arena in Survival or Adventure to awaken the Null Warden.
7. Claim the blade and heart, then return to the courts: defeating the boss unlocks harder **Ascended Trials** globally in that realm.
8. Forge a Resonance Matrix and awaken your Nullblade at a smithing table.

## A realm of its own

The dimension no longer uses the End's noise settings or biome.

| Biome | Identity |
| --- | --- |
| **Hushed Grove** | Sculk-covered ridges, branching basalt growths, luminous crowns, drifting souls |
| **Prism Wastes** | Pale calcite surfaces, amethyst formations, violet fog, floating motes |
| **Cinder Steps** | Dark basalt terraces, ash, fractured spires |

Seeded terrain combines layered ridges with smaller three-dimensional fractures. Procedural decoration places three small ruin layouts: an enclosed **reliquary**, an open **observatory**, and a divided **archive**. These are code-generated, chunk-contained features, not copied vanilla structure templates. Courts contain a loot cache and an opt-in trial core. They do not currently integrate with `/locate structure`.

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

Boss rewards are delivered once per eligible participant: **Nullblade, Heart, four shards, 500 XP**. A full inventory drops the rewards instead of deleting them.

## After the boss

Courts remain useful after victory:

- Three waves with short breathers; difficulty scales with enrolled players.
- Nearby eligible players enroll at activation; only enrolled survivors still at the court receive completion rewards.
- Ordinary courts award two shards and 50 XP per player.
- Ascended courts add enemies, increase their health, and award four shards and 120 XP.
- A completed court recharges after five minutes of world time.
- Abandoning a trial for 30 seconds resets it. Interrupted trials do not resume after a server restart; their guardians are cleaned up when loaded. Cooldowns persist.
- Ruin caches and occasional mob drops provide additional shards and expedition supplies.
- An advancement path tracks entry, the heart, gathering resonance, and awakening the blade.

A replacement compass can be crafted with a compass and an echo shard. Use it once to bind its lodestone component; its custom needle already tracks the arena while in the realm.

## Build and validation

```sh
# Java 21 must be installed; the Gradle 8.12 wrapper is included.
./gradlew build

# Fast data/resource checks, Python 3 only:
python3 -m unittest discover -s tests -v
```

Windows: `gradlew.bat build`. Output: `build/libs/`.

CI runs resource tests and the Loom build for main/arena branch pushes and pull requests. A successful CI build is still not a substitute for the dedicated-server and multiplayer playtests in [docs/TESTING.md](docs/TESTING.md).

Original pixel textures and worldgen resources are reproducible:

```sh
python3 scripts/generate_art.py
python3 scripts/generate_realm_data.py
python3 scripts/generate_loot.py
```

The Gradle wrapper is sourced from the official Gradle 8.12 repository (Apache-2.0). The mod retains its existing declared MIT license.
