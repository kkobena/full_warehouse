# Analyse du module `entities/reports` — Pharma-Smart

**Date** : 2026-05-21  
**Périmètre** : `src/main/webapp/app/entities/reports`

---

## 1. Structure actuelle

Le module est organisé en **4 piliers métier** avec 18 composants spécialisés :

```
reports/
├── sales/          → 7 rapports (Dashboard CA, Synthèse, Top Produits,
│                                  Rentabilité, Comparatif, Prévisions, Panier)
├── stock/          → 5 rapports (Alertes, Valorisation, Recap Vendus/Invendus,
│                                  Rotation, ABC Pareto)
├── partners/       → 2 rapports (Segmentation Clients RFM, Performance Fournisseurs)
└── finance/        → 4 rapports (P&L Analytique, Vieillissement Créances,
                                   Concentration Payeurs, Cash Flow BFR)
```

---

## 2. Redondances identifiées

### 2.1 Métriques CA / Marge — affichées dans 6 composants différents

| Métrique | Dashboard CA | Sales Summary | Top Products | Comparative | PnL | Forecast |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| CA Total | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Taux de marge | ✓ | — | — | — | ✓ | — |
| Panier moyen | ✓ | ✓ | — | — | — | — |
| CA N vs N-1 | ✓ | — | — | ✓ | ✓ | — |

**Problème** : chaque composant recalcule ou re-fetche ces chiffres indépendamment. Un seul
`KPIDashboardService` suffisait à partager la synthèse.

### 2.2 Quatre composants conteneurs quasiment identiques

`SalesReportsComponent`, `StockReportsComponent`, `PartnersReportsComponent`,
`FinanceReportsComponent` suivent le même patron : navigation par onglets + vérification
de permissions AbilityService. Un composant générique `ReportTabContainerComponent` éliminerait
ce code dupliqué.

### 2.3 Filtres de période dupliqués

Chaque composant réimplémente son propre sélecteur `startDate`/`endDate`. Un composant partagé
`DateRangeFilterComponent` avec les presets standard (Aujourd'hui, 7j, 30j, Mois, Année,
Personnalisée) serait réutilisable.

### 2.4 Pattern exports PDF/Excel/CSV — 12 composants

Même logique Tauri vs Web répétée partout. Un service `ReportExportService(url, params, label)`
centralisé éviterait ~200 lignes par composant.

### 2.5 Construction de charts Chart.js — 8 composants

Cycle destroy → create réimplémenté à la main dans chaque `ngOnInit`/`ngOnDestroy`. Un
`ChartBuilderService` avec des méthodes `createLineChart()`, `createBarChart()`, etc. factoriserait
cela proprement.

---

## 3. Données éparpillées — regroupements recommandés

### 3.1 Double classification ABC (incohérence nommage)

- `ClassePareto` (A+, A, B, C, D) dans `abc-pareto/`
- `CategorieABC` (A, B, C) dans `stock-rotation/`

Ce sont deux systèmes de classification distincts mais le nom prête à confusion. Les deux enums
devraient vivre dans `shared/model/enums/` avec des noms explicites :
`ClasseParetoCA` et `ClasseRotationStock`.

### 3.2 Types de vente — 3 nomenclatures pour la même chose

| Composant | Nomenclature |
|---|---|
| Sales Summary | `ThirdPartySales`, `CashSale`, `VenteDepot` |
| PnL Analytique | `COMPTANT`, `ASSURANCE`, `CARNET` |
| Comparative | `VO`, `VNO`, `Dépôts` |

Un enum unique `SaleType` dans `shared/model/enums/` avec mapping d'affichage est nécessaire.

### 3.3 KPIs de synthèse transverses

Ces indicateurs apparaissent dans plusieurs onglets sans cohérence :
- **CA + Marge** : Sales, Finance (P&L)
- **Panier moyen** : Dashboard, Sales Summary, Customer Segmentation
- **Valeur stock** : Stock Valuation, ABC Pareto, Rotation

**Recommandation** : créer une API `/api/reports/kpi-summary?period=X` qui renvoie un objet
unique consolidé, affiché dans un bandeau commun en haut de chaque section.

### 3.4 Données comparatives N / N-1 chargées en double

Top Products, Comparative Analysis, Supplier Performance et Concentration Payers chargent chacun
les données de l'année courante et de l'année précédente via deux appels séparés.
Une interface `IComparisonPeriod<T> { current: T; previous: T; delta: number; }` et un
`ComparisonLoaderService` éviteraient cela.

---

## 4. Menus / Onglets — ce qui devrait être regroupé

### 4.1 `Profitability Analysis` devrait rejoindre `Finance → P&L`

`ProfitabilityAnalysisComponent` (actuellement sous **Ventes**) affiche marges par produit, taux
de marge min/max, seuils de rentabilité. C'est exactement la dimension « famille/produit » du
`PnlAnalytiqueComponent` (sous **Finance**). Les deux devraient être des onglets d'un même écran
P&L avec deux granularités : globale (P&L Analytique) et par ligne de produit (Profitabilité
détaillée).

### 4.2 `ABC Pareto` et `Stock Rotation` sont redondants à 70 %

Les deux classent les produits en A/B/C selon un critère différent (CA pour Pareto, fréquence de
rotation pour Rotation). Ils pourraient être deux onglets d'un même rapport **Analyse ABC** avec
un toggle « Par chiffre d'affaires » / « Par rotation ».

### 4.3 `Vieillissement Créances` et `Concentration Payeurs` forment un bloc cohérent

Ces deux rapports concernent les **organismes tiers-payants** (mutuelles, CNAM). Ils méritent
une sous-section dédiée dans Finance : **« Tiers-payants »** avec :
- Onglet Vieillissement (aging)
- Onglet Concentration (HHI)
- Onglet Évolution mensuelle (timeline combinée des deux)

---

## 5. Rapports évolutifs / comparatifs manquants — pertinents pour une officine

Ces catégories de rapports sont proposées par les principaux logiciels officinaux (Winpharma,
Lgpi, Pharmagest, Isipharm) et répondent à des besoins réels de gestion.

### 5.1 Rapports réglementaires / dispensation

| Rapport | Contenu | Intérêt |
|---|---|---|
| **Registre de substitution génériques** | Taux de substitution par molécule (DCI), évolution mensuelle vs objectif | Obligation de résultat en France (taux Cnam) |
| **Suivi des médicaments à statut particulier** | Stupéfiants, NMR, médicaments liste I/II : quantités délivrées par mois | Conformité réglementaire |
| **Ordonnances bizone** | Ratio partie obligatoire / partie complémentaire par organisme | Facturation correcte |
| **Rapport rétrocessions** | Médicaments rétrocédés à d'autres structures : volumes, CA | Traçabilité |

### 5.2 Analyse évolutive des ventes

| Rapport | Contenu | Valeur |
|---|---|---|
| **Saisonnalité** | Courbe de ventes superposée sur 3 ans pour les produits saisonniers (grippe, allergie, solaire, moustiques) | Anticipation des commandes |
| **Évolution prix fabricants** | Hausse/baisse des prix d'achat par molécule / fournisseur sur 12 mois | Impact marge |
| **Ventes par collaborateur** | CA, panier moyen, nb transactions par pharmacien / préparateur | Management |
| **Taux de service** | % ordonnances entièrement servies vs partiellement / refusées (rupture) | Qualité de service |
| **Démarque & pertes** | Produits sortis sans vente (casse, péremption, vol) : valeur mensuelle | Gestion des pertes |

### 5.3 Analyse comparative tiers-payants

| Rapport | Contenu | Valeur |
|---|---|---|
| **Retours de lots tiers-payants** | Montants rejetés par organisme, motifs de rejet, taux de rejet | Optimisation facturation |
| **Délai moyen de règlement par mutuelle** | Evolution du DSO par organisme sur 12 mois | Cash flow |
| **Comparaison organismes** | Classement mutuelles / CNAM par volume CA, délai paiement, taux rejet | Négociation |
| **Suivi télétransmission SESAM-Vitale** | Taux de succès, rejets, FSE en attente | Conformité |

### 5.4 Analyse de la clientèle

| Rapport | Contenu | Valeur |
|---|---|---|
| **Rétention & fidélisation** | Taux de retour clients sur 6 mois, cohortes mensuelles | Fidélité |
| **Nouveaux clients vs clients récurrents** | Évolution mensuelle du mix, panier moyen par catégorie | Acquisition |
| **Clients avec carte de fidélité** | CA, fréquence, points accumulés vs convertis | ROI fidélité |
| **Analyse géographique** | Répartition des clients par code postal (si données disponibles) | Zonage marché |

### 5.5 Tableaux de bord de pilotage global (manquant)

Aucun rapport actuel n'offre une **vue synthétique sur une seule page** combinant :

```
┌────────────────────────────────────────────────────┐
│  TABLEAU DE BORD OFFICINE — Mois courant vs M-1   │
├──────────┬──────────┬──────────┬───────────────────┤
│  CA      │  Marge % │  Stock   │  Tiers-Payants    │
│  +3.2%   │  28.5%   │  42 j    │  Encours 15 200 € │
├──────────┴──────────┴──────────┴───────────────────┤
│  Top 5 familles │ Alertes stock │ Prochaines péremp.│
│  ...            │ 3 ruptures    │ 12 produits < 30j │
└──────────────────────────────────────────────────────┘
```

Ce tableau de bord exécutif (1 seule page, chargé en < 2s) est la première chose qu'un
titulaire d'officine regarde chaque matin. Il n'existe pas en tant que tel — le `DashboardCA`
couvre les ventes mais pas le stock ni les tiers-payants.

### 5.6 Analyse des remises & promotions

| Rapport | Contenu | Valeur |
|---|---|---|
| **Impact des remises sur la marge** | Montant total remises accordées, impact marge par période | Pilotage remises |
| **Remises par collaborateur** | Qui accorde le plus de remises, pour quels montants | Management |
| **Remises par type client** | Tiers-payant vs comptant, fidélité | Cohérence politique tarifaire |

---

## 6. Problèmes techniques à corriger

| Problème | Impact | Priorité |
|---|---|---|
| Memory leaks potentiels (`subscribe` sans `takeUntilDestroyed`) | Stabilité | Haute |
| Pas de debouncing sur les filtres (ex : recherche texte) | Performance | Moyenne |
| Aucun cache côté frontend des réponses API | Performances (rechargements inutiles) | Moyenne |
| `RecapProduitVenduComponent` > 500 lignes | Maintenabilité | Moyenne |
| Enums dupliqués entre abc-pareto et stock-rotation | Cohérence données | Faible |

---

## 7. Plan de rationalisation recommandé

### Phase 1 — Regroupements sans casser l'existant
1. Fusionner **ABC Pareto + Stock Rotation** en un seul composant `StockABCComponent` (2 onglets).
2. Déplacer **Profitability Analysis** vers la section Finance, onglet supplémentaire du P&L.
3. Créer sous-section **Tiers-payants** dans Finance (Vieillissement + Concentration + Évolution).

### Phase 2 — Nouveaux rapports prioritaires
1. **Tableau de bord exécutif** (1 page, tous indicateurs) — valeur immédiate pour le titulaire.
2. **Saisonnalité des ventes** sur 3 ans — décision de stock avant saison.
3. **Taux de substitution génériques** — nécessité réglementaire (Cnam).
4. **Ventes par collaborateur** — management équipe.

### Phase 3 — Refactoring technique
1. Composant `DateRangeFilterComponent` partagé.
2. Service `ReportExportService` partagé.
3. `ChartBuilderService` partagé.
4. Enum `SaleType` centralisé dans `shared/model/enums/`.

---

## 8. Récapitulatif des 18 composants existants

| Composant | Pilier | KPIs principaux | Filtres | Exports |
|---|---|---|---|---|
| DashboardCAComponent | Ventes | CA 4 horizons, Marge, Dettes | Période | PDF/Excel/CSV |
| SalesSummaryComponent | Ventes | CA Total, CA Net, Panier moyen | Date, Type vente | — |
| TopProductsComponent | Ventes | CA, Qté, Nb ventes | Mois, Limite | — |
| ProfitabilityAnalysisComponent | Ventes | Taux marge, Marge brute | Famille, Recherche | — |
| ComparativeAnalysisComponent | Ventes | CA N vs N-1 | Période, Année | PDF |
| SalesForecastComponent | Ventes | CA Prévu, Confiance % | Méthode, Horizon | — |
| MarketBasketComponent | Ventes | Support, Confiance, Lift | Date, Seuils | — |
| StockAlertsComponent | Stock | Compteurs RUPTURE/ALERTE/PEREMPTION | Types alerte | PDF |
| StockValuationComponent | Stock | Valeur stock, Qté, Rotation | Famille, Rayon | PDF |
| RecapProduitVenduComponent | Stock | CA, Marge, Stock | 8+ filtres | PDF/Excel/CSV |
| StockRotationComponent | Stock | Taux rotation, Stock immobilisé | Catégorie, ABC | PDF |
| ABCParetoComponent | Stock | CA par classe Pareto | Famille, Classe | PDF |
| CustomerSegmentationComponent | Partners | Total dépensé, Panier moyen, RFM | Classification | PDF |
| SupplierPerformanceComponent | Partners | Perf %, Montant achats, Délais | Filtre perf | PDF |
| PnlAnalytiqueComponent | Finance | CA, Marge %, Nb transactions | Année | — |
| VieillissementCreancesComponent | Finance | Encours par tranche, DSO | Tranches | — |
| ConcentrationPayersComponent | Finance | HHI, Top N, Stress test | Période, Top N | — |
| CashFlowBfrComponent | Finance | CCC, DSO, DPO | — | — |
