# Plan — Dashboard personnalisable (façon Kibana)

> État des lieux au 2026-09-28 et plan de réalisation.

## En bref

L'écran `/dashboard` n'est qu'une maquette. On peut déplacer des tuiles vides, mais aucune n'affiche
de données. La sauvegarde perd le type de chaque widget, et un layout enregistré n'est jamais
affiché ni rechargé correctement. Le backend `dashboard_layout` est solide et on le garde. Le front
est à reconstruire.

## 1. Ce qui existe

| Brique | Fichier | État |
|---|---|---|
| Écran principal (GridStack 13) | `entities/dashboard/customizable-dashboard/customizable-dashboard.component.ts` | Grille fonctionnelle, contenu factice |
| 3 fenêtres (ajouter, sauvegarder, charger) | `customizable-dashboard/*-modal.component.ts` | Fonctionnent |
| 5 composants de widget (KPI, courbe, barres, camembert, tableau) | `entities/dashboard/widgets/*` | **Jamais utilisés**, données de démo codées en dur |
| Modèle | `shared/model/dashboard-layout.model.ts` | `dataSource`, `refreshInterval` et `config` sont prévus mais jamais utilisés |
| API | `DashboardLayoutResource`, `DashboardLayoutServiceImpl` | CRUD, choix du layout par défaut (utilisateur puis rôle), clonage, cache |
| Accueil | `home/home.component.html:42` | `<!-- TODO : <app-customizable-dashboard> -->` : l'accueil affiche `home-base` à la place |
| Sources de données possibles | `DashboardResource` (`/api/dashboard/*`), `CaissierDashboardResource`, `ResponsableCommandeDashboardResource` | Une quinzaine d'endpoints réutilisables |

À savoir : la table `dashboard_layout` sert déjà au **choix de l'accueil par rôle** (`componentKey`
PHARMACIEN, CAISSIER ou COMMANDE, et `isRoute` ; voir `V1.4.9__dashboard_layout_authority.sql`). Le
dashboard personnalisé partage cette table.

Aucune entrée de menu (`nav_item`) ne pointe vers `/dashboard` : on n'y accède qu'en tapant l'URL.

## 2. Pourquoi ça ne marche pas

### Front

1. **Le layout par défaut ne s'affiche jamais.** `ngOnInit` lance `loadDefaultLayout()` sans
   attendre la réponse. `ngAfterViewInit` crée la grille avant qu'elle n'arrive, et `currentLayout`
   est encore vide. Quand la réponse arrive, rien ne remplit la grille.
2. **Les widgets sont des coquilles vides.** `createWidgetContent()` injecte du HTML statique (icône
   et titre) via `innerHTML`. Les composants de `widgets/` ne sont jamais créés.
3. **La sauvegarde écrase les widgets.** Dans `saveLayout()`, chaque tuile est enregistrée avec
   `type: KPI_CARD, title: 'Widget'` codés en dur. Une fois rechargé, tout devient « Widget ».
4. **« Sauvegarder » crée toujours un nouveau layout.** Il n'existe ni mise à jour, ni « définir par
   défaut », ni lien avec l'accueil.
5. **La suppression d'une tuile laisse un fantôme.** Le bouton
   `onclick="this.closest(...).remove()"` retire l'élément de la page sans prévenir GridStack. Un
   gestionnaire `onclick` écrit dans le HTML risque aussi d'être bloqué par la CSP de Tauri. Enfin,
   le bouton reste visible hors du mode édition.
6. **Faille XSS.** Le titre est injecté sans échappement (`${widgetConfig.title}`). Un layout
   partagé ou public peut donc exécuter du script chez les autres utilisateurs.
7. **« Charger » liste les layouts système** (`home-base`, `/sales-home/prevente`…, qui sont
   PUBLIC). Les charger donne une grille vide.

### Backend

8. **N'importe quel utilisateur peut modifier ou supprimer les layouts système.** `update` et
   `delete` ne vérifient le propriétaire que si `layout.getUser() != null`. Les layouts livrés à la
   création de la base n'ont pas de propriétaire, donc aucun contrôle ne s'applique.
9. **`clone()` ne copie pas `componentKey`.** L'insertion envoie `NULL` sur une colonne `NOT NULL`,
   ce qui provoque une erreur. Même si elle passait, `toDTO` planterait sur
   `getComponentKey().name()`.
10. **`update()` perd le `componentKey` existant.** Les lignes 94 et 96 se contredisent, et la
    ligne 96 remet `ROUTE` quand le DTO n'en fournit pas.
11. **`setAsDefaultForRole` n'est pas réservé aux administrateurs,** alors que le commentaire le
    prétend. Les erreurs sont des `RuntimeException` au lieu de `GenericError`.
12. **Un layout personnel est sauvegardé avec `componentKey = ROUTE`.** Même corrigé, l'accueil
    tomberait dans la branche `@default`, qui affiche `home-base`.

## 3. Ce que « façon Kibana » demande

| Fonction Kibana | Équivalent Pharma-Smart |
|---|---|
| Sélecteur de période global (aujourd'hui, 7 j, mois…) qui s'applique à tous les panneaux | Période commune à tous les widgets, avec une période propre possible par widget |
| Filtres globaux | Magasin/dépôt, type de vente, rayon… |
| Panneau = source de données + visualisation + paramètres | Catalogue de **sources** (CA, top produits, alertes stock…) × **types de graphique** autorisés |
| Mode édition / mode lecture | Existe déjà, à fiabiliser |
| Rafraîchissement automatique | Intervalle global (désactivé, 30 s, 1 min, 5 min) |
| Réglages d'un panneau (titre, paramètres) | Fenêtre de configuration du widget |
| Dupliquer, partager, dashboard par défaut | Portée PRIVATE, SHARED ou PUBLIC, défaut par utilisateur et par rôle |
| Clic sur un graphique pour filtrer | En phase 4 (optionnel) |

## 4. Ce que font les autres outils

### 4.1 Kibana (Elastic)

- **Bibliothèque de visualisations** : chaque visualisation est un objet sauvegardé indépendant, avec
  un titre, une source (*data view*), un type de graphique et ses paramètres. On la crée une fois,
  puis on la réutilise dans plusieurs dashboards.
- **Ajout d'un panneau** : le bouton « Ajouter un panneau » ouvre un panneau latéral avec deux
  entrées.
  - **Créer une visualisation** : l'éditeur Lens, où l'on glisse des champs et où Kibana propose le
    type de graphique.
  - **Ajouter depuis la bibliothèque** : une liste recherchable des visualisations déjà
    enregistrées, filtrable par type et par étiquette. Un clic suffit à ajouter la visualisation.
- **Deux façons d'ajouter** : un panneau *lié à la bibliothèque* suit les modifications de
  l'original. Un panneau *par valeur* en est une copie propre au dashboard. On passe de l'un à
  l'autre (« Enregistrer dans la bibliothèque » / « Dissocier »).
- **Contexte global** : barre de recherche, filtres (« pills »), sélecteur de période et
  rafraîchissement automatique, appliqués à tous les panneaux. Un panneau peut garder sa propre
  période.
- **Contrôles** : listes déroulantes, curseurs et plages placés en tête du dashboard, qui filtrent
  tous les panneaux.
- **Menu d'un panneau** : modifier, dupliquer, copier vers un autre dashboard, agrandir,
  « Inspecter » (voir les données et la requête), personnaliser le titre, supprimer.
- **Interactions** : cliquer sur une barre ou une part ajoute un filtre au dashboard. Les
  *drilldowns* ouvrent un autre dashboard ou une URL.
- **Partage** : lien, export PDF/PNG, rapports planifiés, export et import en JSON (*saved
  objects*).

### 4.2 Grafana

- **Ajout** : « Ajouter une visualisation » ou « Importer un panneau de la bibliothèque ». Les
  *library panels* sont partagés : les modifier les modifie partout.
- **Variables de dashboard** : listes en tête de page (serveur, magasin…) injectées dans les
  requêtes. C'est l'équivalent des filtres globaux, construit sans code.
- **Lignes repliables** pour regrouper les panneaux par thème.
- **Dashboard = JSON** versionné, avec un historique des versions et une restauration possible.
- **Seuils et couleurs** par panneau (vert, orange, rouge), et alertes branchées sur un panneau.

### 4.3 Metabase

- **Une « question » = un widget** : on l'enregistre depuis un écran de requête simple, puis on
  l'ajoute à un ou plusieurs dashboards.
- **Filtres du dashboard reliés aux cartes** : on choisit, pour chaque carte, quel champ le filtre
  pilote. Une carte non reliée ignore le filtre.
- **Cartes texte et titres** pour structurer ou commenter.
- **Abonnements** : envoi du dashboard par e-mail à une fréquence choisie.

### 4.4 Power BI et Odoo

- **Épingler** : depuis n'importe quel rapport ou vue de liste, le bouton « Épingler au tableau de
  bord » (Power BI) ou « Ajouter au tableau de bord » (Odoo) crée le widget avec les filtres en
  cours. On n'a pas besoin de passer par un éditeur.
- **Odoo** propose en plus un tableau de bord personnel par utilisateur, qui se construit uniquement
  de cette façon.

### 4.5 Ce qu'on retient pour Pharma-Smart

Les utilisateurs sont des pharmaciens, des caissiers et des responsables de stock, pas des
analystes. Un éditeur libre à la Lens serait trop complexe. On retient :

| Idée | Source | Décision |
|---|---|---|
| Panneau latéral « Ajouter un widget » avec recherche, catégories et aperçu | Kibana, Grafana | **Oui (phase 2)** |
| Catalogue de widgets prêts à l'emploi et paramétrables (au lieu d'un éditeur libre) | Metabase, Odoo | **Oui (phase 2)** |
| Bibliothèque de widgets enregistrés et réutilisables | Kibana, Grafana | **Oui (phase 3)**, par valeur d'abord |
| Période, filtres et rafraîchissement globaux | Tous | **Oui (phase 2)** |
| Menu du panneau : modifier, dupliquer, agrandir, voir les données | Kibana | **Oui (phase 3)** |
| Glisser un widget depuis le panneau vers la grille | Grafana | Oui : GridStack le permet (`GridStack.setupDragIn`) |
| « Épingler au dashboard » depuis les écrans existants (rapports, stat produit) | Power BI, Odoo | **Oui (phase 4)** |
| Clic sur un graphique = filtre global | Kibana | Phase 4 |
| Seuils de couleur sur les KPI (ex. stock sous le minimum) | Grafana | Phase 3 |
| Cartes texte / titres de section | Metabase, Grafana | Phase 3 |
| Envoi par e-mail, rapports planifiés | Kibana, Metabase | Hors périmètre (le module `batch` pourrait l'accueillir) |
| Éditeur de requêtes libre (Lens, SQL) | Kibana, Metabase | **Non** : trop complexe pour la cible, et risqué pour la sécurité |

### 4.6 Panneau « Ajouter un widget » proposé

```
┌─ Ajouter un widget ──────────────────────── ✕ ┐
│ 🔍 Rechercher un widget…                      │
│ [Tous] [Ventes] [Caisse] [Stock] [Achats]     │
│ [Mes widgets]                                 │
├───────────────────────────────────────────────┤
│ ┌─────────┐ Chiffre d'affaires                │
│ │  ▁▃▅▇   │ Ventes · KPI, courbe, barres      │
│ └─────────┘ CA TTC sur la période             │
│                              [+ Ajouter]      │
│ ┌─────────┐ Top produits vendus   ● déjà ajouté│
│ │ ▇▅▃▂    │ Ventes · barres, tableau          │
│ └─────────┘ Paramètres : nombre, tri          │
│                              [+ Ajouter]      │
│ ┌─────────┐ Produits en rupture               │
│ │  ⚠ 12   │ Stock · KPI, tableau              │
│ └─────────┘                    [+ Ajouter]    │
│ ┌─────────┐ Marge par rayon            🔒      │
│ │  ◔      │ Non inclus dans votre licence     │
│ └─────────┘                                   │
└───────────────────────────────────────────────┘
```

- Onglet **Catalogue** : les widgets livrés avec l'application, groupés par catégorie. Chaque
  entrée montre une vignette, les visualisations possibles et les paramètres.
- Onglet **Mes widgets** : les widgets que l'utilisateur a configurés et enregistrés (phase 3).
- **Ajouter** place le widget à la première place libre et ouvre ses réglages s'il a des paramètres
  obligatoires. On peut aussi le glisser directement sur la grille.
- Un widget refusé par les droits de l'utilisateur est masqué. Un widget refusé par la licence est
  affiché grisé avec un cadenas, pour signaler qu'il existe.
- Un badge « déjà ajouté » évite les doublons involontaires, sans les interdire.

### 4.7 Conséquences sur le modèle

- **Catalogue (définitions)** : l'affichage de chaque widget vit dans le code front. Son droit
  d'accès et sa source de données vivent côté backend (§ 5), pour que les droits ne dépendent pas
  que de ce que le front veut bien masquer.
- **Bibliothèque (widgets enregistrés)** : une nouvelle table `dashboard_widget` (id, owner, scope,
  widget_key, viz, title, params jsonb). Un item du layout porte soit une copie
  (`widgetKey` + `params`), soit une référence (`libraryWidgetId`).
- **Historique des versions** façon Grafana : facultatif. Une table `dashboard_layout_version`
  suffirait si le besoin apparaît.

## 5. Autorisation des widgets par rôle

**Règle** : un utilisateur ne peut ni ajouter, ni voir les données d'un widget auquel son rôle n'a
pas droit. Un caissier ne peut pas s'ajouter « Marge brute » ou « P&L analytique ». Le masquer dans
le catalogue ne suffit pas : il faut aussi que l'API refuse de lui envoyer les données.

### 5.1 Ce que l'application sait déjà faire

- **Droits par rôle sur les éléments de navigation** : `nav_item` et `nav_item_role` (`can_display`,
  `can_access`, `can_create`, `can_edit`, `can_delete`, `can_export`, `can_execute`). L'écran
  d'administration des menus sait déjà attribuer des éléments à un rôle
  (`NavItemServiceImpl.assignItemsToRole`).
- **Côté front**, `AbilityService.can('display', code)` répond à partir de l'arbre de navigation
  reçu à la connexion. Tout code absent de l'arbre est refusé.
- **Côté backend**, `DomainUserDetailsService` ajoute aux autorités Spring les codes des
  `nav_item` de type `ACTION` pour lesquels le rôle a `can_execute`. On peut donc écrire
  `@PreAuthorize("hasAuthority('ventes.en-cours.delete')")`.

### 5.2 Faille constatée : les rapports ne sont pas protégés côté API

Sur les 21 contrôleurs de `web/rest/report/`, seuls `SalesForecastResource` et
`SalesSummaryReportResource` portent un `@PreAuthorize`. Les annotations de
`TopProductsReportResource` sont commentées. `DashboardResource`, `CaissierDashboardResource` et
`ResponsableCommandeDashboardResource` n'en ont aucune.

Conséquence : **tout utilisateur connecté, caissier compris, peut appeler directement
`/api/pnl-analytique`, `/api/marges-profitability` ou `/api/tiers-payant/creances`.** Les menus
cachent les écrans, mais ne protègent pas les données. Le dashboard personnalisable ne doit pas
reproduire ce défaut. Corriger les rapports eux-mêmes est un chantier à part, à planifier.

### 5.3 Modèle proposé : un widget = un `nav_item` de type `WIDGET`

- Nouvelle valeur `WIDGET` dans `NavTargetType`.
- Chaque widget du catalogue est un `nav_item` de code `widget.<clé>` (par exemple
  `widget.ca-periode`), rangé sous un groupe `dashboard-perso.widgets` puis par catégorie (Ventes,
  Caisse, Stock, Achats, Finances, Clients).
- **Droit** : `can_display` sur ce `nav_item` = le rôle peut ajouter le widget et voir ses données.
- **Administration sans nouvel écran** : l'administrateur gère les widgets d'un rôle depuis l'écran
  d'attribution des menus existant.
- **Attribution initiale** : la migration donne à chaque widget les mêmes droits que le rapport
  dont il provient. Elle copie les lignes `nav_item_role` du `nav_item` source (par exemple
  `rapport-finance.pnl-analytique`), et l'administrateur ajuste ensuite. On évite ainsi une double
  saisie, tout en gardant la possibilité d'accorder un widget sans accorder le rapport complet.
- **Licence** : un widget peut en plus exiger une fonctionnalité de licence (`@RequiresFeature`).
  Les droits du rôle et la licence se cumulent.

### 5.4 Contrôle à trois niveaux

| Niveau | Contrôle | Comportement |
|---|---|---|
| Catalogue (front) | `AbilityService.can('display', 'widget.x')` | Widget interdit par le rôle : **absent** de la liste. Widget non inclus dans la licence : grisé avec un cadenas |
| Affichage d'un dashboard (front) | Même contrôle, widget par widget | Un dashboard partagé ou attribué au rôle peut contenir un widget interdit à l'utilisateur. La tuile affiche « Widget non autorisé », sans aucune donnée. Le reste du dashboard fonctionne |
| Données (backend) | `@PreAuthorize("hasAuthority('widget.x')")` sur l'endpoint du widget | Refus 403 même si l'appel ne passe pas par le front |
| Enregistrement (backend) | À la sauvegarde d'un layout, chaque `widgetKey` est vérifié pour son auteur | Impossible d'enregistrer, même à la main, un widget auquel on n'a pas droit |

**Réalisé en phase 0, avec deux ajustements par rapport à ce qui précède :**

- Les niveaux 3 et 4 **lisent `nav_item_role` à chaque requête** (`WidgetAuthorizationService`)
  au lieu de s'appuyer sur les autorités du jeton JWT. Le jeton ne porte que le **premier** rôle
  de l'utilisateur, et un droit retiré doit s'appliquer tout de suite. `DomainUserDetailsService`
  n'est donc pas modifié.
- Un refus renvoie une `GenericError` (HTTP 400, `errorKey` `widgetNonAutorise` ou
  `widgetNonSouscrit`), car `ExceptionTranslator` transforme toute exception inattendue en 500, y
  compris une `AccessDeniedException` levée par un `@PreAuthorize`.
  > Correction : on avait cru qu'un 403 déconnectait l'utilisateur. C'est faux, seul un 401 le
  > fait (`auth-expired.interceptor.ts`). Le 403 commun est prévu par
  > [PLAN-SECURISATION-ENDPOINTS.md](PLAN-SECURISATION-ENDPOINTS.md).

### 5.5 Deux périmètres de données : « moi » et « officine »

Certains indicateurs existent en deux versions : « Mes encaissements » pour le caissier, « Encaissements
de l'officine » pour le pharmacien. On en fait **deux widgets distincts**, avec deux droits
distincts, plutôt qu'un paramètre « périmètre ». Un paramètre pourrait être modifié par
l'utilisateur ; un widget séparé non. Les widgets « moi » filtrent côté serveur sur l'utilisateur
connecté, jamais sur un identifiant envoyé par le front.

## 6. Catalogue de widgets

### 6.1 Ce que contiennent aujourd'hui les tableaux de bord et les rapports

**Les quatre tableaux de bord existants :**

| Tableau de bord | Rôle par défaut | Contenu |
|---|---|---|
| `home-base` (accueil pharmacien) | ADMIN, PHARMACIEN | Alertes (péremptions, ruptures, à commander, ajustements, modifications de prix) ; 9 KPI (CA net, marge brute, marge sur 12 mois, ventes comptant, tiers-payant, dépôt, créances TP, achats, stock valorisé, différés) ; modes de règlement ; ventes par tiers-payant ; achats par fournisseur ; top produits en valeur et en quantité ; Pareto 20/80 |
| `caissier-dashboard` | CAISSIER | État de la caisse (fond, espèces encaissées, espèces théoriques) ; encaissements de ma session ; différés à relancer ; livraisons du jour ; mes dernières transactions |
| `commande-home` | RESPONSABLE_COMMANDE | Alertes stock, commandes en cours, péremptions, rotation, suggestions, analyse ABC, performance fournisseurs (`/api/responsable-commande/dashboard/*`) |
| Onglet `ventes.kpi` | ADMIN, PHARMACIEN (refusé au caissier) | Indicateurs de ventes |

**Le menu Rapports & Statistiques** (codes `nav_item`) :

| Famille | Rapports |
|---|---|
| Ventes (`rapport-ventes.*`) | Dashboard CA, synthèse des ventes, tableaux comparatifs, top produits, rentabilité, analyse du panier, prévisions, saisonnalité, remises, performance vendeurs, génériques & substitution, rétention clients |
| Stock (`rapport-stock.*`) | Alertes, valorisation, rotation, ABC, ABC Pareto, récap vendus/invendus, démarque & ajustements |
| Finances (`rapport-finance.*`) | P&L analytique, rentabilité, BFR & liquidité, créances TP, situation et vieillissement des créances, taux de recouvrement, concentration des payeurs, avoirs TP, différés clients |
| Partenaires (`rapport-partners.*`) | Segmentation clients, performance fournisseurs |
| Comptabilité et déclaration | Balance caisse, récapitulatif de caisse, tableau pharmacien, rapport TVA, retraitement du CA (hors dashboard : documents réglementaires, pas des indicateurs à suivre) |

Presque tous ces rapports ont déjà un endpoint qui renvoie un résumé (`/summary`, `/kpi`, `/count`,
`/top`). **La plupart des widgets ne demandent donc que la couche de sécurité et d'adaptation
(§ 7), pas de nouvelles requêtes SQL.**

### 6.2 Widgets tirés de l'existant

Légende des rôles : **A** admin, **P** pharmacien, **C** caissier, **V** vendeur, **RC** responsable
commande. Il s'agit des droits proposés par défaut ; l'administrateur les ajuste.

**Ventes**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| CA net de la période (+ évolution) | KPI, courbe | `/api/dashboard/ca`, `/api/dashboard-ca/summary` | A, P |
| CA par période | Courbe, barres | `/api/dashboard/ca-by-periode`, `/api/dashboard-ca/evolution` | A, P |
| CA par type de vente (comptant, TP, dépôt) | Camembert, barres, KPI | `/api/dashboard/ca-by-type-vente` | A, P |
| Modes de règlement | Camembert, tableau | `/api/dashboard/ca-by-mode-paiment` | A, P |
| Top produits (valeur / quantité) | Barres, tableau | `/api/top-products/by-revenue`, `/by-quantity` | A, P, RC |
| Panier moyen et son évolution | KPI, courbe | `/api/dashboard-ca/basket-evolution` | A, P |
| Ventes par famille de produits | Barres, camembert | `/api/dashboard-ca/product-families` | A, P |
| Performance vendeurs | Barres, tableau | `/api/dashboard-ca/by-staff` | A, P |
| Remises accordées | KPI, tableau | `/api/dashboard-ca/remises-analysis` | A, P |
| Génériques & substitution | KPI, camembert | `/api/dashboard-ca/generics-substitution` | A, P |
| Comparatif N / N-1 | Barres, tableau | `/api/comparative-reports/monthly`, `/yearly` | A, P |
| Saisonnalité et prévisions | Courbe | `/api/sales-forecast/seasonality`, `/summary` | A |
| Associations de produits (panier) | Tableau | `/api/market-basket/summary` | A, P |
| Mes ventes du jour *(moi)* | KPI, tableau | `/api/caissier/dashboard/ventes-recentes` | C, V |

**Caisse**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| État de ma caisse *(moi)* : fond, espèces, théorique | KPI | `/api/caissier/dashboard/caisse-status` | C |
| Encaissements de ma session *(moi)* | KPI, camembert | `/api/caissier/dashboard/session-encaissements` | C |
| Synthèse des caisses *(officine)* | Tableau, KPI | `/api/cash-register/summary` | A, P |
| Mouvements de caisse | Tableau | `/api/cash-register/movements` | A, P |

**Stock**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| Compteurs d'alertes (péremptions, ruptures, à commander, ajustements, prix) | Pastilles KPI | `/api/dashboard/alert-counts` | A, P, RC |
| Alertes de stock | Tableau, KPI | `/api/stock/alerts`, `/count` | A, P, RC |
| Stock valorisé | KPI | `/api/stock/valuation/summary` | A, P, RC |
| Rotation du stock et produits lents | Tableau, barres | `/api/stock/rotation/slow`, `/count` | A, P, RC |
| Analyse ABC / Pareto 20/80 | Barres, tableau | `/api/abc-pareto/summary`, `/top` | A, P, RC |
| Péremptions à venir | Tableau, KPI | `/api/responsable-commande/dashboard/peremptions` | A, P, RC |
| Démarque et ajustements | KPI, camembert | `/api/demarque-report/kpi`, `/by-motif` | A, P, RC |

**Achats**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| Achats de la période | KPI | `/api/dashboard/ca-achats` | A, P, RC |
| Commandes en cours | Tableau, KPI | `/api/responsable-commande/dashboard/commandes-en-cours` | A, P, RC |
| Suggestions de commande | Tableau | `/api/responsable-commande/dashboard/suggestions` | A, P, RC |
| Livraisons du jour | Tableau | `/api/caissier/dashboard/livraisons-du-jour` | A, P, C, RC |
| Performance fournisseurs (score, délai, conformité) | Tableau, barres | `/api/supplier-performance/top`, `/summary` | A, P, RC |

**Finances et tiers-payant**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| Marge brute de la période | KPI | données de `home-base` / `/api/marges-profitability/summary` | A, P |
| Produits à faible marge | Tableau | `/api/marges-profitability/faible-marge` | A, P |
| P&L par segment ou famille | Barres, tableau | `/api/pnl-analytique/segment`, `/famille` | A |
| BFR et liquidité | KPI, courbe | `/api/cash-flow-bfr/snapshot`, `/evolution` | A |
| Créances TP (encours, > 90 j) | KPI, tableau | `/api/tiers-payant/creances/summary` | A, P |
| Vieillissement des créances / DSO par organisme | Barres, tableau | `/api/vieillissement-creances/global`, `/dso-organisme` | A, P |
| Concentration des payeurs | Camembert | `/api/concentration-payers/summary` | A |
| Différés clients (encours) *(officine)* | KPI | données de `home-base` | A, P |
| Différés à relancer | Tableau | `/api/caissier/dashboard/differes-relance` | A, P, C |

**Clients**

| Widget | Visualisations | Source existante | Rôles |
|---|---|---|---|
| Rétention clients | KPI, courbe | `/api/client-retention/kpi` | A, P |
| Segmentation (champions, à risque) | Camembert, tableau | `/api/customers/segmentation/*` | A, P |

**Sans données (tous les rôles)**

| Widget | Rôle |
|---|---|
| Raccourcis (liens vers des écrans) | Chaque lien n'est affiché que si l'utilisateur a accès à l'écran visé |
| Note / texte libre, titre de section | Structurer le dashboard |

Cela fait **une cinquantaine de widgets** réalisables sans nouvelle requête SQL.

### 6.3 Widgets à valeur ajoutée pour la gestion d'officine

Ces idées viennent des pratiques courantes des logiciels de gestion d'officine (LGO) et des outils
d'aide au pilotage. Elles ne proviennent pas d'une comparaison vérifiée produit par produit. Aucune
n'a de source toute prête dans Pharma-Smart ; chacune demande du développement backend.

**Pilotage quotidien**

| Widget | Intérêt | Rôles | Effort |
|---|---|---|---|
| **À faire aujourd'hui** : une liste d'actions (livraisons à réceptionner, différés échus, factures TP à éditer, lots périmés à sortir, commandes en retard), chaque ligne menant à l'écran concerné | L'écran d'accueil devient une liste de travail, pas seulement des chiffres | Tous, chacun ne voit que ses tâches | Moyen : agrège des requêtes existantes |
| **CA du jour vs objectif** (jauge) avec un rythme horaire | Savoir en milieu de journée si l'objectif est tenable | A, P | Faible, mais demande un paramètre « objectif » (journalier ou mensuel) |
| **CA du jour vs même jour N-1 / semaine précédente** | Plus parlant qu'un CA brut | A, P | Faible |
| **Fréquentation par heure et jour** (carte de chaleur) | Adapter les effectifs au comptoir et les horaires | A, P | Faible : une agrégation sur la date de vente |

**Stock et achats**

| Widget | Intérêt | Rôles | Effort |
|---|---|---|---|
| **Stock dormant valorisé** (aucune vente depuis N jours) | Argent immobilisé, à retourner ou à brader | A, P, RC | Faible (dérivé de la rotation) |
| **Couverture de stock en jours** par produit ou famille | Anticiper les ruptures mieux qu'un seuil fixe | A, P, RC | Moyen |
| **Péremptions valorisées à 3 / 6 mois** | Chiffrer la perte à venir pour agir (retour fournisseur, mise en avant) | A, P, RC | Faible |
| **Manquants clients** : produits demandés mais non servis ou vendus en stock forcé | Mesurer le service rendu et les ventes perdues | A, P, RC | Moyen : s'appuie sur les ventes forcées et les avoirs |
| **Avoirs clients à servir** (produits dus) | Ne pas oublier un patient à qui l'on doit un produit | A, P, C | Faible : les avoirs existent |
| **Retards fournisseurs** : commandes au-delà du délai habituel | Relancer au bon moment | A, P, RC | Faible (délai moyen déjà calculé) |
| **Écarts d'inventaire du dernier inventaire** | Suivre la démarque inconnue | A, P, RC | Faible |

**Tiers-payant et finances**

| Widget | Intérêt | Rôles | Effort |
|---|---|---|---|
| **Ventes TP pas encore facturées** (montant, ancienneté) | C'est de la trésorerie qui dort avant même d'être réclamée | A, P | Faible |
| **Rejets et régularisations TP** | Traiter vite ce qui ne sera pas payé | A, P | À étudier selon les données de facturation disponibles |
| **Marge perdue en remises** | Relier les remises à la marge, pas au CA | A, P | Faible |

**Contrôle interne**

| Widget | Intérêt | Rôles | Effort |
|---|---|---|---|
| **Écarts de caisse par caissier** (sur 30 jours) | Repérer une dérive, sans oublier l'écart normal de l'arrondi à 5 F | A, P | Faible |
| **Espèces en caisse au-dessus d'un seuil** | Déclencher une remise en coffre | A, P, C (sa caisse) | Faible, demande un seuil paramétrable |
| **Annulations, modifications de prix et ventes forcées par utilisateur** | Détecter des pratiques anormales | A | Faible : les journaux existent |

**Clients**

| Widget | Intérêt | Rôles | Effort |
|---|---|---|---|
| **Nouveaux clients / clients perdus** du mois | Suivre la patientèle | A, P | Faible (segmentation existante) |
| **Clients à risque** à recontacter | Fidélisation | A, P | Faible |

### 6.4 Priorités proposées

1. **Lot 1 (phase 2)** : reprendre les widgets des quatre tableaux de bord existants. On obtient la
   parité et un dashboard par défaut pour chaque rôle.
2. **Lot 2 (phase 3)** : le reste de l'existant (§ 6.2), puis « À faire aujourd'hui », « CA vs N-1 »,
   « Stock dormant », « Péremptions valorisées », « Avoirs à servir », « Ventes TP non facturées ».
   Forte valeur, peu d'effort.
3. **Lot 3 (phase 4)** : objectifs de CA, fréquentation horaire, couverture de stock, manquants,
   contrôle interne.

## 7. Architecture proposée

**Au cœur : un catalogue de widgets, déclaré des deux côtés.**

Côté backend, chaque widget est un composant Spring :

```java
public interface WidgetDataProvider {
    String key();                         // "ca-periode" → nav_item "widget.ca-periode"
    Optional<Feature> feature();          // contrôle de licence
    WidgetData load(WidgetContext ctx, Map<String, Object> params); // délègue aux services existants
}
```

Un seul contrôleur, `DashboardWidgetResource` (`GET /api/dashboard-widgets/{key}`), vérifie
l'autorité `widget.<key>` et la licence, puis appelle le fournisseur. Les widgets n'appellent
**jamais directement** les endpoints de rapport, qui ne sont pas protégés (§ 5.2).

Côté front, chaque widget déclare son affichage :

```ts
interface WidgetDefinition {
  key: string;                     // 'ca-periode', 'top-produits', 'alertes-stock'…
  label: string;
  category: string;                // Ventes, Caisse, Stock, Achats, Finances, Clients
  visualizations: VizType[];       // KPI | LINE | BAR | PIE | TABLE | LIST
  params?: ParamDef[];             // ex. limite = 10, type de vente
  scope: 'MOI' | 'OFFICINE';
}
// Droit : AbilityService.can('display', `widget.${key}`)
```

- **Données** : un format commun `WidgetData` (`kpi` / `series` / `table` / `list`) quel que soit le
  graphique, renvoyé par `/api/dashboard-widgets/{key}`.
- **Affichage** : Angular génère les tuiles à partir d'un signal `items`, via `@for` et
  `NgComponentOutlet`, puis `grid.makeWidget()`. Plus de `innerHTML`, ce qui règle aussi la faille
  XSS. Autre option : le wrapper Angular fourni par gridstack 13 (à vérifier). Les graphiques
  passent par le wrapper maison `app-chart`, plus par `Chart.register` dans chaque widget.
- **Contexte global** : un `DashboardContextStore` à base de signaux (période, filtres,
  rafraîchissement) que chaque widget écoute.
- **Stockage** : `layoutConfig` passe à un format versionné,
  `{ version: 2, context, items: [{ id, x, y, w, h, widgetKey, viz, title, params }] }`. On ajoute
  la valeur `CUSTOM` à `DashboardComponentKey` via une migration Flyway.
- **Emplacement** : `features/dashboard/` (`data-access`, `feature`, `ui`, `widgets`), à la manière
  de `features/sales`. `entities/dashboard` sera supprimé à la fin.

## 8. Plan de réalisation

### Phase 0 — Corrections backend et socle des droits (≈ 2 j) — ✅ réalisée le 2026-09-28

Migration `V2.1.5__dashboard_widgets_socle.sql`, package `service/dashboard/widget/`,
`DashboardWidgetResource`, tests `DashboardWidgetDroitsIntegrationTest` (15) et
`DashboardWidgetServiceTest` (5). Contenu prévu :

- Points 8 à 11 : contrôle du propriétaire (layouts système réservés aux administrateurs), `clone`
  et `update` qui conservent `componentKey`, `@Secured(ADMIN)` sur les actions par rôle,
  `GenericError`.
- Migration `CUSTOM` ; `findAllForCurrentUser` exclut les layouts système.
- `NavTargetType.WIDGET`, conteneur `dashboard-perso.widgets` et ses six catégories, droits lus en
  base par `WidgetAuthorizationService` (voir § 5.4).
- `DashboardWidgetResource` et l'interface `WidgetDataProvider` (sans widget réel encore).
- Vérification des widgets à l'enregistrement d'un layout.
- Tests d'intégration : un caissier reçoit 403 sur un widget pharmacien, et ne peut pas enregistrer
  un layout qui le contient.

### Phase 1 — Socle front (≈ 3 j) — ✅ réalisée le 2026-09-28

Module `features/dashboard/` (store, façade, grille, tuile, panneau d'ajout, éditeur, vue
d'accueil) ; `entities/dashboard` supprimé. Migration `V2.1.6` : entrée de menu
« Mon tableau de bord » sous « Rapports & Statistiques », accordée aux rôles qui voient déjà ce
groupe ; widgets de mise en page `note` et `titre-section`, autorisés à tous les rôles pour que
l'éditeur serve avant la phase 2. Grille fixée à 12 colonnes : le passage à une colonne sur petit
écran ferait enregistrer une disposition écrasée, il attend une gestion propre. Contenu prévu :

- `features/dashboard` : store (layout en cours, items, mode édition, modifications non
  enregistrées).
- Grille pilotée par Angular : ajout, suppression et redimensionnement synchronisés avec le store.
- Sauvegarde qui enregistre réellement chaque widget, « Enregistrer » ou « Enregistrer sous »,
  « Définir comme accueil ».
- Correction du chargement : on crée la grille une fois le layout reçu.
- Ajouter l'entrée de menu (`nav_item`) et remplacer le TODO de `home.component.html` pour afficher
  le layout `CUSTOM`.

### Phase 2 — Catalogue et vraies données, lot 1 (≈ 5 j) — 🟡 lot 1a réalisé le 2026-09-28

**Fait (lot 1a)** : 21 widgets de données branchés sur les services des tableaux de bord
existants (`service/dashboard/widget/provider/`), migration `V2.1.7` (widgets et droits par rôle
du § 6.2) ; widget de données générique (indicateur avec évolution, courbe, barres, anneau,
camembert, tableau ; chargement, erreur, vide) ; période et rafraîchissement communs, enregistrés
avec le dashboard ; choix de la visualisation par tuile ; modèles « Pilotage officine »,
« Ma caisse », « Stock et achats » (front, filtrés par les droits). L'évolution du CA passe par
`DashboardCAService` plutôt que `/dashboard/ca-by-periode`, dont la double jointure pourrait
compter certains montants plusieurs fois (non vérifié).

**Fait (lot 1b)** : 9 widgets issus de l'accueil pharmacien (migration `V2.1.8`, nouvelle
configuration `FinancesWidgets`) — marge sur 12 mois, stock valorisé, créances tiers payant par
ancienneté et par organisme, différés clients, Pareto 20/80, ventes par tiers payant, achats par
fournisseur, qualité fournisseurs. Seul le taux de recouvrement de la facturation n'est pas repris.
La devise affichée vient de `APP_DEVISE`.

Glisser un widget du panneau vers la grille : écarté, le panneau latéral recouvre la grille.
Filtre magasin : écarté, les services de statistiques ne le prennent pas en paramètre.

Contenu prévu :

- Contexte global : sélecteur de période, filtre magasin, rafraîchissement automatique.
- Lot 1 (§ 6.4) : les widgets des quatre tableaux de bord existants, chacun avec son
  `WidgetDataProvider`, son `nav_item` et ses droits initiaux (migration Flyway).
- Visualisations génériques KPI, courbe, barres, camembert, tableau, liste, avec états chargement /
  vide / erreur / « non autorisé ».
- Panneau latéral « Ajouter un widget » (§ 4.6) : recherche, catégories, vignettes, badge « déjà
  ajouté », filtré selon les droits, cadenas pour la licence, glisser-déposer vers la grille.
- Un dashboard par défaut par rôle, construit avec ces widgets.

### Phase 3 — Édition complète, bibliothèque, lot 2 (≈ 7 j) — 🟡 en grande partie réalisée le 2026-09-28

**Fait** : fenêtre de réglages par tuile (titre, visualisation, période propre, nombre de lignes,
classement, seuil d'alerte d'un indicateur, signalé par icône et libellé) ; menu de tuile
(dupliquer, agrandir, voir les données, retirer) ; bouton « Agrandir » en lecture ; attribution
d'un dashboard comme accueil d'un rôle, réservée à l'administrateur ; actions d'enregistrement
regroupées dans un bouton à menu ; lot 2 partiel (migration `V2.1.9`) : « À faire aujourd'hui »
(pense-bête mobile), CA comparé à l'an dernier, péremptions valorisées, stock dormant.

**Reste** : bibliothèque « Mes widgets » (table `dashboard_widget`) ; cartes texte déjà couvertes
par Note et Titre ; lot 2 restant : avoirs à servir (attendre la fin du chantier avoirs en cours),
ventes tiers payant non facturées (requête à écrire). « Dupliquer un dashboard » est couvert par
« Enregistrer sous ».

Contenu prévu :

- Lot 2 (§ 6.4) : le reste des widgets de l'existant et les six premiers widgets à valeur ajoutée.
- Fenêtre de réglages par widget : titre, visualisation, paramètres, période propre.
- Menu du panneau : modifier, dupliquer, agrandir, « Voir les données », supprimer.
- Bibliothèque « Mes widgets » : table `dashboard_widget` et onglet dans le panneau d'ajout.
- Seuils de couleur sur les KPI, cartes texte et titres de section.
- Dupliquer un dashboard, partage (portée), défaut par rôle pour les administrateurs.
- Modèles prêts à l'emploi (Pharmacien, Caissier, Stock) livrés en migration, qui pourraient à
  terme remplacer `home-base`.

### Phase 4 — Optionnel

- « Épingler au dashboard » depuis les écrans existants (rapports, statistiques produit).
- Clic sur un graphique pour filtrer le dashboard, lien vers l'écran détaillé.
- Plein écran / mode affichage mural.
- Export PNG ou PDF, import/export du JSON d'un dashboard.
- Lot 3 (§ 6.4) : objectifs de CA, fréquentation horaire, couverture de stock, manquants, contrôle
  interne.

### Chantier séparé — Sécuriser les endpoints de rapport

> Élargi à toute l'API dans un plan dédié : [PLAN-SECURISATION-ENDPOINTS.md](PLAN-SECURISATION-ENDPOINTS.md).

Ajouter les contrôles manquants sur `web/rest/report/*` et sur les trois contrôleurs de tableau
de bord (§ 5.2). Indépendant du dashboard personnalisable, mais la faille existe dès aujourd'hui.

**Constat du 2026-09-28 (base de dev)** — droits `can_access` sur les sections `rapport-*` :

| Rôle | Sections accessibles |
|---|---|
| ADMIN | les 36 |
| PHARMACIEN | 22 — **pas** `rapport-ventes.dashboard-ca`, `.top-products`, `.comparative`, `.market-basket`, `.sales-forecast`, `.sales-summary`, `.profitability`, ni `rapport-stock.stock-valuation`, `.abc-pareto`, `.stock-alerts`, `.stock-rotation`, `.recap-produit-vendu`, ni `rapport-partners.*` |
| RESPONSABLE_COMMANDE | 8 — stock (ABC Pareto, récap, alertes, rotation) et top produits |
| CAISSIER, VENDEUR | aucune |

**Piège** : l'accueil pharmacien (`home-base`) appelle `/api/stock/valuation/summary`,
`/api/supplier-performance/summary` et `/api/supplier-performance/achats`, dont le pharmacien n'a
pas la section de rapport. Exiger le droit de la section casserait son accueil.

**Corrigé** : c'était un oubli d'attribution. La migration `V2.1.10` accorde au pharmacien
`rapport-stock.stock-valuation` et `rapport-partners.supplier-performance` (afficher, ouvrir,
exporter). L'option 2 ci-dessous devient la voie naturelle.

**Contraintes techniques** :
- les autorités du jeton ne portent que les codes `ACTION` : un `@PreAuthorize("hasAuthority('rapport-…')")`
  ne verrait jamais un code `SECTION` ;
- une `AccessDeniedException` sort en 500 (un 403, lui, ne déconnecte pas : seul le 401 le fait)
  par `ExceptionTranslator`.

**Options** :
1. **Contrôle en base par section**, sur le modèle de `WidgetAuthorizationService` : une annotation
   `@RequiresNavAccess({"rapport-stock.stock-valuation", …})` et un aspect qui lit `nav_item_role` ;
   refus en `GenericError`. Un endpoint partagé liste plusieurs codes (ex. valorisation : la section
   de rapport **ou** l'accès à l'accueil pharmacien).
2. **Même contrôle, en accordant d'abord au pharmacien** les sections qu'utilise son accueil (une
   migration), pour garder un code par endpoint.
3. **Garde minimale par rôle** : exclure CAISSIER et VENDEUR de `web/rest/report/*`, sans finesse
   par section. Rapide, ferme la faille la plus grave (un caissier qui lit la marge), ne règle pas
   le reste.

Vérifié : ni `pharma-mobile-report` ni `sales-android` n'appellent ces endpoints. Reste à
recenser les écrans web qui les partagent (accueil, rapports, exports PDF/Excel) pour l'option 1.

### Tests

Tests unitaires Jest du store et du catalogue, un parcours e2e (créer → ajouter 2 widgets →
sauvegarder → recharger → définir comme accueil), et un parcours e2e caissier (le catalogue ne
propose pas les widgets pharmacien ; un dashboard partagé qui en contient les affiche « non
autorisé »).

## 9. Décisions à prendre avant la phase 1

1. **Garder `home-base`, `caissier-dashboard`, etc. à côté du dashboard personnalisable, ou les
   convertir à terme en modèles ?** La conversion donne un seul moteur, mais il faut d'abord
   atteindre la parité en phase 3.
2. **Qui peut créer un dashboard et le partager ?** Tout utilisateur connecté, ou un privilège
   dédié ?
3. **Droits initiaux des widgets** : les recopier depuis le rapport source (proposé, § 5.3), ou
   partir de zéro et tout attribuer à la main ?
4. **Répartition des rôles par défaut** des tableaux du § 6.2 : à valider par le métier, en
   particulier ce que voient le caissier et le vendeur.
