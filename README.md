# 🕳️ The Null Warden

> A handcrafted Minecraft 1.21.1 Fabric boss encounter hidden beneath an Ancient City.

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

Every push to `main` triggers the GitHub Actions build.

You can also run it manually from:

**GitHub → Actions → Build Mod → Run workflow**

The generated JAR is uploaded as the **MyFirstMod** artifact when the build succeeds.

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
│   │   └── portal/
│   └── resources/
└── client/
    ├── java/
    └── resources/
```

## 🧪 Current development status

This is an active development project rather than a finished release.

The current architecture focuses on making the encounter recoverable and multiplayer-aware before adding more content. The next major gameplay layer is deeper arena interaction and more deliberate attack counterplay.

## 📄 License

MIT

---

Made by **Qynl** 🕳️
