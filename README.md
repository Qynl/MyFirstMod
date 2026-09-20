# ⚔️ LEGIONS & FACTIONS: Autonomous War Sandbox ⚔️
### *The Ultimate Autonomous Army, Faction & War Simulation Sandbox for Minecraft 1.21.1 (Fabric)*

```text
  ██████╗ ███████╗ ██████╗ ██╗ ██████╗ ███╗   ██╗███████╗
  ██╔══██╗██╔════╝██╔════╝ ██║██╔═══██╗████╗  ██║██╔════╝
  ██████╔╝█████╗  ██║  ███╗██║██║   ██║██╔██╗ ██║███████╗
  ██╔══██╗██╔══╝  ██║   ██║██║██║   ██║██║╚██╗██║╚════██║
  ██║  ██║███████╗╚██████╔╝██║╚██████╔╝██║ ╚████║███████║
  ╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝
           &  F A C T I O N S  :  W A R  S A N D B O X
```

---

## 📜 Welcome, Commander!

Ever wanted to command a **50-strong battalion of heavily armored Royal Villager Knights**, supported by **Skeleton Marksmen firing arrow volleys**, **Field Medics spraying healing mists**, **Pyrotechnic Artillery bombarding with firework rockets**, and **Necromancers summoning zombie hordes**?

**Legions & Factions** transforms Minecraft into a **server-authoritative tactical war simulator**. 
Create any living Minecraft entity into a custom battle template, equip it, configure its AI behaviors, assign it to a faction with unique perks, place outposts, whistle custom army formations, and watch legendary battles unfold!

![Dashboard Overview](docs/unit-sandbox-dashboard.png)

---

## 🎮 The Commander's Arsenal (Items & Tools)

| Item | Name | Primary Action | Shift Action | Tactical Role |
| :--- | :--- | :--- | :--- | :--- |
| 🪄 | **Legion Creator Tool** | **Right-Click**: Open Creator GUI<br>**Left-Click**: Spawn equipped unit<br>**Right-Click Unit**: View dossier | **Shift + Right-Click**: Quick-cycle units<br>**Shift + Left-Click**: Spawn 5-unit squad | Army creation, inspection & unit deployment |
| 📯 | **Tactical Formation Whistle** | **Right-Click**: Instantly reposition troops into formation | **Shift + Right-Click**: Cycle formation mode (*Shield Wall, Wedge, Circle, Spread*) | Battlefield unit positioning & battlelines |
| 🪄 | **Faction Commander's Scepter** | **Right-Click Block**: Set Move/Rally waypoint<br>**Right-Click Enemy**: Mark priority focus target | **Action bar**: View troops in radius | Real-time battlefield tactical command |
| 🎺 | **Commander's Tactical War Horn** | **Right-Click**: Sound rally horn (Area Strength & Speed) | **Shift + Right-Click**: Cycle orders (*Rally / Charge / Hold*) | Morale boosts & squad rally |
| ⚡ | **Reinforcement Drop Beacon** | **Right-Click Block**: Call down a 4-unit elite drop wave | *Cooldown: 10s* | Emergency reinforcements |
| 🚩 | **Faction Outpost Standard** | **Right-Click Block**: Establish faction outpost territory | *Defensive Zone: 32 blocks* | "Home Turf" territorial defense |

---

## 🛡️ Tactical Roles & Combat Behaviors

```text
              [ FRONT LINE ]
        ⚔ Melee Tanks & Knights
        🛡 Shield Wall Formations
                   │
              [ MID LINE ]
        🏹 Ranged Marksmen & Snipers
        💣 Siege Bombardiers & Mortars
        🎆 Pyrotechnic Fireworks Artillery
                   │
              [ BACK LINE ]
        💖 Field Medics & Healers
        🎶 Battlefield Bards (War Songs)
        ☠ Dark Necromancers & Summoners
        👑 Commanders & Tacticians
```

### 1. ⚔ Melee Tanks & Knights (`TankAI` / `CombatAI`)
- **Armed & Dangerous**: Gives melee weapons, custom attack animations, and sprint crits to *any* mob (even Villagers!).
- **Shield Deflection**: Raises shields during combat, deflecting incoming attacks with spark bursts.
- **Taunt**: Draws enemy fire away from squishy medics and archers.

### 2. 🎆 Pyrotechnic Artillery & Fireworks (`FireworksArtilleryAI`)
- **Long-Range Salvos**: Pyros fire fireworks rockets and explosive crossbow shells into enemy ranks.
- **Detonation Fireworks**: Deals massive AoE splash damage and triggers colorful particle fireworks explosions!

### 3. 💖 Field Medics & Healers (`HealerMedicAI`)
- **Restoration Rays**: Scans for wounded allies in `healRange`, casts divine restoration beams, cleanses negative status effects (Poison/Wither), and throws splash healing flasks.
- **Self-Preservation**: Consumes healing potions or golden apples when injured.

### 4. 🧪 Throwable Potion Alchemists (`ThrowablePotionAI`)
- **Ballistic Splash Flasks**: Launches splash and lingering flasks of Harming, Poison, Slowness, and Weakness over enemy frontline units.

### 5. ☠ Dark Necromancers (`NecromancerAI`)
- **Unholy Summoning**: Channels dark summoning pentagrams in combat to summon armed skeleton and zombie thralls.

### 6. 🩸 Bloodrage Berserkers (`BerserkerAI`)
- **Bloodrage**: When falling below 50% HP, roars fiercely and enters Bloodrage (gaining Strength II, Speed II, and Haste II with fire auras).

### 7. 🎶 Battlefield Bards (`BardAI`)
- **War Song Melodies**: Plays harp chords and emits musical note particles, providing ongoing Speed, Regeneration, and Resistance to all nearby allies.

### 8. 💣 Siege Bombardiers (`BombardierAI`)
- **Mortar Canisters**: Launches explosive charges at enemy fortifications, cracking enemy lines with shockwaves.

### 9. 🌿 Nature Druids (`DruidAI`)
- **Rooting Vines**: Entangles enemies in thorny roots (Slowness IV) and summons Nature Spirit Wolves.

### 10. ✨ Holy Paladins (`PaladinAI`)
- **Holy Smite**: Radiates golden light, deals +60% radiant damage to undead, and releases healing waves to nearby allies on hit.

### 11. 🗡 Flanking Assassins (`AssassinAI`)
- **Backstabs**: Sprints behind enemy lines to assassinate high-value commanders and medics with +175% backstab critical damage.

### 12. 🦅 Recon Scouts (`ScoutAI`)
- **Enemy Spotting**: High-speed recon unit that spots enemies, marks hostile commanders with Glowing, and alerts squadmates.

### 13. 🏗 Combat Engineers (`EngineerBuildingAI`)
- **Field Barricades**: Places 2-block high tactical barricades when under fire, builds torches in dark areas, and places ladders.

---

## ⭐ Veteran Progression System

Surviving warriors gain battle experience and battlefield promotions!

```text
  [ Recruit ] ────► [ Veteran ] ────► [ Elite Champion ] ────► [ Legendary Hero ]
   0-2 Kills          3 Kills              7 Kills                15 Kills
                   +4 Max Health        +8 Max Health          +15 Max Health
                   +1.0 Damage          +2.0 Damage            +3.5 Damage
                   Golden Title         Resistance I Aura      Strength I & Glowing
```

---

## 🏛️ Default Factions & Relations Matrix

```text
 ┌───────────────────────┬────────────┬──────────────────────────────────────┐
 │ Faction               │ Color      │ Signature Units                      │
 ├───────────────────────┼────────────┼──────────────────────────────────────┤
 │ 🔵 Kingdom of Eldoria │ Royal Blue │ Royal Knights, Archers, Medics, Pyro │
 │ 🔴 Iron Raiders       │ Red        │ Berserkers, Crossbowmen, Alchemists  │
 │ 🟢 Village Alliance   │ Emerald    │ Town Militia, Guards, Cleric Healers │
 │ 🟣 Undead Legion      │ Purple     │ Necromancers, Dread Knights, Snipers │
 │ 🔷 Arcane Order       │ Cyan       │ Battle Mages, Bards, Arcane Golems   │
 └───────────────────────┴────────────┴──────────────────────────────────────┘
```

### 14 Faction Perks:
1. **Military Discipline**: +15% unit attack damage.
2. **Heavy Armor**: +20% armor rating & bonus knockback resistance.
3. **Swift Army**: +15% army movement speed.
4. **Rallying Cry**: Commanders grant boosted Strength II & Speed II.
5. **Battle Regeneration**: Steady passive HP regeneration.
6. **Night Fighters**: Night Vision & +25% combat power at night.
7. **Hardened Veterans**: Permanent extra HP & damage for battle veterans.
8. **Pyrotechnics**: +50% firework explosive splash damage & radius.
9. **Holy Might**: +50% healer potency & reach.
10. **Iron Fortification**: 15% reduction to all incoming damage.
11. **Dark Necromancy**: Slain warriors rise as friendly undead thralls.
12. **Berserk Fury**: Injured units automatically enter Bloodrage.
13. **Harmonic War Song**: Allied units in battle gain passive speed and regeneration.
14. **Fire Mastery**: Attacks ignite targets and units are immune to fire.

---

## 🖥️ 5-Tab Interactive Creator Dashboard

```text
  ┌────────────────────────────────────────────────────────────────────────┐
  │ [⚔ EDITOR]  [📋 UNITS]  [🛡 FACTIONS]  [💥 BATTLE]  [⚙ SETTINGS]       │
  ├────────────────────────────────────────────────────────────────────────┤
  │                                                                        │
  │  [⮜ Next Unit]  [➕ Duplicate]           [💾 Save Slots]               │
  │  [Entity: Villager ⮞ Skeleton ⮞ ...]     [⚔ Spawn Unit (1)]           │
  │  [Role: Melee ⮞ Ranged ⮞ Medic ⮞ ...]    [🛡 Spawn Squad (5)]          │
  │  [Faction: Kingdom ⮞ Raiders ⮞ ...]      [🗑 Delete Unit]              │
  │  [Commander: Toggle ON/OFF]                                            │
  │                                                                        │
  │  Equipment Slots:  [ H ] [ C ] [ L ] [ F ]  |  [ Main ] [ Offhand ]    │
  │  Unit Inventory:   [ 1 ] [ 2 ] [ 3 ] [ 4 ] [ 5 ] [ 6 ] [ 7 ] [ 8 ] [ 9] │
  │  Player Inventory: (Drag-and-drop any gear, weapons, food, potions)   │
  └────────────────────────────────────────────────────────────────────────┘
```

---

## 🕹️ Command Reference

```bash
# Unit Management
/unit list                                    # List all saved unit templates
/unit stats                                   # View persistent battle statistics
/unit spawn <id> [count] [x y z]              # Spawn unit at player or coordinates
/unit squad <id> [count]                      # Spawn structured squad
/unit create <id> <entity> <name>             # Create new template
/unit delete <id>                             # Delete template
/unit duplicate <id>                          # Clone template
/unit equip <id> <slot> <item>                # Equip armor or weapons
/unit inventory <id> <item> [count]           # Add ammunition, food, potions, blocks
/unit set <id> health/damage/speed/armor <v>  # Edit attribute
/unit set <id> role <role>                    # Set role (melee, ranged, medic, etc.)
/unit set <id> rank <rank>                    # Set rank (soldier, captain, etc.)
/unit set <id> squad <name>                   # Assign squad name
/unit set <id> commander <true|false>         # Toggle commander status

# Factions & Diplomacy
/faction list                                 # List all factions
/faction create <id> <name>                   # Create new faction
/faction relation <from> <to> <relation>      # Set ALLIED, NEUTRAL, HOSTILE
/faction perk <faction> <perk>                # Add faction perk

# Battle Sandbox
/battle start <faction1> <faction2> [size]    # Deploy armies in battlefield formation
/battle clear                                 # Despawn all active battle mobs
/battle stats                                 # View kills & deaths scoreboard
/battle reset                                 # Reset statistics
```

---

## ⚡ Performance & Server Authority

* **100% Server Authoritative**: Client screens only send validated requests; all spawning, stats, inventory, equipment, and AI execute on the server.
* **Spatial & Throttled Queries**: AI ticks every 10 ticks and uses spatial bounding box queries—**zero full-world O(N²) scans**.
* **Zero Zombification in Overworld**: Piglin and Piglin Brute units automatically have zombification disabled so Nether units can fight in the Overworld!
* **Sunlight Protection**: Custom undead units (Zombies, Skeletons) do not burn during daytime battles.
* **Persistent World Data**: All units, factions, relationships, perks, and battle statistics persist across world reloads and server restarts.

---

### 📦 Build & Requirements
* **Minecraft Version**: `1.21.1`
* **Fabric Loader**: `>=0.16.10`
* **Fabric API**: `0.116.17+1.21.1`
* **Java**: `21`
