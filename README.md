# ✨ MyFirstMod

> A tiny, clean Fabric mod for Minecraft 1.21.1 that gives the main menu a simpler look.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-1.21.1-DBD0B4?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square)
![Build](https://github.com/Qynl/MyFirstMod/actions/workflows/build.yml/badge.svg)

## 🎯 What does it do?

MyFirstMod currently has **one job**:

**Make the Minecraft title screen cleaner.**

It removes the rotating yellow splash text that appears above the Minecraft logo, leaving the menu with a more minimal look.

That's it. No extra gameplay mechanics, no background services, no unnecessary features.

## 🧩 Features

- 🧼 Removes the title-screen splash text
- 🎮 Client-side only
- 🪶 Lightweight and intentionally minimal
- 🔧 Built for Minecraft **1.21.1**
- 🧱 Uses Fabric
- ☕ Requires Java **21+**

## 🛠️ Project structure

```text
MyFirstMod/
├── .github/
│   └── workflows/
│       └── build.yml          # Automatic GitHub build
├── src/
│   ├── main/
│   │   └── resources/
│   │       └── fabric.mod.json
│   └── client/
│       ├── java/
│       │   └── dev/qynl/myfirstmod/
│       │       ├── MyFirstModClient.java
│       │       └── mixin/
│       │           └── TitleScreenMixin.java
│       └── resources/
│           └── myfirstmod.mixins.json
├── build.gradle
├── gradle.properties
└── settings.gradle
```

## ⚙️ Building

The project is configured to build through **GitHub Actions**, so you don't need to install Gradle on your computer.

Every push to `main` starts the build workflow.

You can also start a build manually:

**GitHub → Actions → Build Mod → Run workflow**

When the build succeeds, open the workflow run and download the **MyFirstMod** artifact.

## 🎮 Installing

1. Install **Minecraft 1.21.1**.
2. Install **Fabric Loader** for 1.21.1.
3. Install the required **Fabric API** version.
4. Download the latest `.jar` from the GitHub Actions artifact.
5. Put the mod JAR into your Minecraft `mods` folder.
6. Launch Minecraft with the Fabric profile.

> MyFirstMod is a **client-side** mod, so it is intended for the Minecraft client rather than a dedicated server.

## 🔒 Keeping it simple

This project deliberately has a very small codebase.

The mod itself does not need:

- ❌ An external server
- ❌ A database
- ❌ An account or login
- ❌ A background service
- ❌ A custom launcher
- ❌ A separate runtime

Its actual client code only changes the title screen's splash renderer.

## 📦 Dependencies

The project uses the normal Fabric development stack:

- **Minecraft 1.21.1**
- **Fabric Loader 0.19.5**
- **Fabric API 0.116.17+1.21.1**
- **Fabric Loom 1.17-SNAPSHOT**
- **Java 21**

Dependency versions are kept in `gradle.properties`.

## 🧑‍💻 Development

This is intentionally a small starting point for learning Fabric mod development.

Good places to start experimenting:

- `MyFirstModClient.java` for client initialization
- `TitleScreenMixin.java` for title-screen changes
- `myfirstmod.mixins.json` for mixin configuration
- `fabric.mod.json` for mod metadata

## 📄 License

MyFirstMod is released under the **MIT License**.

---

Made by **Qynl** 🧱

*Small mod. Clean menu. Good starting point.*
