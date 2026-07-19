# zRegions - Futurs flags proposes

> Document de conception pour une implementation future. Ces flags ne sont pas livres
> aujourd'hui et ne doivent pas etre ajoutes a `DOCUMENTATION.md` tant que le runtime,
> les tests, les messages et les imports ne sont pas implementes.

## Sources consultees

- WorldGuard 7 - Region Flags: https://worldguard.enginehub.org/en/latest/regions/flags/
- WorldGuard ExtraFlags - Spigot: https://www.spigotmc.org/resources/worldguard-extra-flags.4823/
- WorldGuard ExtraFlags - GitHub: https://github.com/aromaa/WorldGuardExtraFlags
- WorldGuard ExtraFlags Plus - Spigot: https://www.spigotmc.org/resources/worldguard-extraflags-plus.129946/
- GriefDefender - Advanced Flags: https://docs.griefdefender.com/wiki/advanced/Flags
- GriefPrevention - Features: https://docs.griefprevention.com/features/
- GPFlags - Spigot/Modrinth pages: https://www.spigotmc.org/resources/gpflags.55773/
  and https://modrinth.com/plugin/gpflags
- Lands - Roles and their Flags: https://wiki.incredibleplugins.com/lands/configuration/roles-and-their-flags
- Lands - Natural flags: https://wiki.incredibleplugins.com/lands/configuration/natural-flags
- Residence - flags examples: https://www.netycraft.cz/pomoc-residence-nastaveni_residence_vlajky
- PlotSquared - Plot flags: https://intellectualsites.gitbook.io/plotsquared/features/plot-flags
- BentoBox - Flags: https://docs.bentobox.world/en/latest/BentoBox/Flags/
- Towny - world/plot toggles: https://github-wiki-see.page/m/TownyAdvanced/Towny/wiki/How-Towny-Works
- Catalogue actuel zRegions: `common/src/main/java/fr/maxlego08/zregions/common/flag/Flags.java`
- Listeners actuels: `ProtectionListener`, `EnvironmentProtectionListener`,
  `PlayerStateListener`, `MovementListener`

## Etat actuel

zRegions embarque deja 46 flags, tous enforces:

- Blocs, actions joueur: `block-break`, `block-place`, `interact`,
  `container-access`, `bucket-fill`, `bucket-empty`, `armor-stand`,
  `hanging-break`, `hanging-place`, `vehicle-place`, `vehicle-destroy`,
  `crop-trample`.
- Environnement: `redstone`, `piston`, `fire-ignite`, `fire-spread`,
  `fluid-flow`, `leaf-decay`, `block-explosion`, `entity-explosion`.
- Entites: `mob-spawning`, `mob-griefing`, `damage-animals`, `mob-damage`.
- Joueurs: `pvp`, `invincible`, `fall-damage`, `hunger`, `item-drop`,
  `item-pickup`, `enderpearl`, `chorus-fruit`, `keep-inventory`, `exp-drop`,
  `chat`, `elytra`, `fly`, `totem`, `command-blacklist`.
- Zone: `entry`, `exit`, `greeting`, `farewell`, `title`, `subtitle`,
  `action-bar`.

La couverture est deja solide pour la protection classique et l'import WorldGuard.
Les manques les plus visibles sont les flags naturels fins, les restrictions
specialisees de gameplay, les messages custom par region, et quelques flags a valeur
non booleenne.

## Focus - WorldGuard ExtraFlags

WorldGuard ExtraFlags 4.2.4 annonce 26 flags additionnels. Par rapport a zRegions:

| Flag WorldGuard ExtraFlags | Etat zRegions | A ajouter ? | Notes |
|---|---|---|---|
| `teleport-on-entry` | absent | oui | Location flag + integration `RegionMovementTracker`; protection anti-boucle necessaire |
| `teleport-on-exit` | absent | oui | Meme chantier que `teleport-on-entry` |
| `command-on-entry` | absent | oui | String/list flag; execution joueur a l'entree |
| `command-on-exit` | absent | oui | String/list flag; execution joueur a la sortie |
| `console-command-on-entry` | absent | oui, avec prudence | Puissant mais sensible; placeholders stricts et permission admin only |
| `console-command-on-exit` | absent | oui, avec prudence | Meme risque securite |
| `walk-speed` | absent | oui | Double flag; restaurer la vitesse precedente en sortie |
| `fly-speed` | absent | oui | Double flag; restaurer la vitesse precedente |
| `keep-inventory` | deja present | non | zRegions preserve inventaire + XP; ExtraFlags separe XP |
| `keep-exp` | partiel | oui | zRegions n'a pas le flag separe: `keep-inventory` garde XP, `exp-drop` controle les orbs |
| `chat-prefix` | absent | plus tard | Depend fortement des plugins chat; possible via integration/format hook |
| `chat-suffix` | absent | plus tard | Meme remarque |
| `godmode` | present sous `invincible` | non | Equivalent fonctionnel |
| `blocked-effects` | absent | oui | Liste d'effets potion refuses/retieres en region |
| `respawn-location` | absent | oui | Location flag + `PlayerRespawnEvent`; attention Essentials/spawn plugins |
| `worldedit` | absent | oui | Hook WorldEdit/FAWE optionnel; pas dans `common` pur |
| `give-effects` | absent | oui | Liste d'effets appliques en region + restauration propre en sortie |
| `fly` | deja present | non | zRegions bloque le demarrage du vol; ExtraFlags peut activer/desactiver le vol a l'entree |
| `play-sounds` | absent | oui | Sound/list flag, one-shot ou repeat; stop en sortie |
| `frostwalker` | absent | oui | Peut rejoindre `frosted-ice-form`, mais le nom est plus parlant cote enchant |
| `nether-portals` | absent | oui | Creation de portails Nether, pas seulement utilisation |
| `glide` | partiel | oui si effet actif souhaite | zRegions a `elytra` pour bloquer le glide, mais ne donne pas l'effet glide |
| `chunk-unload` | absent | a etudier | Tres technique; risque memoire si mal utilise |
| `item-durability` | absent | oui | Annuler l'usure d'items dans la region |
| `join-location` | absent | oui | Teleport a la connexion si le joueur est dans la region |

WorldGuard ExtraFlags Plus ajoute ou remet en avant d'autres flags absents/interessants:

| Flag ExtraFlags Plus | Etat zRegions | Notes |
|---|---|---|
| `disable-completely` | absent | Bloque l'utilisation complete de certains items/actions; demande un modele item/action |
| `disable-throw` | absent | Bloque uniquement les lancers: egg, snowball, pearl, XP bottle, etc. |
| `entry-min-level` | absent | Condition d'entree basee sur niveau XP minimum |
| `entry-max-level` | absent | Condition d'entree basee sur niveau XP maximum |
| `villager-trade` | deja propose | Tres bon candidat P1 |
| `disable-collision` | absent | Collision joueur/entite; possiblement scoreboard/team ou Paper API |
| `deny-item-drops` | present sous `item-drop` | Equivalent proche |
| `deny-item-pickup` | present sous `item-pickup` | Equivalent proche |
| `allow-block-place` | absent | Material allowlist pour `block-place` |
| `deny-block-place` | absent | Material denylist pour `block-place` |
| `allow-block-break` | absent | Material allowlist pour `block-break` |
| `deny-block-break` | absent | Material denylist pour `block-break` |
| `mace` / item blockers modernes | absent | Peut rentrer dans `blocked-items` ou `disable-completely` |
| `wind-charge` | absent | Peut rentrer dans `disable-throw` ou `projectile-launch` |
| `totem` | deja present | zRegions peut deja refuser le totem |

Synthese ExtraFlags: les plus gros manques zRegions sont les flags d'action a
l'entree/sortie (`teleport-*`, `command-*`), les effets persistants (`give-effects`,
`blocked-effects`, `play-sounds`), les locations (`respawn-location`, `join-location`),
les vitesses (`walk-speed`, `fly-speed`), `worldedit`, `nether-portals`,
`item-durability`, et les allow/deny lists de blocks/items.

## Priorite 1 - Tres utiles et raisonnables

### `ride`

- Type: state, defaut `allow`.
- Effet: empeche de monter dans un vehicule ou sur une entite montable: cheval,
  bateau, minecart, cochon, strider, lama, etc.
- Pourquoi: WorldGuard a un flag `ride`; GriefDefender separe aussi les actions de
  riding. C'est courant pour les spawns, hubs et zones d'exposition.
- Events Bukkit: `VehicleEnterEvent`, `EntityMountEvent` si disponible en Spigot
  1.20.4, avec fallback selon API.
- Import WorldGuard: `ride` -> `ride`.
- Notes: ne pas confondre avec `vehicle-place`/`vehicle-destroy`; ici on bloque
  l'utilisation, pas la creation/destruction.

### `sleep`

- Type: state, defaut `allow`.
- Effet: empeche de dormir dans un lit dans la region.
- Pourquoi: present dans WorldGuard; utile pour hotels, donjons, hubs, events.
- Events Bukkit: `PlayerBedEnterEvent`.
- Import WorldGuard: `sleep` -> `sleep`.

### `respawn-anchor`

- Type: state, defaut `allow`.
- Effet: empeche la charge ou l'utilisation des respawn anchors.
- Pourquoi: WorldGuard expose `respawn-anchors`; c'est important en Nether/hubs et
  pour eviter des points de respawn non voulus.
- Events Bukkit: `PlayerInteractEvent` sur `RESPAWN_ANCHOR`; distinguer charge avec
  glowstone et activation.
- Import WorldGuard: `respawn-anchors` -> `respawn-anchor`.

### `item-frame-rotation`

- Type: state, defaut `allow`.
- Effet: empeche la rotation d'un item dans un item frame.
- Pourquoi: WorldGuard le gere; zRegions protege deja la casse/pose des frames, mais
  pas la rotation. Petit ajout, gros confort anti-troll.
- Events Bukkit: `PlayerInteractEntityEvent` ou `PlayerInteractAtEntityEvent` sur
  `ItemFrame`.
- Import WorldGuard: `item-frame-rotation` -> `item-frame-rotation`.

### `firework-damage`

- Type: state, defaut `allow`.
- Effet: empeche les degats causes par les feux d'artifice.
- Pourquoi: present dans WorldGuard; utile dans les zones PVP-off ou cosmiques ou les
  feux d'artifice restent decoratifs.
- Events Bukkit: `EntityDamageByEntityEvent`, damager `Firework` ou projectile/source
  liee selon version.
- Notes: a resoudre cote victime, comme `mob-damage`, sans bypass si l'objectif est
  de proteger le joueur; a trancher avant implementation.

### `deny-message`

- Type: string, defaut vide.
- Effet: message MiniMessage custom envoye quand une action est refusee dans cette
  region; fallback sur `Message.ACTION_DENIED`.
- Pourquoi: WorldGuard propose `deny-message`; tres utile pour expliquer les regles
  d'un spawn, d'une mine, d'une prison, etc.
- Implementation: remplacer `sendDeniedMessage(player)` par une methode qui recoit la
  location et resout `deny-message` avant le fallback.
- Notes: probablement flag positionnel, pas region-scoped, contrairement a
  `greeting`/`farewell`.
- Import WorldGuard: `deny-message` -> `deny-message`.

### `command-whitelist`

- Type: list string, defaut liste vide.
- Effet: si la liste est non vide, seuls ces roots de commandes sont autorises dans
  la region.
- Pourquoi: WorldGuard a `allowed-cmds`; zRegions a deja `command-blacklist`, donc le
  parser `StringListFlag` et la logique de root command existent.
- Semantique proposee: bypass exempt; `command-blacklist` gagne sur la whitelist si
  les deux sont definies.
- Import WorldGuard: `allowed-cmds` -> `command-whitelist`.

### `use-anvil`

- Type: state, defaut `allow`.
- Effet: empeche l'utilisation des anvils.
- Pourquoi: WorldGuard, Residence et BentoBox exposent ce controle. Utile pour hubs,
  maps aventure et economies qui veulent limiter la reparation/renommage.
- Events Bukkit: `PrepareAnvilEvent` n'est pas cancellable; preferer
  `InventoryOpenEvent` sur `InventoryType.ANVIL` et/ou `PlayerInteractEvent` sur
  `ANVIL`, `CHIPPED_ANVIL`, `DAMAGED_ANVIL`.
- Import WorldGuard: `use-anvil` -> `use-anvil`.

### `beacon`

- Type: state, defaut `allow`.
- Effet: empeche l'ouverture ou le changement d'effet d'un beacon.
- Pourquoi: Residence, Lands/BentoBox segmentent ce type d'interaction; aujourd'hui
  `container-access` peut couvrir l'ouverture selon Bukkit, mais un flag dedie est
  plus lisible.
- Events Bukkit: `InventoryOpenEvent`, `InventoryClickEvent`, `PlayerInteractEvent`.

### `villager-trade`

- Type: state, defaut `allow`.
- Effet: empeche le commerce avec les villagers et wandering traders.
- Pourquoi: Lands a `INTERACT_VILLAGER`, Residence a `trade`; tres demande en zones
  shops, halls de villagers et maps aventure.
- Events Bukkit: `PlayerInteractEntityEvent` / `InventoryOpenEvent` merchant.

### `shear`

- Type: state, defaut `allow`.
- Effet: empeche de tondre moutons, mooshrooms, snow golems, bogged, etc. selon API.
- Pourquoi: Lands et Residence exposent cette action; GriefPrevention cite la
  protection des animaux au-dela du simple kill.
- Events Bukkit: `PlayerShearEntityEvent`.

### `leash`

- Type: state, defaut `allow`.
- Effet: empeche d'attacher/detacher des entites avec une laisse.
- Pourquoi: Residence expose `leash`; GriefPrevention protege contre le fait de
  lurer/voler des animaux.
- Events Bukkit: `PlayerLeashEntityEvent`, `EntityUnleashEvent` avec cause joueur si
  disponible; sinon `PlayerInteractEntityEvent` fallback.

### `animal-breeding`

- Type: state, defaut `allow`.
- Effet: empeche la reproduction d'animaux dans la region.
- Pourquoi: BentoBox expose `BREEDING`; utile pour farms publiques et limites
  d'entites.
- Events Bukkit: `EntityBreedEvent`.

### `harvest`

- Type: state, defaut `allow`.
- Effet: empeche la recolte sans casser la capacite generale de construire: crops
  matures, baies, sweet berries, cave vines, cocoa, pumpkins/melons selon choix.
- Pourquoi: Lands separe `PLANT` et `HARVEST`; c'est plus ergonomique pour fermes
  communautaires que `block-break`.
- Events Bukkit: souvent `BlockBreakEvent` + material filter; certains cas passent
  par `PlayerInteractEvent`.

### `plant`

- Type: state, defaut `allow`.
- Effet: empeche de planter graines, saplings, fleurs, crops, champignons, etc. sans
  bloquer tout `block-place`.
- Pourquoi: Lands le met au premier niveau des flags de role.
- Events Bukkit: `BlockPlaceEvent` + material/category filter; bon candidat pour une
  abstraction `MaterialClassifier` cote bukkit.

### `entity-interact`

- Type: state, defaut `allow`.
- Effet: controle les interactions non couvertes avec entites: allay item give/take,
  armor stand deja couvert, mobs d'exposition, etc.
- Pourquoi: BentoBox a des flags dedies (`ALLAY`, `AXOLOTL_SCOOPING`) et
  GriefDefender a un modele source/target; un flag general limite l'explosion du
  catalogue.
- Notes: les flags dedies restent meilleurs pour les cas courants (`villager-trade`,
  `shear`, `leash`, `ride`).

### `sign-edit`

- Type: state, defaut `allow`.
- Effet: empeche l'edition des panneaux, y compris les panneaux editables modernes.
- Pourquoi: WorldGuard mentionne la protection des sign changes dans son scope;
  essentiel pour shops, infos et panneaux d'admin.
- Events Bukkit: `SignChangeEvent`, `PlayerSignOpenEvent` si disponible selon API.

### `projectile-launch`

- Type: state, defaut `allow`.
- Effet: empeche de tirer arc/arbalete/trident/snowball/egg/fireball dans une region.
- Pourquoi: Residence a `shoot`; WorldGuard protege certains impacts/projections.
- Events Bukkit: `ProjectileLaunchEvent`, source joueur.

### `projectile-impact`

- Type: state, defaut `allow`.
- Effet: empeche les projectiles d'agir a l'impact dans la region: boutons, targets,
  pressure plates, potions selon exclusions.
- Pourquoi: utile en spawn et maps aventure. A separer de `projectile-launch`: on
  peut autoriser le tir depuis dehors mais bloquer les effets dedans.
- Events Bukkit: `ProjectileHitEvent` + cas specifiques.

### `fishing-hook`

- Type: state, defaut `allow`.
- Effet: empeche de tirer des entites/items avec la canne a peche.
- Pourquoi: Residence expose `hook`; tres utile contre les vols d'items ou le
  deplacement d'animaux/villagers.
- Events Bukkit: `PlayerFishEvent`.

## Priorite 2 - Parite WorldGuard / controle monde

### `lightning`

- Type: state, defaut `allow`.
- Effet: empeche la foudre de frapper dans la region.
- Events Bukkit: `LightningStrikeEvent`.
- Notes: verifier l'interaction avec `CreatureSpawnEvent.SpawnReason.LIGHTNING`
  deja listee dans `mob-spawning`.

### `lava-fire`

- Type: state, defaut `allow`.
- Effet: empeche la lave d'allumer un feu.
- Pourquoi: WorldGuard separe `lava-fire` de `fire-spread`; zRegions fusionne
  aujourd'hui les ignitions non joueur dans `fire-spread`.
- Events Bukkit: `BlockIgniteEvent` avec cause liee a lava.

### `water-flow` et `lava-flow`

- Type: state, defaut `allow`.
- Effet: version separee de `fluid-flow`.
- Pourquoi: WorldGuard distingue les deux; utile pour autoriser l'eau dans une ferme
  mais bloquer la lave en zone build.
- Strategy: garder `fluid-flow` comme flag general, puis laisser le flag specifique
  surcharger si defini. Necessite une regle claire dans la doc.

### `snow-fall`, `snow-melt`, `ice-form`, `ice-melt`, `frosted-ice-form`, `frosted-ice-melt`

- Type: state, defaut `allow`.
- Effet: controle la neige/glace et le frost walker.
- Pourquoi: parite WorldGuard et tres utile pour maps custom.
- Events Bukkit: `BlockFormEvent`, `BlockFadeEvent`, `EntityBlockFormEvent`.
- Notes: attention aux events frequents; rester silencieux et sans message.

### `soil-dry`

- Type: state, defaut `allow`.
- Effet: empeche les farmland de redevenir dirt.
- Pourquoi: utile pour farms, zones decoratives, claims agricoles.
- Events Bukkit: `BlockFadeEvent` ou event exact a verifier sur Spigot 1.20.4.

### `coral-fade`

- Type: state, defaut `allow`.
- Effet: empeche les coraux de mourir hors eau.
- Pourquoi: maps decoratives, aquariums, builds custom.
- Events Bukkit: `BlockFadeEvent`.

### `copper-aging`

- Type: state, defaut `allow`.
- Effet: controle l'oxydation du cuivre.
- Pourquoi: WorldGuard l'appelle `copper-fade`; cote Minecraft, "aging" est plus
  clair pour l'utilisateur.
- Events Bukkit: `BlockFormEvent` / `BlockFadeEvent` selon API exacte.
- Import WorldGuard: `copper-fade` -> `copper-aging`.

### `snowman-trails`

- Type: state, defaut `allow`.
- Effet: empeche les snow golems de poser de la neige au sol.
- Pourquoi: WorldGuard et Residence ont ce controle (`snowman-trails` /
  `snowtrail`).
- Events Bukkit: `EntityBlockFormEvent`.

### `block-spread`

- Type: state, defaut `allow`.
- Effet: controle les propagations de blocs non couvertes par des flags dedies:
  grass, mycelium, sculk, champignons, amethyst/budding-like selon version.
- Pourquoi: Lands a `BLOCK_SPREADING`, Residence a `spread`.
- Notes: garder les flags fins quand ils existent; ce flag general peut etre le
  fallback.

### `fire-burn`

- Type: state, defaut `allow`.
- Effet: empeche le feu de consumer les blocs, sans forcement empecher l'existence du
  feu.
- Pourquoi: BentoBox separe `FIRE_BURNING`, `FIRE_IGNITE`, `FIRE_SPREAD`; zRegions
  fusionne aujourd'hui burn/spread dans `fire-spread`.
- Events Bukkit: `BlockBurnEvent`.

### `bed-anchor-explosion`

- Type: state, defaut `allow`.
- Effet: controle les explosions de lits et respawn anchors, separement de TNT ou
  creepers.
- Pourquoi: BentoBox a `BLOCK_EXPLODE_DAMAGE` pour bed/anchors; WorldGuard classe
  souvent ces explosions dans `other-explosion`.
- Events Bukkit: `BlockExplodeEvent` / damage cause exact a verifier.

## Priorite 3 - Croissance et biomes vivants

Ces flags sont utiles mais peuvent devenir bruyants en hot path. Les regrouper
derriere un listener environnemental bien teste.

### `crop-growth`

- Type: state, defaut `allow`.
- Effet: empeche la pousse des cultures, cactus, canne a sucre, bambou, melon,
  citrouille, etc.
- Events Bukkit: `BlockGrowEvent`, `BlockSpreadEvent`, cas speciaux a lister.
- Lien avec `IDEES.md`: peut evoluer plus tard vers `growth-rate` numerique.

### `tree-growth`

- Type: state, defaut `allow`.
- Effet: empeche les saplings de devenir arbres.
- Pourquoi: WorldGuard couvre ca en partie via croissance; en produit, un flag dedie
  est plus lisible.
- Events Bukkit: `StructureGrowEvent`.

### `mushroom-growth`, `grass-spread`, `mycelium-spread`, `vine-growth`, `rock-growth`, `sculk-growth`

- Type: state, defaut `allow`.
- Effet: controles fins des propagations naturelles.
- Pourquoi: parite WorldGuard et maps custom.
- Strategy: commencer par `crop-growth` + `vine-growth`, puis etendre si demande
  client. Trop de flags d'un coup rend l'editeur moins lisible.

### `bone-meal`

- Type: state, defaut `allow`.
- Effet: empeche l'utilisation de bone meal dans la region.
- Pourquoi: si les admins bloquent la croissance naturelle, ils veulent souvent
  bloquer aussi l'acceleration manuelle.
- Events Bukkit: `PlayerInteractEvent` avec bone meal + `BlockFertilizeEvent` si
  disponible.

### `entity-transform`

- Type: state, defaut `allow`.
- Effet: controle les transformations naturelles ou provoquees: zombie villager,
  drowned, piglin zombification, mooshroom/shear edge cases selon API.
- Pourquoi: WorldGuard evite de bloquer certaines conversions via `mob-spawning`;
  un flag dedie donne un controle explicite sans supprimer l'entite source par
  accident.
- Events Bukkit: `EntityTransformEvent`.

## Priorite 4 - Entites et explosions fines

### `deny-spawn`

- Type: list string ou nouveau `EntityTypeListFlag`.
- Effet: interdit certains types d'entites dans une region.
- Pourquoi: WorldGuard et GriefDefender permettent de cibler les entites; zRegions a
  seulement `mob-spawning` global.
- Syntaxe proposee: `/rg flag spawn deny-spawn cow, pig, zombie`.
- Events Bukkit: `CreatureSpawnEvent`.
- Notes: accepter `minecraft:cow` et `cow`; ignorer les types inconnus au parse doit
  etre refuse, pas silently accepted.

### `entity-damage`

- Type: state, defaut `allow`, ou futur flag cible par type.
- Effet: protege toutes les entites non joueur contre les degats joueur.
- Pourquoi: zRegions a `damage-animals`, mais pas villagers, armor stands hors flag
  dedie, golems, mobs d'exposition.
- Notes: peut remplacer/englober `damage-animals` a terme, mais eviter une migration
  cassante.

### `entity-explosion-damage`

- Type: state, defaut `allow`.
- Effet: empeche les explosions de blesser les entites/joueurs dans la region, sans
  toucher aux blocs.
- Pourquoi: GriefDefender distingue `explosion-block` et `explosion-entity`; zRegions
  distingue deja les explosions de blocs mais pas les degats entites.
- Events Bukkit: `EntityDamageByEntityEvent` / `EntityDamageEvent` cause explosion.

### Explosion family: `creeper-explosion`, `tnt`, `ghast-fireball`, `wither-damage`, `enderdragon-block-damage`

- Type: state, defaut `allow`.
- Effet: granularite par source.
- Pourquoi: WorldGuard les expose; utile pour serveurs survival avances.
- Strategy: garder `entity-explosion` comme general, puis specialiser par source si le
  flag specifique est defini. Documenter la priorite general/specific.

### `animal-spawning`

- Type: state, defaut `allow`.
- Effet: controle le spawn naturel des animaux passifs separement des monstres.
- Pourquoi: Residence, Lands et BentoBox distinguent animaux/monstres; tres utile
  pour farms et iles skyblock.
- Events Bukkit: `CreatureSpawnEvent` + classifier entity category.

### `monster-spawning`

- Type: state, defaut `allow`.
- Effet: controle le spawn naturel des monstres separement des animaux.
- Pourquoi: alternative plus precise a `mob-spawning`.
- Notes: `mob-spawning` peut rester le flag global; la resolution doit definir si un
  flag specifique surcharge le general.

### `spawner-spawning`

- Type: state, defaut `allow`.
- Effet: controle les spawns issus de spawners, separement du naturel.
- Pourquoi: Lands a une option include-spawners; BentoBox separe natural/spawner pour
  animaux et monstres. Utile pour farms admin.
- Events Bukkit: `CreatureSpawnEvent.SpawnReason.SPAWNER`.

### `animal-spawner-spawning` et `monster-spawner-spawning`

- Type: state, defaut `allow`.
- Effet: version fine de `spawner-spawning`.
- Pourquoi: parite BentoBox (`ANIMAL_SPAWNERS_SPAWN`,
  `MONSTER_SPAWNERS_SPAWN`).
- Notes: batch avance seulement si `spawner-spawning` est trop grossier.

### `phantom-spawning`

- Type: state, defaut `allow`.
- Effet: controle specifiquement le spawn des phantoms.
- Pourquoi: Lands et Residence exposent ce cas; tres frequent en survival.
- Events Bukkit: `CreatureSpawnEvent`, entity type `PHANTOM`.

### `slime-spawning`

- Type: state, defaut `allow`.
- Effet: controle le spawn de slimes/magma cubes.
- Pourquoi: les chunks a slime sont un cas economique important; peut eviter de
  bloquer tous les monstres.
- Events Bukkit: `CreatureSpawnEvent`.

### `villager-damage`

- Type: state, defaut `allow`.
- Effet: protege villagers et wandering traders des degats joueur.
- Pourquoi: halls de trade et spawns; `damage-animals` ne couvre pas clairement les
  villagers.
- Events Bukkit: `EntityDamageByEntityEvent`.

### `monster-damage`

- Type: state, defaut `allow`.
- Effet: empeche les joueurs d'attaquer les monstres dans une zone.
- Pourquoi: Lands a `ATTACK_MONSTER`; utile pour expositions, donjons scripts,
  farms publiques.
- Notes: Lands lie ce flag a la protection reciproque contre les monstres; zRegions
  devrait garder `mob-damage` separe pour eviter les surprises.

### `pet-damage`

- Type: state, defaut `allow`.
- Effet: protege les pets apprivoises contre les degats non autorises.
- Pourquoi: GriefPrevention met en avant la protection des pets.
- Events Bukkit: `EntityDamageByEntityEvent`, `Tameable#isTamed`.

### `wither-attack-animal`

- Type: state, defaut `allow`.
- Effet: controle les degats des withers sur animaux.
- Pourquoi: Lands expose ce controle pour les farms.
- Notes: flag tres specialise; a mettre en P4/P5.

### `entity-portal-teleport`

- Type: state, defaut `allow`.
- Effet: empeche les entites non joueur d'utiliser des portails.
- Pourquoi: BentoBox expose ce flag; utile pour fermes et anti-transport de mobs.
- Events Bukkit: `EntityPortalEvent` si present cote Spigot.

### `limit-mobs-to-region`

- Type: state, defaut `deny` ou `allow` a trancher.
- Effet: supprime ou ramene les mobs qui sortent d'une region.
- Pourquoi: BentoBox a `GEO_LIMIT_MOBS`; utile pour iles, arenes et farms.
- Notes: demande un tracker de mouvement entite ou un check periodique; attention aux
  performances.

## Priorite 5 - Experiences joueur / map making

### `gamemode`

- Type: enum string (`survival`, `creative`, `adventure`, `spectator`, vide).
- Effet: applique un gamemode a l'entree, restaure a la sortie si possible.
- Pourquoi: WorldGuard `game-mode`; tres utile pour maps, lobbies, arenas.
- Implementation: necessite un etat par joueur dans `RegionMovementTracker` ou un
  service dedie pour restaurer proprement.
- Risque: conflits avec plugins de minigames et permissions.

### `time-lock`

- Type: integer/string, defaut vide.
- Effet: heure client-only vue par le joueur dans la region.
- Pourquoi: WorldGuard; fort impact ambiance, sans changer le monde.
- Platform abstraction: ajouter `RegionPlayer#setPlayerTime(...)` et reset.

### `weather-lock`

- Type: enum string (`clear`, `rain`, vide).
- Effet: meteo client-only vue par le joueur.
- Platform abstraction: `RegionPlayer#setPlayerWeather(...)` et reset.

### Healing/feeding: `heal-delay`, `heal-amount`, `heal-min-health`, `heal-max-health`, `feed-delay`, `feed-amount`, `feed-min-hunger`, `feed-max-hunger`

- Type: integer/double selon flag.
- Effet: zone hopital, zone poison, zone restauration.
- Pourquoi: WorldGuard; tres populaire pour lobbies RPG et arenas.
- Implementation: nouveau scheduler commun par joueur/region, pas dans un listener
  Bukkit direct. Ajouter `IntegerFlag` et `DoubleFlag`.
- Notes: `hunger` actuel bloque la perte de faim; il ne remplace pas `feed-*`.

### `potion-splash`

- Type: state, defaut `allow`.
- Effet: empeche les potions splash/lingering d'affecter les entites dans la region.
- Pourquoi: WorldGuard; utile en safe zones.
- Events Bukkit: `PotionSplashEvent`, `LingeringPotionSplashEvent`.

### `portal-use`

- Type: state, defaut `allow`.
- Effet: empeche l'utilisation des portails pour entrer/sortir.
- Pourquoi: GriefDefender expose `portal-use`; WorldGuard couvre une partie via
  entry/exit, mais un flag dedie est plus lisible.
- Events Bukkit: `PlayerPortalEvent`.

### `receive-chat`

- Type: state, defaut `allow`.
- Effet: empeche de recevoir le chat serveur pendant que le joueur est dans la
  region.
- Pourquoi: WorldGuard expose `receive-chat`; utile pour zones de tutoriel,
  cinematiques ou salles d'enigmes.
- Implementation: l'envoi chat moderne varie selon plateforme; en Spigot 1.20.4,
  `AsyncPlayerChatEvent` permet de retirer des recipients, mais les plugins de chat
 /proxy peuvent contourner.

### `entry-deny-message` et `exit-deny-message`

- Type: string, defaut vide.
- Effet: message MiniMessage dedie quand `entry` ou `exit` refuse un mouvement.
- Pourquoi: WorldGuard les separe de `deny-message`; meilleure UX pour prisons,
  zones VIP et arenes.
- Implementation: `RegionMovementTracker.checkMove` doit renvoyer la region/flag qui
  a refuse, pas seulement un booleen.

### `notify-enter` et `notify-leave`

- Type: boolean/state, defaut `deny` ou `false`.
- Effet: notifie les joueurs/staff avec permission quand quelqu'un entre/sort.
- Pourquoi: WorldGuard; utile pour staff zones et arenes.
- Notes: probablement lie a une permission `zregions.notify`.

### `farewell-title`

- Type: string, defaut vide.
- Effet: titre affiche a la sortie d'une region.
- Pourquoi: WorldGuard propose `farewell-title`; zRegions a deja `title`/`subtitle`
  pour l'entree mais pas l'equivalent sortie.
- Notes: soit ajouter `farewell-title`/`farewell-subtitle`, soit generaliser
  `title` vers `enter-title`.

### `teleport`

- Type: location, defaut vide.
- Effet: destination de `/rg teleport <region>`.
- Pourquoi: WorldGuard et Residence ont une notion de teleport de region.
- Implementation: demande `LocationFlag` + commande `/rg teleport`.
- Lien: idee deja presente dans `IDEES.md`.

### `spawn`

- Type: location, defaut vide.
- Effet: point de respawn si le joueur meurt dans la region.
- Pourquoi: WorldGuard et BentoBox ont des flags/settings de respawn.
- Implementation: `PlayerRespawnEvent` + resolution region-scoped; attention aux
  conflits avec beds/anchors et plugins de spawn.

### `teleport-message`

- Type: string, defaut vide.
- Effet: message affiche quand un joueur est teleporte via le flag `teleport`.
- Pourquoi: WorldGuard; utile si `/rg teleport` est ajoute.

### `spawn-teleport`

- Type: state, defaut `allow`.
- Effet: autorise/refuse l'utilisation de la teleportation vers le spawn de region
  selon le role/target.
- Pourquoi: Lands a `SPAWN_TELEPORT`; utile si zRegions ouvre `/rg teleport` aux
  joueurs.

### `exit-via-teleport`

- Type: state, defaut `allow`.
- Effet: permet de quitter une region par teleport meme si `exit` est deny.
- Pourquoi: WorldGuard expose ce compromis; evite de pieger des joueurs tout en
  bloquant la marche.

### `exit-override`

- Type: boolean, defaut `false`.
- Effet: autorise toujours la sortie, meme si une autre region/parent aurait bloque.
- Pourquoi: WorldGuard; utile pour eviter les pieges de claims.
- Notes: impact direct sur l'algorithme de resolution movement.

### `move`

- Type: state, defaut `allow`.
- Effet: empeche les deplacements dans la region apres entree.
- Pourquoi: Residence a `move`; utile pour jails, cinematiques, zones de freeze.
- Notes: potentiellement frustrant; doit avoir une logique anti-softlock et bypass.

### `biome`

- Type: enum string ou namespaced key, defaut vide.
- Effet: change le biome client/monde dans la region.
- Pourquoi: GriefPreventionFlags avait `ChangeBiome`; utile pour claims premium ou
  maps. Mais casse selon versions.
- Implementation: tres platform-specific; probablement pas P1.

### `visitor-invincible`

- Type: state, defaut `deny`.
- Effet: rend les visiteurs invincibles dans la region, sans forcement proteger les
  membres.
- Pourquoi: BentoBox a `INVINCIBLE_VISITORS`; pratique pour iles visitables et shops.
- Notes: zRegions a deja `invincible` avec targets; ce flag pourrait etre inutile si
  la doc met en avant `/rg flag shop invincible allow -t visitor`.

## Catalogue etendu de flags candidats

Cette section vise le maximum d'idees exploitables. Elle inclut volontairement des
flags qui se recoupent avec des flags actuels; au moment d'implementer, il faudra
choisir entre garder un flag general, ajouter un flag fin, ou creer une priorite
general -> specifique.

### Interactions blocs fines

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `door-use` | state | Lands, Residence, PlotSquared | `PlayerInteractEvent`; portes + fence gates |
| `trapdoor-use` | state | Lands | Peut etre fusionne avec `door-use` |
| `button-use` | state | Residence, PlotSquared | Sous-ensemble de `interact` |
| `lever-use` | state | Residence, PlotSquared | Sous-ensemble de `interact` |
| `pressure-plate-use` | state | Residence | `Action.PHYSICAL`, attention mobs/joueurs |
| `tripwire-use` | state | PlotSquared | Peut rejoindre `mechanism-use` |
| `mechanism-use` | state | Lands | Levers/buttons/plates/repeaters/comparators |
| `repeater-use` | state | Residence `diode` | Changement de delay/lock |
| `note-block-use` | state | Residence | `PlayerInteractEvent` |
| `jukebox-use` | state | BentoBox-like | Interaction disque |
| `lectern-use` | state | Bukkit modern | Lecture/changement livre |
| `crafting-table-use` | state | Residence `table` | Ouvre crafting table |
| `craft` | state | Residence | Flag large: crafting table + autres postes |
| `brewing-stand-use` | state | Residence, BentoBox | Aujourd'hui container-access peut couvrir |
| `enchant-table-use` | state | Residence | `InventoryOpenEvent` |
| `grindstone-use` | state | CMI/BentoBox-like | `InventoryType.GRINDSTONE` |
| `smithing-table-use` | state | Modern MC | `InventoryType.SMITHING` |
| `stonecutter-use` | state | Modern MC | `InventoryType.STONECUTTER` |
| `cartography-table-use` | state | Modern MC | `InventoryType.CARTOGRAPHY` |
| `loom-use` | state | Modern MC | `InventoryType.LOOM` |
| `composter-use` | state | Modern MC | Interact on composter |
| `cauldron-use` | state | Modern MC | Eau/poudreuse/potions selon version |
| `cake-eat` | state | Residence | Interaction bloc cake |
| `ender-chest-use` | state | BentoBox | Peut rester dans container-access ou etre separe |
| `hopper-use` | state | BentoBox break/use hoppers | Interactions + transfert possible |
| `break-hoppers` | state | BentoBox | Surcharge de `block-break` |
| `break-spawners` | state | BentoBox | Surcharge de `block-break`, economie |
| `place-spawners` | state | Economies survival | Surcharge de `block-place` |

### Inventaires, items et transferts

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `inventory-move` | state | GriefDefender/GPFlags-like | Hoppers/minecarts; risque perf |
| `hopper-transfer` | state | Protection plugins | `InventoryMoveItemEvent`, frequent |
| `drop-on-death` | state | GP/BentoBox death loot | Different de `keep-inventory` et `exp-drop` |
| `death-item-pickup` | state | GriefPrevention | Empeche voler le stuff de mort |
| `item-despawn` | state | Map making | `ItemDespawnEvent` |
| `item-merge` | state | Perf/map making | `ItemMergeEvent` |
| `item-frame-damage` | state | BentoBox | Non-player damage aux item frames |
| `painting-damage` | state | WorldGuard pair | Non-player damage aux paintings |
| `item-rename` | state/string-regex | CMI inspiration | Anvil rename, utile anti-insultes |
| `item-enchant` | state | Map/economie | Enchanting table + anvil books |
| `item-repair` | state | Economie | Anvil/grindstone/mending? a preciser |

### Entites et animaux

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `allay-interact` | state | BentoBox | Donner/prendre item a un allay |
| `axolotl-scoop` | state | BentoBox | Bucket sur axolotl |
| `fish-scoop` | state | Bukkit survival | Bucket poissons/tadpoles |
| `entity-bucket-scoop` | state | Generalisation | Axolotl/fish/tadpole |
| `dye-animal` | state | Residence `dye` | Moutons, collars selon choix |
| `tame-animal` | state | GriefPrevention scope | `EntityTameEvent` |
| `sit-pet` | state | Pet protection | Interaction sit/stand |
| `milk` | state | Survival farms | `PlayerInteractEntityEvent` cow/goat |
| `collect-honey` | state | Farms | Bee nests/hives bottle/shears |
| `villager-claim-workstation` | state | Villager halls | Difficile: POI API Bukkit limitee |
| `entity-transform` | state | WorldGuard caution | Voir P3 |
| `entity-teleport` | state | Endermen/shulkers? | `EntityTeleportEvent` |
| `entity-target` | state | Safe zones | `EntityTargetLivingEntityEvent` |
| `entity-ai` | state | Map making | Potentiellement lourd/invasif |

### Combat et degats

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `friendly-fire` | state | Towny | Demande notion ally/nation/role futur |
| `projectile-pvp` | state | PVP granularite | Separer melee/projectile |
| `melee-pvp` | state | PVP granularite | Separer melee/projectile |
| `potion-pvp` | state | WorldGuard potion-splash | Effets negatifs entre joueurs |
| `fire-damage` | state | Map making | Damage cause fire/lava distinct |
| `lava-damage` | state | Map making | Peut etre separe de fire |
| `drowning-damage` | state | Map making | Damage cause drowning |
| `suffocation-damage` | state | Map making | Safe zones |
| `void-damage` | state | Skyblock | Peut casser gameplay si mal utilise |
| `freeze-damage` | state | Powder snow | Modern MC |
| `starvation-damage` | state | Alternative a hunger | `EntityDamageEvent.STARVATION` |
| `explosion-entity-damage` | state | GriefDefender/BentoBox | Voir P4 |
| `wither-effect` | state | Arenas | Potion/effect cause |
| `poison-effect` | state | Arenas | Potion/effect cause |

### Spawns et limites d'entites

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `natural-spawning` | state | BentoBox/Lands | SpawnReason natural only |
| `plugin-spawning` | state | WorldGuard mob-spawning broad | Attention plugins externes |
| `egg-spawning` | state | WorldGuard broad | Spawn eggs |
| `command-spawning` | state | WorldGuard broad | Spawns commandes/plugins |
| `breeding-spawning` | state | Bukkit spawn reason | Peut croiser `animal-breeding` |
| `raid-spawning` | state | Vanilla villages | SpawnReason raid/patrol |
| `patrol-spawning` | state | Vanilla patrols | SpawnReason patrol |
| `portal-spawning` | state | Nether portals | Zombie piglins, etc. |
| `mob-cap` | integer | BentoBox `LIMIT_MOBS` | Nombre max d'entites dans region |
| `mob-cap-by-type` | map/list | GriefDefender contexts | Ex: `cow=20,zombie=5` |
| `spawn-rate` | double | `IDEES.md` | Multiplicateur naturel |
| `spawner-rate` | double | `IDEES.md` | Multiplicateur spawners |

### Redstone, pistons et technique

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `redstone-clock` | state/integer | RedProtect | Detecter/desactiver clocks par region |
| `redstone-clock-limit` | integer | RedProtect | Updates max par fenetre |
| `dispenser-use` | state | Mechanism protection | `BlockDispenseEvent` |
| `dropper-use` | state | Mechanism protection | Souvent meme event |
| `tnt-cannon` | state | WG/GP scope | Difficile attribution/domaine |
| `sand-cannon` | state | GP scope | FallingBlock entrant en region |
| `falling-block-enter` | state | Anti-grief | `EntityChangeBlockEvent` / falling block |
| `piston-cross-border` | state | Lands piston_griefing | Plus fin que `piston` |
| `nonplayer-domain` | string list | WorldGuard | Voir "a ne pas ajouter" |

### Mouvement, acces et softlock

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `teleport-in` | state | WG entry + teleport | Bloque seulement arrivees teleport |
| `teleport-out` | state | WG exit-via-teleport inverse | Bloque sorties teleport |
| `portal-in` | state | Portal granularite | Destination dans region |
| `portal-out` | state | Portal granularite | Depart depuis region |
| `vehicle-enter` | state | BentoBox boat/riding | Plus fin que `ride` |
| `vehicle-exit` | state | Jails/maps | Attention softlocks |
| `boat-use` | state | BentoBox | Place/break/enter si voulu |
| `minecart-use` | state | Bukkit transport | Peut inclure ride/place/destroy |
| `elytra-boost` | state | Firework boost | Complement de `elytra` |
| `riptide` | state | Movement combat | Trident riptide |
| `swim` | state | Map making | Difficile a rendre propre |
| `crawl` | state | Map making | Difficile |

### Chat, commandes et communication

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `chat-format` | string | Map making | Prefix/suffix par region, depend plugins chat |
| `chat-channel` | string | RPG/roleplay | Region-local chat |
| `command-cooldown` | map/timed | PlotSquared timed type | Ex: `spawn=30,tp=10` |
| `command-on-enter` | string list | PlotSquared-like | Commandes console/player a l'entree |
| `command-on-exit` | string list | PlotSquared-like | Commandes console/player a la sortie |
| `console-command-on-enter` | string list | Servers RPG | Dangereux, permission stricte |
| `console-command-on-exit` | string list | Servers RPG | Dangereux, permission stricte |
| `tab-complete` | state | Anti-cheat/security | Bukkit/Paper differences |

### Map making, ambience et etat joueur

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `walk-speed` | double | RPG zones | Platform abstraction player speed |
| `fly-speed` | double | RPG zones | Restaurer proprement |
| `gravity` | state | Bukkit attribute? | Version dependent |
| `glow` | enum/string | CMI style | Team/metadata conflicts |
| `bossbar` | string | Region ambience | Needs service common |
| `action-bar-repeat` | string/integer | zRegions action-bar once today | Repeating messages |
| `potion-effects` | string list | Map making | `speed:1,invisibility:0` |
| `clear-effects` | state/list | Arenas | Restore? |
| `experience-multiplier` | double | RPG/economy | XP pickup/drop modify |
| `block-drops` | state | WG-like addons | Deny block item drops |
| `mob-drops` | state | Farms/economy | `EntityDeathEvent` |
| `player-drops` | state | Death rules | Alternative a keep-inventory |
| `keep-level` | state | Death rules | Separer XP level/items |

### Meteo, temps et monde visible

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `day` | state/time | Residence | Client time or world time? |
| `night` | state/time | Residence | Preferer `time-lock` |
| `sun` | state/weather | Residence | Preferer `weather-lock clear` |
| `rain` | state/weather | Residence | Preferer `weather-lock rain` |
| `thunder` | state/weather | Vanilla | Client weather possible |
| `title-hide` | boolean | Lands | Cache les titres d'entree |
| `enter-exit-messages` | boolean | BentoBox | Toggle global par region |

### Claim/region management flags

Ces flags viennent surtout de Lands/Towny/BentoBox. Ils ne protegent pas un event
Minecraft, mais deleguent la gestion d'une region. zRegions est aujourd'hui plutot
admin-command-driven, donc ce serait un chantier produit separe.

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `member-add` | state | Lands `PLAYER_TRUST` | Autoriser un owner a trust |
| `member-remove` | state | Lands `PLAYER_UNTRUST` | Role priority necessaire |
| `member-role-set` | state | Lands `PLAYER_SETROLE` | Requiert roles dynamiques |
| `member-ban` | state | Lands `PLAYER_BAN` | Region ban + entry deny |
| `flag-edit` | state | Residence `admin`, Lands settings | Deleguer `/rg flag` |
| `priority-edit` | state | Region management | Risque fort |
| `parent-edit` | state | Region management | Risque cycles/heritage |
| `region-redefine` | state | Region management | Tres sensible |
| `region-rename` | state | Region management | API/stockage |
| `spawn-set` | state | Lands | Si `teleport` flag arrive |
| `claim-create` | state | Lands/Towny world toggles | Utile global region |
| `claim-border` | state | Lands | Ignore distance entre claims |
| `balance-withdraw` | state | Lands | Seulement si economie region |

### Flags a valeur complexe

| Flag propose | Type | Inspiration | Remarque implementation |
|---|---|---|---|
| `allowed-items` | material list | GPFlags/GriefDefender context | Autorise seulement certains items |
| `blocked-items` | material list | GPFlags/GriefDefender context | Bloque certains items |
| `allowed-blocks-place` | material list | PlotSquared BlockTypeList | Surcharge `block-place` |
| `blocked-blocks-place` | material list | PlotSquared BlockTypeList | Surcharge `block-place` |
| `allowed-blocks-break` | material list | PlotSquared BlockTypeList | Surcharge `block-break` |
| `blocked-blocks-break` | material list | PlotSquared BlockTypeList | Surcharge `block-break` |
| `allowed-entities-interact` | entity list | GriefDefender context | Source/target simplifie |
| `blocked-entities-interact` | entity list | GriefDefender context | Source/target simplifie |
| `entity-source-rules` | rule list | GriefDefender | Nouveau modele, tres avance |
| `material-source-rules` | rule list | GriefDefender | Nouveau modele, tres avance |

## Synthese des ajouts les plus rentables

Si l'objectif est d'augmenter rapidement le nombre de flags sans gros changement de
modele, le meilleur batch serait:

1. `ride`, `sleep`, `respawn-anchor`, `item-frame-rotation`, `use-anvil`,
   `villager-trade`, `shear`, `leash`, `animal-breeding`, `beacon`.
2. `command-whitelist`, `deny-message`, `entry-deny-message`, `exit-deny-message`,
   `receive-chat`.
3. `lightning`, `lava-fire`, `water-flow`, `lava-flow`, `fire-burn`,
   `snowman-trails`, `soil-dry`, `coral-fade`, `copper-aging`.
4. `crop-growth`, `tree-growth`, `vine-growth`, `grass-spread`,
   `mycelium-spread`, `sculk-growth`, `bone-meal`.
5. `animal-spawning`, `monster-spawning`, `spawner-spawning`, `phantom-spawning`,
   `deny-spawn`.

Cela ajouterait environ 35 flags avec des listeners Bukkit relativement directs et
une bonne valeur commerciale. Les flags de map making persistants (`gamemode`,
`time-lock`, `weather-lock`, heal/feed, potion effects) sont tres attractifs, mais
ils meritent un chantier a part car ils doivent restaurer l'etat joueur sans conflit
avec les autres plugins.

## A ne pas ajouter tout de suite

- `passthrough` / `build`: zRegions a choisi des flags allow par defaut et reproduit
  l'implicite WorldGuard seulement a l'import. Ajouter `build` changerait le modele
  mental et risque de rendre la resolution moins claire.
- `nonplayer-protection-domains`: interessant pour pistons/TNT transfrontaliers, mais
  complexe et tres specifique WorldGuard. A garder pour plus tard si les clients le
  demandent.
- Flags GriefDefender avec contexte `used_item`, `source`, `target` dynamique: tres
  puissants mais impliquent un nouveau modele de flag/context, pas seulement un ajout
  au catalogue.
- `inventory-item-move`: utile pour hoppers, mais GriefDefender note lui-meme le cout
  perf. A etudier avec benchmarks avant livraison.

## Types de flags a ajouter avant plusieurs chantiers

- `IntegerFlag`: parse base 10, bornes optionnelles selon flag.
- `DoubleFlag`: parse invariant locale, bornes optionnelles.
- `EnumFlag`: parser generique pour petites enums (`gamemode`, `weather`).
- `LocationFlag`: necessaire pour `teleport`/`spawn` si ces flags sont repris.
- `StringListFlag` existe deja et suffit pour `command-whitelist`; pour
  `deny-spawn`, preferer un type dedie qui valide les entites Bukkit cote plateforme
  ou via une liste abstraite exposee par `common`.

## Plan d'implementation recommande

1. Livrer un petit batch P1: `ride`, `sleep`, `respawn-anchor`,
   `item-frame-rotation`, `deny-message`, `command-whitelist`.
2. Ajouter les imports WorldGuard correspondants et des tests d'import.
3. Mettre a jour `FlagsTest` avec le nouveau compteur, `DOCUMENTATION.md`, les
   `messages.yml` si necessaire, et le GUI flags zMenu si l'affichage depend du type.
4. Livrer un batch environnement P2/P3 avec tests ciblant `EnvironmentProtectionListener`.
5. Garder les flags a etat persistant joueur (`gamemode`, `time-lock`,
   `weather-lock`, heal/feed) pour un chantier dedie.
