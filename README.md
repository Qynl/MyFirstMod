# Minecraft 1.21.1 Fabric Mod: Unit & Faction Sandbox

A production-quality, server-authoritative Fabric 1.21.1 mod for creating, customizing, inspecting, commanding, and simulating autonomous Minecraft units, factions, armies, and tactical battles.

The mod is fully data-driven: any compatible living Minecraft entity can become a custom unit with tailored attributes, equipment, inventories, tactical roles, behavior toggles, ranks, squads, and faction affiliations.

---

## 🌟 Comprehensive Tactical Tool Suite

### 1. Main Creator Tool (`unit_creator`)
- **Right-Click**: Opens the interactive **5-Tab Creator Dashboard GUI**.
- **Right-Click Unit**: Inspects the targeted unit in-world, printing a detailed **Unit Dossier** (HP bar, gear, role, faction, ammo, veteran kills, target).
- **Left-Click (Attack Block/Air)**: Spawns the currently equipped unit template at target.
- **Shift + Right-Click**: Quick-cycles through saved units with audio feedback and action bar notification.
- **Shift + Left-Click**: Spawns a structured **Squad Formation (5 units)**.
- **Dynamic Tooltips**: Displays equipped unit, faction, role, base entity, health, damage, and controls.

### 2. Tactical Formation Whistle (`tactical_whistle`)
- **Right-Click**: Instantly repositions all nearby allied troops into tactical formation!
- **Shift + Right-Click**: Cycles tactical formation mode:
  - `[Shield Wall (Defensive Line)]`: Frontline tanks and melee units lock shields side-by-side.
  - `[Wedge (Shock Assault)]`: Spearhead formation for punching through enemy ranks.
  - `[Circle (Perimeter Guard)]`: Surrounds and protects the commander facing outward.
  - `[Loose Spread (Anti-Artillery)]`: Disperses units to minimize AoE firework and potion blast damage.

### 3. Faction Commander's Scepter (`faction_scepter`)
- **Right-Click on Block**: Places a **Tactical Waypoint Marker** with beacon beams; all nearby allied troops march to position.
- **Right-Click on Enemy**: Designates a **Priority Focus Target** with glowing crosshairs; all nearby troops concentrate fire.

### 4. Reinforcement Drop Beacon (`reinforcement_beacon`)
- **Right-Click on Block**: Summons an instant 4-unit elite tactical reinforcement wave with lightning effects and beacon chimes.

### 5. Commander's Tactical War Horn (`commander_horn`)
- **Right-Click**: Blows the war horn (`SoundEvents.EVENT_RAID_HORN`), granting Strength, Speed, and Resistance to all allied faction units in a 32-block radius.
- **Shift + Right-Click**: Cycles tactical orders (`[Rally to Commander]`, `[All-Out Charge]`, `[Hold Position & Defend]`).

### 6. Faction Outpost Standard (`faction_banner`)
- **Right-Click on Block**: Plants a faction outpost standard establishing a 32-block territory defense zone with "Home Turf" buffs.

---

## ⚔ Tactical Roles & Combat Behaviors

- **⚔ Melee Frontline & Tanks (`TankAI`)**: Custom melee combat for all mobs with attack animations, jump crits, shield blocking, and aggro taunts to protect squishy backlines.
- **🛡 Active Shield Defense (`ShieldDefenseAI`)**: Units equipped with shields actively raise their shield when attacked, deflecting damage with blocking audio and sparks.
- **🏹 Ranged Marksmen (`RangedAI`)**: Bows and crossbows with tactical distance keeping (8–22 blocks), backpedaling, and real finite ammunition tracking (`ammo:` tag) or infinite ammo option.
- **💖 Field Medics & Healers (`HealerMedicAI`)**: Scans for injured allies in `healRange`, casts divine restoration rays with amethyst chime audio and heart particles, throws splash potions of healing/regen, and cleanses negative effects.
- **🧪 Throwable Potion Specialists & Alchemists (`ThrowablePotionAI`)**: Parabolic projectile throwing of splash/lingering flasks of Harming, Poison, Slowness, Weakness, and buff potions.
- **🎆 Pyrotechnic Artillery & Fireworks (`FireworksArtilleryAI`)**: Launches explosive firework rocket salvos and crossbow artillery at enemy groups, dealing multi-colored explosion bursts and AoE damage.
- **💣 Siege Bombardiers (`BombardierAI`)**: Launches heavy explosive mortar canisters at enemy fortifications and clustered units.
- **🌿 Nature Druids (`DruidAI`)**: Entangles enemies in rooting thorn vines (Slowness IV & Weakness) and summons Nature Spirit Wolf companions.
- **✨ Holy Paladins (`PaladinAI`)**: Heavy armored crusaders emitting radiant auras, dealing +60% smite bonus against undead, and healing nearby allies on every hit.
- **☠ Dark Necromancers (`NecromancerAI`)**: Channels dark summoning pentagram rituals to raise armed undead thralls during battle.
- **🩸 Bloodrage Berserkers (`BerserkerAI`)**: Enters Bloodrage below 50% HP (Strength II, Speed II, Haste II, flame aura, and ferocious roars).
- **🎶 Battlefield Bards (`BardAI`)**: Plays harmonic war songs on harp chords, providing continuous Speed, Regeneration, and Resistance auras.
- **🗡 Flanking Assassins (`AssassinAI`)**: Sprints behind enemy lines targeting High-Value Targets and executes deadly Backstab critical strikes (+175% critical damage).
- **🦅 Recon Scouts (`ScoutAI`)**: Fast patrol radius (32 blocks), spots enemies, marks hostile commanders with Glowing, and alerts squad members.
- **🏗 Combat Engineers (`EngineerBuildingAI`)**: Server-validated block placement! Builds defensive barricades/cover when under enemy fire, places torches in low light, and places ladders.
- **⭐ Veteran Promotion System (`VeteranProgression`)**: Surviving warriors track battle kills (`kills:N`) and earn automated promotions:
  - **3 Kills**: *Veteran* (+4 Max HP, +1 DMG, Golden title).
  - **7 Kills**: *Elite Champion* (+8 Max HP, +2 DMG, permanent Resistance I, Diamond title).
  - **15 Kills**: *Legendary Hero* (+15 Max HP, +3.5 DMG, permanent Strength I & Glowing aura).

---

## 🛡 Faction System, Perks & Friendly Fire

- **Faction Model**: ID, Name, Custom Hex Color, Description, Relations Matrix, and Perks.
- **Relations**: `ALLIED` (Green), `NEUTRAL` (Slate), `HOSTILE` (Red).
- **Friendly Fire Protection**: Attacks and damage between members of the same or allied factions are strictly prevented.
- **14 Data-Driven Faction Perks**:
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
  - `Dark Necromancy`: Slain warriors rise as friendly thralls.
  - `Berserk Fury`: Injured units enter Bloodrage (+50% attack & speed).
  - `Harmonic War Song`: Allied units gain passive speed and regeneration.
  - `Fire Mastery`: Attacks ignite targets and units are immune to fire.

---

## 🎨 Interactive Dashboard GUI

- **Tab 0: Unit Editor**: Unit identity, base entity cycle, role cycle, faction cycle, rank cycle, commander toggle, stats overview, 6 equipment slots (Head, Chest, Legs, Feet, Main Hand, Off Hand) + 9 unit inventory slots + player inventory drag-and-drop.
- **Tab 1: Saved Units Library**: Real-time search filter, unit cards with stats and badges, actions for Spawn, Spawn Squad, Cycle, Duplicate, and Delete.
- **Tab 2: Faction Manager**: Faction profiles, color swatches, active perks display, direct interactive relation cycling buttons.
- **Tab 3: Battle Sandbox**: Any-faction matchup selector (Side A cycle, Side B cycle), army size selector (8v8, 16v16, 24v24), structured formation spawning, live battle scoreboard, victory fanfares, and one-click battlefield clearing.
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
