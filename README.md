# Unit & Faction Sandbox

A Fabric 1.21.1 foundation for creating configurable Minecraft units and autonomous faction battles. The project is deliberately data-driven: a unit is an entity type plus attributes, equipment, role, and faction metadata—not a special-case mob.

## Version 0.1 vertical slice

1. Give yourself the **Unit Creator** (`/give @s myfirstmod:unit_creator`).
2. Right-click it to open the server-authoritative creator screen.
3. Create a **Village Guard**, **Royal Archer**, and the sample hostile factions.
4. Click **Equip first saved unit**, close the screen, then left-click a block or entity with the tool to spawn it.
5. Spawn units from the two factions and watch the bounded, throttled faction AI acquire hostile targets. Same-faction units are never valid targets.

The guard demonstrates the important villager path: it is still a vanilla villager, with real armor, sword, shield, health, and attack attributes. The archer remains a skeleton and uses its vanilla ranged combat goals with the configured bow.

## Architecture

- `unit/UnitDefinition` is serializable template data and applies only real vanilla attributes/equipment.
- `unit/UnitWorldData` stores the library in server PersistentState, so definitions survive reloads and restarts.
- `faction/Faction` stores relationships (`ALLIED`, `NEUTRAL`, `HOSTILE`) and persists them with the library.
- `unit/UnitSystem` runs every ten server ticks, performs spatially bounded target queries, caches existing targets, and never scans an entity cross-product.
- `gui/` uses a normal server ScreenHandler. Buttons are validated and executed server-side; the client only renders controls.
- `UnitCreatorItem` references a saved unit through server state rather than embedding a unit definition in the item.

The same library is also fully addressable from the server command layer, which is useful for operators, testing, and future datapack integration:

```text
/unit create royal_guard minecraft:villager Royal Guard
/unit set royal_guard health 40
/unit set royal_guard damage 8
/unit set royal_guard role melee
/unit equip royal_guard mainhand minecraft:iron_sword
/unit equip royal_guard offhand minecraft:shield
/unit inventory royal_guard minecraft:bread 16
/unit set royal_guard retreat 0.20
/unit faction create kingdom Kingdom of Eldoria
/unit set royal_guard faction kingdom
/unit faction create raiders Iron Raiders
/unit faction relation kingdom raiders HOSTILE
/unit spawn royal_guard
```

This repository is now exclusively the Unit & Faction Sandbox. The former boss, portal, dimension, relic, and custom-renderer content has been removed rather than carried as unrelated legacy baggage. The core is deliberately ready for the next systems: inventories, item capabilities, squads, perks, building validation, imports/exports, and battle setup can all use the same saved definitions and server authority.

## Build

Minecraft 1.21.1, Fabric Loader 0.16.10+, Fabric API, and Java 21 are required. Run `gradle build` with a Gradle installation or use the repository's CI workflow.

## Support and potion behavior

Medic and support roles are active gameplay roles. They search a bounded area for injured members of their own faction, heal the most injured ally, apply regeneration, and consume potion items on a cooldown. Use the GUI's **New Medic** action or configure a saved template with `/unit set <id> role medic`. Potion, food, weapon, shield, ranged weapon, ammunition, tool, and block capability classification is centralized so future behaviors can add real world interactions without adding item-name special cases.
