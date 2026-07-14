# zRegions — Plan d'implémentation & structure du projet

> Document technique de cadrage. Compagnon de [`AUDIT-MARCHE.md`](./AUDIT-MARCHE.md) (le *quoi/pourquoi*) et de [`ARCHITECTURE.md`](./ARCHITECTURE.md) (**le blueprint détaillé de la couche d'abstraction multi-plateforme, calqué sur le code réel de LuckPerms**). Ce plan fixe le *comment* d'ensemble ; ARCHITECTURE.md détaille les interfaces `RegionSender`/`RegionPlayer`/bootstrap/scheduler/commandes.
> Cible : plugin **premium** de régions admin GUI-first (créneau UltraRegions), extensible vers un module claims joueurs en v2.
> Décisions calquées sur les projets existants du workspace : **LuckPerms** (modèle multi-plateforme `common` + bootstraps — analysé en profondeur, voir ARCHITECTURE.md), **zMenu** (dossier `Hooks/` optionnels), **DialogWarps** (stack Sarah + dialogs + Folia), **zKothV3** (primitive `Cuboid` + module `API` publié), **Sarah** (ORM), **CurrenciesAPI** (économie unifiée), **UltraRegionAddon** (data-model UltraRegions pour l'importateur).

> **⚠️ Cette v3 du plan intègre les changements d'architecture demandés :**
> 1. **Module `common` platform-agnostic** → viser à terme les serveurs vanilla (Fabric/Forge) et Bedrock (Nukkit/Geyser), comme LuckPerms. **Un maximum de logique dans `common`** ; les modules plateforme ne font que le pont jeu↔plugin via des abstractions (`RegionSender`, `RegionPlayer`, bootstrap…) — détaillées dans [`ARCHITECTURE.md`](./ARCHITECTURE.md).
> 2. **Ciblage Spigot** : le module `bukkit` compile son sourceSet `main` contre **spigot-api** (compat Spigot *garantie par le compilateur*, car le plugin est vendu sur SpigotMC), avec un sourceSet `paper` isolé pour exploiter Paper/Folia quand présents. **Jamais l'« API Bukkit » nue ni Paper en dépendance de `main`.**
> 3. **Régions de formes variées** : carré, rectangle, cube, cercle, sphère, étoile/polygone — pas seulement cuboïde.
> 4. **zMenu optionnel** (dans `Hooks/`) : le plugin fonctionne **100 % en commandes** ; zMenu ajoute le support des inventaires.
> 5. **Économie = CurrenciesAPI uniquement** (shadée + relocalisée) : elle gère déjà Vault + 15 autres. Pas de hook Vault séparé.
> 6. **Multi-serveur prévu en amont** : modèle de données cross-server dès la v1 (MySQL, `origin_server`, `version`), couche messaging (`RegionMessenger` Redis/plugin-message) en v2 — calqué sur LuckPerms + le code maison zAuctionHouse Redis. Voir [`ARCHITECTURE.md` §14](./ARCHITECTURE.md) et [`AUDIT-MARCHE.md` §10](./AUDIT-MARCHE.md).

---

## 1. Décisions d'architecture (résumé)

| Décision | Choix | Justification |
|---|---|---|
| **Architecture** | **Multi-plateforme en 3 couches façon LuckPerms** : `api` + `common` (toute la logique) + `bukkit`/`bukkit:loader` (le pont). Détail complet → [`ARCHITECTURE.md`](./ARCHITECTURE.md) | Objectif produit : régions sur serveurs vanilla (Fabric/Forge) et Bedrock, comme LuckPerms. `common` n'importe **jamais** `org.bukkit.*` ni zMenu. |
| Framework de base | Loader `JavaPlugin` mince (jar-in-jar) → Bootstrap plateforme → `AbstractZRegionsPlugin` (common). **PAS** le vieux `zcore/ZPlugin` | Modèle LuckPerms (3 couches, `LoaderBootstrap` 📋 MIT copiable). zcore est copié-collé, dérivé, Bukkit-only. |
| **Ciblage API** | `bukkit/src/main` compile contre **`spigot-api`** ; `bukkit/src/paper` (sourceSet isolé) contre **paper/folia-api**, instancié seulement après `Class.forName` | Compat Spigot **garantie par le compilateur** (plugin vendu sur SpigotMC). LuckPerms compile contre Paper et isole ; on va plus loin pour la garantie Spigot. |
| **Abstraction joueur** | `RegionSender` (source de commande : joueur/console) **+** `RegionPlayer` (joueur en ligne + **position/monde**) via factories `<P,T>` | Copie du `Sender`/`SenderFactory` de LuckPerms, enrichie de la position (que LuckPerms n'a pas mais que zRegions exige). Détail → ARCHITECTURE.md §5-6. |
| **Formes de région** | Abstraction `RegionShape` : **Cuboid** (carré/rectangle/cube), **Cylinder** (cercle/disque), **Sphere**, **Polygon** (étoile/polygone) | Demande produit. Différenciateur : WorldGuard ne fait que cuboïde + polygone ; UltraRegions que cuboïde. |
| **GUI zMenu** | **Optionnel**, dans `Hooks/zMenu/`. Le plugin marche en **commandes seules** ; zMenu ajoute les inventaires | Cohérent avec le multi-plateforme (zMenu est Bukkit-only). Dégradation propre en commandes. Le GUI reste le différenciateur *là où zMenu est installé*. |
| **Économie** | **CurrenciesAPI seule**, shadée + relocalisée (`libs.currencies`) | Gère déjà Vault + 15 économies via un `CurrencyProvider` unique. Pas de dépendance Vault directe. |
| Persistance | **Sarah** (SQLite / MySQL / MariaDB) — dans `common` | Multi-serveur (v2), robustesse. JDBC est platform-agnostic. |
| **Multi-serveur** | **Prévu dès la v1 dans le modèle de données** (MySQL, colonne `origin_server`, `version`, cache invalidable par région) ; **couche messaging livrée en v2** (`RegionMessenger` Redis/plugin-message dans `common`, style LuckPerms + zAuctionHouse). Détail → [`ARCHITECTURE.md` §14](./ARCHITECTURE.md), marché → [`AUDIT-MARCHE.md` §10](./AUDIT-MARCHE.md) | Une région est liée à un `(serveur, monde, coord)` : le multi-serveur = gestion admin centralisée + cohérence de cache + (v2) claim-blocks réseau-wide, PAS la réplication d'un monde. Concevoir le modèle maintenant évite la réécriture. |
| Moteur spatial | `RegionShape` + **index par chunk** (bounding box) — dans `common` | zKoth itère linéairement (~10 zones OK) ; inacceptable pour des centaines de régions. L'index est l'anti-UltraRegions (audit §4.2). |
| Hooks | Dossier **`Hooks/`** auto-inclus (settings.gradle.kts), chargés par réflexion = soft-deps | Exactement le modèle zMenu (25+ hooks). Rend zMenu/CurrenciesAPI/LuckPerms/WorldEdit optionnels et isolés. |
| Commandes | **`RegionCommandManager` dans `common`** (copié de LuckPerms 📋) ; binding par plateforme (`BukkitCommandExecutor`, + Brigadier/async dans le sourceSet `paper`) | La logique de commande est écrite une fois dans `common` et bindée par chaque plateforme. Détail → ARCHITECTURE.md §11. |
| Texte | **Adventure** (`Component` + MiniMessage) dans `common` ; envoi via `adventure-platform-bukkit` (jamais l'Adventure natif Paper) | Standard cross-plateforme. Ne dépend pas de zMenu. Marche identiquement Spigot/Paper. Répond à WorldGuard #2287. |
| Dépendances | Loader jar-in-jar + **DependencyManager** (télécharge/relocalise/checksum au runtime, 📋 LuckPerms) pour les grosses libs ; Sarah/CurrenciesAPI shadés | Évite les conflits de libs avec d'autres plugins vendus ; portable Spigot (le `plugin.yml > libraries` de Paper n'existe pas sur Spigot). Détail → ARCHITECTURE.md §9. |
| Java | **21** (release 21) | MC 1.20.5→1.21.x = Java 21 ; Fabric 1.21 = Java 21. `api`/`common` compilables en 17 si un futur bootstrap l'exige. |
| Compat | Spigot/Paper/Purpur **1.20→dernière**, **Folia**, Bedrock via **Geyser** dès v1 ; Fabric/Forge/Nukkit = plateformes futures | Audit §6 + objectif multi-plateforme. Plancher spigot-api = 1.20.4. |
| Prix | ~**12–15 €** (SpigotMC + Polymart + BuiltByBit) | Audit §9.5. |

---

## 2. ⭐ Architecture multi-plateforme (le modèle LuckPerms)

> **Le détail complet — interfaces, signatures, mapping fichier-par-fichier vers LuckPerms — est dans [`ARCHITECTURE.md`](./ARCHITECTURE.md).** Ci-dessous le résumé.

Principe : **toute la logique métier vit dans `common`, sans une seule dépendance Bukkit ni zMenu.** L'analyse du code réel de LuckPerms a révélé un modèle en **3 couches** (pas 2) :

```
        ┌──────────────────────────────────────────────────┐
        │  api/      interfaces publiques, 0 dépendance     │  ← addons tiers, autres plugins
        └──────────────────────────────────────────────────┘
                          ▲
        ┌──────────────────────────────────────────────────┐
        │  common/   TOUTE la logique, platform-agnostic    │
        │  régions+formes · index spatial · flags · Sarah   │
        │  · RegionCommandManager · config/Adventure        │
        │  · RegionSender/RegionPlayer/bootstrap (abstrait)  │
        └──────────────────────────────────────────────────┘
                          ▲                          ▲
     ┌────────────────────┴──────┐        (futur : fabric/ nukkit/ …)
     │  bukkit/   le pont jeu↔plugin        réutilisent common tel quel
     │  src/main → spigot-api  (compat Spigot garantie)
     │  src/paper → paper/folia-api (classes isolées, Class.forName)
     └────────────────────┬──────┘
                          ▲
     ┌────────────────────┴──────┐   ┌───────────────────────────────┐
     │ bukkit:loader             │   │ Hooks/ (optionnels, Bukkit)   │
     │ JavaPlugin du plugin.yml  │   │ zMenu · CurrenciesAPI ·       │
     │ (jar-in-jar → bootstrap)  │   │ LuckPerms · WorldEdit · PAPI  │
     └───────────────────────────┘   └───────────────────────────────┘
```

**Les 3 couches** (détail ARCHITECTURE.md §2) : `bukkit:loader` (le seul `JavaPlugin`, monte un jar-in-jar, zéro logique) → `bukkit` Bootstrap (pont : primitives serveur, accès joueurs, latches, cycle de vie) → `AbstractZRegionsPlugin` dans `common` (logique + `enable()` `final` + hooks abstraits).

**Les abstractions que `common` définit** (interfaces ; impl. Bukkit ; détail ARCHITECTURE.md §4-11) : `ZRegionsBootstrap` (accès joueurs/serveur), `RegionSender` + `RegionSenderFactory<P,T>` (source de commande joueur/console), `RegionPlayer` + `RegionPlayerFactory<T>` (joueur en ligne **+ position/monde**), `SchedulerAdapter` (+`JavaSchedulerAdapter` en common), `PluginLogger`, `ClassPathAppender`, `AbstractConnectionListener` (join/quit → charge/décharge), `RegionCommandManager` (+ hiérarchie `RegionCommand`), `ConfigurationAdapter`, `AbstractEventBus`, et les services optionnels `GuiService` (zMenu) / `EconomyService` (CurrenciesAPI).

⚠️ **Le hot path (check de protection, résolution de flag) est du code `common` pur.** Les plateformes ne font que capter des événements natifs et poser des questions au `RegionManager`. C'est ce qui garantit portabilité *et* performance.

**Cibles Bedrock (deux voies)** : (1) *v1* — joueurs Bedrock via **Geyser/Floodgate** sur serveur Bukkit (marche déjà) ; (2) *futur* — bootstrap **`nukkit`** pour Bedrock natif, rendu possible car `common` est platform-agnostic.

---

## 3. Stack technique & coordonnées exactes

Repositories : `mavenCentral`, `repo.papermc.io`, `repo.groupez.dev/releases`, `jitpack.io`, `repo.extendedclip.com` (PAPI), `repo.tcoded.com` (FoliaLib).

| Dépendance | Version | Module / Scope | Rôle |
|---|---|---|---|
| `fr.maxlego08.sarah:sarah` | `1.23` | `common` — `implementation` (**shadé + relocaté**) | ORM |
| `net.kyori:adventure-api` + `adventure-text-minimessage` | (BOM récent) | `common` — `implementation` | Texte cross-plateforme (MiniMessage) |
| `com.github.cryptomorin:XSeries` | `9.4.0` | `bukkit` — `implementation` (relocaté) | Materials cross-version |
| `net.kyori:adventure-platform-bukkit` | `4.4.0` | `bukkit` — `compileOnly` (téléchargé runtime) | Envoi Adventure (Spigot **et** Paper) |
| **`org.spigotmc:spigot-api`** | `1.20.4-R0.1-SNAPSHOT` | `bukkit` **sourceSet `main`** — `compileOnly` | Plateforme, plancher **compat Spigot garantie** |
| `dev.folia:folia-api` | `1.21.7-R0.1-SNAPSHOT` | `bukkit` **sourceSet `paper`** — `compileOnly` | Classes Paper/Folia isolées (scheduler, tab-complete async) |
| `fr.maxlego08.menu:zmenu-api` | `1.1.1.5` | `Hooks/zMenu` — `compileOnly` | GUI (optionnel) |
| `fr.traqueur.currencies:currenciesapi` | `1.0.13` | `Hooks/CurrenciesAPI` — `implementation` (**shadé + relocaté**) | Économie (Vault + 15 autres) |
| `me.clip:placeholderapi` | `2.11.6` | `Hooks/PlaceholderAPI` — `compileOnly` | Placeholders + conditions |
| Drivers JDBC | — | **DependencyManager runtime** (📋 LuckPerms) ou shadés | `sqlite-jdbc` par défaut ; mysql/mariadb à la demande, relocalisés + checksum |
| JUnit 5 (`junit-bom`) | `5.11.4` | tous — `test*` | Tests (le gros dans `common`, sans serveur) |

> Note : **plus de dépendance `paper-api` sur le sourceSet `main`** (contrairement à la v2 du plan). Le `main` ne voit que spigot-api ; Paper/Folia est confiné au sourceSet `paper` instancié après `Class.forName`. Voir ARCHITECTURE.md §3.

**Relocations shadow** (dans le shadowJar de la plateforme `bukkit`) :
```kotlin
relocate("fr.maxlego08.sarah",    "fr.maxlego08.zregions.libs.sarah")
relocate("fr.traqueur.currencies", "fr.maxlego08.zregions.libs.currencies")
relocate("net.kyori.adventure",   "fr.maxlego08.zregions.libs.adventure")
relocate("com.cryptomorin.xseries","fr.maxlego08.zregions.libs.xseries")
// adventure : shadé seulement pour la compat Spigot (Paper le fournit nativement)
```
Références workspace : `zShopV3` relocate `fr.traqueur.currencies` → `libs.currencies` ; DialogWarps relocate Sarah → `libs.sarah`.

---

## 4. Structure du projet (multi-module Gradle)

```
zRegions/
├── settings.gradle.kts        # include api, common, bukkit + Hooks auto-inclus
├── build.gradle.kts           # root : allprojects { java 21, repos }
├── gradlew.bat / gradlew
├── AUDIT-MARCHE.md · PLAN-IMPLEMENTATION.md
│
├── api/                        # publié → target-api/ (re.alwyn974.groupez.publish). 0 dépendance plateforme.
│   └── src/main/java/fr/maxlego08/zregions/api/
│       ├── ZRegions.java              # façade API
│       ├── region/    Region, RegionType
│       ├── shape/     RegionShape, ShapeType, BoundingBox, Vector3      ← §7
│       ├── flag/      Flag, FlagValue, FlagRegistry, FlagState, GroupTarget
│       ├── member/    RegionMember, MemberRole
│       ├── manager/   RegionManager, FlagManager
│       └── event/     RegionEnterEvent, RegionExitEvent, RegionCreateEvent, RegionDeleteEvent, RegionFlagChangeEvent
│
├── common/                     # ⭐ toute la logique, platform-agnostic (0 import org.bukkit)
│   └── src/main/java/fr/maxlego08/zregions/common/
│       ├── plugin/     ZRegionsPlugin (interface), AbstractZRegionsPlugin (enable() final + hooks)  ← ARCHI §8
│       │               bootstrap/ZRegionsBootstrap · scheduler/{SchedulerAdapter,JavaSchedulerAdapter}
│       │               logging/PluginLogger · classpath/ClassPathAppender · util/AbstractConnectionListener
│       ├── sender/     RegionSender, AbstractRegionSender, RegionSenderFactory, DummyConsoleSender  ← ARCHI §5
│       ├── player/     RegionPlayer, AbstractRegionPlayer, RegionPlayerFactory, RegionLocation      ← ARCHI §6
│       ├── command/    RegionCommandManager, abstraction/{RegionCommand,Single,Parent},
│       │               utils/{ArgumentList,ArgumentTokenizer}, tabcomplete/*   (📋 LuckPerms)       ← ARCHI §11
│       ├── region/     ZRegion, ZRegionManager (CRUD + cache + index)
│       ├── shape/      CuboidShape, CylinderShape, SphereShape, PolygonShape        ← §7
│       ├── spatial/    ChunkRegionIndex                                             ← §8
│       ├── flag/       ZFlagManager, flags/*, value/*                               ← §11
│       ├── protection/ ProtectionQuery  (logique pure : "cette action est-elle permise ?")
│       ├── storage/    RegionRepository, dto/*, migrations/*  (Sarah)               ← §12
│       ├── importer/   WorldGuardImporter, UltraRegionsImporter, RedProtectImporter (interfaces)  ← §15
│       ├── config/     ZRegionsConfiguration, MessageService (Adventure/MiniMessage), ConfigurationAdapter
│       └── selection/  SelectionManager (par forme)                                 ← §7
│
├── common/loader-utils/        # 📋 JarInJarClassLoader + LoaderBootstrap (MIT, copiés de LuckPerms)
│
├── bukkit/                     # le pont — SHIP v1
│   ├── build.gradle.kts        # 2 sourceSets, dépend de common + Hooks
│   └── src/
│       ├── main/java/fr/maxlego08/zregions/bukkit/    → compile contre spigot-api
│       │   ├── ZRegionsBukkitBootstrap.java  (ZRegionsBootstrap : primitives serveur, latches)
│       │   ├── ZRegionsBukkitPlugin.java     (AbstractZRegionsPlugin : remplit les hooks)
│       │   ├── BukkitSenderFactory / BukkitPlayerFactory / BukkitSchedulerAdapter / JavaPluginLogger
│       │   ├── command/  BukkitCommandExecutor (bind RegionCommandManager)   ← ARCHI §11
│       │   ├── listener/ BukkitConnectionListener, MovementListener, ProtectionListener   ← §10
│       │   └── event/    BukkitEventDispatcher (pont events Bukkit pour l'interop GroupeZ)
│       ├── paper/java/…/paper/     → compile contre paper/folia-api (isolé, Class.forName)
│       │   ├── FoliaSchedulerAdapter · BukkitAsyncCommandExecutor (tab-complete Paper)
│       └── resources/{plugin.yml*, language.yml, {en,fr,es,it}/…}
│           (*plugin.yml décrit le loader, pas ce module — voir ci-dessous)
│
├── bukkit/loader/              # ZRegionsLoaderPlugin (le JavaPlugin du plugin.yml) + zregions-bukkit.jarinjar
│
├── Hooks/                      # auto-inclus (settings.gradle.kts), Bukkit-side, optionnels — §16
│   ├── zMenu/  CurrenciesAPI/  LuckPerms/  WorldEdit/  PlaceholderAPI/
│   │   (v2) BlueMap/ Pl3xMap/ Dynmap/ EssentialsX/ zEssentials/ zAuctionHouse/
│
└── (futur) fabric/  nukkit/    # nouveaux ponts, réutilisent common tel quel
```

**Pourquoi `api` séparé** : publié sur `repo.groupez.dev` (jar `target-api/`), il permet aux tiers d'écrire des addons (flags, formes, hooks) sans le plugin complet — le modèle d'écosystème qui manque à HuskClaims (audit §3.4).
**Pourquoi `loader-utils` + `bukkit/loader`** : le jar-in-jar isole les libs relocatées et permet le téléchargement de dépendances au runtime (ARCHITECTURE.md §2, §9). Le jar final `target/zRegions.jar` est produit par `bukkit:loader` (il contient `zregions-bukkit.jarinjar`, lui-même produit par `bukkit`).

---

## 5. Fichiers de build (concrets)

### `settings.gradle.kts`
```kotlin
rootProject.name = "zRegions"
include("api", "common", "common:loader-utils", "bukkit", "bukkit:loader")
// Hooks auto-inclus (modèle zMenu)
file("Hooks").listFiles()?.filter { it.isDirectory && it.name != "build" }
    ?.forEach { include(":Hooks:${it.name}") }
```

### `build.gradle.kts` (root)
```kotlin
plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0" apply false
    id("re.alwyn974.groupez.repository") version "1.0.0"
}
allprojects {
    apply(plugin = "java-library")
    group = "fr.maxlego08.zregions"; version = "1.0.0"
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.groupez.dev/releases")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.tcoded.com/releases")
    }
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
    tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release.set(21) }
}
```

### `common/build.gradle.kts`
```kotlin
dependencies {
    api(project(":api"))
    implementation("fr.maxlego08.sarah:sarah:1.23")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.github.cryptomorin:XSeries:9.4.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

### `bukkit/build.gradle.kts` (2 sourceSets : main→spigot-api, paper→paper/folia-api)
```kotlin
plugins { id("com.gradleup.shadow") version "9.0.0" }
val paper by sourceSets.creating
dependencies {
    implementation(project(":common"))
    compileOnly(project(":common:loader-utils"))
    // sourceSet main : SPIGOT uniquement → compat Spigot garantie par le compilateur
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.0")
    // sourceSet paper : Paper/Folia, isolé
    "paperCompileOnly"("dev.folia:folia-api:1.21.7-R0.1-SNAPSHOT")
    "paperImplementation"(sourceSets["main"].output)   // paper voit les interfaces de main
    file("../Hooks").listFiles()?.filter { it.isDirectory && it.name != "build" }
        ?.forEach { implementation(project(":Hooks:${it.name}")) }
}
tasks.shadowJar {
    archiveFileName.set("zregions-bukkit.jarinjar")     // jar interne, embarqué par bukkit:loader
    from(paper.output)                                   // assemble main + paper + common
    relocate("fr.maxlego08.sarah",     "fr.maxlego08.zregions.libs.sarah")
    relocate("fr.traqueur.currencies", "fr.maxlego08.zregions.libs.currencies")
    relocate("net.kyori.adventure",    "fr.maxlego08.zregions.libs.adventure")
    relocate("com.cryptomorin.xseries","fr.maxlego08.zregions.libs.xseries")
}
```

### `bukkit/loader/build.gradle.kts` (produit le jar final `target/zRegions.jar`)
```kotlin
plugins { id("com.gradleup.shadow") version "9.0.0" }
dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")   // juste JavaPlugin
    implementation(project(":common:loader-utils"))
}
tasks {
    jar { enabled = false }
    processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
    shadowJar {
        archiveBaseName.set("zRegions"); archiveClassifier.set("")
        destinationDirectory.set(rootProject.file("target"))
        from(project(":bukkit").tasks.shadowJar.get().archiveFile)   // embarque le .jarinjar
    }
    build { dependsOn(shadowJar) }
}
```

### `bukkit/loader/src/main/resources/plugin.yml` — **main = le loader ; zMenu softdepend, pas depend**
```yaml
name: zRegions
version: '${version}'
main: fr.maxlego08.zregions.bukkit.loader.ZRegionsLoaderPlugin   # ← le LOADER (jar-in-jar), pas le plugin
api-version: '1.20'
authors: [ Maxlego08 ]
website: https://groupez.dev
# AUCUN depend : le plugin fonctionne seul, en commandes.
softdepend: [ zMenu, PlaceholderAPI, WorldEdit, LuckPerms, WorldGuard, UltraRegions, RedProtect ]
folia-supported: true
permissions:
  zregions.use:    { default: true }
  zregions.admin:  { default: op }
  zregions.bypass: { default: op }
# PAS de `libraries:` — les drivers JDBC passent par le DependencyManager runtime (ARCHITECTURE.md §9),
# téléchargés + relocalisés + vérifiés par checksum, portable Spigot (contrairement au library-loader Paper).
```
- Le `main` pointe sur le **loader** (`ZRegionsLoaderPlugin`), qui monte le jar-in-jar et instancie `ZRegionsBukkitBootstrap`.
- **AUCUN `depend`** : le plugin fonctionne 100 % en commandes ; zMenu (GUI) et les autres sont des soft-deps optionnels.
- CurrenciesAPI n'est **pas** en softdepend : shadée dans le jar, pas chargée depuis le serveur.

---

## 6. Modèle de domaine & API publique

```java
// api/region/Region.java
public interface Region {
    UUID   getId();
    String getName();
    String getWorldName();
    RegionShape getShape();                       // ← forme quelconque (§7)
    int    getPriority();                          // la plus haute gagne (WorldGuard-like)
    Optional<Region> getParent();                  // héritage de flags
    boolean isGlobal();
    Set<RegionMember> getMembers();
    boolean hasRole(UUID player, MemberRole role);
    <T> FlagValue<T> getFlag(Flag<T> flag, GroupTarget target);   // brut
    <T> T           resolveFlag(Flag<T> flag, UUID who);          // effectif (priorité+parent+global+défaut)
}

// api/manager/RegionManager.java — service central, exposé par common, requêté par les plateformes
public interface RegionManager {
    Region create(String world, String name, RegionShape shape, int priority);
    void   delete(Region region);
    Optional<Region> getByName(String world, String name);
    List<Region>     getRegionsAt(String world, double x, double y, double z);   // HOT PATH (§8)
    <T> T queryFlag(String world, double x, double y, double z, Flag<T> flag, UUID who);
}
```
Types de valeur (`Vector3`, `BoundingBox`) sont des value objects `double`/`int` **sans Bukkit** — c'est ce qui rend `api` portable.

Événements zRegions : émis via `EventDispatcher` (bus interne, disponible sur toute plateforme) ; sur Bukkit, `BukkitEventDispatcher` les re-publie aussi comme events Bukkit pour l'interop avec les autres plugins GroupeZ.

---

## 7. ⭐ Formes de régions (carré, rectangle, cube, cercle, sphère, étoile)

Abstraction centrale — toutes les formes implémentent la même interface, et **le moteur spatial les traite de façon uniforme via leur bounding box** :

```java
// api/shape/RegionShape.java
public interface RegionShape {
    ShapeType getType();                          // CUBOID, CYLINDER, SPHERE, POLYGON
    boolean   contains(double x, double y, double z);   // test précis (délégué par le moteur)
    BoundingBox getBoundingBox();                 // AABB min/max → pilote l'index chunk (§8)
    Map<String,Object> serialize();               // → shape_data JSON (§12)
}
```

| Demande utilisateur | Forme | Paramètres | `contains` |
|---|---|---|---|
| **Carré / Rectangle** (2D, toute hauteur) | `CuboidShape` | min/max x,z ; minY/maxY (ou monde entier) | comparaison de bornes |
| **Cube** | `CuboidShape` | min/max x,y,z | comparaison de bornes |
| **Cercle / Disque** (extrudé verticalement) | `CylinderShape` | centre x,z ; rayon r ; minY,maxY | `dx²+dz² ≤ r²` **et** `minY≤y≤maxY` |
| **Sphère** | `SphereShape` | centre x,y,z ; rayon r | `dx²+dy²+dz² ≤ r²` |
| **Étoile / Polygone** (extrudé = prisme) | `PolygonShape` | sommets `[(x,z)…]` ; minY,maxY | point-in-polygon (ray casting) **et** Y range |

- Une **étoile** est un polygone non-convexe : `PolygonShape` la couvre nativement. Un générateur d'étoile (centre + N branches + rayon interne/externe) produit simplement la liste de sommets.
- `CuboidShape` réutilise directement le `Cuboid` de zKoth (`[Spigot] zKothV3/API/.../utils/Cuboid.java`) — coins min/max en int, `contains(Location)` vérifiant le monde, `getChunks()`. On le généralise en implémentant `RegionShape`.
- **Toutes les formes sont bon marché à tester** (distance au carré, ray casting) — et surtout, le test précis ne tourne **que** sur les rares régions candidates du chunk (§8), jamais sur toutes les régions.

**Sélection par forme** (`SelectionManager`, wand + WorldEdit) :
- Cuboïde → 2 points (wand clic gauche/droit, style WorldEdit).
- Cylindre/Sphère → 1 point centre + rayon (commande ou molette).
- Polygone/Étoile → N clics pour poser les sommets + hauteur, ou générateur d'étoile paramétré.

Différenciateur produit : WorldGuard = cuboïde + polygone 2.5D uniquement ; UltraRegions = cuboïde. zRegions offrant cercles/sphères/étoiles est un argument marketing concret et rare.

---

## 8. Moteur spatial — index par chunk (shape-agnostic)

Inchangé sur le principe (audit §4.2 : la perf a tué UltraRegions), mais **piloté par la bounding box** de chaque forme, donc indépendant de la forme :

```java
// common/spatial/ChunkRegionIndex.java  (un index par monde)
private final Map<Long, List<Region>> byChunk = new HashMap<>();   // clé = (chunkX<<32)|(chunkZ&0xFFFFFFFFL)
private final List<Region> largeRegions = new ArrayList<>();       // régions énormes hors index
private static final int LARGE_THRESHOLD_CHUNKS = 1024;

void add(Region r) {
    BoundingBox bb = r.getShape().getBoundingBox();                // ← marche pour cuboïde/sphère/étoile
    long chunks = bb.chunkCount();
    if (chunks > LARGE_THRESHOLD_CHUNKS) { largeRegions.add(r); return; }
    for (long key : bb.chunkKeys()) byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
}
List<Region> query(int bx, int by, int bz) {                       // HOT PATH
    List<Region> cand = byChunk.getOrDefault(chunkKey(bx>>4, bz>>4), List.of());
    // union avec largeRegions, filtre par shape.contains(bx,by,bz), tri par priorité desc
}
```

Règles de perf non négociables (l'argument de vente) :
1. **Tout en mémoire.** Un check de flag ne touche jamais la DB. La DB = chargement au boot + écritures async.
2. **Index par chunk** : un `BlockBreakEvent` au spawn ne teste que les régions du chunk concerné → jamais « 20 % des threads pour protéger le spawn ».
3. **Grosses régions séparées** (`largeRegions`) : une région couvrant tout un monde ne pollue pas des milliers de buckets.
4. **Cache par joueur** pour l'enter/exit (§9).
5. **Résolution** (§11) : priorité desc → parent → région globale du monde → défaut du flag.

`HashMap<Long,…>` d'abord ; migrer vers `fastutil Long2ObjectOpenHashMap` (à relocater) seulement si le profiling le justifie.

---

## 9. Détection entrée/sortie de région (Folia-safe)

Amélioration du modèle zKoth (throttle temporel) → recalcul **au franchissement de bloc** seulement :
```java
// bukkit/listener/MovementListener.java  → délègue à common
onMove(from, to):
  if (!crossedBlockBoundary(from, to)) return;
  now = regionManager.getRegionSet(to);   was = playerRegions.get(uuid);
  for r in (was \ now): dispatch RegionExitEvent   // farewell
  for r in (now \ was): dispatch RegionEnterEvent  // greeting ; si annulé → repousser (setTo(from))
```
- **Folia** : `PlayerMoveEvent` est dispatché sur le thread de région du joueur → calcul + diff sur le bon thread, sans hop. La logique de diff est dans `common` ; seul le listener est Bukkit.
- Câblé aussi : teleport, join (init du set), quit (purge), respawn.

---

## 10. Protection (listeners plateforme → requête common)

Chaque plateforme traduit ses événements natifs en une seule question à `common` : `ProtectionQuery.isAllowed(world, x, y, z, action, who)`. Sur Bukkit :
```java
@EventHandler(ignoreCancelled = true)
public void onBreak(BlockBreakEvent e) {
    if (!query.isAllowed(loc, Actions.BLOCK_BREAK, e.getPlayer().getUniqueId()))
        e.setCancelled(true);   // + message throttlé
}
```
Événements Bukkit couverts (MVP) : BlockBreak/Place/Burn/Ignite/Spread, BlockFromTo (fluides), Entity/BlockExplode, PlayerInteract(Entity), InventoryOpen (conteneurs), EntityDamageByEntity (PvP + mobs), CreatureSpawn, HangingBreak, Piston, Bucket, EntityChangeBlock, item drop/pickup, redstone. Un autre bootstrap (Fabric) fournira ses propres listeners appelant **le même** `ProtectionQuery`.
Court-circuit : bypass admin caché + `query` vide si aucune région dans le chunk → coût nul.

---

## 11. Système de flags

- **Registry** (`FlagRegistry`, dans common) : flags built-in enregistrés au boot ; addons via l'API (comme UltraRegionAddon).
- **Types de valeur** : `StateFlagValue` (ALLOW/DENY — l'équivalent du `FlagValue` ternaire d'UltraRegions), `StringFlagValue`, `IntFlagValue`, `MessageFlagValue` (MiniMessage), `LocationFlagValue`, `SetFlagValue<Material>`.
- **Valeurs par `GroupTarget`** : OWNER / MEMBER / VISITOR + par **groupe LuckPerms** → répond à WorldGuard #2257 et aux manques de WG-GUI (audit §5).
- **Résolution effective** : priorité desc → parent → région `__global__` du monde → défaut.

**~50 flags MVP** : *blocs* (break, place, interact, use, container, redstone, piston, fire-spread/ignite, fluid-flow, block-explosion, leaf-decay, crop-trample) · *entités* (mob-spawning, mob-damage, mob-griefing, entity-explosion, damage-animals, hanging-break, armor-stand, vehicle) · *joueurs* (pvp, invincible, fall-damage, hunger, item-drop/pickup, exp-drop, chat, commands whitelist/blacklist, enderpearl, chorus, elytra, fly, god, keep-inventory) · *zone* (entry, exit, greeting, farewell, entry-permission, teleport, title, action-bar, boss-bar) · *méta 1.21* (mace-damage, wind-charge, totem, trident, firework).

---

## 12. Persistance Sarah (schéma adapté aux formes)

Architecture `storage/` de DialogWarps (interface + repository + DTO `@Column` + migrations), dans `common`. Le schéma stocke **la forme de façon générique** : type + données JSON + bounding box dénormalisée pour l'indexation.

```java
// migrations/CreateRegionsTable — createOrAlter(...) pour l'auto-ajout futur de colonnes
schema.uuid("id").primary();                  // UUID (pas auto-inc → upsert SQLite OK, note Sarah)
schema.string("name", 64); schema.string("world", 64);
schema.integer("priority").defaultValue(0);
schema.string("shape_type", 16);              // CUBOID / CYLINDER / SPHERE / POLYGON
schema.text("shape_data");                     // JSON des paramètres (centre, rayon, sommets, minY/maxY…)
schema.integer("bb_min_x"); schema.integer("bb_min_y"); schema.integer("bb_min_z");
schema.integer("bb_max_x"); schema.integer("bb_max_y"); schema.integer("bb_max_z");   // bounding box
schema.uuid("parent_id").nullable();
schema.bool("is_global").defaultValue(false);
schema.string("origin_server", 64).defaultValue("global");   // multi-serveur : le serveur qui possède la région (§ARCHI 14)
schema.integer("version").defaultValue(0);                    // verrou optimiste cross-serveur
schema.createdAt();

// CreateFlagsTable
schema.autoIncrement("id");
schema.uuid("region_id").foreignKey("%prefix%regions", "id", true);   // CASCADE
schema.string("flag_key", 64);
schema.string("group_target", 32).nullable();   // OWNER/MEMBER/VISITOR/<groupe>/null
schema.text("flag_value");

// CreateMembersTable
schema.autoIncrement("id");
schema.uuid("region_id").foreignKey("%prefix%regions", "id", true);
schema.uuid("player"); schema.string("role", 16); schema.createdAt();
```
Rappels Sarah (issus de l'exploration) : `MigrationManager.setDatabaseConfiguration(...)` **avant** `execute` ; `createOrAlter` pour l'auto-migration ; upsert SQLite exige PK non-auto-inc ou `unique()` (d'où l'UUID en PK) ; DTO = ordre des champs == ordre du constructeur. Connexion SQLite/MySQL exactement comme `WarpRepository.connect()`.
**Cache** : toutes les régions chargées en mémoire au boot → index chunk ; écritures en async (`SchedulerAdapter.async`). Zéro I/O sur le hot path. Point d'ancrage multi-serveur : **toutes** les invalidations passent par une méthode unique `RegionManager.reload(regionId)` (remove index → `loadRegion(id)` → re-add) — c'est là que le messaging v2 se branchera.
**Multi-serveur** : en mono-serveur, SQLite convient ; **dès que `multi-server.enabled`, MySQL/MariaDB est requis** (SQLite = fichier local, corruption sur partage réseau). Une instance ne charge que les régions où `origin_server == config.server`. Voir [`ARCHITECTURE.md` §14](./ARCHITECTURE.md).

---

## 13. GUI zMenu — hook **optionnel**, avec repli commandes

`common` définit `GuiService` (ouvrir la gestion d'une région, l'éditeur de flags, la liste). **Deux implémentations** :
- **`Hooks/zMenu`** (si zMenu présent) : inventaires YAML riches + dialogs (copie du `PluginDialog`/`DialogService` de DialogWarps).
  - `region-list` : liste paginée/recherchable.
  - `flag-editor` : **paginé, filtré par permission, recherche** → répond à WorldGuard #2260 + manques de WG-GUI. Chaque flag = `ButtonLoader` custom (toggle ALLOW/DENY, cycle de valeur, sélecteur `GroupTarget`), enregistré via `ButtonLoaderRegisterEvent`.
  - `member-manager`, `shape-editor` (redimensionner/convertir la forme).
  - Confirmations/saisies via `DialogManager` (delete, rename, valeur de flag).
- **Repli texte** (zMenu absent) : `GuiService` no-op → tout passe par les **commandes** avec sortie Adventure (`/rg info` liste flags/membres en texte, `/rg flag …` en CLI). **Le plugin est 100 % utilisable sans zMenu.**

Ainsi le GUI reste le différenciateur *là où zMenu est installé*, sans en faire une dépendance dure — et les futures plateformes (Fabric/Nukkit) fourniront leur propre `GuiService` ou resteront en commandes.

---

## 14. Commandes (`RegionCommandManager` dans common, binding par plateforme)

Toute la *logique* (parsing, routage, permissions, tab-complétion) vit dans `common` via le `RegionCommandManager` + la hiérarchie `RegionCommand` — **copiés de LuckPerms** (📋, `TabCompleter`/`ArgumentList`/`ArgumentTokenizer` réutilisables tels quels). Détail et signatures : [`ARCHITECTURE.md`](./ARCHITECTURE.md) §11. Le *binding* est par plateforme : `BukkitCommandExecutor extends RegionCommandManager` convertit `CommandSender/String[]` → `RegionSender/List<String>` (sourceSet `main`, spigot-api) ; la tab-complétion async Paper (`AsyncTabCompleteEvent`) et l'enregistrement Brigadier/Commodore optionnel sont dans le sourceSet `paper`. Racine `/region` + alias **`/rg`** (mémoire WorldGuard → migration facilitée) ; `/zregions` pour l'admin.
```
/rg wand                       /rg create <name> [shape] [priority]     /rg redefine <name>
/rg shape <name> <cuboid|cylinder|sphere|polygon>    /rg remove <name>  /rg list [world]
/rg info [name]                /rg flag <name> <flag> [value] [-g group]
/rg addmember|addowner <name> <player|g:group>       /rg setpriority|setparent …
/zregions reload               /zregions import <worldguard|ultraregions|redprotect> [--dry-run]
/zregions bypass
```
Permissions déclaratives (`RegionPermission` + `isAuthorized(sender)`), tab-complétion asynchrone. **Parité CLI/GUI totale** (attente réelle du segment admin, et obligatoire puisque le GUI est optionnel). Une commande écrite dans `common` fonctionne sur Bukkit et, demain, sur Fabric/Nukkit sans réécriture.

---

## 15. Importateurs (levier de conversion)

Dans `common` (interfaces) + `Hooks/` (impl. dépendant du plugin source). Migration sans friction = facteur d'achat (audit §8, « an absolute breeze »).
- **WorldGuard** (critique) : API `RegionContainer → ProtectedRegion` → cuboïde/priorité/parent/owners/members/flags. **Bonus formes** : les régions *polygonales* WG s'importent directement dans notre `PolygonShape` (là où d'autres les perdent). Cylindres WG n'existent pas → rien à perdre.
- **UltraRegions** : `compileOnly` contre `libs/UltraRegions.jar` (comme `UltraRegionAddon`), lire `plugin.getWorlds() → ManagedWorld → Region`, flags via `FlagValue`. ⚠️ Data-model confirmé sur UR ≈1.6 → valider par `javap` sur le jar 1.9.7.
- **RedProtect** : secondaire (segment hybride).
Tous : `--dry-run` + rapport (importé/sauté/erreurs). Jamais de troncature silencieuse.

---

## 16. Hooks (dossier `Hooks/`, modèle zMenu)

Chaque sous-dossier = un sous-module Gradle auto-inclus, chargé par réflexion si son plugin cible est présent (isolé, soft-dep). Le bootstrap `bukkit` scanne et instancie les hooks disponibles au boot.

**MVP** :
- **`CurrenciesAPI`** → `EconomyService` (achat/location de régions). **Seule dépendance économie**, shadée + relocalisée (`libs.currencies`). API : enum `Currencies` + `CurrencyProvider` (`deposit`/`withdraw`/`getBalance`, `BigDecimal`, `OfflinePlayer`). Gère Vault + PlayerPoints + CoinsEngine + zEssentials + 12 autres → **pas de hook Vault séparé**.
- **`zMenu`** → `GuiService` (inventaires/dialogs). Optionnel (§13).
- **`PlaceholderAPI`** → expansion `%zregions_*%` + conditions d'entrée.
- **`LuckPerms`** → membres/flags par groupe.
- **`WorldEdit`** → sélection visuelle (wand, `//sel` → forme).

**v1.x / v2** : `EssentialsX`+`zEssentials` (blocage `/home`,`/back`), cartes web **BlueMap**+**Pl3xMap**+**Dynmap** (trio standard, audit §7 — rendu des formes, y compris cercles/étoiles), `ProtocolLib`/`PacketEvents` (visualisation), `zAuctionHouse` (vente/enchère de régions).

---

## 17. Folia & scheduling

`common` ne connaît que `SchedulerAdapter` — dont **l'essentiel (`JavaSchedulerAdapter`, async pur JDK) est déjà dans `common`** (📋 LuckPerms). La plateforme n'ajoute que l'exécution *sync* (ARCHITECTURE.md §9) :
- `async` → pool JDK (`common`) : écritures Sarah, imports, chargement, housekeeping — **hors thread serveur**.
- Sur Spigot/Paper : `BukkitSchedulerAdapter` (sourceSet `main`), `executeSync` via le scheduler Bukkit.
- Sur Folia : `FoliaSchedulerAdapter` (sourceSet `paper`, isolé), route vers `EntityScheduler`/`RegionScheduler`/`GlobalRegionScheduler`. **Choisi par `Class.forName("…RegionizedServer")`** — pas via FoliaLib (abandonnée ici).
Checks de protection (§10) et diff enter/exit (§9) tournent sur le thread où l'événement natif est déjà dispatché. `folia-supported: true`.
⚠️ Ne pas répéter l'erreur de zKoth (FoliaLib shadée mais code sur `Bukkit.getScheduler()` → pas réellement Folia). Folia = promesse commerciale → **tester sur un vrai serveur Folia** (dossier `papers/`).

---

## 18. Config & messages

- Texte = **Adventure `Component`** produit dans `common`, **MiniMessage** dans les fichiers. Envoi via `Sender.sendMessage(Component)` (impl. plateforme : Paper natif, Spigot via adventure-platform shadé). Indépendant de zMenu.
- Multilingue **en/fr/es/it** (config.yml, messages.yml par langue) ; `language.yml` chargé en 1er, jamais traduit. YAML auto-réparant (merge des nouvelles clés, comme DialogWarps).
- **Docs Docusaurus bilingues EN/FR** obligatoires (convention workspace) : `plugins/zregions/docs/` + `i18n/fr/`.

---

## 19. Tests (JUnit 5)

Le module `common` est **testable sans serveur** (c'est tout l'intérêt du platform-agnostic — grand gain vs un plugin Bukkit-only). Cibles :
- `shape/*Test` : `contains` de chaque forme (cuboïde limites, cercle/sphère rayon, polygone/étoile ray casting, Y range), bounding box, sérialisation JSON aller-retour.
- `ChunkRegionIndexTest` : add/query multi-chunks, seuil `largeRegions`, retrait à la suppression.
- `FlagResolutionTest` : priorité, héritage parent, fallback global→défaut, valeurs par `GroupTarget`. **Cœur métier — profondeur requise.**
- `RegionRepositoryTest` : CRUD Sarah SQLite temp, cascade delete, idempotence migrations, round-trip forme.
- `importer/*Test` : parsing d'exemples WorldGuard/UltraRegions → régions attendues (polygones → PolygonShape).
Pas de Mockito (comme DialogWarps) : les chemins `permission` ne sont pas testés unitairement ; la logique pure l'est — et elle représente ici l'essentiel du code.

---

## 20. Roadmap par jalons

**J0 — Squelette multi-module (1 sem.)** : `api` + `common` + `bukkit` + `Hooks/` vides qui compilent ; `bukkit` boot → hooke Sarah, charge config/messages, log « ready ». `gradlew build` → `target/zRegions.jar`. **Valide la chaîne multi-module + relocations avant tout.**

**J1 — Cœur régions & formes (2 sem.)** : `RegionShape` + 4 formes, `Cuboid` généralisé, `ChunkRegionIndex`, `RegionManager` (CRUD + cache), schéma Sarah + repository (forme JSON), `SelectionManager` (wand + WorldEdit), commandes de base.

**J2 — Flags & protection (2 sem.)** : `FlagRegistry` + ~50 flags, valeurs par `GroupTarget`, `ProtectionQuery` (common) + `ProtectionListener` (bukkit), `MovementListener` (enter/exit + greeting/farewell), résolution. **Profiling perf obligatoire** (benchmark : N régions × Y joueurs, TPS stable — l'anti-UltraRegions).

**J3 — GUI zMenu (optionnel) + repli commandes (2 sem.)** : `GuiService`, `Hooks/zMenu` (inventaires + dialogs, flag-editor filtré), **et vérifier que tout marche sans zMenu** (sortie texte). Le différenciateur, dégradant proprement.

**J4 — Migration & hooks (1,5 sem.)** : importateurs WG (critique, avec polygones) + UltraRegions + RedProtect (`--dry-run`), `Hooks/CurrenciesAPI` (économie), LuckPerms, PlaceholderAPI.

**J5 — Finition & sortie (1,5 sem.)** : multilingue en/fr/es/it, docs Docusaurus, bStats (⚠️ à câbler, non fait dans DialogWarps), **test Folia réel**, benchmarks publiés, **API figée + addon d'exemple**.

→ **MVP vendable ≈ 9–10 semaines** (Bukkit). Les bootstraps Fabric/Forge/Nukkit viennent après, réutilisant `common` sans le retoucher.

**v2 (roadmap publique)** : **couche messaging multi-serveur** (`RegionMessenger` Redis/plugin-message dans `common` + `SyncTask`/`BufferedRequest`, style LuckPerms + zAuctionHouse — [`ARCHITECTURE.md` §14](./ARCHITECTURE.md) ; le modèle de données est déjà prêt en v1), **module claims joueurs** avec claim-blocks réseau-wide (le vrai volume, audit §9.1 + §10.2), achat/location/enchère de régions (synergie zAuctionHouse, réutilise les verrous Lua), cartes web (formes incluses), gestion de mondes (« Multiverse » d'UltraRegions), **bootstrap Nukkit (Bedrock natif)**, bot Discord.

> Note : le **modèle de données multi-serveur** (MySQL, `origin_server`, `version`, `RegionManager.reload(id)`) est intégré **dès la v1** (§12) — seule la couche transport/messaging est reportée en v2. Voir la checklist « à faire dès la v1 » dans ARCHITECTURE.md §14.8.

---

## 21. Risques techniques & points de vigilance

1. **Sur-ingénierie du multi-plateforme** : n'écrire QUE `bukkit` en v1, mais garder `common` propre (zéro import Bukkit/zMenu). Le piège inverse (tout mettre dans bukkit) tuerait l'objectif Fabric/Bedrock. → Revue d'architecture : `common` ne doit jamais importer `org.bukkit`.
2. **Performance** (risque n°1) : le moteur spatial (§8). Benchmark dès J2 avant toute cosmétique.
3. **Formes non-cuboïdes & sélection** : l'UX de sélection d'un polygone/étoile est plus délicate qu'un cuboïde. → Commencer cuboïde+cercle+sphère (simples), polygone/étoile juste après.
4. **Data-model UltraRegions non vérifié en 1.9.7** → `javap` sur le vrai jar.
5. **Folia réel** (piège zKoth) → tester sur serveur Folia.
6. **zMenu optionnel = double chemin** (GUI + texte) à maintenir. → La sortie texte est la source de vérité (parité CLI), le GUI est une couche par-dessus.
7. **Adventure sur Spigot** : Paper le fournit nativement, Spigot non → shader adventure-platform-bukkit uniquement pour la compat Spigot, sinon conflits.
8. **Fenêtre concurrentielle** (audit §9.7) : viser le MVP à ~10 semaines avant qu'UltraRegions ne renaisse.
9. **`fastutil` optionnel** : commencer sans, ajouter si profiling.
