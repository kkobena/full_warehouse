# Plan — Module Finances & Comptabilité Pharma-Smart

> Analyse comparative **France + Afrique de l'Ouest** pour rendre Pharma-Smart compétitif
> face aux leaders du marché officinal (Pharmagest, Winpharma, AS Pharm, PioneerRx).
> Version 2 — 05/05/2026

---

## 1. Benchmark — Ce que proposent les LGOs de référence

### 1.1 Pharmagest LGPI (France — leader, ~9 000 pharmacies)

| Fonctionnalité financière | Détail |
|---|---|
| Tableau de bord pharmacien | CA jour/semaine/mois, évolution N-1, panier moyen, nb ordonnances |
| Statistiques ventes | Par famille thérapeutique, laboratoire, produit, opérateur |
| Gestion tiers-payants | Lots AMO/AMC, télétransmission SESAM-Vitale, rapprochement |
| Suivi impayés TP | Vieillissement créances, relances automatiques |
| Suivi fournisseurs | Commandes, soldes dus, RFA (Remises de Fin d'Année) |
| Avoirs fournisseurs | Retours produits, avoirs crédit |
| Valorisation du stock | Valeur PUMP, rotation, produits dormants |
| Marges | Par famille / laboratoire / opérateur / comparatif N-1 |
| Export expert-comptable | Vers SAGE, Cegid (fichier structuré) |
| **Absent** | Grand-livre, balance, journaux SYSCOHADA/PCG |

> **Leçon Pharmagest :** Pas de comptabilité formelle. Tout est dashboard opérationnel + export.
> Le pharmacien voit ses chiffres — le comptable fait la compta dans SAGE.

---

### 1.2 Winpharma / Datascan (France + Afrique de l'Ouest — 77 % Bamako)

| Fonctionnalité financière | Détail |
|---|---|
| Tableau de bord | CA, marges, évolution |
| Gestion tiers-payants | Lots, facturation, rapprochement |
| Suivi dettes fournisseurs | Soldes dus, historique règlements |
| Facturation clients différés | Créances, règlements |
| Statistiques ventes | Par famille, laboratoire |
| Export comptable | Paramétrable selon installation locale |
| **Absent** | Journaux formels, grand-livre, balance |

---

### 1.3 LGPI / Opus (France — marché secondaire)

| Fonctionnalité financière | Détail |
|---|---|
| Marges et rentabilité | Analyse détaillée par produit / famille / laboratoire |
| Comparatif N / N-1 | Évolution CA, volumes, marges |
| Suivi groupements | Remises grossistes OCP, Phoenix, CERP, Alliance Healthcare |
| RFA automatisées | Calcul remises de fin d'année par fournisseur |
| Stock dormant | Produits sans mouvement > X jours, valeur immobilisée |
| Valorisation PUMP | Coût réel du stock à l'instant T |

---

### 1.4 AS Pharm (Afrique — SaaS panafricain, le plus complet sur le marché africain)

| Fonctionnalité financière | Détail |
|---|---|
| Dashboard financier intégré | CA, marges, dettes, créances en temps réel |
| Comptes fournisseurs (AP) | Suivi intégral : soldes, échéances, règlements |
| Comptes tiers-payants (AR) | Facturation, rapprochement, impayés |
| Comptabilité intégrée | Journaux automatiques, balance, grand-livre |
| Analytique | Ventes par famille, labo, opérateur |
| Export expert-comptable | Format SYSCOHADA |
| **Argument commercial** | « Tout dans un seul système » — différenciateur clé |

---

### 1.5 PioneerRx (USA — référence internationale)

| Fonctionnalité financière | Détail |
|---|---|
| GL intégré | Grand-livre en temps réel |
| AR/AP natif | Créances + dettes dans le LGO |
| COGS automatique | Coût marchandises vendues → écriture instantanée |
| Réconciliation caisse | Automatique |
| Tableaux de bord | Rentabilité par segment, évolution |

---

## 2. Tableau comparatif — Pharma-Smart vs concurrents

| Fonctionnalité | Pharmagest | Winpharma | AS Pharm | PioneerRx | **Pharma-Smart actuel** |
|---|:---:|:---:|:---:|:---:|:---:|
| Dashboard financier global | ✅ | ✅ | ✅ | ✅ | ⚠️ Partiel |
| Comparatif N / N-1 | ✅ | ✅ | ✅ | ✅ | ❌ |
| Marges par famille/labo | ✅ | ✅ | ✅ | ✅ | ❌ |
| Valorisation du stock PUMP | ✅ | ✅ | ✅ | ✅ | ❌ |
| Produits dormants / rotation | ✅ | ⚠️ | ✅ | ✅ | ❌ |
| Comptes fournisseurs AP | ✅ | ✅ | ✅ | ✅ | ❌ |
| Remises & RFA fournisseurs | ✅ | ⚠️ | ✅ | ❌ | ❌ |
| AR Tiers-payants complet | ✅ | ✅ | ✅ | ✅ | ✅ Complet |
| AR Clients différés | ✅ | ✅ | ✅ | ✅ | ✅ |
| Réconciliation caisse | ✅ | ✅ | ✅ | ✅ | ✅ |
| Déclaration TVA complète | ⚠️ Export | ⚠️ | ✅ | ✅ | ⚠️ Collectée seul. |
| Export expert-comptable | ✅ | ✅ | ✅ | ✅ | ❌ |
| Grand-livre / Balance | ❌ | ❌ | ✅ | ✅ | ❌ |
| Journaux comptables formels | ❌ | ❌ | ✅ | ✅ | ❌ |

---

## 3. Inventaire réel — Ce qui existe vs ce qui manque

### Modules `reports` déjà implémentés dans Pharma-Smart

| Composant | Module route | Fonctionnalité couverte | Plan initial |
|---|---|---|---|
| `jhi-dashboard-ca` | `reports/sales` | CA + graphiques ventes par période | ≈ Dashboard financier (partiel) |
| `jhi-profitability-analysis` | `reports/sales` | CA, marge brute, taux marge, produits faible marge, par famille | ✅ **Marges & Rentabilité** |
| `jhi-comparative-analysis` | `reports/sales` | Tableaux comparatifs N/N-1 | ✅ **Comparatif N/N-1** |
| `jhi-top-products` | `reports/sales` | Top produits par CA/volume | ✅ **Top produits** |
| `jhi-sales-forecast` | `reports/sales` | Prévisions de ventes | bonus |
| `jhi-market-basket` | `reports/sales` | Analyse du panier moyen | bonus |
| `jhi-stock-valuation` | `reports/stock` | Valeur stock PUMP + valeur vente, export PDF | ✅ **Stock & Valorisation** |
| `jhi-stock-rotation` | `reports/stock` | Rotation du stock, taux | ✅ **Dormants/rotation** |
| `jhi-abc-pareto` | `reports/stock` | Classement ABC produits | bonus |
| `jhi-stock-alerts` | `reports/stock` | Alertes rupture/péremption | bonus |
| `jhi-supplier-performance` | `reports/partners` | Score qualité, délais livraison, conformité fournisseurs | ≠ AP — analytics perf uniquement |
| `jhi-customer-segmentation` | `reports/partners` | Segmentation clients | bonus |

> ⚠️ **Important :** `jhi-supplier-performance` mesure *"ce fournisseur est-il fiable ?"*
> (délais, conformité, score) — c'est de l'**analytique**.
> Les **Comptes fournisseurs AP** mesurent *"combien est-ce que je lui dois ?"*
> (soldes, dettes, règlements, échéances) — c'est de la **trésorerie**. Ce sont deux modules distincts.

---

## 4. Ce que Pharma-Smart doit encore avoir pour être compétitif

### 🔴 Niveau 1 — Manquants critiques (présents chez TOUS les concurrents)

| # | Fonctionnalité | Statut actuel | Impact métier |
|---|---|---|---|
| 1 | **Dashboard financier global** — CA + marges + dettes + créances en 1 écran | ⚠️ `dashboard-ca` existe mais sans dettes/créances fournisseurs | Vision 360° manquante |
| 2 | **Comptes fournisseurs AP** — soldes, dettes, échéances, règlements | ❌ `supplier-performance` ≠ AP | Risque blocage livraison, perte remises |
| 3 | **Export expert-comptable** one-click | ❌ | 2-3h/mois perdues sur Excel |

### 🟡 Niveau 2 — Manquants différenciateurs

| # | Fonctionnalité | Statut actuel | Valeur |
|---|---|---|---|
| 4 | **TVA enrichie** — collectée + déductible + net à payer | ⚠️ `jhi-taxe-report` = TVA collectée seulement | Déclaration complète en 1 clic |
| 5 | **Remises & RFA fournisseurs** | ❌ | Récupère de l'argent souvent oublié |

### ✅ Niveau 1 — Déjà implémentés (à intégrer dans le menu Finances)

| # | Fonctionnalité | Composant existant |
|---|---|---|
| A | Marges & Rentabilité par famille/laboratoire | `jhi-profitability-analysis` |
| B | Comparatif N / N-1 | `jhi-comparative-analysis` |
| C | Stock & Valorisation PUMP | `jhi-stock-valuation` |
| D | Rotation du stock / dormants | `jhi-stock-rotation` |
| E | Top produits | `jhi-top-products` |
| F | Performance fournisseurs (qualité/délais) | `jhi-supplier-performance` |

### 🟢 Niveau 3 — Avancé (AS Pharm / PioneerRx uniquement)

| # | Fonctionnalité | Pertinence |
|---|---|---|
| 6 | Journaux comptables automatiques + export SYSCOHADA/FEC | Pour expert-comptable, phase 3 |

---

## 5. Menu dédié proposé — « Finances »

> Nom choisi : **"Finances"** plutôt que "Comptabilité" — plus accessible pour le pharmacien.
> Les composants marqués ✅ **existent déjà** et sont simplement à référencer dans ce nouveau menu.

```
┌─────────────────────────────────────────────────────────────┐
│  💰  Finances                                                │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ── VUE GLOBALE ────────────────────────────────────────── │
│  🏠  Tableau de bord financier  [P1 — ENRICHIR dashboard-ca]│
│      CA · Marges · Dettes fourn. · Créances TP · N-1         │
│                                                              │
│  ── CAISSE (existant — conservé dans mvt-caisse aussi) ──── │
│  💵  Mouvements de caisse       [existant — mvt-caisse]     │
│  ⚖️  Balance caisse              [existant — mvt-caisse]    │
│  🧾  Récapitulatif de caisse    [existant — mvt-caisse]     │
│  ⚙️  Gestion de caisse           [existant — mvt-caisse]    │
│                                                              │
│  ── FOURNISSEURS ──────────────────────────────────────── │
│  🚚  Comptes fournisseurs (AP)  [P1 — NOUVEAU]              │
│      Soldes · Dettes · Échéances · Règlements               │
│  ⭐  Performance fournisseurs   [✅ jhi-supplier-performance]│
│      Score · Délais · Conformité · Volume                   │
│  🎁  Remises & RFA              [P2 — NOUVEAU]              │
│      Paliers en cours · RFA attendues · Avoirs              │
│                                                              │
│  ── ANALYSES ──────────────────────────────────────────── │
│  📈  Rentabilité & Marges       [✅ jhi-profitability-analysis]│
│      CA · Marge brute · Taux · Par famille · Faible marge   │
│  📊  Tableaux comparatifs N-1   [✅ jhi-comparative-analysis]│
│      Évolution CA · Volumes · Marges vs période précédente  │
│  🏆  Top Produits               [✅ jhi-top-products]        │
│  📦  Valorisation du Stock      [✅ jhi-stock-valuation]     │
│      Valeur PUMP · Valeur vente · Par famille               │
│  🔄  Rotation du Stock          [✅ jhi-stock-rotation]      │
│      Taux rotation · Dormants · Invendus                    │
│                                                              │
│  ── FISCAL & EXPORT ───────────────────────────────────── │
│  🧾  Déclaration TVA            [P2 — ENRICHIR taxe-report] │
│      Collectée + Déductible + Net à payer                   │
│  ⬇️  Export expert-comptable    [P2 — NOUVEAU]              │
│      PDF · Excel · CSV SYSCOHADA                            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Détail des nouveaux menus

### 5.1 Tableau de bord financier global (P1)

**Ce que montre Pharmagest / Winpharma / AS Pharm :**

```
┌─────────────────────────────────────────────────────────┐
│  KPI ROW 1 — CHIFFRE D'AFFAIRES                         │
│  CA aujourd'hui | CA mois (vs N-1 ▲+12%) | CA année     │
├─────────────────────────────────────────────────────────┤
│  KPI ROW 2 — MARGES                                     │
│  Marge brute % | Marge en valeur | Meilleure famille     │
├─────────────────────────────────────────────────────────┤
│  KPI ROW 3 — TRÉSORERIE & TIERS                         │
│  Dettes fournisseurs | Créances TP | Différés clients    │
├─────────────────────────────────────────────────────────┤
│  GRAPHIQUE — Évolution CA 12 mois (barre) + Marge (ligne)│
├─────────────────────────────────────────────────────────┤
│  TOP 5 familles CA   |   TOP 5 familles marge            │
└─────────────────────────────────────────────────────────┘
```

**Sources données existantes :** `Sales`, `SalesLine`, `Commande`, `MvtCaisse`, `Facture`

---

### 5.2 Comptes fournisseurs AP (P1)

**Ce que montre Winpharma / Pharmagest :**

```
KPI Banner : Total dû | En retard | Échéance J+7 | Nb fournisseurs actifs
───────────────────────────────────────────────────────────────────────────
Table :
  Fournisseur | Dernier achat | Total commandé | Réglé | Solde dû | Statut
───────────────────────────────────────────────────────────────────────────
Panel détail (clic ligne) :
  Historique commandes non réglées | Règlements enregistrés
  [Enregistrer un règlement]   [Voir les avoirs]
```

**Sources données existantes :** `Fournisseur`, `Commande`, `Reglement`

---

### 5.3 Marges & Rentabilité (P1)

**Ce que montre Pharmagest / Opus :**

```
Filtres : Du [date] au [date] | Famille | Laboratoire | Opérateur
───────────────────────────────────────────────────────────────────
Table AG Grid :
  Famille | CA | Qtés | Prix moy. | Coût moy. | Marge % | Marge val. | Évol. N-1
───────────────────────────────────────────────────────────────────
Graphique : Marge % par famille triée (barres horizontales)
Export Excel
```

**Sources données existantes :** `SalesLine`, `Produit`, `FamilleProduit`, `Storage`

---

### 5.4 Stock & Valorisation (P1)

**Ce que montre Pharmagest / Winpharma :**

```
KPI Banner : Valeur totale PUMP | Nb références | Dormants > 60j | Taux rotation moyen
───────────────────────────────────────────────────────────────────────────────
Onglets :
  [Valorisation par famille]  [Produits dormants]  [Rotation rapide]  [Ruptures]
───────────────────────────────────────────────────────────────────────────────
Dormants : tri par valeur immobilisée → bouton "Retour fournisseur" direct
```

**Sources données existantes :** `Storage`, `Produit`, `SalesLine`, `CommandeItem`

---

### 5.5 Remises & RFA fournisseurs (P2)

**Spécifique Pharmagest / LGPI France + AS Pharm :**

```
Table : Fournisseur | Palier RFA objectif | CA commandé | % atteint | RFA estimée
Historique des avoirs reçus des fournisseurs
Alerte : "Vous êtes à 85% du palier LABOREX — commandez 15 000 FCFA de plus pour
          débloquer la remise de fin d'année"
```

---

### 5.6 Déclaration TVA enrichie (P2)

**Ce que font les LGOs sérieux (AS Pharm, PioneerRx) — absente chez Pharmagest :**

```
Période : Mois / Trimestre
─────────────────────────────────────
TVA collectée (ventes)       :  X FCFA
TVA déductible (achats)      : -Y FCFA
─────────────────────────────────────
TVA nette à payer            :  Z FCFA
─────────────────────────────────────
Détail par taux (18%, 0%)
Export PDF état déclaratif
```

> Remplace progressivement l'existant `jhi-taxe-report` qui ne couvre que la TVA collectée.

---

### 5.7 Export expert-comptable (P2)

**Présent chez TOUS les LGOs sérieux :**

```
Sélection période : Du [date] au [date]
Format : ○ Excel  ○ CSV SYSCOHADA  ○ PDF récapitulatif

Contenu inclus (cases à cocher) :
  ☑ Ventes par famille
  ☑ Achats par fournisseur
  ☑ Mouvements de caisse
  ☑ Tiers-payants (créances + règlements)
  ☑ Différés clients
  ☑ TVA collectée + déductible

[Générer l'export]
```

---

## 6. Ce qui reste dans `mvt-caisse`

Le module `mvt-caisse` conserve son rôle **opérationnel quotidien** — inchangé.

| Menu | Maintenu | Remarque |
|---|---|---|
| Mouvements de caisse | ✅ | Opérationnel caissier |
| Balance caisse | ✅ | Clôture journalière |
| Récapitulatif de caisse | ✅ | Ticket Z par user |
| Gestion de caisse | ✅ | Config |
| Tableau pharmacien | ✅ | Aussi accessible depuis Finances |
| Rapport d'activité | ✅ | Aussi accessible depuis Finances |
| Rapport TVA | 🔄 | Remplacé par TVA enrichie en P2 |

---

## 7. Architecture Angular — `features/finances`

```
src/main/webapp/app/features/finances/
├── finances.routes.ts
├── data-access/
│   ├── models/
│   │   ├── dashboard-financier.model.ts
│   │   ├── fournisseur-ap.model.ts
│   │   ├── marge.model.ts
│   │   ├── stock-valorisation.model.ts
│   │   └── tva.model.ts
│   └── services/
│       ├── dashboard-financier-api.service.ts
│       ├── fournisseur-ap-api.service.ts
│       ├── marge-api.service.ts
│       ├── stock-valorisation-api.service.ts
│       └── export-api.service.ts
├── feature/
│   ├── finances-layout/              ← sidebar + ngbNav
│   ├── dashboard-financier/          ← P1 - KPIs globaux + graphiques
│   ├── comptes-fournisseurs/         ← P1 - AP soldes/dettes
│   ├── marges-rentabilite/           ← P1 - marges par famille/labo
│   ├── stock-valorisation/           ← P1 - PUMP + dormants
│   ├── remises-rfa/                  ← P2 - remises fournisseurs
│   ├── declaration-tva/              ← P2 - TVA enrichie
│   └── export-comptable/             ← P2 - export one-click
└── ui/
    ├── kpi-finances-banner/          ← KPI banner réutilisable
    ├── evolution-chart/              ← Chart.js CA + marge 12 mois
    ├── marge-table/                  ← AG Grid marges
    └── fournisseur-detail-panel/     ← panel détail fournisseur
```

---

## 8. APIs Backend à créer

```
GET  /api/finances/dashboard?debut=&fin=              → KPIs globaux + top familles
GET  /api/finances/fournisseurs/ap?page=&size=        → soldes AP avec échéances
GET  /api/finances/fournisseurs/ap/{id}/lignes        → détail commandes par fournisseur
POST /api/finances/fournisseurs/ap/{id}/reglement     → enregistrer un règlement
GET  /api/finances/marges?debut=&fin=&groupBy=        → marges par famille|labo|operateur
GET  /api/finances/marges/evolution?debut=&fin=       → comparatif N / N-1
GET  /api/finances/stock/valorisation                 → valeur PUMP par famille/dépôt
GET  /api/finances/stock/dormants?seuil=60            → produits sans mouvement
GET  /api/finances/tva?debut=&fin=                    → TVA collectée + déductible
GET  /api/finances/export?debut=&fin=&format=excel|csv|pdf → fichier export
```

---

## 9. Phases et planning révisées

### ✅ Phase 0 — Zéro développement — Simplement intégrer l'existant dans le menu Finances

Ces composants existent, il suffit de les référencer dans `finances-layout` :

| Composant | Sélecteur | Menu cible |
|---|---|---|
| `ProfitabilityAnalysisComponent` | `jhi-profitability-analysis` | Rentabilité & Marges |
| `ComparativeAnalysisComponent` | `jhi-comparative-analysis` | Tableaux comparatifs N-1 |
| `TopProductsComponent` | `jhi-top-products` | Top Produits |
| `StockValuationComponent` | `jhi-stock-valuation` | Valorisation du Stock |
| `StockRotationComponent` | `jhi-stock-rotation` | Rotation du Stock |
| `SupplierPerformanceComponent` | `jhi-supplier-performance` | Performance fournisseurs |

### 🔴 Phase 1 — A créer (must-have manquants) — ~1 semaine

| Tâche | Effort | Composant |
|---|---|---|
| Dashboard financier global (enrichir `dashboard-ca` avec dettes/créances) | 2-3j | `jhi-dashboard-ca` à enrichir |
| Comptes fournisseurs AP — soldes, dettes, règlements | 3-4j | nouveau composant |

### 🟡 Phase 2 — A créer (différenciateurs) — ~1 semaine

| Tâche | Effort | Composant |
|---|---|---|
| Déclaration TVA enrichie (+ TVA déductible sur achats) | 2j | enrichir `jhi-taxe-report` |
| Export expert-comptable one-click | 2j | nouveau composant |
| Remises & RFA fournisseurs | 3j | nouveau composant |

### 🟢 Phase 3 — Avancé (surpasser AS Pharm) — ~1 semaine

| Tâche | Effort |
|---|---|
| Journaux comptables automatiques + export SYSCOHADA/FEC | 4-5j |

---

*Document mis à jour le 05/05/2026 — Version 3 (inventaire composants existants + plan révisé)*
