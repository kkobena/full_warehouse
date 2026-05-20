# Proposition UX — Module Rapports Officine (v2)

## 1. Vision

Le module rapports doit devenir le **cockpit de pilotage** de l'officine : une navigation intuitive par profil utilisateur (titulaire, comptable, responsable stock, préparateur) avec des actions directes depuis les rapports (commander, relancer un organisme, créer un inventaire).

---

## 2. Architecture de navigation proposée

### 2.1 Structure actuelle (à conserver et étendre)

```
/reports
  ├── /sales-reports          (conteneur existant — 7 onglets)
  ├── /stock-reports          (conteneur existant — 5 onglets)
  └── /partners-reports       (conteneur existant — 2 onglets)
```

### 2.2 Structure cible (v2)

> **Note** : Le module `features/facturation` couvre déjà le cycle complet rapprochement → règlement → avoir → récapitulatif mensuel. Les nouveaux rapports ci-dessous **complètent** ce qui existe sans dupliquer.

```
/reports
  ├── /sales                 
  │     ├── dashboard-ca
  │     ├── sales-summary
  │     ├── comparative-analysis
  │     ├── sales-forecast
  │     ├── top-products
  │     └── market-basket
  ├── /stock
  │     ├── stock-alerts
  │     ├── stock-valuation
  │     ├── stock-rotation
  │     ├── abc-pareto
  │     └── recap-produit-vendu
  ├── /finance                ← Nouveau conteneur
  │     ├── pnl-analytique         ← NOUVEAU (GAP-003) — PRIORITÉ 1
  │     ├── vieillissement-creances ← NOUVEAU (GAP-002) — complément rapprochement TP
  │     ├── concentration-payers   ← NOUVEAU (GAP-011) — risque institutionnel
  │     ├── cash-flow-bfr          ← NOUVEAU (GAP-005)
  │     ├── budget-vs-realise      ← NOUVEAU (GAP-006)
  │     └── remises-fournisseurs   ← NOUVEAU (GAP-007)
  │
  │  Note : GAP-004 (Clôture Caisse) → enhancement du module existant
  │          `entities/mvt-caisse` (billetage + bouton clôture + rapport EOD)
  ├── /compliance             ← Nouveau conteneur (backlog — voir section 8)
  └── /clients              
        ├── customer-segmentation (RFM existant)
        └── substitution-detail  ← NOUVEAU (GAP-010)
```

---

## 3. Fiches UX — Rapports prioritaires

### 3.1 Vieillissement Créances & DSO (GAP-002) — COMPLÉMENT RAPPROCHEMENT

> **Contexte** : Le module `features/facturation/rapprochement` couvre déjà la réconciliation ligne à ligne et le récapitulatif mensuel. Ce rapport ajoute la vue d'**analyse du risque financier** agrégée par organisme.

**Route** : `/reports/finance/vieillissement-creances`

```
┌─────────────────────────────────────────────────────────────────┐
│  VIEILLISSEMENT DES CRÉANCES TP        [Période ▼] [Organisme ▼]│
├────────────┬────────────┬────────────┬────────────┬────────────┤
│  TOTAL DÛ  │  0 – 30j   │  31 – 60j  │  61 – 90j  │  > 90j ❌  │
│  2,8M F    │  1,2M F    │  680K F    │  500K F    │  420K F    │
│            │  43%       │  24%       │  18%       │  15%       │
│            │ [■■■ vert] │[■■ jaune]  │[■■ orange] │[■■ rouge]  │
├─────────────────────────────────────────────────────────────────┤
│  DSO PAR ORGANISME (délai moyen encaissement)                   │
│  Organisme          DSO actuel  DSO N-1   Tendance  Fiabilité   │
│  CNAM               24j         21j       ↗ +3j     🟢 Bon      │
│  INAM               38j         35j       ↗ +3j     🟡 À surveiller│
│  Mut. Entreprise X  52j         48j       ↗ +4j     🟠 Risque   │
│  ONG Santé Y        67j         —         Nouveau   ⚫ Inconnu  │
│  [→ Voir factures]  [→ Rapprochement]                           │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION DSO MOYEN (graphe courbe 12 mois)    [Export PDF/Excel]│
│  [DSO global mensuel + ligne objectif]                          │
└─────────────────────────────────────────────────────────────────┘
```

**Interactions clés** :
- Clic sur une tranche d'âge → lien vers le rapprochement filtré sur les lignes correspondantes
- Clic sur un organisme → lien vers le rapprochement filtré sur cet organisme
- Score de fiabilité : calculé sur 12 mois (DSO moyen, variance, tendance)

---

### 3.2 Marge Brute par Segment / Famille (GAP-003) — PRIORITÉ 1

**Route** : `/reports/finance/pnl-analytique`

> **Périmètre** : L'application ne gérant pas les charges fixes (personnel, loyer, autres), ce rapport couvre le **compte de résultat commercial** jusqu'à la marge brute. L'EBE n'est pas calculable automatiquement.
> Deux axes d'analyse disponibles via un toggle : **par segment de vente** (VO/VNO/Para/Services) et **par famille de produit**.

```
┌─────────────────────────────────────────────────────────────────┐
│  MARGE BRUTE                           [Année ▼] [Mois ▼]      │
│  Vue : [Par segment ●] [Par famille ○]                          │
├─────────────────────────────────────────────────────────────────┤
│  SYNTHÈSE GLOBALE                                               │
│  CA Total: 45,2M F      Marge Brute: 28,4%   Marge: 12,9M F   │
├─────────────────────────────────────────────────────────────────┤
│  VUE "PAR SEGMENT"                                              │
├────────────┬───────────┬───────────┬───────────┬───────────────┤
│            │REMBOURSABLE│   OTC    │   PARA    │   SERVICES    │
│ CA         │  28,5M F  │  12,3M F  │  3,8M F  │   0,6M F     │
│ CA %       │    63%    │    27%    │    8%     │    1,3%      │
│ Coût achat │  21,6M F  │   8,0M F  │  2,3M F  │   0,1M F     │
│ Marge €    │   6,9M F  │   4,3M F  │  1,5M F  │   0,5M F     │
│ Marge %    │ **24,2%** │ **34,9%** │**39,5%** │  **83%**     │
├─────────────────────────────────────────────────────────────────┤
│  VUE "PAR FAMILLE DE PRODUIT" (toggle alternatif)               │
├────────────────────────┬──────────┬──────────┬─────────────────┤
│ Famille                │  CA      │  Marge € │  Marge %       │
│ Antibiotiques          │  8,2M F  │  1,9M F  │  23,2%         │
│ Antalgiques            │  5,6M F  │  2,1M F  │  37,5%         │
│ Antihypertenseurs      │  4,8M F  │  1,1M F  │  22,9%         │
│ Antipaludéens          │  3,9M F  │  1,4M F  │  35,9%         │
│ Dermatologie           │  3,2M F  │  1,3M F  │  40,6%  ▲     │
│ [Voir les 40 familles] │          │          │  [tri Marge %] │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION MENSUELLE (graphe barres)         [Export PDF/Excel] │
│  [CA + Marge brute × 12 mois — selon vue active]               │
└─────────────────────────────────────────────────────────────────┘
```

**Interactions clés** :
- Toggle "Par segment / Par famille" rechargé sans appel réseau (deux jeux de données chargés en une seule requête)
- Tri par colonne dans la vue famille (CA, Marge €, Marge %)
- Clic sur une famille → lien vers le rapport Profitability Analysis filtré sur cette famille

---

### 3.3 BFR & Ratios de Liquidité (GAP-005)

**Route** : `/reports/finance/cash-flow-bfr`

> **Périmètre** : Sans gestion des charges fixes, la projection de trésorerie n'est pas calculable. Le rapport se concentre sur le **BFR** (données disponibles dans le système : stock valorisé + créances TP + dettes fournisseurs) et les ratios de rotation.

```
┌─────────────────────────────────────────────────────────────────┐
│  BESOIN EN FONDS DE ROULEMENT (BFR)         [Mois ▼]           │
├────────────┬────────────┬────────────┬────────────┬────────────┤
│  BFR       │  STOCK     │  CRÉANCES TP│  DETTES FN │            │
│  4,8M F    │  +8,2M F   │  +2,3M F   │  -5,7M F   │            │
│  Ce mois   │  DIO: 42j  │  DSO: 31j  │  DPO: 38j  │            │
├─────────────────────────────────────────────────────────────────┤
│  CYCLE DE CONVERSION (CCC)                                      │
│  DIO(42) + DSO(31) - DPO(38) = 35 jours   [normal <40j]       │
├─────────────────────────────────────────────────────────────────┤
│  ÉVOLUTION BFR (graphe courbe 12 mois)      [Export PDF/Excel]  │
│  [BFR mensuel | Stock | Créances TP | Dettes fournisseurs]     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Patterns UX transverses

### 4.1 Filtre & Période — Standard unifié

Tous les rapports utilisent le même composant de sélection de période :
```
[Aujourd'hui] [7j] [30j] [Ce mois] [Trimestre] [Année] [Custom 📅…📅]
```
- La sélection est persistée dans les query params (URL partageable)
- Le filtre personnalisé affiche un date-range picker (ng-bootstrap Datepicker, pas p-calendar)

### 4.2 Export — Standard unifié

Bandeau d'export cohérent sur tous les rapports qui le supportent :
```
[📄 PDF] [📊 Excel] [📋 CSV]    Généré le: 18/05/2026 14:32
```
- Le PDF utilise le template Thymeleaf backend (Flying Saucer)
- L'Excel utilise EasyExcel côté backend
- Le bouton est désactivé (avec spinner) pendant la génération

### 4.3 Tableau de données — Colonne Actions

Pour les rapports où une action est possible depuis la liste :
- Colonne "Actions" en dernière position
- Actions contextuelles selon le statut de la ligne (pas un menu générique)
- Confirmation sur actions destructives

### 4.4 Aide contextuelle (Help Drawer)

Déjà implémenté sur plusieurs rapports existants — à standardiser :
- Bouton "?" en haut à droite de chaque rapport
- Drawer latéral (non-bloquant) expliquant la méthodologie de calcul

### 4.5 Navigation par onglets (conteneurs)

Les conteneurs existants (Sales Reports, Stock Reports, Partners Reports) conservent leur pattern d'onglets. Les nouveaux conteneurs (Finance, Compliance) adoptent le même pattern :
- Tabs horizontaux en haut
- Visibilité des onglets conditionnée aux autorisations Angular (`UserRouteAccessService`)
- Mémorisation de l'onglet actif dans les query params

---

## 5. Accès par profil utilisateur

| Rapport | Titulaire | Comptable | Responsable Stock | Préparateur |
|---|:---:|:---:|:---:|:---:|
| P&L Analytique | ✅ | ✅ | ❌ | ❌ |
| Vieillissement Créances / DSO | ✅ | ✅ | ❌ | ❌ |
| Concentration Payers | ✅ | ✅ | ❌ | ❌ |
| Cash Flow BFR | ✅ | ✅ | ❌ | ❌ |
| Budget vs Réalisé | ✅ | ✅ | ❌ | ❌ |
| Clôture Caisse (mvt-caisse) | ✅ | ✅ | ❌ | ❌ |
| Remises Fournisseurs | ✅ | ✅ | ❌ | ❌ |
| Substitution Générique | ✅ | ❌ | ❌ | ❌ |
| Stock Alerts | ✅ | ❌ | ✅ | ✅ |
| ABC Pareto | ✅ | ❌ | ✅ | 🔶 |
| Stock Rotation | ✅ | ❌ | ✅ | 🔶 |
| Segmentation RFM | ✅ | ✅ | ❌ | ❌ |
| Performance Fournisseurs | ✅ | 🔶 | ✅ | ❌ |

✅ Accès complet | 🔶 Lecture seule / vue limitée | ❌ Pas d'accès

---

## 6. Roadmap de développement

### Phase 1 — Fondations (Sprint 1-2)
1. Restructuration navigation (ajout conteneur Finance)
2. Standardiser le composant Export (PDF/Excel/CSV) commun

### Phase 2 — Pilotage financier (Sprint 3-7)
4. Vieillissement Créances & DSO (GAP-002)
5. Marge Brute par Segment / Famille (GAP-003)
6. Concentration Payers — risque institutionnel (GAP-011)
7. BFR & Ratios de Liquidité (GAP-005)
8. ~~Remises Arrières Fournisseurs (GAP-007)~~ — déjà couvert par `features/finances/remises-rfa`

### Phase 3 — Opérationnel (Sprint 8-10)
10. Substitution Générique Détail (GAP-010)
11. Rappels de Lot / Pharmacovigilance (GAP-012)

---

## 7. Backlog (hors scope actuel)

Ces rapports sont identifiés comme utiles mais non prioritaires à ce stade :

| Rapport | GAP | Raison du report |
|---|---|---|
| **Registre Stupéfiants** | GAP-001 | Obligation légale — module dédié, hors périmètre reports |
| **Budget vs. Réalisé** | GAP-006 | Plus pertinent une fois les rapports de marge et BFR en place |
| **Indicateurs Qualité Dispensation** | GAP-009 | Dépend d'un référentiel d'objectifs à définir |
| **Adhérence Patients Chroniques** | GAP-008 | Nécessite d'identifier les patients chroniques dans le système |
| **Shrinkage & Pertes Inventaire** | GAP-013 | Dépend d'un module inventaire physique complet |
| **Recrutement & Churn Patients** | GAP-014 | Valeur métier à confirmer selon usage réel |
| **Calendrier Saisonnier** | GAP-015 | Faible priorité — Sales Forecast couvre déjà la prévision |
| **Productivité Personnel** | GAP-016 | Dépend du lien vente ↔ opérateur dans le modèle de données |
