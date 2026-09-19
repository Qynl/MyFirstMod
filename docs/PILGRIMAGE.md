# 1.5 — Ashen Pilgrimage

A darker, more deliberate direction for the **existing** Null Realm: navigation, safe rest, dangerous exploration, stronger enemy silhouettes, and readable combat openings. Fabric / Minecraft 1.21.1 / Java 21.

## Your first expedition

1. Enter the realm. Existing and new players receive **one Ashen Flask and one Pilgrim’s Step** on their first visit after updating. This gift is recorded in your save; dropping the kit or dying does not duplicate it.
2. The sanctuary gains a **waystone at (6, 81, 160)**, only if the block is empty. Use it to bind your existing recall thread. Its normal use still binds rather than healing instantly.
3. Hold the flask and **sneak-use for five seconds** near a safe waystone. This restores health and refills your shared flask reservoir.
4. Explore new chunks for a **Mourning Cathedral**. Recover its ember to expand your flask, gather supplies, then push courts, observatories, and the Warden.

No forced inventory loss, soul-currency tax, or death penalty has been added. Vanilla death rules still apply. Older progression and equipment are retained.

## Rest and flask rules

- Hold use for **1.6 seconds** to drink: restores **8 health / four hearts**, consumes one charge, and applies a four-second flask cooldown.
- Damage interrupts drinking and resting. Drinking at full health or with an empty reservoir cannot begin.
- Initially **three charges per player**, not per flask. Crafting, swapping, relogging, or passing flasks between players does not refill the player's reservoir.
- Rest requires the Null Realm, grounded footing, a waystone within three blocks horizontally/two vertically, **no living hostiles within twelve blocks of the player bounding box**, and no active enrolled court/rift or nearby Warden fight. Safety is rechecked throughout the channel.
- Rest with a **Mourning Ember in your offhand** to consume it and permanently increase capacity by one, up to **five**. Hold the flask in your main hand.
- At the capacity cap, the ember is not consumed. Flask capacities and remaining charges survive restarts.
- The flask is realm-only. Its HUD shows charges and a use-progress bar.
- Replacement flask: glass bottle + Resonite Ingot + hushberry. Replacement Step: leather + dusk fiber + Resonite Ingot. Both shapeless.

## Mourning Cathedrals

A pointed, rib-vaulted nave, four pinnacles, stained windows and funerary benches stand above **two underground floors**. This is a vertically expanded, **15×15-block, chunk-contained** dungeon, not a sprawling multi-chunk castle. Its height is approximately 38 blocks including foundations and spires.

- The western flight descends from the nave to the crypt. The eastern flight continues to the ossuary. Both have two-wide stairs and headroom, not ladder shafts or intentional lethal drops.
- The crypt contains a Sentinel spawner; the ossuary contains a Shardstalker spawner. These are ordinary breakable spawners. Suppress or destroy them before lingering over the rite.
- Supply chests can contain repair kits, stew, ingots, ammunition, shards, and sigils.
- The lowest reliquary explains the rite. Activate **the crypt seal, then the nave seal, then the ossuary seal**: drowned → crowned → nameless.
- Correct seals glow. A mistake resets the sequence, without unavoidable damage or deleting the dungeon.
- Once all three are remembered, use the lowest reliquary for **one Mourning Ember, six shards, one Echo Sigil, and 180 XP**.
- Puzzle progress is stored in block states and survives unloading/restarts. The reward is **one per cathedral, shared world-wide**, not one per player. Claiming consumes the reliquary before item delivery. A fresh cathedral is needed for another ember.
- Exploration stays sandbox-friendly: no forced Adventure mode, indestructible surrounding dungeon shell, or invisible arena walls.

## Reworked enemies and combat

### Rift Sentinel
Original horned armored-knight mesh with pauldrons, shield plate, greatblade, and explicit windup/recovery poses replaces the vanilla zombie renderer. Sentinels remain adult-sized.

Its special attack is now a **32-tick committed forward cleave**, not an omnidirectional pulse. Circle behind it or leave its 3.5-block arc. A player hit with at least six incoming damage during the windup interrupts it into a 35-tick stagger. Otherwise the cleave deals nine damage, followed by 30 ticks of recovery. Normal melee is disabled during windup and recovery.

### Shardstalker
Original **six-limbed crystal hunter** mesh with mandibles, bladed legs, a crystal spine, crouching tell, lunge pose, and recovery animation replaces the vanilla spider renderer.

It marks a fixed position for 30 ticks, then lunges with ordinary collision—not teleportation—and recovers for 28 ticks. Step away rather than chasing the marked position. Normal melee is disabled during the special sequence. Its lunge can hit near its final physical position, so the ground marker is its intended destination, not a guaranteed explosion boundary.

Both mobs retain vanilla-derived navigation and spawning foundations; they are not entirely custom pathfinding systems.

### Pilgrim’s Step
Use to evade up to three blocks in your horizontal look direction; **sneak-use backsteps**. Grounded only, 2.5-second cooldown, modest hunger exhaustion. It samples the path in quarter-block increments and stops at obstruction, unloaded chunks, fluid, unsafe ground, or ledges. No invulnerability, wall phasing, or extra damage. It is an item ability, not a new keybinding.

### Null Warden
Its existing four phases and rematch progression remain. **Recovery openings grant +20% melee damage**, communicated through the HUD and hit particles; ranged hits do not gain this bonus. Invulnerability/pylon phases remain protected. The arena's cardinal gateways now have pointed gothic profiles.

## Realm and presentation

- More pronounced broad ridges and fractures in new terrain.
- Taller, leaning Hushwood, roots, reaching branches, layered broken canopies and hanging lights replace flat tree caps.
- Unequal clustered crystal/basalt spears replace single rectangular decoration poles.
- Subdued region-arrival titles with biome-specific subtitles and a cooldown against border flicker.
- A targeted-enemy health strip and textual cleave/lunge/recovery cues.
- Existing accessibility is retained: vanilla controls, no forced camera, no new camera shake or flashing overlays. F1 hides the HUD. Particle tells are supported by text, not color alone.
- Original deterministic textures for new equipment, seals, and the redesigned mobs. No new recorded music or voice acting is claimed.

## Upgrade and testing caveats

**Back up saves.** A fresh test world gives the most coherent terrain. New terrain and cathedrals appear only in newly generated chunks; old/new terrain can have seams. Existing enemy entities use the new model/behavior. Sanctuary migration only fills empty designated blocks, so occupied player builds are not replaced.

Automation covers Java/client compilation, pure rite/capacity/attack-boundary rules, player-record persistence, resource references and deterministic generation, plus dedicated-server worldgen, spawner data, entity and loot fixtures. It does **not** simulate a connected player completing the rite, channeling the flask, fighting a boss, or navigating stairs. Visual quality, encounter balance, controller feel, modpack compatibility, and multiplayer griefing/coordination need hands-on testing.
