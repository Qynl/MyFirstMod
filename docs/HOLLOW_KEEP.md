# 1.7 — The Hollow Keep

A separate **shared dungeon dimension**, with a hand-designed 63×63 fortress, three ward chambers, a ribbed central great hall, four towers, and a new original-model boss. This is not a procedural room randomizer or one private instance per party.

## Enter and prepare

After the Null Warden has fallen at least once in the realm, use the **Hollow Gate at (-6,81,158)** in the sanctuary. The migration adds it only if the block is empty. If an old build occupies the gate spot, its owner can clear that block and re-enter the realm to install the gate. No occupied block is overwritten.

On first use the reserved fortress is constructed one chunk per server tick in `myfirstmod:hollow_keep`. Wait, then use the gate again to enter. The fortress persists and is not rebuilt for each expedition. Do not remove its required interactable blocks or build a base in its combat rooms.

A waystone near the entrance permits resting before the run. **Ashen Flasks, persistent vows, and Resonite armor bonuses work here**, drawing on the same player state as the realm. The old atlas/compass/recall thread are realm navigation tools, not Keep navigation tools. The entrance gate permits returning to the sanctuary even during an expedition. Entry/exit searches for a supported, fluid-free, collision-free landing; an obstructed destination fails rather than overwriting blocks. Falling below Y=55 while alive rescues a player to the Keep entrance.

## Shared expedition

Gather living Survival/Adventure players within **16 blocks of the Expedition Heart at (0,65,22)**, then use it. Peaceful cannot start a run. Party scaling caps at four players, but enrollment itself is not capped. There is one run at a time; late entrants are blocked unless already enrolled or Creative. Players already inside but outside enrollment range are not eligible for rewards.

Activate and clear the three wards in any order, **one active room at a time**:

| Ward | Coordinates | Guardians |
| --- | --- | --- |
| West | (-20,65,0) | Sentinels |
| East | (20,65,0) | Shardstalkers |
| North | (0,65,-20) | Mixed |

Each chamber spawns 3–6 guardians according to party scaling. Guardians are tethered near their room. If spawn space is obstructed, the activation fails and spawned actors are discarded; clear the space and retry. Completion is based on actual zero health, not an unloaded/discarded actor. Rest and vow changes are blocked for enrolled players until the run ends, including if they temporarily return home.

All three wards trigger a **five-second emergence**, then the Regent appears at **(0.5,65,0.5)** in the great hall. The boss bar communicates ward progress and boss health.

## The Grave Regent

An original crowned funeral-monarch model, split robes, animated mantle and arms, and an oversized ritual staff. Health scales **360–720**, with eight armor. Its special attacks have roughly two-second warnings:

- **Royal Cleave:** a committed forward cone within five blocks. Circle behind or leave its reach.
- **Funeral Ring:** safe inside 2.5 blocks or outside 7.5 blocks.
- **Cross of Ash:** two fixed perpendicular lanes centered on a selected player's marked position. Move out of both lanes; the cross extends six blocks along each axis.

Attacks require line of sight and appropriate vertical proximity. Below half health, damage rises from 10 to 12 and recovery falls from 70 to 45 ticks. No block destruction or forced camera control.

## Rewards and replay

Each enrolled player present in the dimension for at least **ten seconds** receives a persisted reward claim after victory:

- One Regent Crest.
- One Astral Core.
- Eight Resonant Shards.
- 300 XP.
- **First claimed Keep clear:** a Requiem Glaive.

Return through the gate to the Null Realm to receive the queued rewards. Offline participants retain their claim; normal offer-or-drop inventory behavior applies. Summoning a standalone Regent does not award expedition loot.

The Keep becomes available again after **one minute**. An empty party times out after thirty seconds; the maximum run is fifteen minutes. Exiting is allowed; enrolled players can rejoin while the run remains active. Restarting cancels active runs and cleans up tagged actors; it does not refund or grant completion loot. Geometry, claimed clear counts, and queued rewards persist. The short replay cooldown is runtime-only and resets on restart.

## Requiem Glaive

A durable sword-type weapon with a charged reaping arc:

- Hold use for at least one second, then release.
- Six-block forward cone; 14 ability damage to visible hostile, non-allied targets.
- Successful hits apply Slowness II for three seconds and grant the wielder Absorption I for five seconds (two temporary hearts, not permanent stacking health).
- Seven-second cooldown; three durability per cast.
- Supports the existing Vigor/Gale/Focus forge attunements.
- Ordinary melee still follows normal PvP rules; the special arc excludes players and passive creatures.

A replacement is crafted diagonally with a **Regent Crest, Resonite Ingot, and stick**. It is not a projectile or a grappling/teleport weapon.

## Administration and validation

`/hollowkeep prepare` requires permission level 2 and prepares the fortress without a player. It reports `KEEP_BUILDING` or `KEEP_READY`; it does not start an expedition or grant rewards. This is also used by the dedicated-server smoke test to exercise the real incremental builder and inspect each required room block and tower.

Build tests cover attack boundaries, party health, save/mailbox compatibility, generated resources and assets, the new dimension, fortress construction, Regent spawning and its empty standalone loot table. They do not replace a connected-player full-clear, multiplayer/rejoin test, animation review, or balance pass. Back up worlds before updating.
