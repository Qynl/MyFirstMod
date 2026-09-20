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

## Squads, commanders, food, and battle stats

```text
/unit set royal_guard rank captain
/unit set royal_guard squad first_guard
/unit set royal_guard commander true
/unit inventory royal_guard minecraft:bread 16
/unit stats
```

Commanders rally nearby faction members with Strength. Squad members follow their commander. Injured units consume food from their hand or compatible inventory. Kills and deaths are persisted per faction in server world data and can be viewed with `/unit stats`.

## Target priorities and ammunition

Units can prioritize commanders, medics, ranged units, weakest targets, or nearest targets:

```text
/unit set royal_guard priority commander
```

Ranged templates receive an ammunition budget from their saved arrow inventory. The simulation tracks that budget on each spawned unit and stops custom ranged units when ammunition is exhausted instead of silently granting unlimited supplies.

## Faction perks

Faction perks are data-driven and persistent:

```text
/unit faction perk kingdom military_discipline
/unit faction perk kingdom heavy_armor
/unit faction perk kingdom swift_army
/unit faction perk kingdom regeneration
```

These affect real attributes or server-side healing rather than merely changing UI text.

## Creator dashboard

The creator is designed as a dashboard rather than a single-purpose spawner. It has separate Units, Factions, Battle, and Settings areas, a searchable unit library, template cards, duplicate/delete actions, and a synchronized six-slot equipment editor.

![Unit and Faction Sandbox dashboard](docs/unit-sandbox-dashboard.png)

> The image above is a design reference for the dashboard layout. In-game rendering uses Minecraft's native widgets and inventory slot rendering so the equipment is interactive and server-synchronized.

### Unit authoring workflow

1. Open the Unit Creator from the Tools creative tab or give it with `/give @s myfirstmod:unit_creator`.
2. Create a guard, archer, or medic template from the Units tab.
3. Drag armor and items into the equipment editor and press **Save Equipment**.
4. Use `/unit` commands for arbitrary registry entity types, names, stats, roles, inventories, squads, ranks, and priorities.
5. Equip the saved template and spawn it with the creator tool.

### Gameplay loops

- Build a faction with allied, neutral, or hostile relationships.
- Create frontline, ranged, medic, scout, commander, and support units.
- Give ranged units finite arrow supplies and medics finite potion supplies.
- Assign squads and commanders so units rally and regroup.
- Add faction perks that affect actual attributes and regeneration.
- Start the sample battle and inspect persistent faction kill/death statistics.

### Screenshots and future UI

The UI is intentionally structured around reusable panels. The next editor expansion can add entity selection, numeric stat fields, behavior toggles, target-priority chips, inventory pages, faction relation matrices, and battle army composition without replacing the server-side data model.
