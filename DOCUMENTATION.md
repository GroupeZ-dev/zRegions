# zRegions — Documentation

> **Maintenance contract:** this file is the product reference for zRegions. **Every feature
> addition or change (command, flag, permission, config key, message, language, API) MUST update
> this document in the same change** — see `CLAUDE.md`. If a feature is not documented here, it
> is not ready for release.

A high-performance region protection plugin for Minecraft servers, built on a chunk-indexed
spatial engine: a protection check only ever tests the handful of regions overlapping one chunk,
never the whole region set.

| | |
|---|---|
| **Version** | 1.0.0 (unreleased) |
| **Server** | Spigot / Paper / Purpur **1.20.4+**, **Folia** supported (`folia-supported: true`) |
| **Java** | 21+ |
| **Bedrock** | Playable through Geyser/Floodgate (server-side plugin, no client mod) |
| **Storage** | SQLite (default) · MySQL · MariaDB |
| **Soft dependencies** | **zMenu** (optional GUI — see §11), **PlaceholderAPI** (outgoing `%zregions_…%` placeholders — see §12), WorldEdit, LuckPerms *(planned)* — none required. WorldGuard regions are importable via `/rg import` (file-based, works without WorldGuard) |

---

## 1. Overview

- **Region protection** driven by **168 flags** (blocks, environment, entities, players, zone,
  fine interactions, world/weather cycles, growth, fine spawns & explosions, item lifecycle,
  persistent player state, heal/feed, movement & teleport, fine damage causes), every one of them
  actually enforced by a listener — no dead flags.
- **Enter/exit engine**: `entry`/`exit` enforcement, `greeting`/`farewell` messages plus
  `title`/`subtitle`/`action-bar` displays (MiniMessage), recomputed only when a player crosses
  a block boundary.
- **Per-target flag values**: a flag can hold a different value for `owner`, `member`,
  `visitor` or `all`.
- **Priorities and inheritance**: overlapping regions resolve highest-priority-first; a region can
  inherit flags from a parent region.
- **Multi-language**: `language.yml` (loaded first, `auto` locale detection) picks the language
  of every default file — messages, config comments and the zMenu menus (en/fr/es/it bundled).
- **Multi-server-ready data model** (shared MySQL, `origin_server` column) — the cross-server
  messaging layer ships in v2.
- **100 % command-driven** — with an **optional zMenu GUI** on top (`/rg menu`: region list,
  per-region menu, flag editor), never a requirement.

## 2. Installation

1. Drop `zRegions.jar` into `plugins/`.
2. Restart the server. `plugins/zRegions/` is populated with `language.yml`, `config.yml` and
   `messages.yml` (plus `inventories/` when zMenu is installed; SQLite database `regions.db` on
   first write). **`language.yml` is loaded first and picks the language of every default file**
   — its default `language: auto` detects the server's system locale (bundled: en/fr/es/it,
   English otherwise), so a French machine gets French files out of the box.
3. Optional: adjust `language.yml`, storage backend and permission nodes, then `/rg reload`.

## 3. Commands

Root command: **`/region`** (alias **`/rg`**). Admin root: **`/zregions`** *(reserved — admin
subcommands currently live under `/rg`)*.

> **Brigadier**: on **Paper/Folia** the commands register as a native Brigadier tree (sub-command
> literals with client-side completion and inline argument suggestions); on **Spigot** completions come
> from the classic Bukkit tab-completer, optionally enriched by commodore on older builds (Spigot ≤1.19).
> The routing, permissions, execution and tab-complete data are identical everywhere — the Brigadier
> layer only binds to the shared command engine, so behaviour never differs by platform.

| Command | Description | Permission |
|---|---|---|
| `/rg help [page]` | Lists the commands you may use — paginated, each entry click-inserts its command | `zregions.use` |
| `/rg wand` | Gives the selection wand (left click = pos1, right click = pos2; item configurable, marker survives renaming) | `zregions.admin` |
| `/rg pos1` · `/rg pos2` | Sets a selection corner at your position | `zregions.admin` |
| `/rg addpoint` | Adds a polygon vertex at your position | `zregions.admin` |
| `/rg clearpoints` | Clears the polygon vertices (keeps pos1/pos2) | `zregions.admin` |
| `/rg star <branches> <outerRadius> [innerRadius]` | Fills the vertex list with a star centered on you (default inner radius: half the outer) | `zregions.admin` |
| `/rg create <name> [shape] [priority]` | Creates a region from your selection — shape `cuboid` (default), `cylinder`, `sphere` or `polygon` | `zregions.admin` |
| `/rg global [world]` | Creates the world-wide **global region** of a world (yours by default; the console must name one). Get-or-create: reports the existing one instead of failing | `zregions.admin` |
| `/rg redefine <name> [shape]` | Replaces a region's shape with your current selection, in its current shape type or an explicit one (same world only) | `zregions.admin` |
| `/rg remove <name>` | Deletes a region — **asks for confirmation** first (a clickable `[✔ Confirm]` button that runs `/rg remove <name> confirm`); the deletion only happens on confirm | `zregions.admin` |
| `/rg list [world]` | Lists regions (your world by default; every world from console) — each entry is **clickable** to open its `/rg info` | `zregions.use` |
| `/rg info [name]` | Region details — without argument: the highest-priority region at your position | `zregions.use` |
| `/rg flags [page]` | **Flag catalogue**: every registered flag, with its description on hover and click-to-start a `/rg flag` command (paginated) | `zregions.use` |
| `/rg menu [region]` | Opens the region GUI — the region list, or one region's menu (**requires zMenu**; without it the command points back to `/rg help`) | `zregions.admin` |
| `/rg show [name] [seconds]` | Outlines a region's borders with particles **only you can see** (shape-aware: box edges, circles, sphere rings, polygon edges); without argument: the region at your position. The optional duration overrides `borders.display-seconds` (capped at 3600 s); `/rg show 30` reads a plain number matching no region name as the duration. Re-running replaces the outline | `zregions.use` |
| `/rg teleport <region>` · `/rg tp` | Teleports you to the region's `teleport` flag location if set, otherwise a safe standable spot at the bounding-box centre column — refused for the shapeless global region, when no safe spot exists, and (for non-bypass players) when the region's `spawn-teleport` flag is denied | `zregions.teleport` |
| `/rg flag <region> <flag> <value…\|unset> [-t <target>]` | Sets, unsets or targets a flag value | `zregions.admin` |
| `/rg addmember <region> <player> [owner\|member]` | Adds a player (default role: member) | `zregions.admin` |
| `/rg removemember <region> <player>` | Removes a member | `zregions.admin` |
| `/rg setpriority <region> <priority>` | Changes the priority (higher wins on overlap) | `zregions.admin` |
| `/rg setparent <region> [parent]` | Sets — or clears, without argument — the flag-inheritance parent | `zregions.admin` |
| `/rg import <worldguard> [--dry-run]` | Imports regions from another protection plugin's data files — see §10. `--dry-run` reports without writing | `zregions.admin` |
| `/rg reload` | Reloads `config.yml` and the messages of the (possibly changed) language | `zregions.admin` |

**Region name resolution** — everywhere a `<region>` is expected:
- `world:name` targets a region explicitly (needed when two worlds hold a region with the same name);
- a bare name prefers a region in **your** world, then a unique cross-world match;
- an ambiguous bare name is refused with the list of matching worlds.

**Flag command details**:
- The value may span several words (`/rg flag spawn greeting Welcome, <player>!`).
- Only a **trailing** `-t <target>` pair is parsed as the target option, so values may contain a
  literal `-t`. Invalid targets are refused (never silently applied to everyone).
- `unset` (or `-`) removes the value for the chosen target: `/rg flag spawn pvp unset -t member`.

## 4. Permissions

| Node | Default | Grants |
|---|---|---|
| `zregions.use` | everyone | `/rg help`, `/rg list`, `/rg info`, `/rg flags` |
| `zregions.admin` | op | Every region management command |
| `zregions.teleport` | op | `/rg teleport` / `/rg tp` |
| `zregions.bypass` | op | Bypasses **every** region protection, including `entry`/`exit` |

The **bypass node is configurable**: `permissions.bypass` in `config.yml` (applied on `/rg reload`).
The player-condition flags (`invincible`, `fall-damage`, `hunger`, `mob-damage`, `keep-inventory`,
`exp-drop`) ignore bypass — they protect the player rather than restrict them.

## 5. Regions

- **Shapes** — all four are creatable in-game from the same two-position selection
  (`/rg pos1`/`pos2` or the wand):
  | Shape | Selection recipe |
  |---|---|
  | `cuboid` | pos1 and pos2 are opposite corners |
  | `cylinder` | pos1 = center; radius = horizontal distance to pos2; height = the two Y levels |
  | `sphere` | pos1 = center; radius = 3D distance to pos2 |
  | `polygon` | vertices via `/rg addpoint` (or `/rg star`); height = the two Y levels of pos1/pos2 |
  Positions snap to block centers; a radius below 1 block is refused. Storage, spatial index,
  flag resolution and `/rg show` are all shape-agnostic.
- **Priority**: when regions overlap, the highest priority wins for positional flag resolution.
- **Parent**: `/rg setparent` links a region to a parent whose flags apply when the child does not
  define them. Cycles are detected and refused. Parent chains are followed at most 10 levels deep
  at resolution time.
- **Members**: `owner` and `member` roles. A flag may hold different values per role — see targets.
- **Global region**: a per-world region without shape acting as world-wide fallback after every
  positional lookup. Created with `/rg global [world]` under the reserved name `__global__`
  (refused to normal regions), then managed like any region: `/rg flag __global__ pvp deny`,
  `/rg setparent`, `/rg remove __global__`… One per world; it is never indexed, never listed as a
  positional match, and has no shape (`/rg show` and `/rg redefine` refuse it).

## 6. Flags

### Resolution order

For a flag at a block position, for a given player:

1. Regions containing the position, **highest priority first**;
2. inside each region: the value for the player's most specific target
   (`owner` > `member` > `visitor`), then `all`, then the **parent chain**;
3. the world's **global region** (same target/parent walk);
4. the flag's **default value**.

`entry`/`exit`/`greeting`/`farewell`/`title`/`subtitle`/`action-bar`/`farewell-title`/`farewell-subtitle`/
`entry-deny-message`/`exit-deny-message` are **region-scoped** (region + parents + default, no global
fallback): a border and its messages are properties of their region. `deny-message`, `receive-chat` and
`command-whitelist` resolve positionally, like the other action flags.

### Catalog

All state flags default to **allow** unless stated — a freshly created region changes nothing
until you deny something. Every flag also carries a **one-line description** (localized, under
`flags.<key>` in the language file): run **`/rg flags`** to browse the whole catalogue with the
description on hover, or read it in the zMenu flag editor's item lore.

**Blocks (player actions)** — denial cancels the action and messages the player (throttled):

| Flag | Blocks when denied |
|---|---|
| `block-break` | Breaking blocks |
| `block-place` | Placing blocks |
| `interact` | Right-clicking non-container blocks (doors, buttons, levers…) |
| `container-access` | Opening containers (chests, furnaces, barrels…) |
| `bucket-fill` · `bucket-empty` | Scooping / placing fluids (checked at the exact fluid block) |
| `armor-stand` | Manipulating or breaking armor stands |
| `hanging-break` · `hanging-place` | Breaking / placing item frames & paintings (a punch popping a framed item counts as a break) |
| `vehicle-place` · `vehicle-destroy` | Placing / destroying boats & minecarts |
| `crop-trample` | Trampling farmland **and turtle eggs** |
| `item-drop` · `item-pickup` | Dropping / picking up items |

**Environment** — denial is silent:

| Flag | Blocks when denied |
|---|---|
| `redstone` | Redstone current changes inside the region |
| `piston` | Piston extend/retract when the piston, a moved block or a destination is inside |
| `fire-ignite` | A player igniting fire (flint & steel) |
| `fire-spread` | Fire spreading, blocks burning, non-player ignitions |
| `fluid-flow` | Water/lava flowing into the region |
| `leaf-decay` | Natural leaf decay |
| `block-explosion` · `entity-explosion` | Explosion **block damage** (bed/anchor vs creeper/TNT) — filtered per block, the explosion itself still happens outside |
| `mob-spawning` | Natural spawns only (spawner, patrol, raid, portal, trap…) — never conversions, breeding, eggs or plugin spawns |
| `mob-griefing` | Endermen/ravagers/silverfish altering blocks, mobs trampling farmland/turtle eggs |

**Entities & players**:

| Flag | Default | Effect when non-default |
|---|---|---|
| `pvp` | allow | Deny cancels player-vs-player damage (projectiles included) |
| `damage-animals` | allow | Deny protects animals from players |
| `mob-damage` | allow | Deny protects players from mobs and other non-player entities (mob projectiles included) — no bypass, silent |
| `invincible` | **deny** | **Allow** makes players inside immune to all damage |
| `fall-damage` | allow | Deny cancels fall damage inside |
| `hunger` | allow | Deny freezes hunger loss inside |
| `enderpearl` · `chorus-fruit` | allow | Deny blocks teleporting **into** the region by pearl / chorus |
| `keep-inventory` | **deny** | **Allow** preserves inventory **and XP** on death inside (no drops, no orbs) — no bypass |
| `exp-drop` | allow | Deny removes the XP orbs of deaths inside (items still drop) — no bypass |
| `chat` | allow | Deny blocks chatting while inside (bypass exempt, throttled message) |
| `elytra` | allow | Deny prevents **starting** to glide inside (bypass exempt) |
| `fly` | allow | Deny prevents **starting** to fly inside — survival/adventure only, creative and spectator flight untouched (bypass exempt) |
| `totem` | allow | Deny makes totems of undying fail inside (bypass exempt) |
| `command-blacklist` | *empty list* | Comma-separated command names blocked inside (`/rg flag spawn command-blacklist tp, home, sethome`). Case-insensitive, leading `/` optional, `minecraft:`-style prefixes matched too; only the command name is compared, never its arguments. Bypass exempt |

**Zone** (region-scoped):

| Flag | Type | Effect |
|---|---|---|
| `entry` | state | Deny prevents entering (walk, teleport, portals) — bypass exempt |
| `exit` | state | Deny prevents leaving — bypass exempt |
| `greeting` | text | MiniMessage sent on enter; placeholders `<player>`, `<region>` |
| `farewell` | text | MiniMessage sent on leave (also on death/respawn out of the region) |
| `title` · `subtitle` | text | MiniMessage title shown on enter (either line may be set alone); same placeholders |
| `action-bar` | text | MiniMessage shown in the action bar on enter; same placeholders |

**Interactions & entities** — denial cancels the action and messages the player (throttled):

| Flag | Blocks when denied |
|---|---|
| `ride` | Mounting vehicles (boats, minecarts) and rideable entities (horses, striders, camels…) |
| `sleep` | Sleeping in a bed |
| `respawn-anchor` | Charging or using respawn anchors |
| `item-frame-rotation` | Rotating the item displayed in an item frame |
| `use-anvil` | Opening anvils |
| `beacon` | Opening beacons |
| `villager-trade` | Trading with villagers and wandering traders |
| `shear` | Shearing sheep, mooshrooms, snow golems… |
| `leash` | Leashing entities with a lead |
| `animal-breeding` | Breeding animals |
| `sign-edit` | Editing sign text (including editable modern signs) |
| `fishing-hook` | Reeling entities/items in with a fishing rod |
| `projectile-launch` | Firing projectiles (bow, crossbow, trident, snowball, egg, fireball…) |
| `receive-chat` | *Receiving* chat while standing inside — independent of the sender's bypass |
| `command-whitelist` | *(list)* When non-empty, only these command roots are usable inside (`/rg flag spawn command-whitelist spawn, home`). `command-blacklist` wins on conflict; bypass exempt |

**Custom messages & exit displays**:

| Flag | Type | Effect |
|---|---|---|
| `deny-message` | text | MiniMessage sent instead of the generic "denied" message for actions refused here (resolved at the player's position); placeholder `<player>` |
| `entry-deny-message` · `exit-deny-message` | text | MiniMessage replacing the generic entry/exit refusal (region-scoped); placeholders `<player>`, `<region>` |
| `farewell-title` · `farewell-subtitle` | text | MiniMessage title shown on leave — symmetric to `title`/`subtitle` on enter; same placeholders |

**Environment — world, weather & natural cycles** — all silent (no player to message):

| Flag | Blocks when denied |
|---|---|
| `lightning` | Lightning striking inside the region |
| `lava-fire` | Lava setting fire nearby (specific of `fire-spread`) |
| `fire-burn` | Fire consuming blocks (specific of `fire-spread`) |
| `water-flow` · `lava-flow` | Water / lava flowing (each a specific of `fluid-flow`) |
| `block-spread` | Non-fire spreading: grass, mycelium, mushrooms, vines, sculk… |
| `snow-fall` · `snow-melt` | Snow layers forming / melting |
| `ice-form` · `ice-melt` | Ice forming / melting |
| `frosted-ice-form` · `frosted-ice-melt` | Frost-walker frosted ice forming / melting |
| `soil-dry` | Farmland reverting to dirt |
| `coral-fade` | Coral dying out of water |
| `snowman-trails` | Snow golems leaving snow trails |

**General → specific resolution**: `water-flow`/`lava-flow` override `fluid-flow`, and `lava-fire`/`fire-burn`
override `fire-spread` — but only where the specific flag is actually set; everywhere else the general flag
decides. So `fluid-flow deny` stops every fluid, then `water-flow allow` in a sub-region lets water through.

**Growth** — all silent:

| Flag | Blocks when denied |
|---|---|
| `crop-growth` | Crops, sugar cane, cactus, bamboo, stems, cave vines… growing |
| `tree-growth` | Saplings growing into trees |
| `mushroom-growth` | Mushrooms growing into huge mushrooms |
| `vine-growth` | Vines (incl. weeping/twisting) spreading — specific of `block-spread` |
| `grass-spread` · `mycelium-spread` | Grass / mycelium spreading — specifics of `block-spread` |
| `sculk-growth` | Sculk & sculk veins spreading — specific of `block-spread` |
| `bone-meal` | Using bone meal |
| `entity-transform` | Entity transformations (zombie-villager cure, mooshroom, piglin zombification…) |

**Fine spawns** — refine `mob-spawning`, resolved most-specific first (entity type → category → reason → `mob-spawning`); silent. Conversion spawns (drowned, frozen…) are never cancelled — the server would delete the source mob.

| Flag | Blocks the spawn of |
|---|---|
| `animal-spawning` · `monster-spawning` | Passive animals / hostile monsters |
| `phantom-spawning` · `slime-spawning` | Phantoms / slimes & magma cubes |
| `spawner-spawning` | Mob spawners |
| `natural-spawning` | Natural (world) spawns |
| `egg-spawning` · `command-spawning` | Spawn eggs / `/summon` & plugin spawns |
| `raid-spawning` · `patrol-spawning` | Raid & village mobs / pillager patrols |
| `portal-spawning` | Nether-portal spawns (zombified piglins…) |
| `deny-spawn` | *(list)* Explicit entity types blocked here (`/rg flag spawn deny-spawn cow, zombie`; `minecraft:` prefix accepted) |

**Fine damage & explosions**:

| Flag | Default | Effect when denied |
|---|---|---|
| `villager-damage` · `monster-damage` | allow | Players damaging villagers / monsters |
| `pet-damage` | allow | Damaging tamed pets |
| `firework-damage` | allow | Firework blast damage to entities (no bypass) |
| `entity-explosion-damage` | allow | Explosion *damage* to entities/players (block damage stays on `entity-explosion`) — no bypass |
| `melee-pvp` · `projectile-pvp` | allow | Melee / projectile PvP — each overrides the general `pvp` where set |
| `creeper-explosion` · `tnt` · `ghast-fireball` · `wither-damage` · `enderdragon-block-damage` | allow | Block damage from that source — each overrides the general `entity-explosion` where set |
| `potion-splash` | allow | Splash / lingering potions taking effect here |

**Fine block interactions** — denial cancels the interaction and messages the player (except pressure plates, silent):

| Flag | Blocks when denied |
|---|---|
| `door-use` · `trapdoor-use` | Opening doors & fence gates / trapdoors — override `interact` |
| `button-use` · `lever-use` | Pressing buttons / flipping levers — override `interact` |
| `pressure-plate-use` | Triggering pressure plates (silent) |
| `ender-chest-use` | Opening ender chests |
| `crafting-table-use` · `enchant-table-use` | Opening crafting / enchanting tables |
| `break-spawners` · `place-spawners` | Breaking / placing mob spawners — override `block-break`/`block-place` |

**Items, drops & merges** — silent (outcomes, not actions):

| Flag | Effect when denied |
|---|---|
| `item-despawn` | Dropped items never despawn |
| `item-merge` | Dropped items don't merge into stacks |
| `mob-drops` | Mobs dying here drop no items or XP |
| `block-drops` | Blocks broken here drop no items |
| `drop-on-death` | A player dying here drops no items (XP is handled by `exp-drop`) |

**Persistent player state** — applied on entering a region that sets it and restored on leaving (highest-priority region wins; per-target values supported):

| Flag | Type | Effect |
|---|---|---|
| `gamemode` | text | Game mode inside (`survival`/`creative`/`adventure`/`spectator`); the mode from before entering is restored on exit |
| `time-lock` | text | Client-only time of day (`day`/`noon`/`sunset`/`night`/`midnight`/`sunrise`, or a tick number); reset on exit |
| `weather-lock` | text | Client-only weather (`clear` or `rain`); reset on exit |
| `walk-speed` · `fly-speed` | number | Walk / fly speed inside (0.0–1.0); restored to the vanilla default (0.2 / 0.1) on exit |

> These override the player's state only while inside; another plugin managing the same state may conflict. Values that persist across sessions (speeds, game mode) are also restored on quit.

**Heal & feed** — applied on a one-second tick to players inside; silent:

| Flag | Type | Effect |
|---|---|---|
| `heal-amount` · `feed-amount` | number | Health / food added each interval (negative = poison / starve zone); setting it is what activates the effect |
| `heal-delay` · `feed-delay` | number | Seconds between applications (default 2) |
| `heal-min-health` · `heal-max-health` | number | Health bounds of the effect (default 0 / 20) |
| `feed-min-hunger` · `feed-max-hunger` | number | Food bounds of the effect (default 0 / 20) |

**Extended state**:

| Flag | Default | Effect |
|---|---|---|
| `glow` | deny | Allow makes players glow while inside (restored on exit/quit) |
| `experience-multiplier` | `1.0` | Multiplies XP gained inside (`0` = none, `2` = double) |

**Movement, portals & teleport**:

| Flag | Blocks when denied |
|---|---|
| `portal-use` | Using nether / end portals |
| `move` | Moving inside — freezes the player at their block (bypass exempt; a teleport still works) |
| `teleport-in` · `teleport-out` | Teleporting **into** / **out of** the region (bypass exempt) |

**Region spawn & teleport** — value and control flags around `/rg teleport` and respawning. Set a
`location` flag by standing where you want it and running `/rg flag <region> <flag> here` (raw
`world;x;y;z;yaw;pitch` also works):

| Flag | Type / default | Effect |
|---|---|---|
| `teleport` | location, unset | Destination `/rg teleport <region>` sends players to — overrides the bounding-box centre; the custom spot is used as-is (no safe-spot search) |
| `spawn` | location, unset | Where players respawn after dying anywhere inside the region (highest-priority region wins), set on `PlayerRespawnEvent` |
| `teleport-message` | text, empty | MiniMessage sent after a `/rg teleport` into the region instead of the generic confirmation; placeholders `<player>`, `<region>` |
| `spawn-teleport` | allow | Deny stops non-bypass players using `/rg teleport` to reach the region |
| `exit-via-teleport` | allow | With `exit` denied, a **teleport** may still leave (a walk cannot) — never traps players; deny locks even teleports in |
| `exit-override` | deny | Allow always permits leaving the region, ignoring every `exit` denial |

**Fine damage causes** — each protects **players** inside from one damage cause; silent, no bypass. All
default to **allow** (vanilla damage stays on). `invincible` (allow) still blocks *every* cause outright;
these are the finer controls for when you only want to neutralise one source.

| Flag | Neutralises when denied |
|---|---|
| `fire-damage` | Burning damage (standing in fire / on fire) |
| `lava-damage` | Damage from standing in lava |
| `drowning-damage` | Drowning damage |
| `suffocation-damage` | Suffocation inside a block |
| `contact-damage` | Cactus / sweet-berry / stalagmite contact damage |
| `void-damage` | The void (below the world) — the player stops taking damage but does **not** stop falling; pair with a teleport |
| `freeze-damage` | Powder-snow freezing damage |
| `starvation-damage` | Starving at empty hunger (distinct from `hunger`, which freezes the bar) |
| `lightning-damage` | Being struck by lightning (distinct from `lightning`, which stops the strike itself) |
| `dragon-breath-damage` | The ender dragon's breath cloud |
| `hot-floor-damage` | Standing on magma blocks |
| `fly-into-wall-damage` | Elytra kinetic (flying into a wall) damage |
| `cramming-damage` | Entity-cramming damage (too many entities in one spot) |

## 7. Configuration reference (`config.yml`)

The default config.yml is shipped **translated** (same keys everywhere, only the comments
differ) and the first boot extracts the translation matching `language.yml`. Keys are never
translated. The language itself is **not** a config.yml key — see §8.

| Key | Default | Description |
|---|---|---|
| `debug` | `false` | Verbose logging |
| `permissions.bypass` | `zregions.bypass` | Permission node bypassing every protection |
| `messages.deny-throttle-milliseconds` | `2000` | Minimum delay between two "denied" messages to the same player |
| `messages.palette.primary` | `#38BDF8` | Colour of headers/titles — a hex colour (`#RRGGBB`, `#` optional); invalid/empty falls back to the default |
| `messages.palette.accent` | `#FBBF24` | Colour of values & names (the emphasis colour) |
| `messages.palette.success` | `#4ADE80` | Colour of confirmations |
| `messages.palette.error` | `#FB7185` | Colour of refusals & errors |
| `messages.palette.body` | `#CBD5E1` | Colour of ordinary text |
| `messages.palette.muted` | `#64748B` | Colour of punctuation & secondary detail |
| `regions.creator-becomes-owner` | `true` | Whether `/rg create` adds the creator as the region's owner; `false` = new regions have no owner (admin-managed only, `zregions.admin`) |
| `selection.wand-item` | `BLAZE_ROD` | Bukkit Material of the `/rg wand` item (invalid names fall back to BLAZE_ROD) |
| `borders.particle` | `FLAME` | Bukkit particle used by `/rg show` (invalid names fall back to FLAME) |
| `borders.display-seconds` | `10` | How long the outline stays visible when the command gives no duration |
| `borders.refresh-milliseconds` | `500` | Particle refresh period |
| `borders.point-spacing` | `0.5` | Distance in blocks between two outline particles |
| `borders.max-points` | `1500` | Particle cap per refresh — huge regions get a sparser outline, never a lag spike |
| `multi-server.enabled` | `false` | Reserved for the v2 cross-server layer; requires MySQL/MariaDB |
| `multi-server.server` | `global` | Logical name of this server on a network (stamped on every region) |
| `storage.type` | `SQLITE` | `SQLITE`, `MYSQL` or `MARIADB` |
| `storage.table-prefix` | `zregions_` | Prefix of every zRegions table |
| `storage.database.*` | — | Host/port/database/user/password for MySQL/MariaDB |

`/rg reload` re-reads `language.yml`, `config.yml` (including the cached hot-path values
above), `messages.yml` and the zMenu inventories — deleted default files regenerate in the
`language.yml` language.

## 8. Languages

The language system follows the zAuctionHouse model: **`language.yml`** sits at the root of
`plugins/zRegions/`, is loaded **before every other file** and is never translated. It selects
the language of the **default files** the plugin extracts from its jar: `config.yml`
(translated comments), **`messages.yml` at the plugin-folder root**, and the zMenu
`inventories/` menus.

- `language: auto` (the default) detects the server's system locale; explicit codes: `en`,
  `fr`, `es`, `it`. An unknown code warns and falls back to English.
- The plugin **never overwrites an existing file**. To change language after the first boot:
  edit `language.yml`, delete the files to regenerate (`config.yml`, `messages.yml`,
  `inventories/` — keep `language.yml` and `regions.db`), then restart or `/rg reload`.
- **Custom language / customization**: edit the extracted `messages.yml` and `inventories/`
  files directly, in any language — they are plain files, no folder convention needed.
- A **missing key** in `messages.yml` falls back to the built-in English default — an outdated
  file never breaks the plugin.
- Format: [MiniMessage](https://docs.advntr.dev/minimessage/format.html) for messages.
  Placeholders like `<region>` or `<player>` are filled by the plugin and must be kept verbatim.
  The inventory files use zMenu's classic `&`/hex color codes and `%placeholder%` tokens.
- **Colour palette**: messages use **semantic tags** — `<primary>` (#38BDF8), `<accent>` (#FBBF24),
  `<success>` (#4ADE80), `<error>` (#FB7185), `<body>` (#CBD5E1), `<muted>` (#64748B) — instead of
  vanilla colour names. This gives a fixed, high-contrast theme that looks the same on every client.
  The six hex values are **configurable** under `messages.palette.*` in **config.yml** (see §7): change
  them there to reskin every message at once (applied on `/rg reload`; an invalid value falls back to
  its default). You can still use any MiniMessage colour/tag directly in your own message edits.
- **Interactive messages**: several messages carry click/hover actions (the `/rg remove`
  confirmation button, clickable `/rg list` and `/rg help` entries, and the `/rg flags` catalogue).
  The interactivity is attached by the plugin — you only translate the visible text.
- **Flag descriptions** live under a `flags:` section (`flags.<key>`), one line per flag, in every
  language file. They feed both `/rg flags` (on hover) and the zMenu flag editor lore. A missing key
  falls back to the built-in English description, so the catalogue is never blank.

## 9. Storage & multi-server

- **SQLite** (default): zero-config local file `regions.db`. **MySQL/MariaDB**: fill
  `storage.database` — required as soon as several servers share the region database.
- All queries are served from an in-memory chunk index; the database is only read at startup and
  written asynchronously (writes are serialized per submission order).
- The data model is **network-ready from v1**: every region carries `origin_server` (from
  `multi-server.server`) and an optimistic-lock `version` column; each instance only loads its own
  regions plus the `global` ones. The Redis/plugin-message **messaging layer ships in v2** — no
  schema migration will be needed.

## 10. Importing from WorldGuard

`/rg import worldguard [--dry-run]` reads WorldGuard's data files directly
(`plugins/WorldGuard/worlds/<world>/regions.yml`) — **WorldGuard does not need to be installed
or loaded**, its leftover files are enough. Always start with `--dry-run`: it parses and reports
without writing anything.

What is imported:

- **Shapes**: `cuboid` and `poly2d` (→ `polygon`). Both keep WorldGuard's **block-inclusive
  boundary**: the block columns lying on a poly2d outline stay protected (the polygon is widened
  one block on its max-facing sides — never a grief strip along an imported border). The
  `__global__` region becomes the world's global region (skipped if one already exists), members
  included.
- **Priority**, **parent links** (linked after all regions are created, whatever the file order)
  and **UUID-based owners/members**. Name-based (pre-UUID) and permission-group entries are
  skipped and reported.
- **WorldGuard's implicit membership protection is reproduced**: in WG every region denies
  building to non-members even with an empty flag list. Each imported region therefore receives
  visitor-targeted denies on `block-break`/`block-place`/`interact`/`container-access` — except
  when the file sets the matching WG flag explicitly (`build`, `block-break`, `block-place`,
  `use`/`interact`, `chest-access`), or `passthrough: allow` marks the region as a
  non-protecting overlay, or the region is the global one. Owners/members keep building, exactly
  like in WG. These synthesized values are regular flags afterwards (`/rg flag … unset -t
  visitor` removes them); they are not counted in the "flag values" import summary.
- **Flags** with a zRegions equivalent, notably: `build` (expands to
  `block-break`+`block-place`+`interact`+`container-access`), `chest-access`→`container-access`,
  `use`/`interact`→`interact`, the explosion family (`creeper-explosion`/`tnt`/`ghast-fireball`
  →`entity-explosion`; `other-explosion` feeds **both** `entity-explosion` and
  `block-explosion` since it covers bed/anchor blasts; deny wins when they conflict),
  `pistons`→`piston`, `water-flow`/`lava-flow`→`fluid-flow`, `lighter`→`fire-ignite`,
  `enderman-grief`→`mob-griefing`, `entity-painting-destroy`/`entity-item-frame-destroy`→
  `hanging-break`, `block-trampling`→`crop-trample`, `chorus-fruit-teleport`→`chorus-fruit`,
  `exp-drops`→`exp-drop`, `send-chat`→`chat`, `blocked-cmds`→`command-blacklist`,
  `greeting`/`farewell`/`greeting-title`→`greeting`/`farewell`/`title`, plus the identically
  named state flags (`pvp`, `invincible`, …). **`entry` and `exit` are imported at the
  `visitor` target**: WG applies them to non-members only, so imported members are never locked
  out of (or trapped inside) their own regions.

Limits (each occurrence is reported, nothing is skipped silently): WG flags without an
equivalent, per-group flag values (`*-group`), region collisions with existing names, and text
flags written with legacy `&` color codes (imported verbatim — rewrite them in MiniMessage).
The chat shows a summary; **every skipped item is detailed in the server log**. A misspelled
option (anything other than `--dry-run`) aborts with the usage line instead of silently running
the real import.

## 11. GUI (optional, zMenu)

When the [zMenu](https://groupez.dev) plugin is installed, `/rg menu` opens a chest GUI —
without it, **every feature stays available through commands** (the GUI is a layer, never a
requirement; the command then points back to `/rg help`).

- **Region list** (`/rg menu`): every region, paginated, world then name order; click to manage.
- **Region menu** (`/rg menu <region>` or a list click): details, a **priority +/- control**
  (left click +1, right click −1, shift ×10, never below 0, live `%priority%`), flag editor,
  member manager, border outline (same per-player particles as `/rg show`), back to the list.
- **Flag editor**: all flags paginated, **each with its own telling icon** (pvp → sword,
  block-break → pickaxe, chat → paper…; addon flags keep the template item), showing their
  current explicit value and default; left-clicking a state flag cycles unset → deny → allow →
  unset, right-clicking removes the value directly (values apply to the `all` target — use
  `/rg flag … -t <target>` for per-role values); text/list flags point back to `/rg flag`.
- **Member manager**: the region's members paginated, owners first; left click toggles the role
  owner ↔ member, right click removes. The **add** button lists online players not yet members —
  click to add as member (offline players go through `/rg addmember`).

The five inventories ship in the jar **in the four bundled languages** and are extracted to
`plugins/zRegions/inventories/` (`regions.yml`, `region.yml`, `flags.yml`, `members.yml`,
`add-member.yml`) on first use, in the `language.yml` language — customize them like any zMenu
inventory (`%region%`, `%world%`, `%flag%`, `%value%`, `%player%`, `%role%`… placeholders are
provided by zRegions button types `ZREGIONS_*`), then `/rg reload`. A region deleted while its
menu is open closes the menu with a message instead of going stale.

## 12. PlaceholderAPI

When [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) is installed,
zRegions registers an outgoing `%zregions_…%` expansion automatically at startup — no
configuration needed. Every placeholder is resolved at the requesting player's **current
location** from the in-memory index (never a database hit) and requires an online player.

| Placeholder | Returns |
|---|---|
| `%zregions_current_region%` | Name of the highest-priority region at the player's position (empty if none) |
| `%zregions_current_region_priority%` | Priority of that region (empty if none) |
| `%zregions_region_count%` | Total number of regions |
| `%zregions_region_count_world%` | Number of regions in the player's world |
| `%zregions_is_owner%` | `true`/`false` — is the player an owner of the region here |
| `%zregions_is_member%` | `true`/`false` — is the player an owner or member of the region here |
| `%zregions_flag_<key>%` | Effective value of a flag at the player's position for that player, e.g. `%zregions_flag_pvp%` → `allow`/`deny`. Unknown flag keys return empty |

Any plugin that expands PlaceholderAPI strings (scoreboards, chat, holograms, tab lists…) can
use them. An unrecognized `%zregions_…%` placeholder is left untouched.

## 13. For developers

- The **`api` module** has zero platform dependencies. On Bukkit the entry point is registered in
  the ServicesManager:
  ```java
  RegionManager regions = Bukkit.getServicesManager().load(RegionManager.class);
  ```
- Key API surface: `RegionManager` (CRUD, `getRegionsAt`, positional & region-scoped
  `resolveFlag`, members/priority/parent/redefine, `reload(UUID)`,
  `createGlobalRegion(world)`/`getGlobalRegion(world)` with the reserved
  `RegionManager.GLOBAL_REGION_NAME`), `Region`, `Flag<T>`, `FlagRegistry` (register custom flags
  **before** regions load), `RegionShape`/`BoundingBox`/`Vector3`
  (`RegionShape.sampleBorder(spacing)` yields the outline points used by `/rg show`).
- Architecture (LuckPerms model — `api` / `common` / `bukkit`): see `ARCHITECTURE.md`.

## 14. Version history

### 1.0.0 — Unreleased
- **Brigadier commands**: `/region`, `/rg` and `/zregions` now register through Brigadier as a binding
  layer over the unchanged common `RegionCommandManager` (routing/permissions/execution stay platform-
  agnostic). **Paper/Folia** get a native tree (sub-command literals + inline suggestions, with an
  unknown-sub-command fallback that keeps zRegions' own message) via the Paper `LifecycleEvents.COMMANDS`
  registrar in the isolated `paper` sourceSet, loaded by name so its Paper types never link on Spigot.
  **Spigot** keeps the Bukkit executor and adds commodore completions where supported (older builds);
  commodore degrades to a no-op on Spigot 1.20+, leaving standard Bukkit tab-completion. Validated by
  booting Paper 1.21.7 and Spigot 1.20.4. Brigadier itself is server-provided (never shaded); commodore
  is shaded + relocated.
- **6 new flags** (162 → 168, batch B10 finish — teleport & location). A new **`LocationFlag`** value
  type (`teleport`/`spawn`, serialised as `world;x;y;z;yaw;pitch`, round-trips through storage and the
  WorldGuard import): **`teleport`** overrides `/rg teleport`'s bounding-box-centre destination (the
  custom spot is used as-is), **`spawn`** sets the respawn point when a player dies inside the region
  (new `RespawnListener` on `PlayerRespawnEvent`), **`teleport-message`** replaces the generic teleport
  confirmation, **`spawn-teleport`** (allow) gates non-bypass players' `/rg teleport` access per region,
  and **`exit-via-teleport`** (allow) / **`exit-override`** (deny) keep a denied `exit` from ever
  trapping players — a teleport escapes an `exit` lockdown by default, and `exit-override` always allows
  leaving (a new `MovementCause` is threaded through the movement check). Adds the region-scoped
  `RegionManager.resolveFlagIfSet` and the `region.teleport-denied` message; WorldGuard import maps
  `teleport`/`spawn` (location) and `exit-via-teleport`/`exit-override` (state).
- **Flag descriptions + `/rg flags`**: every one of the 162 flags now has a localized one-line
  description (`flags.<key>` in each language file — en/fr/es/it), surfaced by the new **`/rg flags`**
  catalogue command (hover for the description, click to start a `/rg flag` command; paginated) and in
  the zMenu flag editor lore (**word-wrapped to ≤6 words per line**, injected into the item lore). Backed
  by a `FlagDescriptions` English fallback so the catalogue is never blank.
- **Interactive flag messages**: the `/rg flag` feedback now makes the flag name itself interactive —
  in the *set*/*unset*/*invalid-value* messages it hovers to show the flag's description and clicks to
  re-open `/rg flag <region> <flag> `; the *unknown flag* message links (click) to `/rg flags`. Built in
  Java and injected as a MiniMessage component placeholder, so hover text with apostrophes stays safe.
- **Interactive messages** (MiniMessage click/hover): `/rg remove` now requires a **confirmation** — a
  clickable `[✔ Confirm]` button (running `/rg remove <name> confirm`) — so a region is never deleted by
  a single command. `/rg list` entries are clickable to their `/rg info`, and `/rg help` (now
  **paginated**, like `/rg flags`) entries click-insert their command; both pages are navigable with
  clickable `«`/`»` arrows. Interactivity is attached in Java with the region's UUID as the click target,
  so region names, world names and descriptions never need MiniMessage escaping.
- **Recoloured, configurable messages**: all chat messages moved off vanilla colour names to a
  high-contrast hex palette exposed as **semantic tags** — `<primary>`/`<accent>`/`<success>`/`<error>`/
  `<body>`/`<muted>` (new `Palette`, resolved by `MessageService`). The six colours are **configurable**
  under `messages.palette.*` in config.yml (hex, per-key fallback to the default, refreshed on `/rg
  reload`), so the whole message theme can be reskinned without touching code. Applied across the English
  defaults and all four language files.
- **13 new flags** (149 → 162, batch B12), all enforced. **Fine damage causes** (in the player-state
  listener, silent, no bypass — each protects players from one `EntityDamageEvent` cause while
  `invincible` still blocks all): `fire-damage`, `lava-damage`, `drowning-damage`, `suffocation-damage`,
  `contact-damage`, `void-damage`, `freeze-damage`, `starvation-damage`, `lightning-damage`,
  `dragon-breath-damage`, `hot-floor-damage`, `fly-into-wall-damage`, `cramming-damage`. All default to
  allow; no WorldGuard equivalents to import.
- **14 new flags** (135 → 149). **Heal/feed** (new `RegionHealFeedTicker`, one-second tick):
  `heal-amount`/`feed-amount` with `*-delay`/`*-min-*`/`*-max-*` bounds (negative amounts = poison/
  starve zones). **Extended state**: `glow`, `experience-multiplier`. **Movement/teleport** (batch
  B10, in the movement listener): `portal-use`, `move` (freeze), `teleport-in`, `teleport-out`.
  Adds the `RegionPlayer` health/food/glow hooks.
- **5 persistent player-state flags** (130 → 135), applied on region enter and restored on exit:
  `gamemode` (restores the previous mode), `time-lock` & `weather-lock` (client-only, named or tick/
  keyword values), `walk-speed` & `fly-speed`. Backed by a new `RegionPlayerStateService` wired into
  the movement tracker (per-player apply/restore, restored on quit) and a new numeric `DoubleFlag`
  type (also the building block for future rate flags). Adds the `RegionPlayer` platform hooks
  (`setPlayerTime`/`setPlayerWeather`/`setWalkSpeed`/`setFlySpeed`/`getGameMode`/`setGameMode`).
- **15 new flags** (115 → 130), all enforced. **Fine block interactions** (override `interact`/
  `block-break`/`block-place` where set): `door-use`, `trapdoor-use`, `button-use`, `lever-use`,
  `pressure-plate-use`, `ender-chest-use`, `crafting-table-use`, `enchant-table-use`,
  `break-spawners`, `place-spawners`. **Item lifecycle** (new `ItemListener`, silent): `item-despawn`,
  `item-merge`, `mob-drops`, `block-drops`, `drop-on-death`.
- **34 new flags** (81 → 115), all enforced, in three families. **Growth** (`GrowthListener`):
  `crop-growth`, `tree-growth`, `mushroom-growth`, `vine-growth`, `grass-spread`, `mycelium-spread`,
  `sculk-growth`, `bone-meal`, `entity-transform`. **Fine spawns** (refine `mob-spawning`, most-specific
  first, conversion spawns never cancelled): `animal-spawning`, `monster-spawning`, `spawner-spawning`,
  `phantom-spawning`, `slime-spawning`, `natural-spawning`, `egg-spawning`, `command-spawning`,
  `raid-spawning`, `patrol-spawning`, `portal-spawning`, `deny-spawn` (entity-type list). **Fine damage &
  explosions**: `villager-damage`, `monster-damage`, `pet-damage`, `firework-damage`,
  `entity-explosion-damage`, `melee-pvp`/`projectile-pvp` (override `pvp`), the explosion family
  `creeper-explosion`/`tnt`/`ghast-fireball`/`wither-damage`/`enderdragon-block-damage` (override
  `entity-explosion`) and `potion-splash`. Adds `RegionManager.resolveFlagIfSet` (the N-way building block
  of the general→specific resolution). WorldGuard import gained the growth mappings.
- **15 environment flags** (66 → 81), all enforced: `lightning`, `lava-fire`, `water-flow`,
  `lava-flow`, `fire-burn`, `block-spread`, `snow-fall`, `snow-melt`, `ice-form`, `ice-melt`,
  `frosted-ice-form`, `frosted-ice-melt`, `soil-dry`, `coral-fade`, `snowman-trails`. Introduces
  **general→specific flag resolution** (`RegionManager.resolveFlagOrGeneral`): `water-flow`/`lava-flow`
  override `fluid-flow` and `lava-fire`/`fire-burn` override `fire-spread`, but only where the specific
  flag is set. WorldGuard import now maps these (and re-points `water-flow`/`lava-flow` to the dedicated
  flags instead of the general `fluid-flow`).
- **20 new flags** (46 → 66), all enforced. Interactions & entities: `ride`, `sleep`,
  `respawn-anchor`, `item-frame-rotation`, `use-anvil`, `beacon`, `villager-trade`, `shear`,
  `leash`, `animal-breeding`, `sign-edit`, `fishing-hook`, `projectile-launch`, `receive-chat`,
  `command-whitelist` (whitelist counterpart of `command-blacklist`, blacklist wins). Custom
  messages & exit displays: `deny-message` (positional, overrides the generic denial),
  `entry-deny-message`/`exit-deny-message` (region-scoped border refusals) and
  `farewell-title`/`farewell-subtitle` (symmetric to the enter `title`/`subtitle`). WorldGuard
  import gained the matching mappings (`ride`, `sleep`, `respawn-anchors`, `item-frame-rotation`,
  `use-anvil`, `receive-chat`, `deny-message`, `farewell-title`).
- **`/rg teleport <region>`** (alias **`/rg tp`**, permission `zregions.teleport`): teleports the
  player to a safe standable spot in the region's bounding-box centre column (solid ground, two
  passable non-liquid blocks above); the shapeless global region is refused and a message is sent
  when no safe spot exists. The world read and the teleport are dispatched to the game thread
  (commands run async). Sub-commands can now carry **aliases**.
- **PlaceholderAPI expansion** (`%zregions_…%`, §12): outgoing placeholders — current region and
  its priority, region counts (total / per-world), owner/member checks, and per-flag value
  (`%zregions_flag_<key>%`) — all resolved at the player's position from the in-memory index and
  auto-registered when PlaceholderAPI is present, loaded through the `Hooks/` infrastructure.
- **GUI additions**: a **priority +/- control** in the region menu (left +1 / right −1 / shift
  ×10, clamped at 0, live `%priority%`, global region left untouched) and a **distinct icon per
  flag** in the flag editor (addon flags keep the template item).
- **Config**: `regions.creator-becomes-owner` (default `true`) toggles whether `/rg create` adds
  the creator as owner — set `false` for admin-only regions.
- **bStats** telemetry wired (relocated under `libs.bstats`); inert until the service id is set
  (`storage_type`, `language`, `multi_server`, `regions` charts).
- Region engine: cuboid/cylinder/sphere/polygon shapes, per-world chunk index, priorities,
  parents, members, per-target flag values, Sarah storage (SQLite/MySQL/MariaDB),
  multi-server-ready schema.
- 46 enforced flags; enter/exit engine with greeting/farewell; two-phase (check/commit) movement
  listeners covering walks, teleports, portals, respawns and world changes.
- `/rg global [world]`: in-game creation of the per-world global region (reserved name
  `__global__`, refused to normal regions), manageable like any region afterwards.
- 11 new flags: `mob-damage`, `keep-inventory`, `exp-drop`, `chat`, `elytra`, `fly`, `totem`,
  `command-blacklist` (first list-valued flag, comma-separated), and the enter displays
  `title`/`subtitle`/`action-bar` delivered through the platform-agnostic player abstraction.
- `/rg import worldguard [--dry-run]`: file-based WorldGuard importer (shapes incl. polygons
  with WG's block-inclusive boundaries, `__global__` with its members, priorities, parents,
  UUID members, ~35 mapped flags incl. `blocked-cmds`→`command-blacklist`) — works without
  WorldGuard installed; reproduces WG's implicit non-member build protection and its
  non-members-only `entry`/`exit` semantics (visitor target); every skipped item is reported in
  the server log, never dropped silently.
- **Optional zMenu GUI** (`/rg menu [region]`): paginated region list, per-region menu
  (details, border outline, flag editor with click-to-cycle / right-click-unset state flags,
  member manager with add / remove / role toggle from the online-player list) — five
  customizable YAML inventories, loaded through the new `Hooks/` infrastructure
  (present-and-enabled check + reflection, never linked without zMenu) behind the common
  `GuiService` abstraction; full command fallback when zMenu is absent.
- Commands: help, pos1/pos2, create, redefine, remove, list, info, flag (multi-word values,
  targets, unset), addmember/removemember, setpriority, setparent, reload.
- Per-language folders with on-demand extraction (en/fr/es/it bundled); localized command
  descriptions; configurable bypass permission and deny-message throttle.
- **zAuctionHouse-style language system**: root `language.yml` (loaded first, never translated,
  `language: auto` locale detection) picks the language of every default file; `messages.yml`
  now lives at the plugin-folder **root**, and the zMenu inventories ship translated
  (en/fr/es/it) and extract in the chosen language. Regenerating defaults = delete the file(s)
  and reload.
- In-game creation of **all four shapes** (`/rg create <name> [shape]`, `/rg redefine <name>
  [shape]`), polygon vertices (`/rg addpoint`/`clearpoints`), star generator (`/rg star`) and a
  configurable selection wand (`/rg wand`, PDC-marked item).
- `/rg show`: per-player particle outline of a region's borders, following the actual shape
  (`RegionShape.sampleBorder`); optional per-command duration (default from config, capped at
  3600 s); configurable particle, duration, refresh, spacing and point cap.
