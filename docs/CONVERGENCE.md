# 1.4 — Convergence

## Observatories and the Rift Herald
Explore **newly generated Null Realm chunks** for ruined four-spired observatories. Use the central Convergence Anchor in Survival/Adventure, outside Peaceful. Alive nearby survival players within 24 blocks join at activation; late arrivals are not enrolled. Party difficulty scales up to four players.

1. Sneak within 2.5 blocks of each glowing seal for three seconds. Partial progress decays when unattended; completed seals stay grounded. Sentinels defend the site.
2. All three seals trigger a three-second emergence. Guardians disappear.
3. Defeat the **Rift Herald**, an original animated floating construct with a crown and orbiting shards. Its two-second tells mark targeted bursts and expanding ring attacks. Move off the burst, or inside three/outside six blocks for the ring. It attacks faster below half health. No terrain destruction.

The Herald has 180–390 health. Each enrolled player present for at least five seconds receives one Astral Core, four shards, and 120 XP via persisted reward delivery, including offline participants when they return. Summoned standalone Heralds do not award cores. A victory locks the anchor for ten minutes. Events expire after ten minutes or thirty seconds without eligible nearby players. Restarting cancels active fights and cleans up their actors; unclaimed rewards persist.

## Relic attunement
A forge is added at the sanctuary (-6, 81, 160), only if that block is empty. You can also craft one: `IRI / BBB`, where I is Resonite Ingot, R is Resonance Matrix, and B is Nullstone Bricks.

Craft a rune with **one Astral Core + one Resonant Shard** and:

| Ingredient | Rune | Effect |
|---|---|---|
| Golden apple | Vigor | Successful ability hits restore one heart; shared ten-second cooldown |
| Feather | Gale | Casting grants Speed I for four seconds; shared ten-second cooldown |
| Prism dust | Focus | Ability cooldowns reduced by 20%, minimum one second |

Hold a Nullblade (either tier), Prism Staff, Cinder Maul, or Rift Aegis in your main hand, rune offhand, and use the forge. One socket per relic. Sneak to replace an existing rune; it is consumed, not refunded. Attunement preserves unrelated custom data/lore. Vigor and Gale timers persist and are shared across gear swaps. Aegis Vigor requires a successful reflection.

## Renewable cultivation
Craft **two Hush Nurseries** with two hushberries, dusk fiber, and lumen moss. Plant on dirt, grass, farmland, or realm/vanilla moss. Four growth stages, bonemeal support, natural growth in realm darkness; outside the realm requires light level 8+. Use a mature nursery to gather two berries and one fiber, returning it to stage one. Breaking any stage returns the nursery.

## Compatibility and verification
Fabric / Minecraft 1.21.1 / Java 21. Existing progression remains; back up worlds before upgrading. Observatory terrain is not retroactively generated. Forge migration never overwrites an occupied block. Attacks and world state are server-authoritative.

Automated coverage includes resource closure and deterministic asset regeneration, pure encounter rules, persistence, main/client compilation, and dedicated-server fixtures for observatories, entity spawning, nursery loot, and previous content. It does **not** replace a connected-client visual, combat-balance, or multiplayer playtest.
