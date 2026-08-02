# Plan d'amélioration — Inventaire officine (mobile + web)

> Rédigé le 2026-07-31. Périmètre : application mobile `mobile-inventory` (Android/Kotlin),
> module web `pharmaSmart-app/src/main/webapp/app/features/inventory` (Angular),
> et compléments backend `com.kobe.warehouse.service.inventaire`.

## Principe directeur

**Le comptage se fait au mobile ; le web pilote.** Le terminal de comptage en officine
est le mobile (scan, déplacement en rayon/réserve) — c'est lui qui doit être irréprochable
en fiabilité et en cadence. Le web sert à créer les inventaires, superviser l'avancement
des compteurs, analyser les écarts et clôturer. Les priorités de ce plan en découlent :
les chantiers mobiles (phases 1 à 3) passent avant tout ; côté web, la supervision
temps réel (W2) prime sur le scan au poste (W1), qui devient optionnel.

## 1. État des lieux

### Mobile (après refonte de juillet 2026)

L'app mobile vient d'être réalignée sur l'API standard (`/api/store-inventories`,
`/api/store-inventory-lines/v2`, `/batch`, lots, rayons, progression). Fonctionnel actuel :
liste des inventaires actifs, détail avec lignes, filtre par rayon, barre de progression,
comptage par saisie ou scan caméra (ZXing), gestion des lots (comptage lot par lot, ajout,
suppression), synchronisation batch, clôture. L'ancienne API `/java-client/mobile/*` est
obsolète et ne doit plus être utilisée.

### Web

Module riche et mature : onglets en cours / tournant / clôturés, création multi-types
(MAGASIN, RAYON, STORAGE, FAMILLY, PERIME, ALERTE_PEREMPTION, VENDU, INVENDU, SOUS_SEUIL,
EN_RUPTURE, SELECTION_PRODUIT, ABC), éditeur AG-Grid avec navigation clavier, filtres
(restant à compter, écarts ±, storage, rayon, recherche), mode aveugle par privilège
(`PR_VOIR_STOCK_INVENTAIRE`), grille lots, valorisation, analyse d'écarts, export PDF,
import CSV, planning d'inventaire tournant.

### Backend

Services découpés (`InventaireCreationService`, `InventaireSyncService`,
`InventaireQueryService`, `InventoryLotService`, `InventaireProgressService`,
`InventoryCloseService`, `PlanningInventaireTournantService`...). Endpoints complets pour
les besoins actuels du web.

---

## 2. Lacunes identifiées — Mobile

### M1. Le hors-ligne n'est pas câblé (critique)

L'infrastructure existe (Room v2, `InventoryDao`/`InventoryLineDao`, `SyncWorker`,
`SyncManager` avec WorkManager) mais **l'écran de comptage appelle l'API en direct** :
une coupure réseau en réserve = saisie perdue. À faire :

- Room devient la source de vérité de l'écran de détail (lecture via `Flow`).
- Chaque saisie écrit d'abord en local (`locallyModified=true`, `syncStatus=PENDING`).
- `SyncWorker` (déjà branché sur `/batch` avec gestion des `failedIds`) pousse dès que
  le réseau revient ; déclenchement aussi sur reconnexion (NetworkCallback).
- Indicateur UI : nombre de lignes en attente de synchro + état connectivité.

### M2. Scan sous-dimensionné pour la cadence officine (critique)

- **Parsing GS1 DataMatrix absent** : les boîtes portent un DataMatrix contenant CIP13
  (AI 01), péremption (AI 17) et n° de lot (AI 10). ZXing lit le DataMatrix mais l'app
  ne décode pas les AI → le lot et la date que le code contient déjà sont saisis à la
  main. Gain immédiat pour la gestion des lots : pré-remplir n° de lot + péremption,
  voire créer le lot automatiquement.
- **Pas de mode scan continu « +1 »** : le standard du comptage est caméra ouverte en
  continu, chaque scan incrémente la quantité de la ligne. Aujourd'hui chaque scan
  rouvre la caméra puis un dialogue.
- **Pas de support douchette Bluetooth/HID** : champ de saisie toujours focalisé qui
  capte les scans clavier (suffixe Entrée), indispensable sur les gros volumes.

### M3. Volumétrie et navigation dans la liste (majeur)

Un inventaire MAGASIN = 5 000 à 20 000 références ; l'app charge tout en mémoire
(boucle de pages de 500) sans recherche ni filtre :

- Recherche texte (le paramètre `search` est déjà câblé jusqu'au repository).
- Filtres backend existants à exposer : `selectedFilter` = NOT_UPDATED / UPDATED /
  GAP / GAP_POSITIF / GAP_NEGATIF — « ce qui reste à compter » est la vue clé.
- Vraie pagination UI (Paging 3 est déjà dans les dépendances, inutilisé).
- Tri (rayon, libellé).

### M4. Métier officine (majeur — priorité confirmée)

- **Multi-opérateurs** : un inventaire se compte à plusieurs. Aucune affectation de
  rayon par opérateur, aucune protection contre l'écrasement concurrent
  (last-write-wins, y compris côté backend). Voir chantier transverse B2.
- **Récapitulatif avant clôture** : la clôture est irréversible et se confirme sur un
  simple oui/non. Afficher avant clôture : lignes non comptées (filtre NOT_UPDATED),
  écarts valorisés (`gapCost`, `gapAmount`), progression — les données existent déjà.
- ~~**Déconditionnement**~~ : **sans objet** (tranché le 2026-07-31). Un produit
  déconditionné est une entité `Produit` distincte (liée au parent par `parentid`), avec
  son propre code et sa propre ligne d'inventaire — ni la création d'inventaire ni
  `StoreInventoryLineFilterBuilder` ne filtrent sur `parent_id`. Le comptage le traite
  donc déjà comme un produit à part entière : rien à implémenter.
- **Stupéfiants** : comptage contradictoire (double comptage par deux opérateurs) et
  traçabilité renforcée réglementaires — rien de prévu, ni mobile ni backend.
- **Mode aveugle** : le web masque le stock théorique selon le privilège
  `PR_VOIR_STOCK_INVENTAIRE` ; le mobile affiche toujours `quantityInit`. Aligner.

### M5. Sécurité (majeur)

- `usesCleartextTraffic="true"` + `BASE_URL` en `http://` : JWT et données de stock en
  clair sur le Wi-Fi. Cible : TLS (profil `tls` backend) ou à défaut
  `networkSecurityConfig` restreinte au LAN.
- **Refresh token backend = 501** (`AuthenticationResource.refresh`) : session de 8 h
  max, déconnexion possible en plein comptage. Gérer aussi le 401 en cours de saisie
  (re-login sans perte de contexte, la saisie étant en Room grâce à M1).

### M6. Finitions

- Indicateur de connectivité/synchro (lié à M1).
- Écran splash/auto-login et parcours de navigation à valider de bout en bout.
- Aucun test (unitaire ou instrumenté) — la logique repository/ViewModel s'y prête
  désormais bien.
- Dépréciations `EncryptedSharedPreferences`/`MasterKey` dans `TokenManager` (API
  security-crypto dépréciée).

---

## 3. Lacunes identifiées — Web (`features/inventory`)

### W1. Pas de flux « scan douchette » dans l'éditeur (mineur — le comptage se fait au mobile)

La grille a une excellente navigation clavier (Entrée → ligne suivante, changement de
page automatique) et une recherche, mais il n'existe **aucun champ scan dédié** :
scanner un code-barres avec une douchette ne sélectionne pas la ligne du produit et
n'incrémente rien. Le comptage étant réalisé au mobile, ce chantier devient optionnel
(utile en dépannage au comptoir). Si réalisé : champ scan global toujours focalisable
dans `inventory-editor` (capture du suffixe Entrée), résolution CIP/EAN → focus de la
ligne (+1 optionnel), parsing GS1 pour préremplir la grille lots.

### W2. Aucune visibilité temps réel sur le comptage mobile (majeur — cœur du rôle du web)

`refreshProgress()` n'est appelé qu'après **ses propres** actions : le web, poste de
supervision, **ne voit pas avancer les compteurs mobiles** sans recharger la page —
ni progression, ni quantités saisies. C'est la lacune web prioritaire puisque le rôle
du web est précisément de piloter le comptage fait au mobile. Options par coût
croissant : polling léger de `/progress` (+ rechargement de la page de grille si
`updatedLines` bouge), SSE, ou websocket. Le polling suffit pour commencer.

### W3. Garde-fous à la clôture (moyen)

`closeInventory()` = simple dialogue de confirmation. Les composants existent déjà
(`GapSummaryComponent`, `InventoryValuationComponent`, filtre NOT_UPDATED) : afficher
un récapitulatif bloquant — X lignes non comptées, écart valorisé total — avant de
confirmer. Cohérent avec M4 côté mobile.

### W4. Traçabilité du comptage (moyen — dépend du backend B4)

Ni la grille ni le PDF n'indiquent **qui** a compté une ligne et **quand**.
`StoreInventoryLineRecord` ne porte ni utilisateur ni horodatage — complément backend
nécessaire (B4), puis colonne grille + PDF.

### W5. Aucune spec Jest sur le module (moyen)

Zéro `*.spec.ts` dans `features/inventory` alors que facades/store (signals) sont
facilement testables. Prioriser : `InventoryEditorFacade`, `InventoryStore`,
logique de filtres de `inventory-editor.component`.

---

## 4. Compléments backend transverses

- **B1. Refresh token** : implémenter `POST /api/auth/refresh` (actuellement 501).
  Préalable à M5.
- **B2. Concurrence** : `updateQuantityOnHand` et `/batch` sont en last-write-wins.
  Ajouter un contrôle optimiste (ex. `updatedAt` attendu dans le DTO → 409 en cas de
  conflit) et/ou une affectation opérateur↔rayon (nouvelle table ou champ sur la ligne)
  pour le comptage à plusieurs. Préalable à M4/W2 complets.
- **B3. Synchro ligne parente des lots** : seul le PUT d'un lot resynchronise
  `quantityOnHand` de la ligne (somme des lots) ; le POST de création et le DELETE ne
  le font pas (le mobile contourne en ré-émettant un PUT après création). Corriger dans
  `InventoryLotServiceImpl.save()`/`delete()` en appelant `syncParentLineQuantity`.
- **B4. Audit par ligne** : exposer `updatedAt` + utilisateur du dernier comptage dans
  `StoreInventoryLineRecord` (et le stocker si absent). Alimente W4 et la traçabilité
  stupéfiants.
- **B5. Stupéfiants** : flag produit/famille « stupéfiant », comptage contradictoire
  (deux saisies concordantes exigées avant validation), journal dédié. À spécifier avec
  le métier.

---

## 5. Phasage proposé

### Phase 1 — Fiabilité terrain (mobile) ✅ implémentée le 2026-07-31
| # | Chantier | Contenu |
|---|----------|---------|
| 1.1 | M1 | Offline-first : Room source de vérité, saisies locales, SyncWorker sur reconnexion |
| 1.2 | M1/M6 | Indicateurs connectivité + « N lignes en attente de synchro » |
| 1.3 | M5 | Gestion 401/retry sans perte de saisie (s'appuie sur 1.1) |

### Phase 2 — Cadence de comptage (mobile) ✅ implémentée le 2026-07-31
| # | Chantier | Contenu |
|---|----------|---------|
| 2.1 | M2 | Parsing GS1 DataMatrix (AI 01/17/10) → pré-remplissage lot + péremption |
|2.2 | M2 | Mode scan continu « chaque scan = +1 », caméra persistante |
| 2.3 | M2 | Support douchette HID (champ scan toujours focalisé) |
| 2.4 | M3 | Recherche + filtres NOT_UPDATED/GAP± + tri (API déjà prête) |

### Phase 3 — Métier officine (mobile + backend) 🟠 partiellement implémentée le 2026-07-31
| # | Chantier | Contenu | État |
|---|----------|---------|------|
| 3.0 | (ajout) | **Permission de clôture** : nav ACTION `pr-cloture-inventaire` (accordé à ROLE_ADMIN + ROLE_PHARMACIEN), **contrôle serveur** `@PreAuthorize("hasAuthority('pr-cloture-inventaire')")` sur `GET /api/store-inventories/close/{id}`, bouton masqué côté web et mobile | ✅ |

> Les trois chantiers ci-dessus partagent une seule migration :
> **`V1.8.5__inventaire_cloture_tracabilite_verrou.sql`** (privilège de clôture,
> `counted_by_id`, `version`).
| 3.1a | M4/B4 | **Traçabilité du comptage** : colonne `counted_by_id` alimentée automatiquement aux 3 points d'écriture (ligne unitaire, batch, sync par lot) ; `countedBy` + `updatedAt` exposés dans `StoreInventoryLineRecord` ; affichés dans la grille web et sur la ligne mobile | ✅ |
| 3.1b | M4/B2 | **Verrou optimiste** : colonne `version` `@Version`, renvoyée par le client et comparée côté serveur. Saisie unitaire → `OptimisticLockException` → **409** (via l'`ExceptionTranslator` existant) ; batch → `conflictedIds` isolés du reste du lot. Mobile : statut `CONFLICT` en Room, bandeau d'arbitrage, jamais rejoué automatiquement. Web : rechargement + notification | ✅ |
| ~~3.1c~~ | M4 | ~~Affectation rayon↔opérateur~~ | ❌ écarté (2026-07-31) — coût administratif réel, ne protège pas des collisions (un produit peut appartenir à plusieurs rayons, `rayon_produit` est N-N), et le filtre rayon mobile couvre déjà le besoin pratique. L'avancement par rayon se dérive de la trace. |
| 3.2 | M4 | Récapitulatif pré-clôture (comptées/restantes, lignes avec écart, écarts valorisés achat/vente) | ✅ mobile |
| 3.3 | M4 | Mode aveugle mobile : sans `pr-voir-stock-inventaire`, stock théorique et écarts masqués (lignes + lots), fail-closed | ✅ |
| 3.4 | M4/B5 | Stupéfiants : comptage contradictoire | ⏳ spéc. métier préalable |
| ~~3.5~~ | M4 | ~~Déconditionnement (boîtes vs unités)~~ | ❌ sans objet — produit déconditionné = `Produit` distinct, déjà compté comme tel |
| 3.6 | B3 | Fix backend : POST (avec quantité) et DELETE d'un lot resynchronisent la ligne parente ; contournement mobile retiré | ✅ |

Note permissions mobile : nouvel endpoint léger **`GET /api/nav/my-abilities`** qui renvoie
uniquement la liste plate des codes ACTION exécutables (`["pr-cloture-inventaire", …]`), au
lieu de l'arbre de navigation complet `/api/nav/my-items` destiné au menu web. L'app le
charge à l'ouverture de l'écran de comptage, met les codes en cache
(EncryptedSharedPreferences) pour le mode hors ligne, et applique un comportement
fail-closed (sans info : clôture masquée, stock masqué). Comme pour le web, l'application
de ces permissions est côté client — le backend n'authentifie que la session (pattern
existant du projet, cf. `ProduitMergeResource`). À noter : ces mêmes codes ACTION sont déjà
fusionnés dans les authorities Spring Security à la connexion
(`DomainUserDetailsService`), ce qui permettrait plus tard un contrôle serveur par
`@PreAuthorize("hasAuthority('pr-cloture-inventaire')")` sur la clôture.

### Phase 4 — Sécurité & robustesse 🟠
| # | Chantier | Contenu |
|---|----------|---------|
| 4.1 | M5/B1 | TLS (ou networkSecurityConfig LAN) + refresh token backend |
| 4.2 | M6 | Tests mobile (repository avec MockWebServer, ViewModels) |
| 4.3 | M6 | Migration security-crypto (dépréciations TokenManager) |

### Phase 5 — Web (supervision du comptage mobile) 🟡 largement implémentée le 2026-07-31
| # | Chantier | Contenu | État |
|---|----------|---------|------|
| 5.1 | W2 | **Suivi temps réel** : scrutation de `/progress` toutes les 15 s tant que l'inventaire est ouvert ; la grille se recharge seule quand `updatedLines` bouge. **Jamais pendant une saisie locale** — un bandeau « des comptages ont été enregistrés depuis un autre poste » propose alors un rafraîchissement manuel | ✅ |
| 5.2 | W3 | **Modale récapitulative de clôture** : comptées/total, restantes, lignes avec écart, écart valorisé achat+vente (via `/valuation`), avertissement si des lignes restent, mention « irréversible ». Remplace la confirmation à l'aveugle | ✅ |
| 5.3 | W4/B4 | Colonne « Compté par » + horodatage dans la grille | ✅ grille — ❌ **PDF écarté** (voir note) |
| 5.4 | W5 | **Specs Jest** : `inventory.store.spec.ts` + `inventory-editor.facade.spec.ts` — 18 tests, dont le transport de la version (verrou optimiste) dans le batch et la remontée du 409 | ✅ |
| 5.5 | W1 | (Optionnel) Champ scan douchette + parsing GS1 dans l'éditeur | ⏳ optionnel — le comptage se fait au mobile |

**Note 5.3 (PDF) — écarté, décision du 2026-07-31.** La traçabilité du comptage n'est
pas portée par l'export PDF : elle reste consultable dans la grille web, où se fait la
supervision. Raisons : le document est en **portrait** (`common/portrait_table`) avec
7 colonnes déjà calibrées à 100 % (12+45+11+11+9+6+6) — une colonne de noms amputerait
le libellé produit — et le pied de page a des `colspan` (7, puis 2+1+1+3) liés
arithmétiquement au nombre de colonnes, donc impossibles à modifier sans contrôle visuel
du PDF généré.

Si le besoin réapparaît, la voie recommandée est **le niveau groupe, pas la ligne** :
afficher les compteurs distincts dans l'en-tête de rayon (« Rayon X — compté par
K. Kobena, M. Diallo »), calculés en Java depuis `group.items`. Coût nul en largeur,
aucun `colspan` à toucher, et l'information est de toute façon constante sur un rayon.

**Ordre recommandé : 1 → 2 → 3 → 4 → 5.** Le mobile est l'outil de comptage : les
phases 1–2 le rendent fiable et rapide en conditions d'officine, la phase 3 traite le
travail à plusieurs et la conformité. Le web (5) suit, recentré sur la supervision ;
le chantier 5.1 (W2) peut néanmoins être intercalé dès la phase 3 puisqu'il complète
le comptage multi-opérateurs.

---

## 6. Critères d'acceptation clés

- **Offline** : couper le Wi-Fi, compter 20 produits, rallumer → tout se synchronise
  sans action utilisateur ni perte ; l'UI a affiché en continu l'état d'attente.
- **GS1** : scanner un DataMatrix de boîte → produit trouvé, lot et péremption
  pré-remplis ; un second scan de la même boîte incrémente de 1.
- **Concurrence** : deux opérateurs sur le même produit → le second reçoit un conflit
  explicite (pas d'écrasement silencieux).
- **Clôture** : impossible de clôturer sans avoir vu le nombre de lignes non comptées
  et l'écart valorisé.
- **Session** : après expiration du token en plein comptage, l'utilisateur se
  reconnecte et retrouve sa saisie intacte.
