# Audit de marché — plugins de régions & claims (préparation zRegions)

> **Date des données : 14 juillet 2026.**
> Audit réalisé par recherche multi-agents (106 agents, 5 angles de recherche, 24 sources lues en profondeur : SpigotMC, Polymart, BuiltByBit, Modrinth, GitHub, documentations officielles, Wayback Machine). 109 affirmations extraites, les 25 plus importantes contre-vérifiées par vote adversarial à 3 voix : **22 confirmées, 3 réfutées** (listées en §10).
> Convention : les chiffres du corps du rapport sont **vérifiés** ; les données marquées `(*)` proviennent de la collecte mais n'ont pas été contre-vérifiées.

---

## 1. Résumé exécutif

Le marché de la protection de territoire se divise en deux segments qui se recoupent :

- **Régions admin** (type WorldGuard) : définir des zones serveur (spawn, arènes, zones PvP) avec des flags. Dominé par **WorldGuard, gratuit et incontournable** — mais vieillissant (backlog de ~135+ issues ouvertes, pas de GUI, pas de Folia officiel, pas de MiniMessage).
- **Claims joueurs** (type GriefPrevention) : les joueurs protègent eux-mêmes leur terrain. Dominé côté gratuit par **GriefPrevention** (~505 k DL SpigotMC + 1,3 M CurseForge) et côté premium par **Lands** (~9 500 ventes à 19,99 €, 4,7/5 — quasi imprenable frontalement).

**Constats clés pour zRegions :**

1. **Les premiums vendent face aux gratuits** quand ils combinent 4 facteurs : levée des limitations du gratuit (flags avancés, GUI), migration sans friction depuis l'incumbent, cadence de maintenance visible, et support réactif. Chaque contre-exemple coûte directement des ventes.
2. **Le concurrent direct visé, UltraRegions (9,98 €), est en train de mourir** : ~1 551 ventes seulement en 8 ans malgré 4,5/5, plaintes de performance récurrentes (chutes de TPS dès 30+ joueurs), et perçu comme abandonné mi-2026 (TechsCode inactif sur ses 13 ressources depuis février 2026). Son concept — « remplaçant de WorldGuard + Multiverse, tout en GUI » — est validé par le marché ; c'est **l'exécution** qui a échoué. Ouverture directe.
3. **La demande de gestion par GUI est prouvée** : WG-GUI, simple addon gratuit pour WorldGuard, cumule 13 491 DL à 4,6/5 ; UltraRegions a une note de 4,5/5 ; Lands fait de son GUI un argument central.
4. **Folia est un différenciateur réel** : ni WorldGuard (fork tiers confidentiel seulement), ni GriefPrevention (refus explicite des mainteneurs), ni RedProtect ne le supportent — alors que des gratuits récents (SimpleClaimSystem, Homestead) le proposent déjà et placent la barre.
5. **Attention au dimensionnement** : le segment « régions admin premium » est petit (UltraRegions ≈ 15 k€ bruts en 8 ans). Le gros volume est côté claims (Lands ≈ 190 k€ bruts cumulés, GriefDefender ≈ 56 k$). La recommandation (§9) : partir « régions admin GUI-first » pour prendre un créneau vacant, avec une architecture qui permet d'ajouter un module claims joueurs en v2 pour adresser le vrai volume.

---

## 2. Panorama comparatif

| Plugin | Segment | Prix | Ventes / DL | Note | Versions MC | Plateformes | État (07/2026) |
|---|---|---|---|---|---|---|---|
| **WorldGuard** | Admin | Gratuit | base installée énorme (non quantifiée) | — | 1.13+ | Spigot/Paper — **pas Folia** | Maintenu, backlog ~135+ issues |
| **UltraRegions** | Admin (GUI) | 9,98 € | ~1 551 | 4,5/5 (146) | 1.8–1.21 (*) | Spigot/Paper | **Perçu abandonné** (02/2026) |
| **WG-GUI** (addon WG) | Admin (GUI) | Gratuit | 13 491 | 4,6/5 (24) | 1.20–26.2 | Spigot/Paper | Maintenu |
| **WorldGuardExtraFlagsPlus** | Admin (flags) | Gratuit (MIT) | 12,8 k (Modrinth) | — | 1.20–1.21.x | Paper/Purpur/**Folia** | Très actif (v4.4.2, 07/2026) |
| **RedProtect** | Hybride | Gratuit | ~543 560 | 4,4/5 (120) | **1.7.2–1.21+** | Spigot/Paper + **moddés hybrides** (Arclight, Mohist) — pas Folia | Maintenu (02/2026) |
| **GriefPrevention** | Claims | Gratuit | ~505 363 (+1,3 M CurseForge) | 4,6/5 (627) | 1.21 | Spigot/Paper/Purpur — **pas Folia** (refus mainteneurs) | Maintenu (03/2026) |
| **Lands** | Claims | **19,99 €** | ~9 546 | **4,7/5** (~305) | 1.20.6–26.2 | Paper + **Bedrock** (forms) + cross-serveur | Très actif (~1 419 versions depuis 2018) |
| **GriefDefender** | Claims | 15 $ | ~3 733 | 4,4/5 (154) | 1.12.2–1.21.9 | Spigot/Paper | Maintenu (01/2026), support fermé |
| **ProtectionStones** | Claims | Gratuit | ~366 137 | — | — | — | (couverture partielle, §10) |
| **HuskClaims** | Claims | Payant (code Apache 2.0) (*) | modeste | — | 1.21.7 | Paper + **Fabric**, cross-serveur | Actif (31 releases) (*) |
| **SimpleClaimSystem** | Claims | Gratuit (MIT) | ~58 896 (*) | 4,9/5 (46) (*) | 1.18+ | Spigot/Paper/Purpur/**Folia** + **Geyser** (*) | Actif |
| **Homestead** | Claims | Gratuit | ~2 703 (*) | 5,0/5 (peu d'avis) (*) | 1.21.9+/26.1 | Paper/**Folia** (*) | Très actif (~3-4 releases/mois) (*) |

*Les compteurs de téléchargements SpigotMC sur ressources premium sont un proxy (borne haute) des ventes uniques, et ne couvrent qu'un canal — Polymart/BuiltByBit non comptés : les volumes réels de Lands/GriefDefender sont donc sous-estimés.*

---

## 3. Segment « claims joueurs » — fiches détaillées

### 3.1 Lands — la référence premium à battre (IncrediblePlugins)

- **19,99 € sur SpigotMC ET Polymart** (pas ~7 € comme souvent supposé), ~9 546 ventes SpigotMC, 4,7/5, maintenu en continu depuis février 2018 avec **~1 419 versions publiées** (dernière MàJ 2 jours avant la collecte). Preuve qu'un premium à ~20 € vend en volume face aux gratuits.
- **Périmètre fonctionnel = le référentiel du marché** : claims par chunks + sous-zones 3D précises au bloc, nations et guerres (capture de drapeaux, boucliers, récompenses), gestion 100 % GUI, taxes/upkeep/banques/loyers, bot Discord (addon), **support Bedrock avec formulaires natifs**, intégration des 4 cartes web (Dynmap, BlueMap, squaremap, Pl3xMap), **synchronisation multi-serveurs via Redis + MySQL**, messages et menus entièrement traduisibles.
- **Import depuis 6 concurrents** (Towny, WorldGuard, GriefPrevention, GriefDefender, Residence, RedProtect) (*) — stratégie délibérée de réduction du coût de migration.
- **Ce que disent les reviews (mars–mai 2026, vérifiées verbatim)** :
  - 👍 « by far the most polished and feature-complete one I've used » (5★) ; « Extremely configurable... The dev is admirably quick with updates » (5★) ; « cross-server support which is rare » (4★) ; « The amount of love that gets put into this plugin is insane » (5★).
  - 👎 Un 1★ pour **perte de données après mise à jour**, un 2★ pour **« bloat » de fonctionnalités activées par défaut**. Thèmes minoritaires mais instructifs : la fiabilité des migrations de données et le « tout activé par défaut » sont des angles d'attaque.
- **Facteurs de succès identifiés** : finition, configurabilité extrême, cross-serveur (rare), réactivité exceptionnelle du développeur.

### 3.2 GriefPrevention — le mastodonte gratuit

- Gratuit (GPL-3.0), ~505 363 DL SpigotMC (4,6/5, 627 avis) + ~1,3 M CurseForge. Toujours vivant : release 16.18.7 (mars 2026), 327 stars, mais **86 issues ouvertes** (*).
- **Officiellement Spigot/Paper/Purpur uniquement. Pas de Folia** : les mainteneurs déclarent (Discussion GitHub #2235) qu'il faudrait réécrire une grande partie du code (non thread-safe, risque de corruption). Pas de serveurs moddés. **Lacune exploitable.**
- Issue #2589 : **corruption de base de données avec perte de données joueurs** (*) — la persistance est un point faible ; un premium avec une couche de stockage robuste (Sarah) a un argument.
- C'est la base installée que tout premium claims doit savoir **importer** (GriefDefender, HuskClaims, SimpleClaimSystem et Lands ont tous un importateur GP).

### 3.3 GriefDefender — la preuve qu'un premium vend contre le gratuit dominant

- 15 $ sur SpigotMC, **~3 733 ventes (~56 000 $ bruts)**, 4,4/5 (154 avis), maintenu (1.12.2–1.21.9, MàJ janv. 2026).
- **Raison d'achat n°1 face à GriefPrevention gratuit** (motif récurrent dans ≥6 avis indépendants 2020–2024) : le contrôle avancé des **flags par claim** et la levée des limitations de GP, avec **migration triviale** — « Migration of regions from a GriefPrevention database was an absolute breeze » (5★) ; « I switched from Grief Prevention to GD primarily for the extensive flag control you have as the admin, something GP lacks » (5★).
- **Deux failles majeures, directement exploitables :**
  1. **Support inaccessible = plainte n°1 des avis 1–3★ (7 des 10 avis négatifs 2023–2025)** : lien Discord expirant en 24 h envoyé par email, emails sans réponse pendant des semaines/mois. « Very poor support, they don't answer e-mails. Discord Support is only with Invite Link which expired 24Hours after buying » (1★, 2025). Le mainteneur assume sur GitHub : « Discord is private. Buy the plugin on spigot if you want a discord link. »
  2. **Complexité excessive, déplorée même par les 5★** : « The most significant issue for me is the plugin's complexity, which often feels excessive » (5★) ; GUI en chat jugée compliquée ; « documentation is half english and half maybe chinese? » (1★).
- Regret du passage en closed-source dans ~3/151 avis (mineur).

### 3.4 Challengers et signaux faibles

- **HuskClaims** (William278) (*) : réécriture moderne de GriefPrevention — cross-serveur natif (claim blocks et claims globaux), implémentations Bukkit/Paper **et Fabric**, trust granulaire (tags LuckPerms `#role/<groupe>`), visualisation par **glowing display entities**, hooks Vault (`/buyclaimblocks`), PAPI (10 placeholders), les 3 cartes web, flag WorldGuard `huskclaims-claim` pour la coexistence claims/régions admin, import GP. Modèle éco intéressant : **code open-source Apache 2.0, binaires + support payants** (SpigotMC/Polymart/BuiltByBit). Faiblesse : écosystème d'addons quasi inexistant.
- **SimpleClaimSystem** (*) : gratuit MIT, ~58 896 DL, 4,9/5 — **couvre déjà Folia ET Bedrock (Floodgate/Geyser)**, GUI, Vault, PAPI, Dynmap/BlueMap/Pl3xMap natifs, ItemsAdder, migration GP. **Place la barre de compatibilité : si un gratuit le fait, un premium n'a pas d'excuse.**
- **Homestead** (*) : gratuit, très actif (~3-4 releases/mois, Folia, MC 26.1, 75+ flags, sous-zones, leasing, taxes, GUI, analytics) mais seulement ~2 703 DL depuis janvier 2025 — **un nouvel entrant, même gratuit et complet, perce difficilement sans notoriété ni canal de distribution.** La marque GroupeZ est un vrai actif ici.
- **Towny / CrashClaim / ProtectionStones** : couverture insuffisante dans cet audit (voir §10). ProtectionStones : ~366 k DL (gratuit). Towny : écosystème carto dédié (BlueMap-Towny, MapTowny) (*).

---

## 4. Segment « régions admin » — fiches détaillées

### 4.1 WorldGuard — l'incumbent gratuit (EngineHub)

Aucun claim vérifié ne quantifie sa base installée (limite de l'audit, §10), mais son statut de standard de fait est incontesté (927 stars, 651 forks). Ce qui compte pour zRegions, c'est son **backlog UX** (~135+ issues ouvertes (*)) :

| Issue | Demande / bug | Signal pour zRegions |
|---|---|---|
| #2260 (03/2026, ouverte) | Masquer dans `/rg flags` les flags auxquels l'utilisateur n'a pas accès | UI de flags **filtrée par permission** — facile en GUI |
| #2257 (02/2026) (*) | Valeurs de flag différentes **par groupe/cible** | Flags par groupe = feature différenciante |
| #2287 (07/2026) (*) | Localisation moderne **MiniMessage** — toujours absente | Messages MiniMessage + multilingue natif |
| #2262 (03/2026) (*) | `rg info` expose les noms de régions (confidentialité) | Permissions fines sur l'info |
| #2284, #2267 (*) | Bypass creeper, knockback Wind Charge bloqué à tort en 1.21.x | Suivre la méta 1.21 de près |

- **Pas de Folia officiellement.** Le seul chemin est un fork communautaire (Euphillya/WorldGuard-Folia, patchs par script, jars via GitHub Actions, 1.21.11 supporté) à l'adoption marginale (13 stars). **La demande « régions admin sur Folia » existe et n'est pas servie par le leader.**
- L'écosystème compense ses manques par des addons — ce qui dessine exactement le produit intégré à construire :
  - **WG-GUI** (gratuit, 13 491 DL, 4,6/5 depuis 2018) prouve la demande de GUI. Ses manques récurrents en reviews = cahier des charges natif de zRegions : gestion des **groupes** (pas seulement membres individuels), **flags par membre**, respect du `max-region-count`, robustesse (crash `InventoryCloseEvent may only be triggered synchronously`), et **issue tracker public** (absent, reproché).
  - **WorldGuardExtraFlagsPlus** (MIT, v4.4.2 du 10/07/2026, 12,8 k DL Modrinth, **Folia**) : 30+ flags dont **conditions d'entrée par XP ou placeholder PAPI** et blocage d'items de la méta PvP 1.21 (**Mace, Wind Charge, Totem, Trident, firework rocket, lances**), hooks PacketEvents/ProtocolLib. → Les flags avancés « méta 1.21 » sont une vraie demande.
  - ⚠️ Nuance importante (claim réfuté 0-3) : les reviews de WG-GUI ne prouvent PAS que la CLI de WorldGuard soit un point de douleur majeur. La demande GUI est réelle, mais ne pas construire tout le pitch dessus — la **valeur ajoutée fonctionnelle** (flags, groupes, perf, Folia) doit porter autant que le confort.

### 4.2 UltraRegions — le concurrent direct, autopsie d'un échec d'exécution

- **9,98 € — ~1 551 ventes depuis juillet 2018** malgré 4,5/5 (146 avis). Soit ≈ 15 k€ bruts en 8 ans : le concept plaît (la note le prouve) mais ne convertit pas en volume.
- **Positionnement (validé par le marché)** : « The Ultimate replacement for Multiverse and World Guard featuring a modern GUI » — régions, flags, **visualisation par particules**, gestion de mondes, « Everything can be controlled in a GUI ».
- **Cause d'échec n°1 : la performance.** 6 des 21 avis ≤3★ citent le TPS/lag ; **tous** les avis négatifs de 2024 sont des plaintes de performance. Verbatim (1★, 2024) : « Ultraregions remains the worst plugin choice for servers with 30 or more players. It uses 20% of server threads just to protect the spawn. »
- **Cause d'échec n°2 : l'abandon.** Dernier avis (24 mai 2026, 1★) : « No updates for months, the author is completely inactive, and even the Discord confirms the plugin is dead ». Corroboration forte : les **13 ressources premium TechsCode affichent toutes la même date de dernière MàJ (01/02/2026)**, zéro update depuis, sur tout le portefeuille. Support déjà critiqué avant (tickets ignorés 2 semaines).
- Réserves : l'abandon est une perception (TechsCode a déjà survécu à un trou de 7 mois) ; la fenêtre peut se refermer.
- **Leçon pour zRegions** : reprendre le concept (GUI-first, visualisation, tout-en-un) en réussissant les deux points qui l'ont tué — **un moteur de lookup spatial performant** (c'est LE sujet technique n°1) et **une cadence de maintenance/support visible** (la force historique de GroupeZ).

### 4.3 RedProtect — le gratuit hybride (les deux segments en un)

- Gratuit, ~543 560 DL, 4,4/5 (120 avis), encore maintenu (release 8.1.4-b11, févr. 2026).
- **Seul plugin majeur à couvrir les DEUX segments** : claims joueurs (création par clôtures + panneau, limites par joueur/groupe) ET régions admin (owner `#server#`).
- Flags étendus : commandes à l'entrée/sortie, elytra (par monde), effets, enderpearl ; protection complète du contenu (coffres, armor stands, bannières, portes, animaux, fermes) ; **addon BuyRent** (achat/location de régions via Vault).
- Hooks : Dynmap, **WorldEditCUI** (`/rp select-we`), Vault, ProtocolLib, EssentialsX, BossBar/ActionBar, **importateurs GriefPrevention et MyChunk**.
- Plage de versions exceptionnelle : **1.7.2 → 1.21+**, y compris serveurs hybrides moddés (**Arclight, Mohist, KCauldron/Thermos**, entités de mods — auto-déclaré par l'auteur, non testé indépendamment). **Ni Folia, ni Bedrock/Geyser.**
- Leçon : l'hybride claims + admin dans un seul produit est faisable et populaire ; c'est aussi la référence du « catalogue de hooks » attendu.

---

## 5. La voix des utilisateurs — synthèse transverse des reviews

**Ce qui fait mettre 5★ (et acheter) :**
- La **finition** et l'étendue fonctionnelle (Lands : « most polished and feature-complete »).
- La **réactivité du développeur** et la cadence de mises à jour (Lands, cité dans quasi tous les avis positifs).
- La **levée des limitations du gratuit** — flags avancés par claim/région (GriefDefender vs GP).
- La **migration facile** depuis le plugin précédent (« an absolute breeze »).
- Le **cross-serveur** (Lands : « rare » — donc différenciant).

**Ce qui fait mettre 1★ (et détruit les ventes) :**
- Le **support inaccessible ou lent** (GriefDefender : 7/10 avis négatifs ; UltraRegions : tickets ignorés).
- La **performance** (UltraRegions : 100 % des avis négatifs 2024).
- L'**abandon perçu** (UltraRegions ; « the author is completely inactive »).
- La **perte de données** lors de mises à jour/migrations (Lands 1★, GriefPrevention #2589).
- La **complexité excessive** — reprochée même dans des avis 5★ (GriefDefender : « overcluttered/overfeatured » ; Lands 2★ : bloat activé par défaut).

**Demandes de fonctionnalités récurrentes (reviews + issues GitHub) :**
- Gestion des **groupes de permissions** (pas seulement des joueurs individuels) — WG-GUI, WorldGuard #2257, HuskClaims trust tags.
- **Flags par membre/groupe/cible** plutôt que par région entière.
- UI de flags **lisible et filtrée** (WorldGuard #2260 — des dizaines de flags illisibles en chat).
- **Conditions d'entrée** paramétrables (XP, placeholders PAPI) et flags de la **méta PvP 1.21** (Mace, Wind Charge, Totem, Trident).
- **Localisation moderne** (MiniMessage, multilingue) — WorldGuard #2287.
- **Issue tracker public** et canal de support ouvert.

---

## 6. Compatibilité versions & plateformes — ce que le marché impose

| Cible | Verdict | Justification |
|---|---|---|
| **Paper 1.20 → dernière (1.21.x/26.x)** | **Obligatoire** | Standard de tous les acteurs actifs (Lands teste 1.20.6→26.2 ; WGEFP 1.20–1.21.x). |
| **Spigot** | Oui | Encore la plateforme d'une grosse part du parc ; coût faible. |
| **Folia** | **Oui, différenciateur majeur** | Absent de WorldGuard (fork confidentiel), GriefPrevention (refus explicite) et RedProtect ; déjà offert par des gratuits récents (SimpleClaimSystem, Homestead, WGEFP) — la barre monte. GroupeZ a déjà FoliaLib dans sa stack. |
| **Versions < 1.20 (1.8–1.19)** | Non recommandé | Seul RedProtect (gratuit, ancien) couvre 1.7.2+ ; les premiums actifs ont abandonné ; coût NMS élevé pour un marché déclinant. |
| **Serveurs moddés (Arclight, Mohist, Fabric)** | **Non prioritaire** | Seuls RedProtect (auto-déclaré, non testé) et HuskClaims (Fabric) s'y risquent ; aucun signal de demande payante quantifiée ; coût de QA élevé. À réévaluer sur demande client réelle. |
| **Bedrock / Geyser** | v2 | Lands (forms natifs) et SimpleClaimSystem le font ; vrai plus pour les serveurs crossplay, mais pas bloquant pour le MVP admin-regions. |
| **Purpur** | Oui (gratuit) | Compatible de fait si Paper l'est ; l'afficher explicitement. |

---

## 7. Intégrations attendues — la checklist du marché

Confirmées par la présence chez ≥2 leaders (RedProtect, Lands, HuskClaims, SimpleClaimSystem, WGEFP, GDHooks) :

**Indispensables (MVP)**
- [ ] **Vault** (économie) — et pour GroupeZ : **CurrenciesAPI** (16+ économies unifiées, un avantage maison)
- [ ] **PlaceholderAPI** — placeholders région (nom, owner, flags…) ET conditions basées sur PAPI (l'expansion WorldGuard officielle de PAPI prouve l'attente d'écosystème)
- [ ] **WorldEdit / WorldEditCUI** — sélection visuelle des régions
- [ ] **LuckPerms** — flags/membres par groupe de permissions
- [ ] **Importateur WorldGuard** (critique — c'est la base installée à convertir) + **UltraRegions** (récupérer les orphelins) ; GriefPrevention si module claims
- [ ] Messages d'entrée/sortie : chat, **ActionBar, BossBar**, title
- [ ] **ProtocolLib/PacketEvents** (hooks paquets pour visualisation/flags avancés)

**Fortement attendues (v1.x/v2)**
- [ ] **Cartes web : BlueMap + Pl3xMap + Dynmap** (le trio est le standard — le répertoire BlueMap liste ~17 addons territoire ; Pl3xMap explicitement demandé en review RedProtect) ; squaremap en bonus
- [ ] **EssentialsX** — blocage `/home`, `/sethome`, `/back` en région (+ équivalent zEssentials, avantage maison)
- [ ] Flags méta PvP 1.21 : **Mace, Wind Charge, Totem, Trident, firework rocket, lances**
- [ ] Conditions d'entrée par XP / placeholder (WGEFP le fait déjà en gratuit)
- [ ] Visualisation moderne : particules (UltraRegions) et/ou **glowing display entities** (HuskClaims)

---

## 8. Pourquoi les premiums vendent face aux gratuits (modèle en 4 facteurs)

Triangulation sur l'ensemble des données vérifiées :

| Facteur | Preuve positive | Contre-exemple (coût direct) |
|---|---|---|
| (a) Levée claire des limitations du gratuit | GriefDefender : flags avancés vs GP → 3,7 k ventes | — |
| (b) Migration sans friction depuis l'incumbent | GD : « migration... an absolute breeze » ; Lands : import 6 concurrents | — |
| (c) Cadence de maintenance visible | Lands : ~1 419 versions, MàJ 2 jours avant la collecte → 4,7/5 | UltraRegions : abandon perçu → avis 1★ « plugin is dead » |
| (d) Support réactif et accessible | Lands : « dev is admirably quick » | GriefDefender : Discord fermé = plainte n°1 des avis négatifs |

Un cinquième facteur implicite : **la performance** — l'échec d'UltraRegions montre qu'une région-engine mal optimisée tue le produit sur les serveurs qui, précisément, ont les moyens de payer (30+ joueurs).

**GroupeZ possède déjà (b), (c), (d)** : marque installée, cadence de release prouvée sur le portefeuille z*, Discord ouvert. Le pari porte sur (a) et la performance.

---

## 9. Recommandations pour zRegions

> Cette section est une synthèse interprétative (confiance moyenne) — les faits sous-jacents sont vérifiés, la stratégie est une recommandation.

### 9.1 Positionnement conseillé

**« Régions admin GUI-first » — le créneau UltraRegions, en voie d'abandon — avec une architecture extensible vers les claims joueurs en v2.**

- Le pitch qui a fait 4,5/5 : *« Remplacez WorldGuard par une GUI moderne »* — mais exécuté avec la performance et la maintenance qui ont manqué à UltraRegions, et un **importateur WorldGuard sans friction**.
- **Ne pas attaquer Lands frontalement** en v1 : 19,99 €, 4,7/5, ~1 400 versions, dev hyper-réactif — quasi imprenable sur les claims. En revanche, l'architecture (régions = zones + flags + membres + rôles) doit être conçue pour qu'un **module « claims joueurs »** (limites par joueur, achat de claims, GUI joueur) s'ajoute en v2 — c'est là qu'est le volume (GP : 1,8 M DL cumulés ; segment où GriefDefender fait 56 k$ avec un support catastrophique).
- Différenciation vs WorldGuard gratuit : GUI native complète, flags par groupe (#2257), UI de flags filtrée (#2260), MiniMessage/multilingue (#2287), Folia, flags méta 1.21, support commercial.
- ⚠️ Honnêteté sur le marché : votre propre `ANALYSE-MARCHE-PLUGINS.md` classe « protection » comme verrouillée par le gratuit — c'est vrai pour un clone. La fenêtre existe précisément parce qu'UltraRegions meurt ET que WorldGuard accumule un backlog UX. Elle peut se refermer (retour de TechsCode, ou un fork WorldGuard modernisé).

### 9.2 Différenciateurs (par ordre d'impact)

1. **Performance mesurable et communiquée** — moteur de lookup spatial optimisé (index spatial par chunk/région, caches par joueur, zéro travail sur le main thread pour les checks) + benchmarks publiés dans la page produit. C'est l'anti-UltraRegions.
2. **GUI native zMenu** — 100 % des écrans configurables/thémables par les admins (personne ne fait ça : les GUI concurrentes sont figées), boutons custom via le pattern Loader.
3. **Folia jour 1** (via FoliaLib) — case que ni WorldGuard ni GP ni RedProtect ne cochent.
4. **Import en 1 commande** : WorldGuard (critique), UltraRegions (orphelins), RedProtect. Avec dry-run et rapport — la peur de la perte de données est un motif d'avis 1★.
5. **Flags nouvelle génération** : par groupe/rôle et par membre, conditions d'entrée (XP, PAPI, permission), méta 1.21 (Mace, Wind Charge…), flags à valeur (pas seulement allow/deny).
6. **Support & confiance GroupeZ** : Discord ouvert, issue tracker public (reproché à WG-GUI et GriefDefender), docs bilingues EN/FR, configs multilingues en/fr/es/it — standard maison.
7. **Visualisation soignée** : particules + glowing display entities, sélection WorldEditCUI.

### 9.3 Périmètre MVP vs v2

**MVP (v1.0) — « WorldGuard remplacé, en mieux » :**
- Régions cuboïdes multi-mondes, priorités et régions parent/enfant, région globale par monde.
- ~40–60 flags essentiels (build, interact, PvP, mobs, explosions, entrée/sortie, commandes exécutées, effets, vol, enderpearl/chorus, méta 1.21) — avec valeurs par groupe de membres (owner/member/visitor + groupes LuckPerms).
- Membres/propriétaires par joueur ET par groupe LuckPerms.
- GUI zMenu complète : liste/recherche de régions, création guidée, éditeur de flags **paginé, filtré par permission, avec recherche**, gestion des membres, redimensionnement.
- Wand de sélection + visualisation particules ; `/rg` CLI complète en parallèle (les admins scriptent).
- Messages entrée/sortie (chat/actionbar/bossbar/title), MiniMessage, multilingue en/fr/es/it.
- Importateur WorldGuard + UltraRegions.
- Intégrations : Vault + CurrenciesAPI, PlaceholderAPI, WorldEdit/CUI, LuckPerms, EssentialsX/zEssentials (`/home`/`/back` bloqués).
- Stockage via **Sarah** (SQLite par défaut, MySQL/MariaDB) avec migrations automatiques ; sauvegarde asynchrone.
- Paper/Spigot/Purpur 1.20→latest, **Folia**.
- **API publique + events custom** dès la v1 (l'écosystème d'addons est ce qui rend un plugin difficile à quitter ; l'absence d'API/addons est la faiblesse notée de HuskClaims).

**v2 (roadmap affichée publiquement — la roadmap visible est un argument de vente) :**
- Régions polygonales et 3D avancées.
- Achat/location/enchères de régions (modèle BuyRent — synergie zAuctionHouse).
- Cartes web : BlueMap + Pl3xMap + Dynmap.
- Module **claims joueurs** optionnel (limites par permission, claim blocks, GUI joueur, import GriefPrevention) — activable par config.
- Gestion de mondes (le volet « Multiverse » d'UltraRegions) — à valider par la demande.
- Bedrock/Geyser (forms), multi-serveur (Redis/MySQL), bot Discord, addons (le modèle GDHooks).

### 9.4 Compatibilité conseillée (synthèse §6)

**Paper/Spigot/Purpur 1.20 → dernière version, Folia via FoliaLib dès la v1. Pas de legacy <1.20, pas de moddé en v1, Bedrock en v2.**

### 9.5 Prix conseillé

**≈ 12–15 €** en lancement (promo de lancement possible à ~10 €).
- UltraRegions vendait 9,98 € — se positionner légèrement au-dessus avec un produit visiblement supérieur, plutôt que d'ancrer « discount ».
- GriefDefender prouve que 15 $ passe sans friction quand la valeur (flags) est claire ; Lands tient 19,99 € — un zRegions v2 avec module claims pourra monter la gamme.
- Canaux : SpigotMC + Polymart + BuiltByBit (les volumes SpigotMC seuls sous-estiment le marché ; Lands vend au même prix sur les deux principaux).

### 9.6 Intégration à l'écosystème GroupeZ

| Brique | Usage dans zRegions |
|---|---|
| **zMenu** (`zmenu-api`, compileOnly + ServicesManager) | Toutes les GUI, boutons custom (pattern Loader), dialogs 1.21+ pour confirmations |
| **Sarah** (shadée/relocatée) | Persistance régions/flags/membres, MigrationManager, SQLite/MySQL |
| **FoliaLib** | Scheduler abstrait → `folia-supported: true` |
| **CurrenciesAPI** | Économie (achat/location de régions) au-delà de Vault |
| **PlaceholderAPI** | compileOnly, placeholders + conditions |
| Pattern maison | `Z`-prefix impls + module API séparé (jar `target-api/`), Hooks/ dynamiques, docs Docusaurus bilingues, configs en/fr/es/it |

Synergies portefeuille : zEssentials (flags home/tp), zAuctionHouse (vente de régions), zKoth (zones de capture), zSpawner/zShop (restrictions par région). Chaque intégration croisée renforce la valeur du portefeuille entier — argument que ni TechsCode ni les indés ne peuvent répliquer.

### 9.7 Risques

1. **Fenêtre concurrentielle** : si TechsCode revient (déjà un trou de 7 mois survécu) ou si un WorldGuard modernisé émerge, le créneau se referme. → Vitesse d'exécution.
2. **Marché admin-premium étroit** (~1,5 k ventes/8 ans pour UltraRegions) : le MVP seul ne fera pas un gros CA ; c'est la **plateforme** (v2 claims + écosystème) qui porte le potentiel. → Ne pas sur-investir le MVP au-delà du nécessaire.
3. **Complexité** : le reproche fait à GriefDefender/Lands. → Défauts sains, onboarding guidé, « ça marche en 5 minutes », features avancées opt-in.
4. **Performance** : promesse centrale — si elle n'est pas tenue, c'est l'échec UltraRegions. → Benchmarks automatisés avant chaque release.

---

## 10. Le multi-serveur (cross-server) — pourquoi, qui, comment

> Section ajoutée après une recherche dédiée (6 agents + vérification adversariale ; 5 affirmations trompeuses réfutées, cf. §11). Le *comment technique* pour zRegions est dans [`ARCHITECTURE.md` §Multi-serveur](./ARCHITECTURE.md).

### 10.1 Qui le fait, et comment (état du marché)

| Plugin | Cross-server ? | Backend requis | Ce qui est synchronisé | Modèle éco |
|---|---|---|---|---|
| **Lands** | ✅ natif, argument phare | **MySQL (« MySQL-v2 ») + Redis OBLIGATOIRE** (« won't work properly without Redis! ») + fuseaux horaires identiques | claims, nations, banques ; **un seul serveur `master`** exécute upkeep/taxes pour tout le réseau | Payant (inclus, pas un module) |
| **HuskClaims** | ✅ « cross-server native » | **MySQL 8.0+/MariaDB/MongoDB** (SQLite exclu) + broker **PLUGIN_MESSAGE** (défaut) ou **Redis** ; `cluster_id` sépare plusieurs réseaux | **claim-blocks accumulés réseau-wide**, liste de claims globale (`/claimslist`), trust ; téléport cross-server via hook HuskHomes | **Open-source (Apache-2.0)** ; binaires+support payants |
| **HuskTowns** | ✅ « cross-server native » (Towny-style) | MySQL requis, Redis optionnel | villes, claims, membres réseau-wide | idem HuskClaims |
| **GriefDefender** | ⚠️ partiel | option `always-read-write-db` = **base partagée, cache désactivé** (pas de sync temps-réel par événements) | données de claims via DB commune ; perf dépend de la DB | **Payant** sur Spigot |
| **GriefPrevention** | ❌ aucun natif | — | plugin tiers « Claim Bridge » qui synchronise **UNIQUEMENT le solde de claim-blocks**, PAS les zones protégées | Gratuit |
| **RedProtect** | ❌ aucun natif | MySQL = stockage centralisé seulement, sans couche de sync | — | Gratuit |

**Enseignement marché (corrigé après vérification) :** le cross-server n'est **pas une niche premium vide** — c'est une fonctionnalité **établie** du segment « réseau », déjà occupée par des acteurs matures, dont **deux en FOSS** (HuskClaims/HuskTowns, Apache-2.0, compilables gratuitement). Ce n'est donc pas « personne ne le fait » ; c'est un **prérequis d'entrée** pour viser les réseaux. En revanche, **aucun de ces plugins cross-server n'est un plugin de régions admin GUI-first** (ce sont tous des plugins de *claims joueurs*), et les gros plugins de *régions admin* (WorldGuard, UltraRegions, RedProtect) n'ont **aucun** cross-server. L'opportunité de zRegions n'est pas « être le seul à faire du cross-server », mais **être le premier plugin de régions admin GUI-first + cross-server, intégré à l'écosystème GroupeZ**.

### 10.2 Pourquoi les réseaux en ont besoin (usage réel)

- Le **proxy** (Velocity/BungeeCord) ne fait que router les connexions vers des serveurs backend **totalement indépendants** (mondes, plugins, bases séparés) — il ne partage **aucune** donnée de plugin ni ne traite les événements de jeu.
- Sans cross-server : **données divergentes** (chaque backend a ses propres régions/claims), **cache obsolète** (une édition sur A n'est pas vue par B), et surtout — pour les *claims joueurs* — **limites contournables** : un joueur retrouve son quota complet de claim-blocks sur chaque serveur → il claime bien au-delà de la limite réseau prévue.
- Les besoins concrets, par ordre de valeur pour zRegions :
  1. **Gestion admin centralisée** : lister/éditer les régions de **tous** les serveurs depuis un point unique (via la base partagée), même les régions non chargées localement.
  2. **Cohérence du cache** : une région éditée sur A est rechargée par les autres serveurs concernés (pas de données périmées).
  3. **(v2 claims joueurs) données joueur réseau-wide** : claim-blocks accumulés/dépensés à l'échelle du réseau (LE cas d'usage n°1 du cross-server claims, il ferme l'exploit « change de serveur pour réinitialiser ton quota »), liste de claims globale, **téléport cross-server** vers une région/warp (nécessite un hook de téléportation).
  4. **Données globales** : bans/blacklist et config appliqués réseau-wide.

### 10.3 Deux idées reçues à éviter (réfutées lors de la vérification)

- **« Sharder la survie oblige à partager les régions. » FAUX.** Le modèle dominant fait tourner des **mondes survie séparés et indépendants** (survival-1/survival-2, ou par mode), chacun avec ses propres régions — aucun partage requis. Le partage de l'état du monde n'est nécessaire que dans le cas **rare et techniquement difficile** d'un *seul* monde continu réparti sur plusieurs serveurs (region-sharding, ex. WorldQL). **Conséquence design : une région est intrinsèquement liée à un `(serveur, monde, coordonnées)` — elle n'a pas vocation à « exister sur tous les serveurs ».**
- **« On peut appliquer des flags de région au niveau du proxy. » FAUX.** Un proxy ne traite pas les événements de jeu (pose/casse de blocs, PvP). Les bans fonctionnent au proxy car ce sont des opérations *de connexion* (interceptables avant le backend) ; les protections de région sont **toujours appliquées par le backend**. La cohérence réseau-wide des protections se fait par réplication/sync sur chaque serveur, pas par un point proxy.

### 10.4 Comment c'est construit (patterns techniques validés)

- **SQLite est exclu du multi-serveur** (fichier local, verrouillage sur partage réseau bogué → corruption). → **MySQL/MariaDB requis** dès qu'on active le multi-serveur.
- La **base partagée = source de vérité**, mais elle **ne propage pas** les changements seule → il faut un **canal de messagerie** : **Redis pub/sub** (rapide, découplé, mais *fire-and-forget* sans garantie de livraison → exige reconnexion + un filet de re-sync périodique) **ou plugin-messaging BungeeCord** (sans dépendance externe, mais exige ≥1 joueur en ligne sur les serveurs émetteur ET récepteur).
- Le pattern de référence (LuckPerms) : **« push notification + pull by id »** — le message ne transporte que « l'objet X a changé », chaque serveur **recharge X depuis la base partagée**. Nuance importante (tirée du code maison zAuctionHouse) : ce n'est pas universel — quand un reload-by-id renverrait `null`/un état incohérent (ex. la ligne DB est déjà supprimée), il faut un **message « gras »** portant l'état nécessaire (leçon du bug « ghost listing »).
- **Cohérence concurrente** : des verrous distribués (Redis `SET NX PX` + Lua atomique) ne sont nécessaires qu'en cas de **forte contention** (ex. deux serveurs achètent le même item). Pour des régions (éditions admin rares), un **verrou optimiste par colonne de version** en SQL suffit.
- **L'écosystème GroupeZ a déjà deux implémentations maison de référence** : **zAuctionHouse Redis** (le patron abouti : pool Jedis, thread subscriber avec backoff, UUID serveur persistant + heartbeat, verrous Lua, whitelist de classes) et **zVaults/Distributed** (plus simple : pub/sub multi-canaux, event-carried state). zRegions doit s'aligner sur le style zAuctionHouse.

**Implication pour zRegions (résumé) :** le multi-serveur doit être **prévu dans le modèle de données dès la v1** (support MySQL via Sarah, colonne `origin_server`, cache invalidable par région) même si la **couche messaging est livrée en v2** — c'est ce qui évite une réécriture. Voir [`ARCHITECTURE.md`](./ARCHITECTURE.md) pour le blueprint.

---

## 11. Limites de l'audit & questions ouvertes

**Affirmations réfutées lors de la vérification (exclues du rapport) :**
1. « Les plaintes récurrentes de Lands (pertes de données, bloat) datent de décembre 2024 » — réfuté 0-3 : les avis existent mais datent de mars–mai 2026 (erreur de conversion de timestamps).
2. « Les reviews de GriefPrevention demandent des claims polygonaux et une intégration économie » — réfuté 0-3 : non corroboré par les sources.
3. « Les reviews de WG-GUI prouvent que la CLI de WorldGuard est un point de douleur majeur » — réfuté 0-3 : la demande GUI est réelle mais cette motivation précise n'est pas démontrée.

**Angles non couverts (à creuser si besoin avant de figer le périmètre v2) :**
- **Base installée et plaintes de WorldGuard lui-même** (bStats/CurseForge) — l'incumbent n°1 n'a pas de quantification vérifiée.
- **Ventes Polymart/BuiltByBit** de Lands/GriefDefender/UltraRegions, et parts de marché de **Towny, ProtectionStones, HuskClaims, CrashClaim** — nécessaire pour dimensionner précisément le segment claims avant la v2.
- **Demande quantifiée Bedrock/Geyser et Folia** (proportion de serveurs concernés).
- **L'inactivité de TechsCode est-elle définitive** (abandon/vente du portefeuille) ? La fenêtre concurrentielle en dépend.

**Autres réserves :** compteurs SpigotMC = borne haute des ventes, un seul canal ; compatibilité moddée de RedProtect auto-déclarée ; « abandon » d'UltraRegions = perception (avis + inactivité corrélée sur 13 ressources), pas un fait annoncé.

---

## 12. Sources principales

| Source | Qualité |
|---|---|
| [Lands — SpigotMC](https://www.spigotmc.org/resources/lands-%E2%AD%95-land-claim-plugin-%E2%9C%85-grief-prevention-protection-gui-management-nations-wars-1-21-x-support.53313/) + [wiki officiel](https://wiki.incredibleplugins.com/lands) + [Polymart](https://polymart.org/product/876/lands-land-claim-plugin) | primaire |
| [GriefPrevention — SpigotMC](https://www.spigotmc.org/resources/griefprevention.1884/) + [GitHub](https://github.com/GriefPrevention/GriefPrevention) (+ Discussion #2235, issues) | primaire |
| [GriefDefender — SpigotMC](https://www.spigotmc.org/resources/1-12-2-1-21-4-griefdefender-claim-plugin-grief-prevention-protection.68900/) (+ reviews, GitHub issues #429/#517) | primaire |
| [UltraRegions — SpigotMC](https://www.spigotmc.org/resources/ultra-regions.58317/) | primaire |
| [RedProtect — SpigotMC](https://www.spigotmc.org/resources/redprotect-anti-grief-server-protection-region-management-mod-mobs-flag-compat-1-7-1-21.15841/) + [GitHub](https://github.com/FabioZumbi12/RedProtect) | primaire |
| [WG-GUI — SpigotMC](https://www.spigotmc.org/resources/wg-gui-better-worldguard-experience-1-20-26-2.57951/) | primaire |
| [WorldGuardExtraFlagsPlus — SpigotMC](https://www.spigotmc.org/resources/worldguard-extraflags-plus.129946/) + [GitHub](https://github.com/tinsware/WorldGuardExtraFlagsPlus) | primaire |
| [WorldGuard — issues GitHub](https://github.com/EngineHub/WorldGuard/issues) (dont [#2260](https://github.com/enginehub/worldguard/issues/2260)) + [fork WorldGuard-Folia](https://github.com/Euphillya/WorldGuard-Folia) | primaire |
| [HuskClaims — GitHub](https://github.com/WiIIiam278/HuskClaims) + [docs hooks](https://william278.net/docs/huskclaims/hooks) + [HuskTowns hooks](https://william278.net/docs/husktowns/hooks) | primaire |
| [SimpleClaimSystem — SpigotMC](https://www.spigotmc.org/resources/simpleclaimsystem-%E2%9C%85-1-18-26-1-2-fully-configurable-%E2%9A%99%EF%B8%8F-folia-bedrock-supported-%E2%9A%A1.115568/) + [GitHub](https://github.com/Xyness/SimpleClaimSystem) | primaire |
| [Homestead — SpigotMC](https://www.spigotmc.org/resources/homestead-%E2%80%94-advanced-land-claiming-plugin.121873/) | primaire |
| [BlueMap — 3rd party support](https://bluemap.bluecolored.de/3rdPartySupport.html) | primaire |
| [Folia — docs PaperMC](https://docs.papermc.io/paper/dev/folia-support/) | primaire |
| Discussions Discord archivées (answeroverflow) | forum |
| ultraregions.com, page BuiltByBit UltraRegions | jugées non fiables, écartées |
| **Multi-serveur** : [Lands — wiki Database](https://wiki.incredibleplugins.com/lands/setup/database) (« MySQL + Redis required, won't work without Redis! ») + [HuskClaims cross-server docs](https://william278.net/docs/huskclaims) + [HuskTowns](https://william278.net/project/husktowns) + [GriefPrevention Claim Bridge](https://www.spigotmc.org/resources/grief-prevention-claim-bridge-sync-across-multiple-servers.29078/) + [GriefDefender Storage `always-read-write-db`](https://docs.griefdefender.com/) + [LiteBans (bans réseau au proxy)](https://docs.bloom.host/multiplatform/litebans) | primaire |
| **Multi-serveur (code de référence)** : LuckPerms (`messaging/`, `storage/`, `SyncTask`, `BufferedRequest`) + code maison GroupeZ `zAuctionHouse Redis` (Jedis, verrous Lua, UUID serveur) et `zVaults/Distributed` | code source |
