# Plan de Refactoring — Module Facturation, Règlement & Différés

> **Périmètre :**
> - `src/main/webapp/app/entities/facturation/` → remplacé par `features/facturation/`
> - `src/main/webapp/app/entities/reglement/` → absorbé dans `features/facturation/`
> - `src/main/webapp/app/entities/differes/` → remplacé par `features/differes/`
> - Backend : `service/facturation/`, `web/rest/facturation/`, domain `ThirdPartySales`
>
> **Stratégie :** Approche "Strangler Fig" — construire les nouveaux modules à côté des anciens, rediriger les routes, puis supprimer l'existant. **Aucun patch sur le code existant** (sauf un seul hotfix de corruption de données).
>
> **Basé sur :** Analyse comparative `ANALYSE-FACTURATION.md` + audit des modules `reglement` et `differes`
> **Standards de référence :** Nielsen Norman Group (10 heuristiques), ISO 9241-11, WCAG 2.1 AA, Odoo / QuickBooks / Winpharma
>
> **Patterns internes à appliquer dans les nouveaux modules :**
> | Pattern | Référence interne |
> |---|---|
> | Layout master/detail | `features/products/feature/produit-home` |
> | KPI / dashboard | `features/commande/feature/appro-unified-dashboard` |
> | Style KPI | `app/shared/scss/dashboard-common.scss` |
> | Actions en masse | `div.bulk-action-bar` de `produit-home.component.html` |
> | Confirmation | `NgbConfirmDialogService` (`inject(NgbConfirmDialogService)`) |
> | Notifications | `NotificationService` (`inject(NotificationService)`) |
> | Téléchargement | `TauriPrinterService.isRunningInTauri()` + `handleBlobForTauri` |
> | Modèles | `interface I*` (jamais `class` pour les DTOs) |
> | Dates | `DATE_FORMAT_ISO_DATE` (jamais `moment`) |
> | Pagination | `ITEMS_PER_PAGE` + `TableLazyLoadEvent` (jamais `size: 999999`) |
> | Cycle de vie | `takeUntilDestroyed(destroyRef)` (jamais `Subject` + `destroy$.complete()`) |
>
> **Date :** 2026-04-02

---

## Sommaire

1. [Diagnostic — Problèmes à ne pas reproduire](#1-diagnostic--problèmes-à-ne-pas-reproduire)
2. [Plan UX — Principes et propositions](#2-plan-ux--principes-et-propositions)
3. [Phase 0 — Hotfix unique](#3-phase-0--hotfix-unique)
4. [Phase 1 — Nouveau module `features/facturation/`](#4-phase-1--nouveau-module-featuresfacturation)
5. [Phase 2 — Nouveau module `features/differes/`](#5-phase-2--nouveau-module-featuresdifferes)
6. [Phase 3 — Complétude métier](#6-phase-3--complétude-métier)
7. [Phase 4 — Valeur ajoutée](#7-phase-4--valeur-ajoutée-long-terme)
8. [Clarification — Double tiers-payant](#8-clarification--double-tiers-payant-non-prioritaire)
9. [Matrice de priorité](#9-matrice-de-priorité)

---

## 1. Diagnostic — Problèmes à ne pas reproduire

> Cette section documente les défauts du code actuel. Elle sert de **liste de contrôle** lors de la construction des nouveaux modules : chaque point doit être corrigé dès l'écriture du nouveau code.

### 1.1 Problèmes communs aux deux modules (règlement + différés)

| Ref | Problème | Règle à appliquer dans le nouveau code |
|---|---|---|
| **P1** | Modèles en `class` au lieu d'`interface` | `export interface IReglement { ... }` avec préfixe `I` |
| **P2** | `moment.js` pour les dates | `DATE_FORMAT_ISO_DATE()` du projet |
| **P3** | `window.history.back()` non déterministe | `Router.navigate(['/...'])` |
| **P4** | `AlertInfoComponent` pour les erreurs | `NotificationService.error(msg, titre)` |
| **P5** | `ConfirmDialog` PrimeNG ou `ConfirmDialogComponent` custom | `NgbConfirmDialogService.onConfirm(...)` |
| **P6** | `Subject` + `destroy$.complete()` pour le cycle de vie | `takeUntilDestroyed(inject(DestroyRef))` |
| **P7** | `size: 999999` — chargement sans limite | `ITEMS_PER_PAGE` + `TableLazyLoadEvent` |
| **P8** | Téléchargement sans vérification Tauri | Pattern `isRunningInTauri()` + `handleBlobForTauri` obligatoire |
| **P9** | `setTimeout(..., 30)` pour l'initialisation de formulaire | `effect()` ou initialisation dans `ngAfterViewInit()` |
| **P10** | `WritableSignal` d'état dans un service `providedIn: root` | Store NgRx Signals (`signalStore`) |

### 1.2 Problèmes spécifiques au module Règlement

| Ref | Problème | Règle à appliquer |
|---|---|---|
| **R1** | ~70% de duplication entre `FaireGroupeReglementComponent` et `RegelementFactureIndividuelleComponent` | Un seul `ReglementWorkspaceComponent` avec `mode = input<'INDIVIDUEL'\|'GROUPE'>()` |
| **R2** | Bug `deleteAll` : `e.id` est `PaymentId` (objet), pas `number` | `selectedDatas.map(e => e.id.id)` — **seul bug traité en Phase 0** |
| **R3** | `isSaving` non géré dans `FaireGroupeReglementComponent` | `finalize(() => this.isSaving = false)` sur l'observable |
| **R4** | Typos : `Regelement*` au lieu de `Reglement*` | Nommer correctement dès la création |
| **R5** | Module règlement séparé physiquement de facturation | Absorber dans `features/facturation/` |
| **R6** | Deux composants de navigation dans `ReglementComponent` (onglet) | Pattern master/detail |

### 1.3 Problèmes spécifiques au module Différés

| Ref | Problème | Règle à appliquer |
|---|---|---|
| **D1** | `GestionDifferesComponent` : navigation onglets NgbNav | Pattern master/detail |
| **D2** | `FaireReglementDiffereComponent` : page routée séparée | Panneau de détail dans split container |
| **D3** | `DiffereSummary` affiché sans style `dashboard-common.scss` | `.kpi-card` avec variantes de couleur |
| **D4** | Duplication `fetchClients()` dans List + Reglement | Service / store partagé |
| **D5** | Filtres date commentés dans `ListDifferesComponent` | Activer dès la création du nouveau composant |

---

## 2. Plan UX — Principes et propositions

### 2.1 Pattern master/detail (modèle `produit-home`)

**Argument (Nielsen #3 — Contrôle et liberté) :** L'utilisateur ne doit jamais perdre le contexte de la liste. Le pattern master/detail — liste à gauche, panneau de détail/action à droite — est établi dans le projet via `produit-home` et doit être reproduit à l'identique.

```html
<!-- Barre d'actions en masse — apparaît si sélection (pattern produit-home) -->
@if (hasSelection()) {
  <div class="bulk-action-bar">
    <span class="bulk-count">{{ selected().length }} facture(s) sélectionnée(s)</span>
    <p-button label="Certifier FNE" icon="pi pi-shield" size="small" severity="info" [outlined]="true" />
    <p-button label="Exporter Excel" icon="pi pi-file-excel" size="small" severity="success" [outlined]="true" />
    <p-button icon="pi pi-times" size="small" severity="secondary" [text]="true"
              pTooltip="Désélectionner tout" (onClick)="onClearSelection()" />
  </div>
}

<!-- Split container -->
<div class="split-container" [class.panel-open]="panelOpen()">

  <div class="list-column">
    <app-facture-list [factures]="factures()" ... (factureSelected)="onFactureSelected($event)" />
  </div>

  @if (panelOpen()) {
    <div class="detail-column">
      <!-- Onglets : Détail | Régler | Versements -->
      <app-facture-detail-panel [facture]="selectedFacture()" (closePanel)="onClosePanel()" />
    </div>
  }

</div>
```

Le panneau de détail expose trois onglets :
- **Détail** — bons, lignes produit, n° assuré, piste d'audit
- **Régler** — `ReglementWorkspaceComponent` (mode INDIVIDUEL ou GROUPE selon le type de facture)
- **Versements** — historique chronologique des versements déjà effectués

Ce même pattern s'applique au module Différés (liste clients → panneau détail + saisie règlement).

---

### 2.2 KPI avec `dashboard-common.scss`

**Règle de cohérence :** Tous les tableaux de bord du projet utilisent `app/shared/scss/dashboard-common.scss`. Les nouveaux modules doivent importer ce fichier et utiliser les classes `.kpi-card`, `.kpi-value`, `.kpi-label`, `.kpi-icon` avec les variantes `.primary-accent`, `.success-accent`, `.warning-accent`, `.danger-accent`.

```scss
// facturation-home.component.scss
@use 'app/shared/scss/dashboard-common' as *;
```

```html
<div class="row g-3 mb-3">
  <div class="col-xl-3 col-md-6">
    <div class="kpi-card primary-accent">
      <div class="card-body">
        <div class="kpi-header">
          <div class="kpi-content">
            <div class="kpi-label">Facturé ce mois</div>
            <div class="kpi-value">{{ kpi()?.totalFacture | number:'1.0-0':'fr' }}
              <span class="value-suffix">FCFA</span>
            </div>
          </div>
          <div class="kpi-icon"><i class="pi pi-file-check"></i></div>
        </div>
        <div class="kpi-badges">
          <span class="badge bg-primary-subtle text-primary">{{ kpi()?.countFactures }} factures</span>
        </div>
      </div>
    </div>
  </div>
  <!-- success-accent → Total réglé -->
  <!-- warning-accent → En attente -->
  <!-- danger-accent  → En retard -->
</div>
```

---

### 2.3 Codes couleur statut et badges

| Statut | `p-tag` severity |
|---|---|
| PAID | `success` |
| PARTIALLY_PAID | `warn` |
| NOT_PAID | `danger` |
| EN RETARD | `danger` + icône `pi-clock` |

---

### 2.4 Pattern confirmation et notification

**Règle du projet — injecter systématiquement :**

```typescript
private readonly confirmDialog  = inject(NgbConfirmDialogService);
private readonly notificationService = inject(NotificationService);

// Confirmation
this.confirmDialog.onConfirm(
  () => this.doDelete(item),
  'Suppression',
  `Voulez-vous supprimer la facture ${item.numFacture} ?`
);

// Succès
this.notificationService.success('Règlement enregistré', 'Succès');

// Erreur
this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur');
```

Référence interne : `features/products/feature/produit-home/produit-home.component.ts`.

---

### 2.5 Téléchargement fichier — Pattern obligatoire Tauri

Toute méthode produisant un `Blob` (PDF, Excel, CSV, ticket) **doit** contenir la bifurcation Tauri :

```typescript
private readonly tauriPrinterService = inject(TauriPrinterService);

private handleBlob(blob: Blob, filename: string): void {
  if (this.tauriPrinterService.isRunningInTauri()) {
    handleBlobForTauri(blob, filename);
  } else {
    window.open(URL.createObjectURL(blob));
  }
}
```

Référence : `list-differes.component.ts:103`, `reglement-differes.component.ts:101`.

---

## 3. Phase 0 — Hotfix unique

> **Principe :** Un seul bug est traité sur le code existant — celui qui corrompt des données en production. Tous les autres défauts seront corrigés naturellement lors de la construction des nouveaux modules. Aucun autre patch sur l'existant.

### H0.1 — Bug `deleteAll` dans `FacturesRegleesComponent`

**Fichier :** `entities/reglement/factures-reglees/factures-reglees.component.ts:107`

**Problème :** `e.id` est de type `PaymentId` (`{ id: number, transactionDate: string }`). Passer des objets sérialisés là où l'API attend des identifiants simples peut provoquer une suppression silencieusement incorrecte.

```typescript
// AVANT (bug)
this.reglementService.deleteAll({ ids: this.selectedDatas.map(e => e.id) })

// APRÈS
this.reglementService.deleteAll({ ids: this.selectedDatas.map(e => e.id.id) })
```

**Effort :** 30 min | **Risque :** Nul

---

## 4. Phase 1 — Nouveau module `features/facturation/`

> **Objectif :** Construire `features/facturation/` de zéro en appliquant directement tous les patterns et corrections listés. Les modules `entities/facturation/` et `entities/reglement/` ne sont **pas modifiés** — ils sont remplacés à la fin.
>
> **Séquence :** Data layer → Store → Composants UI → Page feature → Migration routes → Suppression anciens modules

---

### 1.1 Structure cible

```
src/main/webapp/app/features/facturation/
├── data-access/
│   ├── services/
│   │   ├── facture-api.service.ts           # HTTP pur, pas de conversion moment
│   │   └── reglement-api.service.ts         # HTTP pur, inclut exportExcel()
│   ├── store/
│   │   ├── facturation.store.ts             # NgRx signalStore
│   │   └── reglement.store.ts               # NgRx signalStore
│   └── facades/
│       ├── facturation.facade.ts
│       └── reglement.facade.ts
├── ui/
│   ├── facture-list/                        # p-table lazy, sélection multiple, badges statut
│   ├── facture-detail-panel/                # Panneau 3 onglets (Détail/Régler/Versements)
│   ├── facture-kpi-banner/                  # KPI (dashboard-common.scss)
│   ├── reglement-workspace/                 # Fusion individuel+groupe, mode input
│   └── reglement-form/                      # Formulaire règlement (sans moment, sans setTimeout)
└── feature/
    └── facturation-home/
        ├── facturation-home.component.ts    # Orchestrateur master/detail
        └── facturation-home.component.html  # Split container (pattern produit-home)
```

---

### 1.2 Étape A — Data layer

**`facture-api.service.ts`**
- Migration de `entities/facturation/facture.service.ts`
- Supprimer toutes les conversions `moment`
- Ajouter `exportExcel(params): Observable<Blob>` (débloquer la fonctionnalité cachée)
- Ajouter `getKpi(params): Observable<IFacturationKpi>`
- Tous les modèles retournés : `interface I*` (pas de `class`)

**`reglement-api.service.ts`**
- Migration de `entities/reglement/reglement.service.ts`
- Ajouter `exportExcel(params): Observable<Blob>`
- Modèles : `IReglement`, `IReglementFactureDossier`, `IReglementParams` (interfaces)

**Modèles à créer comme interfaces :**
```typescript
// models/reglement.model.ts
export interface IReglement { id: IPaymentId; organismeId: number; ... }
export interface IReglementFactureDossier { id: number; montantPaye?: number; ... }
export interface IPaymentId { id: number; transactionDate: string; }
// Jamais de 'class' pour les DTOs
```

---

### 1.3 Étape B — Stores NgRx Signals

**`facturation.store.ts`**
```typescript
export const FacturationStore = signalStore(
  { providedIn: 'root' },
  withState<FacturationState>({
    factures: [],
    searchParams: defaultSearchParams,
    selectedFactures: [],
    loading: false,
    kpi: null,
  }),
  withComputed(({ factures }) => ({
    facturesNonReglees: computed(() => factures().filter(f => f.statut === 'NOT_PAID')),
    hasSelection: computed(() => /* selectedFactures */)
  })),
  withMethods(...)
);
```

Remplace `RegelementStateService` (signal manuel dans un service root).

---

### 1.4 Étape C — Composant `reglement-workspace`

Fusion de `RegelementFactureIndividuelleComponent` et `FaireGroupeReglementComponent` en un seul composant. Règles appliquées dès la création :

```typescript
@Component({ selector: 'app-reglement-workspace', ... })
export class ReglementWorkspaceComponent {
  readonly mode = input.required<'INDIVIDUEL' | 'GROUPE'>();
  readonly reglementFactureDossiers = input<IReglementFactureDossier[]>([]);
  readonly dossierFactureProjection = input<IDossierFactureProjection | null>(null);

  // NgbConfirmDialogService — pas de ConfirmDialog PrimeNG
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  // NotificationService — pas d'AlertInfoComponent
  private readonly notificationService = inject(NotificationService);
  // takeUntilDestroyed — pas de Subject/destroy$
  private readonly destroyRef = inject(DestroyRef);

  // Pagination réelle — pas de size: 999999
  private reload(id: IFactureId): void {
    const path = this.mode() === 'GROUPE' ? 'groupes' : 'individuelle';
    this.factureApiService
      .findDossierReglement(id, path, { page: 0, size: ITEMS_PER_PAGE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ ... });
  }

  // Bifurcation mode pour buildReglementParams
  private getModeEditionReglement(): ModeEditionReglement {
    return this.mode() === 'GROUPE'
      ? (this.partialPayment ? ModeEditionReglement.GROUPE_PARTIEL : ModeEditionReglement.GROUPE_TOTAL)
      : (this.partialPayment ? ModeEditionReglement.FACTURE_PARTIEL : ModeEditionReglement.FACTURE_TOTAL);
  }

  // finalize() sur isSaving — pas de remise à false prématurée
  protected onSaveReglement(params: IReglementParams): void {
    this.isSaving = true;
    this.reglementApiService.doReglement(this.buildReglementParams(params))
      .pipe(finalize(() => (this.isSaving = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: res => this.onPrintReceipt(res.body), error: err => this.onError(err) });
  }

  private onError(err: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur règlement');
  }
}
```

---

### 1.5 Étape D — Composant `reglement-form`

Migration de `entities/reglement/reglement-form/`. Corrections appliquées :

- Remplacer `moment(paymentDate).format(DATE_FORMAT)` → `DATE_FORMAT_ISO_DATE(paymentDate)`
- Remplacer `setTimeout(() => ..., 30)` → initialisation dans `ngAfterViewInit()` directement
- `takeUntilDestroyed` pour les `valueChanges`

---

### 1.6 Étape E — Composant `facture-kpi-banner`

Appelle `GET /api/facturation/kpi`. Utilise les classes `.kpi-card` de `dashboard-common.scss`. Chargement non bloquant (parallèle à la liste).

4 cartes :
- `.primary-accent` — Total facturé + count factures
- `.success-accent` — Total réglé + count réglées
- `.warning-accent` — Total en attente + count impayées
- `.danger-accent` — En retard + count en retard (visible uniquement si `countEnRetard > 0`)

---

### 1.7 Étape F — Composant `facture-list`

- `p-table` avec `[lazy]="true"` et `(onLazyLoad)="onLazyLoad($event)"` — pagination réelle
- Colonne sélection `p-tableHeaderCheckbox` + `(selectionChange)="onSelectionChanged($event)"`
- Badges statut avec `p-tag [severity]="getStatutSeverity(f.statut)"`
- Badge "EN RETARD" si `f.enRetard === true`
- Colonne actions : icône "Régler" (ouvre le panneau), "PDF", "Certifier FNE"

---

### 1.8 Étape G — Composant `facture-detail-panel`

Panneau avec trois onglets `NgbNav` :

1. **Détail** — informations facture, liste bons, n° assuré (`matricule`), piste d'audit (`createdBy`, `lastModifiedBy`)
2. **Régler** — `<app-reglement-workspace [mode]="factureMode()" ... />`
3. **Versements** — liste lazy des versements via `reglementApiService.getItems(paymentId)`, chargée à l'activation de l'onglet

Aperçu PDF : bouton "Aperçu" dans l'onglet Détail :
```typescript
onPreviewPdf(): void {
  this.factureApiService.getPdf(this.facture().factureItemId).subscribe(blob => {
    this.handleBlob(blob, `facture-${this.facture().numFacture}`);
  });
}
```

---

### 1.9 Étape H — Page `facturation-home`

Orchestrateur pattern master/detail (identique à `produit-home`) :

```typescript
protected selectedFacture = signal<IFacture | null>(null);
protected panelOpen = computed(() => this.selectedFacture() !== null);
protected selectedFactures = signal<IFacture[]>([]);
protected hasSelection = computed(() => this.selectedFactures().length > 0);
protected clearSelectionTrigger = signal(0);

onFactureSelected(facture: IFacture): void {
  this.selectedFacture.set(facture);
}
onClosePanel(): void { this.selectedFacture.set(null); }
onClearSelection(): void {
  this.selectedFactures.set([]);
  this.clearSelectionTrigger.update(v => v + 1);
}
```

Export Excel et PDF : toujours via `handleBlob()` (bifurcation Tauri).

---

### 1.10 Étape I — Migration des routes et suppression des anciens modules

**Routes :**
```typescript
// app.routes.ts — redirection progressive
{ path: 'facturation', loadChildren: () => import('./features/facturation/facturation.routes') },
{ path: 'reglement',   redirectTo: 'facturation', pathMatch: 'prefix' },
```

**Suppression** (après validation en recette) :
- `entities/facturation/` → supprimé
- `entities/reglement/` → supprimé

**Effort Phase 1 total :** ~10-12 jours

---

## 5. Phase 2 — Nouveau module `features/differes/`

> **Même stratégie :** construire à côté, appliquer tous les patterns dès la création, puis supprimer `entities/differes/`.

### 2.1 Structure cible

```
src/main/webapp/app/features/differes/
├── data-access/
│   ├── services/
│   │   └── differe-api.service.ts           # Migration DiffereService, sans moment, sans class
│   └── store/
│       └── differe.store.ts                 # NgRx signalStore (remplace WritableSignal root)
├── ui/
│   ├── differe-list/                        # p-table lazy, filtres date activés, badges statut
│   ├── differe-detail-panel/                # Onglets : Détail | Régler | Historique
│   ├── differe-kpi-banner/                  # DiffereSummary avec dashboard-common.scss
│   └── reglement-differe-form/             # Migration ReglementDiffereFormComponent
└── feature/
    └── differes-home/
        ├── differes-home.component.ts       # Orchestrateur master/detail
        └── differes-home.component.html     # Split container
```

### 2.2 Règles de construction

**Toutes les règles de la Phase 1 s'appliquent.** Points spécifiques au module Différés :

- **Filtres date activés** : `fromDate`/`toDate` non commentés dans `buildQueryParams()`
- **`DiffereSummary` → `.kpi-card`** : upgrader l'affichage existant avec `dashboard-common.scss`
- **Pas de `window.history.back()`** : `Router.navigate(['/differes'])` dans le panneau de détail
- **`fetchClients()` centralisé** : chargé une seule fois dans le store, pas dupliqué dans deux composants
- **Modèles** : `IDiffere`, `IDiffereItem`, `IDiffereSummary`, `IReglementDiffere` (interfaces)

### 2.3 Migration des routes

```typescript
{ path: 'differes', loadChildren: () => import('./features/differes/differes.routes') },
// L'ancienne route ':id/do-reglement-differe' disparaît — le règlement est dans le panneau
```

**Suppression** (après validation) : `entities/differes/` → supprimé.

**Effort Phase 2 total :** ~5-7 jours

---

## 6. Phase 3 — Complétude métier

> Nouvelles fonctionnalités construites **sur les nouveaux modules** (Phase 1 et 2 terminées).

### T3.1 — Endpoint KPI backend

**`GET /api/facturation/kpi?fromDate=&toDate=&organismeId=`**

```json
{
  "totalFacture": 1240000,
  "totalRegle": 890000,
  "totalRestant": 350000,
  "tauxRecouvrement": 71.8,
  "countFactures": 12,
  "countImpayees": 4,
  "countEnRetard": 2
}
```

Alimente le `FacturationKpiBannerComponent` (Étape F).

**Effort :** 2 jours | **Risque :** Faible

---

### T3.2 — Délai de règlement sur `TiersPayant` ET `GroupeTiersPayant`

Les factures étant générées soit par tiers-payant simple, soit par groupe de tiers-payants, les **deux entités** doivent porter le délai configurable.

**Backend :**
```java
// TiersPayant.java
@Column(name = "delai_reglement", columnDefinition = "int default 30")
private Integer delaiReglement = 30;

// GroupeTiersPayant.java — même ajout
@Column(name = "delai_reglement", columnDefinition = "int default 30")
private Integer delaiReglement = 30;
```

**Logique dans `FactureDto` :**
- Si facture groupée → `dateEcheance = facturationDate + groupeTiersPayant.delaiReglement`
- Si facture simple → `dateEcheance = facturationDate + tiersPayant.delaiReglement`
- `enRetard = dateEcheance < today && statut != PAID`

**Flyway :**
```sql
-- V1.0.X__add_delai_reglement.sql
ALTER TABLE warehouse.tiers_payant
  ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;

ALTER TABLE warehouse.groupe_tiers_payant
  ADD COLUMN IF NOT EXISTS delai_reglement INTEGER NOT NULL DEFAULT 30;
```

**Frontend :** Champ `delaiReglement` dans les formulaires de `TiersPayant` et `GroupeTiersPayant`. Badge "EN RETARD" dans `facture-list`.

**Effort :** 3 jours | **Risque :** Moyen

---

### T3.3 — Afficher le numéro d'assuré (L6.8)

Remonter `matricule` et `numeroAssurance` dans `DossierFactureDto` → `FactureItemDto`. Afficher dans l'onglet Détail du panneau.

**Effort :** 1 jour | **Risque :** Faible

---

### T3.4 — Piste d'audit (L6.5)

Vérifier que `Facture` étend `AbstractAuditingEntity`. Exposer `createdBy` / `lastModifiedBy` dans `FactureDto`. Section "Historique" dans l'onglet Détail.

**Effort :** 1 jour | **Risque :** Faible

---

### T3.5 — Export Excel règlements et différés

Ajouter `exportExcel(params): Observable<Blob>` dans `ReglementApiService` et `DiffereApiService`. Backend : `EasyExcel` ou `Apache POI`. Pattern Tauri obligatoire pour le téléchargement.

**Effort :** 2 jours | **Risque :** Faible

---

## 7. Phase 4 — Valeur ajoutée (long terme)

### T4.1 — Avoir / Note de crédit (L6.1)

Lacune la plus critique de l'analyse comparative. Toute correction de facture partiellement réglée exige aujourd'hui une suppression manuelle — non traçable et non conforme.

**Modèle conceptuel :**
```
Facture (PAID/PARTIAL) ──── génère ────► Avoir
                                          ├── numAvoir  (séquentiel, non modifiable)
                                          ├── montantAvoir
                                          ├── motif     (ERREUR / TROP_PERCU / RETOUR)
                                          └── factureOrigineId
```

**Backend :** `ModeEditionEnum.AVOIR`, service `EditionAvoirService`, entité `Avoir` liée à `Facture`, endpoint `POST /api/facturation/avoir/{factureId}`.

**Frontend :** Bouton "Émettre un avoir" dans l'onglet Détail (visible si `statut = PAID || PARTIALLY_PAID`). Modal ngbModal, `NgbConfirmDialogService` pour confirmer.

**Effort :** 8 jours | **Risque :** Élevé

---

### T4.2 — Relance automatique des impayés (L6.4)

Spring `@Scheduled` quotidien : factures `NOT_PAID` avec `dateEcheance < today - N jours`. Paramètre `N` configurable. Action : table `RelanceFacture` + email via service Gmail.

**Effort :** 5 jours | **Risque :** Élevé

---

### T4.3 — Export comptable FEC (L6.6)

Génération d'un fichier FEC (Fichier des Écritures Comptables) pour intégration SAGE/EBP.

**Effort :** 5 jours | **Risque :** Élevé

---

## 8. Clarification — Double tiers-payant (non prioritaire)

L'analyse de `ThirdPartySales.java` montre que la décomposition du montant est **déjà gérée à la vente** :

```java
@Column(name = "part_assure")       private Integer partAssure = 0;
@Column(name = "part_tiers_payant") private Integer partTiersPayant = 0;
```

Les factures sont générées **par organisme**. Le cas IPM + CNSS est résolu par deux bons séparés à la vente, chacun générant sa propre `ThirdPartySales`. Aucun développement nécessaire — à documenter dans le guide utilisateur.

---

## 9. Matrice de priorité

### Phase 0 — Hotfix (immédiat, < 1h)

| Tâche | Impact | Effort | Risque |
|---|---|---|---|
| H0.1 — Bug `deleteAll` | 🔴 Intégrité données | 30 min | Nul |

---

### Phase 1 — Nouveau module `features/facturation/` (3 semaines)

| Étape | Livrable | Effort | Dépendances |
|---|---|---|---|
| 1.A — Data layer | `facture-api`, `reglement-api`, interfaces | 2j | — |
| 1.B — Stores | `FacturationStore`, `ReglementStore` | 2j | 1.A |
| 1.C — `reglement-workspace` | Composant fusionné, tous patterns | 2j | 1.A, 1.B |
| 1.D — `reglement-form` | Sans moment, sans setTimeout | 1j | 1.A |
| 1.E — `facture-kpi-banner` | KPI avec dashboard-common.scss | 1j | 1.A, 1.B |
| 1.F — `facture-list` | Lazy, sélection, badges | 1j | 1.A |
| 1.G — `facture-detail-panel` | 3 onglets, aperçu PDF | 2j | 1.C, 1.D |
| 1.H — `facturation-home` | Split container master/detail | 1j | 1.E, 1.F, 1.G |
| 1.I — Migration routes + suppression | Routes + delete ancien code | 0.5j | 1.H validé |

---

### Phase 2 — Nouveau module `features/differes/` (1 semaine)

| Étape | Livrable | Effort | Dépendances |
|---|---|---|---|
| 2.A — Data layer | `differe-api`, interfaces, store | 1.5j | — |
| 2.B — `differe-list` | Lazy, filtres date, badges, KPI | 1j | 2.A |
| 2.C — `differe-detail-panel` | Détail + saisie règlement + historique | 1.5j | 2.A |
| 2.D — `differes-home` | Split container | 0.5j | 2.B, 2.C |
| 2.E — Migration routes + suppression | Routes + delete ancien code | 0.5j | 2.D validé |

---

### Phase 3 — Complétude métier (sur nouveaux modules)

| Tâche | Impact | Effort | Priorité |
|---|---|---|:---:|
| T3.1 — KPI backend | 🔴 Visibilité | 2j | **P0** |
| T3.3 — N° assuré visible | 🔴 Conformité | 1j | **P0** |
| T3.4 — Piste d'audit | 🔴 Conformité | 1j | **P0** |
| T3.2 — Délai règlement TP + Groupe | 🟠 Métier | 3j | **P1** |
| T3.5 — Export Excel | 🟠 Conformité | 2j | **P1** |

### Phase 4 — Long terme

| Tâche | Impact | Effort | Priorité |
|---|---|---|:---:|
| T4.1 — Avoir / note de crédit | 🔴 Critique | 8j | **P0** |
| T4.2 — Relance automatique | 🟠 Moyen | 5j | **P1** |
| T4.3 — Export FEC | 🟡 Optionnel | 5j | **P2** |

---

## Récapitulatif des fichiers

### Fichiers à créer (nouveaux modules)

| Fichier | Phase |
|---|---|
| `features/facturation/data-access/services/facture-api.service.ts` | 1.A |
| `features/facturation/data-access/services/reglement-api.service.ts` | 1.A |
| `features/facturation/data-access/store/facturation.store.ts` | 1.B |
| `features/facturation/data-access/store/reglement.store.ts` | 1.B |
| `features/facturation/ui/reglement-workspace/` | 1.C |
| `features/facturation/ui/reglement-form/` | 1.D |
| `features/facturation/ui/facture-kpi-banner/` | 1.E |
| `features/facturation/ui/facture-list/` | 1.F |
| `features/facturation/ui/facture-detail-panel/` | 1.G |
| `features/facturation/feature/facturation-home/` | 1.H |
| `features/differes/data-access/services/differe-api.service.ts` | 2.A |
| `features/differes/data-access/store/differe.store.ts` | 2.A |
| `features/differes/ui/differe-list/` | 2.B |
| `features/differes/ui/differe-kpi-banner/` | 2.B |
| `features/differes/ui/differe-detail-panel/` | 2.C |
| `features/differes/feature/differes-home/` | 2.D |

### Fichiers à modifier (seulement 2)

| Fichier | Modification |
|---|---|
| `entities/reglement/factures-reglees/factures-reglees.component.ts` | H0.1 : bug `deleteAll` |
| `app.routes.ts` | Phases 1.I et 2.E : ajouter nouvelles routes + redirectTo |

### Fichiers à supprimer (fin de chaque phase)

| Dossier | Phase de suppression |
|---|---|
| `entities/facturation/` | Fin Phase 1 (après validation recette) |
| `entities/reglement/` | Fin Phase 1 (après validation recette) |
| `entities/differes/` | Fin Phase 2 (après validation recette) |

### Backend — Fichiers à modifier / créer

| Fichier | Phase | Modification |
|---|---|---|
| `service/facturation/FacturationService.java` | P3 | `exportExcel()`, endpoint `kpi` |
| `web/rest/facturation/EditionFactureResource.java` | P3 | `/export`, `/kpi` |
| `web/rest/reglements/ReglementResource.java` | P3 | `/export` |
| `domain/TiersPayant.java` | P3 | `delaiReglement` |
| `domain/GroupeTiersPayant.java` | P3 | `delaiReglement` |
| `domain/Facture.java` | P3 | Vérifier `AbstractAuditingEntity` |
| `service/dto/FactureDto.java` | P3 | `createdBy`, `matricule`, `dateEcheance`, `enRetard` |

### Base de données — Migrations Flyway

| Migration | Phase | Description |
|---|---|---|
| `V1.0.X__add_delai_reglement.sql` | P3 | `delai_reglement` sur `tiers_payant` ET `groupe_tiers_payant` |
| `V1.0.Y__add_avoir_table.sql` | P4 | Table `avoir` liée à `facture` |
| `V1.0.Z__add_relance_table.sql` | P4 | Table `relance_facture` |

---

*Ce plan est vivant. Réviser après chaque phase en fonction du retour terrain.*
