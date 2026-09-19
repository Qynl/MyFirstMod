# Wilds & Relics equipment guide — 1.3.0

## The resource loop

1. Bring **iron-tier mining tools or better** from your Ancient City expedition.
2. Explore newly generated realm chunks. Mine Nullstone underground for Resonite, Prism, and Cinder ores, generally below Y=65. Fracture caves expose underground areas; you can also mine down normally.
3. Smelt **Raw Resonite** into ingots. Blasting is supported. Fortune improves ore yields; Silk Touch preserves the ore blocks.
4. Harvest **Hush Leaves** for Dusk Fiber and occasional Hushberries. Silk Touch collects the decorative foliage. These are harvestable worldgen growths, not a sapling/regrowth system.
5. Craft equipment, repair supplies, and food. Discover shrines, attune a waystone, then travel deeper into the realm.

Nullstone works in vanilla stone-tool and furnace recipes. Hushwood makes four planks and can fuel furnaces; Hush Planks are included in the vanilla plank tags. Prismstone can be smelted into glass for lamps and bottles.

Ores are generated in bounded clusters in Nullstone, never by rewriting player-built chunks after generation. Resonite accounts for roughly half the vein selections; Cinder Steps favor Cinder among the remaining selections, while other biomes favor Prism. These are approximate generation weights, not guaranteed ore counts per chunk.

## All 21 new items

### Materials and provisions

| Item | Use |
| --- | --- |
| Raw Resonite | Smelt or blast into Resonite Ingots |
| Resonite Ingot | Craft/repair tools and armor; compact nine into a block |
| Prism Dust | Fuel the Prism Staff, craft lamps and surveying equipment |
| Cinder Pearl | Craft the maul and Ember Tonics |
| Dusk Fiber | Craft bindings, a Veil Charm, repair kits, and a Wayfarer Thread |
| Hushberry | Restores 3 hunger; obtained from Hush Leaves |
| Expedition Stew | Restores 10 hunger with strong saturation; returns its bowl |
| Ember Tonic | Drink for 60 seconds of fire resistance; returns a bottle |

Stew can be crafted with three Hushberries and a bowl, or two berries, a brown mushroom, and a bowl. Tonics use a Cinder Pearl, Hushberry, and glass bottle. Hunger points are half-icons: 10 hunger restores five food icons.

### Resonite tools and armor

| Equipment | Properties |
| --- | --- |
| Pickaxe | Diamond-tier mining; 8.5 mining-speed multiplier; 1,800 durability |
| Axe | 1,800 durability; designed for Hushwood and ordinary wood |
| Shovel | 1,800 durability; ordinary shovel interactions |
| Helmet | 3 armor; 418 durability |
| Chestplate | 8 armor; 608 durability |
| Leggings | 6 armor; 570 durability |
| Boots | 3 armor; 494 durability |

Tools use ordinary ingot/stick crafting patterns. Armor uses ordinary armor patterns, totaling **24 Resonite Ingots**. All are repaired with ingots. Armor has a custom material and original worn textures, diamond-equivalent protection, and 2 toughness per piece; it is not Netherite-tier knockback armor.

Wearing **all four pieces in the Null Realm** refreshes night vision and haste I. The effects naturally expire shortly after removing a piece or leaving the realm (up to 15 seconds for night vision, 3 seconds for haste). They do not replace stronger potion effects. No set bonus is granted to spectators.

### Active equipment

| Item | Controls and purpose |
| --- | --- |
| **Prism Staff** | Use to fire a 24-block precision beam. Deals 8 damage to the first intersected hostile enemy, marks successful hits with glowing, and stops at solid blocks. Uses one Prism Dust and one durability per shot. 1.5-second cooldown; 768 durability. |
| **Cinder Maul** | Slow 11-damage / 0.9-speed melee weapon. Use while grounded for a hostile-only, line-of-sight 4-block slam dealing 7 ability damage with knockback. Six-second cooldown; three durability per slam. |
| **Survey Lens** | Use to report the coordinates of up to three nearest realm ores inside a six-block cube radius. Five-second cooldown. Never generates new chunks. |
| **Veil Charm** | Use for eight seconds of invisibility and speed I. Forty-five-second cooldown; 128 uses. Armor stays visible, and this is not invulnerability or a guaranteed aggro reset. |
| **Repair Kit** | Hold damaged mod equipment in your other hand and use the kit. Restores up to 400 durability and consumes one kit. Does not repair other mods' or vanilla equipment. |
| **Wayfarer Thread** | Hold use for three seconds to recall to your bound waystone. Sneak while finishing the channel to return to the sanctuary instead. Sixty-second cooldown after a successful recall. |

Staff and maul area/beam abilities intentionally exclude players, passive mobs, and scoreboard allies. The maul's **ordinary melee attacks** remain normal sword attacks and obey the server's normal PvP rules. Creative players do not need staff ammunition.

### Crafting reference

All recipes are visible in the recipe book once unlocked by entering the realm or finding materials. Armor recipes unlock from a Resonite Ingot.

- **Staff:** three Prism Dust, one Resonite Ingot, one stick.
- **Maul:** four Resonite Ingots, two Cinder Pearls, one stick.
- **Lens:** two Resonite Ingots, one Prism Dust, two sticks.
- **Veil Charm:** four Dusk Fiber surrounding an ender pearl.
- **Repair Kit:** two Resonite Ingots, one Dusk Fiber, one Resonant Shard.
- **Wayfarer Thread:** three Dusk Fiber, one ender pearl, one Resonant Shard.
- **Four Prism Lamps:** four Prism Dust surrounding glass.

Note the distinct materials: **Resonite** is the mined metal, while **Resonant Shards** remain the older court/boss progression currency. Neither replaces the other.

## Waystone shrines

Small fractured shrines generate outside the central sanctuary/arena reservation. Each has a permanent, non-mineable Waystone and a supply cache. Use the stone to bind it to **your** expedition record. Another player's attunement does not overwrite yours. Binding a new stone replaces your previous destination.

Recall works only inside the Null Realm. Taking damage interrupts the channel. It is blocked for enrolled trial participants and within 48 blocks of an active Warden encounter. A blocked/missing landing fails without spending the cooldown; recall never clears blocks to force a landing. It checks solid support, collision, the world border, and fluids. The destination chunk is loaded intentionally for the teleport.

An unbound thread returns to the sanctuary. The journal shows the current bound location; the existing compass still has arena, sanctuary, and discovered-court modes, not a shrine-locator mode.

## Building palette

Fifteen collectible blocks: Nullstone, Polished Nullstone, Nullstone Bricks, Resonite Ore, Prism Ore, Cinder Ore, Resonite Block, Prism Lamp, Prismstone, Cinderstone, Hushed Moss, Lumen Moss, Hushwood, Hush Planks, and Hush Leaves. Waystones are an additional worldgen-only block.

The wood and foliage are simple decorative blocks, not a full new vanilla wood family: no dedicated door, boat, sapling, leaf-decay, or stripped-log variants are claimed. Plank tags allow compatible generic recipes where vanilla uses those tags.

## Development status

CI validates compilation, assets/recipes, saved records, and dedicated-server loading/worldgen/loot. It does not prove the combat balance, verify every collision edge case, or inspect armor/GUI rendering at all scales. Back up saves and use a fresh test world for generation testing.
