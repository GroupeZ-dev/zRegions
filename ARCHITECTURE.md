# zRegions — Architecture multi-plateforme (blueprint calqué sur LuckPerms)

> Document de référence pour la couche d'abstraction plateforme. Complète [`PLAN-IMPLEMENTATION.md`](./PLAN-IMPLEMENTATION.md) (structure globale) et [`AUDIT-MARCHE.md`](./AUDIT-MARCHE.md) (marché).
> **Fondé sur une analyse en profondeur du code source réel de LuckPerms** (cloné, lu fichier par fichier). Chaque décision cite la classe LuckPerms correspondante.
> Objectif : **un maximum de logique dans `common`** (platform-agnostic, testable sans serveur) ; les modules plateforme (bukkit d'abord, fabric/nukkit ensuite) ne font QUE le pont jeu↔plugin. Cible finale : serveurs Java (Spigot/Paper/Folia), vanilla (Fabric/Forge) et Bedrock (Nukkit), comme LuckPerms.

> ⚖️ **Licence** : LuckPerms est sous **licence MIT**. Ses classes d'infrastructure (JarInJarClassLoader, SchedulerAdapter, TabCompleter, ArgumentList, SenderFactory…) sont **réutilisables** en conservant l'en-tête MIT et l'attribution. Beaucoup peuvent être copiées quasi telles quelles — indiqué par 📋 ci-dessous.

---

## 1. Principe directeur

```
Règle absolue : le module `common` n'importe JAMAIS org.bukkit.*, ni zMenu, ni aucune API serveur.
Tout ce qui touche au jeu passe par une interface définie dans `common` et implémentée par la plateforme.
```

LuckPerms applique ce principe à la lettre : un `grep "org.bukkit"` dans `common/` ne renvoie rien. Le résultat : ~90 % du code (permissions, stockage, commandes, config, messaging) est écrit une seule fois et tourne sur 10 plateformes. Pour zRegions, cela veut dire que le moteur spatial, le moteur de flags, la résolution, le stockage Sarah, les commandes et les importateurs sont dans `common`, et que `bukkit` ne contient que des adaptateurs.

---

## 2. Le modèle en 3 couches (loader → bootstrap → plugin)

LuckPerms n'a pas 2 mais **3 niveaux**. Flux réel d'instanciation :

```
Serveur → ZRegionsLoaderPlugin (JavaPlugin, plugin.yml) → ZRegionsBukkitBootstrap → ZRegionsBukkitPlugin → AbstractZRegionsPlugin
          └─ module bukkit:loader ──┘   └───────────── module bukkit ─────────────┘   └──── module common ────┘
```

| Couche | Module | Rôle | Réf. LuckPerms |
|---|---|---|---|
| **Loader** | `bukkit:loader` | Le seul `JavaPlugin` que le serveur connaît (dans `plugin.yml`). **Zéro logique** : monte un `JarInJarClassLoader`, instancie le bootstrap par réflexion, délègue `onLoad/onEnable/onDisable`. | `BukkitLoaderPlugin.java:32-58` |
| **Bootstrap** | `bukkit` | Le pont plateforme : **détient** les objets natifs (logger, scheduler, serveur), fournit les primitives (version, dataFolder, accès joueurs), pilote le cycle de vie via des **latches**. Instancie le plugin common. | `LPBukkitBootstrap.java:58,101-111` |
| **Plugin** | `common` | `AbstractZRegionsPlugin` : **toute la logique** + `enable()` `final` à ordre fixe + hooks abstraits que la plateforme remplit. | `AbstractLuckPermsPlugin.java:94,153-276` |

Le **loader jar-in-jar** (`LoaderBootstrap` + `JarInJarClassLoader`, 📋 copiables) isole les dépendances relocatées du classloader du serveur. Contrat minimal (`LoaderBootstrap.java:31-38`) :
```java
public interface LoaderBootstrap { void onLoad(); default void onEnable(){} default void onDisable(){} }
```
Le loader (`BukkitLoaderPlugin`) :
```java
public class ZRegionsLoaderPlugin extends JavaPlugin {
    private static final String JAR_NAME = "zregions-bukkit.jarinjar";
    private static final String BOOTSTRAP = "fr.maxlego08.zregions.bukkit.ZRegionsBukkitBootstrap";
    private final LoaderBootstrap plugin;
    public ZRegionsLoaderPlugin() {
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP, JavaPlugin.class, this);
    }
    @Override public void onLoad()   { plugin.onLoad(); }
    @Override public void onEnable() { plugin.onEnable(); }
    @Override public void onDisable(){ plugin.onDisable(); }
}
```

> **Décision zRegions** : adopter le loader jar-in-jar dès le départ. Pour un plugin **vendu qui cohabite avec d'autres**, il évite les conflits de versions de libs (Adventure, drivers JDBC…) et permet le téléchargement de dépendances au runtime (§9) — supérieur au `plugin.yml > libraries` de Paper (qui n'existe pas sur Spigot pur et ne relocalise pas). *Simplification possible en phase 0 : faire du bootstrap directement le `JavaPlugin` et shader classiquement, puis introduire le loader quand le catalogue de libs grossit.*

---

## 3. Ciblage Spigot vs Paper (LE point de votre contrainte)

**Constat vérifié** : LuckPerms compile le module bukkit contre **`dev.folia:folia-api`** (sur-ensemble de Paper, lui-même sur-ensemble de Spigot) — **jamais** `spigot-api`. Il garantit la compat Spigot **par isolation de classes + détection runtime**, pas par la dépendance de compilation (`LPBukkitPlugin.java:346-361`, `LPBukkitBootstrap.java:317-324`).

Votre contrainte est plus stricte (« compat Spigot **garantie**, plugin vendu »). D'où la stratégie retenue, **plus sûre que LuckPerms** :

### Deux sourceSets dans le module `bukkit`
```
bukkit/src/main/java   → compile contre  org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT   (le plancher supporté)
bukkit/src/paper/java  → compile contre  dev.folia:folia-api:1.21.x                       (Paper + Folia)
```
- Le sourceSet **`main`** contient tout le pont Bukkit standard. Compiler contre spigot-api fait que **le compilateur REFUSE tout appel Paper-only** → compat Spigot prouvée mécaniquement, pas par discipline.
- Le sourceSet **`paper`** contient **uniquement** les classes qui touchent Paper/Folia (`PaperSchedulerAdapter`, `FoliaSchedulerAdapter`, `PaperAsyncTabCompleteListener`, envoi Adventure natif éventuel). Elles ne sont **jamais référencées directement** par `main` — seulement via `Class.forName` + une interface commune.

### Le pattern d'isolation (copié de LuckPerms)
```java
// dans le sourceSet main (spigot-safe)
static boolean classExists(String n){ try{ Class.forName(n); return true; } catch(Throwable e){ return false; } }
static boolean isFolia(){ return classExists("io.papermc.paper.threadedregions.RegionizedServer"); }
static boolean isPaper(){ return classExists("com.destroystokyo.paper.PaperConfig") || isFolia(); }

// aiguillage — la classe Folia n'est chargée QUE si Folia est présent
SchedulerAdapter scheduler = isFolia()
        ? instantiate("fr.maxlego08.zregions.paper.FoliaSchedulerAdapter", this)   // sourceSet paper
        : new BukkitSchedulerAdapter(this);                                         // sourceSet main, pur Bukkit
```
La JVM ne linke jamais une classe non référencée : sur Spigot, `FoliaSchedulerAdapter` (qui `import io.papermc…`) n'est jamais chargée → aucun `NoClassDefFoundError`. C'est exactement ce que fait LuckPerms pour Folia (`LPBukkitBootstrap.java:105-107`) et le tab-complete async Paper (`LPBukkitPlugin.java:153-157`).

### Messages : `adventure-platform-bukkit`, jamais l'Adventure natif Paper
LuckPerms envoie via `BukkitAudiences` (`BukkitSenderFactory.java:45,65-71`) — la lib détecte elle-même si le serveur a Adventure natif (Paper récent) et bascule, sinon sérialise en legacy. **Fonctionne identiquement sur Spigot et Paper.** Ne jamais appeler `player.sendMessage(Component)` directement (Paper-only, casse Spigot). Relocaliser `net.kyori.adventure` → `fr.maxlego08.zregions.libs.adventure`.

> **`plugin.yml`** : `api-version: '1.20'`, `folia-supported: true` (le badge n'est valide que si tout accès scheduler passe par un adapter Folia isolé). Version spigot-api plancher = la plus basse que vous vendez (ici 1.20.4).

---

## 4. Couche d'abstraction — catalogue complet

Chaque ligne = une interface (ou classe abstraite générique) dans `common`, une implémentation dans `bukkit`. C'est **le contrat plateforme** de zRegions.

| Adaptateur | `common` (abstrait) | `bukkit` (impl) | Réf. LuckPerms | 📋 |
|---|---|---|---|---|
| Bootstrap | `ZRegionsBootstrap` (interface) | `ZRegionsBukkitBootstrap` | `LuckPermsBootstrap` | |
| Plugin | `ZRegionsPlugin` + `AbstractZRegionsPlugin` | `ZRegionsBukkitPlugin` | `AbstractLuckPermsPlugin` | |
| **Sender** (source de commande) | `RegionSender` + `AbstractRegionSender<T>` + `RegionSenderFactory<P,T>` | `BukkitSenderFactory<…,CommandSender>` | `Sender`/`SenderFactory` | ~ |
| **Player** (joueur en ligne + position) | `RegionPlayer` + `AbstractRegionPlayer<T>` + `RegionPlayerFactory<T>` | `BukkitPlayerFactory<…,Player>` | *(enrichissement, voir §6)* | |
| Scheduler | `SchedulerAdapter` + `JavaSchedulerAdapter` | `BukkitSchedulerAdapter` / `FoliaSchedulerAdapter` | idem | 📋 |
| Logger | `PluginLogger` (interface) | `JavaPluginLogger` | idem | 📋 |
| ClassPathAppender | `ClassPathAppender` | `JarInJarClassPathAppender` | idem | 📋 |
| ConnectionListener | `AbstractConnectionListener` | `BukkitConnectionListener` | idem | ~ |
| CommandManager | `RegionCommandManager` + hiérarchie `RegionCommand` | `BukkitCommandExecutor` (+async) | `CommandManager` | 📋 |
| Config | `ConfigurationAdapter` | `BukkitConfigAdapter` | idem | ~ |
| EventBus | `AbstractEventBus<P>` | `BukkitEventBus` | idem | ~ |
| GuiService (optionnel) | `GuiService` | `Hooks/zMenu` | *(spécifique zRegions)* | |
| EconomyService (optionnel) | `EconomyService` | `Hooks/CurrenciesAPI` | *(spécifique zRegions)* | |

📋 = copiable quasi tel quel (MIT) · ~ = à adapter mais structure directement réutilisable.

---

## 5. `RegionSender` — la source de commande (joueur OU console)

Copie fidèle de `Sender` de LuckPerms (`Sender.java:43-160`). C'est l'abstraction pour **envoyer un message et vérifier une permission**, que l'émetteur soit un joueur ou la console.

```java
// common — fr.maxlego08.zregions.common.sender
public interface RegionSender {
    UUID   CONSOLE_UUID = new UUID(0, 0);
    String CONSOLE_NAME = "Console";

    ZRegionsPlugin getPlugin();
    String  getName();
    UUID    getUniqueId();                       // CONSOLE_UUID pour la console
    void    sendMessage(Component message);      // Adventure
    boolean hasPermission(String permission);
    boolean hasPermission(RegionPermission permission);   // enum de permissions déclaratives
    void    performCommand(String commandLine);
    boolean isConsole();
    default boolean isValid() { return true; }
    Optional<RegionPlayer> asPlayer();           // présent si l'émetteur est un joueur en ligne (§6)
}
```

**Une seule implémentation** `AbstractRegionSender<T>` (📋 `AbstractSender.java:49-133`) : capture `uuid`/`name`/`isConsole` au wrap, découpe les messages multi-lignes pour la console, `equals`/`hashCode` par UUID. La console **n'est pas une classe séparée** — c'est un `AbstractRegionSender` où `isConsole()==true` + UUID sentinelle. (Un `DummyConsoleSender` 📋 sert de repli quand aucune console native n'existe, ex. exécution interne de commande.)

**La factory plateforme** `RegionSenderFactory<P,T>` (📋 `SenderFactory.java:41-79`) — 7 méthodes à implémenter par plateforme, `wrap(T)` produit le `RegionSender` :
```java
// common
public abstract class RegionSenderFactory<P extends ZRegionsPlugin, T> implements AutoCloseable {
    protected abstract UUID    getUniqueId(T sender);
    protected abstract String  getName(T sender);
    protected abstract void    sendMessage(T sender, Component message);
    protected abstract Tristate getPermissionValue(T sender, String node);
    protected abstract boolean hasPermission(T sender, String node);
    protected abstract void    performCommand(T sender, String command);
    protected abstract boolean isConsole(T sender);
    public final RegionSender wrap(T sender) { return new AbstractRegionSender<>(getPlugin(), this, sender); }
}
// bukkit — T = org.bukkit.command.CommandSender (spigot-api)
public class BukkitSenderFactory extends RegionSenderFactory<ZRegionsBukkitPlugin, CommandSender> {
    // getUniqueId: Player→getUniqueId() sinon CONSOLE_UUID ; sendMessage via BukkitAudiences ;
    // isConsole: instanceof ConsoleCommandSender||RemoteConsoleCommandSender ; performCommand: server.dispatchCommand
}
```
Le `common` manipule `RegionSender`, jamais `CommandSender`.

---

## 6. `RegionPlayer` — le joueur en jeu (l'enrichissement clé)

**LuckPerms n'a pas besoin de la position ; zRegions oui.** C'est la principale différence : on ajoute une abstraction *joueur en ligne* qui porte **monde + position** (indispensable pour savoir dans quelle région se trouve un joueur). Même patron que le Sender (factory + impl générique), mais enrichi.

```java
// common — value object de position, sans Bukkit
public final class RegionLocation {
    private final String world; private final double x, y, z; private final float yaw, pitch;
    public int blockX(){ return (int) Math.floor(x); } /* blockY, blockZ … */
}

// common — le joueur en ligne
public interface RegionPlayer {
    UUID    getUniqueId();
    String  getName();
    String  getWorldName();
    RegionLocation getLocation();
    boolean hasPermission(String permission);
    void    sendMessage(Component message);
    void    sendActionBar(Component message);      // messages greeting/farewell (§ flags zone)
    void    teleport(RegionLocation to);
    boolean isOnline();
    RegionSender asSender();                        // un RegionPlayer EST aussi une source de commande
}

// common — factory (calquée sur RegionSenderFactory)
public abstract class RegionPlayerFactory<T> {     // T = org.bukkit.entity.Player
    protected abstract UUID getUniqueId(T h);
    protected abstract String getName(T h);
    protected abstract RegionLocation getLocation(T h);
    protected abstract boolean hasPermission(T h, String node);
    protected abstract void sendMessage(T h, Component msg);
    protected abstract void teleport(T h, RegionLocation to);
    protected abstract boolean isOnline(T h);
    public final RegionPlayer wrap(T handle) { return new AbstractRegionPlayer<>(this, handle); }
}
```

**Distinction à garder nette** (elle existe aussi chez LuckPerms entre `Sender` et le joueur natif) :
- Le **système de commandes** raisonne en `RegionSender` (joueur ou console).
- Le **moteur de régions/protection** raisonne en `RegionPlayer` (a une position). Les listeners de protection (`onBlockBreak`, `onMove`) traduisent l'événement natif en `RegionPlayer` + `RegionLocation`, puis interrogent le `RegionManager` — 100 % logique common derrière.

---

## 7. `ZRegionsBootstrap` — le pont vers les joueurs en ligne

Interface `common` implémentée par la plateforme. C'est **le seul moyen pour `common` d'énumérer/récupérer des joueurs sans toucher Bukkit** (📋 `LuckPermsBootstrap.java:166-218`).

```java
// common — fr.maxlego08.zregions.common.plugin.bootstrap
public interface ZRegionsBootstrap {
    // primitives plateforme
    PluginLogger getPluginLogger();
    SchedulerAdapter getScheduler();
    ClassPathAppender getClassPathAppender();
    CountDownLatch getLoadLatch();
    CountDownLatch getEnableLatch();               // synchronise les events précoces (§10)
    String getVersion(); Instant getStartupTime();
    Platform.Type getType(); String getServerBrand(); String getServerVersion();
    Path getDataDirectory();
    // accès joueurs en ligne
    Optional<RegionPlayer> getPlayer(UUID uniqueId);
    Optional<UUID>   lookupUniqueId(String username);   // nom → uuid (cache serveur)
    Optional<String> lookupUsername(UUID uniqueId);
    Collection<UUID> getOnlinePlayers();
    Collection<String> getPlayerList();
    int getPlayerCount();
    boolean isPlayerOnline(UUID uniqueId);
    RegionSender getConsoleSender();
}
```
Impl Bukkit : `getServer().getPlayer(uuid)`, `getOfflinePlayer(name)`, `getOnlinePlayers()` (📋 `LPBukkitBootstrap.java:249-293`).

---

## 8. Cycle de vie : `AbstractZRegionsPlugin` (enable() final + hooks)

`enable()` est **`final`** et fixe l'ordre de démarrage ; les plateformes ne remplissent que des **hooks abstraits nommés** (📋 `AbstractLuckPermsPlugin.java:153-276,362-373`). C'est ce qui garantit que toutes les plateformes démarrent dans le même ordre.

```java
// common
public abstract class AbstractZRegionsPlugin implements ZRegionsPlugin {
    public final void load() {
        this.dependencyManager = createDependencyManager();          // §9
        this.configuration = new ZRegionsConfiguration(this, provideConfigurationAdapter()); // hook
    }
    public final void enable() {
        setupSenderFactory();            // hook  → BukkitSenderFactory
        setupPlayerFactory();            // hook  → BukkitPlayerFactory
        this.storage = new StorageFactory(this).getInstance();       // Sarah (common)
        registerPlatformListeners();     // hook  → connection + movement + protection listeners
        registerCommands();              // hook  → BukkitCommandExecutor
        setupManagers();                 // hook  → RegionManager + FlagManager (chargent l'index spatial)
        setupPlatformHooks();            // hook  → zMenu?/CurrenciesAPI?/PAPI?
        this.eventDispatcher = new EventDispatcher(provideEventBus());// hook
        registerApiOnPlatform();         // hook  → expose l'API via ServicesManager
        performFinalSetup();             // hook  → charge les régions des joueurs déjà connectés
        this.running = true;
    }
    public final void disable() {
        getBootstrap().getScheduler().shutdownScheduler();
        this.storage.shutdown();
        getBootstrap().getScheduler().shutdownExecutor();
    }
    @Override public PluginLogger getLogger() { return getBootstrap().getPluginLogger(); }

    // hooks abstraits = le contrat plateforme
    protected abstract void setupSenderFactory();
    protected abstract void setupPlayerFactory();
    protected abstract ConfigurationAdapter provideConfigurationAdapter();
    protected abstract void registerPlatformListeners();
    protected abstract void registerCommands();
    protected abstract void setupManagers();
    protected abstract void setupPlatformHooks();
    protected abstract AbstractEventBus<?> provideEventBus();
    protected abstract void registerApiOnPlatform();
    protected abstract void performFinalSetup();
}
```
`ZRegionsPlugin` (interface, contrat plateforme) expose les getters : `getBootstrap`, `getConfiguration`, `getStorage`, `getRegionManager`, `getFlagManager`, `getConnectionListener`, `getCommandManager`, `getEventDispatcher`, `getOnlineSenders`, `getConsoleSender`. Le `ZRegionsBukkitPlugin` ne fait que stocker le bootstrap et remplir les hooks.

---

## 9. Scheduler & gestion des dépendances runtime

### Scheduler (📋 quasi tel quel)
`SchedulerAdapter` (contrat) + `JavaSchedulerAdapter` (**impl JDK complète dans `common`** : `ScheduledThreadPoolExecutor` + `ForkJoinPool`, `JavaSchedulerAdapter.java:47-139`). La plateforme n'ajoute que `executeSync` (~15 lignes) :
```java
// bukkit
public class BukkitSchedulerAdapter extends JavaSchedulerAdapter {
    private final Executor sync; // = r -> server.getScheduler().scheduleSyncDelayedTask(loader, r)
    public void executeSync(Runnable t){ sync.execute(t); }
}
```
Variante `FoliaSchedulerAdapter` (sourceSet `paper`) route vers `EntityScheduler`/`RegionScheduler`/`GlobalRegionScheduler`. **Toutes les écritures Sarah et le housekeeping tournent en async pur** (hors thread serveur).

### Dépendances runtime (recommandé pour un plugin vendu)
LuckPerms **shade très peu** : il télécharge la plupart des libs au runtime (relocalisées à la volée, **vérifiées par checksum SHA-256**) via un enum `Dependency` + `DependencyManager` (`Dependency.java`, `DependencyManagerImpl.java:167-223`), dans un classloader isolé. Avantages vs `plugin.yml > libraries` : portable Spigot+Paper, relocalisé (pas de conflit avec d'autres plugins vendus qui shadent les mêmes libs), intégrité vérifiée.

> **Décision zRegions** : viser ce modèle pour les grosses libs (drivers JDBC MySQL/MariaDB, Adventure). Sarah + CurrenciesAPI restent shadés+relocalisés (petits, maison). *Démarrage pragmatique : shader tout en phase 0, migrer les gros drivers vers le DependencyManager quand le catalogue grossit.*

---

## 10. Chargement des régions au join (AbstractConnectionListener)

Le modèle exact pour charger/décharger les données joueur (📋 `AbstractConnectionListener.java:41-128`). **Logique dans `common`** (ne connaît que `UUID`/monde) ; l'adaptateur Bukkit ne fait que traduire les events.

```java
// common
public abstract class AbstractConnectionListener {
    public void onLogin(UUID uuid, String name, String world) {
        // (optionnel) précharger les régions du monde du joueur, initialiser son "region set" courant
    }
    public void handleDisconnect(UUID uuid) {
        // purger le cache per-joueur (region set courant)
    }
}
// bukkit — traduit les events, en s'appuyant sur les LATCHES du bootstrap
@EventHandler public void onQuit(PlayerQuitEvent e){ handleDisconnect(e.getPlayer().getUniqueId()); }
```
Les **latches** (`loadLatch`/`enableLatch` du bootstrap) permettent aux events async précoces d'attendre la fin de l'enable — indispensable si un joueur se connecte pendant le démarrage. Pour zRegions, l'`AbstractConnectionListener` initialise le *region set courant* du joueur (pour le diff enter/exit, cf. plan §9) et le `MovementListener` (Bukkit) le met à jour.

---

## 11. Commandes : `common` définit, la plateforme binde

Le `CommandManager` de LuckPerms est **directement étendu** par `BukkitCommandExecutor` — la plateforme ne réécrit rien, elle convertit `CommandSender/String[]` → `RegionSender/List<String>`.

### Dans `common` (📋 copiables : `TabCompleter`, `CompletionSupplier`, `ArgumentList`, `ArgumentTokenizer`)
```java
public class RegionCommandManager {
    private final Map<String, RegionCommand<?>> commands;   // + hook plugin.getExtraCommands()
    public void executeCommand(RegionSender sender, String label, List<String> args) { /* executor dédié + rate-limit + watchdog */ }
    public List<String> tabCompleteCommand(RegionSender sender, List<String> args) { /* TabCompleter positionnel */ }
}
public abstract class RegionCommand<T> {
    public abstract void execute(ZRegionsPlugin p, RegionSender s, T target, ArgumentList a, String label);
    public List<String> tabComplete(ZRegionsPlugin p, RegionSender s, ArgumentList a) { return List.of(); }
    public boolean isAuthorized(RegionSender s) { return permission == null || s.hasPermission(permission); }
}
// + RegionSingleCommand extends RegionCommand<Void>  (commande simple)
// + RegionParentCommand<T,I>  (routage 2 niveaux : /rg flag <REGION> <sub> — parseTarget/getTarget/lock par cible)
```
(📋 `CommandManager.java:107-322`, `Command.java`, `SingleCommand.java`, `ParentCommand.java`.) La cible générique `T` d'une commande zRegions sera souvent `Region` (ex. `/rg flag <region> …`), résolue par `parseTarget`.

### Le binding Bukkit (📋 `BukkitCommandExecutor.java:49-91`)
```java
public class BukkitCommandExecutor extends RegionCommandManager implements TabExecutor {
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        executeCommand(plugin.getSenderFactory().wrap(s), label, ArgumentTokenizer.EXECUTE.tokenizeInput(args));
        return true;
    }
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        return tabCompleteCommand(plugin.getSenderFactory().wrap(s), ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args));
    }
}
```
- **Tab-complétion async** (Paper) : sous-classe écoutant `AsyncTabCompleteEvent` (sourceSet `paper`), appelant le même `tabCompleteCommand` (📋 `BukkitAsyncCommandExecutor.java:43-75`).
- **Brigadier/Commodore** (optionnel) : décrit la *forme* de `/rg` au client (coloration/structure), le prédicat de permission réutilise `RegionCommandManager` ; l'exécution réelle reste sur `onCommand` (📋 `LuckPermsBrigadier.java:44-56`). Alternative moderne : Brigadier natif Paper via `LifecycleEvents.COMMANDS` (comme DialogWarps) dans le sourceSet `paper`.

> Ainsi une commande est écrite **une fois** dans `common` et fonctionne sur Bukkit, et demain sur Fabric (qui bindera son propre exécuteur sur le même `RegionCommandManager`).

---

## 12. Ce qui vit où (récapitulatif « max de logique dans common »)

| Dans `common` (écrit une fois) | Dans `bukkit` (le pont, ~10 % du code) |
|---|---|
| Moteur spatial (formes, index chunk, résolution) | Listeners de protection (BlockBreak, Move…) → traduisent en `RegionPlayer`+`RegionLocation` |
| Moteur de flags + résolution priorité/parent/global | `BukkitSenderFactory` / `BukkitPlayerFactory` (wrap natifs) |
| Stockage Sarah (repository, DTO, migrations) | `ZRegionsBukkitBootstrap` (primitives serveur, accès joueurs) |
| `RegionCommandManager` + toutes les commandes | `BukkitCommandExecutor` (convertit CommandSender/args) |
| Config + messages (Adventure/MiniMessage) | `BukkitConfigAdapter`, `BukkitSchedulerAdapter`(+Folia dans `paper`) |
| Importateurs (WorldGuard, UltraRegions, RedProtect) | `BukkitConnectionListener` (join/quit → common) |
| `AbstractConnectionListener`, `SchedulerAdapter`(Java), EventBus | `BukkitEventBus` (pont events Bukkit pour l'interop GroupeZ) |
| API publique (`api/`) | `registerApiOnPlatform` (ServicesManager) |

Les hooks optionnels (`Hooks/zMenu` pour la GUI, `Hooks/CurrenciesAPI` pour l'économie) sont aussi côté plateforme, implémentant des interfaces `common` (`GuiService`, `EconomyService`).

---

## 13. Modules Gradle (mise à jour du plan)

```
zRegions/
├── api/                     # contrat public, 0 dépendance serveur, publié target-api/
├── common/                  # TOUTE la logique — 0 import org.bukkit / zMenu
│   └── src/main/java/fr/maxlego08/zregions/common/
│       ├── plugin/  (bootstrap/, scheduler/, logging/, classpath/, util/AbstractConnectionListener)
│       ├── sender/  RegionSender, AbstractRegionSender, RegionSenderFactory, DummyConsoleSender
│       ├── player/  RegionPlayer, AbstractRegionPlayer, RegionPlayerFactory, RegionLocation
│       ├── command/ RegionCommandManager, abstraction/*, utils/ArgumentList+Tokenizer, tabcomplete/*
│       ├── region/ shape/ spatial/ flag/ protection/ storage/ importer/ config/
├── common/loader-utils/     # 📋 JarInJarClassLoader + LoaderBootstrap (MIT, copiés)
├── bukkit/                  # pont Bukkit — SHIP v1
│   └── src/
│       ├── main/java  → compile contre  org.spigotmc:spigot-api:1.20.4   (compat Spigot garantie)
│       └── paper/java → compile contre  dev.folia:folia-api:1.21.x       (classes Paper/Folia isolées)
├── bukkit/loader/           # ZRegionsLoaderPlugin (JavaPlugin du plugin.yml) + jarinjar en ressource
├── Hooks/                   # zMenu, CurrenciesAPI, LuckPerms, WorldEdit, PlaceholderAPI
└── (futur) fabric/  nukkit/ # nouveaux ponts, réutilisent common tel quel
```
`settings.gradle.kts` : `include("api","common","common:loader-utils","bukkit","bukkit:loader")` + Hooks auto-inclus.

Sketch build `bukkit` (deux sourceSets) :
```kotlin
sourceSets { create("paper") }
dependencies {
    "compileOnly"("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")                 // main : plancher Spigot
    "paperCompileOnly"("dev.folia:folia-api:1.21.7-R0.1-SNAPSHOT")               // paper : Paper+Folia
    "paperImplementation"(sourceSets["main"].output)                             // paper voit les interfaces de main
    implementation(project(":common"))
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.0")
}
tasks.shadowJar {                                                                 // assemble main + paper + common
    from(sourceSets["paper"].output)
    relocate("net.kyori.adventure", "fr.maxlego08.zregions.libs.adventure")
    relocate("fr.maxlego08.sarah",  "fr.maxlego08.zregions.libs.sarah")
    relocate("fr.traqueur.currencies","fr.maxlego08.zregions.libs.currencies")
}
```

---

## 14. ⭐ Multi-serveur (cross-server) — blueprint

> Marché & usage : [`AUDIT-MARCHE.md` §10](./AUDIT-MARCHE.md). Fondé sur le code réel de **LuckPerms** (`messaging/`, `storage/`, `SyncTask`, `BufferedRequest` — lus dans le clone) ET des **deux implémentations cross-server maison de GroupeZ** : `zAuctionHouse Redis` (le patron abouti) et `zVaults/Distributed`.

### 14.0 Principe — ce que « multi-serveur » veut dire pour des régions

Une région est **intrinsèquement liée à un `(serveur, monde, coordonnées)`** — elle n'a pas vocation à « exister sur tous les serveurs » (l'audit §10.3 réfute l'idée qu'un monde shardé imposerait de partager les régions). Le multi-serveur pour zRegions apporte **trois** choses, pas la réplication d'un monde :
1. **Gestion admin centralisée** — lister/éditer les régions de *tous* les serveurs depuis une **base partagée**, même celles non chargées localement.
2. **Cohérence du cache** — une édition sur le serveur A est rechargée par les serveurs concernés (pas de cache périmé).
3. **(v2 claims joueurs) données joueur réseau-wide** — claim-blocks accumulés/dépensés à l'échelle du réseau, liste de claims globale, téléport cross-server.

**Règle de séquencement :** le **modèle de données doit être multi-serveur-ready dès la v1** (support MySQL, colonne `origin_server`, cache invalidable par région), la **couche messaging étant livrée en v2**. C'est ce qui évite une réécriture. Tout ce qui suit est conçu pour se brancher sans toucher au cœur v1.

### 14.1 Découpage GLOBAL vs PAR-SERVEUR (modèle LuckPerms)

LuckPerms ne code aucun concept de « serveur » en dur : chaque instance porte une clé de config `server` (défaut `global`), transformée en contexte `server=<nom>` injecté dans toutes les requêtes (`ConfigurationContextCalculator.java:45-52`), et chaque donnée porte des colonnes `server`/`world` dédiées (`mysql.sql:8-9`) ; une donnée sans contexte serveur est « globale ». Transposition zRegions :

| Donnée | Portée | Colonne serveur |
|---|---|---|
| **Régions** (forme, flags, membres, index spatial) | **PAR-SERVEUR** — chaque instance ne charge/protège que ses régions | `origin_server` = l'UUID/nom du serveur |
| Config du plugin, bans/blacklist | GLOBAL | aucune (NULL/`global`) |
| (v2) claim-blocks par joueur, limites réseau-wide | GLOBAL | aucune |

### 14.2 La couche messaging dans `common` (contrat LuckPerms)

Le contrat de transport est **minuscule** (lu en direct dans le clone) — tout l'intelligence est dans `common`. Interfaces à définir dans `common` (copie 1:1 de LuckPerms 📋) :

```java
// common/messaging — le transport (impl. par plateforme : Redis, PluginMessage)
public interface RegionMessenger extends AutoCloseable {          // = LuckPerms Messenger.java:37-54
    void sendOutgoingMessage(OutgoingMessage msg);                // toujours async
    default void close() {}
}
public interface RegionMessengerProvider {                        // = MessengerProvider.java:41-61
    String getName();
    RegionMessenger obtain(IncomingMessageConsumer consumer);     // le consumer est fourni par common
}
public interface IncomingMessageConsumer {                        // = IncomingMessageConsumer.java:38-75
    boolean consumeIncomingMessageAsString(String encoded);       // false si ping-id déjà traité (dédup + anti-écho)
}
```
Un transport se contente de **diffuser une chaîne encodée** et de **rendre les chaînes reçues au consumer**. Toute la logique (types de message, ping-id, push/pull, buffer) vit dans un `MessagingService` de `common`, calqué sur `LuckPermsMessagingService`.

**Ping-id (anti-écho + dédup)** — chaque message porte un UUID aléatoire pré-inséré dans un `ExpiringSet` (5 min) local *avant* l'envoi ; à la réception, `if (!receivedMessages.add(id)) return false;` → un serveur ignore son propre message ET les doublons (`LuckPermsMessagingService.java:107-111,157-159`). *(Note : le code maison zAuctionHouse utilise plutôt un **UUID de serveur persistant** — voir §14.4 ; les deux approches sont valables, l'UUID persistant sert en plus à estampiller `origin_server`.)*

### 14.3 Le flux d'un changement (push notification → pull by id)

Le pattern par défaut (LuckPerms `StorageAssistant.java:77-138` + `LuckPermsMessagingService.java:254-300`) :

```
Serveur A (édition d'une région)
  1. storage.saveRegion(region)          // écrit dans la DB PARTAGÉE = source de vérité
  2. if (messaging != null && auto-push)  messaging.pushRegionUpdate(regionId)   // PING : juste l'id + ping-id
       │  (le message NE transporte PAS la région)
       ▼
Serveur B (reçoit le ping)
  3. consumeIncomingMessage → RegionUpdateMessage(regionId)
  4. storage.loadRegion(regionId)         // relit UNIQUEMENT cette région depuis la DB partagée
  5. scheduler.sync(region.world) → index.remove(old) ; index.add(reloaded)   // applique sur le thread de région
```

**Messages de région (dans `common`)** — LÉGER par défaut, GRAS par exception :

| Message | Champs | Léger/Gras | Pourquoi |
|---|---|---|---|
| `RegionUpdateMessage` | `regionId` | **LÉGER** (reload) | création / redéfinition / priorité / parent : la DB a l'état à jour |
| `RegionFlagChangeMessage` | `regionId, flagKey, newValue, groupTarget` | **gras léger** | éviter de recharger toute la région pour un seul flag |
| `RegionDeleteMessage` | `regionId, worldName` | **GRAS (minimal)** | le récepteur doit **purger son cache sans relire la DB** (la ligne est déjà supprimée → un reload-by-id renverrait `null`) — c'est exactement la leçon du bug « ghost listing » de `ItemBoughtMessage.java:3-25` |
| `RegionMemberChangeMessage` | `regionId, memberUuid, role, added` | LÉGER ou semi | reload par id suffit |

> **La leçon clé du code maison** (`ItemBoughtMessage` de zAuctionHouse) : « notification + pull by id » n'est **pas** universel. Dès qu'un reload renverrait `null`/incohérent (ligne supprimée), embarquer l'état minimal dans le message. Règle : **id + blob d'état minimal, jamais l'objet entier ; re-résoudre les références par id côté récepteur** (cf. `VaultUpdateAdapter.java:64` qui re-résout le `Vault` par UUID).

### 14.4 Les transports côté plateforme (style GroupeZ)

Deux transports, tous deux implémentant `RegionMessenger`. **Suivre le squelette de `zAuctionHouse Redis`** (le patron abouti), pas celui de zVaults (plus fragile).

**A. `RedisMessenger`** (recommandé pour les réseaux sérieux) — combine la simplicité LuckPerms et la robustesse maison :
- **Pool Jedis** standalone + sentinel via une factory (`RedisConnectionFactory.java:16-72` de zAuctionHouse) — **jamais** un `new Jedis()` par publish (l'anti-pattern de zVaults, `ZDistributedManager.java:353-361`).
- **Un thread subscriber daemon** unique, **reconnexion à backoff exponentiel 1s→60s** (`RedisSubscriberRunnable.java:61-100`). *(LuckPerms fait un backoff plus simple de 5s, `RedisMessenger.java:153-155` ; préférer le backoff exponentiel maison.)*
- `sendOutgoingMessage` → `jedis.publish(channel, encoded)` (`RedisMessenger.java:110-113`).
- **UUID serveur persistant** (`server.info`) + **registre Redis + heartbeat** anti-collision (`ZAuctionHouseRedis.java:137-204`) — sert aussi à alimenter `origin_server`.
- **Whitelist de classes** avant tout `Class.forName` (sécurité anti-désérialisation, `RedisSubscriberRunnable.java:22-27,124-128`).
- Jedis `5.2.0` en `compileOnly`, **relocalisé** (`fr.maxlego08.zregions.libs.jedis`) — pas via `plugin.yml libraries` (cohérent avec la stratégie de relocation § du plan et le DependencyManager runtime §9).

**B. `PluginMessageMessenger`** (sans dépendance externe) — `AbstractPluginMessageMessenger.java:44-76` : encode en `writeUTF`, envoie sur le canal BungeeCord, la plateforme fournit `sendOutgoingMessage(byte[])`. **Limite connue** : exige ≥1 joueur en ligne sur les serveurs émetteur ET récepteur (le message « piggyback » sur une connexion joueur). Bon défaut « zéro-config », insuffisant pour des serveurs vides.

`RegionMessengerProvider` choisi depuis la config (`redis` / `pluginmessage` / `none`), comme `MessagingFactory` de LuckPerms.

### 14.5 Stockage & filet de sécurité

- **MySQL/MariaDB requis en multi-serveur.** SQLite/H2 sont des **DB locales** (LuckPerms les classe explicitement « Local databases », `StorageType.java:51-52`) → exclues dès que `multi-server` est activé (hard-fail au boot avec message clair, ou refus d'activer le messaging). Sarah supporte déjà MySQL/MariaDB.
- **Colonne `origin_server`** sur la table `regions` (+ `world` déjà présent). Chaque instance **ne charge/protège que** les régions où `origin_server == config.server` (équivalent du filtre par contexte `NodeMapBase.java:198`). Les commandes admin peuvent lister/éditer **n'importe quelle** région via la DB partagée (même non chargée localement) — c'est la valeur « gestion centralisée ».
- **`SyncTask` — filet de sécurité périodique** (`SyncTask.java:52-77`) : puisque Redis pub/sub est *fire-and-forget* (un serveur qui redémarre rate les pings émis pendant son absence), une resync complète depuis la DB toutes les `sync-minutes` rattrape les messages perdus. Recommandation LuckPerms : si le messaging est actif, `sync-minutes` peut être bas/désactivé, mais le garder > 0 comme filet est prudent.
- **`BufferedRequest` — coalescing** (`BufferedRequest.java`, buffer de réception 500 ms + buffer d'émission 2 s de LuckPerms) : une rafale de pings ne déclenche qu'**une** resync ; une rafale d'éditions ne pousse qu'**un** message. À copier (📋).
- **Cohérence concurrente** : pour des régions (éditions admin rares), un **verrou optimiste par colonne `version`** (`UPDATE … WHERE region_id=? AND version=?`) suffit — pas besoin des verrous distribués Lua. **Garder le patron Lua `SET NX PX` + token + fallback** de `RedisAuctionClusterBridge.java:40-87` **en réserve pour la v2** (achat/location/enchère de régions = contention monétaire, synergie zAuctionHouse).

### 14.6 Thread-safety & impact sur l'index spatial

- **Réception** : le handler tourne sur le **thread subscriber** (hors thread de jeu). Le reload DB se fait en **async** (pool JDK de `common`), puis la mutation de l'**index spatial** (remove + re-add de la région) est **re-basculée sur le thread de région** via le `SchedulerAdapter` (équivalent de `scheduler.runNextTick`, `ItemListedListener.java:42`). **Ne jamais** faire un `.get()` bloquant sur le thread principal (l'anti-pattern de zVaults `ZDistributedListener.java:35`).
- **Émission** : publier hors thread de jeu (`CompletableFuture.runAsync`, comme le bridge zAuctionHouse) pour ne pas bloquer le tick sur l'I/O Redis.
- **Index spatial** : l'invalidation par région est déjà supportée par le design v1 (`ChunkRegionIndex.add/remove`, ARCHITECTURE via plan §8) → un reload cross-server = `index.remove(regionId)` + `storage.loadRegion(regionId)` + `index.add(reloaded)`. Aucune refonte nécessaire.
- **Folia** : la mutation d'index s'exécute sur le thread de la région concernée (`RegionScheduler`), cohérent avec le confinement Folia déjà prévu.

### 14.7 Activation & config

Suivre zVaults pour l'ergonomie : un **toggle config** + **enregistrement conditionnel** du service (`ZVaultsPlugin.java:125-127`).
```yaml
multi-server:
  enabled: false                 # défaut mono-serveur (SQLite OK)
  server: survival-1             # nom/id de CETTE instance (→ origin_server ; "global" = ignoré)
  messenger: redis               # redis | pluginmessage | none
  sync-minutes: 10               # filet de sécurité (resync complète)
  redis:
    mode: standalone             # standalone | sentinel
    address: "127.0.0.1:6379"
    username: ""
    password: ""
    channel: "zregions"
    pool: { max-total: 32, max-idle: 16, min-idle: 8 }
```
Dans `AbstractZRegionsPlugin.enable()` (§8), le hook `setupPlatformHooks()`/un nouveau `setupMessaging()` n'instancie le `MessagingService` que si `multi-server.enabled`. En mono-serveur, **zéro dépendance Redis, zéro overhead** — le plugin reste 100 % fonctionnel.

### 14.8 À faire dès la v1 (pour ne pas se réécrire)

Même si le messaging est livré en v2, la v1 doit **déjà** :
1. Supporter **MySQL/MariaDB** via Sarah (déjà prévu) — ne pas coder en dur des hypothèses SQLite.
2. Inclure la colonne **`origin_server`** (défaut `global`) et une colonne **`version`** (verrou optimiste) dans la table `regions` (via `createOrAlter`, elles seront simplement `global`/`0` en mono-serveur).
3. Router **toutes** les écritures via `RegionRepository` (jamais de SQL brut ailleurs) et **toutes** les invalidations de cache via une méthode `RegionManager.reload(regionId)` — le point unique où le messaging se branchera.
4. Garder l'`AbstractConnectionListener` et le `SchedulerAdapter` (déjà là) comme points d'ancrage.

---

## 15. Fichiers de référence (chemins)

### 15.1 LuckPerms (clone) — architecture & messaging
Racine : `…\scratchpad\LuckPerms`. Modules `common/` et `bukkit/` = `…/src/main/java/me/lucko/luckperms/{common,bukkit}/`.

- Sender : `common/sender/{Sender,SenderFactory,AbstractSender,DummyConsoleSender}.java` · `bukkit/BukkitSenderFactory.java`
- Bootstrap/cycle de vie : `common/plugin/{LuckPermsPlugin,AbstractLuckPermsPlugin}.java` · `common/plugin/bootstrap/{LuckPermsBootstrap,BootstrappedWithLoader}.java` · `bukkit/{LPBukkitBootstrap,LPBukkitPlugin}.java`
- Loader : `common/loader-utils/…/loader/{LoaderBootstrap,JarInJarClassLoader}.java` · `bukkit/loader/…/BukkitLoaderPlugin.java`
- Scheduler : `common/plugin/scheduler/{SchedulerAdapter,SchedulerTask,JavaSchedulerAdapter}.java` · `bukkit/{BukkitSchedulerAdapter,FoliaSchedulerAdapter}.java`
- Connexion : `common/plugin/util/AbstractConnectionListener.java` · `bukkit/listeners/BukkitConnectionListener.java`
- Commandes : `common/command/{CommandManager}.java`, `command/abstraction/{Command,SingleCommand,ParentCommand,ChildCommand}.java`, `command/{tabcomplete/*,utils/ArgumentList,utils/ArgumentTokenizer}.java` · `bukkit/{BukkitCommandExecutor,BukkitAsyncCommandExecutor}.java`, `bukkit/brigadier/LuckPermsBrigadier.java`
- Joueur/contexte : `api/…/platform/PlayerAdapter.java` · `common/api/implementation/ApiPlayerAdapter.java` · `common/context/manager/ContextManager.java`
- Build/modules : `settings.gradle`, `common/build.gradle`, `bukkit/build.gradle`, `bukkit/loader/build.gradle`, `bukkit-legacy/build.gradle`, `common/…/dependencies/{Dependency,DependencyManagerImpl,DependencyRepository}.java`
- **Messaging/multi-serveur** : `api/…/messenger/{Messenger,MessengerProvider,IncomingMessageConsumer}.java` + `api/…/messenger/message/type/*` · `common/messaging/{LuckPermsMessagingService,MessagingFactory}.java`, `messaging/{redis/RedisMessenger,pluginmsg/AbstractPluginMessageMessenger,sql/AbstractSqlMessenger}.java` · `bukkit/messaging/{BukkitMessagingFactory,PluginMessageMessenger}.java`
- **Storage/sync** : `common/storage/{Storage,StorageFactory,StorageType}.java`, `storage/implementation/{StorageImplementation,split/SplitStorage}.java` · `common/tasks/SyncTask.java` · `common/cache/BufferedRequest.java` · `common/command/utils/StorageAssistant.java` · `common/context/calculator/ConfigurationContextCalculator.java` · `schema/mysql.sql`

*(Le clone LuckPerms est dans le scratchpad de session ; le re-cloner si besoin : `git clone --depth 1 https://github.com/LuckPerms/LuckPerms`. Licence MIT.)*

### 15.2 Code cross-server maison GroupeZ (le style à suivre)
Racine workspace : `D:\Users\Maxlego08\workspace2.0`.

- **`zAuctionHouse Redis`** (patron abouti à copier) : `…\redis\ZAuctionHouseRedis.java` (bootstrap, UUID serveur persistant `server.info`, `sendMessage`, registre+heartbeat), `…\redis\RedisAuctionClusterBridge.java` (scripts Lua `SET NX PX` + token, état/verrous, source de vérité DB vs Redis), `…\redis\listener\RedisSubscriberRunnable.java` (thread daemon + backoff exponentiel + whitelist de classes), `…\redis\listener\messages\ItemBoughtMessage.java` (**la leçon léger-vs-gras / ghost-listing**), `…\redis\listener\listeners\*Listener.java` (retour main-thread via `runNextTick`), `…\redis\connection\{RedisConnectionFactory,RedisConfig}.java` (pool standalone/sentinel). Addon séparé, Jedis `5.2.0`.
- **`zVaults\Distributed`** (request/reply + event-carried state, plus simple) : `…\distributed\ZDistributedManager.java`, `ZDistributedListener.java`, `adapter\VaultUpdateAdapter.java` (re-résolution de référence par id), records `zVaults\API\…\distributed\requests\*.java`, activation par toggle `multi-server-sync-support` (`ZVaultsPlugin.java:125-127`). ⚠️ Anti-patterns à **ne pas** copier : `new Jedis()` par publish, pas de backoff, `.get()` bloquant sur le main thread, UUID serveur volatil.
