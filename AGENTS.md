# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

> Workspace context lives in `../AGENTS.md`. **This project is the deliberate exception to almost everything that file says about the ecosystem.** zRegions does **not** use the copy-pasted `zcore`/`ZPlugin` framework, does not extend `ZPlugin`, and does not follow the flat Maven layout. It is architected from scratch as a **LuckPerms-style multi-platform plugin** — read this file, not the ecosystem defaults, before reasoning about zRegions.

## What this is

A land/region **protection** plugin (a WorldGuard competitor) for Minecraft, package `fr.maxlego08.zregions`, sold on SpigotMC. Players/admins define regions with shapes, assign priority/parent/members, and set **flags** that the protection engine resolves per block-position to allow or cancel actions.

The single most important architectural fact: **the goal is to keep ~90% of the code platform-agnostic in `common`**, so `bukkit` is only a thin bridge and future platforms (Fabric/Nukkit) reuse `common` unchanged. This is copied from LuckPerms — the design docs cite LuckPerms classes line-by-line.

## Build, test, run

Gradle (Kotlin DSL), Java **21** toolchain, `com.gradleup.shadow`. From the project root:

```bash
gradlew.bat build                         # Windows — shaded jar → target/zRegions-1.0.0.jar
./gradlew build                           # bash
gradlew.bat test                          # all tests (common module only)
gradlew.bat :common:test --tests "fr.maxlego08.zregions.common.shape.ShapeCodecTest"        # one class
gradlew.bat :common:test --tests "fr.maxlego08.zregions.common.shape.ShapeCodecTest.roundTrips"  # one method
./build-and-deploy.sh                     # build + copy jar to a local dev server (uses JDK at ~/.jdks/ms-21.0.10)
```

- **Output is `target/zRegions-1.0.0.jar`, not `build/libs/`** (shadow `destinationDirectory` is overridden). `build dependsOn shadowJar`, so a plain `build` already produces the distributable jar; the thin `jar` task is disabled.
- `shadowJar` **relocates** `fr.maxlego08.sarah`, `net.kyori`, and `com.google.gson` under `fr.maxlego08.zregions.libs.*`, and folds the `paper` sourceSet output into the final jar.
- `plugin.yml` version is injected via `processResources { expand("version" …) }`.
- **Tests are real and meaningful here** (unlike most of this workspace): JUnit 5 in `common/src/test`, covering shapes, the shape codec, the chunk index, flag resolution, selection, the argument tokenizer, and Sarah storage (with `sqlite-jdbc` as a test-runtime dep). Run them after touching `common`.

## Module layout & the hard rules

Four Gradle modules (`settings.gradle.kts` also auto-includes any `Hooks/<Name>/` that has a `build.gradle.kts` — currently none exist):

| Module | Role | Dependency rule |
|---|---|---|
| `api/` | Public contract (`Region`, `Flag`, `RegionManager`, `RegionShape`, …). Published later to `target-api/` for third-party addons. | **Zero platform deps** — no Bukkit, no Adventure, no zMenu. |
| `common/` | **All the logic**: region engine, spatial index, flags, resolution, storage, commands, config, messages, platform abstractions. | ⚠️ **Never import `org.bukkit.*`, zMenu, or any server API.** A `grep org.bukkit common/` must return nothing. Everything game-related goes through an interface implemented by the platform. |
| `bukkit/` | The game↔plugin bridge only. Produces the final jar. | May use Bukkit/Paper (see sourceSets below). |
| `Hooks/` | Optional integrations (zMenu GUI, CurrenciesAPI, …) via the ecosystem Hook pattern. | Auto-discovered; implement `common` service interfaces. |

## Spigot vs Paper: the two-sourceSet strategy (THE build constraint)

`bukkit` has **two sourceSets**, and this is load-bearing — do not collapse them:

- `bukkit/src/main/java` compiles against **`org.spigotmc:spigot-api:1.20.4`** (the supported floor). Because it compiles against Spigot only, **the compiler mechanically rejects any Paper-only call** → Spigot compatibility is *proven*, not merely intended. This is stricter than LuckPerms (which compiles against Folia-API) and is deliberate because the plugin is sold on SpigotMC.
- `bukkit/src/paper/java` compiles against **`io.papermc.paper:paper-api:1.21.7`** and holds *only* Paper/Folia-specific classes (e.g. `FoliaSchedulerAdapter`). These are **never referenced directly** from `main` — only via `Class.forName` behind a shared interface, so the JVM never links them on a Spigot server (no `NoClassDefFoundError`). `folia-supported: true` in `plugin.yml` is only valid as long as every scheduler access stays behind such an adapter.
- **Messages always go through `adventure-platform-bukkit`** (`BukkitAudiences`), which works identically on Spigot and Paper. **Never call `player.sendMessage(Component)` directly** — that's Paper-only and breaks Spigot.

## The runtime architecture (all in `common` unless noted)

**3-layer lifecycle** (LuckPerms model): `ZRegionsBukkitLoader` (the only `JavaPlugin` in `plugin.yml`, zero logic) → `ZRegionsBukkitBootstrap` (holds native server objects, logger, scheduler, player access) → `ZRegionsBukkitPlugin extends AbstractZRegionsPlugin` (the logic; fills the platform hooks).
- `AbstractZRegionsPlugin.load()/enable()/disable()` are **`final`** and fix the startup order; platforms only implement named abstract hooks (`setupSenderFactory`, `provideConfigurationAdapter`, `registerCommands`, …). To change startup order, edit the `final` methods in `common`, not the Bukkit subclass.
- ⚠️ The **jar-in-jar loader is not adopted yet** — `ZRegionsBukkitLoader` currently does `new ZRegionsBukkitBootstrap(this)` directly (the documented "phase 0" simplification). `ARCHITECTURE.md` describes the eventual `JarInJarClassLoader` + `bukkit:loader` module; it does not exist in code.

**Platform abstractions** (`common` interface + factory, `bukkit` impl): commands reason in **`RegionSender`** (player *or* console); the protection engine reasons in **`RegionPlayer`** (has world + `RegionLocation`, plus `spawnBorderParticle` for the per-player `/rg show` outline driven by `common/visual/BorderDisplayManager` + `RegionShape.sampleBorder`). `common` never touches `CommandSender`/`Player` — the Bukkit factories wrap the native objects.

**Region engine** — `ZRegionManager` (`common`):
- In-memory caches: `byId`, `byName` (per world), a per-world `ChunkRegionIndex`, and `globalByWorld`. **Queries never hit the database.** Mutations take a single `ReentrantLock` write lock, update the cache, then persist **async** via the scheduler.
- `ChunkRegionIndex` is the performance core ("anti-UltraRegions"): a lookup only tests the few regions overlapping one chunk. Regions over `LARGE_THRESHOLD_CHUNKS` (1024) are held out and scanned linearly; queries are lock-free reads over a `volatile` snapshot; **global (world-wide) regions are not indexed** — the manager applies them as a fallback.
- **Flag resolution** (`resolveFlag`): regions at the point, highest priority first → `lookupWithParents` (most-specific `GroupTarget` → `ALL` → parent chain, bounded to `MAX_PARENT_HOPS`=10 to survive cycles) → world-global region → the flag's default value.
- `reload(UUID)` is **the single multi-server anchor point**: it reloads one region from storage and swaps it in the cache/index (async read, sync index swap). Cross-server messaging (v2, not built) will call only this.

**Shapes**: `CuboidShape`, `CylinderShape`, `SphereShape`, `PolygonShape` (`api` `RegionShape` + `common` impls). `ShapeCodec` serializes shape geometry to/from JSON (Gson) for the `shape_data` column.

**Protection**: the `bukkit/listener/` package is the only place native events are handled (`ProtectionListener` for player block/entity actions, `EnvironmentProtectionListener` for playerless events, `PlayerStateListener` for player-condition flags, `MovementListener` for enter/exit with a NORMAL-check/MONITOR-commit two-phase pattern). Listeners translate events into `resolveFlag` calls, cancel on deny, throttle denied messages (configurable, `messages.deny-throttle-milliseconds`), and honor the **configurable** bypass node (`permissions.bypass`, default `zregions.bypass`). All decisions live in `common`.

**Storage**: Sarah ORM (shaded+relocated). `RegionStorage` interface → `SarahRegionStorage`, with `RegionDTO`/`MemberDTO`/`FlagDTO` and migrations `CreateRegionsTable`/`CreateMembersTable`/`CreateFlagsTable`. SQLite by default; MySQL/MariaDB supported. Table prefix `zregions_`. **Route all persistence through `RegionStorage`, never raw SQL.**

**Multi-server readiness (data model only, v1)**: the `regions` table carries `origin_server` and a `version` (optimistic-lock) column from day one, and `ZRegionManager.loadAllBlocking()` filters by `getServerName()` (config `multi-server.server`, default `global`). The messaging/Redis layer is **v2 and not implemented** — but the schema and the `reload()` seam already exist so it can be added without a rewrite. See `ARCHITECTURE.md §14`.

**Commands**: `RegionCommandManager` + `RegionCommand` abstraction + the concrete commands (`CreateCommand`, `FlagCommand`, `InfoCommand`, `ListCommand`, `Pos1/Pos2Command`, `ReloadCommand`, `RemoveCommand`, `HelpCommand`) all live in `common`; `BukkitCommandExecutor` just converts `CommandSender`/`String[]`. Registered as `/region` (alias `/rg`) and `/zregions`. `TabCompleter`, `ArgumentList`, and `ArgumentTokenizer` are copied from LuckPerms.

**Messages/i18n** (zAuctionHouse model): a root **`language.yml`** (bundled at `bukkit/src/main/resources/language.yml`, extracted to `plugins/zRegions/language.yml`, never translated) is loaded **first** by `AbstractZRegionsPlugin.resolveConfiguredLanguage()` — `language: auto` (default) resolves the JVM locale, unknown codes warn and fall back to English. It picks the language of every **default file** extracted on first use by `ensureDefaultFile`: `config.yml` (translated comments), **`messages.yml` at the data-folder root** (no per-language folders on disk), and the zMenu inventories (`plugins/zRegions/inventories/`). The jar keeps the translations as sources: `bukkit/src/main/resources/languages/<lang>/{config.yml,messages.yml}` and `Hooks/zMenu/src/main/resources/languages/<lang>/inventories/*.yml` (en/fr/es/it; keys/structure identical everywhere, only display strings translated). Existing files are never overwritten — regenerating = delete the file + `/rg reload` (which re-reads language.yml and re-extracts missing defaults). Missing message keys fall back to the English defaults hardcoded in the `Message` enum. Therefore: adding a `Message` entry means enum default + **all four** `languages/*/messages.yml`; adding a config key means **all four** `languages/*/config.yml`; changing a GUI menu means **all four** `languages/*/inventories/` copies. Command descriptions are localized under `commands.descriptions.<name>`. Messages are Adventure/MiniMessage; inventories use zMenu's `&`/hex codes.

## Design docs & attribution

`ARCHITECTURE.md` (the multi-platform blueprint, in French, citing LuckPerms line numbers), `PLAN-IMPLEMENTATION.md`, and `AUDIT-MARCHE.md` (market analysis) are the reference documents — consult `ARCHITECTURE.md` before large structural changes. Note they describe the **target** design, which is ahead of the code in two places: the **jar-in-jar loader** and the **cross-server messaging layer** are documented but not yet built.

Infrastructure classes copied from **LuckPerms are MIT-licensed** — keep the MIT header and attribution when copying more of them (scheduler, sender/factory, command framework, tab-complete utilities are all marked copyable in `ARCHITECTURE.md §4`).

## Conventions

- **`DOCUMENTATION.md` is mandatory and MUST be updated in the same change as any feature work.** It is the product reference (commands, permissions, flags, config keys, languages, API) that keeps the plugin release-ready. Concretely: adding/changing a command, flag, permission node, config key, message, language file or public API **requires** the matching edit in `DOCUMENTATION.md` (including its `Version history → Unreleased` section). A feature absent from that file is considered unfinished.
- French is the original developer language; comments and design docs are French, code identifiers are English. Match the surrounding style.
- Keep the `common` purity rule above all else — if a change in `common` seems to need Bukkit, the correct move is a new abstraction interface implemented in `bukkit`.
- Hot-path settings (bypass permission node `permissions.bypass`, deny-message throttle `messages.deny-throttle-milliseconds`) are **cached in `ZRegionsConfiguration`** and refreshed on `reload()` — never read them through the adapter per event, and route new hot-path config values through the same cached-field pattern.
