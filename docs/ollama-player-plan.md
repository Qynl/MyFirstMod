# Ollama Player — Design & Implementation Plan

> A Fabric 1.21.1 mod that puts a **local LLM (Ollama) inside a real Minecraft player**.
> Not a chatbot with a skin: a survivor that walks, mines, crafts, fights, eats, sleeps, remembers
> and takes orders — driven by a model that runs on your own machine.
>
> Working name: **Ollama Player** · mod id `ollamaplayer` · package `dev.qynl.ollamaplayer` · default companion name **Nova**

---

## 0. What we are building (one paragraph)

A server-side Fabric mod that spawns a **fake player** (Fabric API's `net.fabricmc.fabric.api.entity.FakePlayer`,
i.e. a genuine `ServerPlayerEntity` — real inventory, hunger, XP, damage, crafting, advancements, tab-list entry)
and gives it a brain. The brain is a local Ollama model. The body is deterministic Java code that we write:
our own A\* pathfinder, our own skill state machines, our own recipe planner, our own memory. The LLM never
touches the world directly — it emits **intents** (tool calls), a validator checks them against what the
companion can actually perceive, and the skill layer executes them. If Ollama is off, too slow, or missing,
the companion degrades to a competent scripted survivor instead of standing still. **No model name is
hard-coded**: at startup the mod asks Ollama what's installed and picks the best available model.

---

## 1. Goals, non-goals, success criteria

### Goals

| # | Goal |
|---|------|
| G1 | A visible, nameable, persistent player-like entity that shares your world and your rules (survival, hunger, damage, death). |
| G2 | **Locomotor competence**: reliable navigation over real terrain — hills, water, forests, caves, doors, ladders — no teleporting, no phasing through blocks. |
| G3 | **Manual competence**: mine the right block with the right tool, place blocks, use items, craft (recursively), smelt, eat, equip, fight, flee, farm, trade, sleep. |
| G4 | **Language**: natural chat with the player, streaming replies, understands orders given in plain English ("go get me 16 iron and smelt it"). |
| G5 | **Autonomy**: when nobody says anything, it still does something sensible — the day/night survival loop (light the base, eat, mine, shelter at night, fight or flee). |
| G6 | **Memory & identity**: a journal, remembered landmarks, a configurable personality, persistent across restarts. |
| G7 | **Model-agnostic**: works with whatever Ollama has installed, from 3B to 70B, tool-capable or not. |
| G8 | **Safe & cheap**: hard tick budget, no griefing, undo, rate limits, never blocks the server thread. |

### Non-goals (explicitly out of scope for v1)

- Cloud/OpenAI endpoints (v1 is local-only; the transport is one interface, so it's a later option).
- Voice (STT/TTS), vision/screenshot perception, schematic-blueprint mega-builds.
- Multiplayer PvP competence, speedrunning to the End, redstone engineering.
- Making it undetectable from a human player on a public server (it is a fake player — for singleplayer/trusted servers only).

### Success criteria (the "is it done?" test)

1. Fresh world, no orders given: survives 3 in-game days without dying, ends with a shelter, a bed, food, and stone tools.
2. `Nova, get me 32 cobblestone and 10 oak planks` → completes in one in-game day without help, deposits into a chest.
3. `Nova, follow me` → follows at ~4 blocks through forest and over a hill, never falls in a ravine, never gets stuck >5 s.
4. Ollama killed mid-session → companion keeps mining/fighting/eating, says something about it, resumes when Ollama returns.
5. Loaded world, 1 companion, mid-size base, 8B model: **server MSPT delta ≤ 1.0 ms**, client FPS unaffected by inference.
6. Works with `qwen2.5:3b` (no tool support fallback path) **and** `llama3.1:8b` (tool-calling path), no config changes.

---

## 2. Stack & hard constraints

| Item | Choice | Note |
|---|---|---|
| Minecraft | **1.21.1** | matches the existing toolchain |
| Loader | Fabric Loader 0.16.10 + Fabric API 0.116.17+1.21.1 | |
| Mappings | Yarn `1.21.1+build.3` | |
| Java | 21 | |
| Runtime deps | **Fabric API only** | no Baritone, no AltoClef, no PlayerEngine, no Mineflayer, no Cloth Config, no external mod |
| HTTP | `java.net.http.HttpClient` (JDK) | no OkHttp/Apache HttpClient shading |
| JSON | `com.google.gson` (bundled with Minecraft) | |
| LLM server | Ollama (`http://127.0.0.1:11434`, configurable) | required for the brain, optional for the body |
| Env | `main` (server) source set does all the work; `client` only holds optional UI | works on a dedicated server headless |

**Rule: if it can't be done with vanilla Minecraft + Fabric API + the JDK, we write it ourselves.**
That means we own the pathfinder, the skills, the recipe planner, the memory store, the vector search (if any),
the config layer and the settings UI (built on vanilla `Screen`, not on a config mod).

### Where the code lives

This is a self-contained mod. Recommended: its own Gradle project (`ollamaplayer/`) so it never tangles with the
existing Null Warden content; if it stays in this repo, it gets its own `fabric.mod.json`, its own package root,
and zero imports from `dev.qynl.myfirstmod.*`.

---

## 3. Architecture: three loops, one body

The central design decision: **the LLM is a policy, not a driver.** It runs slowly (hundreds of ms), the world
runs at 20 TPS (50 ms). So we split into three loops with a strict priority arbiter.

```
                          ┌──────────────────────── player chat / orders ───────────────────────┐
                          │                                                                     │
                          ▼                                                                     │
 ┌───────────────┐   ┌───────────────────────────────────────────────────────────────────┐      │
 │  PERCEPTION   │──▶│                       ARBITER (goal stack)                        │      │
 │  (bounded,    │   │   priorities: SURVIVE > PLAYER_ORDER > ACTIVE_GOAL > IDLE_AUTONOMY│      │
 │   tick-spread)│   └───────────────┬──────────────────────┬────────────────────────────┘      │
 └───────────────┘                   │                      │                                    │
                                     ▼                      ▼                                    │
        ┌────────────────────────────────────┐   ┌──────────────────────────────────┐           │
        │ LOOP A · REFLEXES (every tick)     │   │ LOOP C · COGNITION (async)        │◀──────────┘
        │  no LLM, deterministic, <0.2 ms    │   │  every N ticks / on events        │
        │  lava · fall · drown · suffocate   │   │  Ollama /api/chat (+tools)        │
        │  eat · flee · unstuck · attack     │   │  → intents (not world mutations)  │
        └────────────────┬───────────────────┘   └───────────────┬──────────────────┘
                         │  preempt                              │ validated intents
                         ▼                                       ▼
        ┌──────────────────────────────────────────────────────────────────────────┐
        │ LOOP B · SKILL EXECUTOR (every tick)                                     │
        │  current Skill = small state machine: GoTo · Mine · Craft · Fight · ...   │
        │  uses NAVIGATION + BODY actuators; reports SUCCESS/FAILURE + reason       │
        └────────────────────────────────┬─────────────────────────────────────────┘
                                         │ failure reason → memory → next cognition
                                         ▼
                              ┌───────────────────────┐
                              │  BODY · AiPlayer      │  extends FakePlayer
                              │  (real ServerPlayer)  │
                              └───────────────────────┘
```

**The contract that keeps this safe:**

- The model may only return **tool calls against data it was shown** (entity ids, block positions, item ids from the
  observation block). Anything else is rejected by the validator and fed back as an error message.
- Every skill has a **tick budget** and a **failure reason**. Failures are how the companion "learns" — the reason goes
  into memory and into the next prompt.
- The arbiter can **preempt** any skill at any tick (a creeper beats a crafting order).

---

## 4. The body: `AiPlayer extends FakePlayer`

### Why `FakePlayer` (and not a custom mob)

Fabric API ships `net.fabricmc.fabric.api.entity.FakePlayer`, a `ServerPlayerEntity` with no network connection.
It gives us, for free and *legitimately*:

- a real `PlayerInventory`, `HungerManager`, XP, `PlayerAbilities`, equipment slots, armor & damage model
- vanilla mining/place/use/attack pipelines through `ServerPlayerInteractionManager` ⚠️
- appearing in the tab list, taking real damage, dying with a death message, sleeping, riding, trading
- compatibility with other mods and datapacks — we are a player, not a mob emulating one

Cost: vanilla does **not** save fake players, and a fake player has no skin (no signed `textures` property).
Both are handled below.

### Class outline

```java
public final class AiPlayer extends FakePlayer {
    final AiBrain brain;      // cognition: prompt building, Ollama calls, intent parsing
    final AiSenses senses;    // bounded perception
    final SkillRunner skills; // current skill + goal stack
    final Navigator nav;      // pathfinding + path following
    final AiMemory memory;    // journal, landmarks, profile
    final ActionLog edits;    // block-edit journal for /opm undo

    @Override public void tick() {           // called from our own tick hook, not by PlayerManager
        if (!isAlive()) { handleDeath(); return; }
        senses.tick();                       // bounded, tick-spread scanning
        reflexes.tick();                     // may preempt
        skills.tick();                       // advance current skill
        brain.tickMaybe();                   // schedules async inference, never blocks
        super.tick();                        // vanilla player ticking: physics, hunger, regen, ...
    }
}
```

### Lifecycle

| Event | Behaviour |
|---|---|
| Server start / first player join | Load companions from `world/data/ollamaplayer/*.dat`, spawn at saved pos (or owner's pos). |
| Spawn | `world.spawnEntity(ai)`; set `GameMode.SURVIVAL` unless the owner is in creative; **force-load its chunk** (server chunk ticket / forced chunk) so it never freezes in an unloaded chunk — released when it despawns. |
| Per world tick | `ServerTickEvents.END_WORLD_TICK` → tick all companions in that world. ⚠️ |
| Auto-sleep | No player within 96 blocks **and** no pending goal → companion stops ticking (and the model is allowed to unload after its idle timeout). Wakes on player proximity, chat, or `/opm`. |
| Death | Normal player death → respawn at bed/spawn, keep memory, log the death + cause as a lesson. |
| Server stop / world save | Persist to NBT (see §9). |

### Actuators (the only ways the world changes)

All of these go through vanilla player code — no direct `world.setBlockState`, no direct damage application.

| Capability | Mechanism |
|---|---|
| Move | set `PlayerInput` (forward/back/left/right/jump/sneak/sprint) + yaw/pitch on the entity each tick ⚠️ (fallback: `LivingEntity#travel` with a computed input vector) |
| Look | smooth yaw/pitch toward target with angular speed cap (looks human, not snappy) |
| Mine | `interactionManager.processBlockBreakingAction(pos, START_DESTROY_BLOCK, …)` then `interactionManager.update()` each tick; `STOP_DESTROY_BLOCK` to finish; `ABORT_DESTROY_BLOCK` to cancel ⚠️ (vanilla tool speed, enchantments, hunger penalty all apply for free) |
| Place / use on block | `interactionManager.interactBlock(player, world, stack, hand, new BlockHitResult(...))` |
| Use item (eat, bow, shield, bucket) | `interactionManager.interactItem(...)` / `ItemUsageContext`; hold-to-use supported via `itemUseTimeLeft` |
| Attack | select target, `player.attack(target)`, `swingHand(Hand.MAIN_HAND)`, respect attack cooldown |
| Hotbar / equip | `inventory.selectedSlot`, `equipStack(EquipmentSlot, ItemStack)`, armor via inventory slots |
| Craft | build a `CraftingInventory` (2×2 = `playerScreenHandler`, 3×3 = temporary `CraftingScreenHandler`) → `world.getRecipeManager().getFirstMatch(...)` → take result |
| Smelt / containers | open the real `ScreenHandler`, `quickMove` inputs, poll output slot, `closeHandledScreen()` |
| Chat | talk via the player's `sendMessage`/`PlayerManager` broadcast ⚠️ (deliberately styled differently from human chat) |
| Sleep / ride / trade / fish / farm | vanilla player methods, driven by skills |

⚠️ = signature to be confirmed against yarn 1.21.1+build.3 in the **Phase 0 spike**; these are the intended hooks.

---

## 5. Navigation (written from scratch)

### Design

- **Snapshot, then search.** The world is only ever read on the server thread. Before an A\* search we copy the
  relevant volume into an immutable `NavRegion` (palette-id `short[]`/`long[]` + a "solid/standable/hazard" bitmask).
  The search itself runs on a worker thread over that snapshot. This is the single most important threading rule
  in the project.
- **Graph.** Node = a `BlockPos` where the entity can stand (air + non-solid above head + standable floor).
  Movement primitives, each with its own cost:
  `WALK`, `DIAGONAL`, `STEP_UP (+1)`, `STEP_DOWN (−1…−3, with fall-damage penalty)`, `JUMP_GAP(1–2)`,
  `SWIM`, `LADDER_UP/DOWN`, `DOOR/GATE/TRAPDOOR (open, walk, close)`, `CLIMB_VINE/SCAFFOLD`.
- **A\*** with a custom binary heap (no `PriorityQueue` boxing in hot loop), octile heuristic, tie-breaking,
  hard caps: 20 000 expansions, 250 ms wall clock, 256-block radius. Coarse-to-fine: if the goal is far, first find a
  "waypoint chain" through remembered landmarks; else fall back to straight-line steering with obstacle sidestep.
- **Hazard costs**, not bans: lava/fire/cactus/sweet berries = prohibitive cost; water = cheap if it can swim;
  open sky at night near hostile spawn = small penalty; darkness = penalty (encourages lighting).
- **Path executor**: follow waypoints with steering + jump timing + ladder/water handling; smooth look direction;
  sprint when the path is long and straight.

### Stuck recovery (this is where bots die)

Detectors: no position delta for N ticks, oscillating position, path node unreachable, block in face.
Responses, escalating: re-path with the blocked node penalised → sidestep perpendicular → break the blocking
block (if allowed) → jump → mark area as "difficult" in memory → give up with reason `PATH_UNREACHABLE` and tell
the brain. Every give-up is reported, never silent.

### Deliverable tests

- Synthetic grid tests (no Minecraft needed): flat, staircase, maze, diagonal, water, 3-deep pit, door, ladder, parkour gap.
- In-world GameTest: cross a 40×40 pre-built obstacle course; descend a ravine safely; refuse a path through lava.

---

## 6. Perception: the observation block

The prompt is the bottleneck. Perception produces a **compact, token-counted** observation:

```jsonc
{
  "self": {"hp":16,"food":7,"air":10,"pos":[128,64,-40],"dim":"overworld","xp":3,
           "held":"iron_pickaxe","armor":2,"inv_free":9,"state":"mining"},
  "time": {"t":13200,"day":3,"light":7,"weather":"clear","biome":"forest"},
  "nearby_blocks": [           // nearest N, spiral scan, budget-limited
    {"id":"oak_log","pos":[131,65,-38],"d":3.2},
    {"id":"stone","pos":[126,63,-41],"d":4.1}
  ],
  "entities": [
    {"eid":17,"type":"zombie","d":12.4,"hostile":true},
    {"eid":22,"type":"item:iron_ore","d":2.1}
  ],
  "players": [{"name":"Qynl","d":6.0,"hp":20}],
  "landmarks": [{"name":"home","pos":[128,64,-40]},{"name":"mine_shaft","pos":[140,32,-55]}],
  "memory": ["I have a chest with 12 iron ore at home", "Day 1: died to a creeper near the ravine"],
  "last_result": {"skill":"GatherItem(oak_log x8)","status":"failed","reason":"NO_MATCHING_BLOCK_WITHIN_64"}
}
```

Budgets: `nearby_blocks ≤ 24`, `entities ≤ 12`, `memory ≤ 8 lines`, whole observation ≤ ~700 tokens.
Scanning is **spread across ticks** (a spiral cursor that advances a fixed number of block reads per tick) and
cached with a dirty flag — a 32-block-radius scan must never happen in one tick.

---

## 7. The brain: Ollama integration

### 7.1 Model resolution — no hard-coded model

```
1. config.model == "auto" (default)
2. GET /api/tags                       → list of installed models w/ details.family, details.parameter_size, size, modified_at, digest
3. GET /api/show  (per candidate)      → capabilities (does it support "tools"? template? context length?)
4. score each candidate:
      +tool support                (big win)
      +fits VRAM budget            (from /api/ps + reported size; skip models that would evict MC's GPU memory)
      +instruction-tuned name tags (instruct / it / chat)
      +parameter size tier         (prefer the largest that fits; never pick > config.maxParams)
      +recently used / recently modified
5. pick the best; remember the digest in the config cache
6. if no tool-capable model → JSON_MODE path (see 7.4)
7. if nothing at all / unreachable → OFFLINE mode (scripted survivor; periodic rediscovery every 5 min)
```

Manual override is a single config value or `/opm model <name>`, and `/opm brain` prints the resolved model,
its capabilities, and a benchmark.

### 7.2 Capability probe & warm-up (startup, once)

- `POST /api/chat` with `keep_alive: -1`, `num_predict: 1` → **measures cold load + first-token latency**.
- A 3-request micro-benchmark → **tokens/second** → auto-derives `cognitionIntervalTicks`
  (fast model → decide every 20 ticks; slow model → every 60–100 ticks) and `maxOutputTokens`.
- Result cached to `config/ollamaplayer/model-cache.json` so restarts are instant, re-validated by digest.

### 7.3 Two request types

| | `decide()` | `chat()` |
|---|---|---|
| Endpoint | `POST /api/chat` | `POST /api/chat`, `stream: true` |
| Temperature | 0.2–0.4 | 0.8 |
| Output | tool calls (function calling) | prose, streamed to chat line-by-line |
| `num_predict` | ≤ 300 | ≤ 120 |
| Timeout | 8 s (configurable), cancelled if stale | 20 s, chunked |
| Concurrency | one per companion, queued globally | preempts `decide()` |

- **Single-flight per companion**, plus a global request queue so 5 companions don't fight over one GPU.
- **Staleness**: each request carries a world-version stamp; if the world changed materially (new order, damage
  taken, goal finished), the result is discarded rather than applied late.
- **Circuit breaker**: 3 consecutive failures → exponential backoff → OFFLINE mode → retry every 5 minutes.
- **Warm model**: `keep_alive: -1` while any companion is awake; release (`keep_alive: 0`) on auto-sleep so Ollama
  frees VRAM for Minecraft.

### 7.4 Output handling — assume the model misbehaves

Two supported output modes, chosen by capability:

1. **TOOLS mode** — `tools:` with our JSON schemas (`/api/chat` tool calling; verified available on Qwen3/Llama 3.1/Devstral-class models).
2. **JSON mode** — system prompt demands exactly one JSON object; request `format: "json"`; same schema.

Then, in both cases, a **recovery ladder**: strip markdown fences → extract the first balanced JSON object →
repair (trailing commas, single quotes, truncated tail) → validate against a hand-written schema validator
(unknown tool, missing required arg, wrong type) → **clamp** (`count ≤ 64`, `radius ≤ configured max`) →
**cross-check against perception** (an `eid` or `pos` not present in the last observation is rejected) →
on total failure, retry once with a "your last reply was invalid, here is the error" message → then fall back
to IDLE + a short apology in chat.

### 7.5 Prompt anatomy

```
[system]      persona (config/ollamaplayer/persona.txt, user-editable)
              hard rules: never claim actions you didn't take · never invent coordinates or ids ·
              prefer one small step · ask if the request is ambiguous · you are a player, not an assistant
[context]     rolling summary of the journal + active goals + last 6 events
[skills]      machine-generated catalogue of skills with JSON arg schemas (single source of truth in code)
[observation] the JSON block from §6
[examples]    2–3 few-shot exemplars (order → tool call)
```

Token budget is enforced programmatically: if the assembled prompt exceeds `num_ctx * 0.75`, we drop observation
detail (blocks → entities → memory) and then trigger a **summarization call** to compress the history.
Everything is regenerated from code, so the prompt never drifts from the actual skill list.

---

## 8. Skills: deterministic competence

A skill is a tiny state machine. No LLM inside.

```java
public interface Skill {
    String id();                      // "gather_item" — must match the tool name exposed to the model
    boolean canStart(AiPlayer p, SkillArgs a);   // precondition check → reason if false
    void start(AiPlayer p, SkillArgs a);
    void tick(AiPlayer p);            // called every tick, must be cheap
    boolean isFinished(AiPlayer p);
    Result result(AiPlayer p);        // SUCCESS / FAILED with a machine-readable reason
    void abort(AiPlayer p);           // leave the world tidy: stop mining, close screens, drop nothing
    int tickBudget(AiPlayer p);       // watchdog, default 20 min
}
```

### v1 skill set (~30)

| Category | Skills |
|---|---|
| Locomotion | `idle`, `look_at`, `go_to`, `follow`, `flee`, `retreat_home`, `explore`, `climb`, `swim_to_land` |
| Gathering | `mine_block`, `gather_item` (scan→path→mine→collect loop), `chop_tree`, `dig_stairs`, `farm_harvest`, `fish`, `pick_up_items` |
| Crafting | `craft` (recursive planner), `smelt`, `cook`, `brew?` (later) |
| Building | `place_block`, `build_shelter`, `build_wall`, `place_torch`, `dig_shelter`, `fill_hole`, `build_bridge` |
| Combat | `fight`, `defend_player`, `shield_up`, `shoot_bow`, `avoid_entity` |
| Survival | `eat`, `sleep`, `wake`, `equip_best`, `heal`, `extinguish`, `breach_air` |
| Social | `say`, `emote`, `give_item`, `drop_item`, `follow_order`, `wait` |
| Logistics | `store_to_chest`, `take_from_chest`, `deposit_all`, `organize_inventory`, `use_station` |

### Recursive crafting planner (the part that makes it feel smart)

```
craft(iron_pickaxe)
  └─ needs 3 iron_ingot, 2 stick
       ├─ iron_ingot ← smelt(iron_ore) ← gather(iron_ore)   [needs stone pickaxe, needs torch, needs to find a cave]
       └─ stick      ← craft(planks) ← gather(oak_log)      [needs axe? no — punching is slow but valid]
```

Implemented as a dependency graph over `RecipeManager` (mod recipes included for free), with: cycle guard,
depth limit, "prefer cheapest total cost (tool tier + distance)", and automatic insertion of prerequisites
(fuel for smelting, a crafting table near a station, torches before dark). Each node becomes a goal pushed onto
the goal stack; the arbiter pops them in order.

---

## 9. Memory

| Tier | Content | Storage |
|---|---|---|
| **Working** | ring buffer of last 64 events, active goal stack, last failed attempt | in-memory, NBT on save |
| **Episodic** | journal, one entry per in-game day + notable events ("Day 3: found a ravine at 140,-55; lost a pickaxe to lava") | NBT, summarized by the LLM when it exceeds a line budget |
| **Semantic** | landmarks (`home`, `mine`, `farm`, `portal`, `chest:iron`, `bed`), player preferences, learned "lessons" | NBT, capped at ~300 entries with LRU eviction |
| **Procedural** | per-skill statistics: success rate, average duration, typical failure reason | NBT, updated by the executor |

Persistence: our own `NbtIo.writeCompressed` file per companion at
`<world>/data/ollamaplayer/<uuid>.dat`, autosaved every 5 minutes and on world save / server stop.
Vanilla will not save fake players, so we own inventory, equipment, hunger, XP, position, and mind.

**Optional semantic search (off by default):** if an embedding model is present (`/api/embed`), keep vectors for
journal/landmark entries in the same NBT file; brute-force cosine similarity over ≤ 2000 × ≤ 1024 floats is
~2 ms — no external vector library needed.

---

## 10. Living with humans (UX)

### Triggers

Companion replies when: its name is mentioned (`Nova, …`), the chat starts with `@nova`, a player is within
4 blocks and asks a question, or the order arrives through `/nova <text>`.

### Commands (`/ollamaplayer`, aliases `/opm`, `/nova`)

```
/opm spawn [name]            /opm despawn [name]
/opm status                  what it's doing, current goal, last decision, MSPT cost
/opm say <text>              force it to speak
/opm order <text>            same as chatting an order
/opm follow | stop | home | come | stay | sleep
/opm mine <block> <count>    /opm craft <item> [count]   /opm build shelter|wall|bridge
/opm inventory | give <item> | drop
/opm brain                   resolved model, capabilities, tok/s, last latency, token usage
/opm model <name|auto>       /opm reload                 (config + persona hot reload)
/opm undo [steps]            roll back its block edits
/opm protect add <radius>    mark an area it must never modify
/opm memory dump | clear | journal
/opm log [n]                 last n prompts+responses (debug, ops only)
/opm perf                    timing breakdown: senses / nav / skills / cognition
```

### Safety rails (always on)

- **Edit log + undo**: every block change is journaled; `/opm undo` reverts up to 500 edits.
- **Protected areas**: player-marked zones, plus a global deny on bedrock/end-portal-frames and anything with
  a container the player owns (configurable).
- **Radius limit**: won't modify blocks farther than `config.maxEditDistance` from where the owner last saw it,
  unless the order explicitly says otherwise.
- **Rate limits**: max block edits/second, max chat messages/minute (with a "…" style cooldown), no chat spam loops.
- **No PvP by default**; `/opm pvp` unlocks it for consenting adults.
- **Anti-grief default**: it will not break blocks that are part of a structure it didn't build, if a player placed
  them (tracked via a lightweight "player-placed" flag we record on block-edit events).

### Client-side niceties (optional, `client` source set, no extra mods)

Name/skin selection (a `GameProfile` `textures` property can point at any skin URL — optional, off by default),
a small "Nova is: mining oak logs" action bar opt-in, and a vanilla-`Screen` config UI. **All optional**; the mod
is fully functional headless.

---

## 11. Performance budget

| Layer | Budget (per companion, per tick) |
|---|---|
| Senses | ≤ 0.3 ms (amortized, spread scanning) |
| Reflexes | ≤ 0.1 ms |
| Skills + navigation steering | ≤ 0.3 ms |
| Pathfinding | ≤ 0.2 ms on-thread (search itself is on a worker, ≤ 1 search in flight) |
| Cognition | **0 ms on-thread** (async; only result application, ≤ 0.05 ms) |
| **Total** | **≤ 1.0 ms/tick** |

Guard rails:
- `DegradationManager`: if server MSPT > 40 ms → halve scan budgets → double cognition interval → pause idle
  companions → log why. Reversible automatically.
- Max 5 active companions (configurable, default 3); extras auto-sleep.
- Because Ollama usually runs **on the same machine as the game**, GPU/VRAM and CPU cache contention is a real
  risk: default `OLLAMA_NUM_PARALLEL=1` guidance, small `num_ctx` (4096–8192), Q4_K_M quantizations, and the
  ability to point the mod at a **second machine** on the LAN (`config.ollamaHost`) — documented as the
  recommended setup for players with only 8 GB VRAM.

---

## 12. Package layout

```
dev.qynl.ollamaplayer
├── OllamaPlayer.java              // ModInitializer: config, lifecycle, companion registry
├── config/
│   ├── ModConfig.java             // typed config + JSON (de)serialization + schema migration
│   ├── Persona.java               // persona.txt loading
│   └── ConfigCommands.java
├── body/
│   ├── AiPlayer.java              // extends net.fabricmc.fabric.api.entity.FakePlayer
│   ├── AiPlayerManager.java       // spawn/despawn/persist, tick dispatch, dimension moves
│   ├── Actuators.java             // the ONLY place that calls interactionManager/attack/use
│   └── ActionLog.java             // block-edit journal + undo
├── sense/
│   ├── Senses.java                // tick-spread scanners, dirty flags
│   ├── BlockScanner.java          // spiral scan with per-tick budget
│   ├── EntityScanner.java
│   └── Observation.java           // the object we serialize into the prompt
├── nav/
│   ├── NavRegion.java             // immutable block snapshot (never reads the world off-thread)
│   ├── Pathfinder.java            // A*, primitives, cost model, budgets
│   ├── Path.java / PathExecutor.java / StuckDetector.java
│   └── HazardCosts.java
├── skill/
│   ├── Skill.java / SkillRunner.java / GoalStack.java / Arbiter.java
│   ├── Result.java (SUCCESS/FAILED + machine-readable reason enum)
│   ├── locomotion/ gathering/ crafting/ building/ combat/ survival/ social/ logistics/   // the ~30 skills
│   └── craft/
│       ├── RecipePlanner.java     // recursive dependency graph over vanilla RecipeManager
│       └── StationUse.java        // crafting table / furnace / chest screen handling
├── brain/
│   ├── Brain.java                 // decision loop, staleness, priorities, circuit breaker
│   ├── OllamaClient.java          // java.net.http; tags/show/chat/embed; timeouts, retries
│   ├── ModelResolver.java         // discovery, scoring, capability probe, benchmark, cache
│   ├── PromptBuilder.java         // system + context + skills + observation + budget trimming
│   ├── ToolSchema.java            // generated from the Skill registry — single source of truth
│   ├── OutputParser.java          // tool-call + JSON mode, repair ladder, validation, clamping
│   └── RequestQueue.java          // global single-flight queue with priorities
├── mind/
│   ├── AiMemory.java              // working + episodic + semantic + procedural
│   ├── Journal.java / Landmarks.java / Lessons.java
│   └── persistence/ (NBT read/write, migrations)
├── reflex/
│   └── Reflexes.java              // tick-level survival, no LLM: lava/fall/drown/eat/flee/unstuck
├── chat/
│   ├── ChatBridge.java            // inbound routing, streaming reply delivery, cooldowns
│   └── Commands.java              // /opm, /nova
└── debug/
    ├── PerfMonitor.java           // per-layer timings, MSPT watcher
    └── DegradationManager.java
```

---

## 13. Config

`config/ollamaplayer/ollamaplayer.json` (typed, versioned, hot-reloadable):

```jsonc
{
  "schema": 1,
  "ollama": {"host":"http://127.0.0.1:11434","model":"auto","maxParams":"14B",
             "numCtx":8192,"temperatureDecide":0.3,"temperatureChat":0.8,
             "timeoutMs":8000,"keepAliveSeconds":-1},
  "cognition": {"mode":"auto","minIntervalTicks":20,"maxIntervalTicks":120,
                "maxOutputTokens":300,"autonomyEnabled":true},
  "limits": {"maxCompanions":3,"maxEditsPerSecond":6,"maxScanRadius":48,
             "maxChatPerMinute":6,"maxEditDistanceFromOwner":96},
  "safety": {"pvp":false,"breakPlayerPlaced":false,"protectedAreas":[],"undoDepth":500},
  "body": {"name":"Nova","gameModeFollowsOwner":true,"autoSleepDistance":96},
  "memory": {"journalMaxLines":200,"landmarkMax":300,"embeddings":false},
  "debug": {"verbosePrompts":false,"logLevel":"info"}
}
```

Plus `config/ollamaplayer/persona.txt` (plain text, editable in-game via `/opm persona` opening a vanilla book/screen).

---

## 14. Testing strategy

| Level | What | How |
|---|---|---|
| **Unit** (no Minecraft) | output parser (malformed/truncated/hallucinated JSON), tool validation, prompt budget trimming, model scoring, arbiter priorities, recipe planner graph, config migration | JUnit 5 in the `test` source set; `fabric-loader-junit` where MC classes are needed |
| **Pathfinding** (synthetic) | flat/stairs/maze/water/pit/door/ladder/parkour grids; assert path validity, cost monotonicity, budget compliance | pure Java, `NavRegion` built from arrays — runs in ms, ideal for TDD |
| **Mock Ollama** | whole decision loop against `com.sun.net.httpserver.HttpServer` returning canned + *adversarial* responses (tool calls with unknown ids, prose instead of JSON, half-streamed chunks, 500s, timeouts) | integration tests, fully deterministic |
| **GameTest** | real world: walk 12 blocks, chop a tree, craft planks→sticks→crafting table→pickaxe, eat at low hunger, flee a spawned zombie, store to chest, refuse lava paths, `/opm undo`, survive one night | `net.fabricmc.fabric.api.gametest.v1.FabricGameTest`, run via `./gradlew runGametest` with a seed |
| **Manual playbook** | a fixed 15-minute script: fresh world → no commands → observe; then the 6 success criteria from §1 | recorded checklist in `docs/playbook.md`, run before every release |
| **CI** | `./gradlew build` (+ gametest job if the runner can fetch MC artifacts) | existing GitHub Actions workflow |

---

## 15. Roadmap

Each phase ends with something you can *play with*, and has an explicit exit test.

| Phase | Deliverable | Exit test |
|---|---|---|
| **P0 · Spikes** (≈1 session) | S1 fake player spawns, walks, turns (pins down the movement-actuation API) · S2 mine + place via `ServerPlayerInteractionManager` · S3 A\* on a synthetic grid · S4 `OllamaClient`: `/api/tags` → pick model → tool call → parsed · S5 MSPT/perf harness | a fake player walks a 10-block square and mines one log; model auto-selection prints a chosen model on a machine with 2 models installed |
| **P1 · Body & senses** | `AiPlayer`, lifecycle, persistence, actuators, tick integration, reflexes, chunk tickets, `/opm spawn\|despawn\|status` | survives being left alone for 10 minutes: eats, avoids lava, doesn't fall down ravines, inventory persists across restart |
| **P2 · Navigation** | `NavRegion`, A\*, executor, stuck recovery, hazard costs, async search, `/opm goto` | crosses the obstacle-course GameTest; reaches a target 200 blocks away through forest without human help |
| **P3 · Skills & crafting** | skill framework, ~20 skills, recursive recipe planner, station & container use, goal stack | `craft iron_pickaxe` from scratch (punches a tree → planks → table → stone → mine iron → smelt) end-to-end |
| **P4 · Brain** | model resolution + benchmark, prompt builder, tool schemas, output parser, decision loop, streaming chat, circuit breaker, offline mode | the 4 success-criteria orders work on **both** a tool-capable and a non-tool model; killing Ollama degrades gracefully |
| **P5 · Memory & autonomy** | journal, landmarks, lessons, summarization, procedural stats, day/night autonomy loop, `/opm memory` | criterion 1: survives 3 in-game days unsupervised, with a shelter, bed, food, stone tools — and its journal reads sensibly |
| **P6 · UX, config, safety** | all commands, undo + edit log, protected areas, persona editing, optional client UI/HUD, config screen, README | a new user can install, run, configure and understand it from the README alone |
| **P7 · Hardening** | multi-companion, dedicated server, dimension travel (nether portal handling), mod compat, profiling pass, perf tuning | 3 companions + 2 players on a dedicated server: MSPT delta ≤ 2.5 ms, no crashes over an hour |

Effort shape: P0 is a day, P1–P3 are the bulk of the engineering (this is really a "write a small game AI" project),
P4 is where it becomes *magic*, P5+ is polish and depth.

---

## 16. Risks & mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **LLM latency vs 20 TPS** | companion feels drunk/stale | brain is advisory only; reflexes+skills run every tick; stale results are discarded by world-version stamp; cognition interval auto-tuned to measured tok/s |
| **GPU/CPU contention** (Ollama + Minecraft on one box) | frame drops, MSPT spikes | small `num_ctx`, 1 concurrent request, `keep_alive` release on idle, quantization guidance, LAN-remote Ollama documented, degradation manager |
| **Model can't do tool calling** | brain useless | capability probe → JSON mode → repair ladder → offline scripted mode; never a hard dependency on one model feature |
| **Hallucinated coordinates/ids** | companion walks into a wall forever | every argument is validated against the last observation; invalid → error message fed back → retry once |
| **Vanilla doesn't save fake players** | progress lost on restart | we own NBT persistence for inventory/position/mind, autosave + save on world save/stop |
| **Fake player has no skin / looks odd** | immersion | optional `GameProfile` textures property; default is a normal Steve/Alex skin |
| **Pathfinding in caves/ravines is hard** | stuck, falls | hazard costs, fall-damage-aware step costs, "never dig straight down" rule, stuck ladder, memory of bad areas |
| **Unbounded world editing = griefing** | trust destroyed | edit log + undo, protected areas, radius + rate limits, no PvP, no breaking player-placed blocks |
| **Ollama not installed / wrong version** | mod looks broken | startup health check → clear in-game message + OFFLINE mode that still works; `/opm brain` diagnostics |
| **Ollama API drift** | breakage | all calls behind `OllamaClient`; version-checked capability probe; defensive JSON parsing everywhere |
| **Fake player on a public server = cheating concerns** | bad-faith use | singleplayer/trusted-server stance in the README; server-side config to disable; the companion is deliberately identifiable in chat |
| Scope creep (this is a *big* project) | never ships | phases are vertical slices; each phase is playable; non-goals are written down (§1) |

---

## 17. Open questions (answer before P1)

1. **Name and identity**: `Nova` default; is the companion owned by one player or shared by the world?
2. **How many companions at once** by default? (plan assumes 1 active, max 3.)
3. **Does the LLM see other players' names?** (privacy default: yes for the owner's world, configurable.)
4. **Skin source**: none (Steve/Alex), bundled custom skin PNG, or user-supplied URL?

---

## Appendix A — what a real exchange looks like

**Player:** `Nova, we need iron. Go get 16 iron ore and smelt it.`

**Tool call returned by the model:**
```json
{"tool":"gather_item","args":{"item":"iron_ore","count":16,"radius":64}}
```

**Executor:** checks `count ≤ 64`, `radius ≤ maxScanRadius` → pushes goal `GatherItem(iron_ore, 16)`.
Skill scan finds no iron ore within 64 → reports `FAILED: NO_MATCHING_BLOCK_WITHIN_64` → brain is told →
it replies in chat *"No iron near here — I'll need to dig down. Want me to start a mine shaft?"* and, with
`autonomyEnabled`, proposes `dig_stairs` + `place_torch` as a two-step plan instead of silently doing nothing.

**Appendix B — manual Ollama checks while developing**
```bash
curl -s http://127.0.0.1:11434/api/tags | head            # is the model list what we think it is?
curl -s http://127.0.0.1:11434/api/show -d '{"name":"<model>"}' | grep -i capab
curl -s http://127.0.0.1:11434/api/chat -d '{
  "model":"<model>","stream":false,"keep_alive":"10m",
  "options":{"num_ctx":8192,"temperature":0.3,"num_predict":200},
  "messages":[{"role":"user","content":"Say OK."}]}'
```

**Appendix C — definition of "fully functional player" (checklist we build toward)**

```
[ ] walks, runs, jumps, swims, climbs ladders, opens doors, rides         (P1–P2)
[ ] mines with the correct tool, at correct speed, with enchantments      (P1)
[ ] places blocks, builds a shelter, bridges gaps, places torches         (P3)
[ ] crafts recursively through the tech tree, smelts, uses stations       (P3)
[ ] eats, heals, sleeps, avoids lava/falls/drowning, respawns on death    (P1)
[ ] fights, flees, uses a shield and a bow, protects the player           (P3)
[ ] manages inventory, uses chests, gives items to the player             (P3)
[ ] understands and executes natural-language orders                      (P4)
[ ] chats with memory of what happened, in a consistent personality       (P4–P5)
[ ] acts sensibly when nobody is watching                                 (P5)
```
