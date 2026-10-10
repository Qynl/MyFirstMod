# Ollama Player (Nova)

A Fabric 1.21.1 mod that adds **Nova**, a server-side companion driven by a local [Ollama](https://ollama.com) model.
Nova follows you, talks in chat, mines blocks, fights hostile mobs, eats when hungry, picks up drops, and respawns after death.

Requires Fabric Loader 0.16.10+ and Fabric API 0.116.17+1.21.1. No other mods.

## Quick start

```
cd ollamaplayer
gradle runClient          # dev client with the mod loaded
```

Or install `build/libs/ollamaplayer-*.jar` (from the CI artifact `ollamaplayer`) into a 1.21.1 Fabric `mods/` folder.

In a world, as operator:

| Command | Effect |
|---|---|
| `/nova spawn` | spawn Nova next to you |
| `/nova despawn` | remove Nova |
| `/nova status` | show mode, health, inventory |
| `/nova follow` / `/nova stay` | follow you / hold position |
| `/nova mine <what> [count]` | mine blocks matching a keyword, e.g. `/nova mine log 8` |
| `/nova eat` | eat the best food in the inventory |
| `/nova say <text>` | talk to Nova without going through chat |

You can also just chat with Nova: messages to Nova are sent to the model, which replies with a short line and an action.

## Ollama

Config lives in `config/ollamaplayer.json` (created on first start):

- `ollamaUrl` - default `http://localhost:11434`
- `model` - `auto` picks the largest installed model whose size is at most `maxModelGb` (default 14 GB). Set a name to force one.
- `maxModelGb` - default 14
- `nameTag` - default `Nova`
- `chatterMinSeconds` / `chatterMaxSeconds` - idle remarks, default 120 to 300 s

Ollama calls run on a background thread and use JSON-schema structured output, so replies always parse into `{say, action, target, count}`.
If Ollama is unreachable or slow, Nova falls back to keyword heuristics (`brain/Heuristics.java`), so the mod keeps working offline.

## Layout

- `companion/NovaBody.java` - the body. Fabric's `FakePlayer` disables ticking and is invulnerable; this subclass adds gravity, collision movement, hunger and damage.
- `companion/Companion.java` - behaviour: follow, fight, mine, eat, pick up drops, describe status.
- `companion/Pathfinder.java`, `Navigator.java` - A* over walkable blocks (step-up, drops up to 3 blocks), path following with jumps and stuck detection.
- `companion/CompanionManager.java` - one companion, respawn after death, cross-dimension relocation.
- `brain/` - Ollama chat, reply parsing, heuristics fallback.
- `ollama/OllamaClient.java` - model discovery (`/api/tags`) and chat (`/api/chat`).
- `test/NovaGameTest.java` - Fabric GameTest that spawns Nova, mines a log, and checks the drop reaches the inventory. Run with `gradle runGameTest`.

## Known limits

- Pathfinding is local (range 36 blocks) and does not swim, climb ladders, or bridge gaps.
- Crafting and building are not yet implemented.
- Nova is one companion per server.
