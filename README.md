# 🕳️ The Null Warden

> A handcrafted Minecraft 1.21.1 Fabric boss encounter hidden beneath an Ancient City — now with **Null Kinetics**, a full movement suite.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-1.21.1-DBD0B4?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square)
![Build](https://github.com/Qynl/MyFirstMod/actions/workflows/build.yml/badge.svg)

## ⚔️ The encounter

The Null Warden is a multi-phase boss encounter built around Ancient City technology and a custom **Null Realm**.

The intended loop is:

1. Find an Ancient City.
2. Find the reinforced-deepslate seal.
3. Ignite the completed seal with flint and steel.
4. Enter the Void Gate.
5. Fight the Null Warden through four phases.
6. Defeat the encounter.
7. Claim the **Nullblade** and **Heart of the Null**.
8. Use the return gate to go back to your exact entry location.
9. *Chain the realm's residual null energy with the **Null Kinetics** movement suite.*

## 🧿 Boss phases

The fight progressively introduces different attack patterns:

- **Phase 1:** Void Cleave
- **Phase 2:** Sculk Ring and Void Rain
- **Phase 3:** Null Dash and Gravity Well
- **Phase 4:** Reality Tear and Collapse

Later phases can also summon **Null Echoes**, which are tracked as part of the encounter so resets can clean them up correctly.

## 🏟️ The Null Realm

The Null Realm contains a deterministic arena with:

- reinforced-deepslate pylons
- crying-obsidian perimeter
- polished-blackstone combat floor
- sculk structures
- dedicated return portal
- encounter-controlled boss state

The arena is rebuilt from a known footprint, making failed encounters recoverable instead of leaving an uncontrolled collection of boss entities behind.

## 💨 Null Kinetics

Null Kinetics is the mod's movement layer: a parkour-style kit powered by a shared
**energy** pool, styled after the residual null energy of the realm. It works in
every dimension (yes, including the Null Realm) and is fully multiplayer-safe.

| Ability | Input | What it does |
| --- | --- | --- |
| **Void Dash** | `V` (keybind) | Blink along your look direction with an FOV punch, portal burst and electric trail. Aim up to dash skyward. |
| **Null Step** | `Space` while airborne | Up to two mid-air jumps (configurable) that refund one charge on wall jumps. |
| **Sculk Stride** | automatic | Sprint into a wall while airborne to run along it. Jump off with `Space` for a momentum-keeping **wall jump**. |
| **Null Drift** | hold `C` | Glide on null currents: capped fall speed, forward steering, phantom-wing audio. |
| **Phase Slide** | tap `Sneak` while sprinting | Anti-friction ground slide that keeps momentum; press `Space` mid-slide for a **slide hop**. |
| **Nullhook** | right-click the item | A craftable grapple: the hook flies up to 44 blocks, anchors, and reels you in. Right-click or sneak to release. |

### Flow

Chaining abilities (dash → wall jump → air jump → grapple …) without touching the
ground builds **Flow**, up to six stacks. Each stack grants +4% movement speed and
a 5% energy-cost reduction, decaying if you stop being stylish. Reach max Flow for
the *Flow State* advancement.

### Energy

Everything costs energy from a 100-point pool that regenerates on the ground.
The HUD (right of the hotbar) shows the energy bar, ability icons with cooldown
sweeps, active-window glows, and Flow pips. Press `J` to hide it.

### Networking & fairness

- Every ability is **validated and applied by the server**; the client mirrors
  the exact same math (`KineticMath` is shared between both sides) for zero-lag
  prediction with no rubber-banding.
- Continuous effects (wall run, glide, slide, grapple) run as server-revalidated
  "windows" — the server drains energy, forgives fall distance, and closes
  windows the moment they become invalid.
- Fall damage has a short grace window after any kinetic action.

### Configuration

Everything is tunable in `config/myfirstmod-kinetics.json` (auto-generated,
clamped, and hot-reloadable with `/kinetics reload`): ability toggles, speeds,
cooldowns, energy costs, wall-run duration, grapple range, Flow scaling,
fall-damage grace, and an optional `requireRelic` mode that gates all abilities
behind carrying the Heart of the Null.

### Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/kinetics info` | all | Show your energy, Flow, charges and active windows |
| `/kinetics reload` | op | Reload the config |
| `/kinetics toggle <ability>` | op | Enable/disable an ability globally (`all`, `dash`, `double_jump`, `wall_run`, `glide`, `slide`, `grapple`, `flow`) |
| `/kinetics energy <amount>` | op | Set your energy |

### Crafting the Nullhook

```text
┌───┐
│ C │  chain
├───┤
│ S │  string
├───┤
│ E │  echo shard
└───┘
```

### Kinetics advancements

Rooted at *Null Kinetics — Move like the Warden*:

- **Void Dash** — perform your first dash
- **Airwalker** — jump five times without touching the ground *(challenge)*
- **Wall Dancer** — three wall jumps in a single flight *(goal)*
- **Null Drift** — glide for ten seconds total
- **Hooked** — anchor the Nullhook at least twenty blocks away *(goal)*
- **Flow State** — reach maximum Flow *(challenge)*

## 👥 Multiplayer

The encounter tracks participating players by UUID.

The system handles:

- players joining an active fight
- players leaving the realm
- deaths and disconnects
- boss-bar membership
- per-player reward protection
- encounter reset when everyone has abandoned the fight

The return portal also stores the player's original dimension, position, yaw, and pitch.

Null Kinetics is per-player: energy, cooldowns, Flow and windows are tracked
server-side for each player independently, so movement never desyncs between clients.

## 🗡️ Nullblade

The **Nullblade** is the primary boss reward.

It is a custom Netherite-based weapon with a charged area ability, cooldown, knockback, and a visual Null-themed particle effect.

## 🛠️ Development

### Requirements

- Minecraft **1.21.1**
- Fabric Loader
- Fabric API
- Java **21**
- Gradle / GitHub Actions

### Build

Every push to `main` (and `arena/*` working branches) triggers the GitHub Actions build.

You can also run it manually from:

**GitHub → Actions → Build Mod → Run workflow**

The generated JAR is uploaded as the **MyFirstMod** artifact when the build succeeds.

CI also runs a **dedicated-server smoke test** on every build: the fresh JAR is
dropped into a real Fabric 1.21.1 server alongside Fabric API, the server must
boot to `Done` with zero mod-related datapack/mixin/registration errors, and
the `/kinetics` command tree plus the Nullhook entity are exercised live over
RCON (`tools/rcon.py`).

### Source layout

```text
src/
├── main/
│   ├── java/dev/qynl/myfirstmod/
│   │   ├── MyFirstMod.java
│   │   ├── boss/
│   │   │   └── NullWardenManager.java
│   │   ├── block/
│   │   ├── item/
│   │   ├── kinetics/            ← Null Kinetics (shared physics + server authority)
│   │   │   ├── KineticMath.java       (movement math shared by client + server)
│   │   │   ├── KineticsManager.java   (per-tick validation, energy, flow, sync)
│   │   │   ├── KineticsNetworking.java(custom payloads)
│   │   │   ├── KineticsConfig.java    (JSON config, hot reload)
│   │   │   ├── KineticsCommands.java  (/kinetics)
│   │   │   ├── NullhookEntity.java    (grapple hook entity)
│   │   │   └── ...
│   │   └── portal/
│   └── resources/               (models, lang, recipe, advancements, icons)
└── client/
    ├── java/dev/qynl/myfirstmod/
    │   └── kinetics/client/     ← prediction, keybinds, HUD, hook renderer
    └── resources/               (client mixins config)
```

The HUD icon textures are generated by `tools/gen_kinetics_icons.py`, a
dependency-free Python rasterizer (supersampled + box-filtered for clean AA).

## 🧪 Current development status

This is an active development project rather than a finished release.

The boss encounter is multiplayer-aware and recoverable; the current gameplay
layer is Null Kinetics (movement) plus the Nullhook. Next up: deeper arena
interaction and more deliberate attack counterplay in the Warden fight.

## 📄 License

MIT

---

Made by **Qynl** 🕳️
