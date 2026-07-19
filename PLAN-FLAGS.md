# zRegions — Plan d'implémentation des flags de `FUTURE-FLAGS.md`

> Plan d'exécution pour ajouter les flags proposés dans [`FUTURE-FLAGS.md`](./FUTURE-FLAGS.md).
> Ce document dit *comment* et *dans quel ordre* ; `FUTURE-FLAGS.md` reste la source du *quoi/pourquoi*
> (events Bukkit, inspirations, mapping WorldGuard par flag). Convention projet :
> un flag n'est **livré** que quand runtime + tests + doc + i18n + import sont faits (`CLAUDE.md`,
> règle « no dead flags » de `Flags.java`). **Rien n'est ajouté à `DOCUMENTATION.md` avant d'être enforced.**

---

## 0. Cadrage : « tous les flags » n'est pas un seul lot

`FUTURE-FLAGS.md` contient **~200 candidats** (P1→P5 + catalogue étendu). Les livrer d'un bloc est
irréaliste et contre‑productif : beaucoup se recoupent (`interact` vs `door-use`/`button-use`/…),
plusieurs sont explicitement à reporter (`build`/`passthrough`, contextes GriefDefender dynamiques,
`inventory-item-move`), et une partie (`gamemode`, `time-lock`, heal/feed, rôles‑management) exige des
**sous‑systèmes** (état joueur persistant, rôles dynamiques) qui sont des chantiers à part.

Ce plan propose donc :
1. une **Phase 0 d'infrastructure** (types de flags + mécanismes transverses) qui débloque tout le reste ;
2. des **batchs livrables** regroupés **par mécanisme d'enforcement** (= un listener + un jeu de flags +
   ses tests), pas seulement par priorité ;
3. une **liste explicite de ce qu'on ne fait pas** (ou pas maintenant).

Cible réaliste « core program » : **Phase 0 + Batchs B1→B8 ≈ 80–95 flags** à forte/moyenne valeur avec des
listeners directs. Les batchs B9→B11 (état joueur persistant, mouvement/téléport avancé, claim‑management)
sont des **chantiers séparés** avec leurs propres prérequis.

---

## 1. État de l'infra actuelle (ce sur quoi on s'appuie)

| Brique | Existe | Détail |
|---|---|---|
| Contrat `Flag<T>` | ✅ | `api/flag/Flag.java` — `parse`/`serialize`/`getDefaultValue`, **totalement générique** : un nouveau type de valeur ne touche pas l'API. |
| Types concrets | partiel | `StateFlag` (Boolean), `StringFlag`, `StringListFlag`. **Manquent : `IntegerFlag`, `DoubleFlag`, `EnumFlag`, `LocationFlag`, `EntityTypeListFlag`, `MaterialListFlag`.** |
| Catalogue | ✅ | `common/flag/Flags.java` : 46 flags dans `ALL`, `registerAll(registry)`. Ordre = ordre d'affichage. |
| Règle | ⚠️ | **« no dead flags »** : chaque flag de `ALL` est enforced par un listener. À respecter à chaque ajout. |
| Registry | ✅ | `ZFlagRegistry` (map concurrente, clé lowercase). Addons via `FlagRegistry.register` **avant** chargement des régions. |
| Résolution | ✅ (booléen/simple) | `ZRegionManager.resolveFlag(world,x,y,z,flag,uuid)` (positionnel) et `resolveFlag(region,flag,uuid)` (region‑scoped). **Pas** de logique « général → spécifique » (nécessaire pour les familles spawn/explosion/fluid). |
| Enforcement | ✅ | 4 listeners : `ProtectionListener` (actions joueur bloc/entité), `EnvironmentProtectionListener` (events sans joueur), `PlayerStateListener` (conditions joueur), `MovementListener` (enter/exit). Helpers : `check(flag,block,player,event)`, `isDenied(...)`, `isDeniedAt(flag,loc)`, `sendDeniedMessage(player)` (throttlé, envoie `Message.ACTION_DENIED`), `hasBypass(player)` (cache async‑safe). |
| GUI | ✅ | `Hooks/zMenu/…/FlagMaterials` : icône par flag (fallback template si absent). `RegionFlagsButton` route les flags **non‑Boolean** vers `/rg flag` (pas d'édition GUI in‑place). |
| Import WG | ✅ | `WorldGuardImporter` : table de mapping WG→zRegions + tests (`WorldGuardImporterTest`). Chaque flag avec équivalent WG s'ajoute ici. |
| Tests | ✅ | `FlagsTest.allContainsEveryBuiltInFlag` → **`assertEquals(46, …)`** : à incrémenter à chaque batch. `FlagResolutionTest` pour la résolution. |
| Test‑compteur | ⚠️ | `FlagsTest` fige le nombre (46) et vérifie l'unicité des clés → tout batch met à jour le compteur. |

### « Definition of done » d'UN flag (checklist répétée)

Pour chaque nouveau flag, dans le **même commit** :

1. Choisir/écrire le **type** (`StateFlag` par défaut ; sinon un type de Phase 0).
2. **Constante** dans `Flags.java` + ajout dans `ALL` (garde l'ordre par famille).
3. **Enforcement** : handler dans le bon listener (ou nouveau listener) → `resolveFlag` → cancel + `sendDeniedMessage` (actions joueur) **ou** `isDeniedAt` silencieux (environnement).
4. **`FlagsTest`** : incrémenter le compteur ; le test d'unicité des clés protège des doublons.
5. **Test** de comportement quand pertinent (résolution / parsing pour les types non triviaux / import).
6. **`FlagMaterials`** : une icône (sinon fallback template — acceptable, mais viser une icône pour la lisibilité GUI).
7. **Import WorldGuard** : ajouter le mapping dans `WorldGuardImporter` (si équivalent WG) + cas de test.
8. **`DOCUMENTATION.md`** §6 (catalogue de flags) + §13 Version history → Unreleased.
9. **i18n** : seulement si le flag introduit **un message** (la plupart des state‑flags réutilisent `ACTION_DENIED` → **pas** de `messages.yml`). Si message → **×4 langues** ; si clé config → **×4 config.yml**.

---

## 2. Décisions de conception à VERROUILLER avant de coder

Ces choix conditionnent plusieurs batchs — les trancher d'abord évite des reprises.

| # | Décision | Options | Reco |
|---|---|---|---|
| D1 | **Résolution « général → spécifique »** (fluid‑flow vs water/lava‑flow ; mob‑spawning vs animal/monster/spawner ; entity‑explosion vs creeper/tnt/…) | (a) le flag spécifique surcharge le général s'il est **défini** ; (b) flags indépendants | **(a)** : ajouter un helper `resolveMostSpecific(flags…)` dans le manager qui prend la 1re valeur explicitement posée dans l'ordre spécifique→général, sinon le défaut. À écrire **une fois** en Phase 0, testé (`FlagResolutionTest`). |
| D2 | **`deny-message`** (message custom par région à la place de `ACTION_DENIED`) | positionnel vs region‑scoped | **Positionnel** : `sendDeniedMessage` doit recevoir la **location** pour résoudre `deny-message` avant fallback. Refactor transverse (tous les call‑sites de `ProtectionListener`/`EnvironmentProtectionListener`). À faire en Phase 0. |
| D3 | **`entry-deny-message`/`exit-deny-message`** | — | `MovementListener`/`RegionMovementTracker.checkMove` doit **renvoyer la région+flag** qui a refusé, pas un booléen, pour résoudre le message. Refactor du tracker. |
| D4 | **Nouveaux types de valeur** | combien maintenant | Livrer en Phase 0 : `DoubleFlag`, `IntegerFlag`, `EnumFlag`, `LocationFlag`, `EntityTypeListFlag`. `MaterialListFlag` seulement quand un batch les consomme (B7/B8). ⚠️ `EntityTypeListFlag`/`MaterialListFlag` valident des noms Bukkit → **la validation vit côté plateforme** (abstraction `common`), pour ne pas importer `org.bukkit` dans `common`. |
| D5 | **Cible (`GroupTarget`) & flags de management** | — | Les flags `member-add`/`flag-edit`/… (B11) exigent des **rôles dynamiques** → dépendent du `target-api-refactor` du plan API. **Ne pas** les entamer avant. |
| D6 | **`farewell-title`** | flag dédié vs généraliser | Ajouter `farewell-title`/`farewell-subtitle` (symétrie avec `title`/`subtitle` déjà region‑scoped) plutôt qu'un renommage cassant. |
| D7 | **Interaction avec `/rg teleport` (déjà livré)** | — | `/rg teleport` va au centre de la bbox. Un futur **`teleport` LocationFlag** (destination custom) le **surcharge** ; `spawn` LocationFlag = respawn. Prévoir `LocationFlag` (D4) avant de reprendre ces flags. |
| D8 | **Numériques ↔ `IDEES.md`** | — | `DoubleFlag`/`IntegerFlag` (Phase 0) débloquent **aussi** `spawn-rate`/`spawner-rate`/`growth-rate`/`mob-cap` de `IDEES.md`. Mutualiser. |

---

## 3. Phase 0 — Infrastructure (prérequis, à livrer d'abord)

Aucun flag utilisateur ici seul : c'est le socle. À faire avant B1.

| Lot | Contenu | Effort |
|---|---|---|
| **P0.1 Types de flags** | `DoubleFlag`, `IntegerFlag`, `EnumFlag<E>` (petit parser générique), `LocationFlag` (sérialise `world,x,y,z,yaw,pitch` en JSON/plat), `EntityTypeListFlag` + `MaterialListFlag` (liste de clés validées **via une abstraction plateforme** `TypeValidator`). Tests de parse/round‑trip pour chacun (modèle `StringFlagTest`). ⚠️ round‑trip **stable** (cf. `toLiveRegion` drop les valeurs invalides). | M |
| **P0.2 Résolution général→spécifique (D1)** | `resolveMostSpecific(...)` + tests `FlagResolutionTest`. | S |
| **P0.3 `deny-message` plumbing (D2)** | Refactor `sendDeniedMessage(player)` → `sendDeniedMessage(player, world,x,y,z)` qui résout un `StringFlag deny-message` avant le fallback `ACTION_DENIED`. Toucher tous les call‑sites. | M |
| **P0.4 Tracker de mouvement enrichi (D3)** | `checkMove` renvoie `{denied, region, flag}` pour `entry/exit-deny-message`. | M |
| **P0.5 Abstraction état joueur** (pour B9, différable) | `RegionPlayer#setPlayerTime/Weather/WalkSpeed/GameMode(...)` + service de restauration à la sortie. **Ne bloque pas B1–B8** ; à livrer avec B9. | L |
| **P0.6 Scaffolding GUI/import/test** | Étendre `FlagMaterials` (icônes des nouveaux flags), et — si un batch le justifie — une petite section `materials:` YAML surchargeable. | S |

---

## 4. Roadmap par batchs (livrables indépendants)

Chaque batch = 1 (ou peu de) listener(s) + un jeu de flags + tests + doc + import. Chaque batch **incrémente le compteur `FlagsTest`** et suit le « definition of done » (§1).

### B1 — Interactions & entités « P1 » (le meilleur rapport valeur/effort) — **prioritaire**
`ride`, `sleep`, `respawn-anchor`, `item-frame-rotation`, `use-anvil`, `beacon`, `villager-trade`,
`shear`, `leash`, `animal-breeding`, `sign-edit`, `fishing-hook`, `projectile-launch`.
- Type : tous **state**. Listener : surtout `ProtectionListener` (nouveaux handlers) — `VehicleEnterEvent`/`EntityMountEvent`, `PlayerBedEnterEvent`, `PlayerInteractEvent` (respawn‑anchor/anvil/beacon/bone‑meal), `PlayerInteractEntityEvent` (item‑frame‑rotation/villager‑trade), `PlayerShearEntityEvent`, `PlayerLeashEntityEvent`, `EntityBreedEvent`, `SignChangeEvent`, `PlayerFishEvent`, `ProjectileLaunchEvent`.
- Import WG : `ride→ride`, `sleep→sleep`, `respawn-anchors→respawn-anchor`, `item-frame-rotation→…`.
- Effort : **M** (13 flags, listeners directs, aucun type nouveau). **Idéal en 1er.**

### B2 — Messages & commandes
`deny-message` (string), `entry-deny-message`, `exit-deny-message` (string, region‑scoped),
`command-whitelist` (StringListFlag — réutilise `command-blacklist`, blacklist gagne), `receive-chat`, `farewell-title`/`farewell-subtitle`.
- Dépend de **P0.3 + P0.4**. i18n : ces flags **portent des messages** → attention, mais ce sont des valeurs de flag (pas des clés `messages.yml`). `receive-chat` : `AsyncPlayerChatEvent` en retirant des recipients (limites proxy à documenter).
- Effort : **M** (surtout le plumbing de P0.3/P0.4).

### B3 — Environnement / contrôle du monde
`lightning`, `lava-fire` (sépare l'ignition lave de `fire-spread`), `water-flow`/`lava-flow` (spécifiques de `fluid-flow`, via **D1**), `fire-burn` (`BlockBurnEvent`, séparé de `fire-spread`), `snowman-trails`,
`snow-fall`/`snow-melt`/`ice-form`/`ice-melt`/`frosted-ice-form`/`frosted-ice-melt`, `soil-dry`, `coral-fade`, `copper-aging`, `block-spread`.
- Type : state. Listener : `EnvironmentProtectionListener` — `LightningStrikeEvent`, `BlockIgniteEvent`, `BlockFromToEvent`, `BlockBurnEvent`, `BlockFormEvent`/`BlockFadeEvent`/`EntityBlockFormEvent`, `BlockSpreadEvent`.
- ⚠️ **Perf** : `BlockForm/Fade/Spread` sont fréquents → silencieux, `isDeniedAt`, mesurer.
- Import WG : `lightning`, `lava-fire`, `water-flow`, `lava-flow`, `snow-fall`, `snow-melt`, `ice-form`, `ice-melt`, `frosted-ice-form`, `frosted-ice-melt`, `snowman-trails`, `copper-fade→copper-aging`.
- Effort : **L** (~15 flags, events fréquents à profiler).

### B4 — Croissance (naturelle)
`crop-growth`, `tree-growth`, `vine-growth`, `grass-spread`, `mycelium-spread`, `sculk-growth`, `mushroom-growth`, `rock-growth`, `bone-meal`, `entity-transform`.
- Type : state. Nouveau `GrowthListener` (déjà envisagé pour `growth-rate` d'`IDEES.md`) : `BlockGrowEvent`, `StructureGrowEvent`, `BlockSpreadEvent` (hors feu), `BlockFertilizeEvent`/`PlayerInteractEvent` (bone‑meal), `EntityTransformEvent`.
- ⚠️ Coordination avec la branche `FIRE` de `onSpread` (ne pas double‑traiter). **Perf** : events de ferme très fréquents.
- Lien : convergera plus tard avec `growth-rate` (`DoubleFlag`) — même listener.
- Effort : **M–L**.

### B5 — Spawns fins (via **D1** général→spécifique)
`animal-spawning`, `monster-spawning`, `spawner-spawning`, `phantom-spawning`, `slime-spawning`,
`natural-spawning`, `egg-spawning`, `command-spawning`, `raid-spawning`, `patrol-spawning`, `portal-spawning`,
`deny-spawn` (**EntityTypeListFlag**, P0.1), `animal-spawner-spawning`/`monster-spawner-spawning` (batch avancé si besoin).
- Listener : `EnvironmentProtectionListener.onCreatureSpawn` — réutiliser le `BLOCKED_SPAWN_REASONS` **déjà curé** (n'exclut jamais les reasons de conversion). `mob-spawning` reste le général ; les spécifiques surchargent (D1) par `SpawnReason` / catégorie d'entité (classifier via abstraction plateforme).
- ⚠️ Ne **pas** élargir le denylist de reasons (sécurité anti‑suppression du mob source).
- Lien : ouvre la voie à `spawn-rate`/`spawner-rate`/`mob-cap` (numériques, `IDEES.md`).
- Effort : **M–L**.

### B6 — Dégâts & explosions fines (via **D1**)
`villager-damage`, `monster-damage`, `pet-damage` (Tameable), `firework-damage`, `entity-explosion-damage`,
famille explosion par source `creeper-explosion`/`tnt`/`ghast-fireball`/`wither-damage`/`enderdragon-block-damage`,
`potion-splash`, `melee-pvp`/`projectile-pvp` (granularité de `pvp`), `bed-anchor-explosion`.
- Listener : `ProtectionListener`/`EnvironmentProtectionListener` — `EntityDamageByEntityEvent`, `EntityDamageEvent` (cause explosion), `PotionSplashEvent`/`LingeringPotionSplashEvent`, `BlockExplodeEvent`/`EntityExplodeEvent`.
- Sémantique « protège la victime » (comme `mob-damage`) → **trancher le bypass** flag par flag (cf. `firework-damage`).
- Import WG : `creeper-explosion`, `tnt`, `ghast-fireball`, `wither-damage`, `enderdragon-block-damage`.
- Effort : **L**.

### B7 — Interactions blocs fines (sous‑ensembles de `interact`)
`door-use`, `trapdoor-use`, `button-use`, `lever-use`, `pressure-plate-use`, `mechanism-use`,
`note-block-use`, `jukebox-use`, `lectern-use`, `crafting-table-use`, `brewing-stand-use`, `enchant-table-use`,
`grindstone-use`/`smithing-table-use`/`cartography-table-use`/`loom-use`/`stonecutter-use`/`composter-use`/`cauldron-use`,
`ender-chest-use`, `break-spawners`/`place-spawners`/`break-hoppers` (surcharges éco de break/place).
- Type : state. Listener : `ProtectionListener` — `PlayerInteractEvent` (par `Material`/`InventoryType`), `InventoryOpenEvent`.
- ⚠️ **Explosion du catalogue** : ces flags **recoupent `interact`/`container-access`**. Reco : livrer un **sous‑ensemble ciblé** (workstations éco + door/button/lever/plate) plutôt que les 25. Décider par demande client. Considérer un flag `mechanism-use` comme regroupement.
- Effort : **M** (mécanique) mais **arbitrage produit** requis (ne pas tout ajouter).

### B8 — Items, drops & transferts
`drop-on-death`/`player-drops`/`keep-level` (règles de mort, distincts de `keep-inventory`/`exp-drop`),
`death-item-pickup`, `item-despawn`, `item-merge`, `item-frame-damage`/`painting-damage` (non‑joueur),
`mob-drops`, `block-drops`, `hopper-transfer`/`inventory-move` (⚠️ **perf**, `InventoryMoveItemEvent` très fréquent → benchmark avant, cf. « à ne pas ajouter »).
- Listener : `ProtectionListener`/`EnvironmentProtectionListener` — `PlayerDeathEvent`, `ItemDespawnEvent`, `ItemMergeEvent`, `EntityDamageEvent` (frames/paintings), `EntityDeathEvent`, `InventoryMoveItemEvent`.
- Effort : **M** (hors flags perf‑sensibles à repousser).

### B9 — **Chantier séparé** : état joueur persistant (P0.5 requis)
`gamemode`, `time-lock`, `weather-lock`, heal/feed (`heal-delay`/`heal-amount`/…, `feed-*`), `walk-speed`/`fly-speed`,
`potion-effects`/`clear-effects`, `bossbar`, `experience-multiplier`, `glow`, `gravity`.
- Nécessite un **service d'état par joueur** (application à l'entrée, **restauration propre** à la sortie), intégré à `RegionMovementTracker`, + un **scheduler par joueur/région** (heal/feed), + les abstractions plateforme `RegionPlayer#set…`.
- **Risque** : conflits avec plugins minigames/chat/scoreboard. Types : `EnumFlag`/`IntegerFlag`/`DoubleFlag`.
- Effort : **XL** — un projet en soi. Fort attrait commercial (lobbies RPG/arènes) mais **ne pas mélanger** aux batchs simples.

### B10 — **Chantier séparé** : mouvement, portails & téléport avancés
`portal-use`, `teleport-in`/`teleport-out`, `exit-via-teleport`, `exit-override`, `move`,
`teleport`/`spawn` (**LocationFlag**, D7), `teleport-message`, `spawn-teleport`.
- Modifie la **résolution du mouvement** (`MovementListener`/tracker) et introduit `LocationFlag`. `move`/`exit-override` touchent l'algo enter/exit (anti‑softlock + bypass).
- Interagit avec `/rg teleport` déjà livré (le `teleport` LocationFlag le surcharge).
- Effort : **L–XL**.

### B11 — **Chantier séparé** : claim‑management (dépend des rôles dynamiques)
`member-add`/`member-remove`/`member-role-set`/`member-ban`, `flag-edit`, `priority-edit`, `parent-edit`,
`region-redefine`/`region-rename`, `spawn-set`, `claim-create`/`claim-border`.
- **Dépend de `target-api-refactor` + rôles dynamiques** (plan API, `IDEES.md` custom‑roles). zRegions est aujourd'hui admin‑command‑driven ⇒ c'est un **pivot produit** (déléguer la gestion aux owners). **Ne pas** entamer avant la refonte des cibles.
- Effort : **XL**, conditionné.

---

## 5. Import WorldGuard (transverse)

À chaque batch, ajouter les mappings WG→zRegions dans `WorldGuardImporter` (+ cas `WorldGuardImporterTest`),
en réutilisant le pattern « deux passes » existant. Mappings notables : `ride`, `sleep`, `respawn-anchors`,
`item-frame-rotation`, `use-anvil`, `deny-message`, `allowed-cmds→command-whitelist`, `lava-fire`,
`water-flow`/`lava-flow`, `snow-*`/`ice-*`, `copper-fade→copper-aging`, `creeper-explosion`/`tnt`/`ghast-fireball`/`wither-damage`/`enderdragon-block-damage`, `game-mode→gamemode`, `time-lock`, `weather-lock`, `teleport`/`spawn`,
`farewell-title`, `exit-via-teleport`, `exit-override`, `notify-enter`/`notify-leave`. Rien de silencieux : tout
mapping absent reste reporté (comportement actuel de l'importer).

---

## 6. Ce qu'on NE fait PAS (ou pas maintenant)

- **`build` / `passthrough`** : zRegions a choisi « allow par défaut » et ne reproduit l'implicite WG **qu'à l'import**. Ajouter `build` changerait le modèle mental → **skip**.
- **Contextes GriefDefender dynamiques** (`used_item`/`source`/`target`, `entity-source-rules`, `material-source-rules`) : nouveau modèle de flag/contexte, pas un simple ajout → **hors périmètre**.
- **`inventory-item-move`/`hopper-transfer`** : `InventoryMoveItemEvent` est un hot‑path massif → **benchmark obligatoire avant**, sinon ne pas livrer.
- **`nonplayer-protection-domains`** (pistons/TNT transfrontaliers WG) : complexe et très spécifique → différer.
- **Catalogue étendu redondant** (`door-use`+`button-use`+`lever-use`+… tous en même temps) : livrer **des sous‑ensembles ciblés**, pas les 25 — la lisibilité de l'éditeur de flags prime.
- **`biome`/`swim`/`crawl`/`entity-ai`** : très version‑spécifiques ou invasifs → P5, sur demande client.

---

## 7. Séquencement recommandé & effort

```
Phase 0 (infra) ──► B1 (P1 interactions, M) ──► B2 (messages/commandes, M)
        │                                             │
        └──► P0.1 types débloquent aussi:             ▼
             spawn-rate / growth-rate (IDEES)     B3 (environnement, L)
                                                      ▼
                                                  B4 (croissance, M–L)
                                                      ▼
                                                  B5 (spawns fins, M–L)  ─┐ (D1 partagé)
                                                  B6 (dégâts/explosions, L)┘
                                                      ▼
                                                  B7 (interactions fines, M — arbitrage)
                                                  B8 (items/drops, M)
        ─────────── chantiers séparés (prérequis dédiés) ───────────
        B9 état joueur persistant (XL, P0.5)
        B10 mouvement/téléport (L–XL, LocationFlag)
        B11 claim‑management (XL, dépend rôles dynamiques)
```

- **Livrable minimal à forte valeur** : Phase 0 + **B1 + B2** (≈ 20 flags, listeners directs, débloque `deny-message`).
- **Programme « catalogue riche »** : + B3→B6 (≈ +55 flags, parité WG/environnement/spawns/dégâts).
- **B7/B8** : ajouts mécaniques, mais **arbitrer** pour ne pas noyer l'éditeur de flags.
- **B9/B10/B11** : chantiers produits distincts, à planifier séparément.

Après **chaque** batch : `gradlew build` (tests + `FlagsTest` compteur), `DOCUMENTATION.md`, import WG + tests, icônes `FlagMaterials`. Profiler B3/B4/B5 (events fréquents) — c'est la promesse anti‑UltraRegions.

---

## 8. Interdépendances avec les autres plans

- **`IDEES.md` (flags numériques)** : `DoubleFlag`/`IntegerFlag` de **Phase 0** débloquent `spawn-rate`/`spawner-rate`/`growth-rate`/`mob-cap` — mêmes listeners que B4/B5. À mutualiser.
- **Plan « forme d'API » (`target-api-refactor`)** : prérequis de **B11** (claim‑management par rôle) et des cibles `-t group:` — à faire **avant** tout gel d'API si ces flags sont visés.
- **`/rg teleport` (déjà livré)** : le futur **`teleport` LocationFlag** (B10) le surcharge (destination custom vs centre bbox).
- **`FlagMaterials` (déjà livré)** : chaque nouveau flag ajoute une entrée d'icône.
