# Plan de Complétion — Module `features/facturation`

> **Contexte :** Le nouveau module `features/facturation/` est partiellement implémenté.
> Il couvre la liste des factures, le panneau de détail, le règlement intégré et l'historique.
> Il lui manque l'**édition de factures**, la **certification FNE** et plusieurs correctifs UX.
>
> **Stratégie :** Ne pas toucher `entities/facturation/` (laisser en production).
> Construire les composants manquants directement dans `features/facturation/`
> en appliquant les patterns du projet (NgbConfirmDialogService, takeUntilDestroyed, patterns Tauri).
>
> **Référence patterns internes :**
> | Pattern | Source |
> |---|---|
> | Layout sidebar onglets | `features/facturation/feature/facturation-layout/` |
> | Split master/detail | `features/facturation/feature/facturation-home/` |
> | Store NgRx Signals | `features/facturation/data-access/store/facturation.store.ts` |
> | Confirmation | `inject(NgbConfirmDialogService)` |
> | Notification | `inject(NotificationService)` |
> | Téléchargement | `isRunningInTauri()` + `handleBlobForTauri()` |
> | Styles toolbar | `@use 'app/shared/scss/pharma-toolbar' as *` |
> | Styles tableau | `@use 'app/shared/scss/table-common' as *` |
> | Styles KPI | `@use 'app/shared/scss/dashboard-common' as *` |
> | Cycle de vie | `takeUntilDestroyed(inject(DestroyRef))` |
>
> **Date :** 2026-04-03

---

## Sommaire

1. [État des lieux — Ce qui existe](#1-état-des-lieux)
2. [Correctifs sur lexistant](#2-correctifs-sur-lexistant)
3. [Phase A — Édition de factures](#3-phase-a--édition-de-factures)
4. [Phase B — Certification FNE](#4-phase-b--certification-fne)
5. [Phase C — Compléments UX](#5-phase-c--compléments-ux)
6. [Matrice de priorité](#6-matrice-de-priorité)

---

## 1. État des lieux

### 1.1 Composants existants (features/facturation)

| Composant | Fichier | Statut |
|---|---|---|
| `FacturationLayoutComponent` | `feature/facturation-layout/` | ✅ OK |
| `FacturationHomeComponent` | `feature/facturation-home/` | ✅ OK |
| `HistoriqueReglementsComponent` | `feature/historique-reglements/` | ✅ OK |
| `FactureListComponent` | `ui/facture-list/` | ⚠️ Correctifs nécessaires |
| `FactureDetailPanelComponent` | `ui/facture-detail-panel/` | ⚠️ Certification manquante |
| `FactureKpiBannerComponent` | `ui/facture-kpi-banner/` | ⚠️ Endpoint KPI inexistant |
| `ReglementWorkspaceComponent` | `ui/reglement-workspace/` | ✅ OK |
| `ReglementFormComponent` | `ui/reglement-form/` | ✅ OK |
| `FactureSearchDrawerComponent` | `ui/facture-search-drawer/` | ✅ OK |
| `DossierReglementInfoComponent` | `ui/dossier-reglement-info/` | ✅ OK |

### 1.2 Composants ABSENTS (à créer)

| Composant | Équivalent entities | Priorité |
|---|---|---|
| `FacturationEditionComponent` | `entities/facturation/edition/` | 🔴 Critique |
| `CertificationService` | `entities/facturation/certification.service.ts` | 🔴 Critique |
| `FneCertificateViewerComponent` | `entities/facturation/fne-certificate-viewer/` | 🟠 Important |
| `FactureDetailDialogComponent` | `entities/facturation/facture-detail/` | 🟡 Mineur |
| `GroupeFactureDetailDialogComponent` | `entities/facturation/groupe-facture-detail/` | 🟡 Mineur |

### 1.3 Endpoints backend disponibles

```
GET    /api/edition-factures                               Liste factures simples
GET    /api/edition-factures/groupes                       Liste factures groupées
GET    /api/edition-factures/{id}/{date}                   Détail facture
DELETE /api/edition-factures/{id}/{date}                   Suppression
POST   /api/edition-factures/edit                          Créer/éditer factures ← non utilisé
GET    /api/edition-factures/bons                          Dossiers (mode SELECTION_BON)
GET    /api/edition-factures/data                          Tiers-payants + dossiers (autres modes)
GET    /api/edition-factures/pdf/{id}/{date}               PDF facture individuelle
GET    /api/edition-factures/pdf                           PDF batch après édition
GET    /api/edition-factures/export                        Excel factures simples
GET    /api/edition-factures/groupes/export                Excel factures groupées
GET    /api/edition-factures/kpi                           KPI (à vérifier backend)
GET    /api/edition-factures/reglement/{id}/{date}         Projection dossier règlement
GET    /api/edition-factures/reglement/single/{id}/{date}  Dossiers règlement simple
GET    /api/edition-factures/reglement/groupes/{id}/{date} Dossiers règlement groupe
GET    /api/certification-factures/certifier/{id}          Certifier FNE simple
GET    /api/certification-factures/certifier-groupe/{id}   Certifier FNE groupe
```

---

## 2. Correctifs sur l'existant

> Ces correctifs s'appliquent aux composants déjà créés dans `features/facturation/`.

---

### C1 — `FactureListComponent` : filtres statut bloquants

**Fichier :** `ui/facture-list/facture-list.component.ts`

**Problème :** `buildSearchParams()` force `statuts: ['PARTIALLY_PAID', 'NOT_PAID']`.
L'utilisateur ne peut jamais voir les factures `PAID` ni toutes les factures.

**Correction :**
- Ajouter un `p-select` "Statut" avec les options `ALL | PAID | NOT_PAID | PARTIALLY_PAID`
- `ALL` → ne pas inclure le paramètre `statuts` dans la requête
- Valeur par défaut : `ALL` (pas de filtre forcé)
- Dans `buildSearchParams()` : n'inclure `statuts` que si `selectedStatut !== 'ALL'`

```typescript
// Modèle options statut
protected readonly statutOptions = [
  { label: 'Tous', value: 'ALL' },
  { label: 'Réglées', value: 'PAID' },
  { label: 'Non réglées', value: 'NOT_PAID' },
  { label: 'Partiellement réglées', value: 'PARTIALLY_PAID' },
];
protected selectedStatut = 'ALL';

// Dans buildSearchParams()
...(this.selectedStatut !== 'ALL' ? { statuts: [this.selectedStatut] } : {})
```

---

### C2 — `FactureListComponent` : toggle facturation provisoire manquant

**Fichier :** `ui/facture-list/facture-list.component.ts` + `.html`

**Problème :** Le filtre "Factures provisoires" (`factureProvisoire: boolean`) existe dans l'ancien
composant mais est absent du nouveau. Certaines pharmacies travaillent en mode provisoire.

**Correction :**
- Ajouter `protected factureProvisoire = false;`
- Ajouter un `p-toggleswitch` dans la toolbar (label "Provisoires")
- Inclure dans `buildSearchParams()` : `factureProvisoire: this.factureProvisoire`

---

### C3 — `FactureListComponent` : suppression individuelle manquante

**Fichier :** `ui/facture-list/facture-list.component.ts` + `.html`

**Problème :** Seule la suppression en masse (sélection multiple) est gérée.
Il n'y a pas de bouton "Supprimer" sur une ligne individuelle.

**Correction :**
- Ajouter un bouton `pi pi-trash` dans la colonne actions (visible uniquement si `f.statut === 'NOT_PAID'`)
- Handler `onDeleteSingle(facture: IFacture)` avec `NgbConfirmDialogService`
- Appel `factureApiService.delete(factureId)` puis `onSearch()`

```typescript
onDeleteSingle(facture: IFacture): void {
  this.confirmDialog.onConfirm(
    () => this.factureApiService.delete({ id: facture.id, invoiceDate: facture.invoiceDate })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: () => this.onSearch(), error: err => ... }),
    'Suppression facture',
    `Supprimer la facture ${facture.numFacture} ?`
  );
}
```

---

### C4 — `FactureDetailPanelComponent` : boutons certifier FNE

**Fichier :** `ui/facture-detail-panel/facture-detail-panel.component.ts` + `.html`

**Problème :** L'onglet "Détail" n'expose pas les boutons "Certifier FNE" / "Voir certificat FNE".

**Correction :** Voir Phase B (certification FNE) — ces boutons seront ajoutés dans le cadre de B1.

---

### C5 — `FactureKpiBannerComponent` : endpoint KPI à vérifier

**Fichier :** `ui/facture-kpi-banner/facture-kpi-banner.component.ts`

**Action :** Vérifier que `GET /api/edition-factures/kpi` existe côté backend.
Si l'endpoint n'existe pas, le composant doit afficher un état vide sans erreur console.

**Correction (défensive) :**
```typescript
// Dans facturation-home.component.ts
this.factureApiService.getKpi({}).subscribe({
  next: res => this.store.setKpi(res.body ?? null),
  error: () => this.store.setKpi(null), // Silencieux si endpoint absent
});
```

---

## 3. Phase A — Édition de factures

> **Objectif :** Créer `FacturationEditionComponent` qui remplace `EditionComponent` de l'ancien module.
> Appliquer tous les patterns modernes dès la création.

---

### A1 — Modèles manquants

**Fichier à créer :** `data-access/models/edition.model.ts`

```typescript
export interface IEditionSearchParams {
  modeEdition: ModeEdition;
  fromDate: string | null;
  toDate: string | null;
  factureProvisoire: boolean;
  typeEditionReglement?: string;
  groupeTiersPayantId?: number;
  tiersPayantId?: number;
  selectedDossierIds?: number[];
}

export type ModeEdition =
  | 'ALL'
  | 'SELECTION_BON'
  | 'TIERS_PAYANT'
  | 'TYPE'
  | 'GROUP'
  | 'SELECTED';

export interface IDossierFacture {
  id: number;
  numBon: string;
  customer: string;
  saleDate: string;
  montantVente: number;
  montantFacture: number;
  tiersPayantId?: number;
  tiersPayantName?: string;
}

export interface ITiersPayantDossierFacture {
  tiersPayantId: number;
  tiersPayantName: string;
  nbreDossiers: number;
  montant: number;
  dossiers?: IDossierFacture[];
}

export interface IFactureEditionResponse {
  invoices: IFactureEditionItem[];
  nbreFactures: number;
  montantTotal: number;
}

export interface IFactureEditionItem {
  id: number;
  invoiceDate: string;
  numFacture: string;
}
```

Ajouter les exports dans `data-access/models/index.ts`.

---

### A2 — Service API : vérifier les méthodes édition dans `facture-api.service.ts`

**Fichier :** `data-access/services/facture-api.service.ts`

Les méthodes suivantes existent déjà — **vérifier leur signature** et compléter si nécessaire :

| Méthode | Endpoint | Statut |
|---|---|---|
| `queryBons(params)` | `GET /api/edition-factures/bons` | ✅ Présente |
| `queryEditionData(params)` | `GET /api/edition-factures/data` | ✅ Présente |
| `editInvoices(params)` | `POST /api/edition-factures/edit` | ✅ Présente |
| `exportAllInvoices(response)` | `GET /api/edition-factures/pdf` | ✅ Présente |

Si la signature des paramètres ne correspond pas aux nouveaux modèles (`IEditionSearchParams`),
adapter la méthode sans casser les autres appels.

---

### A3 — Composant `FacturationEditionComponent`

**Fichier à créer :** `feature/facturation-edition/facturation-edition.component.ts`

**Responsabilités :**
1. Formulaire de filtres (mode édition, dates, tiers-payant/groupe, type)
2. Tableau de résultats selon le mode sélectionné
3. Bouton "Éditer" → POST `/edit` → confirmation PDF
4. Téléchargement PDF batch (pattern Tauri)

**Structure TypeScript :**

```typescript
@Component({
  selector: 'app-facturation-edition',
  imports: [
    CommonModule, FormsModule,
    TableModule, ButtonModule, SelectModule, TooltipModule,
    FloatLabel, DatePickerModule, Toolbar, AutoCompleteModule, ToggleSwitch,
    CheckboxModule,
  ],
  templateUrl: './facturation-edition.component.html',
  styleUrl: './facturation-edition.component.scss',
})
export class FacturationEditionComponent implements OnInit {
  protected readonly modeOptions: { label: string; value: ModeEdition }[] = [
    { label: 'Tous les dossiers',              value: 'ALL' },
    { label: 'Sélection de bons',              value: 'SELECTION_BON' },
    { label: 'Par tiers-payant',               value: 'TIERS_PAYANT' },
    { label: 'Par type de règlement',          value: 'TYPE' },
    { label: 'Par groupe de tiers-payants',    value: 'GROUP' },
    { label: 'Sélection groupée',              value: 'SELECTED' },
  ];

  protected selectedMode: ModeEdition = 'ALL';
  protected factureProvisoire = false;
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();

  // Filtres conditionnels
  protected groupeTiersPayants: IGroupeTiersPayant[] = [];
  protected selectedGroupeTiersPayant: IGroupeTiersPayant | undefined;
  protected tiersPayants: ITiersPayant[] = [];
  protected selectedTiersPayant: ITiersPayant | undefined;
  protected typeEditionOptions = ['COMPTANT', 'ASSURANCE', 'CARNET'];
  protected selectedType: string | null = null;

  // Données
  protected dossiers: IDossierFacture[] = [];
  protected tpDossiers: ITiersPayantDossierFacture[] = [];
  protected selectedDossiers: IDossierFacture[] = [];
  protected selectedTpDossiers: ITiersPayantDossierFacture[] = [];

  // État
  protected loading = false;
  protected loadingEdit = false;
  protected page = 0;
  protected readonly itemsPerPage = ITEMS_PER_PAGE;
  protected totalItems = 0;

  // Injections
  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly groupeTiersPayantService = inject(GroupeTiersPayantService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngOnInit(): void {
    this.onSearch();
  }

  get isModeSelectionBon(): boolean { return this.selectedMode === 'SELECTION_BON'; }
  get isModeGroupe(): boolean { return this.selectedMode === 'GROUP' || this.selectedMode === 'SELECTED'; }
  get isModeTP(): boolean { return this.selectedMode === 'TIERS_PAYANT'; }
  get isModeType(): boolean { return this.selectedMode === 'TYPE'; }
  get hasSelection(): boolean {
    return this.selectedDossiers.length > 0 || this.selectedTpDossiers.length > 0;
  }

  onSearch(): void {
    this.page = 0;
    if (this.isModeSelectionBon) {
      this.loadBons();
    } else {
      this.loadData();
    }
  }

  onModeChange(): void {
    this.dossiers = [];
    this.tpDossiers = [];
    this.selectedDossiers = [];
    this.selectedTpDossiers = [];
    this.onSearch();
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    this.page = Math.floor((event.first ?? 0) / (event.rows ?? this.itemsPerPage));
    if (this.isModeSelectionBon) {
      this.loadBons(event.rows);
    } else {
      this.loadData(event.rows);
    }
  }

  onEdit(): void {
    this.confirmDialog.onConfirm(
      () => this.doEdit(),
      'Édition de factures',
      'Confirmer la génération des factures pour la sélection ?'
    );
  }

  searchTiersPayant(event: { query: string }): void { /* autocomplete */ }
  searchGroupeTiersPayant(event: { query: string }): void { /* autocomplete */ }

  private loadBons(rows = this.itemsPerPage): void {
    this.loading = true;
    this.factureApiService
      .queryBons({ ...this.buildParams(), page: this.page, size: rows })
      .pipe(finalize(() => (this.loading = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.dossiers = res.body ?? [];
          this.totalItems = Number(res.headers.get('X-Total-Count'));
        },
        error: err => this.notificationService.error(
          this.errorService.getErrorMessage(err), 'Chargement'
        ),
      });
  }

  private loadData(rows = this.itemsPerPage): void {
    this.loading = true;
    this.factureApiService
      .queryEditionData({ ...this.buildParams(), page: this.page, size: rows })
      .pipe(finalize(() => (this.loading = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.tpDossiers = res.body ?? [];
          this.totalItems = Number(res.headers.get('X-Total-Count'));
        },
        error: err => this.notificationService.error(
          this.errorService.getErrorMessage(err), 'Chargement'
        ),
      });
  }

  private doEdit(): void {
    this.loadingEdit = true;
    const params = this.buildEditionParams();
    this.factureApiService
      .editInvoices(params)
      .pipe(finalize(() => (this.loadingEdit = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const response = res.body;
          if (response) {
            this.notificationService.success(
              `${response.nbreFactures} facture(s) générée(s) — Total : ${response.montantTotal}`,
              'Édition réussie'
            );
            this.confirmDialog.onConfirm(
              () => this.downloadPdf(response),
              'Impression',
              'Voulez-vous télécharger le PDF des factures générées ?'
            );
          }
          this.onSearch();
        },
        error: err => this.notificationService.error(
          this.errorService.getErrorMessage(err), 'Erreur édition'
        ),
      });
  }

  private downloadPdf(response: IFactureEditionResponse): void {
    this.factureApiService
      .exportAllInvoices(response)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          if (this.tauriPrinterService.isRunningInTauri()) {
            handleBlobForTauri(blob, 'factures-editees');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: err => this.notificationService.error(
          this.errorService.getErrorMessage(err), 'Téléchargement PDF'
        ),
      });
  }

  private buildParams(): IEditionSearchParams {
    return {
      modeEdition: this.selectedMode,
      fromDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      toDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      factureProvisoire: this.factureProvisoire,
      ...(this.isModeGroupe && this.selectedGroupeTiersPayant
        ? { groupeTiersPayantId: this.selectedGroupeTiersPayant.id } : {}),
      ...(this.isModeTP && this.selectedTiersPayant
        ? { tiersPayantId: this.selectedTiersPayant.id } : {}),
      ...(this.isModeType && this.selectedType
        ? { typeEditionReglement: this.selectedType } : {}),
    };
  }

  private buildEditionParams(): IEditionSearchParams {
    const base = this.buildParams();
    if (this.selectedMode === 'SELECTED' && this.selectedTpDossiers.length > 0) {
      return {
        ...base,
        selectedDossierIds: this.selectedTpDossiers.map(d => d.tiersPayantId),
      };
    }
    if (this.isModeSelectionBon && this.selectedDossiers.length > 0) {
      return {
        ...base,
        selectedDossierIds: this.selectedDossiers.map(d => d.id),
      };
    }
    return base;
  }
}
```

---

### A4 — Template `facturation-edition.component.html`

**Structure HTML à implémenter :**

```
pharma-smart-content
├── pharma-toolbar pharma-toolbar-compact
│   ├── #start — filtres
│   │   ├── p-toggleswitch "Provisoires"
│   │   ├── p-datePicker "Du"
│   │   ├── p-datePicker "Au"
│   │   ├── p-select "Mode d'édition" [(ngModel)]="selectedMode" (onChange)="onModeChange()"
│   │   ├── @if (isModeGroupe || isModeTP) — p-autoComplete tiers-payant / groupe
│   │   └── @if (isModeType) — p-select type règlement
│   └── #end — boutons
│       ├── p-button "Rechercher" (click)="onSearch()"
│       └── p-button "Éditer" (click)="onEdit()" severity="success" [disabled]="!hasSelection && isModeSelectionBon"
│
└── table selon mode
    ├── @if (!isModeSelectionBon) — Table tiers-payants + dossiers
    │   Colonnes : Tiers-payant | Nbre dossiers | Montant
    │   Checkbox si SELECTED ou GROUP
    │   Lazy loading
    └── @if (isModeSelectionBon) — Table bons
        Colonnes : Client | Date | Réf bon | Mt Vente | Mt Facturé | ✓
        Sélection multiple via checkboxes
        Lazy loading
```

---

### A5 — SCSS `facturation-edition.component.scss`

```scss
@use 'app/shared/scss/pharma-toolbar' as *;
@use 'app/shared/scss/table-common' as *;
```

---

### A6 — Intégration dans le layout et les routes

**`feature/facturation-layout/facturation-layout.component.html`** — ajouter l'onglet :

```html
<ng-container ngbNavItem="edition">
  <a class="pharma-nav-vertical-link" ngbNavLink>
    <i class="pi pi-file-plus"></i>
    <span>Édition de factures</span>
    <span class="link-arrow">›</span>
  </a>
  <ng-template ngbNavContent>
    <app-facturation-edition />
  </ng-template>
</ng-container>
```

**`feature/facturation-layout/facturation-layout.component.ts`** — ajouter l'import :

```typescript
imports: [NgbNavModule, FacturationHomeComponent, HistoriqueReglementsComponent, FacturationEditionComponent],
```

Pas de nouvelle route nécessaire — l'édition est un onglet dans le layout existant.

---

## 4. Phase B — Certification FNE

---

### B1 — Service `CertificationApiService`

**Fichier à créer :** `data-access/services/certification-api.service.ts`

```typescript
@Injectable({ providedIn: 'root' })
export class CertificationApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/certification-factures';
  private readonly http = inject(HttpClient);

  certify(factureId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/certifier/${factureId}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }

  certifyGroupe(factureId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/certifier-groupe/${factureId}`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
```

---

### B2 — Intégration dans `FactureDetailPanelComponent`

**Fichier :** `ui/facture-detail-panel/facture-detail-panel.component.ts`

**Ajouts :**
- Injecter `CertificationApiService` et `TauriPrinterService`
- Ajouter `protected certificationLoading = false;`
- Ajouter `protected certifieBlob = signal<Blob | null>(null);`
- Méthode `onCertify()` :

```typescript
onCertify(): void {
  const f = this.facture();
  if (!f) return;
  this.certificationLoading = true;
  const call = f.groupeFactureId
    ? this.certificationApi.certifyGroupe(f.id)
    : this.certificationApi.certify(f.id);
  call.pipe(
    finalize(() => (this.certificationLoading = false)),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe({
    next: res => {
      const blob = res.body!;
      this.certifieBlob.set(blob);
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, `fne-${f.numFacture}`);
      } else {
        window.open(URL.createObjectURL(blob));
      }
      this.notificationService.success('Facture certifiée FNE', 'Certification');
    },
    error: err => this.notificationService.error(
      this.errorService.getErrorMessage(err), 'Certification FNE'
    ),
  });
}

onViewFne(): void {
  const blob = this.certifieBlob();
  if (blob) {
    if (this.tauriPrinterService.isRunningInTauri()) {
      handleBlobForTauri(blob, `fne-${this.facture()?.numFacture}`);
    } else {
      window.open(URL.createObjectURL(blob));
    }
  }
}
```

**Dans le template** (`facture-detail-panel.component.html`) — onglet Détail, zone actions :

```html
@if (facture()?.statut !== 'PAID' || !certifieBlob()) {
  <p-button
    (click)="onCertify()"
    [loading]="certificationLoading"
    [raised]="true"
    icon="pi pi-shield"
    label="Certifier FNE"
    severity="info"
    size="small"
  />
}
@if (certifieBlob()) {
  <p-button
    (click)="onViewFne()"
    [raised]="true"
    icon="pi pi-eye"
    label="Voir certificat FNE"
    severity="secondary"
    size="small"
  />
}
```

---

### B3 — Bouton certifier dans `FactureListComponent`

**Fichier :** `ui/facture-list/facture-list.component.html`

Ajouter dans la colonne actions (après le bouton PDF) :

```html
<p-button
  (click)="onCertify(f)"
  [rounded]="true"
  [text]="true"
  icon="pi pi-shield"
  pTooltip="Certifier FNE"
  severity="info"
  size="small"
/>
```

La méthode `onCertify(facture)` dans le composant délègue à `CertificationApiService`
et gère le pattern Tauri.

---

## 5. Phase C — Compléments UX

---

### D1 — Détail facture : colonnes manquantes dans `facture-list`

**Fichier :** `ui/facture-list/facture-list.component.html`

L'ancien composant affiche ces colonnes absentes du nouveau :
- **Remise** (`remise`)
- **Remise forfaitaire** (`remiseForfaitaire`)
- **Montant Brut** (`montantBrut`)
- **Montant payé** (`montantPaye`)
- **Montant restant** (`montantRestant`)

Vérifier le modèle `IFacture` dans `data-access/models/facture.model.ts` et ajouter les champs
manquants, puis les colonnes dans le tableau.

---

### D2 — Numéro d'assuré visible dans le panneau de détail

**Fichier :** `ui/facture-detail-panel/facture-detail-panel.component.html`

Afficher `matricule` et `numeroAssurance` dans l'onglet "Détail" si présents :

```html
@if (facture()?.matricule) {
  <div class="detail-row">
    <span class="detail-label">N° assuré</span>
    <span class="detail-value">{{ facture().matricule }}</span>
  </div>
}
```

Vérifier que `IFacture` et `IFactureItem` exposent bien ces champs depuis le backend
(`FactureDto` → `FactureItemDto`).

---

### D3 — Export Excel dans `HistoriqueReglementsComponent`

**Fichier :** `feature/historique-reglements/historique-reglements.component.ts`

Le bouton "Imprimer" existe (PDF). Ajouter un bouton "Excel" via `ReglementApiService.exportExcel()`.
Pattern Tauri obligatoire.

```typescript
onExportExcel(): void {
  this.loadingExcel = true;
  this.reglementApiService.exportExcel(this.buildParams())
    .pipe(finalize(() => (this.loadingExcel = false)), takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: blob => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          handleBlobForTauri(blob, 'reglements-factures');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: err => this.notificationService.error(
        this.errorService.getErrorMessage(err), 'Export Excel'
      ),
    });
}
```

---

## 6. Matrice de priorité

| Ref | Tâche | Impact | Effort | Priorité |
|---|---|---|---|:---:|
| **A1** | Modèles édition (`edition.model.ts`) | Prérequis A2-A6 | 1h | **P0** |
| **A2** | Vérifier `facture-api.service.ts` méthodes édition | Prérequis A3 | 30min | **P0** |
| **A3** | `FacturationEditionComponent` (TS) | 🔴 Critique métier | 1j | **P0** |
| **A4** | Template édition (HTML) | 🔴 Critique métier | 4h | **P0** |
| **A5** | SCSS édition | Styles | 30min | **P0** |
| **A6** | Intégration layout + onglet | Navigation | 30min | **P0** |
| **C1** | Filtre statut débloqué | 🟠 UX bloquant | 1h | **P1** |
| **C2** | Toggle facturation provisoire | 🟠 Fonctionnel | 30min | **P1** |
| **C3** | Suppression individuelle | 🟠 Fonctionnel | 1h | **P1** |
| **B1** | `CertificationApiService` | Prérequis B2-B3 | 30min | **P2** |
| **B2** | Boutons certifier dans détail panel | 🟠 Conformité | 1h | **P2** |
| **B3** | Bouton certifier dans liste | 🟡 Confort | 30min | **P2** |
| **D1** | Colonnes manquantes dans liste | 🟡 Complétude | 1h | **P3** |
| **D2** | N° assuré dans panneau détail | 🟡 Conformité | 30min | **P3** |
| **D3** | Export Excel historique règlements | 🟡 Fonctionnel | 1h | **P3** |

### Séquence recommandée

```
A1 → A2 → A3 + A4 + A5 → A6
           ↓
         C1 + C2 + C3 (en parallèle des correctifs liste)
           ↓
         B1 → B2 + B3
           ↓
         D1 + D2 + D3
```

**Effort total estimé : 3-4 jours**

---

## Annexe — Fichiers à créer

```
features/facturation/
├── data-access/
│   ├── models/
│   │   └── edition.model.ts                        ← CRÉER (A1)
│   └── services/
│       └── certification-api.service.ts            ← CRÉER (B1)
└── feature/
    └── facturation-edition/
        ├── facturation-edition.component.ts        ← CRÉER (A3)
        ├── facturation-edition.component.html      ← CRÉER (A4)
        └── facturation-edition.component.scss      ← CRÉER (A5)
```

## Annexe — Fichiers à modifier

```
features/facturation/
├── data-access/
│   ├── models/
│   │   └── index.ts                                ← Ajouter export edition.model.ts
│   └── services/
│       └── facture-api.service.ts                  ← Vérifier signatures (A2)
├── feature/
│   └── facturation-layout/
│       ├── facturation-layout.component.html       ← Ajouter onglet édition (A6)
│       └── facturation-layout.component.ts         ← Ajouter import (A6)
└── ui/
    ├── facture-list/
    │   ├── facture-list.component.ts               ← C1 + C2 + C3
    │   └── facture-list.component.html             ← C1 + C2 + C3 + B3 + D1
    └── facture-detail-panel/
        ├── facture-detail-panel.component.ts       ← B2 + D2
        └── facture-detail-panel.component.html     ← B2 + D2
```
