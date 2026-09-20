# Minecraft 1.21.1 Fabric Mod: Unit & Faction Sandbox

A production-quality, server-authoritative Fabric 1.21.1 mod for creating, customizing, inspecting, and simulating autonomous Minecraft units, factions, armies, and battles.

The mod is fully data-driven: any compatible living Minecraft entity can become a custom unit with tailored attributes, equipment, inventories, tactical roles, behavior toggles, ranks, squads, and faction affiliations.

---

## 🌟 Key Features & Tool Suite

### 1. Main Creator Tool (`Unit Creator`)
- **Right-Click**: Opens the interactive **5-Tab Creator Dashboard GUI**.
- **Right-Click Unit**: Inspects the unit in-world, printing a detailed **Unit Dossier** (HP bar, gear, role, faction, ammo, veteran kills, target).
- **Left-Click (Attack Block/Air)**: Spawns the currently equipped unit template at target.
- **Shift + Right-Click**: Quick-cycles through saved units with audio feedback and action bar notification.
- **Shift + Left-Click**: Spawns a structured **Squad Formation (5 units)**.
- **Dynamic Tooltips**: Displays equipped unit, faction, role, base entity, health, damage, and controls.

### 2. Commander's Tactical War Horn (`commander_horn`)
- **Right-Click**: Blows the war horn (`SoundEvents.EVENT_RAID_HORN`), granting Strength, Speed, and Resistance to all allied faction units in a 32-block radius.
- **Shift + Right-Click**: Cycles tactical army orders:
  - `[Rally to Commander]`: Troops regroup and move to the player.
  - `[All-Out Charge]`: Troops aggressively seek and attack the nearest hostiles.
  - `[Hold Position & Defend]`: Troops lock formation and hold ground.

### 3. Faction Outpost Standard (`faction_banner`)
- **Right-Click on Block**: Plants a faction outpost standard.
- **Territory Claim**: Establishes a 32-block faction defense zone granting defending troops "Home Turf" bonuses.

---

## ⚔ Tactical Roles & Combat Behaviors

- **⚔ Melee Frontline & Tanks**: Custom melee combat for all mobs (including Villagers, Piglins, Golems) with attack animations, jump critical hits, and hit particles.
- **🛡 Active Shield Defense**: Units equipped with shields in main-hand or off-hand actively raise their shield when attacked, deflecting damage and triggering shield block sounds and spark particles.
- **🏹 Ranged Marksmen**: Bows and crossbows with tactical distance keeping (8–22 blocks), backpedaling, and real finite ammunition tracking (`ammo:` tag) or infinite ammo option.
- **💖 Field Medics & Healers**: Scans for injured allies in `healRange`, casts divine restoration rays with amethyst chime audio and heart particles, throws splash potions of healing/regen, cleanses negative effects, and drinks potions when self is low.
- **🧪 Throwable Potion Specialists & Alchemists**: Offensive units throw splash/lingering flasks of Harming, Poison, Slowness, and Weakness at enemy clusters, and buff flasks at allies.
- **🎆 Pyrotechnic Artillery & Fireworks**: Launches explosive firework rocket salvos and crossbow artillery at enemy groups, dealing multi-colored explosion bursts and area splash damage.
- **🗡 Flanking Assassins**: Sprints behind enemy lines targeting High-Value Targets (Commanders, Medics, Pyrotechnics) and executes deadly Backstab critical strikes (+175% critical damage).
- **🦅 Recon Scouts**: Fast patrol radius (32 blocks), spots enemies, marks hostile commanders with the Glowing effect (`StatusEffects.GLOWING`), and alerts nearby squad members.
- **🛡 Heavy Tanks**: Taunts nearby hostiles to protect squishy backlines and gains Iron Fortitude resistance when surrounded by 3+ enemies.
- **🏗 Combat Engineers**: Server-validated block placement! Builds defensive barricades/cover when under enemy fire, places torches in low light, and places ladders.
- **🎺 Commanders & Rallying Cry**: Blows war horns, buffing nearby allies with Strength, Speed, and Resistance.
- **👥 Squad Cohesion**: Squad members follow their commander, maintain formation, and focus-fire commander's target.
- **🍗 Food Consumption**: Injured units consume food from hand/inventory to recover health with eating sounds and particles.
- **🏃 Tactical Retreating**: Configurable retreat threshold (`retreatHealth`); low-health units retreat away from danger towards allies.
- **⭐ Veteran Promotion System**: Surviving warriors track battle kills (`kills:N`) and earn automated promotions:
  - **3 Kills**: *Veteran* (+4 Max HP, +1 DMG, Golden title).
  - **7 Kills**: *Elite Champion* (+8 Max HP, +2 DMG, permanent Resistance I, Diamond title).
  - **15 Kills**: *Legendary Hero* (+15 Max HP, +3.5 DMG, permanent Strength I & Glowing aura).

---

## 🛡 Faction System, Perks & Friendly Fire

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

---

## 🎨 Interactive Dashboard GUI

- **Tab 0: Unit Editor**: Unit identity, base entity cycle, role cycle, faction cycle, rank cycle, commander toggle, stats overview, 6 equipment slots (Head, Chest, Legs, Feet, Main Hand, Off Hand) + 9 unit inventory slots + player inventory drag-and-drop.
- **Tab 1: Saved Units Library**: Real-time search filter, unit cards with stats and badges, actions for Spawn, Spawn Squad, Cycle, Duplicate, and Delete.
- **Tab 2: Faction Manager**: Faction profiles, color swatches, active perks display, direct interactive relation cycling buttons.
- **Tab 3: Battle Sandbox**: Army size selector (8v8, 16v16, 24v24), structured formation spawning, live battle scoreboard, victory fanfares, and one-click battlefield clearing.
- **Tab 4: Simulation Settings**: AI tick rate, toggleable modules (Building AI, Potion Throwing, Fireworks Artillery, Shield Defense, Friendly Fire), factory reset button.

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
├── MyFirstMod.java              - Mod entrypoint, item groups, attack callbacks
├── MyFirstModClient.java        - Client entrypoint, screen handler registry
│
├── item/
│   ├── ModItems.java            - Item registry (Unit Creator, Commander Horn, Faction Banner)
│   ├── UnitCreatorItem.java     - Main tool item with right-click GUI & entity inspector
│   ├── CommanderHornItem.java   - Tactical war horn item for army orders
│   └── FactionBannerItem.java   - Placeable faction outpost standard
│
├── unit/
│   ├── UnitDefinition.java      - Template data model (NBT & JSON serializable)
│   ├── UnitSpawner.java         - Server entity spawner, perks, formatting, tags
│   ├── UnitInspector.java       - In-game unit dossier inspector
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
│   ├── AssassinAI.java          - Flanking, high-value target hunting, backstabs
│   ├── ScoutAI.java             - Recon scouting, glowing target marks, squad alerts
│   ├── TankAI.java              - Aggro taunting, crowd defense, iron fortitude
│   ├── VeteranProgression.java  - Automated kill tracking and battle promotions
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
│   └── BattleSandbox.java       - Army formation spawner, battle manager, victory fanfare
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
