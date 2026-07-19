# zRegions — Boîte à idées

> Backlog libre d'idées de fonctionnalités, sans engagement de roadmap. Une idée retenue
> passe dans `PLAN-IMPLEMENTATION.md` (et, une fois livrée, dans `DOCUMENTATION.md`).

## Taux de spawn des entités configurable par région

Pouvoir modifier les valeurs de spawn des entités dans une région, pour **augmenter ou
réduire** :

- le **spawn naturel** ;
- le **spawn des spawners**.

Tout doit être configurable (par région, et idéalement par type d'entité).

*Pistes techniques* : aujourd'hui `mob-spawning` est binaire (allow/deny). Il faudrait des
flags à valeur **numérique** (multiplicateur ou pourcentage, ex. `spawn-rate: 0.5`,
`spawner-rate: 2.0`) — donc un nouveau type `Flag<Double>` dans le pipeline existant
(parse/serialize le gèrent déjà génériquement). Côté enforcement :
`CreatureSpawnEvent` avec `SpawnReason.NATURAL` vs `SPAWNER`, et une réduction se fait en
annulant une fraction des spawns (tirage aléatoire contre le taux) ; une augmentation est
plus délicate (re-spawn actif ou manipulation du spawn limit — à creuser).

## GUI : un material différent par flag dans l'éditeur de flags

Chaque flag du menu des flags devrait avoir son propre item (ex. `pvp` →
DIAMOND_SWORD, `block-break` → IRON_PICKAXE, `chat` → PAPER…), au lieu du
NAME_TAG uniforme.

*Pistes techniques* : table de correspondance flag → material par défaut dans le
hook, surchargée par une section `materials:` dans `flags.yml` (clé de flag →
material) pour rester personnalisable ; fallback sur l'item du bouton pour les
flags inconnus (addons).

## Téléportation vers une région (commande + bouton)

`/rg teleport <region>` (alias `tp`) et un bouton dans le menu d'une région.

*Pistes techniques* : `RegionPlayer.teleport` existe déjà ; cible = centre de la
bounding box. Trouver une position **sûre** (Y praticable, pas dans un bloc)
demande une lecture du monde → nouvelle abstraction plateforme (ex.
`findSafeSpot(RegionLocation)` implémentée côté bukkit). Région globale : refuser
(pas de shape). Permission dédiée `zregions.teleport` ou admin.

## GUI : boutons +/- de priorité dans le menu d'une région

Un bouton pour augmenter/diminuer la priorité (clic gauche +1, clic droit −1,
shift ±10 ?) — `setPriority` existe déjà, re-render après clic.

## GUI : liste des régions groupée par monde + visuel de priorité

Le menu des régions doit d'abord lister **les mondes**, puis les régions du
monde cliqué. La priorité doit se voir d'un coup d'œil : la **taille du stack**
de l'item = la priorité (priorité 4 → 4 items ; clamp 1–64, priorité ≤ 0 → 1).

*Pistes techniques* : deux inventaires (`worlds.yml` + `regions.yml` filtré par
monde) ; le monde géré rejoint l'état par joueur du service (comme la région
gérée) ; `ItemStack.setAmount` après `build(...)` dans le bouton paginé.

## GUI : item personnalisé par région

Pouvoir changer l'item qui représente une région dans les menus — un item
unique et différent par région.

*Pistes techniques* : nouvelle donnée persistée par région (colonne `icon`
VARCHAR dans `zregions_regions`, migration `createOrAlter`), API
`Region.getIcon()` + `RegionManager.setIcon(...)`, commande `/rg icon <region>
[material|hand]` + bouton GUI (clic avec un item en curseur ?) ; fallback sur
l'item du YAML quand aucune icône n'est définie.

## Owner par défaut à la création : activable

Une clé de config pour activer/désactiver l'ajout automatique du créateur comme
owner lors de `/rg create` (aujourd'hui : toujours ajouté).

*Pistes techniques* : clé `regions.creator-becomes-owner` (défaut `true`),
champ caché dans `ZRegionsConfiguration` (pattern des valeurs cachées),
`CreateCommand` passe `null` comme créateur quand désactivé ; ×4 config.yml + doc.

## Rôles personnalisés + flags par membre et par rôle

Aller au-delà de owner/member/visitor : des **rôles personnalisés** définissables
**globalement, par monde ou par région**, des valeurs de flags **par rôle** et
même **par membre individuel**.

*Pistes techniques* : gros chantier — le `GroupTarget` enum devient un système de
cibles dynamiques (`role:<nom>`, `player:<uuid>`), nouvelles tables
(`zregions_roles` avec portée global/monde/région, priorité de rôle), résolution
étendue (membre spécifique > rôle le plus prioritaire > ALL > parents), UI
(commandes `/rg role create|assign` + GUI). Impact sur l'API publique
(`GroupTarget` est dans `api/`) : à concevoir avant de figer l'API v1 — voir
`resolveFlag`/`lookupWithParents` et le stockage `group_target` (VARCHAR 32,
déjà générique, prêt pour des clés dynamiques).

## Taux de pousse configurable par région

La même chose pour la **pousse** (cultures, sapling, canne à sucre, etc.) : accélérer ou
ralentir la croissance dans une région. Tout doit être configurable.

*Pistes techniques* : mêmes flags numériques (ex. `growth-rate`). Événements concernés :
`BlockGrowEvent` (cultures), `StructureGrowEvent` (arbres), `BlockSpreadEvent`
(champignons, bambou). Ralentir = annuler une fraction des ticks de croissance ;
accélérer = appliquer des ticks de croissance supplémentaires lors de l'événement
(à valider côté performance, ces événements sont fréquents).
