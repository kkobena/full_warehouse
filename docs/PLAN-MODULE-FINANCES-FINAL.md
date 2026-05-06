# Plan final — Module « Finances » Pharma-Smart

> Version finale — 05/05/2026
> Objectif : rendre Pharma-Smart compétitif face à Pharmagest, Winpharma, AS Pharm
> en exploitant au maximum l'existant avant de créer du nouveau.

---

## Principe d'architecture

> Les modules `reports/sales`, `reports/stock`, `reports/partners` et `mvt-caisse` existent déjà
> avec leurs propres sidebars et leurs propres routes. **Les dupliquer dans `finances-layout`
> serait une erreur** : double navigation, confusion utilisateur, maintenance doublée.
>
> `finances-layout` ne contient **que ce qui n'existe nulle part ailleurs**.
> Les autres modules restent à leur place et sont accessibles via des liens depuis le Dashboard.

## Résumé exécutif

| Phase | Effort | Ce qu'on fait |
|---|---|---|
| **P0** | ~2-3 jours | Enrichir `jhi-dashboard-ca` (+dettes/créances) + enrichir `jhi-taxe-report` (+TVA déductible) |
| **P1** | ~3-4 jours | Nouveau module : Comptes fournisseurs AP |
| **P2** | ~5 jours | Remises/RFA + Export expert-comptable |
| **P3** | ~5 jours | Journaux SYSCOHADA (avancé) |

---

## 1. Inventaire des composants existants à intégrer

### Module `reports/sales` (route `/reports/sales`)

| Composant | Sélecteur | Ce qu'il couvre |
|---|---|---|
| `DashboardCaComponent` | `jhi-dashboard-ca` | CA aujourd'hui/semaine/mois/année + évolution N-1 + graphiques + export PDF/Excel/CSV |
| `ProfitabilityAnalysisComponent` | `jhi-profitability-analysis` | CA, marge brute, taux marge, produits faible marge, par famille |
| `ComparativeAnalysisComponent` | `jhi-comparative-analysis` | Comparatif N / N-1 ventes + volumes |
| `TopProductsComponent` | `jhi-top-products` | Top produits par CA et volume |
| `SalesForecastComponent` | `jhi-sales-forecast` | Prévisions de ventes |
| `MarketBasketComponent` | `jhi-market-basket` | Analyse du panier moyen |

### Module `reports/stock` (route `/reports/stock`)

| Composant | Sélecteur | Ce qu'il couvre |
|---|---|---|
| `StockValuationComponent` | `jhi-stock-valuation` | Valeur PUMP + valeur vente par famille, export PDF |
| `StockRotationComponent` | `jhi-stock-rotation` | Rotation du stock, taux, produits dormants |
| `AbcParetoComponent` | `jhi-abc-pareto` | Classement ABC produits |
| `StockAlertsComponent` | `jhi-stock-alerts` | Alertes rupture / péremption |
| `RecapProduitVenduComponent` | `jhi-recap-produit-vendu` | Récap produits vendus/invendus |

### Module `reports/partners` (route `/reports/partners`)

| Composant | Sélecteur | Ce qu'il couvre |
|---|---|---|
| `SupplierPerformanceComponent` | `jhi-supplier-performance` | Score qualité, délais livraison, conformité, volume achats |
| `CustomerSegmentationComponent` | `jhi-customer-segmentation` | Segmentation clients |

### Module `mvt-caisse`

| Composant | Sélecteur | Ce qu'il couvre | Enrichissement P0 |
|---|---|---|---|
| `TaxeReportComponent` | `jhi-taxe-report` | TVA collectée (ventes), HT, TTC, `montantAchat` déjà présent | ➕ Ajouter TVA déductible sur achats |

> ⚠️ **Distinction importante :**
> `jhi-supplier-performance` = analytics qualité fournisseur (score/délais/conformité)
> `Comptes fournisseurs AP` = trésorerie (combien je dois, à qui, quand) → **à créer en P1**

---

## 2. Menu « Finances » — 5 entrées seulement

> Les modules `reports/sales`, `reports/stock`, `reports/partners` et `mvt-caisse`
> restent à leur place avec leurs propres sidebars.
> `finances-layout` ne contient que ce qui **n'existe nulle part ailleurs**.

```
┌──────────────────────────────────────────────────────────────┐
│  💰  Finances                                                 │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  🏠  Dashboard Financier Global   [P0 — enrichir dashboard-ca]│
│      CA · Marge · Dettes fourn. · Créances TP                 │
│      + liens vers les modules existants                       │
│                                                               │
│  🚚  Comptes fournisseurs (AP)    [P1 — NOUVEAU]             │
│      Soldes · Dettes · Échéances · Règlements                 │
│                                                               │
│  🧾  Déclaration TVA              [P0 — enrichir taxe-report] │
│      Collectée + Déductible (achats) + Net à payer            │
│                                                               │
│  🎁  Remises & RFA                [P2 — NOUVEAU]             │
│      Paliers en cours · RFA attendues · Avoirs reçus          │
│                                                               │
│  ⬇️  Export expert-comptable      [P2 — NOUVEAU]             │
│      Excel · CSV SYSCOHADA · PDF récapitulatif                │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### Modules existants — inchangés, accessibles depuis leurs routes habituelles

| Route | Module | Menus |
|---|---|---|
| `/reports/sales` | Chiffre d'Affaires | Dashboard CA · Rentabilité · Comparatif N-1 · Top Produits · Prévisions · Panier |
| `/reports/stock` | Stock & Inventaire | Valorisation · Rotation · ABC Pareto · Alertes · Récap |
| `/reports/partners` | Clients & Fournisseurs | Performance fournisseurs · Segmentation clients |
| `/mvt-caisse` | Caisse | Mouvements · Balance · Récapitulatif · Gestion · Tableau pharmacien · Activité |

---

## 3. Phase 0 — Enrichissements légers (pas de nouveau module)

### 3.1 Dashboard Financier — enrichir `jhi-dashboard-ca`

**Ce qu'il y a déjà :**
- CA aujourd'hui / semaine / mois / année + évolution N-1
- Graphiques + export PDF / Excel / CSV

**Ce à ajouter (2 nouvelles cartes KPI + 1 appel API) :**

```
┌──────────────────────────┐  ┌──────────────────────────┐
│  🚚 Dettes fournisseurs  │  │  📋 Créances TP en attente│
│  Total dû : X FCFA       │  │  Total : Y FCFA          │
│  ⚠ 3 échéances passées   │  │  ⚠ Z impayées > 30j      │
└──────────────────────────┘  └──────────────────────────┘
```

**Backend — 1 endpoint à ajouter :**
```
GET /api/reports/dashboard-ca/summary-finances
→ Retourne en plus : totalDetteFournisseurs, nbEcheancesEnRetard,
                     totalCreancesTP, nbFacturesImpayees
```

**Frontend — `dashboard-ca.component.html` :**
- Ajouter 2 cartes dans la row KPI existante
- Lien cliquable vers "Comptes fournisseurs" et "Facturation"

---

### 3.2 Déclaration TVA — nouveau composant `app-declaration-tva`

> **Choix d'architecture :** nouveau composant indépendant dans `features/finances`.
> `jhi-taxe-report` dans `mvt-caisse` reste **inchangé**.
> Après comparaison usage terrain, l'un des deux sera supprimé.

| | `jhi-taxe-report` (existant) | `app-declaration-tva` (nouveau) |
|---|---|---|
| Emplacement | `mvt-caisse` | `features/finances` |
| TVA collectée | ✅ | ✅ |
| TVA déductible (achats) | ❌ | ✅ |
| TVA nette à payer | ❌ | ✅ |
| Graphique doughnut | ✅ | optionnel |
| Export PDF | ✅ | ✅ |
| Touché par ce plan | ❌ non | création P0 |

**Backend — nouvelle route dans `TaxeResource.java` existant (pas de nouveau controller) :**
```java
// Ajouter dans TaxeResource.java
GET /api/taxe-report/declaration
→ TaxeWrapperDTO enrichi avec tvaDeductible + tvaNette
```

**`TaxeWrapperDTO.java` — ajouter 2 champs :**
```java
private long tvaDeductible;  // TVA sur achats
private long tvaNette;       // montantTaxe - tvaDeductible
```

**`TaxeService.java` — ajouter 1 méthode :**
```java
TaxeWrapperDTO fetchDeclarationTva(MvtParam mvtParam);
```

**Affichage du nouveau composant :**
```
Filtres : Du [date] au [date] | Type vente

──────────────────────────────────────────────────────
  RÉSUMÉ TVA
  TVA collectée  (sur ventes)    :   X FCFA
  TVA déductible (sur achats)    : - Y FCFA
  ──────────────────────────────────────────
  TVA nette à payer              :   Z FCFA
──────────────────────────────────────────────────────

  Tableau de détail par taux (18%, 9%, 0%...)
  | Taux | Base HT ventes | TVA collectée | Base HT achats | TVA déductible |

  [Export PDF]
```

---

## 4. Phase 1 — Comptes fournisseurs AP

**Contexte :** `SupplierPerformanceReportResource.java` gère déjà les données fournisseurs
(performance analytics depuis la vue matérialisée `mv_supplier_performance`).
Les routes AP sont **ajoutées dans ce même controller** — pas de nouveau fichier.

> **Distinction :**
> - Routes existantes `/supplier-performance` → analytics (score, délais, conformité, volumes)
> - Nouvelles routes `/supplier-performance/ap/**` → trésorerie (soldes, dettes, règlements)

### 4.1 Backend — routes à ajouter dans `SupplierPerformanceReportResource.java`

```java
// Nouvelles routes à ajouter dans SupplierPerformanceReportResource.java

GET  /api/supplier-performance/ap
     → Liste des fournisseurs avec solde dû, statut, prochaine échéance

GET  /api/supplier-performance/{id}/ap/lignes
     → Commandes non réglées + partiellement réglées pour ce fournisseur

POST /api/supplier-performance/{id}/ap/reglement
     → Enregistrer un règlement manuel (montant, date, référence)
```

**Source des données :** `Commande` + `Reglement` — tables déjà existantes en base.

**Nouveaux DTOs à créer** (dans `service/dto/report/`) :

```java
// CompteFournisseurAPDTO.java
public record CompteFournisseurAPDTO(
    Integer fournisseurId,
    String  fournisseurName,
    String  fournisseurCode,
    String  phone,
    Long    totalCommande,      // TTC total commandé sur la période
    Long    totalRegle,         // Total réglé
    Long    solde,              // totalCommande - totalRegle
    Integer nbCommandesEnAttente,
    LocalDate prochaineEcheance,
    String  statut              // A_JOUR | EN_RETARD | CRITIQUE
) {}

// LigneFournisseurAPDTO.java
public record LigneFournisseurAPDTO(
    Long      commandeId,
    String    numBon,
    LocalDate dateCommande,
    LocalDate dateEcheance,
    Long      montant,
    Long      montantRegle,
    Long      restantDu,
    String    statut            // EN_ATTENTE | PARTIEL | REGLE | EN_RETARD
) {}

// ReglementFournisseurCommand.java
public record ReglementFournisseurCommand(
    Long      montant,
    LocalDate dateReglement,
    String    reference,
    String    commentaire
) {}
```

**Nouveau service à créer** : `CompteFournisseurAPService` (ou méthode dans `SupplierPerformanceReportService`)

### 4.2 Frontend

**Nouveau composant :** `features/finances/feature/comptes-fournisseurs/`
**Sélecteur :** `app-comptes-fournisseurs`

```
KPI Banner (4 cartes) :
  Total dû | Échéances dépassées | Échéances J+7 | Nb fournisseurs actifs
──────────────────────────────────────────────────────────────────────────
Table AG Grid :
  Fournisseur | Code | Tél. | Total commandé | Réglé | Solde dû | Statut
  (coloré : vert=A_JOUR, orange=EN_RETARD, rouge=CRITIQUE)
──────────────────────────────────────────────────────────────────────────
Panel détail (clic ligne) :
  Liste des commandes non réglées (numBon, date, échéance, restant dû)
  [Enregistrer un règlement]
```

**Nouveau service Angular :** `fournisseur-ap-api.service.ts`
```typescript
// appelle /api/supplier-performance/ap
// appelle /api/supplier-performance/{id}/ap/lignes
// appelle POST /api/supplier-performance/{id}/ap/reglement
```

---

## 5. Phase 2 — Remises & RFA + Export expert-comptable

### 5.1 Remises & RFA fournisseurs

**Source données :** `Avoir` (avoirs fournisseurs) + paramétrage paliers RFA par fournisseur.

**Affichage :**
```
Table : Fournisseur | Palier RFA | CA commandé N | % atteint | RFA estimée | RFA reçue
Alerte : "85% du palier LABOREX — 50 000 FCFA de plus débloquent la RFA"
Historique avoirs reçus
```

### 5.2 Export expert-comptable

**Contenu one-click :**
```
Période : Du [date] au [date]
Format  : ○ Excel  ○ CSV SYSCOHADA  ○ PDF récapitulatif

☑ Ventes par famille (CA + TVA)
☑ Achats par fournisseur
☑ Mouvements de caisse
☑ Tiers-payants (créances + règlements)
☑ Différés clients
☑ TVA collectée + déductible + nette
[Générer l'export]
```

---

## 6. Phase 3 — Journaux SYSCOHADA (avancé)

Pour les pharmacies qui ont un expert-comptable exigeant un fichier FEC/SYSCOHADA standardisé.

**Table à créer :**
```sql
-- V1.x.x__journal_comptable.sql
CREATE TABLE ecriture_journal (
    id            BIGSERIAL PRIMARY KEY,
    journal_code  VARCHAR(2)     NOT NULL,  -- VE AC CA BQ OD
    date_ecriture DATE           NOT NULL,
    reference     VARCHAR(50)    NOT NULL,
    libelle       VARCHAR(200),
    compte_debit  VARCHAR(10)    NOT NULL,
    compte_credit VARCHAR(10)    NOT NULL,
    montant       NUMERIC(15,2)  NOT NULL,
    source_type   VARCHAR(20),              -- SALE COMMANDE MVT_CAISSE MANUEL
    source_id     BIGINT,
    exercice      INTEGER        NOT NULL,
    periode       INTEGER        NOT NULL
);
```

**Écritures automatiques :**

| Événement | Journal | Débit | Crédit |
|---|---|---|---|
| Vente comptant | VE | 571 Caisse | 701 Ventes |
| Vente tiers payant | VE | 411 Créances TP | 701 Ventes |
| Vente différé | VE | 411 Créances clients | 701 Ventes |
| Réception commande | AC | 321 Stock | 401 Fournisseurs |
| Mouvement caisse | CA | Variable | 571 Caisse |

---

## 7. Architecture Angular du module Finances

```
src/main/webapp/app/features/finances/
├── finances.routes.ts
├── data-access/
│   ├── models/
│   │   ├── fournisseur-ap.model.ts        ← P1
│   │   ├── declaration-tva.model.ts       ← P0 (nouveau)
│   │   ├── remise-rfa.model.ts            ← P2
│   │   └── index.ts
│   └── services/
│       ├── fournisseur-ap-api.service.ts  ← P1
│       ├── declaration-tva-api.service.ts ← P0 (nouveau)
│       └── remise-rfa-api.service.ts      ← P2
└── feature/
    ├── finances-layout/                   ← sidebar 5 menus
    ├── declaration-tva/                   ← P0 — nouveau composant TVA
    ├── comptes-fournisseurs/              ← P1 — AP soldes/dettes
    └── remises-rfa/                       ← P2 — remises fournisseurs
```

> `jhi-taxe-report` dans `mvt-caisse` reste **inchangé**.
> `app-declaration-tva` est un composant indépendant — pas de couplage avec l'existant.
> Les autres modules (`reports/*`, `mvt-caisse`) restent à leur place — **zéro duplication**.

---

## 8. Routing

```typescript
// features/finances/finances.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/finances-layout/finances-layout.component')
        .then(m => m.FinancesLayoutComponent),
    data: { pageTitle: 'Finances', abilitySubject: 'finances' },
    canActivate: [AuthGuard],
  },
];
export default routes;
```

```typescript
// Ajout dans entity.routes.ts
{
  path: 'finances',
  loadChildren: () =>
    import('../features/finances/finances.routes').then(m => m.default),
  canActivate: [AuthGuard],
  data: { abilitySubject: 'finances' },
}
```

> `AuthGuard` remplace `UserRouteAccessService` — guard unifié qui gère authentification
> + ROLE_ADMIN bypass + ABAC strict via `abilitySubject`.
> Pas d'`authorities[]` dans les données — le contrôle d'accès passe par `NavItemRole`.

---

## 9. Permissions AbilityService

```typescript
// Clés de permission — préfixe "finances."
'finances.dashboard'               // P0
'finances.comptes-fournisseurs'    // P1
'finances.performance-fournisseurs'// P0 (existant)
'finances.remises-rfa'             // P2
'finances.rentabilite'             // P0 (existant)
'finances.comparatif'              // P0 (existant)
'finances.top-produits'            // P0 (existant)
'finances.stock-valorisation'      // P0 (existant)
'finances.stock-rotation'          // P0 (existant)
'finances.declaration-tva'         // P0 (enrichi)
'finances.export'                  // P2
```

---

## 10. Récapitulatif final

| Menu dans `finances-layout` | Composant | Phase | Effort | Impact existant |
|---|---|---|---|---|
| Dashboard Financier Global | `jhi-dashboard-ca` enrichi (+2 KPI) | P0 | 1j | minimal |
| Comptes fournisseurs AP | `app-comptes-fournisseurs` nouveau | P1 | 3-4j | aucun |
| Déclaration TVA | `app-declaration-tva` nouveau | P0 | 1-2j | **aucun** — `jhi-taxe-report` inchangé |
| Remises & RFA | `app-remises-rfa` nouveau | P2 | 3j | aucun |
| Export expert-comptable | `app-export-comptable` nouveau | P2 | 2j | aucun |

**Total : 5 menus. 4 nouveaux composants créés from scratch. 1 enrichissement léger (dashboard).**

### `jhi-taxe-report` et `app-declaration-tva` — coexistence temporaire

| | `jhi-taxe-report` | `app-declaration-tva` |
|---|---|---|
| Où | `mvt-caisse` sidebar | `finances` sidebar |
| TVA déductible | ❌ | ✅ |
| Décision future | garder ou supprimer | garder ou supprimer |

### Modules existants — non touchés

| Module | Route | Action |
|---|---|---|
| `reports/sales` | `/reports/sales` | rien |
| `reports/stock` | `/reports/stock` | rien |
| `reports/partners` | `/reports/partners` | rien |
| `mvt-caisse` | `/mvt-caisse` | rien (sauf +2 KPI dans dashboard-ca) |

---

*Document créé le 05/05/2026 — Plan final v3 (TVA = nouveau composant isolé)*

