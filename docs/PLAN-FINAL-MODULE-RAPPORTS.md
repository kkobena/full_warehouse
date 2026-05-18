# Plan Final — Module Rapports Pharma-Smart

> Document de référence unique. Remplace `ANALYSE-COMPARATIVE-RAPPORTS-OFFICINE.md`,
> `UX-PROPOSITION-MODULE-RAPPORTS.md` et `ANALYSE-RAPPORTS-COMPARATIFS-EVOLUTIFS.md`.

---

## 1. Vision

Le module rapports devient le **cockpit de pilotage** de l'officine : navigation par profil utilisateur, rapports dimensionnés (par famille, par fournisseur, par organisme TP), vues comparatives dans le temps, et actions directes depuis les rapports.

---

## 2. État des lieux — Ne pas redévelopper

### 2.1 Rapports existants dans `entities/reports`

| Rapport | Capacités actuelles | Limite principale |
|---|---|---|
| `dashboard-ca` | J/S/M/A vs période précédente ; top 10 familles snapshot ; graphe journalier | Pas d'évolution dans le temps sur les familles |
| `comparative-analysis` | CA global N vs N-1 (M/T/An) ; par type de vente (VO/VNO/Dépôt) | Global uniquement — pas de vue famille/fournisseur |
| `sales-forecast` | Régression linéaire + MM + saisonnalité ; IC 95% ; 3/6/12 mois | Prévision globale uniquement |
| `top-products` | Top N par CA ou quantité pour un mois | Un seul mois — pas de delta rang ni tendance |
| `sales-summary` | Détail quotidien par type de vente | Pas de comparaison ni trend |
| `profitability-analysis` | Marge brute par famille sur une période | Snapshot — pas d'évolution dans le temps |
| `supplier-performance` | Taux de service, délais fournisseur | Snapshot — pas d'évolution mensuelle |
| `abc-pareto` | Classement ABC par CA et quantité | — |
| `customer-segmentation` | RFM (Récence/Fréquence/Montant) | — |
| `stock-rotation`, `stock-valuation`, `stock-alerts` | Gestion stock | — |
| `market-basket` | Analyse paniers / associations produits | — |
| `recap-produit-vendu` | Récapitulatif produits vendus | — |
| `partners-reports` | Performance partenaires | — |

### 2.2 Modules complémentaires déjà couverts (hors `entities/reports`)

| Besoin | Module existant | Statut |
|---|---|---|
| Réconciliation TP (rapprochement ligne à ligne) | `features/facturation/rapprochement` | ✅ Complet |
| Récapitulatif mensuel TP | `features/facturation/recapitulatif-mensuel` | ✅ Complet |
| Historique règlements & avoirs TP | `features/facturation/historique`, `/avoirs` | ✅ Complet |
| Rapport EOD / Clôture caisse | `entities/ticketZ/recapitulatif-caisse` | ✅ Complet |
| Billetage (comptage physique) | `entities/mvt-caisse` | ✅ Complet |
| Gestion caisse (gap/écart) | `entities/mvt-caisse` | ✅ Complet |
| Remises RFA fournisseurs (paliers + % atteint) | `features/finances/remises-rfa` | ✅ Complet |

---

## 3. Navigation cible

### 3.1 Structure des routes

```
/reports
  ├── /sales
  │     ├── dashboard-ca           ← Existant (améliorations Phase 2)
  │     ├── sales-summary          ← Existant
  │     ├── comparative-analysis   ← Existant (améliorations Phase 2)
  │     ├── sales-forecast         ← Existant
  │     ├── top-products           ← Existant (amélioration Phase 2)
  │     └── market-basket          ← Existant
  │
  ├── /stock
  │     ├── stock-alerts           ← Existant
  │     ├── stock-valuation        ← Existant
  │     ├── stock-rotation         ← Existant
  │     ├── abc-pareto             ← Existant
  │     └── recap-produit-vendu    ← Existant
  │
  ├── /finance                     ← NOUVEAU CONTENEUR (Phase 3)
  │     ├── pnl-analytique         ← NOUVEAU — GAP-003 + GAP-C4
  │     ├── vieillissement-creances ← NOUVEAU — GAP-002
  │     ├── concentration-payers   ← NOUVEAU — GAP-011
  │     └── cash-flow-bfr          ← NOUVEAU — GAP-005
  │
  ├── /clients
  │     ├── customer-segmentation  ← Existant (RFM)
  │     └── substitution-detail    ← NOUVEAU — GAP-010 (Phase 4)
  │
  └── /partners-reports            ← Existant
        └── supplier-performance   ← Existant (amélioration Phase 4 — GAP-C7)
```

### 3.2 Améliorations des rapports existants (pas de nouveaux conteneurs)

Ces items enrichissent des rapports déjà en production :

| GAP | Rapport cible | Nature |
|---|---|---|
| GAP-C1 | `comparative-analysis` | Nouvel onglet "Par famille / Par fournisseur" |
| GAP-C2 | `dashboard-ca` | Nouvelle carte "Panier moyen 12 mois" |
| GAP-C3 | `comparative-analysis` | Nouveau graphe "Part TP vs. comptant" |
| GAP-C6 | `top-products` | Colonne "Δ Rang vs. période précédente" |

---

## 4. GAPs consolidés

### 4.1 Liste unifiée

| Réf. | Titre | Catégorie | Priorité | Effort |
|---|---|---|:---:|:---:|
| **GAP-C1** | N vs N-1 par famille de produit | Amélioration existant | 🔴 1 | S |
| **GAP-C2** | Évolution panier moyen (12–24 mois) | Amélioration existant | 🔴 1 | XS |
| **GAP-C3** | Évolution part TP vs. comptant dans le CA | Amélioration existant | 🔴 1 | XS |
| **GAP-C6** | Delta rang dans Top Products | Amélioration existant | 🔴 1 | XS |
| **GAP-003** | Marge Brute par Segment / Famille | Nouveau — Finance | 🔴 1 | M |
| **GAP-002** | Vieillissement Créances & DSO | Nouveau — Finance | 🟠 2 | M |
| **GAP-011** | Concentration Payers / Risque institutionnel | Nouveau — Finance | 🟠 2 | M |
| **GAP-005** | BFR & Ratios de Liquidité (DIO/DSO/DPO/CCC) | Nouveau — Finance | 🟠 2 | M |
| **GAP-C4** | Tendance marge brute par famille (12 mois) | Extension GAP-003 | 🟠 2 | S |
| **GAP-010** | Substitution Générique Détail | Nouveau — Clients | 🟡 3 | M |
| **GAP-C7** | N vs N-1 par fournisseur | Amélioration existant | 🟡 3 | S |
| **GAP-C5** | Heat map trafic transactionnel | Backlog | — | L |
| **GAP-C8** | Comparaison 3 années simultanées | Backlog | — | S |
| **GAP-C9** | Overlay moyenne mobile sur historique | Backlog | — | S |
| **GAP-C10** | Drill-down famille → produit (comparatif) | Backlog | — | L |
| **GAP-012** | Rappels de Lot / Pharmacovigilance | Backlog | — | L |

> Effort : XS < 2j · S = 2–5j · M = 5–10j · L > 10j

### 4.2 Ce que proposent les concurrents (benchmark)

| Fonctionnalité | Standard marché | Pharma-Smart |
|---|:---:|:---:|
| CA global N vs N-1 (M/T/An) | ✅ | ✅ Fait |
| YTD + glissement 12 mois | ✅ | ✅ Fait (Dashboard CA) |
| Prévisions avec intervalles de confiance | ✅ | ✅ Fait (3 algos) |
| Comparaison par type de vente | ✅ | ✅ Fait (VO/VNO/Dépôt) |
| **N vs N-1 par famille** | ✅ | ❌ GAP-C1 |
| **Évolution panier moyen** | ✅ | ❌ GAP-C2 |
| **Part TP vs. comptant dans le temps** | ✅ | ❌ GAP-C3 |
| **Delta rang Top Produits** | ✅ | ❌ GAP-C6 |
| Marge brute par segment | 🔵 Avancé | ❌ GAP-003 |
| Vieillissement créances / DSO | 🔵 Avancé | ❌ GAP-002 |
| BFR / CCC | 🔵 Avancé | ❌ GAP-005 |
| Tendance marge par famille (12 mois) | 🔵 Avancé | ❌ GAP-C4 |
| N vs N-1 par fournisseur | 🔵 Avancé | ❌ GAP-C7 |
| Heat map trafic | 🔵 Avancé | Backlog |
| Drill-down comparatif multi-niveaux | 🔵 Avancé | Backlog |

---

## 5. Fiches UX — Rapports prioritaires

### 5.1 GAP-C1 — N vs N-1 par famille (extension Comparative Analysis)

> **Intégration** : Nouvel onglet "Par famille" dans `comparative-analysis` existant.
> Même sélecteur de période (mensuel/trimestriel/annuel). Données chargées en une requête.

```
┌─────────────────────────────────────────────────────────────────┐
│  ANALYSE COMPARATIVE                [Année ▼] [Mois/Trim/An ▼] │
│  [Globale] [Par famille ●] [Par fournisseur]                    │
├─────────────────────────────────────────────────────────────────┤
│  ┌────────────────── Graphe barres groupées ──────────────────┐ │
│  │  ■ 2026  ■ 2025                                            │ │
│  │  Antibiotiques ████████████  7,1M / ██████████ 8,2M +15%  │ │
│  │  Antalgiques   ████████      5,4M / █████████  5,6M  +4%  │ │
│  │  Antipaludéens █████████     4,3M / ███████    3,9M  -9%  │ │
│  └────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────┤
│  Famille              N-1 (2025)    N (2026)    Évol.  Tendance │
│  Antibiotiques        7,1M F        8,2M F    +15,5%   ↗        │
│  Antalgiques          5,4M F        5,6M F     +3,7%   →        │
│  Antipaludéens        4,3M F        3,9M F     −9,3%   ↘        │
│  Dermatologie         2,8M F        3,2M F    +14,3%   ↗        │
│  [Voir les 40 familles]                        [Tri Évol. ▼]    │
│                                              [Export PDF/Excel] │
└─────────────────────────────────────────────────────────────────┘
```

**Interactions clés** :
- Tri par colonne N, N-1, Évolution %
- Clic sur une famille → lien vers `profitability-analysis` filtré sur cette famille
- Export PDF/Excel de la vue active

---

### 5.2 GAP-C2 + GAP-C3 — Évolution panier moyen & Part TP (extensions Dashboard CA)

> **Intégration** : Section "Tendances" à ajouter en bas du `dashboard-ca`.
> Données chargées avec le reste du dashboard (une requête supplémentaire par graphe).

```
┌──────────────────────────────┐  ┌──────────────────────────────┐
│  PANIER MOYEN — 12 mois      │  │  CA par MODE — 12 mois        │
│  Ce mois : 4 850 F           │  │  (aires empilées)             │
│  ↗ +220 F vs M-1 (+4,7%)     │  │  ████████████████ 100%        │
│  [courbe 12 mois]            │  │  ████ TP (68%)  ██ Compt.(32%)│
│  Meilleur mois : Nov (5 120) │  │  Janv TP 62% → Déc TP 68%    │
│  Tendance 6M : +3,2% /an     │  │  ↗ Dépendance TP croissante  │
└──────────────────────────────┘  └──────────────────────────────┘
```

---

### 5.3 GAP-003 + GAP-C4 — Marge Brute par Segment / Famille

**Route** : `/reports/finance/pnl-analytique`

> **Périmètre** : Compte de résultat commercial jusqu'à la marge brute (CA - coût d'achat).
> Les charges fixes (personnel, loyer) ne sont pas dans le système — EBE non calculable.
> GAP-C4 (tendance marge 12 mois) est intégré comme onglet "Évolution" dans ce même rapport.

```
┌─────────────────────────────────────────────────────────────────┐
│  MARGE BRUTE                     [Année ▼] [Mois ▼]            │
│  [Snapshot ●]  [Évolution 12 mois ○]                            │
│  Vue snapshot : [Par segment ●] [Par famille ○]                 │
├─────────────────────────────────────────────────────────────────┤
│  SYNTHÈSE GLOBALE                                               │
│  CA Total: 45,2M F      Marge Brute: 28,4%   Marge: 12,9M F   │
├────────────┬───────────┬───────────┬───────────┬───────────────┤
│            │REMBOURSABLE│   OTC    │   PARA    │   SERVICES    │
│ CA         │  28,5M F  │  12,3M F  │  3,8M F  │   0,6M F     │
│ CA %       │    63%    │    27%    │    8%     │    1,3%      │
│ Coût achat │  21,6M F  │   8,0M F  │  2,3M F  │   0,1M F     │
│ Marge €    │   6,9M F  │   4,3M F  │  1,5M F  │   0,5M F     │
│ Marge %    │ **24,2%** │ **34,9%** │**39,5%** │  **83%**     │
├─────────────────────────────────────────────────────────────────┤
│  VUE "PAR FAMILLE" (toggle)                                     │
│  Famille           CA       Marge €   Marge %   Trend 6M       │
│  Antibiotiques   8,2M F   1,9M F    23,2%      →              │
│  Antalgiques     5,6M F   2,1M F    37,5%      ↗              │
│  Dermatologie    3,2M F   1,3M F    40,6%      ↗ ▲ Top        │
│  [Voir les 40 familles]             [Tri Marge % ▼]            │
├─────────────────────────────────────────────────────────────────┤
│  ONGLET "ÉVOLUTION 12 MOIS" (GAP-C4)                           │
│  ┌─────────────── Marge % par famille × 12 mois ─────────────┐ │
│  │  Dermatologie ──────────────────────── 40,6% ↗            │ │
│  │  Antalgiques  ─────────────────── 37,5% →                 │ │
│  │  Antibiotiques ────────── 23,2% ↘                         │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                              [Export PDF/Excel] │
└─────────────────────────────────────────────────────────────────┘
```

**Interactions clés** :
- Toggle "Par segment / Par famille" : deux jeux de données en une requête
- Onglet "Évolution 12 mois" : graphe courbes Marge % × 12 mois, sélection des familles à afficher
- Tri par colonne dans la vue famille
- Clic famille → `profitability-analysis` filtré

---

### 5.4 GAP-002 — Vieillissement Créances & DSO

**Route** : `/reports/finance/vieillissement-creances`

> **Contexte** : `features/facturation/rapprochement` couvre la réconciliation ligne à ligne.
> Ce rapport ajoute la vue d'**analyse du risque financier** agrégée par organisme.

```
┌─────────────────────────────────────────────────────────────────┐
│  VIEILLISSEMENT DES CRÉANCES TP        [Période ▼] [Organisme ▼]│
├────────────┬────────────┬────────────┬────────────┬────────────┤
│  TOTAL DÛ  │  0 – 30j   │  31 – 60j  │  61 – 90j  │  > 90j ❌  │
│  2,8M F    │  1,2M F    │  680K F    │  500K F    │  420K F    │
│            │  43%       │  24%       │  18%       │  15%       │
│            │ [■ vert]   │[■ jaune]   │[■ orange]  │[■ rouge]   │
├─────────────────────────────────────────────────────────────────┤
│  DSO PAR ORGANISME                                              │
│  Organisme          DSO actuel  DSO N-1  Tendance  Fiabilité    │
│  CNAM               24j         21j      ↗ +3j     🟢 Bon       │
│  INAM               38j         35j      ↗ +3j     🟡 À surveiller│
│  Mut. Entreprise X  52j         48j      ↗ +4j     🟠 Risque    │
│  ONG Santé Y        67j         —        Nouveau   ⚫ Inconnu   │
│  [→ Voir factures]  [→ Rapprochement]                           │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION DSO MOYEN + PART TP DANS CA — 12 mois               │
│  [courbe DSO global | objectif] [aires empilées TP/Comptant]   │
│                                              [Export PDF/Excel] │
└─────────────────────────────────────────────────────────────────┘
```

> **Note** : le graphe "Part TP dans CA" (GAP-C3) est intégré ici plutôt que dans Comparative Analysis
> car il est directement lié au risque de créance TP.

**Interactions clés** :
- Clic sur une tranche d'âge → `rapprochement` filtré sur les lignes correspondantes
- Score de fiabilité calculé sur 12 mois (DSO moyen, variance, tendance)
- Clic organisme → `rapprochement` filtré sur cet organisme

---

### 5.5 GAP-011 — Concentration Payers / Risque Institutionnel

**Route** : `/reports/finance/concentration-payers`

> **Besoin** : En contexte africain (CNAM, INAM, mutuelles entreprises), un seul organisme peut
> représenter 30–50% du CA TP. Si cet organisme retarde ses paiements ou suspend ses remboursements,
> la trésorerie de l'officine est directement menacée. Ce rapport quantifie cette exposition.

```
┌─────────────────────────────────────────────────────────────────┐
│  CONCENTRATION PAYERS — RISQUE INSTITUTIONNEL    [Période ▼]    │
├─────────────────────────────────────────────────────────────────┤
│  CNAM           ████████████████████████ 42% du CA TP  DSO 24j  │
│  INAM           ████████████████ 28%                   DSO 38j  │
│  Mut. Ent. X    ████████ 14%                           DSO 52j  │
│  Autres TP      ████ 9% (14 organismes)                          │
│  Comptant       ███ 7%                                           │
│                                 [Indice de concentration HHI]   │
├─────────────────────────────────────────────────────────────────┤
│  SCÉNARIO DE STRESS                                             │
│  Si CNAM suspend 30j : impact trésorerie estimé  −1,18M F       │
│  Si INAM suspend 30j : impact trésorerie estimé  −0,78M F       │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION CONCENTRATION — 12 mois (graphe aires empilées)      │
│                                              [Export PDF/Excel] │
└─────────────────────────────────────────────────────────────────┘
```

---

### 5.6 GAP-005 — BFR & Ratios de Liquidité

**Route** : `/reports/finance/cash-flow-bfr`

> **Périmètre** : BFR calculé depuis les données disponibles — stock valorisé + créances TP
> (récapitulatif facturation) + dettes fournisseurs (commandes). Pas de projection de trésorerie
> (charges fixes absentes du système).

```
┌─────────────────────────────────────────────────────────────────┐
│  BESOIN EN FONDS DE ROULEMENT (BFR)         [Mois ▼]           │
├────────────┬────────────┬────────────┬────────────┬────────────┤
│  BFR       │  STOCK     │  CRÉANCES TP│  DETTES FN │            │
│  4,8M F    │  +8,2M F   │  +2,3M F   │  -5,7M F   │            │
│  Ce mois   │  DIO: 42j  │  DSO: 31j  │  DPO: 38j  │            │
├─────────────────────────────────────────────────────────────────┤
│  CYCLE DE CONVERSION DE TRÉSORERIE (CCC)                        │
│  DIO(42) + DSO(31) − DPO(38) = 35 jours   [seuil normal <40j] │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION BFR — 12 mois (courbe)           [Export PDF/Excel]  │
│  [BFR mensuel | Stock | Créances TP | Dettes fournisseurs]      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. Patterns UX transverses

### 6.1 Sélecteur de période — Standard unifié

Tous les rapports utilisent le même composant :
```
[Aujourd'hui] [7j] [30j] [Ce mois] [Trimestre] [Année] [Custom 📅…📅]
```
- Sélection persistée dans les query params (URL partageable)
- Filtre personnalisé : date-range picker ng-bootstrap (pas `p-calendar`)

### 6.2 Export — Standard unifié

```
[📄 PDF] [📊 Excel] [📋 CSV]    Généré le: 18/05/2026 14:32
```
- PDF : template Thymeleaf + Flying Saucer (backend)
- Excel : EasyExcel (backend)
- Bouton désactivé avec spinner pendant génération

### 6.3 Navigation par onglets

- Tabs horizontaux en haut de chaque conteneur
- Visibilité conditionnée aux rôles Angular (`UserRouteAccessService`)
- Onglet actif mémorisé dans les query params

### 6.4 Aide contextuelle (Help Drawer)

- Bouton "?" en haut à droite de chaque rapport
- Drawer latéral non-bloquant avec méthodologie de calcul
- Pattern déjà en place sur plusieurs rapports existants — à standardiser

### 6.5 Tableau de données — Colonne Actions

Pour les rapports avec actions possibles depuis la liste :
- Colonne "Actions" en dernière position
- Actions contextuelles selon statut de la ligne
- Confirmation sur actions destructives

---

## 7. Accès par profil

| Rapport | Titulaire | Comptable | Resp. Stock | Préparateur |
|---|:---:|:---:|:---:|:---:|
| P&L Analytique (GAP-003) | ✅ | ✅ | ❌ | ❌ |
| Vieillissement Créances / DSO (GAP-002) | ✅ | ✅ | ❌ | ❌ |
| Concentration Payers (GAP-011) | ✅ | ✅ | ❌ | ❌ |
| Cash Flow BFR (GAP-005) | ✅ | ✅ | ❌ | ❌ |
| Comparative Analysis — vue famille (GAP-C1) | ✅ | ✅ | ❌ | ❌ |
| Panier moyen 12 mois (GAP-C2) | ✅ | ✅ | ❌ | ❌ |
| Part TP vs. comptant (GAP-C3) | ✅ | ✅ | ❌ | ❌ |
| Delta rang Top Products (GAP-C6) | ✅ | ✅ | 🔶 | ❌ |
| Substitution Générique (GAP-010) | ✅ | ❌ | ❌ | ❌ |
| Stock Alerts | ✅ | ❌ | ✅ | ✅ |
| ABC Pareto | ✅ | ❌ | ✅ | 🔶 |
| Stock Rotation | ✅ | ❌ | ✅ | 🔶 |
| Segmentation RFM | ✅ | ✅ | ❌ | ❌ |
| Supplier Performance (+ GAP-C7) | ✅ | 🔶 | ✅ | ❌ |

✅ Accès complet · 🔶 Lecture seule / vue limitée · ❌ Pas d'accès

---

## 8. Roadmap de développement

### Phase 1 — Fondations (Sprint 1-2)

Pré-requis techniques communs :

1. Ajout du conteneur `/finance` dans la navigation
2. Standardisation du composant Export (PDF/Excel/CSV) partagé par tous les rapports
3. Standardisation du sélecteur de période commun

---

### Phase 2 — Quick wins : améliorations rapports existants (Sprint 3-4)

**~10 jours · Fort impact · Zéro nouveau conteneur**

Ces items enrichissent des rapports déjà en production.

| GAP | Action | Effort |
|---|---|:---:|
| **GAP-C1** | Onglet "Par famille / Par fournisseur" dans `comparative-analysis` | 4j |
| **GAP-C2** | Carte "Panier moyen 12 mois" dans `dashboard-ca` | 2j |
| **GAP-C6** | Colonne "Δ Rang vs. période précédente" dans `top-products` | 1j |
| **GAP-C3** | Graphe "Part TP vs. comptant 12 mois" → intégré dans `vieillissement-creances` (Phase 3) | — |

---

### Phase 3 — Module Finance (Sprint 5-10)

**Nouveau conteneur `/reports/finance`**

| # | GAP | Rapport | Effort |
|---|---|---|:---:|
| 1 | **GAP-003 + GAP-C4** | `pnl-analytique` — Marge Brute par Segment/Famille + onglet Évolution 12 mois | 8j |
| 2 | **GAP-002 + GAP-C3** | `vieillissement-creances` — DSO, tranches d'âge, fiabilité + graphe TP/Comptant | 7j |
| 3 | **GAP-011** | `concentration-payers` — Part par organisme, scénario de stress | 6j |
| 4 | **GAP-005** | `cash-flow-bfr` — BFR, DIO/DSO/DPO, CCC, évolution 12 mois | 5j |

---

### Phase 4 — Opérationnel & Fournisseurs (Sprint 11-13)

| # | GAP | Action | Effort |
|---|---|---|:---:|
| 1 | **GAP-010** | Nouveau rapport `substitution-detail` dans `/clients` | 7j |
| 2 | **GAP-C7** | Onglet "Évolution mensuelle" dans `supplier-performance` (N vs N-1 coût, délais) | 4j |

---

## 9. Backlog

Ces items sont identifiés mais non prioritaires à ce stade.

| Réf. | Rapport | Raison du report |
|---|---|---|
| **GAP-C5** | Heat map trafic transactionnel (heure × jour) | Composant matrice à créer de zéro — effort élevé |
| **GAP-C8** | Comparaison 3 années simultanées | Refonte modèle de données du graphe |
| **GAP-C9** | Overlay moyenne mobile sur historique | Plus pertinent une fois GAP-C1 livré |
| **GAP-C10** | Drill-down comparatif famille → produit | Routing imbriqué complexe |
| **GAP-012** | Rappels de Lot / Pharmacovigilance | Dépend d'un flux DPM/AIRP à définir |
| **GAP-006** | Budget vs. Réalisé | Plus pertinent une fois marge et BFR en place |
| **GAP-001** | Registre Stupéfiants | Module dédié hors périmètre reports |
| **GAP-009** | Indicateurs Qualité Dispensation | Référentiel d'objectifs à définir localement |
| **GAP-008** | Adhérence Patients Chroniques | Identification patients chroniques non disponible |
| **GAP-013** | Shrinkage & Pertes Inventaire | Dépend d'un module inventaire physique complet |
| **GAP-014** | Recrutement & Churn Patients | Valeur métier à confirmer |
| **GAP-015** | Calendrier Saisonnier | Sales Forecast couvre déjà la prévision |
| **GAP-016** | Productivité Personnel | Lien vente ↔ opérateur absent du modèle |

---

## 10. Résumé exécutif

```
PHASE 1 — Fondations        Sprint 1-2     Navigation + Export commun
PHASE 2 — Quick wins        Sprint 3-4     4 améliorations existants (~10j)
  GAP-C1  N vs N-1 famille  Sprint 3       +4j — comparative-analysis
  GAP-C2  Panier moyen 12M  Sprint 3       +2j — dashboard-ca
  GAP-C6  Delta rang Top    Sprint 4       +1j — top-products
PHASE 3 — Module Finance    Sprint 5-10   ~26j
  GAP-003 Marge Brute       Sprint 5-6    priorité 1
  GAP-002 Créances / DSO    Sprint 7-8    priorité 2
  GAP-011 Concentration     Sprint 9      priorité 2
  GAP-005 BFR / CCC         Sprint 10     priorité 2
PHASE 4 — Opérationnel      Sprint 11-13  ~11j
  GAP-010 Substitution      Sprint 11-12
  GAP-C7  Fournisseur N/N-1 Sprint 13
BACKLOG                     —             12 items non prioritaires
```
