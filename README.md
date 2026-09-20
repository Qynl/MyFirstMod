# ⚔️ LEGIONS & FACTIONS: Autonomous War Sandbox ⚔️
### *The Ultimate Autonomous Army, Faction, Cavalry & War Simulation Sandbox for Minecraft 1.21.1 (Fabric)*

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

Ever wanted to command a **50-strong battalion of heavily armored Royal Villager Knights charging on armored horses**, supported by **Bogged Snipers firing venom arrows**, **Tempest Breeze wind-mages**, **Pixie Combat Medics**, **Colossal 1.5x Iron Titans**, **Pyrotechnic Artillery launching firework volleys**, and **Necromancers summoning undead hordes**?

**Legions & Factions: Autonomous War Sandbox** transforms Minecraft into an **autonomous, server-authoritative tactical war simulator**. 
Create any living Minecraft entity into a custom battle template, equip it, customize its scale, mount, aura and death explosions, assign it to a faction with unique perks, place outposts, whistle army formations, transmute wild mobs, and watch legendary battles unfold!

![Dashboard Overview](docs/unit-sandbox-dashboard.png)

---

## 🎮 The Commander's Arsenal (8 Interactive Tactical Tools)

| Item | Name | Primary Action | Shift Action | Tactical Role |
| :--- | :--- | :--- | :--- | :--- |
| 🪄 | **Legion Creator Tool** | **Right-Click**: Open Creator GUI<br>**Left-Click**: Spawn equipped unit<br>**Right-Click Unit**: View dossier | **Shift + Right-Click**: Quick-cycle units<br>**Shift + Left-Click**: Spawn 5-unit squad | Full unit creation, inspection & deployment |
| 📯 | **Tactical Formation Whistle** | **Right-Click**: Instantly order nearby troops into formation | **Shift + Right-Click**: Cycle formation mode (*Shield Wall, Wedge, Circle, Spread*) | Tactical positioning & battlelines |
| 🌟 | **Unit Transmutation Wand** | **Right-Click Living Mob**: Transmute/Recruit into active unit template | *Plays totem flash & syncs gear/faction* | Rapid recruitment of wild mobs |
| 💖 | **Grand Healer's Divine Staff** | **Right-Click**: Cast massive 16-block restoration wave (8 HP + Regen II + Cleanse) | *Cooldown: 5s* | Direct army healing & support |
| 🪄 | **Faction Commander's Scepter** | **Right-Click Block**: Set Move/Rally waypoint<br>**Right-Click Enemy**: Mark focus target | **Action bar**: View active troops in radius | Real-time battlefield tactical command |
| 🎺 | **Commander's Tactical War Horn** | **Right-Click**: Sound rally horn (Area Strength & Speed) | **Shift + Right-Click**: Cycle orders (*Rally / Charge / Hold*) | Morale boosts & squad rally |
| ⚡ | **Reinforcement Drop Beacon** | **Right-Click Block**: Call down a 4-unit elite drop wave | *Cooldown: 10s* | Emergency frontline reinforcements |
| 🚩 | **Faction Outpost Standard** | **Right-Click Block**: Establish faction outpost territory | *Defensive Zone: 32 blocks* | "Home Turf" territorial defense |

---

## 🛡️ Tactical Roles & Combat Behaviors

```text
                     [ CAVALRY & VANGUARD ]
               🐎 Armored Knights on Warhorses
               🕷 Spider Riders & Wolf Cavalry
                         │
                     [ FRONT LINE ]
               ⚔ Melee Tanks & Knights
               🛡 Shield Wall Formations
               ⚡ Colossal Iron Titans
                         │
                     [ MID LINE ]
               🏹 Ranged Marksmen & Bogged Snipers
               💣 Siege Bombardiers & Mortars
               🎆 Pyrotechnic Fireworks Artillery
               🌪 Tempest Breeze Wind Casters
                         │
                     [ BACK LINE ]
               💖 Field Medics & Allay Pixie Healers
               🎶 Battlefield Bards (War Songs)
               ☠ Dark Necromancers & Summoners
               👑 Commanders & Tacticians
```

### 1. 🐎 Mounted Cavalry System (`MountedCavalrySpawner`)
- Units can ride **Armored Warhorses**, **Skeletal Steeds**, **Cave Spiders**, **Dire Wolves**, and **Ravagers**.
- Mounted units gain increased movement speed, charge impact knockback, and synchronized rider AI!

### 2. 📏 Entity Scaling (0.25x Miniature to 5.0x Colossus)
- Scale any warrior up or down via the attributes engine (`EntityAttributes.SCALE`).
- Create colossal **Iron Titan Juggernauts** or miniature **Goblin Swarms** with matching hitboxes and step heights!

### 3. 🧠 Dynamic Morale & Panic System (`MoraleSystem`)
- **Commander Casualties**: When a squad leader dies, recruit-tier units suffer **Morale Break** (retreating in panic with smoke particles).
- **Vengeance Rage**: Veteran and Captain units enter **Vengeance Fury** (+40% speed and damage) to avenge fallen officers!

### 4. ✨ Particle Auras & 💥 Death Actions
- **Custom Visual Auras**: `Flame`, `Soul Flame`, `Enchanted Hit`, `Portal`, `Heart`, `Totem of Undying`, `Electric Spark`.
- **Lethal Death Actions**: `Explosion`, `Healing Mist`, `Victory Fireworks`, `Lightning Strike`, `Poison Cloud`.

### 5. 🎆 Pyrotechnic Artillery (`FireworksArtilleryAI`)
- Pyros fire fireworks rockets and explosive crossbow shells into enemy ranks with colorful particle explosions.

### 6. 💖 Field Medics & Flying Pixies (`HealerMedicAI` / `SpecialEntityAI`)
- Casts divine restoration beams, cleanses negative debuffs (Poison/Wither), and throws splash healing flasks.
- Allay Combat Medics fly between wounded troops delivering healing fairy dust!

### 7. 🧪 Potion Alchemists (`ThrowablePotionAI`)
- Ballistic splash flasks of Harming, Poison, Slowness, and Weakness over enemy frontline units.

### 8. ☠ Dark Necromancers (`NecromancerAI`)
- Channels dark summoning circles in combat to summon armed skeleton and zombie thralls.

### 9. 🩸 Bloodrage Berserkers (`BerserkerAI`)
- Roars fiercely when injured and enters Bloodrage (gaining Strength II, Speed II, and Haste II with fire auras).

### 10. 🎶 Battlefield Bards (`BardAI`)
- Plays melodic horn songs that grant Speed, Regeneration, and Resistance to all nearby allies.

### 11. 💣 Siege Bombardiers (`BombardierAI`)
- Heavy sappers launching explosive TNT mortar charges at enemy lines.

### 12. 🌿 Nature Druids (`DruidAI`)
- Entangles enemies in thorny roots (Slowness IV) and summons Nature Spirit Wolves.

### 13. ✨ Holy Paladins (`PaladinAI`)
- Golden crusaders dealing +60% radiant damage to undead and releasing healing waves on hit.

### 14. 🗡 Flanking Assassins (`AssassinAI`)
- Sprints behind enemy lines to backstab commanders and medics with +175% critical damage.

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
 │ 🔴 Iron Raiders       │ Red        │ Berserkers, Bombardiers, Alchemists  │
 │ 🟢 Village Alliance   │ Emerald    │ Town Militia, Druids, Iron Titans    │
 │ 🟣 Undead Legion      │ Purple     │ Necromancers, Dread Cavalry, Bogged  │
 │ 🔷 Arcane Order       │ Cyan       │ Bards, Tempest Breeze, Pixie Medics  │
 └───────────────────────┴────────────┴──────────────────────────────────────┘
```

---

## 🖥️ 5-Tab Interactive Creator Dashboard

```text
  ┌────────────────────────────────────────────────────────────────────────┐
  │ [⚔ EDITOR]  [📋 UNITS]  [🛡 FACTIONS]  [💥 BATTLE]  [⚙ SETTINGS]       │
  ├────────────────────────────────────────────────────────────────────────┤
  │                                                                        │
  │  [⮜ Next Unit]     [➕ Duplicate]         [💾 Save Slots]             │
  │  [Entity: Cycle]   [Role: Cycle]          [⚔ Spawn Unit (1)]          │
  │  [Faction: Cycle]  [Rank: Cycle]          [🛡 Spawn Squad (5)]         │
  │  [Mount: Cycle]    [Aura: Cycle]          [🗑 Delete Unit]             │
  │  [Death: Cycle]    [Scale: Cycle]                                      │
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
# Unit Management & Customization
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
/unit set <id> scale <0.25 - 5.0>             # Set entity visual scale
/unit set <id> mount <entity_id>              # Set cavalry mount (e.g. minecraft:horse)
/unit set <id> aura <flame|portal|heart|...>  # Set visual particle aura
/unit set <id> death <explosion|healing|...>  # Set death effect action
/unit set <id> role <role>                    # Set combat role (melee, ranged, etc.)
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
