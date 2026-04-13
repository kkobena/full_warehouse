# Plan de Priorité — Refonte Dashboard Pharmacien

> **Basé sur :** `ANALYSE-DASHBOARD-PHARMACIEN.md`
> **Périmètre :** `home/` · `entities/reports/services/` · backend Java
> **Date :** Avril 2026

---

## Vue d'ensemble — Dashboard cible

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│  🔴 Péremptions(5)  🔴 Ruptures(12)  🟡 À commander(8)  🔵 Ajust.(3)           │
│  [● Auj.]  [Semaine]  [Mois]  [Semestre]  [Année]     🔄 14:32  [↻]            │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────────────────────┤
│ CA NET   │ MARGE    │ PANIER   │ STOCK    │ CRÉANCES │ ACHATS                   │
│ 1 245 k  │ 324 k    │ 8 750    │ 45 300 k │ 2 100 k  │ 23 800 k                 │
│ ▲+8%     │ 26%▲+2pt │ ▲+5%     │ PA/PV    │ 450k>90j │ 82% qualité · 2j délai  │
├──────────┴──────────┴──────────┴──────────┴──────────┴──────────────────────────┤
│  [Vue Tabulaire ●]  [Vue Graphique]                                              │
├──────────────────────────┬──────────────────────────────────────────────────────┤
│  Modes de règlement      │  Ventes par tiers-payant  [Top 5 ▼]                  │
├──────────────────────────┴──────────────────────────────────────────────────────┤
│  Achats par grossiste  [30j ●][12m]  [Top 5 ▼]  (tableau score + doughnut)     │
├──────────────────────────────────────────────────────────────────────────────────┤
│  Meilleures ventes en qté [Top ▼]    │  Meilleures ventes en valeur [Top ▼]    │
├──────────────────────────────────────────────────────────────────────────────────┤
│  Pareto 20/80 ── [Quantité] [Montant]  (onglets)                                │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## Tableau de bord des priorités

```
Priorité  Libellé                              Durée   Backend  Risque
────────  ───────────────────────────────────  ──────  ───────  ──────
🔴 P0     4 corrections de bugs critiques      < 1h    ❌ Non   ⚡ Zéro
🟠 P1     KPI manquants (services existants)   4-6h    ❌ Non   ⚡ Zéro
🟠 P2     Refactorisation navigation période   4-6h    ❌ Non   🟡 Faible
🟡 P3     Panel achats fournisseurs            2-3h    ❌ Non   ⚡ Zéro
🟡 P4     UX — ergonomie et états              3-4h    ❌ Non   ⚡ Zéro
─────────────────────────────────────────────────────────────────────
          TOTAL P0 → P4  (zéro backend)        ~2 j    ❌ Non
─────────────────────────────────────────────────────────────────────
🟢 P5     Stats par groupe grossiste           ~1 j    ✅ Oui   🟡 Faible
🔵 P6     Courbe évolution CA + DashboardCA    1-2 j   ❌ Non   🟡 Faible
⚪ P7     Refonte UX avancée                   1-2 m   🔧 Part. 🟡 Moyen
```

---

## 🔴 P0 — Corrections de bugs critiques (< 1h — risque zéro)

> **Faire AVANT tout autre changement.** Aucun risque de régression.

### Bug 1 — Pareto 20/80 : la section "montant" affiche les données "quantité"

**Fichier :** `home-base.component.ts`

```typescript
// Dans build2080Chart() — AJOUTER après la construction de twentyEightyChartData :
this.twentyEightyMontantChartData = {
  labels: this.row20x80Montant.map(p => p.libelle.slice(0, 20)),
  datasets: [
    {
      type: 'line',
      label: '% Montant cumulé',
      data: this.row20x80Montant.map(p => p.pourcentage),
      borderColor: 'rgba(234,88,12,1)',
      backgroundColor: 'rgba(234,88,12,0.08)',
      yAxisID: 'yPercent',
    },
    {
      type: 'bar',
      label: 'Montant',
      data: this.row20x80Montant.map(p => p.total),
      backgroundColor: 'rgba(234,88,12,0.7)',
      yAxisID: 'yValue',
    },
  ],
};
```

**Fichier :** `home-base.component.html` — section Pareto montant

```html
<!-- Remplacer [data]="twentyEightyChartData" par : -->
<p-chart type="bar" [data]="twentyEightyMontantChartData"
  [options]="twentyEightyChartOptions" height="400px" />
```

---

### Bug 2 — HalfyearlyDataComponent charge les données **annuelles**

**Fichier :** `halfyearly-data.component.ts` ligne 32

```typescript
// AVANT (faux)
this.dashboardPeriode = CaPeriodeFilter.yearly;
// APRÈS
this.dashboardPeriode = CaPeriodeFilter.halfyearly;
```

---

### Bug 3 — WeeklyDataComponent redéclare une propriété héritée

**Fichier :** `weekly-data.component.ts` — supprimer la ligne 29

```typescript
// SUPPRIMER cette ligne :
protected dashboardPeriode: CaPeriodeFilter | null = null;
```

---

### Bug 4 — GrapheDailyComponent charge les données **annuelles**

**Fichier :** `home-graphe/daily/graphe-daily.component.ts` ligne 28

```typescript
// AVANT (faux)
protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.yearly;
// APRÈS
protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.daily;
```

---

## 🟠 P1 — KPI manquants via services reports existants (4-6h — zéro backend)

> Le backend est **déjà implémenté**. Il s'agit uniquement de brancher les appels Angular.

### Services à connecter

| KPI absent du dashboard | Service Angular | Méthode |
|------------------------|-----------------|---------|
| Marge brute + taux % | `MargeReportService` | `getMargeSummary()` |
| Panier moyen + évolution N-1 | `DashboardCAService` | `getOverallSummary()` |
| Stock valorisé (PA / PV) | `StockValuationReportService` | `getStockValuationSummary()` |
| Créances tiers-payants aging | `TiersPayantReportService` | `getCreancesSummary()` |

### 1.1 — `home-base.component.ts` — imports + champs + forkJoin

```typescript
// Ajouter les imports :
import { MargeReportService }          from 'app/entities/reports/services/marge-report.service';
import { DashboardCAService }          from 'app/entities/reports/services/dashboard-ca.service';
import { StockValuationReportService } from 'app/entities/reports/services/stock-valuation-report.service';
import { TiersPayantReportService }    from 'app/entities/reports/services/tiers-payant-report.service';
import { IMargeSummary }               from 'app/shared/model/report/marge.model';
import { IDashboardCASummary }         from 'app/shared/model/report';
import { IStockValuationSummary }      from 'app/shared/model/report/stock-valuation.model';
import { ITiersPayantCreancesSummary } from 'app/shared/model/report/tiers-payant-report.model';

// Injecter :
private readonly margeReportService          = inject(MargeReportService);
private readonly dashboardCAService          = inject(DashboardCAService);
private readonly stockValuationReportService = inject(StockValuationReportService);
private readonly tiersPayantReportService    = inject(TiersPayantReportService);

// Nouveaux champs :
protected margeSummary:         IMargeSummary | null = null;
protected caSummary:            IDashboardCASummary | null = null;
protected stockValuationSummary: IStockValuationSummary | null = null;
protected creancesSummary:      ITiersPayantCreancesSummary[] = [];
protected totalCreances         = 0;
protected creancesPlusDe90j     = 0;
protected isLoading             = signal(false);
protected lastUpdate            = signal<Date | null>(null);

// Dans le forkJoin de loadDashboardData() — ajouter :
margeSummary:    this.margeReportService.getMargeSummary(),
caSummary:       this.dashboardCAService.getOverallSummary(),
stockValuation:  this.stockValuationReportService.getStockValuationSummary(),
creancesSummary: this.tiersPayantReportService.getCreancesSummary(),

// Dans le handler next — ajouter :
this.margeSummary          = data.margeSummary.body;
this.caSummary             = data.caSummary.body;
this.stockValuationSummary = data.stockValuation.body;
this.creancesSummary       = data.creancesSummary.body ?? [];
this.totalCreances         = this.creancesSummary.reduce((s, c) => s + (c.montantTotal ?? 0), 0);
this.creancesPlusDe90j     = this.creancesSummary.reduce((s, c) => s + (c.montantPlusDe90Jours ?? 0), 0);
this.isLoading.set(false);
this.lastUpdate.set(new Date());
```

### 1.2 — `home-base.component.html` — 5 KPI Cards

```html
<!-- Remplacer les 4 cards actuelles par 5 cards (col-xl-2 chacune) -->
<div class="row g-2 mb-2">

  <!-- Card 1 : CA Net + variation N-1 + panier moyen -->
  <div class="col-xl-2 col-md-6 mb-2">
    <div class="card h-100 kpi-card primary-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <p class="kpi-label">Chiffre d'affaires net</p>
            <h4 class="kpi-value"><span>{{ venteRecord?.netAmount | number }}</span></h4>
            @if (caSummary?.caWeekEvolutionPct !== undefined) {
              <div class="kpi-variation"
                [class.kpi-up]="(caSummary?.caWeekEvolutionPct ?? 0) >= 0"
                [class.kpi-down]="(caSummary?.caWeekEvolutionPct ?? 0) < 0">
                <i [class]="(caSummary?.caWeekEvolutionPct ?? 0) >= 0
                  ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"></i>
                {{ caSummary?.caWeekEvolutionPct | number:'1.1-1' }}% vs préc.
              </div>
            }
            <div class="kpi-badges mt-1">
              <span class="badge bg-primary-subtle text-primary">
                {{ caSummary?.panierMoyenToday | number }} panier moy.
              </span>
              <span class="badge bg-info-subtle text-info">
                {{ venteRecord?.saleCount | number }} ventes
              </span>
            </div>
          </div>
          <div class="kpi-icon text-primary"><i class="pi pi-shopping-cart"></i></div>
        </div>
      </div>
      <div class="card-footer">
        <span class="footer-label">Remises</span>
        <span class="badge bg-warning-subtle text-warning">
          {{ venteRecord?.discountAmount | number }}
        </span>
      </div>
    </div>
  </div>

  <!-- Card 2 : Marge brute -->
  <div class="col-xl-2 col-md-6 mb-2">
    <div class="card h-100 kpi-card success-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <p class="kpi-label">Marge brute</p>
            <h4 class="kpi-value"><span>{{ margeSummary?.margeBruteGlobale | number }}</span></h4>
            <div class="kpi-badges mt-1">
              <span class="badge bg-success-subtle text-success">
                {{ margeSummary?.tauxMargeMoyen | number:'1.1-1' }}% taux
              </span>
              <span class="badge bg-warning-subtle text-warning"
                pTooltip="Produits dont le taux de marge est insuffisant" tooltipPosition="top">
                {{ margeSummary?.nbProduitsMargeInsuffisante }} faible marge
              </span>
            </div>
          </div>
          <div class="kpi-icon text-success"><i class="pi pi-percentage"></i></div>
        </div>
      </div>
      <div class="card-footer">
        <span class="footer-label">Coût d'achat</span>
        <span class="badge bg-danger-subtle text-danger">
          {{ margeSummary?.coutAchatGlobal | number }}
        </span>
      </div>
    </div>
  </div>

  <!-- Card 3 : Stock valorisé -->
  <div class="col-xl-2 col-md-6 mb-2">
    <div class="card h-100 kpi-card warning-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <p class="kpi-label">Stock valorisé</p>
            <h4 class="kpi-value">
              <span>{{ stockValuationSummary?.totalPurchaseValue | number }}</span>
              <span class="value-suffix">PA</span>
            </h4>
            <div class="kpi-badges mt-1">
              <span class="badge bg-success-subtle text-success">
                {{ stockValuationSummary?.totalSalesValue | number }} PV
              </span>
              <span class="badge bg-info-subtle text-info">
                {{ stockValuationSummary?.averageMarginPercentage | number:'1.1-1' }}% marge pot.
              </span>
            </div>
          </div>
          <div class="kpi-icon text-warning"><i class="pi pi-box"></i></div>
        </div>
      </div>
      <div class="card-footer">
        <span class="footer-label">{{ stockValuationSummary?.totalProducts | number }} réf.</span>
        <span class="badge bg-warning-subtle text-warning">
          {{ stockValuationSummary?.totalQuantity | number }} unités
        </span>
      </div>
    </div>
  </div>

  <!-- Card 4 : Créances tiers-payants -->
  <div class="col-xl-2 col-md-6 mb-2">
    <div class="card h-100 kpi-card danger-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <p class="kpi-label">Créances TP</p>
            <h4 class="kpi-value"><span>{{ totalCreances | number }}</span></h4>
            <div class="kpi-badges mt-1">
              <span class="badge bg-danger-subtle text-danger"
                pTooltip="Créances de plus de 90 jours — risque élevé" tooltipPosition="top">
                {{ creancesPlusDe90j | number }} > 90j
              </span>
              <span class="badge bg-secondary-subtle text-secondary">
                {{ creancesSummary.length }} assureurs
              </span>
            </div>
          </div>
          <div class="kpi-icon text-danger"><i class="pi pi-credit-card"></i></div>
        </div>
      </div>
      <div class="card-footer">
        <span class="footer-label">Achats mois</span>
        <span class="badge bg-warning-subtle text-warning">
          {{ achatRecord?.receiptAmount | number }}
        </span>
      </div>
    </div>
  </div>

  <!-- Card 5 : Achats fournisseurs -->
  <div class="col-xl-2 col-md-6 mb-2">
    <div class="card h-100 kpi-card warning-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <p class="kpi-label">Achats fournisseurs</p>
            <h4 class="kpi-value">
              <span>{{ achatRecord?.receiptAmount | number }}</span>
              <span class="value-suffix">TTC</span>
            </h4>
            <div class="kpi-badges mt-1">
              <span class="badge bg-success-subtle text-success">
                {{ achatRecord?.netAmount | number }} HT
              </span>
              <span class="badge bg-info-subtle text-info">
                {{ achatRecord?.achatCount | number }} cdes
              </span>
            </div>
          </div>
          <div class="kpi-icon text-warning"><i class="pi pi-truck"></i></div>
        </div>
      </div>
      <div class="card-footer">
        <span class="footer-label">TVA</span>
        <span class="badge bg-info-subtle text-info">{{ achatRecord?.taxAmount | number }}</span>
      </div>
    </div>
  </div>

</div>
```

### 1.3 — `dashboard-common.scss` — variation N-1

```scss
// ─── KPI Variation N-1 ────────────────────────────────────────────
.kpi-variation {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 0.25rem;
  padding: 0.15rem 0.5rem;
  border-radius: 12px;

  &.kpi-up   { color: $success-green; background: rgba($success-green, 0.1); }
  &.kpi-down { color: $danger-red;    background: rgba($danger-red, 0.1);    }

  i { font-size: 0.65rem; }
}
```

---

## 🟠 P2 — Refactorisation navigation temporelle (4-6h — risque faible)

> Supprimer 5 fichiers inutiles, libérer 2 colonnes, activer le ToggleStateService.

### 2.1 — `home-base.component.ts` — ajouter le sélecteur de période

```typescript
// Interface et options de période
interface PeriodOption {
  label: string;
  value: CaPeriodeFilter;
  icon: string;
}

protected readonly periodeOptions: PeriodOption[] = [
  { label: "Auj.",     value: CaPeriodeFilter.daily,      icon: 'pi pi-sun'          },
  { label: 'Semaine',  value: CaPeriodeFilter.weekly,     icon: 'pi pi-calendar'     },
  { label: 'Mois',     value: CaPeriodeFilter.monthly,    icon: 'pi pi-calendar-plus'},
  { label: 'Semestre', value: CaPeriodeFilter.halfyearly, icon: 'pi pi-chart-bar'    },
  { label: 'Année',    value: CaPeriodeFilter.yearly,     icon: 'pi pi-chart-line'   },
];

protected activePeriode = signal<CaPeriodeFilter>(CaPeriodeFilter.daily);

protected onPeriodeChange(p: CaPeriodeFilter): void {
  this.activePeriode.set(p);
  this.dashboardPeriode = p;
  // Activer le ToggleStateService (était commenté dans tous les héritiers) :
  this.showGraphs = this.toggleStateService.toggleState();
  this.loadDashboardData();
}
```

### 2.2 — `home-base.component.html` — header unifié

```html
<!-- Remplacer la row actuelle du header par : -->
<div class="row mb-2">
  <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">

    <!-- Zone gauche : alertes inchangées -->
    <div class="quick-actions d-flex gap-2 align-items-center flex-wrap">
      @if (peremptionCount > 0) { ... }
      @if (ruptureCount > 0) { ... }
      @if (urgentCount > 0) { ... }
      @if (ajustementCount > 0) { ... }
      @if (prixModifCount > 0) { ... }
    </div>

    <!-- Zone centre : sélecteur période (remplace la sidebar col-lg-2) -->
    <div class="dashboard-periode-selector">
      @for (opt of periodeOptions; track opt.value) {
        <button type="button" class="periode-pill"
          [class.active]="activePeriode() === opt.value"
          (click)="onPeriodeChange(opt.value)"
          [pTooltip]="opt.label" tooltipPosition="bottom">
          <i [class]="opt.icon"></i>
          <span class="pill-label">{{ opt.label }}</span>
        </button>
      }
    </div>

    <!-- Zone droite : horodatage + refresh + toggle graphique -->
    <div class="d-flex align-items-center gap-2">
      @if (lastUpdate()) {
        <span class="last-update-label">
          <i class="pi pi-clock"></i> {{ lastUpdate() | date:'HH:mm' }}
        </span>
      }
      <p-button icon="pi pi-refresh" [text]="true" severity="secondary" size="small"
        [loading]="isLoading()"
        (onClick)="loadDashboardData()"
        pTooltip="Actualiser maintenant" tooltipPosition="left" />
      <!-- Segment tabulaire / graphique inchangé -->
      <div class="custom-segment-control"> ... </div>
    </div>

  </div>
</div>
```

### 2.3 — `home.component.html` — supprimer la sidebar

```html
<!-- Remplacer toute la structure col-lg-2 + col-lg-10 par : -->
@if (isAdmin()) {
  <div class="container-fluid">
    <jhi-home-base></jhi-home-base>
  </div>
}
```

### 2.4 — `dashboard-common.scss` — styles du sélecteur de période

```scss
// ─── Period Pill Selector ─────────────────────────────────────────
.dashboard-periode-selector {
  display: inline-flex;
  align-items: center;
  gap: 0.2rem;
  padding: 3px;
  background: rgba($gray-border, 0.4);
  border-radius: 12px;
  flex-wrap: wrap;

  .periode-pill {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    padding: 0.28rem 0.7rem;
    border: none;
    border-radius: 9px;
    background: transparent;
    color: $text-secondary;
    font-size: 0.78rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;

    i { font-size: 0.75rem; }
    .pill-label { font-size: 0.78rem; }

    &:hover:not(.active) {
      background: rgba($primary-blue, 0.08);
      color: $primary-blue;
    }

    &.active {
      background: linear-gradient(135deg, $primary-blue 0%, $primary-light 100%);
      color: #fff;
      font-weight: 600;
      box-shadow: 0 2px 8px rgba($primary-blue, 0.25);
    }
  }
}

// ─── Last update label ────────────────────────────────────────────
.last-update-label {
  font-size: 0.75rem;
  color: $text-secondary;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  white-space: nowrap;
  i { font-size: 0.7rem; }
}

@media (max-width: 576px) {
  .dashboard-periode-selector .periode-pill .pill-label { display: none; }
}
```

### 2.5 — Fichiers à supprimer

```
src/main/webapp/app/home/daily/daily-data/daily-data.component.ts       (35 lignes)
src/main/webapp/app/home/weekly/weekly-data/weekly-data.component.ts     (37 lignes)
src/main/webapp/app/home/monthly/monthly-data/monthly-data.component.ts  (35 lignes)
src/main/webapp/app/home/halfyearly/halfyearly-data/halfyearly-data.component.ts (36 lignes)
src/main/webapp/app/home/yearly/yearly-data/yearly-data.component.ts     (36 lignes)
```

Retirer de `home.component.ts` : imports + tableau `imports[]` de ces 5 composants.

### Bilan P2

| | Avant | Après |
|--|-------|-------|
| Fichiers .ts | 6 | 1 |
| Lignes dupliquées | 179 | 0 |
| Colonnes écran | 10/12 | 12/12 (+16%) |
| ToggleStateService | ❌ Code mort | ✅ Actif |

---

## 🟡 P3 — Panel achats fournisseurs (2-3h — zéro backend)

> Utiliser `SupplierPerformanceReportService` existant (`api/supplier-performance`).

### 3.1 — `home-base.component.ts`

```typescript
import { SupplierPerformanceReportService } from 'app/entities/reports/services/supplier-performance-report.service';
import { ISupplierPerformance, ISupplierPerformanceSummary } from 'app/shared/model/report/supplier-performance.model';

private readonly supplierService = inject(SupplierPerformanceReportService);

protected topFournisseurs:    ISupplierPerformance[] = [];
protected supplierSummary:    ISupplierPerformanceSummary | null = null;
protected fournisseurPeriod:  '30d' | '12m' = '30d';
protected TOP_MAX_FOURNISSEUR = { label: 'Top 5', value: 5 };
protected fournisseurChartData: any;
protected fournisseurChartOptions: any;

// Ajouter au forkJoin :
topFournisseurs: this.supplierService.getTopSuppliersByVolume(this.TOP_MAX_FOURNISSEUR.value),
supplierSummary: this.supplierService.getSupplierPerformanceSummary(),

// Dans next handler :
this.topFournisseurs = data.topFournisseurs.body ?? [];
this.supplierSummary = data.supplierSummary.body;
this.buildFournisseurChart();

protected onTopFournisseurChange(): void {
  this.buildFournisseurChart();
}

private buildFournisseurChart(): void {
  const items = this.topFournisseurs.slice(0, this.TOP_MAX_FOURNISSEUR.value);
  const amounts = this.fournisseurPeriod === '30d'
    ? items.map(f => f.purchaseAmountLast30Days ?? 0)
    : items.map(f => f.purchaseAmountLast12Months ?? 0);
  this.fournisseurChartData = {
    labels: items.map(f => f.fournisseurName?.slice(0, 18) ?? ''),
    datasets: [{
      data: amounts,
      backgroundColor: ['#008cba', '#5bc0de', '#43ac6a', '#e99002', '#f04124'],
      borderWidth: 2,
    }],
  };
  this.fournisseurChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right', labels: { boxWidth: 12, font: { size: 10 } } } },
  };
}
```

### 3.2 — `home-base.component.html` — panneau "Achats par fournisseur"

```html
<div class="col-xl-6 col-md-6 mb-2">
  <div class="card h-100 data-card">
    <div class="card-header">
      <h5 class="card-title">
        <i class="pi pi-truck text-warning"></i>
        Achats par fournisseur
      </h5>
      <div class="d-flex align-items-center gap-2">
        <div class="dashboard-periode-selector">
          <button type="button" class="periode-pill"
            [class.active]="fournisseurPeriod === '30d'"
            (click)="fournisseurPeriod = '30d'; buildFournisseurChart()">30 j</button>
          <button type="button" class="periode-pill"
            [class.active]="fournisseurPeriod === '12m'"
            (click)="fournisseurPeriod = '12m'; buildFournisseurChart()">12 mois</button>
        </div>
        <p-select (onChange)="onTopFournisseurChange()"
          [(ngModel)]="TOP_MAX_FOURNISSEUR"
          [options]="tops" optionLabel="label" />
      </div>
    </div>
    <div class="card-body py-4">
      @if (!showGraphs) {
        <ul class="list-group list-group-flush">
          @for (f of topFournisseurs; track f.fournisseurId; let i = $index) {
            <li class="list-group-item d-flex justify-content-between align-items-center px-0">
              <div class="d-flex align-items-center gap-2">
                @if (i === 0) { <span class="rank-gold">#1</span> }
                @else if (i === 1) { <span class="rank-silver">#2</span> }
                @else if (i === 2) { <span class="rank-bronze">#3</span> }
                @else { <span class="text-muted small">{{ i + 1 }}</span> }
                <div>
                  <div class="fw-semibold">{{ f.fournisseurName }}</div>
                  <small class="text-muted">
                    Score :
                    <span [class.text-success]="(f.performanceScore ?? 0) >= 70"
                          [class.text-warning]="(f.performanceScore ?? 0) >= 50 && (f.performanceScore ?? 0) < 70"
                          [class.text-danger]="(f.performanceScore ?? 0) < 50">
                      {{ f.performanceScore | number:'1.0-0' }}
                    </span>
                    · {{ f.avgDeliveryDays }}j délai
                  </small>
                </div>
              </div>
              <span class="fw-semibold text-warning fs-6">
                {{ (fournisseurPeriod === '30d'
                  ? f.purchaseAmountLast30Days
                  : f.purchaseAmountLast12Months) | number }}
              </span>
            </li>
          }
        </ul>
      } @else {
        <div class="d-flex justify-content-center align-items-center h-100">
          <p-chart type="doughnut" [data]="fournisseurChartData"
            [options]="fournisseurChartOptions" height="300" />
        </div>
      }
    </div>
    @if (supplierSummary) {
      <div class="card-footer d-flex justify-content-between">
        <span class="footer-label">
          Qualité : <strong>{{ supplierSummary.avgConformityRate | number:'1.0-1' }}%</strong>
        </span>
        <span class="badge bg-info-subtle text-info">
          {{ supplierSummary.avgDeliveryDays | number:'1.0-1' }}j délai moy.
        </span>
      </div>
    }
  </div>
</div>
```

### 3.3 — `dashboard-common.scss` — rangs visuels

```scss
// ─── Rangs top 3 ─────────────────────────────────────────────────
.rank-gold   { color: #f59e0b; font-weight: 700; font-size: 0.8rem; min-width: 22px; }
.rank-silver { color: #6b7280; font-weight: 700; font-size: 0.8rem; min-width: 22px; }
.rank-bronze { color: #b45309; font-weight: 700; font-size: 0.8rem; min-width: 22px; }
```

---

## 🟡 P4 — UX — Ergonomie et états (3-4h — risque zéro)

### 4.1 — Rang `#` dans les tableaux Top N (meilleures ventes)

Dans `home-base.component.html` — pour chaque `p-table` des top ventes :

```html
<!-- Dans ng-template #header — ajouter en première colonne : -->
<th style="width:40px">#</th>

<!-- Dans ng-template #body — ajouter en première colonne : -->
<td>
  @if ($index === 0) { <span class="rank-gold">#1</span> }
  @else if ($index === 1) { <span class="rank-silver">#2</span> }
  @else if ($index === 2) { <span class="rank-bronze">#3</span> }
  @else { <span class="text-muted small">{{ $index + 1 }}</span> }
</td>
```

### 4.2 — Skeleton loaders pendant le chargement

```typescript
// Ajouter dans le tableau imports[] du @Component :
SkeletonModule  // from 'primeng/skeleton'
```

```html
<!-- Entourer les 5 KPI cards avec : -->
@if (isLoading()) {
  <div class="row g-2 mb-2">
    @for (_ of [1,2,3,4,5]; track _) {
      <div class="col-xl-2 col-md-6 mb-2">
        <div class="card" style="height:140px; padding:1rem">
          <p-skeleton height="0.8rem" width="60%" styleClass="mb-2" />
          <p-skeleton height="1.8rem" width="75%" styleClass="mb-2" />
          <p-skeleton height="0.8rem" width="50%" />
        </div>
      </div>
    }
  </div>
} @else {
  <!-- cards normales -->
}
```

### 4.3 — Fusion des 2 sections Pareto en onglets

```typescript
// Ajouter dans HomeBaseComponent :
protected activePareto: 'qty' | 'amt' = 'qty';
```

```html
<!-- Remplacer les 2 col-lg-6 Pareto séparées par un seul col-12 : -->
<div class="row">
  <div class="col-12 mb-2">
    <div class="card data-card">
      <div class="card-header">
        <h5 class="card-title">
          <i class="pi pi-chart-line text-info"></i>
          Analyse Pareto 20/80
        </h5>
        <div class="dashboard-periode-selector">
          <button type="button" class="periode-pill"
            [class.active]="activePareto === 'qty'"
            (click)="activePareto = 'qty'">
            <i class="pi pi-sort-amount-down"></i> Quantité
          </button>
          <button type="button" class="periode-pill"
            [class.active]="activePareto === 'amt'"
            (click)="activePareto = 'amt'">
            <i class="pi pi-dollar"></i> Montant
          </button>
        </div>
      </div>
      <div class="card-body p-0">
        @if (activePareto === 'qty') {
          <!-- Contenu actuel section Pareto Quantité (inchangé) -->
        } @else {
          <!-- Contenu actuel section Pareto Montant (inchangé) -->
        }
      </div>
    </div>
  </div>
</div>
```

### 4.4 — États vides explicites sur chaque tableau

```html
<!-- Dans chaque ng-template #emptymessage : -->
<ng-template #emptymessage>
  <tr>
    <td [attr.colspan]="..." class="pharma-empty-message">
      <div class="pharma-empty-content">
        <i class="pi pi-inbox" style="font-size:2rem; color:var(--p-text-color-secondary)"></i>
        <p class="mt-2 text-muted mb-0">Aucune donnée sur la période sélectionnée</p>
      </div>
    </td>
  </tr>
</ng-template>
```

---

## 🟢 P5 — Stats achats par groupe grossiste (~1 jour — backend requis)

> 1 migration SQL + 2 DTOs Java + 1 service + 2 endpoints + 1 modèle Angular.

### 5.1 — Migration Flyway `V1.5.0__mv_groupe_fournisseur_stats.sql`

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS warehouse.mv_groupe_fournisseur_stats AS
SELECT
    gf.id           AS groupe_id,
    gf.libelle      AS groupe_name,
    COUNT(DISTINCT f.id) AS nb_fournisseurs,
    COALESCE(SUM(CASE WHEN r.receipt_date >= CURRENT_DATE - INTERVAL '30 days'
        THEN r.net_amount ELSE 0 END), 0)    AS purchase_amount_last_30_days,
    COUNT(DISTINCT CASE WHEN r.receipt_date >= CURRENT_DATE - INTERVAL '30 days'
        THEN r.id END)                       AS nb_orders_last_30_days,
    COALESCE(SUM(CASE WHEN r.receipt_date >= CURRENT_DATE - INTERVAL '12 months'
        THEN r.net_amount ELSE 0 END), 0)    AS purchase_amount_last_12_months,
    COUNT(DISTINCT CASE WHEN r.receipt_date >= CURRENT_DATE - INTERVAL '12 months'
        THEN r.id END)                       AS nb_orders_last_12_months,
    AVG(sp.avg_delivery_days)                AS avg_delivery_days,
    AVG(sp.conformity_rate_pct)              AS avg_conformity_rate,
    AVG(sp.performance_score)                AS avg_performance_score
FROM warehouse.groupe_fournisseur gf
LEFT JOIN warehouse.fournisseur f ON f.groupe_fournisseur_id = gf.id
LEFT JOIN warehouse.reception r
    ON r.fournisseur_id = f.id
   AND r.receipt_statut IN ('RECEIVED', 'CLOSED')
LEFT JOIN warehouse.mv_supplier_performance sp ON sp.fournisseur_id = f.id
GROUP BY gf.id, gf.libelle
ORDER BY purchase_amount_last_12_months DESC;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_groupe_fournisseur_stats_id
    ON warehouse.mv_groupe_fournisseur_stats (groupe_id);
```

### 5.2 — DTOs Java

```java
// GroupeFournisseurStatsDTO.java
public record GroupeFournisseurStatsDTO(
    Integer groupeId, String groupeName, Integer nbFournisseurs,
    Long purchaseAmountLast30Days,   Integer nbOrdersLast30Days,
    Long purchaseAmountLast12Months, Integer nbOrdersLast12Months,
    BigDecimal avgDeliveryDays, BigDecimal avgConformityRate, BigDecimal avgPerformanceScore
) {}

// GroupeFournisseurStatsSummaryDTO.java
public record GroupeFournisseurStatsSummaryDTO(
    Integer totalGroupes, Integer totalFournisseurs,
    Long totalPurchaseLast30Days,   Integer totalOrdersLast30Days,
    Long totalPurchaseLast12Months, Integer totalOrdersLast12Months,
    BigDecimal avgDeliveryDays, BigDecimal avgConformityRate
) {}
```

### 5.3 — Endpoints REST (ajouter dans `SupplierPerformanceReportResource.java`)

```java
@GetMapping("/supplier-performance/by-group")
public ResponseEntity<List<GroupeFournisseurStatsDTO>> getTopGroupesByVolume(
        @RequestParam(defaultValue = "10") Integer limit) {
    return ResponseEntity.ok(groupeFournisseurStatsService.getTopGroupesByVolume(limit));
}

@GetMapping("/supplier-performance/by-group/summary")
public ResponseEntity<GroupeFournisseurStatsSummaryDTO> getGroupeSummary() {
    return ResponseEntity.ok(groupeFournisseurStatsService.getSummary());
}
```

### 5.4 — Modèle + service Angular

```typescript
// groupe-fournisseur-stats.model.ts
export interface IGroupeFournisseurStats {
  groupeId?: number;    groupeName?: string;    nbFournisseurs?: number;
  purchaseAmountLast30Days?: number;   nbOrdersLast30Days?: number;
  purchaseAmountLast12Months?: number; nbOrdersLast12Months?: number;
  avgDeliveryDays?: number; avgConformityRate?: number; avgPerformanceScore?: number;
}

// Dans SupplierPerformanceReportService — ajouter :
getTopGroupesByVolume(limit = 5): Observable<HttpResponse<IGroupeFournisseurStats[]>> {
  return this.http.get<IGroupeFournisseurStats[]>(
    `${this.resourceUrl}/by-group`,
    { params: new HttpParams().set('limit', limit), observe: 'response' }
  );
}
```

### 5.5 — `dashboard-common.scss` — barre de score visuelle

```scss
// ─── Score bar ────────────────────────────────────────────────────
.score-bar-container {
  display: flex; align-items: center; gap: 0.4rem; min-width: 80px;

  .score-bar {
    height: 6px; border-radius: 3px; flex: 1;
    transition: width 0.4s ease;
    &.score-good { background: $success-green; }
    &.score-avg  { background: $warning-orange; }
    &.score-bad  { background: $danger-red; }
  }
  .score-label { font-size: 0.7rem; font-weight: 600; min-width: 22px; }
}
```

---

## 🔵 P6 — Courbe évolution CA + lien DashboardCAComponent (1-2 jours — 0 backend)

> `DashboardCAService.getEvolutionData()` existe. `DashboardCAComponent` (523 lignes)
> est implémenté mais accessible uniquement via `/reports/dashboard-ca`.

### 6.1 — Nouvelle section courbe dans le dashboard

```typescript
// Ajouter au forkJoin :
caEvolution: this.dashboardCAService.getEvolutionData('daily', startDate, endDate),

// Champs :
protected caEvolution: IDashboardCAEvolution | null = null;
protected evolutionChartData: any;

private buildEvolutionChart(): void {
  if (!this.caEvolution?.labels) return;
  this.evolutionChartData = {
    labels: this.caEvolution.labels,
    datasets: [
      {
        label: 'CA Période actuelle',
        data: this.caEvolution.caValues,
        borderColor: '#008cba',
        backgroundColor: 'rgba(0,140,186,0.08)',
        fill: true, tension: 0.3,
      },
      {
        label: 'CA Période précédente',
        data: this.caEvolution.caPreviousValues,   // N-1
        borderColor: '#e99002',
        borderDash: [5, 5],
        fill: false, tension: 0.3,
      },
    ],
  };
}
```

### 6.2 — Lien rapide vers DashboardCAComponent

```html
<!-- Dans le header du dashboard, à droite du bouton refresh : -->
<a routerLink="/reports/dashboard-ca" class="btn btn-sm btn-outline-info ms-1"
  pTooltip="Tableau de bord CA avancé avec exports" tooltipPosition="left">
  <i class="pi pi-external-link"></i>
</a>
```

---

## ⚪ P7 — Refonte UX avancée (1-2 mois — à planifier)

| Action | Effort | Bénéfice |
|--------|--------|---------|
| Unifier les icônes FontAwesome → PrimeIcons | Faible | Cohérence visuelle |
| Animation `pulse` sur les alertes critiques | Faible | Attention utilisateur |
| Widget "Tâches du jour" | Moyen | Vision opérationnelle |
| Taux de service Rx (nouveau domaine) | Élevé | Conformité officine |
| Export PDF dashboard 1 clic | Moyen | Partage / reporting |
| URL bookmarkable `?periode=monthly` | Faible | Navigation directe |
| Dashboard personnalisable (drag & drop) | Très élevé | Différenciation |

---

## Résumé final — Ce que l'utilisateur gagne

| Avant | Après P0→P4 | Après P5→P6 |
|-------|-------------|-------------|
| 4 KPI (CA, Net, Type, Achats) | 5 KPI avec N-1 | + courbe évolution |
| Marge absente | Marge brute + taux % | Stats par grossiste |
| Panier moyen absent | Panier moyen affiché | — |
| Stock valorisé absent | Card PA/PV + marge pot. | — |
| Créances TP absentes | Card avec aging >90j | — |
| Sidebar 16% écran perdu | Pill inline plein écran | — |
| 5 héritiers inutiles | Supprimés (179 lignes) | — |
| Toggle reset à chaque onglet | Persisté (ToggleService actif) | — |
| Pareto : 2 sections identiques | 1 panneau onglets Qté/Montant | — |
| Top N sans rang | #1 🥇 #2 🥈 #3 🥉 | — |
| Chargement sans feedback | Skeleton loaders | — |
| Pas de fournisseurs | Top fournisseurs score/délai | Top grossistes groupé |
| DashboardCA inaccessible | Lien "Avancé →" | Intégré |

