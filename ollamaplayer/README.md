# Ollama Player (Nova)

A Fabric 1.21.1 mod that adds **Nova**, a server-side companion driven by a local [Ollama](https://ollama.com) model.
Nova follows you, talks in chat, mines blocks, fights hostile mobs (and whatever hits you, but never another player), eats and cooks when hungry, picks up drops, crafts and smelts from a recipe planner, gears up, builds a shelter, places torches in caves, backs away from creepers, swims out of water, bridges one-block gaps and pillars straight up with cobblestone or dirt from its bag, shoots skeletons, pillagers, witches and blazes with a carried bow, and respawns after death.
Nova's bag, armor, position, mode and owner are saved in the world folder (`ollamaplayer-companion.nbt`) every 30 seconds and when the server stops, so she comes back with her stuff after a restart. `/nova despawn` and death clear the save.
If it dies, it walks back to where it fell to collect its drops (junk is dumped first to make room), then returns to you empty-handed.

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
| `/nova status` | show mode, health, inventory, and the current plan |
| `/nova follow` / `/nova stay` | follow you / hold position |
| `/nova mine <what> [count]` | mine blocks matching a keyword, e.g. `/nova mine log 8` |
| `/nova craft <what> [count]` | craft or smelt anything: plans the missing steps, gathers materials, uses or places a table/furnace, e.g. `/nova craft iron_pickaxe` |
| `/nova gear` | wear the best armor and wield the best sword/axe/pickaxe/shield it carries |
| `/nova shelter` | wall itself into a 3x3 hut from blocks in the bag and hold still |
| `/nova give` | hand everything in its bag to you |
| `/nova eat` | eat the best food in the inventory (cooked food first) |
| `/nova help` | list these commands in chat |
| `/nova say <text>` | talk to Nova without going through chat |

You can also just chat with Nova: messages to Nova are sent to the model, which replies with a short line and an action. The actions are `none, follow, stay, mine, craft, gear, shelter, give, eat, status`.
For example: "Nova, get me 8 cobblestone", "make a crafting table", "gear up", "build a shelter".

## Ollama

Config lives in `config/ollamaplayer.json` (created on first start):

- `ollamaHost` - default `http://127.0.0.1:11434`
- `model` - `auto` picks the largest installed model whose size is at most `ollamaMaxModelBytes` (default 14 GB, so a 16 GB GPU fits). It is chosen from `/api/tags`, so whatever you've pulled (e.g. `gemma4:12b`) is used. Set a model name to force one.
- `useLlm` - `false` disables Ollama and uses only the keyword heuristics
- `numCtx`, `timeoutSeconds` - Ollama context size and request timeout
- `companionName` - default `Nova`
- `chatterMinSeconds` / `chatterMaxSeconds` - idle remarks, default 120 to 300 s
- `persona` - the system prompt for the model. An existing config keeps its saved persona; delete the `persona` line to get the current default.

Ollama calls run on a background thread and use JSON-schema structured output, so replies always parse into `{say, action, target, count}`.
If Ollama is unreachable or slow, Nova falls back to keyword heuristics (`brain/Heuristics.java`), so the mod keeps working offline.

## Layout

- `companion/NovaBody.java` - the body. Fabric's `FakePlayer` disables ticking and is invulnerable; this subclass adds gravity, collision movement, hunger and damage.
- `companion/Companion.java` - behaviour: follow, fight, mine, eat, pick up drops, describe status.
- `companion/Pathfinder.java`, `Navigator.java` - A* over walkable blocks (step-up, drops up to 3 blocks), path following with jumps and stuck detection.
- `companion/CompanionManager.java` - one companion, respawn after death, cross-dimension relocation.
- `brain/` - Ollama chat, reply parsing, heuristics fallback.
- `ollama/OllamaClient.java` - model discovery (`/api/tags`) and chat (`/api/chat`).
- `craft/Recipes.java`, `craft/Planner.java` - the recipe table and a backward planner. Asking for an item expands it into steps: gather raw materials, smelt, craft at a table, place stations.
- `test/NovaGameTest.java` - Fabric GameTests that run headless: mining a log, crafting at a table, smelting with a placed furnace, wearing armor, mining iron, the full ore-to-iron-pickaxe loop, building a shelter, swimming out of water, and cooking beef instead of eating it raw. Run with `gradle runGameTest`.

## Known limits

- Pathfinding is local (range 36 blocks). It swims through water (slowly), bridges single air gaps at floor level and pillars straight up one block at a time when it carries cobblestone or dirt. It does not climb ladders or open doors, and it can still get stuck at the edge of a ledge it has pillared onto.
- Nova is one companion per server. Her chat memory is not saved; only her body and bag are.
- Shelters use cobblestone-like blocks from the bag; they are not sealed against every mob spawn rule.
- Gametests run on a flat stone floor; real terrain (caves, lava lakes, nights) is only covered by playing with `gradle runClient`.
- The bow is not covered by a gametest: the test world is peaceful, which removes hostile mobs, and switching difficulty disturbs the other parallel tests. Check it in `gradle runClient`.
- Creeper handling is simple: Nova backs away from a creeper within about 2.5 blocks, which helps more than trading hits but does not outrun it.
- Lost drops are a known issue in dense multi-companion test setups; the full loop test passes with five ores.
