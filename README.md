# Minecraft 1.21.1 Fabric Mod: Unit & Faction Sandbox

A production-quality, server-authoritative Fabric 1.21.1 mod for creating, customizing, and simulating autonomous Minecraft units, factions, armies, and battles.

The mod is fully data-driven: any compatible living Minecraft entity can become a custom unit with tailored attributes, equipment, inventories, tactical roles, behavior toggles, ranks, squads, and faction affiliations.

---

## 🌟 Key Features

### 1. Main Creator Tool (`Unit Creator`)
- **Right-Click**: Opens the full interactive **Creator Dashboard GUI**.
- **Left-Click (Attack Block/Air)**: Spawns the currently equipped unit template at target.
- **Shift + Right-Click**: Quick-cycles through saved units with audio feedback and action bar notification.
- **Shift + Left-Click**: Spawns a structured **Squad Formation (5 units)**.
- **Dynamic Tooltips**: Displays equipped unit, faction, role, base entity, health, damage, and controls.

### 2. Tactical Roles & Combat Behaviors
- **⚔ Melee Frontline & Tanks**: Custom melee combat for all mobs (including Villagers, Piglins, Golems) with attack animations, jump critical hits, and hit particles.
- **🛡 Active Shield Defense**: Units equipped with shields in main-hand or off-hand actively raise their shield when attacked, deflecting damage and triggering shield block sounds and spark particles.
- **🏹 Ranged Marksmen**: Bows and crossbows with tactical distance keeping (8–22 blocks), backpedaling, and real finite ammunition tracking (`ammo:` tag) or infinite ammo option.
- **💖 Field Medics & Healers**: Scans for injured allies in `healRange`, casts divine restoration rays with amethyst chime audio and heart particles, throws splash potions of healing/regen, cleanses negative effects, and drinks potions when self is low.
- **🧪 Throwable Potion Specialists & Alchemists**: Offensive units throw splash/lingering flasks of Harming, Poison, Slowness, and Weakness at enemy clusters, and buff flasks at allies.
- **🎆 Pyrotechnic Artillery & Fireworks**: Launches explosive firework rocket salvos and crossbow artillery at enemy groups, dealing multi-colored explosion bursts and area splash damage.
- **🏗 Combat Engineers**: Server-validated block placement! Builds defensive barricades/cover when under enemy fire, places torches in low light, and places ladders.
- **🎺 Commanders & Rallying Cry**: Blows war horns (`SoundEvents.EVENT_RAID_HORN`), buffing nearby allies with Strength, Speed, and Resistance.
- **👥 Squad Cohesion**: Squad members follow their commander, maintain formation, and focus-fire commander's target.
- **🍗 Food Consumption**: Injured units consume food from hand/inventory to recover health with eating sounds and particles.
- **🏃 Tactical Retreating**: Configurable retreat threshold (`retreatHealth`); low-health units retreat away from danger towards allies.

### 3. Faction System, Perks & Friendly Fire
- **Faction Model**: ID, Name, Custom Hex Color, Description, Relations Matrix, and Perks.
- **Relations**: `ALLIED` (Green), `NEUTRAL` (Slate), `HOSTILE` (Red).
- **Friendly Fire Protection**: Attacks and damage between members of the same or allied factions are strictly prevented.
- **10 Data-Driven Faction Perks**:
  - `Military Discipline`: +15% unit attack damage.
  - `Heavy Armor`: +20% armor rating & bonus knockback resistance.
  - `Swift Army`: +15% army movement speed.
  - `Rallying Cry`: Commanders grant Strength II & Speed II.
  - `Battle Regeneration`: Steady passive HP regeneration.
  - `Night Fighters`: Night Vision & +25% combat power during night.
  - `Hardened Veterans`: Extra HP & attack for surviving warriors.
  - `Pyrotechnics`: +50% firework explosive splash damage & radius.
  - `Holy Might`: +50% healer power & reach.
  - `Iron Fortification`: 15% reduction to all incoming damage.

### 4. Interactive Dashboard GUI
- **Tab 0: Unit Editor**: Unit identity, role, rank, squad, commander toggle, stats overview, 6 equipment slots (Head, Chest, Legs, Feet, Main Hand, Off Hand) + 9 unit inventory slots + player inventory drag-and-drop.
- **Tab 1: Saved Units Library**: Real-time search filter, unit cards with stats and badges, actions for Spawn, Spawn Squad, Cycle, Duplicate, and Delete.
- **Tab 2: Faction Manager**: Faction profiles, color swatches, active perks display, relations matrix, friendly fire rules.
- **Tab 3: Battle Sandbox**: Live army builder (Kingdom of Eldoria vs Iron Raiders), `[START BATTLE]`, `[CLEAR ALL BATTLE MOBS]`, `[RESET BATTLE STATS]`, live scoreboard.
- **Tab 4: Simulation Settings**: AI tick rate, toggleable modules (Building AI, Potion Throwing, Fireworks Artillery, Shield Defense, Friendly Fire), factory reset button.

### 5. Battle Statistics & World Persistence
- Server PersistentState (`UnitWorldData`, `BattleStats`) survives world saves, restarts, and server reloads.
- Tracks Kills, Deaths, Damage Dealt, and Battles Won per faction.

---

## 🎮 Commands

### `/unit` Commands
```text
/unit list                                    - List all saved unit templates
/unit stats                                   - View persistent battle statistics
/unit spawn <id> [count] [x y z]              - Spawn units at player or coordinate
/unit squad <id> [count]                      - Spawn squad formation
/unit create <id> <entity> <name>             - Create new unit template
/unit delete <id>                             - Delete unit template
/unit duplicate <id>                          - Clone unit template
/unit equip <id> <slot> <item>                - Equip item to HEAD, CHEST, LEGS, FEET, MAINHAND, OFFHAND
/unit inventory <id> <item> [count]           - Add items (arrows, potions, fireworks, blocks) to inventory
/unit set <id> health/damage/speed/armor <val>- Set unit attribute
/unit set <id> role <melee|ranged|medic|...>  - Set combat role
/unit set <id> rank <soldier|captain|...>     - Set unit rank
/unit set <id> squad <name>                   - Assign to squad
/unit set <id> commander <true|false>         - Set commander status
/unit set <id> faction <factionId>            - Assign faction
/unit set <id> retreat <0.0 - 0.99>           - Set retreat health percentage
/unit set <id> heal-range <blocks>            - Set healer reach
```

### `/faction` Commands
```text
/faction list                                 - List all registered factions
/faction create <id> <name>                   - Create a new faction
/faction relation <from> <to> <relation>      - Set relation: ALLIED, NEUTRAL, HOSTILE
/faction perk <faction> <perk>                - Add faction perk
```

### `/battle` Commands
```text
/battle start <faction1> <faction2> [size]    - Start battle between two factions
/battle clear                                 - Clear all active battle mobs from world
/battle stats                                 - Display kill/death scoreboard
/battle reset                                 - Reset all statistics
```

---

## 🏗 Modular Architecture

```text
dev.qynl.myfirstmod/
├── MyFirstMod.java              - Mod entrypoint, item groups, attack events
├── MyFirstModClient.java        - Client entrypoint, screen handler registry
│
├── item/
│   ├── ModItems.java            - Item registry
│   └── UnitCreatorItem.java     - Tool with right-click GUI, left-click spawn, shift-click actions
│
├── unit/
│   ├── UnitDefinition.java      - Template data model (NBT & JSON serializable)
│   ├── UnitSpawner.java         - Server entity spawner, perks, formatting, tags
│   ├── UnitWorldData.java       - PersistentState storage & default presets
│   └── BattleStats.java         - Persistent battle kill/death/damage metrics
│
├── faction/
│   ├── Faction.java             - Faction model, colors, relations, perks
│   ├── FactionRelation.java     - ALLIED, NEUTRAL, HOSTILE enum
│   ├── FactionPerk.java         - 10 data-driven perk definitions
│   └── FactionManager.java      - Faction relation queries & friendly fire logic
│
├── ai/
│   ├── UnitSystem.java          - Central throttled server simulation loop
│   ├── CombatAI.java            - Melee combat, attack animations, crits
│   ├── RangedAI.java            - Bow & crossbow tactical distance and firing
│   ├── HealerMedicAI.java       - Healing rays, splash healing, cleansing
│   ├── ThrowablePotionAI.java   - Splash potions of harm/poison/slowness/strength
│   ├── FireworksArtilleryAI.java- Crossbow artillery, rocket salvos, AoE bursts
│   ├── ShieldDefenseAI.java     - Shield blocking defense against attacks
│   ├── EngineerBuildingAI.java  - Server block placement, barricades, torches
│   ├── SquadAI.java             - Squad follower behavior & leader protection
│   ├── CommanderAI.java         - War horn rallies, strength & speed buffs
│   ├── FoodAI.java              - Eating food when injured
│   └── RetreatAI.java           - Low health tactical retreat
│
├── equipment/
│   └── ItemCapabilities.java    - Classification: weapon, shield, ammo, potion, block, etc.
│
├── battle/
│   └── BattleSandbox.java       - Army formation spawner, battle manager, cleanup
│
├── gui/
│   ├── ModScreenHandlers.java   - Screen handler registry
│   └── CreatorScreenHandler.java- Server-authoritative slots and button dispatch
│
├── client/
│   └── CreatorScreen.java       - 5-tab GUI dashboard with unit editor & library
│
└── command/
    └── UnitCommands.java        - Full command suite for units, factions, battles
```

---

## ⚙ Build & Requirements

- **Minecraft**: 1.21.1
- **Fabric Loader**: >=0.16.10
- **Fabric API**: 0.116.17+1.21.1
- **Java**: 21
