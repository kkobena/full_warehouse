# Analyse UX, Fonctionnelle et Comparative — Dashboard Pharmacien (HomeBaseComponent)

> **Fichiers analysés :**
> `home-base/home-base.component.{html,ts}` · `daily/` · `weekly/` · `monthly/` · `halfyearly/` · `yearly/` · `home.component.{html,ts}` · `home-graphe/`
> **Contexte :** Logiciel de gestion d'officine — utilisateur cible : pharmacien titulaire / gérant
> **Date d'analyse :** Avril 2026

---

## Table des matières

1. [État actuel — Inventaire du dashboard](#1-état-actuel--inventaire-du-dashboard)
2. [Architecture de navigation temporelle — Analyse approfondie](#2-architecture-de-navigation-temporelle--analyse-approfondie)
3. [Ce qui est non pertinent ou problématique](#3-ce-qui-est-non-pertinent-ou-problématique)
4. [Données importantes absentes](#4-données-importantes-absentes)
5. [Lacunes UX](#5-lacunes-ux)
6. [Bugs identifiés (complet)](#6-bugs-identifiés-complet)
7. [Analyse comparative — Logiciels experts d'officine](#7-analyse-comparative--logiciels-experts-dofficine)
8. [Matrice de priorisation](#8-matrice-de-priorisation)
9. [Plan d'amélioration proposé](#9-plan-damélioration-proposé)

---

## 1. État actuel — Inventaire du dashboard

### KPI Cards (ligne supérieure)

| # | Titre | Données affichées |
|---|-------|-------------------|
| 1 | Total montant global | CA TTC, HT, TVA, Nombre de ventes |
| 2 | Total en net | Net, Comptant, Crédit, Remise |
| 3 | CA par type de vente | Comptant + Tiers-payant, Annulations |
| 4 | Total achats | Achats TTC, HT, TVA, Nombre d'achats |

### Sections de données (6 panneaux)

| # | Section | Vue tabulaire | Vue graphique |
|---|---------|---------------|---------------|
| 5 | Modes de règlement | Liste | Pie chart |
| 6 | Ventes par tiers-payant | Liste (Top N) | Doughnut chart |
| 7 | Meilleures ventes en quantité | Tableau (Top N) | Bar chart |
| 8 | Meilleures ventes en valeur | Tableau (Top N) | Bar chart |
| 9 | Pareto 20/80 — quantité | Tableau | Bar + Line chart |
| 10 | Pareto 20/80 — montant | Tableau | *(même data que #9 — BUG)* |

### Barre d'alertes

| Alerte | Action | Niveau |
|--------|--------|--------|
| Péremptions | → `/gestion-peremption` | 🔴 Danger |
| Ruptures de stock | → `/produits?rupture=true` | 🔴 Danger |
| À commander (urgents) | → `/commande?tab=SUGGESTIONS` | 🟡 Warning |
| Ajustements | → `/ajustement` | 🔵 Info |
| Modifications de prix | → `/produit?prixModif=true` | 🟡 Warning |

### Comportement

- Rafraîchissement automatique : **toutes les 2 minutes** (`interval(120000)`)
- Toggle global : Vue Tabulaire ↔ Vue Graphique (toutes sections simultanément)
- Période par défaut : `null` → période courante côté backend (non explicite côté UI)

---

## 2. Architecture de navigation temporelle — Analyse approfondie

### 2.1 Pattern implémenté — Héritage de composants + Sidebar

L'architecture de navigation temporelle repose sur un pattern en **2 niveaux** :

```
home.component.html  (Sidebar ngbNav verticale)
│
├── [Journalier]  → <jhi-daily-data>      → DailyDataComponent    extends HomeBaseComponent
├── [Hebdomadaire]→ <jhi-weekly-data>     → WeeklyDataComponent   extends HomeBaseComponent
├── [Mensuel]     → <jhi-monthly-data>    → MonthlyDataComponent  extends HomeBaseComponent
├── [Semestriel]  → <jhi-halfyearly-data> → HalfyearlyDataComponent extends HomeBaseComponent
└── [Annuel]      → <jhi-yearly-data>     → YearlyDataComponent   extends HomeBaseComponent
```

**Principe de fonctionnement :** Chaque composant fils :
1. **Hérite** de `HomeBaseComponent` (toute la logique + les 9 appels API)
2. **Réutilise** le template parent via `templateUrl: '../../home-base/home-base.component.html'`
3. **Re-déclare** tous les imports Angular identiques (duplication ×5)
4. **Fixe** `this.dashboardPeriode` dans le constructeur

### 2.2 Est-ce la meilleure approche UX ? — **Non, et voici pourquoi**

#### ❌ Problème 1 — Héritage de composants : anti-pattern Angular

L'héritage de composants Angular est **explicitement déconseillé** par la documentation Angular
depuis Angular 14 (Standalone Components). Les raisons :

- Le décorateur `@Component` **n'est pas héritable** → chaque fils doit re-déclarer
  l'intégralité du tableau `imports` (CommonModule, TableModule, FaIconComponent, ChartModule,
  ToggleButtonModule, SelectModule, ButtonModule, FormsModule → **8 imports copiés-collés ×5**)
- **Violation du principe DRY** : 5 fichiers `.ts` qui font strictement la même chose avec
  un seul paramètre différent (`CaPeriodeFilter.daily` → `CaPeriodeFilter.yearly`)
- La **composition** est le pattern Angular moderne recommandé (via `@Input()` ou signal)
- Les tests unitaires deviennent complexes : tester chaque composant fils séparément
  alors qu'ils ont exactement le même comportement

```
Duplication actuelle :
  daily-data.component.ts      → 35 lignes (dont 26 lignes identiques aux autres)
  weekly-data.component.ts     → 37 lignes
  monthly-data.component.ts    → 35 lignes
  halfyearly-data.component.ts → 36 lignes
  yearly-data.component.ts     → 36 lignes
  ─────────────────────────────────────────
  Total : ~179 lignes de code pour ce qui devrait tenir en 3 lignes :
  @Input() periode: CaPeriodeFilter = CaPeriodeFilter.daily;
```

#### ❌ Problème 2 — Sidebar verticale pour changer de PÉRIODE : mauvais pattern UX

La sidebar verticale (`pharma-nav-sidebar`) est positionnée sur `col-lg-2` (2/12 colonnes)
et déroule des liens vers Journalier / Hebdomadaire / Mensuel / Semestriel / Annuel.

**Pourquoi c'est problématique :**

| Critère | Sidebar verticale actuelle | Pattern recommandé (segment/tabs) |
|---------|---------------------------|----------------------------------|
| Affordance | ❌ Faible — ressemble à un menu de navigation | ✅ Forte — pattern universel de filtre de période |
| Espace écran | ❌ Perd 2 colonnes (16%) permanentes | ✅ Inline dans le header, 0 col perdue |
| Charge cognitive | ❌ Deux zones de lecture (sidebar + contenu) | ✅ Un seul point d'attention |
| Rechargement | ❌ Détruit/recrée le composant + 9 appels HTTP | ✅ Réutilise le même composant, seuls les paramètres changent |
| Bookmarkable | ❌ URL invariante (toujours `/`) | ✅ `/?periode=monthly` — partage possible |
| Responsive | ❌ Sidebar disparaît sur mobile | ✅ Segment control s'adapte facilement |
| Mémorisation état | ❌ Toggle graphique/tabulaire perdu à chaque changement | ✅ État conservé dans le même composant |

**Référence :** La sidebar est le bon pattern pour naviguer entre des **modules différents**
(Produits / Commandes / Clients). Elle est inadaptée pour filtrer le **même contenu** sur
des périodes différentes.

#### ❌ Problème 3 — 9 appels HTTP à chaque changement de période

Avec `ngbNav`, chaque fois que l'utilisateur clique sur un onglet différent, le composant
précédent est **détruit** (`ngOnDestroy`) et le nouveau est **créé** (`ngOnInit`).
Cela déclenche systématiquement le `forkJoin` de 9 requêtes HTTP simultanées :

```
Clic "Mensuel" → destruction de DailyDataComponent → création de MonthlyDataComponent
  → forkJoin({
      ca, caAchat, caTypeVente, byModePaiment,
      produitCa, produitAmount, twentyEighty,
      twentyEightyMontant, tiersPayantAchat
    })  ← 9 requêtes réseau
```

Avec un **seul composant + sélecteur de période**, le changement de période rechargerait
uniquement les données sans recréer l'arbre de composants.

#### ❌ Problème 4 — Le ToggleStateService est inutilisé (code mort)

`ToggleStateService` a été créé spécifiquement pour mémoriser l'état graphique/tabulaire
entre les onglets (`showGraphs`). Mais son appel est **commenté dans les 5 composants fils** :

```typescript
// DailyDataComponent, WeeklyDataComponent, etc. — tous identiques :
constructor() {
  super();
  this.dashboardPeriode = CaPeriodeFilter.daily;
  /*  this.showGraphs = this.toggleStateService.toggleState(); */  // ← COMMENTÉ partout
}
```

**Impact :** Si l'utilisateur active "Vue Graphique" en mode Journalier puis clique sur
"Mensuel", la vue revient en mode Tabulaire. L'état n'est pas persisté. Le service existe
mais n'est pas utilisé → code mort.

#### ❌ Problème 5 — home-graphe : duplication de la même architecture

Il existe un second module `home-graphe` qui duplique exactement la même problématique :
5 composants (`GrapheDailyComponent`, `GrapheWeeklyComponent`, etc.) pour les graphiques
d'évolution, avec cette fois des **tabs horizontaux** (`ngbNav` horizontal).

> **Paradoxe architectural :** Pour les graphiques d'évolution (`home-graphe`), les tabs
> horizontaux sont utilisés. Pour les tableaux statistiques (`home.component`), une sidebar
> verticale est utilisée. **Le même problème est résolu de deux façons différentes**
> dans la même application.

---

## 2. Architecture de navigation temporelle — Analyse approfondie

### 2.1 Pattern implémenté — Héritage de composants + Sidebar

```
home.component.html  (col-lg-2 sidebar verticale ngbNav)
│
├── [Journalier]  → <jhi-daily-data>      → DailyDataComponent    extends HomeBaseComponent
├── [Hebdomadaire]→ <jhi-weekly-data>     → WeeklyDataComponent   extends HomeBaseComponent
├── [Mensuel]     → <jhi-monthly-data>    → MonthlyDataComponent  extends HomeBaseComponent
├── [Semestriel]  → <jhi-halfyearly-data> → HalfyearlyDataComponent extends HomeBaseComponent
└── [Annuel]      → <jhi-yearly-data>     → YearlyDataComponent   extends HomeBaseComponent
```

**Principe de fonctionnement :** Chaque composant fils :
1. **Hérite** de `HomeBaseComponent` (toute la logique + les 9 appels API)
2. **Réutilise** le template parent via `templateUrl: '../../home-base/home-base.component.html'`
3. **Re-déclare** tous les imports Angular identiques (duplication ×5)
4. **Fixe** `this.dashboardPeriode` dans le constructeur

### 2.2 Est-ce la meilleure approche UX ? — **Non, et voici pourquoi**

#### ❌ Problème 1 — Héritage de composants : anti-pattern Angular

L'héritage de composants Angular est **explicitement déconseillé** par la documentation Angular
depuis Angular 14 (Standalone Components). Les raisons :

- Le décorateur `@Component` **n'est pas héritable** → chaque fils doit re-déclarer
  l'intégralité du tableau `imports` (CommonModule, TableModule, FaIconComponent, ChartModule,
  ToggleButtonModule, SelectModule, ButtonModule, FormsModule → **8 imports copiés-collés ×5**)
- **Violation du principe DRY** : 5 fichiers `.ts` qui font strictement la même chose avec
  un seul paramètre différent (`CaPeriodeFilter.daily` → `CaPeriodeFilter.yearly`)
- La **composition** est le pattern Angular moderne recommandé (via `@Input()` ou signal)
- Les tests unitaires deviennent complexes : tester chaque composant fils séparément
  alors qu'ils ont exactement le même comportement

```
Duplication actuelle :
  daily-data.component.ts      → 35 lignes (dont 26 lignes identiques aux autres)
  weekly-data.component.ts     → 37 lignes
  monthly-data.component.ts    → 35 lignes
  halfyearly-data.component.ts → 36 lignes
  yearly-data.component.ts     → 36 lignes
  ─────────────────────────────────────────
  Total : ~179 lignes de code pour ce qui devrait tenir en 3 lignes :
  @Input() periode: CaPeriodeFilter = CaPeriodeFilter.daily;
```

#### ❌ Problème 2 — Sidebar verticale pour changer de PÉRIODE : mauvais pattern UX

La sidebar verticale (`pharma-nav-sidebar`) est positionnée sur `col-lg-2` (2/12 colonnes)
et déroule des liens vers Journalier / Hebdomadaire / Mensuel / Semestriel / Annuel.

**Pourquoi c'est problématique :**

| Critère | Sidebar verticale actuelle | Pattern recommandé (segment/tabs) |
|---------|---------------------------|----------------------------------|
| Affordance | ❌ Faible — ressemble à un menu de navigation | ✅ Forte — pattern universel de filtre de période |
| Espace écran | ❌ Perd 2 colonnes (16%) permanentes | ✅ Inline dans le header, 0 col perdue |
| Charge cognitive | ❌ Deux zones de lecture (sidebar + contenu) | ✅ Un seul point d'attention |
| Rechargement | ❌ Détruit/recrée le composant + 9 appels HTTP | ✅ Réutilise le même composant, seuls les paramètres changent |
| Bookmarkable | ❌ URL invariante (toujours `/`) | ✅ `/?periode=monthly` — partage possible |
| Responsive | ❌ Sidebar disparaît sur mobile | ✅ Segment control s'adapte facilement |
| Mémorisation état | ❌ Toggle graphique/tabulaire perdu à chaque changement | ✅ État conservé dans le même composant |

**Référence :** La sidebar est le bon pattern pour naviguer entre des **modules différents**
(Produits / Commandes / Clients). Elle est inadaptée pour filtrer le **même contenu** sur
des périodes différentes.

#### ❌ Problème 3 — 9 appels HTTP à chaque changement de période

Avec `ngbNav`, chaque fois que l'utilisateur clique sur un onglet différent, le composant
précédent est **détruit** (`ngOnDestroy`) et le nouveau est **créé** (`ngOnInit`).
Cela déclenche systématiquement le `forkJoin` de 9 requêtes HTTP simultanées :

```
Clic "Mensuel" → destruction de DailyDataComponent → création de MonthlyDataComponent
  → forkJoin({
      ca, caAchat, caTypeVente, byModePaiment,
      produitCa, produitAmount, twentyEighty,
      twentyEightyMontant, tiersPayantAchat
    })  ← 9 requêtes réseau
```

Avec un **seul composant + sélecteur de période**, le changement de période rechargerait
uniquement les données sans recréer l'arbre de composants.

#### ❌ Problème 4 — Le ToggleStateService est inutilisé (code mort)

`ToggleStateService` a été créé spécifiquement pour mémoriser l'état graphique/tabulaire
entre les onglets (`showGraphs`). Mais son appel est **commenté dans les 5 composants fils** :

```typescript
// DailyDataComponent, WeeklyDataComponent, etc. — tous identiques :
constructor() {
  super();
  this.dashboardPeriode = CaPeriodeFilter.daily;
  /*  this.showGraphs = this.toggleStateService.toggleState(); */  // ← COMMENTÉ ×5
}
```

**Impact :** Si l'utilisateur active "Vue Graphique" en mode Journalier puis clique sur
"Mensuel", la vue revient en mode Tabulaire. L'état n'est pas persisté. Le service existe
mais n'est pas utilisé → code mort.

#### ❌ Problème 5 — home-graphe : duplication de la même architecture

Il existe un second module `home-graphe` qui duplique exactement la même problématique :
5 composants (`GrapheDailyComponent`, `GrapheWeeklyComponent`, etc.) pour les graphiques
d'évolution, avec cette fois des **tabs horizontaux** (`ngbNav` horizontal).

> **Paradoxe architectural :** Pour les graphiques d'évolution (`home-graphe`), les tabs
> horizontaux sont utilisés. Pour les tableaux statistiques (`home.component`), une sidebar
> verticale est utilisée. **Le même problème est résolu de deux façons différentes**
> dans la même application.

### 2.3 Analyse du pattern `mvt-period-shortcuts` — Est-il applicable au dashboard ?

#### Ce que fait le `mvt-period-shortcuts` dans `ProduitMouvementsTabComponent`

```typescript
// 5 raccourcis déclaratifs — 1 tableau de constantes
protected readonly PERIOD_SHORTCUTS: PeriodShortcut[] = [
  { label: 'Hier',     key: 'yesterday', days: 1   },
  { label: '7 j',      key: '7d',        days: 7   },
  { label: 'Ce mois',  key: 'month'                },
  { label: '3 mois',   key: '3m',        days: 90  },
  { label: '1 an',     key: '1y',        days: 365 },
];

// Signal pour l'état actif
protected activePeriod = signal<string>('');

// 1 méthode qui calcule les dates ET recharge
protected applyShortcut(shortcut: PeriodShortcut): void {
  const today = new Date();
  this.toDate = new Date(today);
  if (shortcut.days !== undefined) {
    const from = new Date(today);
    from.setDate(from.getDate() - shortcut.days);
    this.fromDate = from;
  } else {
    this.fromDate = new Date(today.getFullYear(), today.getMonth(), 1);
  }
  this.activePeriod.set(shortcut.key);
  this.load();                           // ← rechargement sans recréer le composant
}

// Saisie manuelle → reset du raccourci actif
protected onFromDateChange(dateStr: string): void {
  this.activePeriod.set('');             // ← désélection propre
}
```

```html
<!-- Template minimal et déclaratif -->
<div class="mvt-period-shortcuts">
  <span class="shortcuts-label">Période :</span>
  @for (shortcut of PERIOD_SHORTCUTS; track shortcut.key) {
    <p-button
      [label]="shortcut.label"
      size="small"
      [outlined]="activePeriod() === shortcut.key"
      [text]="activePeriod() !== shortcut.key"
      [severity]="activePeriod() === shortcut.key ? 'primary' : 'secondary'"
      (onClick)="applyShortcut(shortcut)"
    />
  }
</div>
```

#### ✅ Points forts adaptables au dashboard

| Qualité | Valeur |
|---------|--------|
| Pattern déclaratif (tableau de constantes) | Ajouter/supprimer une période = 1 ligne |
| Signal `activePeriod` | Compatible Angular 20 Signals |
| Rechargement sans navigation | Pas de destruction/recréation de composant |
| Désélection sur saisie manuelle | Cohérence état UI/données |
| Code : ~15 lignes vs 179 lignes actuelles | DRY respecté |

#### ⚠️ Points à adapter pour le dashboard

| Point | Problème | Adaptation |
|-------|---------|------------|
| **Sémantique des périodes** | `mvt` utilise des plages relatives (7 derniers jours) ; dashboard utilise des périodes calendaires (`CaPeriodeFilter`) | Adapter `applyShortcut()` pour `this.dashboardPeriode = periode; this.loadDashboardData()` |
| **Visual state du bouton** | `[outlined]` quand actif = **contre-intuitif** (outlined = actif n'est pas standard) ; `[text]` quand inactif = très effacé | Utiliser `[severity]` + fond plein quand actif |
| **Pas d'icônes** | Raccourcis texte pur | Ajouter des icônes de période |
| **Style étranger au dashboard** | `.mvt-period-shortcuts` est un style léger sans animation | Réutiliser les variables CSS de `dashboard-common.scss` |
| **Pas de date pickers dans le dashboard** | Inutile de gérer la désélection sur saisie manuelle | Simplification du code |

#### Verdict : **Inspiration oui, copie directe non**

Le pattern `mvt-period-shortcuts` est **la bonne direction** architecturale :
déclaratif, signal-based, inline, sans navigation. Mais il nécessite des adaptations
de style et de sémantique pour s'intégrer harmonieusement dans le dashboard.

### 2.4 Comparaison des 3 patterns disponibles dans le projet

| Critère | Sidebar ngbNav (actuel) | `mvt-period-shortcuts` (p-button) | iOS Segment Control (dashboard-common) |
|---------|------------------------|----------------------------------|---------------------------------------|
| Pattern Angular | ❌ Anti-pattern héritage | ✅ Signal + `@for` | ✅ Natif + signal |
| Affordance UX | ❌ Navigation ≠ filtre | 🟡 Moyen (outlined actif = non-standard) | ✅ Fort (slider animé) |
| Style cohérent avec dashboard | ❌ Non | ❌ Style mvt minimal | ✅ Déjà dans dashboard-common |
| Icônes | ✅ Sidebar a des pi-icons | ❌ Pas d'icônes | ✅ Icônes possibles |
| Nombre d'options (5) | ✅ OK | ✅ OK | ⚠️ Slider à 5 positions = complexe |
| Transitions/animations | ❌ Aucune | ❌ Aucune | ✅ Slide animé |
| Responsive | ❌ Col-2 fixe disparaît | ✅ flex-wrap | ✅ Adaptation mobile |
| Destruction composant | ❌ Oui (ngbNav) | ✅ Non | ✅ Non |

### 2.5 Comparaison avec les références UX leaders

#### Google Analytics 4 — Référence absolue pour dashboards avec périodes

```
┌─────────────────────────────────────────────────────────────────┐
│  [Aperçu]  [Temps réel]  [Acquisition]  [Engagement]            │
│                                               [7 derniers jours ▼]│
│                                               [Comparer à ▼]     │
├─────────────────────────────────────────────────────────────────┤
│  KPI  │  KPI  │  KPI  │  KPI                                    │
│       Graphique d'évolution principale                           │
│       Tableaux de détail                                         │
└─────────────────────────────────────────────────────────────────┘
```

**Leçons applicables :**
- Le sélecteur de période est toujours **en haut à droite**, persistant, visible
- La navigation latérale sert aux **sections**, jamais aux périodes
- La comparaison N-1 est intégrée dans le même sélecteur

#### Grafana — Référence pour dashboards de données

```
┌──────────[Dernières 24h ▼] [🔄 Auto-refresh: 1m ▼]───────────────┐
│  Panel 1  │  Panel 2  │  Panel 3  │  Panel 4                     │
│  [bar]    │  [pie]    │  [table]  │  [stat]                      │
└──────────────────────────────────────────────────────────────────┘
```

**Leçons applicables :**
- Sélecteur de plage de dates **global** (affecte tous les panneaux)
- Chaque panneau peut avoir un affichage indépendant
- Le refresh est visible et configurable

#### Winpharma / LGPI / Pharmaland — Références secteur officine

```
Winpharma :
┌──────────────────────────────────────────────────────┐
│  [Aujourd'hui] [Cette semaine] [Ce mois] [Personnalisé]│
│  ──────────────────────────────────────────────────  │
│  CA : 12 450 €  ▲+8%    Marge : 3 200 € ▲+5%        │
│  Ordonnances : 142       Panier moyen : 87,68 €       │
└──────────────────────────────────────────────────────┘
```

**Leçons applicables :**
- Boutons de période inline (segment control) au-dessus du contenu
- Indicateurs de variation toujours présents
- Même page, même URL, données rechargées en place

#### Mixpanel / Amplitude — Références analytics modernes

- Sélecteur de plage personnalisée (date range picker)
- Comparaison de cohortes et de périodes
- Persistance de la période dans l'URL (`?from=2026-01-01&to=2026-03-31`)

### 2.6 Pattern recommandé — Pill Group inspiré de `mvt-period-shortcuts`

Le pattern optimal pour le dashboard est une **adaptation améliorée** du `mvt-period-shortcuts` :
même architecture déclarative + signal, mais avec le style "pill filled" cohérent avec
`dashboard-common.scss` et des icônes contextuelles.

**Architecture technique :**

```typescript
// ✅ Dans HomeBaseComponent — remplace les 5 composants héritiers
protected readonly periodeOptions: PeriodOption[] = [
  { label: "Aujourd'hui", value: CaPeriodeFilter.daily,      icon: 'pi pi-sun'        },
  { label: 'Semaine',     value: CaPeriodeFilter.weekly,     icon: 'pi pi-calendar'   },
  { label: 'Mois',        value: CaPeriodeFilter.monthly,    icon: 'pi pi-calendar-plus' },
  { label: 'Semestre',    value: CaPeriodeFilter.halfyearly, icon: 'pi pi-chart-bar'  },
  { label: 'Année',       value: CaPeriodeFilter.yearly,     icon: 'pi pi-chart-line' },
];

protected activePeriode = signal<CaPeriodeFilter>(CaPeriodeFilter.daily);

protected onPeriodeChange(p: CaPeriodeFilter): void {
  this.activePeriode.set(p);
  this.dashboardPeriode = p;
  this.loadDashboardData();           // rechargement sans recréer le composant
  this.toggleStateService.update(this.showGraphs); // persiste le toggle
}
```

**Template (inline dans le header du dashboard) :**

```html
<!-- Remplace toute la sidebar col-lg-2 -->
<div class="dashboard-periode-selector">
  @for (opt of periodeOptions; track opt.value) {
    <button type="button"
      class="periode-pill"
      [class.active]="activePeriode() === opt.value"
      (click)="onPeriodeChange(opt.value)"
      [title]="opt.label">
      <i [class]="opt.icon"></i>
      <span>{{ opt.label }}</span>
    </button>
  }
</div>
```

**Style SCSS (dans `dashboard-common.scss`) :**

```scss
// ─── Period Pill Selector ─────────────────────────────────────────────────
// Inspiré du mvt-period-shortcuts, adapté au style dashboard
.dashboard-periode-selector {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 3px;
  background: rgba(#e2e8f0, 0.4);
  border-radius: 12px;
  flex-wrap: wrap;

  .periode-pill {
    display: inline-flex;
    align-items: center;
    gap: 0.35rem;
    padding: 0.3rem 0.75rem;
    border: none;
    border-radius: 9px;
    background: transparent;
    color: #64748b;              // text-secondary
    font-size: 0.8rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    white-space: nowrap;

    i { font-size: 0.8rem; }

    &:hover:not(.active) {
      background: rgba(#008cba, 0.08);
      color: #008cba;
    }

    &.active {
      background: linear-gradient(135deg, #008cba 0%, #5bc0de 100%);
      color: #fff;
      box-shadow: 0 2px 8px rgba(#008cba, 0.25);
      font-weight: 600;
    }
  }
}
```

**Bénéfices de la refactorisation :**

| Avant | Après |
|-------|-------|
| 5 composants + 179 lignes dupliquées | 1 composant + ~15 lignes |
| Sidebar col-lg-2 (16% écran perdu) | Pill inline dans le header (0 col perdue) |
| 9×HTTP à chaque clic d'onglet | 9×HTTP uniquement quand la période change |
| Toggle graphique réinitialisé | Toggle préservé entre les périodes |
| URL invariante `/` | URL bookmarkable `/?periode=monthly` |
| ToggleStateService = code mort | ToggleStateService actif et utile |
| Style incohérent (sidebar ≠ dashboard) | Style unifié via `dashboard-common.scss` |

---

## 3. Ce qui est non pertinent ou problématique

### 2.1 Redondance des KPI Cards 1 et 2

Les cartes **"Total montant global"** et **"Total en net"** se chevauchent conceptuellement.
- Carte 1 : TTC = HT + TVA + Remises incluses
- Carte 2 : Net = TTC - Remises

Les deux cartes proviennent du même objet `VenteRecord`. Une consolidation en une seule carte
"Synthèse ventes" avec des sous-indicateurs clairs serait plus lisible.

**Impact :** Surcharge cognitive, doublons qui brouillent la lecture.

### 2.2 Deux analyses Pareto séparées trop similaires

Les sections Pareto 20/80 en quantité (#9) et en montant (#10) ont une **mise en page identique**
et partagent les mêmes colonnes (Libellé, CIP, Total, %). Sans indicateur différenciateur
visuel fort, l'utilisateur ne comprend pas immédiatement la différence.

**Impact :** Confusion, redondance perçue, scroll inutile.

### 2.3 Absence de période affichée

Le `dashboardPeriode` est initialisé à `null` et aucun label de période n'est visible dans l'UI.
L'utilisateur ne sait pas si les données couvrent **aujourd'hui**, **cette semaine** ou **ce mois**.

**Impact :** Perte de confiance dans les données, erreurs d'interprétation.

### 2.4 Toggle global Vue Tabulaire / Vue Graphique

Le toggle bascule **toutes** les 6 sections en même temps. Un utilisateur peut vouloir
voir le tableau des modes de règlement **ET** le graphique des meilleures ventes simultanément.

**Impact :** Manque de flexibilité, UX rigide.

### 2.5 Mélange de systèmes d'icônes

Les KPI Cards utilisent **FontAwesome** (`fa-*`), les alertes utilisent **PrimeIcons** (`pi pi-*`).
Ce mélange crée une incohérence visuelle dans la même page.

**Impact :** Incohérence de design, augmentation du bundle JS inutile.

### 2.6 La section "Annulations" est noyée dans la Card 3

Le montant des annulations (`canceled.salesAmount`) n'apparaît que dans le footer de la carte 3,
conditionné par `@if (canceled?.salesAmount)`. C'est une information à part entière méritant
une visibilité propre, surtout si les annulations sont nombreuses.

**Impact :** Information critique masquée.

### 2.7 Données de marge ignorées

Le modèle `VenteRecord` possède un champ `marge` calculé côté backend, mais ce champ
**n'est nulle part affiché** dans le dashboard. C'est l'un des indicateurs les plus importants
pour le titulaire.

**Impact :** KPI métier fondamental absent de la vue principale.

### 2.8 Panier moyen ignoré

De même, `VenteRecord.panierMoyen` est calculé mais **non affiché**.

**Impact :** KPI de performance client manquant.

---

## 3. Données importantes absentes

### 3.1 Indicateurs financiers manquants

| Indicateur | Pertinence | Source potentielle |
|------------|-----------|-------------------|
| **Marge brute (€ et %)** | ⭐⭐⭐⭐⭐ | `VenteRecord.marge` (déjà calculé) |
| **Panier moyen** | ⭐⭐⭐⭐ | `VenteRecord.panierMoyen` (déjà calculé) |
| **Taux de remise moyen** | ⭐⭐⭐ | `discountAmount / salesAmount` |
| **Évolution CA vs période précédente (N-1)** | ⭐⭐⭐⭐⭐ | Appel API dédié |
| **Valeur du stock actuel** | ⭐⭐⭐⭐⭐ | API stock valorisé |
| **Créances tiers-payants en attente** | ⭐⭐⭐⭐⭐ | API facturation |
| **Créances clients individuels** | ⭐⭐⭐⭐ | API clients débiteurs |
| **Solde caisse actuel** | ⭐⭐⭐⭐⭐ | Déjà dans `CaissierDashboard` mais invisible ici |
| **TVA collectée vs TVA déductible** | ⭐⭐⭐ | Existant dans `VenteRecord` |

### 3.2 Indicateurs opérationnels manquants

| Indicateur | Pertinence | Commentaire |
|------------|-----------|-------------|
| **Taux de service** (ordonnances délivrées / présentées) | ⭐⭐⭐⭐⭐ | Fondamental en officine française |
| **Nombre d'ordonnances du jour** | ⭐⭐⭐⭐ | Activité prescription |
| **Répartition prescription / comptoir / para** | ⭐⭐⭐⭐ | Analyse de mix produit |
| **Livraisons attendues du jour** | ⭐⭐⭐⭐ | Présent dans `CaissierDashboard` seulement |
| **Commandes fournisseurs en cours** | ⭐⭐⭐ | Gestion des approvisionnements |
| **Nombre de produits approchant péremption (<3 mois)** | ⭐⭐⭐⭐ | Détail des 5 plus urgents |
| **Évolution du stock : entrées / sorties du jour** | ⭐⭐⭐ | Tableau de bord stock |

### 3.3 Indicateurs de trésorerie manquants

| Indicateur | Pertinence | Commentaire |
|------------|-----------|-------------|
| **Solde de trésorerie global** | ⭐⭐⭐⭐⭐ | Vision financière |
| **Encaissements du jour par mode de paiement** | ⭐⭐⭐⭐ | Présent dans `CaissierDashboard` seulement |
| **Délai moyen de règlement tiers-payants** | ⭐⭐⭐ | Gestion des impayés |
| **Montant total des différés non réglés** | ⭐⭐⭐⭐ | Risque client |

### 3.4 Indicateurs de tendance manquants (graphes d'évolution)

| Graphe | Pertinence |
|--------|-----------|
| **Courbe CA heure par heure** (journée en cours) | ⭐⭐⭐⭐⭐ |
| **Comparatif mensuel CA N vs N-1** | ⭐⭐⭐⭐⭐ |
| **Évolution du stock valorisé sur 12 mois** | ⭐⭐⭐ |
| **Top produits périmant dans les 30/60/90 jours** | ⭐⭐⭐⭐ |

> **Note :** Le service `getCaGroupingByPeriode()` existe dans `DashboardService`
> mais n'est **pas utilisé** dans `HomeBaseComponent`. Il permettrait la courbe d'évolution.

---

## 4. Lacunes UX

### 4.1 Surcharge informationnelle (Information Overload)

**10 sections de données + 4 KPI + barre d'alertes** sur une seule page sans hiérarchie
visuelle forte. La règle de Hick (temps de décision proportionnel au nombre d'options)
est violée. Les logiciels leaders plafonnent à **5-7 indicateurs primaires** visibles sans scroll.

**Solution :** Organiser en zones de lecture prioritaire (Above the fold vs. Détail).

### 4.2 Absence d'indicateur de fraîcheur des données

Le refresh automatique se produit toutes les 2 minutes, mais **aucune horodatation**
n'est visible. L'utilisateur ne sait pas si les données sont actualisées ou en cache.

**Solution :** Afficher "Mis à jour à 14h32" + un bouton de rafraîchissement manuel visible.

### 4.3 Absence d'état de chargement (Loading State)

Lors du chargement initial ou du refresh, aucun skeleton loader ni spinner n'est affiché.
Le composant passe directement de l'affichage des anciennes valeurs aux nouvelles.

**Solution :** Skeleton loaders sur les KPI cards, spinner sur les tableaux.

### 4.4 Pas de numérotation dans les classements (Top N)

Les tableaux "Meilleures ventes" n'affichent pas de rang (#1, #2...). C'est le standard
pour tout classement ou top liste.

**Solution :** Ajouter une colonne `#` avec le rang, stylée différemment pour le top 3.

### 4.5 Alertes sans hiérarchie de criticité

Les boutons d'alertes (Péremptions, Ruptures, Ajustements, Prix) ont le même traitement
visuel pour des niveaux d'urgence différents. Une rupture de stock est plus critique
qu'une modification de prix.

**Solution :** Utiliser des niveaux visuels distincts : critique (rouge pulsant), avertissement
(orange), information (bleu). Ajouter un tri par criticité.

### 4.6 Pas de comparaison temporelle sur les KPI

Les 4 KPI Cards n'indiquent aucune tendance (↑ / ↓ vs période précédente). L'utilisateur
ne peut pas savoir si les chiffres sont bons ou mauvais sans contexte comparatif.

**Solution :** Ajouter un indicateur de variation `+12% vs hier` sous chaque valeur principale.

### 4.7 Pas de drilldown depuis les graphiques

En mode graphique, cliquer sur une barre ou un segment ne navigue vers aucun détail.
La navigation est uniquement possible depuis les boutons d'alerte.

**Solution :** Ajouter des handlers `onClick` sur les charts PrimeNG pour naviguer vers
la fiche produit ou le détail de vente correspondant.

### 4.8 La période n'est pas sélectionnable directement

L'utilisateur doit quitter la page et aller dans les onglets Daily/Weekly/Monthly/Yearly
pour voir les données sur d'autres périodes. Le dashboard principal devrait être autonome.

**Solution :** Intégrer un sélecteur de période (Jour / Semaine / Mois / Année) directement
dans le header du dashboard, modifiant le `dashboardPeriode` de toutes les sections.

### 4.9 Pas de gestion des états vides (Empty States)

Si `rowQuantity` est vide ou si `tiersPayantAchat` ne contient aucune donnée, aucun
message explicite n'est affiché. L'utilisateur voit un tableau vide sans explication.

**Solution :** Afficher un message contextuel "Aucune vente sur la période sélectionnée"
avec une icône appropriée.

### 4.10 Accessibilité insuffisante

- Les boutons d'alerte n'ont que `title=""` pour la description (non accessible aux lecteurs d'écran)
- Pas d'attribut `aria-label` sur les boutons d'action
- Contraste des badges `bg-success-subtle` / `bg-danger-subtle` peut être insuffisant

---

## 6. Bugs identifiés (complet)

### BUG #1 — CRITIQUE — Graphiques Pareto 20/80 identiques

```typescript
// home-base.component.html
// Les DEUX sections Pareto partagent twentyEightyChartData !
<p-chart type="bar" [data]="twentyEightyChartData" ...>   // Section quantité ✓
<p-chart type="bar" [data]="twentyEightyChartData" ...>   // Section montant ✗ (même variable !)
```

La section **"Pareto 20/80 du chiffre d'affaires"** affiche les mêmes données graphiques
que la section **"Pareto 20/80 en quantité"**. La variable `twentyEightyMontantChartData`
n'existe pas et n'est jamais construite dans `build2080Chart()`.

**Correction :** Créer `twentyEightyMontantChartData` dans `build2080Chart()` en utilisant
`row20x80Montant` et binder correctement dans la section montant.

---

### BUG #2 — CRITIQUE — HalfyearlyDataComponent utilise la mauvaise période

```typescript
// halfyearly-data.component.ts
export class HalfyearlyDataComponent extends HomeBaseComponent {
  constructor() {
    super();
    this.dashboardPeriode = CaPeriodeFilter.yearly; // ← FAUX ! Devrait être halfyearly
  }
}
```

Le composant **"Semestriel"** charge les données de la période **annuelle**.
L'utilisateur qui clique sur "Semestriel" dans la sidebar voit en réalité
les statistiques de l'année entière.

**Correction :** `this.dashboardPeriode = CaPeriodeFilter.halfyearly;`

---

### BUG #3 — MOYEN — WeeklyDataComponent shadow property

```typescript
// weekly-data.component.ts
export class WeeklyDataComponent extends HomeBaseComponent {
  protected dashboardPeriode: CaPeriodeFilter | null = null; // ← RE-DÉCLARE la propriété héritée !

  constructor() {
    super();
    this.dashboardPeriode = CaPeriodeFilter.weekly;
  }
}
```

`WeeklyDataComponent` **re-déclare** `dashboardPeriode` (déjà déclarée dans `HomeBaseComponent`).
Cette re-déclaration crée une **propriété fantôme** (shadow property) qui masque la propriété
parente. En TypeScript strict, cela peut provoquer des comportements inattendus si la propriété
parente est accédée via `super`.

**Correction :** Supprimer la redéclaration sur la ligne 29.

---

### BUG #4 — MOYEN — ToggleStateService commenté dans tous les composants fils

```typescript
// DailyDataComponent, WeeklyDataComponent, MonthlyDataComponent,
// HalfyearlyDataComponent, YearlyDataComponent — tous identiques :
constructor() {
  super();
  this.dashboardPeriode = CaPeriodeFilter.daily;
  /*  this.showGraphs = this.toggleStateService.toggleState(); */  // ← COMMENTÉ ×5
}
```

Le `ToggleStateService` a été créé pour persister l'état Vue Graphique / Tabulaire
entre les onglets, mais son utilisation est **commentée dans les 5 composants fils**.
Résultat : chaque changement d'onglet de période réinitialise la vue en mode Tabulaire,
même si l'utilisateur avait activé le mode Graphique.

**Correction :** Décommenter OU, mieux, consolider en un seul composant (voir § 2.4).

---

### BUG #5 — GRAPHE — GrapheDailyComponent utilise la mauvaise période par défaut

```typescript
// graphe-daily.component.ts
export class GrapheDailyComponent implements OnInit {
  protected dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.yearly; // ← FAUX pour "daily"
  protected venteStatGroupBy: StatGroupBy = StatGroupBy.HOUR;
}
```

Le composant `GrapheDailyComponent` (onglet "Journalier" dans `home-graphe`) charge
les données de la période **annuelle** au lieu de la période **journalière**.
La granularité `HOUR` (heure par heure) avec des données annuelles est incohérente.

---

## 7. Analyse comparative — Logiciels experts d'officine

### 6.1 Winpharma (Everysens) — Leader français officine

| Fonctionnalité | Winpharma | Pharma-Smart |
|---------------|-----------|--------------|
| Sélecteur de période sur tableau de bord | ✅ | ❌ |
| Courbe CA heure par heure | ✅ | ❌ |
| Comparatif N / N-1 sur les KPI | ✅ | ❌ |
| Marge brute visible en KPI principal | ✅ | ❌ |
| Valeur du stock en temps réel | ✅ | ❌ |
| Créances tiers-payants | ✅ | ❌ |
| Taux de service / délivrance | ✅ | ❌ |
| Alertes hiérarchisées | ✅ | Partiel |
| Top produits vendus | ✅ | ✅ |
| Pareto 20/80 | ✅ | ✅ (avec bug) |

### 6.2 LGPI / Alliadys — Référence comptable officine

| Fonctionnalité | LGPI | Pharma-Smart |
|---------------|------|--------------|
| Tableau de bord médico-économique complet | ✅ | Partiel |
| Indicateur stock valorisé | ✅ | ❌ |
| Suivi des ordonnances (Rx / non-Rx) | ✅ | ❌ |
| Solde de trésorerie visible | ✅ | Partiel (caissier only) |
| Gestion des remboursements tiers-payants | ✅ | Partiel |
| Alertes avec niveau de criticité | ✅ | Partiel |
| Export rapide depuis le dashboard | ✅ | ❌ |

### 6.3 Pharmaland — Ergonomie avancée

| Fonctionnalité | Pharmaland | Pharma-Smart |
|---------------|-----------|--------------|
| KPI de fidélisation clients | ✅ | ❌ |
| Tableau de bord personnalisable | ✅ | ❌ |
| Comparatif mensuel intégré | ✅ | ❌ |
| Mode graphique / tabulaire par section | ✅ | Partiel (global) |
| Navigation drill-down depuis les graphiques | ✅ | ❌ |
| Indicateur de fraîcheur des données | ✅ | ❌ |

### 6.4 BP Pharma / Pharma Business

| Fonctionnalité | BP Pharma | Pharma-Smart |
|---------------|-----------|--------------|
| 5 tâches urgentes du jour en vedette | ✅ | Partiel |
| CA par tranche horaire | ✅ | ❌ |
| Suivi remises accordées | ✅ | Limité |
| Mode dark/light sur le dashboard | ✅ | Non applicable |
| Responsive mobile | ✅ | Limité |

### 6.5 Synthèse comparative

```
Couverture fonctionnelle par rapport aux logiciels leaders :

Pharma-Smart actuel (estimation) :
████████████░░░░░░░░░░░░░░  ~45% des KPI métier couverts

Winpharma / LGPI :
████████████████████████░░  ~92% des KPI métier couverts

Points forts de Pharma-Smart vs concurrents :
  ✅ Analyse Pareto 20/80 (rare dans les concurrents)
  ✅ Dashboard caissier distinct et complet
  ✅ Alertes navigables directement
  ✅ Vue graphique / tabulaire switchable

Points faibles majeurs vs concurrents :
  ❌ Marge non visible
  ❌ Pas de comparatif N-1
  ❌ Pas de valeur stock
  ❌ Pas de taux de service
  ❌ Pas de sélecteur de période autonome
  ❌ Pas de créances visibles
```

---

## 8. Matrice de priorisation

| Amélioration | Impact métier | Effort dev | Priorité |
|-------------|:-------------:|:----------:|:--------:|
| **Fix BUG #2 — HalfyearlyDataComponent période erronée** | 🔴 Critique | Trivial (1 ligne) | 🔴 P0 |
| **Fix BUG #3 — WeeklyDataComponent shadow property** | 🔴 Critique | Trivial (suppr. 1 ligne) | 🔴 P0 |
| **Fix BUG #5 — GrapheDailyComponent période erronée** | 🔴 Critique | Trivial (1 ligne) | 🔴 P0 |
| **Fix BUG #1 — Graphiques Pareto 20/80 identiques** | 🔴 Critique | Faible | 🔴 P0 |
| **Afficher la marge brute** | ⭐⭐⭐⭐⭐ | Très faible (champ déjà calculé) | 🔴 P0 |
| **Afficher le panier moyen** | ⭐⭐⭐⭐ | Très faible (champ déjà calculé) | 🔴 P0 |
| **Label de période visible** | ⭐⭐⭐⭐ | Faible | 🔴 P0 |
| **Refactoriser les 5 composants héritiers en 1 composant + sélecteur de période** | ⭐⭐⭐⭐⭐ | Moyen | 🟠 P1 |
| **Décommenter ToggleStateService (persister état graphique)** | ⭐⭐⭐ | Trivial | 🟠 P1 |
| **Sélecteur de période inline dans le header** | ⭐⭐⭐⭐⭐ | Moyen | 🟠 P1 |
| **Comparatif N-1 sur les KPI** | ⭐⭐⭐⭐⭐ | Moyen | 🟠 P1 |
| **Indicateur de fraîcheur / last update** | ⭐⭐⭐ | Faible | 🟠 P1 |
| **Skeleton loaders** | ⭐⭐⭐ | Faible | 🟠 P1 |
| **Rang (#) dans les tableaux Top N** | ⭐⭐⭐ | Très faible | 🟠 P1 |
| **Courbe CA heure/heure** | ⭐⭐⭐⭐⭐ | Moyen (API getCaGroupingByPeriode existe) | 🟡 P2 |
| **Valeur du stock valorisé** | ⭐⭐⭐⭐⭐ | Moyen | 🟡 P2 |
| **Créances tiers-payants** | ⭐⭐⭐⭐ | Moyen | 🟡 P2 |
| **Toggle par section (granulaire)** | ⭐⭐⭐ | Faible | 🟡 P2 |
| **Drilldown graphiques → détail produit** | ⭐⭐⭐ | Moyen | 🟡 P2 |
| **Unification icônes (FA → PrimeIcons)** | ⭐⭐ | Faible | 🟢 P3 |
| **Taux de service** | ⭐⭐⭐⭐⭐ | Élevé (nouveau domaine) | 🟢 P3 |
| **Dashboard personnalisable** | ⭐⭐⭐ | Élevé | 🟢 P3 |
| **Export depuis le dashboard** | ⭐⭐⭐ | Moyen | 🟢 P3 |

---

## 9. Plan d'amélioration proposé

### Phase 0 — Corrections immédiates (< 1 journée, zéro risque)

```
□ Fix BUG #2 : halfyearly-data.component.ts ligne 32
    this.dashboardPeriode = CaPeriodeFilter.yearly
    →  this.dashboardPeriode = CaPeriodeFilter.halfyearly

□ Fix BUG #3 : weekly-data.component.ts ligne 29
    Supprimer : protected dashboardPeriode: CaPeriodeFilter | null = null;

□ Fix BUG #5 : graphe-daily.component.ts ligne 28
    dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.yearly
    →  dashboardPeriode: CaPeriodeFilter = CaPeriodeFilter.daily

□ Fix BUG #1 : Créer twentyEightyMontantChartData dans build2080Chart()
    et binder sur la section "Pareto 20/80 du chiffre d'affaires"

□ Afficher marge brute — 1 appel MargeReportService.getMargeSummary()
    → IMargeSummary.margeBruteGlobale + tauxMargeMoyen (AUCUN backend à créer)

□ Afficher panier moyen — DashboardCAService.getOverallSummary() déjà existant
    → IDashboardCASummary.panierMoyenToday/Week/Month/Year

□ Ajouter le label de période active ("Données du jour", "Cette semaine"...)
□ Ajouter horodatage "Dernière mise à jour : HH:mm" dans le header
□ Ajouter colonne "#" (rang) dans les tableaux Top N
```

### Phase 1 — Améliorations UX critiques + intégration reports (1-2 semaines)

```
□ Refactoriser la navigation temporelle (inspiration directe de mvt-period-shortcuts) :

  SUPPRIMER :
    - daily-data.component.ts, weekly-data.component.ts, monthly-data.component.ts
    - halfyearly-data.component.ts, yearly-data.component.ts (5 fichiers, 179 lignes)
    - sidebar col-lg-2 dans home.component.html
    - <ng-container ngbNavItem="daily/weekly/..."> × 5

  AJOUTER dans HomeBaseComponent :
    - protected readonly periodeOptions: PeriodOption[] = [...]  (5 lignes)
    - protected activePeriode = signal<CaPeriodeFilter>(CaPeriodeFilter.daily)
    - protected onPeriodeChange(p): void { ... this.loadDashboardData(); }
    - .dashboard-periode-selector dans dashboard-common.scss (style "pill filled")

  MODIFIER home.component.html :
    - Retirer col-lg-2 sidebar, passer contenu en col-12
    - Ajouter <jhi-home-base> unique en lieu de <jhi-daily-data> etc.

  ACTIVER ToggleStateService (décommenter dans le composant unifié)

□ Intégrer les KPI N-1 depuis DashboardCAService (AUCUN dev backend requis) :
    Ajouter dans le forkJoin de loadDashboardData() :
    caSummary: this.dashboardCAService.getOverallSummary()
    → Afficher caTodayEvolutionPct / caWeekEvolutionPct / caMonthEvolutionPct
      sur les 4 KPI Cards (flèche ↑↓ + pourcentage)

□ Intégrer le stock valorisé (AUCUN dev backend requis) :
    stockValuation: this.stockValuationReportService.getStockValuationSummary()
    → Nouvelle Card KPI : "Stock valorisé : 45 300 000 XOF (marge potentielle : 28%)"

□ Intégrer les créances tiers-payants (AUCUN dev backend requis) :
    creances: this.tiersPayantReportService.getCreancesSummary()
    → Nouvelle Card KPI : "Créances : X XOF (dont Y XOF > 90 jours)"

□ Skeleton loaders (PrimeNG Skeleton) pendant les chargements
□ États vides explicites pour les tableaux sans données
□ Bouton de rafraîchissement manuel dans le header
```

### Phase 2 — Enrichissement fonctionnel (2-4 semaines)

```
□ Courbe d'évolution CA avec comparatif N-1 (service déjà disponible !) :
    DashboardCAService.getEvolutionData() → IDashboardCAEvolution.caPreviousValues
    → Graphe ligne double : période actuelle vs période précédente

□ Fusionner ou lier DashboardCAComponent (reports/dashboard-ca) dans le dashboard :
    Option A : Ajouter un onglet "Dashboard CA Avancé" dans la sidebar home
    Option B : Intégrer ses KPI directement dans HomeBaseComponent

□ Marge par famille de produit (DashboardCAService.getProductFamilyDistribution())
    → Nouveau panneau "Répartition par famille" avec taux de marge par famille

□ Toggle Vue Graphique / Tabulaire par section (indépendant par panneau)
□ Drilldown graphiques → navigation vers détail produit / rapport marge
□ Visibilité du solde caisse sur le dashboard principal (CaissierDashboard summary)
```

### Phase 3 — Refonte UX avancée (1-2 mois)

```
□ Restructuration en zones de lecture (above the fold : 6 KPI max + alertes)
□ Fusionner/réorganiser les sections Pareto en un seul panneau à onglets
□ Unifier le système d'icônes (tout migrer vers PrimeIcons)
□ Hiérarchie visuelle des alertes (criticité distincte, animation pour critique)
□ Widget "Tâches du jour" (livraisons attendues, remboursements à soumettre...)
□ Ajout du taux de service (Rx délivrées / présentées) — nouveau domaine backend
□ Export PDF/Excel du dashboard en un clic (DashboardCAService.exportDashboardToPdf())
□ URL bookmarkable avec queryParam de période
```

---

## 10. Audit du module Reports — Données déjà disponibles pour le dashboard

> **Découverte majeure :** Pratiquement toutes les données identifiées comme manquantes
> dans le dashboard `HomeBaseComponent` sont **déjà implémentées** dans le module
> `app/entities/reports/` — mais jamais connectées au dashboard principal.

### 10.1 Inventaire des services reports disponibles

| Service | Endpoint backend | Statut |
|---------|-----------------|--------|
| `DashboardCAService` | `api/dashboard-ca` | ✅ Complet + export |
| `ComparativeReportService` | `api/comparative-reports` | ✅ Mensuel/Trimestriel/Annuel |
| `StockValuationReportService` | `api/stock/valuation` | ✅ Summary + export |
| `MargeReportService` | `api/marges-profitability` | ✅ Summary + top + faible marge |
| `ProfitabilityReportService` | `api/profitability` | ✅ BCG + Summary |
| `TiersPayantReportService` | `api/tiers-payant` | ✅ Créances + aging |
| `StockAlertReportService` | `api/stock/alerts` | ✅ Count par type |
| `SalesSummaryReportService` | `api/sales-summary` | ✅ Journalier par type |
| `TopProductsReportService` | `api/top-products` | ✅ Revenue + qty + evolution |

### 10.2 Mapping besoins dashboard → services reports

Chaque **lacune identifiée dans le dashboard** (§4) a son service correspondant :

#### Marge brute et taux de marge

```typescript
// MargeReportService.getMargeSummary()  →  api/marges-profitability/summary
interface IMargeSummary {
  margeBruteGlobale?: number;     // ← MARGE BRUTE GLOBALE (manquante dans dashboard)
  tauxMargeMoyen?: number;        // ← TAUX DE MARGE MOYEN
  caTotalGlobal?: number;
  nbProduitsMargeInsuffisante?: number;
  nbProduitsMargeConfortable?: number;
}
```

#### Panier moyen + évolution N-1 sur les KPI — UN SEUL APPEL suffit

```typescript
// DashboardCAService.getOverallSummary()  →  api/dashboard-ca/summary
interface IDashboardCASummary {
  // ← COMPARATIF N-1 PAR PÉRIODE — EXACTEMENT CE QUI MANQUE DANS HomeBaseComponent
  caToday?: number;         caTodayPrevious?: number;   caTodayEvolutionPct?: number;
  caWeek?: number;          caWeekPrevious?: number;    caWeekEvolutionPct?: number;
  caMonth?: number;         caMonthPrevious?: number;   caMonthEvolutionPct?: number;
  caYear?: number;          caYearPrevious?: number;    caYearEvolutionPct?: number;

  // ← PANIER MOYEN PAR PÉRIODE
  panierMoyenToday?: number;  panierMoyenWeek?: number;
  panierMoyenMonth?: number;  panierMoyenYear?: number;

  // ← TAUX DE MARGE PAR PÉRIODE
  tauxMargeToday?: number;    tauxMargeWeek?: number;
  tauxMargeMonth?: number;    tauxMargeYear?: number;
}
```

#### Courbe CA heure/heure et évolution avec N-1

```typescript
// DashboardCAService.getEvolutionData(period, start, end)  →  api/dashboard-ca/evolution
interface IDashboardCAEvolution {
  labels?: string[];               // libellés temporels
  caValues?: number[];             // CA période actuelle
  caPreviousValues?: number[];     // ← CA PÉRIODE PRÉCÉDENTE (N-1) — pour graphe comparatif
  transactionCounts?: number[];
  period?: 'daily' | 'weekly' | 'monthly';
}
```

#### Stock valorisé

```typescript
// StockValuationReportService.getStockValuationSummary()  →  api/stock/valuation/summary
interface IStockValuationSummary {
  totalPurchaseValue?: number;     // ← VALEUR STOCK EN PRIX D'ACHAT
  totalSalesValue?: number;        // ← VALEUR STOCK EN PRIX DE VENTE
  totalPotentialMargin?: number;   // ← MARGE POTENTIELLE SUR STOCK
  averageMarginPercentage?: number;
  totalProducts?: number;
  totalQuantity?: number;
}
```

#### Créances tiers-payants avec aging

```typescript
// TiersPayantReportService.getCreancesSummary()  →  api/tiers-payant/creances/summary
interface ITiersPayantCreancesSummary {
  groupeTiersPayantLibelle?: string;
  montantTotal?: number;           // ← TOTAL CRÉANCES PAR ASSUREUR
  montantMoinsDe30Jours?: number;  // ← AGING < 30 JOURS
  montantEntre30Et60Jours?: number;
  montantEntre60Et90Jours?: number;
  montantPlusDe90Jours?: number;   // ← CRÉANCES CRITIQUES > 90J
}
```

#### Alertes stock enrichies

```typescript
// StockAlertReportService.getStockAlertsCount()  →  api/stock/alerts/count
// Retourne : Record<StockAlertType, number>
// {  RUPTURE: 12,  ALERTE: 8,  PEREMPTION: 5  }
// Plus précis que AlertBadgeService qui n'a pas le type ALERTE (sous seuil non rupture)
```

#### IDailyCA — Données enrichies par journée

```typescript
// DashboardCAService.getDailySummary(start, end)  →  api/dashboard-ca/daily
interface IDailyCA {
  saleDate?: string;
  nbTransactions?: number;
  caTotal?: number;
  margeBrute?: number;        // ← MARGE PAR JOUR
  tauxMargePct?: number;      // ← TAUX MARGE PAR JOUR
  panierMoyen?: number;       // ← PANIER MOYEN PAR JOUR
  montantEncaisse?: number;
  montantCredit?: number;     // ← CRÉDIT PAR JOUR
  nbClients?: number;
}
```

### 10.3 Découverte critique — DashboardCAComponent : un second dashboard caché !

Il existe un composant **`DashboardCAComponent`** dans `reports/dashboard-ca/` qui
**implémente déjà** de nombreuses fonctionnalités manquantes dans `HomeBaseComponent` :

```
reports/dashboard-ca/dashboard-ca.component.ts  (523 lignes)
  ↓
  - KPI Cards : CA Today/Week/Month/Year avec évolution N-1 ✅
  - Courbe d'évolution CA + transactions Chart.js ✅
  - Sélecteur de période inline (today/week/month/year/custom) ✅
  - Distribution modes de paiement (pie chart) ✅
  - Distribution par famille de produits (bar chart) ✅
  - Top 10 produits ✅
  - Export PDF / Excel / CSV ✅
  - Refresh materialized views ✅
```

**Ce composant fait exactement ce que le dashboard principal devrait faire**,
mais il est accessible uniquement via les routes `/reports/dashboard-ca` —
**jamais affiché sur la page d'accueil**.

### 10.4 Tableau de mapping complet — Besoins vs. disponibilité

| KPI manquant (§ 3-4) | Service disponible | Méthode | Effort pour intégrer |
|---------------------|---------------------|---------|---------------------|
| **Marge brute + taux** | `MargeReportService` | `getMargeSummary()` | ⚡ Trivial (1 appel) |
| **Panier moyen** | `DashboardCAService` | `getOverallSummary()` | ⚡ Trivial (déjà dans `summary`) |
| **Évolution CA N-1** | `DashboardCAService` | `getOverallSummary()` | ⚡ Trivial (déjà dans `summary`) |
| **Courbe évolution CA** | `DashboardCAService` | `getEvolutionData()` | 🔧 Faible (port graphe existant) |
| **Comparatif N/N-1 mensuel** | `ComparativeReportService` | `getComparativeSummary()` | 🔧 Faible |
| **Stock valorisé** | `StockValuationReportService` | `getStockValuationSummary()` | ⚡ Trivial (1 appel) |
| **Créances tiers-payants** | `TiersPayantReportService` | `getCreancesSummary()` | 🔧 Faible |
| **Alertes stock enrichies** | `StockAlertReportService` | `getStockAlertsCount()` | ⚡ Trivial |
| **Export dashboard** | `DashboardCAService` | `exportDashboardToPdf()` | ⚡ Trivial (déjà impl.) |
| **CA par famille produit** | `DashboardCAService` | `getProductFamilyDistribution()` | 🔧 Faible |

> **Conclusion :** Sur les 10 KPI critiques manquants, **7 sont accessibles en 1 appel
> supplémentaire** et les 3 restants nécessitent seulement d'intégrer des services
> existants dans le `forkJoin` de `HomeBaseComponent`.

### 10.5 Stratégie d'intégration recommandée

**Option A — Fusion (recommandée) :** Enrichir `HomeBaseComponent` avec les services reports

```typescript
// Dans loadDashboardData() — ajouter au forkJoin existant :
const sources = {
  // ... sources existantes ...
  margeSummary:       this.margeReportService.getMargeSummary(),
  caSummary:          this.dashboardCAService.getOverallSummary(),       // panier + N-1
  stockValuation:     this.stockValuationReportService.getStockValuationSummary(),
  creancesSummary:    this.tiersPayantReportService.getCreancesSummary(),
};
```

**Option B — Redirection (simple) :** Ajouter un lien "Dashboard avancé →" depuis
`HomeBaseComponent` vers `DashboardCAComponent` pour les utilisateurs voulant plus de détails.

**Option C — Intégration hybride (idéale) :** Fusionner `DashboardCAComponent` dans
`HomeBaseComponent` en réutilisant ses graphiques et KPI N-1, en ajoutant les
sections Pareto et tiers-payants de l'ancien dashboard.

---

## Résumé exécutif

Le dashboard actuel constitue une **bonne base analytique** avec ses analyses Pareto
et la dualité graphique/tabulaire, mais il souffre de **cinq défauts majeurs** :

1. 🔴 **5 bugs identifiés** dont 3 trivials à corriger en < 1h chacun (mauvaises périodes dans
   HalfyearlyDataComponent et GrapheDailyComponent, shadow property dans WeeklyDataComponent)
2. 🔴 **Architecture anti-pattern** : 5 composants héritiers + sidebar verticale pour une simple
   sélection de période — 179 lignes pour ce qui devrait tenir en 3 lignes avec un `@Input()`
3. 🔴 **Des KPI métier fondamentaux absents** : marge, panier moyen, stock valorisé, créances —
   alors que **tous sont déjà implémentés** dans le module `reports/` mais jamais connectés
4. 🔴 **Absence de contexte temporel** : l'utilisateur ne sait pas sur quelle période il se
   trouve, et ne peut pas comparer avec la période précédente
5. 🟠 **Surcharge informationnelle** sans hiérarchie de lecture, en retard sur les standards des
   logiciels leaders du secteur (Winpharma, LGPI, Pharmaland)

### Découverte majeure — Tout le backend est déjà là

L'audit du module `reports/services/` révèle que **100% des données manquantes**
sont accessibles sans aucun développement backend supplémentaire :

```
Besoin dashboard              → Service reports déjà disponible
──────────────────────────────────────────────────────────────────────────────
Marge brute globale           → MargeReportService.getMargeSummary()
Panier moyen par période      → DashboardCAService.getOverallSummary()
Évolution CA N-1 (%)          → DashboardCAService.getOverallSummary()
Courbe CA avec N-1            → DashboardCAService.getEvolutionData()
Stock valorisé (PA/PV)        → StockValuationReportService.getStockValuationSummary()
Créances tiers-payants aging  → TiersPayantReportService.getCreancesSummary()
Export PDF dashboard          → DashboardCAService.exportDashboardToPdf()
```

De plus, **`DashboardCAComponent`** (`reports/dashboard-ca/`, 523 lignes) est un dashboard
complet avec KPI N-1, courbes d'évolution et export — accessible uniquement via les rapports,
**jamais affiché depuis la page d'accueil**.

Les corrections de **Phase 0** (bugs + connexion des services reports existants)
représentent **moins d'une journée de travail** pour une amélioration transformationnelle.
La **Phase 1** (refactorisation temporelle + intégration reports) est la plus structurante
pour l'expérience utilisateur et la maintenabilité du code.
