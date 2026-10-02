# Analyse comparative — Écran de gestion des tiers payants (`TiersPayantComponent`) vs logiciels d'officine du marché

> Statut : **analyse** — aucune ligne de code écrite.
> Date : octobre 2026.
> ⚠ Les écarts §3, §4 et §5 ci-dessous ont une **proposition de résolution structurelle unique**,
> plutôt que des correctifs isolés : voir
> [PLAN-REFONTE-MASTER-DETAIL-TIERS-PAYANT.md](PLAN-REFONTE-MASTER-DETAIL-TIERS-PAYANT.md)
> (master/detail sur le patron déjà existant pour les produits, `produit-home`/`produit-detail-panel`).
> Portée analysée : `pharmaSmart-app/src/main/webapp/app/entities/tiers-payant/tiers-payant.component.{html,ts}`
> (écran de référentiel : liste, recherche, création/édition via modale, import, désactivation,
> suppression) + chaîne back : `TiersPayantResource`, `TiersPayantDataService`,
> `TiersPayantServiceImpl`.
> Méthode et référentiel marché identiques aux analyses précédentes :
> [PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md](PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md),
> [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-CLIENT-ASSURE.md),
> [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md)
> (ce dernier analyse la modale de création/édition elle-même, ouverte depuis cet écran — ne pas
> dupliquer son contenu ici).
> Comparatif fait du point de vue **back-office officine** (le profil qui gère au quotidien le
> fichier des organismes payeurs), face à Winpharma, LGPI, Smart Rx, Alliance Healthcare, Covalia,
> et aux solutions ouest-africaines de terrain (PharmaSoft CI, Atlantic Pharma).

---

## 1. Ce que fait l'écran aujourd'hui

Liste paginée (`app-data-table`, sélection simple) des tiers payants avec : filtre par catégorie
(TOUT/ASSURANCE/CARNET/DEPOT), recherche texte, colonnes (type, code, nom abrégé, nom long,
téléphone, plafond conso clients, plafond journalier client, encours, nombre de clients), actions
par ligne (voir tarifs produits négociés, éditer, désactiver, supprimer), bouton « Nouveau tiers
payant » à choix multiple (ASSURANCE/CARNET/DEPOT), import JSON en masse, spinner de progression.

---

## 2. Ce qui existe déjà (ne pas refaire)

| Fonction | État | Fichier |
|---|---|---|
| Filtre par catégorie + recherche texte combinés | ✅ | `onSearch()` |
| Pagination lazy avec choix du nombre de lignes (10/15/20/30/50) | ✅ | `lazyLoading()` |
| Confirmation avant suppression et avant désactivation | ✅ Bonne pratique, pas systématique chez la concurrence low-cost | `confirmDialog.onConfirm` |
| Accès direct aux tarifs produits négociés par organisme depuis la ligne | ✅ Fonction utile, peu courante de façon aussi directe | `voirTarifsProduits()` |
| Import JSON en masse avec barre de progression | ✅ | `openJsonImport()`, `jsonFileUploadProgress` |
| Création contextualisée par catégorie dès le bouton d'ajout (évite de choisir la catégorie dans un champ du formulaire) | ✅ | `tiersPayantSplitbuttons` |
| Portefeuille de factures filtrable par tiers payant, avec rapprochement bancaire et export Excel | ✅ Module `/facturation` séparé, mais fonctionnellement riche — supérieur à la plupart des concurrents | `facturation-edition`, `rapprochement`, `recapitulatif` (cf. §5) |
| Historique des règlements filtrable | ✅ Module `/facturation` séparé | `reglement-workspace`, `reglement-api.service` (cf. §5) |

---

## 3. Écart n°1 — Aucun moyen de revoir ou réactiver un tiers payant désactivé

### Le constat
`GET /api/tiers-payants` (`TiersPayantResource.getAll`) **force le statut `ACTIF`** côté serveur —
ce n'est même pas un paramètre de requête optionnel, c'est une valeur en dur
(`TiersPayantStatut.ACTIF`) dans l'appel à `tiersPayantDataService.fetchList(...)`. Le front ne
propose donc ni filtre de statut, ni colonne « Statut » dans le tableau. Dès qu'un organisme est
désactivé via le bouton « Désactiver » (`confirmDesactivation` → `desable()`), **il disparaît
définitivement de cet écran** — aucun bouton « Réactiver » n'existe nulle part dans l'application
pour ce composant.

### Pourquoi c'est un écart avec les usages courants
Désactiver un tiers payant (fin de partenariat, organisme en restructuration, erreur de
manipulation) est une opération réversible dans tous les logiciels de référence : un filtre
« Actifs / Désactivés / Tous » ou une case à cocher reste toujours disponible pour retrouver et
réactiver une entrée désactivée par erreur, ou simplement consulter l'historique des organismes avec
lesquels l'officine a travaillé. Ici, une désactivation accidentelle (clic sur le mauvais bouton
dans la liste d'actions, les cinq boutons étant regroupés sans confirmation différenciée visuelle
forte) est **irréversible via l'interface** : il faudrait une intervention en base de données pour
revenir en arrière.

### Ce qu'il faudrait
Un filtre de statut (actif/désactivé/tous) à côté du filtre de catégorie, une colonne ou un badge de
statut dans le tableau, et une action « Réactiver » symétrique à « Désactiver » pour les lignes
désactivées.

---

## 4. Écart n°2 — Une fonctionnalité de mise à jour groupée existe côté serveur, inutilisable depuis cet écran

### Le constat
`TiersPayantService.massUpdateFactureConfig(ids, config)` (front) et l'endpoint associé
`PATCH /tiers-payants/mass-update-facture-config` (back, `TiersPayantResource`) permettent de
modifier en une seule opération la périodicité et l'inclusion de facturation automatique
(définitive/provisoire) pour **plusieurs tiers payants à la fois**. Pourtant, `[selectionMode]`
du tableau est fixé à `'single'`, et **aucun composant de l'application n'appelle cette méthode** —
recherche confirmée dans l'ensemble des fichiers `.component.ts`/`.html`. La capacité existe
côté service/back, mais n'est exposée nulle part dans l'interface.

### Pourquoi c'est un écart avec les usages courants
Reconfigurer la périodicité de facturation organisme par organisme, un par un, via la modale
complète (cf. analyse du formulaire tiers payant) est exactement le type de tâche répétitive que
les logiciels de référence permettent de traiter en masse (sélection multiple + action groupée) —
par exemple au moment d'un changement de politique de facturation s'appliquant à tout un groupe
d'organismes. Ici, malgré une API prête à cet effet, l'utilisateur est contraint de rouvrir la
modale complète pour chaque tiers payant un par un.

### Ce qu'il faudrait
Passer `selectionMode` à multiple, ajouter une action groupée (« Modifier la facturation de la
sélection ») déclenchant `massUpdateFactureConfig` avec une mini-modale ne portant que les 4 champs
concernés (périodicités + inclusions).

---

## 5. Écart n°3 — Un « Voir détails » mort, alors que la matière (factures, règlements) existe déjà ailleurs dans l'app

> Point vérifié suite à une question directe : les logiciels concurrents affichent-ils, pour un
> tiers payant donné, ses clients, ses factures et ses règlements ? **Oui, et PharmaSmart aussi** —
> la confusion initiale (ci-dessous corrigée) venait du fait que cette matière existe dans un module
> **séparé** (`/facturation`), pas d'une absence réelle de la fonctionnalité.

### Le constat, précisé
Le bouton « Voir détails » de chaque ligne pointe vers `/tiers-payant/:id/view` — une route qui
**n'existe tout simplement pas** dans `tiers-payant.route.ts` (seule la route `''` vers
`TiersPayantHomeComponent` est déclarée). C'est donc un lien mort au sens strict. Mais contrairement
à ce qu'on pourrait en déduire, **les informations qu'on attendrait d'une fiche détaillée existent
déjà et sont même assez développées** :

- **Factures** : le module `/facturation` (`facturation-edition`, `recapitulatif`,
  `rapprochement`) permet déjà de filtrer un portefeuille de factures par tiers payant (recherche
  avec suggestions `tiersPayantSuggestions`, sélection multiple `tiersPayantIds`), avec export Excel
  et rapprochement bancaire — une richesse fonctionnelle **supérieure** à beaucoup de logiciels
  concurrents qui n'offrent qu'un relevé de factures simple sans rapprochement dédié.
- **Règlements** : `reglement-workspace`/`reglement-api.service` couvrent l'historique des
  règlements, également filtrable.
- **Clients de l'organisme** : en revanche, ici un vrai manque confirmé — `customer.component.ts`
  ne filtre les clients assurés que par **catégorie large** (TOUT/ASSURE/CARNET/DEPOT via
  `CustomerDataService`), pas par un **tiers payant précis**. Il n'existe nulle part un écran
  « tous les clients de la Mutuelle X » alors que la colonne « Nbre Clients » de cet écran prouve
  que la donnée (`ClientTiersPayant` par `tiersPayantId`) est déjà connue côté serveur.

### Pourquoi c'est un écart avec les usages courants
Les logiciels de référence qui gèrent des tiers payants consolident presque toujours une **fiche
dossier** par organisme (comparable à la fiche client 360° déjà existante côté `customer-detail` dans
cette même application) : en un clic depuis la ligne de l'organisme, on retrouve son encours, ses
factures, ses règlements, **et** la liste de ses adhérents/clients rattachés — sans devoir changer
de module et re-sélectionner l'organisme dans trois écrans différents (`/facturation` →
récapitulatif, puis rapprochement, puis chercher les clients ailleurs sans filtre dédié). Ici, la
donnée existe mais **éclatée entre deux modules sans pont** : `/tiers-payant` (référentiel) et
`/facturation` (factures/règlements), plus un angle mort sur la liste de clients par organisme.

### Ce qu'il faudrait
1. Construire la route `/tiers-payant/:id/view` (ou une modale « Dossier ») qui **renvoie** vers le
   module facturation déjà filtré sur ce tiers payant (deep-link avec `tiersPayantIds` préremplis)
   plutôt que de réimplémenter les factures/règlements en double.
2. Ajouter un filtre par tiers payant précis (pas seulement par catégorie) sur l'écran clients, ou à
   défaut une sous-liste directement dans cette fiche dossier — c'est la seule vraie lacune de
   contenu identifiée ici, le reste (factures, règlements) étant déjà couvert ailleurs dans l'app.
3. Nettoyer ou réactiver le bouton toolbar « Importation » (`[hidden]="true"` alors que
   `openJsonImport()` reste fonctionnel) — sujet distinct du point précédent mais même type de
   nettoyage de dette.

---

## 6. Ergonomie — facilité d'utilisation au quotidien

### 6.1 Recherche texte et filtre catégorie n'ont pas le même déclencheur
Le filtre catégorie (`app-select`) relance la recherche immédiatement (`(selectionChange)="onSearch()"`),
alors que le champ texte de recherche nécessite soit d'appuyer sur `Entrée`
(`(keyup.enter)="onSearch()"`), soit de cliquer sur le bouton « Rechercher » séparé. Deux contrôles
côte à côte dans la même barre d'outils, avec deux comportements différents, désoriente
l'utilisateur qui s'attend à la cohérence (taper puis voir la liste se filtrer, comme le fait déjà
`customer-search-table` ailleurs dans l'app avec un typeahead après 2 caractères).

### 6.2 Filtre et recherche non conservés en cas de rafraîchissement
Seuls `page` et `size` sont poussés dans l'URL (`router.navigate`) au chargement d'une page ; le
terme de recherche et la catégorie sélectionnée ne le sont pas. Un rafraîchissement de la page, un
retour arrière navigateur, ou le partage d'un lien vers une liste filtrée perd ces deux critères —
contrairement aux attentes d'un écran de gestion où l'on revient souvent consulter la même vue
filtrée (ex. « tous les DEPOT » consultés plusieurs fois dans la journée).

### 6.3 Montants sans unité affichée dans les colonnes
« Plafond.Conso.Clients », « Plafond.Journalier.Client » et « Encours » affichent des nombres bruts
(`| number`) sans suffixe de devise — écho du même constat déjà fait sur les champs de saisie de la
modale (cf. analyse du formulaire tiers payant, §7.1). Dans une liste consultée rapidement, l'unité
devrait être d'autant plus explicite qu'il n'y a pas de contexte de saisie pour la déduire.

### 6.4 Libellés de colonnes à la ponctuation incohérente
« Plafond.Conso.Clients » et « Plafond.Journalier.Client » utilisent des points comme séparateurs,
alors que toutes les autres colonnes (« Nom abrégé », « Nbre Clients ») utilisent des espaces. Détail
mineur mais visible en permanence sur l'écran principal de gestion.

### 6.5 Pas de tri par colonne
Aucun en-tête de colonne n'est cliquable pour trier (ni `sortField`, ni indicateur visuel de tri
dans le template). Un back-office qui veut identifier rapidement les organismes avec le plus gros
encours ou le plus grand nombre de clients doit parcourir toutes les pages manuellement, alors que
le tri par colonne est une attente standard de tout tableau de gestion.

### 6.6 Pas d'export symétrique à l'import
Un import JSON existe (même si son bouton d'accès direct est actuellement masqué, cf. §5), mais
aucune action d'export (CSV/Excel) n'est proposée pour extraire la liste filtrée — utile pour un
contrôle externe (comptable, auditeur, négociation avec un organisme) sans devoir redemander un
export technique à l'équipe de développement.

---

## 7. Synthèse priorisée

| # | Constat | Impact usage quotidien | Effort estimé |
|---|---|---|---|
| §3 | Statut forcé à ACTIF côté serveur, pas de réactivation possible | Élevé — désactivation irréversible via l'IHM | Moyen (filtre + action réactiver, déjà le même patron que `changeStatus` côté client) |
| §4 | Mise à jour groupée de facturation inaccessible malgré une API prête | Moyen à élevé — tâche répétitive non outillée | Faible à moyen (surtout du câblage UI, la logique serveur existe déjà) |
| §6.1 | Recherche texte et filtre catégorie incohérents (clic vs immédiat) | Moyen — confusion d'usage quotidien | Faible |
| §6.2 | Recherche/filtre non persistés dans l'URL | Moyen — perte de contexte à chaque rafraîchissement | Faible |
| §6.5 | Pas de tri par colonne | Moyen — pénible sur un référentiel qui grossit | Faible à moyen |
| §5 | Pas de fiche dossier reliant tiers payant ↔ factures/règlements (module `/facturation` existant mais déconnecté) ↔ liste de ses clients (filtre par organisme précis absent) | Moyen à élevé — navigation éclatée entre modules pour une même tâche (suivre un organisme) | Moyen (deep-link vers l'existant, pas de redéveloppement de factures/règlements) |
| §6.3 | Montants sans unité affichée | Faible à moyen | Faible |
| §6.6 | Pas d'export symétrique à l'import | Faible à moyen | Faible à moyen |
| §6.4 | Incohérence de ponctuation des libellés de colonnes | Trivial | Trivial |

---

## 8. Hors périmètre volontaire

- La modale de création/édition elle-même (champs, validations, écarts de contenu) est déjà
  détaillée dans
  [PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md](PLAN-ANALYSE-COMPARATIVE-FORMULAIRE-TIERS-PAYANT.md)
  et n'est pas reprise ici.
- Le constat d'absence de filtre de statut (§3) existe potentiellement aussi sur l'écran de gestion
  des clients (`customer.component.ts`) — non vérifié en détail ici, à confirmer séparément si une
  correction transverse est envisagée plutôt qu'un correctif isolé à cet écran.

