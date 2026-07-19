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

- **Region protection** driven by **115 flags** (blocks, environment, entities, players, zone,
  fine interactions, world/weather cycles, growth, fine spawns & explosions), every one of them
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

| Command | Description | Permission |
|---|---|---|
| `/rg help` | Lists the commands you may use | `zregions.use` |
| `/rg wand` | Gives the selection wand (left click = pos1, right click = pos2; item configurable, marker survives renaming) | `zregions.admin` |
| `/rg pos1` · `/rg pos2` | Sets a selection corner at your position | `zregions.admin` |
| `/rg addpoint` | Adds a polygon vertex at your position | `zregions.admin` |
| `/rg clearpoints` | Clears the polygon vertices (keeps pos1/pos2) | `zregions.admin` |
| `/rg star <branches> <outerRadius> [innerRadius]` | Fills the vertex list with a star centered on you (default inner radius: half the outer) | `zregions.admin` |
| `/rg create <name> [shape] [priority]` | Creates a region from your selection — shape `cuboid` (default), `cylinder`, `sphere` or `polygon` | `zregions.admin` |
| `/rg global [world]` | Creates the world-wide **global region** of a world (yours by default; the console must name one). Get-or-create: reports the existing one instead of failing | `zregions.admin` |
| `/rg redefine <name> [shape]` | Replaces a region's shape with your current selection, in its current shape type or an explicit one (same world only) | `zregions.admin` |
| `/rg remove <name>` | Deletes a region | `zregions.admin` |
| `/rg list [world]` | Lists regions (your world by default; every world from console) | `zregions.use` |
| `/rg info [name]` | Region details — without argument: the highest-priority region at your position | `zregions.use` |
| `/rg menu [region]` | Opens the region GUI — the region list, or one region's menu (**requires zMenu**; without it the command points back to `/rg help`) | `zregions.admin` |
| `/rg show [name] [seconds]` | Outlines a region's borders with particles **only you can see** (shape-aware: box edges, circles, sphere rings, polygon edges); without argument: the region at your position. The optional duration overrides `borders.display-seconds` (capped at 3600 s); `/rg show 30` reads a plain number matching no region name as the duration. Re-running replaces the outline | `zregions.use` |
| `/rg teleport <region>` · `/rg tp` | Teleports you to a safe standable spot at the region's bounding-box centre column — refused for the shapeless global region, and messaged when no safe spot exists | `zregions.teleport` |
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
| `zregions.use` | everyone | `/rg help`, `/rg list`, `/rg info` |
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
until you deny something.

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

## 7. Configuration reference (`config.yml`)

The default config.yml is shipped **translated** (same keys everywhere, only the comments
differ) and the first boot extracts the translation matching `language.yml`. Keys are never
translated. The language itself is **not** a config.yml key — see §8.

| Key | Default | Description |
|---|---|---|
| `debug` | `false` | Verbose logging |
| `permissions.bypass` | `zregions.bypass` | Permission node bypassing every protection |
| `messages.deny-throttle-milliseconds` | `2000` | Minimum delay between two "denied" messages to the same player |
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
