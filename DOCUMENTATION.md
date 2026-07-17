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
| **Soft dependencies** | zMenu, PlaceholderAPI, WorldEdit, LuckPerms, WorldGuard *(integrations planned — none required)* |

---

## 1. Overview

- **Region protection** driven by **35 flags** (blocks, environment, entities, players, zone),
  every one of them actually enforced by a listener — no dead flags.
- **Enter/exit engine**: `entry`/`exit` enforcement, `greeting`/`farewell` messages (MiniMessage),
  recomputed only when a player crosses a block boundary.
- **Per-target flag values**: a flag can hold a different value for `owner`, `member`,
  `visitor` or `all`.
- **Priorities and inheritance**: overlapping regions resolve highest-priority-first; a region can
  inherit flags from a parent region.
- **Multi-language**: per-language folders, only the configured language is extracted and loaded.
- **Multi-server-ready data model** (shared MySQL, `origin_server` column) — the cross-server
  messaging layer ships in v2.
- **100 % command-driven** — a GUI (zMenu) is a planned optional add-on, never a requirement.

## 2. Installation

1. Drop `zRegions.jar` into `plugins/`.
2. Restart the server. `plugins/zRegions/config.yml` and `plugins/zRegions/languages/<lang>/messages.yml`
   are created (SQLite database `regions.db` on first write). The **initial config.yml is picked in
   the language of your server's system locale** (bundled: en/fr/es/it, English otherwise) — a
   French machine gets a French-commented config with `language: fr` preset.
3. Optional: set `language`, storage backend and permission nodes in `config.yml`, then `/rg reload`.

## 3. Commands

Root command: **`/region`** (alias **`/rg`**). Admin root: **`/zregions`** *(reserved — admin
subcommands currently live under `/rg`)*.

| Command | Description | Permission |
|---|---|---|
| `/rg help` | Lists the commands you may use | `zregions.use` |
| `/rg pos1` · `/rg pos2` | Sets a selection corner at your position | `zregions.admin` |
| `/rg create <name> [priority]` | Creates a cuboid region from your selection | `zregions.admin` |
| `/rg redefine <name>` | Replaces a region's shape with your current selection (same world only) | `zregions.admin` |
| `/rg remove <name>` | Deletes a region | `zregions.admin` |
| `/rg list [world]` | Lists regions (your world by default; every world from console) | `zregions.use` |
| `/rg info [name]` | Region details — without argument: the highest-priority region at your position | `zregions.use` |
| `/rg show [name] [seconds]` | Outlines a region's borders with particles **only you can see** (shape-aware: box edges, circles, sphere rings, polygon edges); without argument: the region at your position. The optional duration overrides `borders.display-seconds` (capped at 3600 s); `/rg show 30` reads a plain number matching no region name as the duration. Re-running replaces the outline | `zregions.use` |
| `/rg flag <region> <flag> <value…\|unset> [-t <target>]` | Sets, unsets or targets a flag value | `zregions.admin` |
| `/rg addmember <region> <player> [owner\|member]` | Adds a player (default role: member) | `zregions.admin` |
| `/rg removemember <region> <player>` | Removes a member | `zregions.admin` |
| `/rg setpriority <region> <priority>` | Changes the priority (higher wins on overlap) | `zregions.admin` |
| `/rg setparent <region> [parent]` | Sets — or clears, without argument — the flag-inheritance parent | `zregions.admin` |
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
| `zregions.bypass` | op | Bypasses **every** region protection, including `entry`/`exit` |

The **bypass node is configurable**: `permissions.bypass` in `config.yml` (applied on `/rg reload`).
The player-condition flags (`invincible`, `fall-damage`, `hunger`) ignore bypass — they protect
the player rather than restrict them.

## 5. Regions

- **Shape**: regions created in-game are **cuboids** (two corners via `/rg pos1`/`/rg pos2`).
  The engine itself supports cuboid, **cylinder, sphere and polygon** shapes (storage, index and
  flag resolution are shape-agnostic); selection tools for the other shapes are on the roadmap.
- **Priority**: when regions overlap, the highest priority wins for positional flag resolution.
- **Parent**: `/rg setparent` links a region to a parent whose flags apply when the child does not
  define them. Cycles are detected and refused. Parent chains are followed at most 10 levels deep
  at resolution time.
- **Members**: `owner` and `member` roles. A flag may hold different values per role — see targets.
- **Global region**: a per-world region without shape acting as world-wide fallback after every
  positional lookup (currently seeded through the database; a dedicated command is planned).

## 6. Flags

### Resolution order

For a flag at a block position, for a given player:

1. Regions containing the position, **highest priority first**;
2. inside each region: the value for the player's most specific target
   (`owner` > `member` > `visitor`), then `all`, then the **parent chain**;
3. the world's **global region** (same target/parent walk);
4. the flag's **default value**.

`entry`/`exit`/`greeting`/`farewell` are **region-scoped** (region + parents + default, no global
fallback): a border is a property of its region.

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
| `invincible` | **deny** | **Allow** makes players inside immune to all damage |
| `fall-damage` | allow | Deny cancels fall damage inside |
| `hunger` | allow | Deny freezes hunger loss inside |
| `enderpearl` · `chorus-fruit` | allow | Deny blocks teleporting **into** the region by pearl / chorus |

**Zone** (region-scoped):

| Flag | Type | Effect |
|---|---|---|
| `entry` | state | Deny prevents entering (walk, teleport, portals) — bypass exempt |
| `exit` | state | Deny prevents leaving — bypass exempt |
| `greeting` | text | MiniMessage sent on enter; placeholders `<player>`, `<region>` |
| `farewell` | text | MiniMessage sent on leave (also on death/respawn out of the region) |

## 7. Configuration reference (`config.yml`)

The default config.yml is shipped **translated** (`languages/<lang>/config.yml` in the jar —
same keys everywhere, only the comments differ) and the first boot extracts the translation
matching the server's system locale. Keys are never translated.

| Key | Default | Description |
|---|---|---|
| `language` | `en` | Language folder to load (`languages/<language>/messages.yml`) |
| `debug` | `false` | Verbose logging |
| `permissions.bypass` | `zregions.bypass` | Permission node bypassing every protection |
| `messages.deny-throttle-milliseconds` | `2000` | Minimum delay between two "denied" messages to the same player |
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

`/rg reload` re-reads `config.yml` (including the cached hot-path values above) and the messages
of the configured language.

## 8. Languages

Everything language-specific lives in **one folder per language**:
`languages/<language>/messages.yml` on disk, plus a translated `config.yml` template per
language inside the jar.

- Only the folder matching `language` in `config.yml` is **extracted from the jar and loaded** —
  bundled languages: `en`, `fr`, `es`, `it`.
- The **default config.yml** extracted on first boot is the translation matching the server's
  system locale (fallback: English); an existing config.yml is never overwritten.
- **Custom language**: create `languages/<code>/messages.yml` yourself (copy the English one) and
  set `language: <code>`. A language with neither a folder on disk nor a bundled default falls
  back to English.
- A **missing key** in any file falls back to the built-in English default — an outdated language
  file never breaks the plugin.
- Format: [MiniMessage](https://docs.advntr.dev/minimessage/format.html). Placeholders like
  `<region>` or `<player>` are filled by the plugin and must be kept verbatim.

## 9. Storage & multi-server

- **SQLite** (default): zero-config local file `regions.db`. **MySQL/MariaDB**: fill
  `storage.database` — required as soon as several servers share the region database.
- All queries are served from an in-memory chunk index; the database is only read at startup and
  written asynchronously (writes are serialized per submission order).
- The data model is **network-ready from v1**: every region carries `origin_server` (from
  `multi-server.server`) and an optimistic-lock `version` column; each instance only loads its own
  regions plus the `global` ones. The Redis/plugin-message **messaging layer ships in v2** — no
  schema migration will be needed.

## 10. For developers

- The **`api` module** has zero platform dependencies. On Bukkit the entry point is registered in
  the ServicesManager:
  ```java
  RegionManager regions = Bukkit.getServicesManager().load(RegionManager.class);
  ```
- Key API surface: `RegionManager` (CRUD, `getRegionsAt`, positional & region-scoped
  `resolveFlag`, members/priority/parent/redefine, `reload(UUID)`), `Region`, `Flag<T>`,
  `FlagRegistry` (register custom flags **before** regions load), `RegionShape`/`BoundingBox`/
  `Vector3` (`RegionShape.sampleBorder(spacing)` yields the outline points used by `/rg show`).
- Architecture (LuckPerms model — `api` / `common` / `bukkit`): see `ARCHITECTURE.md`.

## 11. Version history

### 1.0.0 — Unreleased
- Region engine: cuboid/cylinder/sphere/polygon shapes, per-world chunk index, priorities,
  parents, members, per-target flag values, Sarah storage (SQLite/MySQL/MariaDB),
  multi-server-ready schema.
- 35 enforced flags; enter/exit engine with greeting/farewell; two-phase (check/commit) movement
  listeners covering walks, teleports, portals, respawns and world changes.
- Commands: help, pos1/pos2, create, redefine, remove, list, info, flag (multi-word values,
  targets, unset), addmember/removemember, setpriority, setparent, reload.
- Per-language folders with on-demand extraction (en/fr/es/it bundled); localized command
  descriptions; configurable bypass permission and deny-message throttle.
- Translated config.yml templates (en/fr/es/it) — the first boot extracts the one matching the
  server's system locale, presetting `language:` accordingly.
- `/rg show`: per-player particle outline of a region's borders, following the actual shape
  (`RegionShape.sampleBorder`); optional per-command duration (default from config, capped at
  3600 s); configurable particle, duration, refresh, spacing and point cap.
