# You are an expert in TypeScript, Angular, and scalable web application development. You write functional, maintainable, performant, and accessible code following Angular and TypeScript best practices.


# PLAN — Migration PrimeNG → ng-bootstrap (+ Design System maison)

> Statut : **Proposition initiale**
> Auteur cible : équipe front Pharma-Smart
> Contexte : PrimeNG 22 impose une licence commerciale payante pour un usage commercial. Objectif : sortir progressivement de cette dépendance en s'appuyant sur `@ng-bootstrap/ng-bootstrap` (déjà présent), un design system maison (`app/shared/ui`), et quelques libs tierces ciblées.

---

## 1. Contexte et objectifs

### 1.1 Pourquoi migrer

- **Licence** — PrimeNG 22 exige un token cryptographique valide au boot (`providePrimeNG` → `verifyLicense`). Une bannière rouge s'affiche autrement. Le tier `community` est réservé au non-commercial ; Pharma-Smart est un produit commercial, donc soit tier `commercial` payant, soit sortie de la dépendance.
- **Cohérence** — Le projet mixe déjà Bootstrap 5 (`bootstrap`, `bootswatch` thème "yeti", `@ng-bootstrap/ng-bootstrap` 21) et PrimeNG. Doublons visuels et deux systèmes de theming à maintenir.
- **Bundle** — PrimeNG (22.x) pèse ~450 KB gzippé pour l'ensemble des composants importés ; Bootstrap est déjà chargé par ailleurs.
- **Lock-in** — Passer par une couche de wrapping maison réduit le coût d'un futur changement de lib.

### 1.2 Objectifs mesurables

| Objectif | Métrique | Cible |
|---|---|---|
| Retirer la dépendance `primeng` | Import `from 'primeng/*'` | 0 |
| Retirer `@primeuix/themes` | Références Aura preset | 0 |
| Retirer la bannière licence | `#p-license-host` dans DOM | 0 |
| Réduire le bundle initial | `initial` gzip | −15 % à −25 % |
| Couverture Design System | Composants `app/shared/ui/*` réutilisés | ≥ 80 % des écrans nouveaux |

### 1.3 Non-objectifs

- **AG Grid** reste (indépendant de PrimeNG, déjà utilisé pour les grosses tables).
- **Chart.js** reste (Prime ne fait qu'un thin wrapper `p-chart`).
- Pas de refonte visuelle : la migration préserve l'UX actuelle.
- Pas de "big-bang" — approche incrémentale par écran.

---

## 2. Inventaire des composants PrimeNG utilisés

Extrait de `grep p-<selector>` sur `pharmaSmart-app/src/main/webapp` (comptages d'occurrences template) :

| Composant | Usages | Priorité |
|---|---|---|
| `p-button` | 1115 | 🔴 Critique |
| `p-table` (+ sous-cellules `p-celleditor`, `p-sorticon`, `p-tablecheckbox`, `p-tableheadercheckbox`) | 368 + 105 | 🔴 Critique |
| `p-floatlabel` | 288 | 🔴 Critique |
| `p-select` | 248 | 🔴 Critique |
| `p-card` | 178 | 🟠 Haute |
| `p-toolbar` | 176 | 🟠 Haute |
| `p-tag` | 171 | 🟠 Haute |
| `p-iconfield` + `p-inputicon` | 122 + 61 | 🟠 Haute |
| `p-divider` | 115 | 🟢 Basse |
| `p-toast` | 100 | 🟠 Haute |
| `p-datepicker` | 87 | 🟠 Haute (déjà wrappé partiellement) |
| `p-inputnumber` | 57 | 🟠 Haute |
| `p-inputgroup` + `p-inputgroup-addon` | 46 + 52 | 🟠 Haute |
| `p-buttongroup` | 50 | 🟢 Basse |
| `p-toggleswitch` | 47 | 🟠 Haute |
| `p-autocomplete` | 40 | 🔴 Critique |
| `p-chart` | 31 | 🟢 Basse |
| `p-splitbutton` | 26 | 🟠 Haute |
| `p-badge` | 24 | 🟢 Basse |
| `p-checkbox` | 23 | 🟠 Haute |
| `p-radiobutton` | 21 | 🟠 Haute |
| `p-skeleton` | 14 | 🟢 Basse |
| `p-drawer` | 14 | 🟠 Haute |
| `p-multiselect` | 13 | 🟠 Haute |
| `p-fileupload` | 12 | 🟠 Haute |
| `p-progressbar` | 11 | 🟢 Basse |
| `p-password` | 11 | 🟢 Basse |
| `p-chip` | 10 | 🟢 Basse |
| `p-dialog` | 9 | 🟠 Haute |
| `p-popover` | 8 | 🟢 Basse |

**Ordre de bataille** : les 8 composants critiques et hautes couvrent ~95 % des usages. Les prioriser fait tomber le reste rapidement.

---

## 3. Table de correspondance PrimeNG → alternative

| PrimeNG | Remplacement | Détails |
|---|---|---|
| `p-button` | `<button class="btn btn-…">` natif Bootstrap 5 | Wrapper `AppButton` maison (existe déjà, à réécrire sans PrimeNG) |
| `p-table` (tous cas) | `AppTable` maison — **API compatible `p-table`** | Objectif : substitution `p-table` → `app-table` avec **zéro modif** des inputs/outputs les plus utilisés (`value`, `columns`, `paginator`, `rows`, `sortField`, `filters`, templates `#header`/`#body`/`#footer`, events `onRowSelect`, `onLazyLoad`, etc.). Voir §4.2 pour le contrat. **AG Grid n'est pas concerné** : les tables actuellement en AG Grid restent en AG Grid (cohabitation identique à l'existant PrimeNG + AG Grid) |
| `p-floatlabel` | `<div class="form-floating">` Bootstrap 5 | Directive utilitaire pour préserver ergonomie label flottant |
| `p-select` (options simples, < 20 items, pas de search) | `<select class="form-select">` natif | Substitution directe, aucun wrapper |
| `p-select` (avec search, lazy, ≥ 20 items) | `AppSelectSearch` maison **wrappant `@ng-select/ng-select`** | Unifié avec `p-multiselect` sur la même lib. `ng-select` gère search, virtual scroll, templates custom, groupes, lazy. **Apparence alignée sur `p-select`** — voir §8.9 pour le calage visuel (theming ng-select) |
| `p-autocomplete` | `AppSelectSearch` maison (wrapper `ng-select`) **ou** `NgbTypeahead` selon complexité | `ng-select` couvre la plupart des cas. `NgbTypeahead` reste pertinent pour un simple input texte avec suggestions attachées (sans dropdown structuré) |
| `p-multiselect` | `AppMultiSelect` maison **wrappant `@ng-select/ng-select`** | Décision figée §14 : `ng-select` retenu. Le wrapper `AppMultiSelect` reste pour l'API cohérente et l'isolation du call site — pas de dépendance directe à `ng-select` dans les écrans |
| `p-card` | `<div class="card data-card">` **natif Bootstrap** (pattern déjà utilisé) | Voir `home-base.component.html:217` — combo `card h-100 data-card` récurrent. **Pas de wrapper** ; substitution directe |
| `p-toolbar` | `<nav class="navbar">` / `<div class="btn-toolbar">` | Pas de wrapper — pattern trivial |
| `p-tag` / `p-badge` / `p-chip` | `<span class="badge">` Bootstrap | Wrapper `AppBadge` |
| `p-iconfield` + `p-inputicon` | `<div class="input-group">` avec `<span class="input-group-text">` | Pattern natif Bootstrap |
| `p-inputgroup` + `p-inputgroup-addon` | `<div class="input-group">` + `<span class="input-group-text">` | Idem, remap direct |
| `p-inputnumber` | `<input type="number">` + directive `AppNumberFormat` | Formatage FR (séparateurs, décimales) via directive maison |
| `p-password` | `<input type="password">` + toggle `AppPasswordInput` | Composant maison ~30 lignes |
| `p-checkbox` | `<input type="checkbox" class="form-check-input">` | Wrapper `AppCheckbox` avec CVA |
| `p-radiobutton` | `<input type="radio" class="form-check-input">` | Wrapper `AppRadio` avec CVA |
| `p-toggleswitch` | `<input class="form-check-input" role="switch">` | Wrapper `AppSwitch` avec CVA |
| `p-datepicker` | `NgbDatepicker` (déjà utilisé) | `pharma-date-picker` **existe déjà** → généraliser |
| `p-divider` | `<hr class="my-3">` | Aucun wrapper — remplacement direct |
| `p-skeleton` | Bootstrap `.placeholder` + `.placeholder-glow` | Wrapper `AppSkeleton` |
| `p-progressbar` | `NgbProgressbar` | Remap direct |
| `p-toast` | `NgbToast` derrière `NotificationService` **existant** | `app/shared/services/notification.service.ts` déjà en place — on **swap l'implémentation** (retire `MessageService` PrimeNG, branche `NgbToast`) sans casser les call sites |
| `p-dialog` | `NgbModal` (déjà utilisé) | `ngb-confirm-dialog` **existe déjà** ; généraliser au reste des dialogs |
| `p-drawer` | `NgbOffcanvas` | Remap direct |
| `p-popover` | `NgbPopover` | Remap direct |
| `p-tooltip` (directive) | `[ngbTooltip]` | Remap direct |
| `p-tabs` / `p-tabpanel` | `NgbNav` + `[ngbNavItem]` | Pattern légèrement différent |
| `p-accordion` | `NgbAccordion` | Remap direct |
| `p-menubar` / `p-panelmenu` | `NgbDropdown` imbriqué / custom | Rare dans le projet, cas par cas |
| `p-splitbutton` | `NgbDropdown` + `<div class="btn-group">` | Wrapper `AppSplitButton` |
| `p-picklist` | **Deux `app-data-table` asymétriques** — voir §6.1 | Usage unique (`tableau-produit/produits`). Pas de wrapper `AppPickList` : décision documentée en §6.1 |
| `p-buttongroup` | `<div class="btn-group">` Bootstrap | Aucun wrapper |
| `p-fileupload` | `<input type="file">` + wrapper `AppFileUpload` | Uploads simples ; s'appuyer sur `HttpClient` pour l'envoi |
| `p-chart` | **`app-chart`** (existant) | `app/shared/chart/chart.component.ts` — wrapper Chart.js **avec API compatible `p-chart`** déjà en place. Substitution `p-chart` → `app-chart` directe |

---

## 4. Design System maison — état cible

L'arborescence cible `app/shared/ui/` centralise le vocabulaire visuel. Les composants existants sont réécrits pour ne plus dépendre de PrimeNG.

```
app/shared/
├── ui/                            # Design System (générique, réutilisable)
│   ├── button/
│   │   └── app-button.component.ts          ⚠ existe — à réécrire (retirer PrimeNG)
│   ├── badge/
│   │   └── app-badge.component.ts           ➕ nouveau (remplace p-tag/p-badge/p-chip)
│   ├── checkbox/
│   │   └── app-checkbox.component.ts        ➕ nouveau (CVA)
│   ├── table/
│   │   └── app-table.component.ts           ➕ nouveau — clé de voûte (voir §4.2)
│   ├── file-upload/
│   │   └── app-file-upload.component.ts     ➕ nouveau
│   ├── float-label/
│   │   └── app-float-label.component.ts     ➕ nouveau (form-floating Bootstrap)
│   ├── form-field/                          ⚠ existe — à ajuster
│   ├── icon-field/
│   │   └── app-icon-field.component.ts      ➕ nouveau (input-group + icon)
│   ├── input/
│   │   └── app-input.component.ts           ⚠ existe — à réécrire
│   ├── input-number/
│   │   └── app-input-number.component.ts    ➕ nouveau (CVA + directive format)
│   ├── modal/                               ⚠ existe — à ajuster (ngbModal wrappers)
│   ├── multi-select/
│   │   └── app-multi-select.component.ts    ➕ nouveau (wrapper @ng-select/ng-select — multiple)
│   ├── password/
│   │   └── app-password.component.ts        ➕ nouveau
│   ├── radio/
│   │   └── app-radio.component.ts           ➕ nouveau (CVA)
│   ├── select-search/
│   │   └── app-select-search.component.ts   ➕ nouveau (wrapper @ng-select/ng-select — single)
│   ├── skeleton/
│   │   └── app-skeleton.component.ts        ➕ nouveau
│   ├── split-button/
│   │   └── app-split-button.component.ts    ➕ nouveau (NgbDropdown + btn-group)
│   ├── switch/
│   │   └── app-switch.component.ts          ➕ nouveau (form-check switch)
│   ├── toast-host/
│   │   └── toast-host.component.ts          ➕ nouveau (consomme NotificationService, rend NgbToast)
│   └── index.ts                             # Barrel export
├── chart/
│   └── chart.component.ts                   ✅ existe — remplace p-chart (Chart.js, API compat)
├── date-picker/
│   └── pharma-date-picker.component.ts      ✅ existe — remplace p-datepicker (NgbDatepicker)
├── dialog/
│   └── ngb-confirm-dialog/
│       └── ngb-confirm-dialog.component.ts  ✅ existe — remplace ConfirmDialog PrimeNG
└── services/
    └── notification.service.ts              ⚠ existe — swap implémentation (retire MessageService, branche NgbToast)
```

**Rien à créer sous `card/`** : l'app utilise déjà le combo natif Bootstrap `.card.data-card` (voir `home-base.component.html:217`). Substitution `<p-card>` → `<div class="card data-card">` directe, avec `<div class="card-header">` / `<div class="card-body">`.

### 4.1 Composants existants à réutiliser (modèles de référence)

Ces composants **servent de modèle** pour tous les autres :

- `app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.component.ts`
  - Basé sur `NgbActiveModal`
  - Ouverture via `ConfirmationService` maison (à créer, cf. §7)
  - Template inline stylé, gestion du `ngbAutofocus` et garde anti-Enter

- `app/shared/date-picker/pharma-date-picker.component.ts`
  - Basé sur `NgbDatepicker` + `FrenchDateParserFormatter`
  - Implémente `ControlValueAccessor`
  - `input()` partout, footer template avec bouton "Aujourd'hui" et "Fermer"

- `app/shared/chart/chart.component.ts`
  - Wrapper Chart.js avec API **compatible `p-chart`** (`type`, `data`, `options`, `plugins`, `width`, `height`, event `dataSelect`)
  - Utilise `effect()` + `untracked()` + `NgZone.runOutsideAngular()` pour la performance
  - Substitution triviale : `<p-chart …>` → `<app-chart …>` (mêmes attributs)

- `app/shared/services/notification.service.ts`
  - API stable (`success/info/warning/error/show/clear`) — **utilisée déjà partout dans l'app**
  - Implémentation actuelle : `MessageService` PrimeNG
  - Migration : remplacer la dépendance sur `MessageService` par un stockage `signal<ToastMessage[]>` interne. Aucun call site à modifier

### 4.2 Contrat `AppTable` — clé de voûte

`p-table` est utilisé 368 fois — le nouveau composant `AppTable` doit permettre une substitution avec **effort minimal par écran**. Le contrat cible :

**Inputs à supporter (parité fonctionnelle avec `p-table`)** :

| Input | Type | Rôle |
|---|---|---|
| `value` | `T[]` | Données. Alias identique à `p-table` |
| `columns` | `AppTableColumn<T>[]` (optionnel) | Colonnes générables. Si absent, l'utilisateur écrit le `<thead>`/`<tbody>` avec templates |
| `paginator` | `boolean` | Active `NgbPagination` en pied de tableau |
| `rows` | `number` | Taille page. Défaut 10 |
| `rowsPerPageOptions` | `number[]` | Sélecteur "10 / 25 / 50 / 100" |
| `totalRecords` | `number` | Pour pagination server-side |
| `lazy` | `boolean` | Mode server-side |
| `sortField` / `sortOrder` | `string` / `1 \| -1` | Tri initial |
| `dataKey` | `string` | Clé identifiante (pour selection) |
| `selection` | `T \| T[]` | Ligne(s) sélectionnée(s) |
| `selectionMode` | `'single' \| 'multiple'` | Mode selection |
| `loading` | `boolean` | Affiche un `AppSkeleton` en overlay |
| `styleClass` | `string` | Classes CSS additionnelles sur `<table>` |
| `stripedRows` | `boolean` | Alterne les couleurs (`.table-striped`) |
| `showGridlines` | `boolean` | Bordures (`.table-bordered`) |
| `size` | `'small' \| 'large'` | Compacte (`.table-sm`) |
| `globalFilterFields` | `string[]` | Champs cibles du filtre global |

**Outputs** :

| Output | Type | Rôle |
|---|---|---|
| `onLazyLoad` | `AppTableLazyLoadEvent` | Émis en lazy mode. Payload compatible `p-table` : `{ first, rows, sortField, sortOrder, filters }` |
| `onRowSelect` / `onRowUnselect` | `{ data, index, originalEvent }` | Idem `p-table` |
| `onSort` | `{ field, order }` | Tri changé |
| `onPage` | `{ first, rows }` | Pagination changée |

**Templates de projection** (avec `#header`, `#body`, `#footer`, `#emptymessage`, `#caption`) — pour matcher l'API `p-table` :

```html
<app-table [value]="products()" [columns]="cols" [paginator]="true" [rows]="10">
  <ng-template #header let-columns>
    <tr>
      @for (col of columns; track col.field) {
        <th [appSortableHeader]="col.field">{{ col.header }}</th>
      }
    </tr>
  </ng-template>

  <ng-template #body let-row let-columns="columns">
    <tr>
      @for (col of columns; track col.field) {
        <td>{{ row[col.field] }}</td>
      }
    </tr>
  </ng-template>

  <ng-template #emptymessage>
    <tr><td [attr.colspan]="cols.length" class="text-center text-muted py-4">Aucune donnée</td></tr>
  </ng-template>
</app-table>
```

**Rendu HTML sous-jacent** — pur Bootstrap 5 :

```html
<div class="app-table-wrapper">
  <table class="table table-hover" [class.table-sm]="size() === 'small'" [class.table-striped]="stripedRows()">
    <thead> <!-- projection #header --> </thead>
    <tbody> <!-- projection #body pour chaque row visible --> </tbody>
    <tfoot> <!-- projection #footer --> </tfoot>
  </table>
  @if (paginator()) {
    <ngb-pagination
      [collectionSize]="totalRecords() ?? value().length"
      [pageSize]="rows()"
      [(page)]="currentPage"
      (pageChange)="onPageChange($event)"
    />
  }
</div>
```

**Directive compagnon** `appSortableHeader` (l'équivalent de `p-sorticon` + `pSortableColumn`) — gère l'affichage de la flèche de tri et le clic.

**Interface `AppTableColumn`** :

```ts
export interface AppTableColumn<T = unknown> {
  field: keyof T & string;
  header: string;
  sortable?: boolean;
  filterable?: boolean;
  width?: string;
  align?: 'left' | 'center' | 'right';
}
```

**Cohabitation avec AG Grid** — même règle qu'aujourd'hui : les écrans actuellement en AG Grid **restent en AG Grid** (pas de migration `ag-grid-angular` → `AppTable`), et les écrans actuellement en `p-table` passent à `AppTable` (pas de migration `p-table` → AG Grid). La décision AG Grid vs table plus légère est déjà tranchée écran par écran dans l'existant ; on **préserve** ce choix.

`AppTable` couvre donc **exactement le périmètre `p-table` actuel** — ni plus, ni moins. Si un cas `p-table` existant utilise des features non triviales à porter (édition inline massive `p-celleditor`, row grouping, etc.), reproduire dans `AppTable` ou négocier un downscope avec le PO — sans jamais basculer par défaut vers AG Grid.

---

## 5. Conventions techniques — règles impératives

Toutes les nouvelles écritures et réécritures **doivent** respecter ces règles.

### 5.1 Composants

- **Une responsabilité par composant**. Si un composant fait 3 choses, on le scinde.
- **`input()` / `output()`** — pas de `@Input` / `@Output` décorateurs.
  ```ts
  // ✅
  readonly label = input<string>('');
  readonly changed = output<string>();
  // ❌
  @Input() label = '';
  @Output() changed = new EventEmitter<string>();
  ```
- **Templates inline** pour tout composant de moins de ~60 lignes de template. Fichier séparé (`.html`) seulement au-delà.
- **`standalone: true` interdit explicitement** — c'est le défaut depuis Angular 20+.
- **`changeDetection: OnPush` interdit explicitement** — c'est le défaut depuis Angular 22+.
- **`@HostBinding` / `@HostListener` interdits** — utiliser l'objet `host` du décorateur.
  ```ts
  // ✅
  @Component({
    selector: 'app-badge',
    host: {
      '[class.app-badge--danger]': 'severity() === "danger"',
      '(click)': 'onClick()',
    },
    template: `…`,
  })
  // ❌
  @HostBinding('class.app-badge--danger') isDanger = false;
  @HostListener('click') onClick() {}
  ```

### 5.2 Bindings

- **`class` binding**, pas `ngClass` :
  ```html
  <!-- ✅ -->
  <div [class.active]="isActive()" [class]="dynamicClasses()">
  <!-- ❌ -->
  <div [ngClass]="{ active: isActive() }">
  ```
- **`style` binding**, pas `ngStyle` :
  ```html
  <!-- ✅ -->
  <div [style.width.px]="width()" [style.background]="bg()">
  <!-- ❌ -->
  <div [ngStyle]="{ width: width() + 'px' }">
  ```

### 5.3 State

- **`signal()`** pour tout état local composant.
- **`computed()`** pour tout état dérivé.
- **`update` / `set`** — jamais `mutate`.
  ```ts
  // ✅
  count.update(v => v + 1);
  items.set([...items(), newItem]);
  // ❌
  items.mutate(arr => arr.push(newItem));
  ```
- Transformations pures et prévisibles — pas d'effet de bord dans les `computed()`.

### 5.4 Routing

- **Lazy loading** obligatoire pour toute nouvelle feature route :
  ```ts
  {
    path: 'partners',
    loadComponent: () => import('./partners.component').then(m => m.PartnersComponent),
  }
  ```

### 5.5 CVA (ControlValueAccessor)

Tous les composants "form control" (`AppCheckbox`, `AppRadio`, `AppSwitch`, `AppInput`, `AppInputNumber`, `AppSelectSearch`, `AppMultiSelect`, `AppPassword`) implémentent `ControlValueAccessor` — se calquer sur `pharma-date-picker.component.ts` pour le pattern.

### 5.6 Squelette de référence

```ts
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-badge',
  host: {
    '[class]': 'hostClasses()',
  },
  template: `
    <span class="app-badge__label">{{ label() }}</span>
    @if (dismissible()) {
      <button class="app-badge__close" (click)="dismissed.emit()" [attr.aria-label]="closeAria()">
        <i class="pi pi-times"></i>
      </button>
    }
  `,
  styles: `
    :host { display: inline-flex; align-items: center; gap: .25rem; }
    .app-badge__close { background: none; border: 0; padding: 0; }
  `,
})
export class AppBadge {
  readonly label = input.required<string>();
  readonly severity = input<'primary' | 'success' | 'warning' | 'danger' | 'info'>('primary');
  readonly dismissible = input<boolean>(false);
  readonly closeAria = input<string>('Fermer');
  readonly dismissed = output<void>();

  protected readonly hostClasses = computed(() => `app-badge app-badge--${this.severity()}`);
}
```

Noter :
- Aucune propriété `standalone`.
- Aucune propriété `changeDetection`.
- `host: { '[class]': '…' }` au lieu de `@HostBinding`.
- `computed()` pour la classe dérivée.

---

## 6. Stratégie de migration — phasage

### Phase 0 — Préparation (1 sprint)
- Créer la branche `feat/migration-primeng-ngb`.
- Vérifier que tous les composants existants du Design System compilent après retrait de leur dépendance à PrimeNG.
- Documenter le mapping visuel (screenshots avant/après) pour les composants critiques.
- **Sortie** : plan validé, branche prête, no-op sur `main`.

### Phase 1 — Fondations Design System (2 sprints)

Créer/réécrire les composants **atomiques** utilisés partout. Pas de migration d'écran encore.

| Ordre | Composant / tâche | Estimation |
|---|---|---|
| 1 | `AppButton` (réécriture, `<button class="btn">`) | 0.5 j |
| 2 | `AppBadge` (couvre `p-tag` / `p-badge` / `p-chip`) | 0.5 j |
| 3 | `AppIconField` + directive `[appInputIcon]` | 1 j |
| 4 | `AppFloatLabel` (form-floating) | 0.5 j |
| 5 | `AppInput` (réécriture, CVA) | 1 j |
| 6 | `AppInputNumber` (CVA + directive format) | 1.5 j |
| 7 | `AppCheckbox` / `AppRadio` / `AppSwitch` | 1.5 j (les 3) |
| 8 | `AppSkeleton` | 0.5 j |
| 9 | Installer `@ng-select/ng-select` + theme de base | 0.25 j |
| 10 | **Calage visuel ng-select ≃ p-select** — `_ng-select-pharma.scss` (§8.9) | **1 j** |
| 11 | `AppSelectSearch` (wrapper `ng-select` single) | 0.75 j |
| 12 | `AppMultiSelect` (wrapper `ng-select` multiple) | 0.75 j |
| 13 | `AppSplitButton` | 1 j |
| 14 | `AppPassword` | 0.5 j |
| 15 | `AppFileUpload` | 1 j |
| 16 | **`AppTable`** + directive `[appSortableHeader]` (voir §4.2) | **3 j** |
| 17 | `ToastHostComponent` (`NgbToast` × N) monté dans layout racine | 0.5 j |
| 18 | Swap implémentation `NotificationService` (retire `MessageService`, branche signal + host) | 1 j |
| 19 | `ConfirmationService` (façade `NgbModal` + `NgbConfirmDialog`) | 0.5 j |
| 20 | Normalisation `pharma-date-picker` (input `icon` par défaut, doc) | 0.25 j |

**Sortie Phase 1** : Design System complet, testé unitairement (Jest), aucun écran migré encore.

### Phase 2 — Écrans à faible risque (2-3 sprints)

Migrer les écrans avec peu de logique métier pour valider les composants du DS.

Cibles suggérées :
- `admin/user-management/*`
- `entities/categorie/*`
- `entities/tva/*`
- `account/settings/*`
- `account/password/*`
- Écrans "list + form" simples de `entities/*`

**Chaque écran** : substitution `p-…` → `app-…`, retrait des imports PrimeNG, vérif visuelle, PR dédiée.

### Phase 3 — Écrans complexes (4-6 sprints)

Écrans avec beaucoup de logique et de table :
- `features/sales/*` — POS, caisse, ticketing
- `features/commande/*` — commandes, réceptions
- `features/facturation/*`
- `features/products/*` (formulaire produit géant)
- `entities/reports/*` — 20+ rapports, migration table par table

**Règle** : préserver la cohabitation actuelle — `p-table` → `AppTable`, AG Grid → AG Grid (inchangé). Aucun écran ne bascule d'une lib vers l'autre pendant la migration ; les arbitrages de lib restent hors scope de ce chantier.

### 6.1 `p-picklist` → deux `app-data-table` asymétriques

> **Priorité basse.** Usage unique dans l'application (`entities/tableau-produit/produits`),
> écran peu fréquenté. À traiter en fin de Phase 3, après les écrans à fort trafic.

**Décision : pas de wrapper `AppPickList`.** Reproduire la liste double à l'identique
reconduirait un patron déjà inadapté aux données.

**Constat.** `produit-associes.component.ts` charge ses deux colonnes en
`page: 0, size: 300` avec recherche serveur, sur un catalogue produits qui dépasse
largement ce volume. Le picklist promet donc une métaphore que les données ne tiennent
pas :

- au-delà de 300 résultats, le reste est invisible **sans aucune indication** ;
- `moveAllToTarget` / `moveAllToSource` n'envoient que les ids **chargés** : « tout
  déplacer » déplace en réalité « les 300 premiers ». Bug silencieux à ne pas reconduire.

**Cible.** Deux tables au rôle distinct, et non deux listes miroir :

| Colonne | Rôle | Contenu |
|---|---|---|
| Gauche | **Sélecteur de catalogue** — un outil de recherche, pas une liste à parcourir | Recherche serveur + pagination réelle, action `Associer` par ligne, sélection multiple pour un lot |
| Droite | **État de l'objet** — court par nature (quelques dizaines) | Produits associés, action `Retirer` |

Bénéfices : pagination et total visibles, tri et colonnes (CIP, prix) exploitables,
boutons natifs focusables là où le drag & drop n'est pas accessible au clavier.

**Perte assumée** : le drag & drop. Sur un catalogue dont on n'affiche qu'une fraction,
on ne fait pas glisser vers ce qu'on ne peut pas voir — la perte est un gain de simplicité.

**⚠ Décision backend en suspens.** Si un « tout associer » est conservé, il doit porter
sur **l'ensemble du résultat de la recherche** : l'API doit recevoir les critères, pas une
liste d'ids. Sinon on réimplémente le bug actuel.

### Phase 4 — Nettoyage (1 sprint)

> ⚠ **Ordre impératif — à faire en premier, avant toute désinstallation :**
>
> ```bash
> node scripts/generate-pharma-tokens.mjs --used-only
> ```
>
> Pendant les Phases 0→3, `_pharma-tokens.scss` contient le **jeu complet** des 392 tokens
> Aura : le Design System en cours d'écriture en consomme de nouveaux à chaque composant,
> et les avoir tous évite de régénérer en permanence (surcoût ~1,8 Ko gzip, invisible tant
> que PrimeNG est encore là de toute façon).
>
> En Phase 4 le jeu est stable, donc élagable sans risque — mais **uniquement tant que
> `primeng` / `@primeuix/themes` sont encore installés** : ce sont eux qui fournissent les
> valeurs. Une fois désinstallés, `_pharma-tokens.scss` est figé définitivement et l'élagage
> devient impossible. Oublier cette étape est en revanche sans danger (on conserve 1,8 Ko).

- Retirer `primeng`, `@primeuix/themes`, `primeicons` de `package.json`.
- Retirer `providePrimeNG(...)` et l'import associé dans `app.config.ts`.
- Retirer les surcharges CSS liées à PrimeNG dans `global.scss` (dont `#p-license-host`, `.p-overlay`, `.p-datepicker-panel`).
- Substituer `pi pi-*` par Bootstrap Icons (`bi bi-*`) ou garder `primeicons` en dépendance stylistique isolée si effort trop lourd.
- Retirer le script de suppression de bannière (`scripts/strip-safe-nav-migration.js` déjà appliqué, mais l'override CSS peut aussi partir).

**Sortie** : bundle allégé, une seule lib UI (`@ng-bootstrap`), plus de check licence, plus de bannière.

---

## 7. Services d'accompagnement à créer

### 7.1 `NotificationService` — adaptation (pas de nouveau service)

Le service existant `app/shared/services/notification.service.ts` **garde son API publique** (`success/info/warning/error/show/clear`). Seule l'**implémentation interne** change : on retire la dépendance à `MessageService` de PrimeNG et on branche un stockage interne + rendu `NgbToast`.

Structure cible :

```ts
export interface ToastMessage {
  id: number;
  severity: NotificationSeverity;   // 'success' | 'info' | 'warn' | 'error' — inchangé
  summary?: string;
  detail: string;
  life: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _messages = signal<ToastMessage[]>([]);
  readonly messages = this._messages.asReadonly();
  private nextId = 1;

  success(message: string, title?: string): void { this.push('success', message, title, 3000); }
  info(message: string, title?: string): void    { this.push('info',    message, title, 3000); }
  warning(message: string, title?: string): void { this.push('warn',    message, title, 4000); }
  error(message: string, title?: string): void   { this.push('error',   message, title, 5000); }
  show(severity: NotificationSeverity, message: string, title?: string, life = 3000): void {
    this.push(severity, message, title, life);
  }
  clear(): void { this._messages.set([]); }

  dismiss(id: number): void { this._messages.update(msgs => msgs.filter(m => m.id !== id)); }

  private push(severity: NotificationSeverity, detail: string, summary: string | undefined, life: number): void {
    const id = this.nextId++;
    this._messages.update(msgs => [...msgs, { id, severity, detail, summary, life }]);
  }
}
```

Un `<app-toast-host />` monté une seule fois dans le layout racine consomme le signal et rend des `<ngb-toast>`. Les ~100 call sites de `NotificationService` restent intacts.

### 7.2 `ConfirmationService`

Remplace `ConfirmationService` PrimeNG. Ouvre `NgbConfirmDialog` via `NgbModal`.

```ts
@Injectable({ providedIn: 'root' })
export class ConfirmationService {
  private readonly modal = inject(NgbModal);

  confirm(opts: {
    header?: string;
    message: string;
    icon?: string;
    acceptLabel?: string;
    rejectLabel?: string;
  }): Promise<boolean> { … }
}
```

Retourne `true` si acceptation, `false` sinon.

### 7.3 Directive `[appInputIcon]`

Remplace `p-iconfield` + `p-inputicon`. Purement structurelle : ajoute une icône à gauche/droite d'un `<input>` en s'intégrant dans `.input-group`.

---

## 8. Préservation du look — theming & tokens CSS

Enjeu critique : le projet utilise **87 variables CSS `--p-*`** distinctes (inventaire ci-dessous), pour un total d'environ **380 occurrences** dans les fichiers `.scss`, `.css`, `.html` et `.ts`. Ces variables sont exposées par `@primeuix/themes` (preset **Aura**) via un provider dynamique injecté par `providePrimeNG({ theme: { preset: Aura } })`. Retirer PrimeNG **sans compensation** ferait s'effondrer visuellement l'app.

### 8.1 Top des variables utilisées

Extrait `grep --p-<token>` :

| Token | Occurrences | Rôle |
|---|---|---|
| `--p-primary-color` | 54 | Couleur primaire (accents, boutons, liens) |
| `--p-text-muted-color` | 53 | Texte secondaire discret |
| `--p-text-color` | 45 | Texte principal |
| `--p-surface-border` | 42 | Bordures cartes / séparateurs |
| `--p-red-500` | 22 | Erreurs / danger |
| `--p-surface-50` | 21 | Fond très clair (hover discret, zebra) |
| `--p-surface-100` | 19 | Fond clair (zones neutres) |
| `--p-surface-200` | 15 | Fond médium |
| `--p-text-color-secondary` | 13 | Texte secondaire |
| `--p-border-radius` | 12 | Rayon de bordure standard |
| `--p-surface-0` | 11 | Blanc pur (fond cartes) |
| … | … | 76 autres (`--p-primary-{50…900}`, `--p-red-*`, `--p-blue-*`, `--p-surface-*`, `--p-*-color`, `--p-dialog-border-radius`, etc.) |

### 8.2 Stratégie — "freeze the tokens"

Créer un fichier de tokens **statiques** qui reproduit exactement la palette Aura actuellement générée par `@primeuix/themes`. Chargé une fois dans `vendor.scss`, il rend transparente la suppression de PrimeNG pour tous les consommateurs.

**Fichier cible** : `pharmaSmart-app/src/main/webapp/content/scss/_pharma-tokens.scss`

```scss
/**
 * Tokens PharmaSmart — reproduction figée du preset Aura de @primeuix/themes.
 * Charge en tête de vendor.scss pour préserver le look historique après
 * retrait de PrimeNG.
 *
 * Généré depuis getComputedStyle(document.documentElement) sur la version
 * 22.x de primeng + @primeuix/themes (2026-07-17). Version figée : NE PAS
 * régénérer sans revue design.
 */
:root {
  // --- Primary (bascule cyan par défaut chez Aura) ---
  --p-primary-color: #06b6d4;
  --p-primary-contrast-color: #ffffff;
  --p-primary-50: #ecfeff;
  --p-primary-100: #cffafe;
  --p-primary-200: #a5f3fc;
  --p-primary-300: #67e8f9;
  --p-primary-400: #22d3ee;
  --p-primary-500: #06b6d4;
  --p-primary-600: #0891b2;
  --p-primary-700: #0e7490;
  --p-primary-800: #155e75;
  --p-primary-900: #164e63;

  // --- Surface neutre (fonds, cartes, bordures) ---
  --p-surface-0: #ffffff;
  --p-surface-50: #f8fafc;
  --p-surface-100: #f1f5f9;
  --p-surface-200: #e2e8f0;
  --p-surface-300: #cbd5e1;
  --p-surface-400: #94a3b8;
  --p-surface-500: #64748b;
  --p-surface-600: #475569;
  --p-surface-700: #334155;
  --p-surface-800: #1e293b;
  --p-surface-900: #0f172a;
  --p-surface-950: #020617;
  --p-surface-border: var(--p-surface-200);
  --p-surface-ground: var(--p-surface-100);
  --p-surface-card: var(--p-surface-0);

  // --- Texte ---
  --p-text-color: var(--p-surface-700);
  --p-text-color-secondary: var(--p-surface-500);
  --p-text-secondary-color: var(--p-surface-500);
  --p-text-muted-color: var(--p-surface-400);

  // --- Bordures & radius ---
  --p-content-border-color: var(--p-surface-200);
  --p-border-radius: 6px;
  --p-dialog-border-radius: 10px;

  // --- Palettes accessoires (utilisées ponctuellement) ---
  --p-red-50:  #fef2f2; --p-red-100: #fee2e2; --p-red-200: #fecaca;
  --p-red-300: #fca5a5; --p-red-400: #f87171; --p-red-500: #ef4444;
  --p-red-600: #dc2626; --p-red-700: #b91c1c;

  --p-blue-50:  #eff6ff; --p-blue-200: #bfdbfe; --p-blue-400: #60a5fa;
  --p-blue-700: #1d4ed8;

  --p-yellow-50:  #fefce8; --p-yellow-200: #fef08a; --p-yellow-300: #fde047;
  --p-yellow-700: #a16207;

  --p-orange-300: #fdba74; --p-orange-600: #ea580c;

  --p-pink-300: #f9a8d4;
  --p-sky-400: #38bdf8;

  // (compléter avec les ~30 tokens résiduels d'après l'inventaire — cf. §8.4)
}
```

### 8.3 Procédure d'extraction fiable des valeurs

Les valeurs ci-dessus sont indicatives (Aura par défaut). **Ne pas les recopier aveuglément** — la source de vérité c'est ce que produit ta config actuelle (le preset `Aura` de `@primeuix/themes` peut avoir été surchargé).

Procédure "capture-once" à faire **avant** de retirer PrimeNG :

1. Lancer l'app (`npm start`), sur n'importe quel écran de production
2. Dans la console navigateur :

   ```js
   const s = getComputedStyle(document.documentElement);
   const tokens = {};
   for (const prop of s) {
     if (prop.startsWith('--p-')) tokens[prop] = s.getPropertyValue(prop).trim();
   }
   console.log(JSON.stringify(tokens, null, 2));
   ```

3. Copier la sortie et l'injecter dans `_pharma-tokens.scss` sous forme `--p-xxx: valeur;` (une passe de transformation `sed` suffit).
4. Retirer les tokens **non-utilisés** dans le projet (rapprocher de l'inventaire §8.1) — pas la peine de figer les ~800 tokens de la palette Aura complète.

### 8.4 Chargement

Modifier `pharmaSmart-app/src/main/webapp/content/scss/vendor.scss` :

```scss
@import 'pharma-tokens';   // ← AVANT bootstrap et n'importe quel usage
@import 'bootstrap/scss/functions';
@import 'bootstrap/scss/variables';
// … existant
```

### 8.5 Alias vers les variables Bootstrap (optionnel)

Pour aller progressivement vers un vocabulaire cohérent Bootstrap, on peut aliaser :

```scss
:root {
  --bs-primary: var(--p-primary-color);
  --bs-body-color: var(--p-text-color);
  --bs-border-color: var(--p-surface-border);
  // …
}
```

Ainsi les composants natifs Bootstrap picorent la palette PharmaSmart, et à terme on peut inverser la relation (les `--p-*` deviennent des alias de `--bs-*`), puis retirer les `--p-*` complètement lorsqu'aucun consommateur n'y fait plus référence.

### 8.6 Icônes — `pi pi-*`

`primeicons` est une **font-icon CSS pure**, sans licence propriétaire, sans dépendance à `providePrimeNG`. Elle peut rester en dépendance directe même après retrait de `primeng`. Le projet a aussi déjà `@fortawesome/angular-fontawesome@5.1.0` installé et configuré. Voir **§9** pour le plan complet de migration des icônes.

### 8.7 Gestion des thèmes sombres (si applicable)

Aura supporte un mode sombre via `data-theme="dark"` sur `<html>`. Si utilisé dans l'app, capturer aussi la palette dark et l'ajouter dans le fichier de tokens :

```scss
:root[data-theme='dark'] {
  --p-surface-0: #0f172a;
  --p-text-color: #f1f5f9;
  // …
}
```

Sinon (mono-thème), l'ignorer.

### 8.8 Base actuelle et projection thème custom PharmaSmart

**Décision §14.5** — approche en deux temps.

#### 8.8.1 Court terme — conserver Bootswatch "yeti"

Le thème Bootstrap actuel est **Bootswatch yeti** (`bootswatch/dist/yeti/*`), chargé via `content/scss/vendor.scss`. Le préserver :

- **Aucune surprise visuelle** pendant la migration : les composants natifs Bootstrap (boutons, tables, forms, cartes) gardent leur look actuel
- Coexistence naturelle avec les tokens `--p-*` figés (§8.2)
- Les composants du DS (`AppButton`, `AppBadge`, etc.) s'appuient sur les classes Bootstrap standard — donc profitent automatiquement du look yeti
- Aucune tâche additionnelle en Phase 0-4

#### 8.8.2 Long terme — thème custom PharmaSmart

Une fois la migration PrimeNG terminée (post-Phase 4), démarrer un **chantier theming séparé** pour un thème identitaire PharmaSmart. Objectifs cibles :

- Palette différenciée reflétant l'identité pharmacie (vert médical / cyan sanitaire selon direction artistique)
- Vocabulaire de tokens propre : `--pharma-primary`, `--pharma-surface`, `--pharma-danger`, etc.
- Découplage : `--bs-*` et `--p-*` (survivants) deviennent des **alias** de `--pharma-*`, pas l'inverse
- Support optionnel du mode sombre (data-theme="dark")
- Un fichier `_pharma-theme.scss` remplace/étend Bootswatch yeti dans `vendor.scss`

#### 8.8.3 Structure cible

```scss
// content/scss/vendor.scss (état futur)
@import 'pharma-tokens';           // (§8.2) — figés depuis Aura, conservés
@import 'pharma-theme';            // 🆕 — palette PharmaSmart, remplace Bootswatch
@import 'bootstrap/scss/functions';
@import 'bootstrap/scss/variables';
@import 'bootstrap/scss/mixins';
// @import 'bootswatch/dist/yeti/…';  // retiré
@import 'bootstrap/scss/bootstrap';
```

Chantier hors périmètre de ce plan. **Prérequis** : direction artistique validée, sinon reporter.

#### 8.8.4 Compatibilité avec ce chantier

Le passage éventuel à un thème custom **ne casse aucune migration effectuée** par ce plan :

- Les composants du DS n'utilisent que des classes Bootstrap standard (`btn`, `card`, `table`, `form-control`, …)
- Les tokens `--p-*` restent définis via `_pharma-tokens.scss` — indépendants du thème sous-jacent
- Le thème custom se contente de redéfinir les `--bs-*` et éventuellement d'alias les `--p-*`

→ Aucune adhérence à Bootswatch dans le code Angular ; la substitution est purement CSS.

### 8.9 Calage visuel `@ng-select/ng-select` sur `p-select`

`@ng-select/ng-select` a par défaut une apparence différente de `p-select` : hauteur, radius, épaisseur de bordure, chevron, chips en mode multiple, focus ring. **Impératif** : quand un écran passe de `<p-select>` à `<app-select-search>` (ou `<app-multi-select>`), l'utilisateur ne doit **pas ressentir de rupture visuelle**.

#### 8.9.1 Stratégie

Créer un fichier de surcharges SCSS dédié `content/scss/_ng-select-pharma.scss`, chargé **après** `@ng-select/ng-select/themes/default.theme.css` (ou le thème Bootstrap 5 fourni par le package), qui aligne l'apparence sur celle actuellement produite par `p-select` (preset Aura).

Les surcharges pointent vers les tokens figés `--p-*` de §8.2 — ainsi, si les tokens évoluent (thème custom §8.8), `ng-select` suit automatiquement.

#### 8.9.2 Points de calage

| Zone | `p-select` (référence) | Cible pour ng-select |
|---|---|---|
| Hauteur input | ~38px (small: ~32px) | `.ng-select .ng-select-container` : `min-height: 38px` |
| Radius | `var(--p-border-radius)` (6px) | `.ng-select-container { border-radius: var(--p-border-radius); }` |
| Bordure | `1px solid var(--p-surface-border)` | `.ng-select-container { border-color: var(--p-surface-border); }` |
| Focus | Anneau primary `2px` | `.ng-select.ng-select-focused .ng-select-container { box-shadow: 0 0 0 2px var(--p-primary-200); border-color: var(--p-primary-500); }` |
| Placeholder | `color: var(--p-text-muted-color)` | `.ng-placeholder { color: var(--p-text-muted-color); }` |
| Chevron dropdown | Icône PrimeIcons `chevron-down` | `.ng-arrow-wrapper` : réécrire avec `content: '\ea1c'` (`pi-chevron-down`) via CSS `::before`, ou masquer + injecter `<i class="pi pi-chevron-down">` dans le template ng-select |
| Panneau dropdown | Fond blanc, ombre discrète, radius 6px | `.ng-dropdown-panel { border-radius: var(--p-border-radius); box-shadow: 0 4px 12px rgba(0,0,0,.08); border-color: var(--p-surface-border); }` |
| Option survolée | Fond `var(--p-primary-50)` | `.ng-option-marked { background: var(--p-primary-50); color: var(--p-text-color); }` |
| Option sélectionnée | Fond `var(--p-primary-100)`, texte foncé | `.ng-option-selected { background: var(--p-primary-100); color: var(--p-primary-800); }` |
| Chips (mode multiple) | Fond `var(--p-primary-100)`, radius 4px | `.ng-value { background: var(--p-primary-100); color: var(--p-primary-800); border-radius: 4px; padding: 2px 8px; }` |
| Chip close | Croix discrète | `.ng-value-icon { border-right: 1px solid var(--p-primary-200); &:hover { background: var(--p-primary-200); } }` |
| Clear all | `pi pi-times` gris | Idem chevron : injection PrimeIcons |
| Taille "small" | `.p-select-sm` (~32px) | `.ng-select.app-select-sm .ng-select-container { min-height: 32px; font-size: .875rem; }` |
| État désactivé | Opacité 0.5, fond gris | `.ng-select.ng-select-disabled .ng-select-container { background: var(--p-surface-50); opacity: 0.6; }` |
| État invalide (Angular Forms) | Bordure rouge | `.ng-select.ng-invalid.ng-touched .ng-select-container { border-color: var(--p-red-500); }` |

#### 8.9.3 Squelette du fichier de surcharges

```scss
// content/scss/_ng-select-pharma.scss
// Aligne @ng-select/ng-select sur l'apparence historique de p-select (preset Aura).
// Consomme les tokens figés dans _pharma-tokens.scss.

.ng-select {
  .ng-select-container {
    min-height: 38px;
    border: 1px solid var(--p-surface-border);
    border-radius: var(--p-border-radius);
    background: var(--p-surface-0);
    color: var(--p-text-color);
    transition: border-color .15s, box-shadow .15s;
  }

  &.ng-select-focused .ng-select-container {
    border-color: var(--p-primary-500);
    box-shadow: 0 0 0 2px var(--p-primary-200);
  }

  &.ng-select-disabled .ng-select-container {
    background: var(--p-surface-50);
    opacity: .6;
    cursor: not-allowed;
  }

  &.ng-invalid.ng-touched .ng-select-container {
    border-color: var(--p-red-500);
  }

  &.app-select-sm .ng-select-container {
    min-height: 32px;
    font-size: .875rem;
  }

  .ng-placeholder {
    color: var(--p-text-muted-color);
  }

  // Chips (mode multiple)
  .ng-value {
    background: var(--p-primary-100);
    color: var(--p-primary-800);
    border-radius: 4px;
    padding: 2px 8px;
    display: inline-flex;
    align-items: center;
    gap: .35rem;

    .ng-value-icon {
      color: var(--p-primary-700);
      padding: 0 4px;
      &:hover { background: var(--p-primary-200); }
    }
  }
}

.ng-dropdown-panel {
  border: 1px solid var(--p-surface-border);
  border-radius: var(--p-border-radius);
  box-shadow: 0 4px 12px rgba(0, 0, 0, .08);
  background: var(--p-surface-0);

  .ng-option {
    color: var(--p-text-color);
    padding: .5rem .75rem;

    &.ng-option-marked {
      background: var(--p-primary-50);
      color: var(--p-text-color);
    }

    &.ng-option-selected {
      background: var(--p-primary-100);
      color: var(--p-primary-800);
      font-weight: 500;
    }

    &.ng-option-disabled {
      color: var(--p-text-muted-color);
    }
  }
}
```

Chargement dans `content/scss/vendor.scss` :

```scss
@import 'pharma-tokens';                                    // §8.2
@import '@ng-select/ng-select/themes/default.theme.css';    // base ng-select
@import 'ng-select-pharma';                                 // 🆕 surcharges
@import 'bootstrap/scss/…';
```

#### 8.9.4 Icônes internes ng-select

`ng-select` utilise ses propres SVG pour chevron/clear/loading. Pour matcher visuellement `p-select` (PrimeIcons), deux options — trancher au moment de l'implémentation :

**A.** Masquer les SVG natifs et injecter des `<i class="pi …">` via le template PrimeNG-like du wrapper :
```html
<ng-select [items]="…" [(ngModel)]="value">
  <ng-template ng-header-tmp><!-- vide --></ng-template>
  <!-- pas de template natif pour chevron dans ng-select : surcharge CSS uniquement -->
</ng-select>
```
→ CSS : `.ng-arrow-wrapper .ng-arrow { display: none; }` + pseudo-élément `::after` avec `content: '\f078'` (unicode PrimeIcons chevron-down).

**B.** Garder les SVG natifs ng-select et ajuster leur `stroke`/`fill` via CSS pour les fondre dans la palette. Zéro effort, apparence "presque pareille".

Recommandation : **B** en Phase 1, **A** seulement si la revue design signale un écart perçu.

#### 8.9.5 Effort

Estimation ajoutée à Phase 1 : **1 j** de calage CSS + revue visuelle sur 3-4 écrans emblématiques (POS caisse, formulaire produit, filtres rapports).

---

## 9. Migration icônes — PrimeIcons → FontAwesome

### 9.1 État des lieux

Le projet contient **1668 occurrences** de la classe `pi pi-*` réparties sur **172 icônes distinctes**. Le top 10 couvre ~60 % des usages :

| Icône | Occurrences |
|---|---|
| `pi pi-times` | 160 |
| `pi pi-check` | 136 |
| `pi pi-search` | 114 |
| `pi pi-info-circle` | 104 |
| `pi pi-check-circle` | 99 |
| `pi pi-inbox` | 98 |
| `pi pi-exclamation-triangle` | 96 |
| `pi pi-trash` | 81 |
| `pi pi-plus` | 70 |
| `pi pi-spin` (animation) | 67 |

Le projet possède déjà `@fortawesome/angular-fontawesome@5.1.0`, `@fortawesome/fontawesome-svg-core@7.2.0`, `@fortawesome/free-solid-svg-icons@7.2.0`. Le pattern d'enregistrement est en place :

- Config d'icônes centralisée : `app/config/font-awesome-icons.ts` (~50 icônes déjà enregistrées)
- Registre : `FaIconLibrary.addIcons(...)` appelé dans `app/app.component.ts:21`
- Composant Angular : `<fa-icon icon="..." />`

### 9.2 Décision — Option B retenue

> ✅ **Décidé** (validé par l'équipe) : **Option B** — conserver `primeicons` comme dépendance CSS isolée après retrait de `primeng`. Pas de mass rewrite planifié. L'Option A reste documentée en §9.3 comme playbook activable ultérieurement si nécessaire.

**Option B — Garder `primeicons` en dépendance isolée.**

- Zéro modification des 1668 sites d'appel
- `primeicons` reste dans `package.json`, chargé via `content/scss/vendor.scss` — c'est un package CSS-only sans dépendance runtime PrimeNG
- Cohabitation avec FontAwesome (déjà utilisé sur ~50 icônes)
- Effort : nul
- Coût : dette d'incohérence stylistique (deux systèmes d'icônes) et ~35 KB de font conservés
- Nouveaux développements : les 172 icônes actuellement utilisées via `pi pi-*` restent valides. Les nouvelles icônes peuvent utiliser au choix `pi pi-*` (cohérence avec l'existant) ou `<fa-icon>` (voie stratégique long terme). Aligner par équipe.

**Option A (long terme, propre — non retenue à ce stade) — Migration complète vers FontAwesome.**

- Tous les `<i class="pi pi-X">` deviennent `<fa-icon icon="X" />`
- Tous les strings TS `icon = 'pi pi-X'` deviennent `icon = faX` (imports à générer)
- Retrait de `primeicons`, `bootstrap-icons` du bundle
- Effort : ~5-10 jours (mass rewrite + tests visuels sur tous les écrans)
- Gain : un seul système d'icônes, SVG (crisper), bundle réduit d'environ 30-50 KB

### 9.3 Playbook Option A — mass rewrite (archivé)

> ⚠ Non-activé — conservé pour référence future si l'équipe change de décision. Exécuter la procédure suivante seulement après Phase 3.

#### 9.3.1 Table de correspondance des noms

FontAwesome 6/7 utilise des noms différents de PrimeIcons pour ~30 % des icônes. Table à figer dès le début de la migration.

| PrimeIcons | FontAwesome (free-solid) | Notes |
|---|---|---|
| `pi-times` | `xmark` | (v5: `times` — déprécié) |
| `pi-check` | `check` | Identique |
| `pi-search` | `magnifying-glass` | (v5: `search`) |
| `pi-info-circle` | `circle-info` | (v5: `info-circle`) |
| `pi-check-circle` | `circle-check` | |
| `pi-exclamation-triangle` | `triangle-exclamation` | (v5: `exclamation-triangle`) |
| `pi-exclamation-circle` | `circle-exclamation` | |
| `pi-times-circle` | `circle-xmark` | |
| `pi-inbox` | `inbox` | Identique |
| `pi-trash` | `trash-can` | (ou `trash`) |
| `pi-plus` | `plus` | Identique |
| `pi-minus` | `minus` | Identique |
| `pi-box` | `box` | Identique |
| `pi-pencil` | `pen` | (ou `pencil`) |
| `pi-chart-bar` | `chart-column` | (v5: `chart-bar`) |
| `pi-chart-line` | `chart-line` | Identique |
| `pi-chart-pie` | `chart-pie` | Identique |
| `pi-shopping-cart` | `cart-shopping` | (v5: `shopping-cart`) |
| `pi-file-pdf` | `file-pdf` | Identique |
| `pi-file-excel` | `file-excel` | Identique |
| `pi-file-edit` | `file-pen` | |
| `pi-file` | `file` | Identique |
| `pi-wallet` | `wallet` | Identique |
| `pi-building` | `building` | Identique |
| `pi-tag` | `tag` | Identique |
| `pi-clock` | `clock` | Identique |
| `pi-calendar` | `calendar` | Identique |
| `pi-users` | `users` | Identique |
| `pi-user` | `user` | Identique |
| `pi-money-bill` | `money-bill` | Identique |
| `pi-lock` | `lock` | Identique |
| `pi-refresh` | `arrows-rotate` | (v5: `sync-alt`) |
| `pi-sync` | `arrows-rotate` | |
| `pi-list` | `list` | Identique |
| `pi-arrow-left` | `arrow-left` | Identique |
| `pi-truck` | `truck` | Identique |
| `pi-ban` | `ban` | Identique |
| `pi-shield` | `shield` | Identique |
| `pi-calculator` | `calculator` | Identique |
| `pi-percentage` | `percent` | (v5: `percentage`) |
| `pi-print` | `print` | Identique |
| `pi-cog` | `gear` | (v5: `cog`) |
| `pi-eye` | `eye` | Identique |
| `pi-download` | `download` | Identique |
| `pi-upload` | `upload` | Identique |
| `pi-filter` | `filter` | Identique |
| `pi-bars` | `bars` | Identique |
| `pi-home` | `house` | (v5: `home`) |
| `pi-bell` | `bell` | Identique |
| `pi-envelope` | `envelope` | Identique |
| `pi-question-circle` | `circle-question` | |
| `pi-external-link` | `up-right-from-square` | |
| `pi-copy` | `copy` | Identique |
| `pi-save` | `floppy-disk` | (v5: `save`) |
| `pi-sign-out` | `right-from-bracket` | (v5: `sign-out-alt`) |
| `pi-sign-in` | `right-to-bracket` | |
| `pi-cloud` | `cloud` | Identique |
| `pi-database` | `database` | Identique |
| `pi-credit-card` | `credit-card` | Identique |
| … | … | Compléter au cas par cas via [fontawesome.com/search](https://fontawesome.com/search?o=r&m=free) |

**Icônes sans équivalent direct** (choix design à faire) :
- `pi-inbox` → `inbox` OK
- `pi-th-large` → `table-cells-large`
- `pi-th-list` → `table-list`
- `pi-mobile` → `mobile-screen`
- Cas exotiques : `pi-github`, `pi-google`, `pi-facebook` → dispo dans `@fortawesome/free-brands-svg-icons` (à installer si nécessaire)

#### 9.3.2 Cas d'usage à traiter

**Cas 1 — HTML statique** :
```html
<!-- AVANT -->
<i class="pi pi-check text-success"></i>
<!-- APRÈS -->
<fa-icon icon="check" class="text-success" />
```

**Cas 2 — Animation `pi-spin`** :
```html
<!-- AVANT -->
<i class="pi pi-spin pi-spinner"></i>
<!-- APRÈS -->
<fa-icon icon="spinner" [spin]="true" />
```

**Cas 3 — Icône dans un `<button>` PrimeNG (`p-button icon="pi pi-check"`)** :
```html
<!-- AVANT (p-button déjà migré vers app-button) -->
<app-button icon="pi pi-check" label="Valider" />
<!-- APRÈS -->
<app-button icon="check" label="Valider" />
<!-- (revoir l'input du composant AppButton pour qu'il émette <fa-icon icon="X"> au lieu de <i class="pi pi-X">) -->
```

**Cas 4 — String dynamique en TS** :
```ts
// AVANT
icon = signal<string>('pi pi-info-circle');
// APRÈS
import { faCircleInfo, IconDefinition } from '@fortawesome/free-solid-svg-icons';
icon = signal<IconDefinition>(faCircleInfo);
```

Puis dans le template :
```html
<!-- AVANT -->
<i [class]="icon()"></i>
<!-- APRÈS -->
<fa-icon [icon]="icon()" />
```

**Cas 5 — Icône passée en input à un composant maison** :

Standardiser l'API des composants du DS pour accepter les deux formes pendant la transition :

```ts
// Dans AppButton, AppBadge, etc.
readonly icon = input<string | IconProp>('');
```

`FaIconComponent` accepte à la fois un `string` (résolu via `FaIconLibrary`) et un `IconDefinition` — donc `<fa-icon [icon]="icon()" />` fonctionne pour les deux.

#### 9.3.3 Automatisation — script de rewrite

Un script Node fait la substitution HTML massive. Pattern général :

```js
// scripts/migrate-primeicons-to-fa.js
const MAPPING = {
  'pi-times': 'xmark',
  'pi-check': 'check',
  'pi-search': 'magnifying-glass',
  // … la table §9.3.1
};

// Regex : <i class="... pi pi-X ...">
// →     : <fa-icon icon="Y" class="autres classes" />
```

Le script :
1. Parse chaque fichier `.html` avec un mini AST HTML (`node-html-parser`) plutôt qu'un regex fragile
2. Détecte chaque `<i>` dont la classe contient `pi pi-<name>` et `.pi` seul
3. Extrait `<name>`, extrait les autres classes (excluant `pi` et `pi-<name>`), extrait `pi-spin` en `[spin]="true"`
4. Réémet `<fa-icon icon="<name-mappé>" class="<autres classes>" />`
5. Ajoute automatiquement l'icône dans `app/config/font-awesome-icons.ts` (via un ensemble collecté)

Pour les fichiers TS avec strings dynamiques, deuxième script :
1. Détecte `'pi pi-<name>'` littéral
2. Le remplace par l'import `faName` correspondant
3. Ajoute l'import en tête de fichier
4. Retire l'icône du string, met à jour la déclaration de type (`string` → `IconDefinition`)

Prévoir passage manuel en 2ᵉ passe pour les cas non-triviaux (concaténations, ternaires).

#### 9.3.4 Validation

- Compter les résidus : `grep -rn "pi pi-" pharmaSmart-app/src/main/webapp` doit retomber à zéro après la passe
- `npm run webapp:build:prod` sans erreur
- Revue visuelle : lancer l'app, vérifier 5-6 écrans emblématiques (dashboard, POS, formulaire produit, ticketing, réception, rapport financier)
- Retirer `primeicons` de `package.json` + import de `vendor.scss`

### 9.4 Impact concret sur le reste du plan

Décision Option B → ajustements suivants :

- **§3 (table de correspondance)** — inchangée : `pi pi-*` reste utilisable partout
- **§4 (arborescence DS)** — les composants du DS (`AppButton`, `AppBadge`, etc.) exposent `icon: input<string>('')` typé simple `string`, et rendent `<i class="{{ icon() }}"></i>`. Pas de complication `IconProp | string`
- **§8.6** — `primeicons` reste dans `package.json` en Phase 4 ; `@primeuix/themes` seul dégage
- **§12 (Timeline)** — aucun changement, aucune phase icônes à budgéter
- **§13 (Livrables)** — retirer le script `migrate-primeicons-to-fa.js` de la liste des livrables (voir mise à jour §13)

---

## 10. Risques et compensations

| Risque | Probabilité | Impact | Compensation |
|---|---|---|---|
| Divergence visuelle sur les écrans migrés | Haute | Moyen | Screenshots avant/après par PR ; hotfix CSS ciblé |
| `p-table` avec édition inline (`p-celleditor`) coûteux à réécrire | Moyenne | Haut | Reproduire dans `AppTable` OU downscoper avec le PO — pas de bascule automatique vers AG Grid (cf. §4.2 cohabitation) |
| `p-autocomplete` / `p-select` — signatures différentes de `ng-select` | Certaine | Moyen | Wrappers `AppSelectSearch` / `AppMultiSelect` isolent les call sites ; calage visuel §8.9 |
| Régression clavier / accessibilité | Moyenne | Haut | Tests manuels a11y sur les 3-4 écrans critiques (POS notamment) |
| Bundle Bootstrap doublé (déjà chargé) si mauvais tree-shaking | Basse | Bas | Auditer avec `webpack-bundle-analyzer` après Phase 4 |
| Timing frappe / focus sur POS de caisse (Enter → confirm) | Certaine | Haut | La garde `ngbAutofocus` + `ready = true` après 200ms de `NgbConfirmDialog` est le pattern à généraliser |
| Perte d'un composant PrimeNG spécifique sans équivalent (`p-cascadeselect`, `p-orderlist`…) | Basse | Bas | Cas par cas — envisager `ng-select`, `cdk-drag-drop` |

---

## 11. Ce qui **ne change pas**

- **AG Grid** (`ag-grid-angular`) — pas de migration, indépendant.
- **Chart.js** — même remarque.
- **@ng-bootstrap/ng-bootstrap** — déjà en 21 (peer Angular 22), la migration en amplifie l'usage.
- **PrimeIcons** (`primeicons`) — dépendance CSS seule, envisagée à retirer en Phase 4 mais non-bloquante.
- **@ngrx/signals** — inchangé.
- **@ngx-translate** — inchangé.

---

## 12. Livrables

- ✅ Ce plan (`docs/PLAN-MIGRATION-PRIMENG-VERS-NGBOOTSTRAP.md`)
- 🔲 Design System complet sous `app/shared/ui/*` (Phase 1)
- 🔲 **`_pharma-tokens.scss`** — snapshot des 87 tokens `--p-*` extraits de l'app en cours (Phase 0, cf. §8.3)
- 🔲 Guide "avant/après" — cheat-sheet des remplacements les plus fréquents pour les développeurs pendant les migrations d'écrans (à ajouter à `app/shared/ui/README.md`)
- 🔲 Tests unitaires Jest pour chaque composant du DS
- 🔲 Rapport final : bundle size avant/après, taux d'adoption, incidents constatés

---

## 13. Timeline indicative

| Phase | Durée | Fenêtre |
|---|---|---|
| Phase 0 — Préparation | 1 sprint | S+0 → S+2 |
| Phase 1 — Design System | 2 sprints | S+2 → S+6 |
| Phase 2 — Écrans faible risque | 2-3 sprints | S+6 → S+12 |
| Phase 3 — Écrans complexes | 4-6 sprints | S+12 → S+24 |
| Phase 4 — Nettoyage & retrait PrimeNG | 1 sprint | S+24 → S+26 |
| **Total** | **~6 mois** | |

Migration opportuniste : chaque équipe peut prendre en charge la migration d'un écran qu'elle touche déjà pour une autre raison, ce qui réduit le temps calendaire réel.

---

## 14. Journal des décisions

**Toutes les décisions sont tranchées** (validées par l'équipe front). Ce journal sert de référence historique — chaque point liste la question initiale, la décision, et le raisonnement.

1. ~~PrimeIcons → FontAwesome (§9)~~ — ✅ **Tranché : Option B**. `primeicons` reste en dépendance CSS isolée après retrait de `primeng`. Pas de mass rewrite au programme. Playbook Option A archivé en §9.3 pour référence future.
2. ~~`ng-select` vs maison pour `p-multiselect`~~ — ✅ **Tranché : `@ng-select/ng-select` retenu**. Utilisé aussi pour `p-select` (avec search) et éventuellement `p-autocomplete` — même lib pour toutes les listes déroulantes riches. Deux wrappers `AppSelectSearch` (single) et `AppMultiSelect` (multiple) isolent la dépendance. Effort en Phase 1 : ~1.75 j au lieu des 2.5 j initialement estimés.
3. ~~AG Grid vs `AppTable` — seuil de choix~~ — ✅ **Tranché : cohabitation identique à l'existant**. `AppTable` remplace `p-table` uniquement ; AG Grid reste sur son périmètre actuel. Pas de bascule croisée pendant le chantier.
4. ~~Tokens `--p-*` — déprécier ou conserver ?~~ — ✅ **Tranché : conservation permanente**. Les ~380 références aux tokens `--p-*` restent en place indéfiniment ; aucune campagne de rewrite. Le préfixe `--p-*` devient une simple convention de nommage sans plus de lien avec PrimeNG (analogue à `--bs-*` = "Bootstrap"). Quand le thème custom PharmaSmart arrivera (§8.8.2), il introduira `--pharma-*` comme source-de-vérité et les `--p-*` deviendront des **alias** vers ces nouveaux tokens — les call sites existants continuent de fonctionner. Rationale : effort/gain défavorable (0 valeur métier pour 1-2 semaines de rewrite risqué).
5. ~~Theming — Bootswatch "yeti" vs thème custom~~ — ✅ **Tranché en 2 temps** : (1) **court terme** : conserver Bootswatch "yeti" comme base — aligné avec l'existant, aucune surprise visuelle ; (2) **projection** : préparer une bascule vers un thème PharmaSmart custom (branding pharmacie, palette différenciée, tokens `--pharma-*`) une fois la migration PrimeNG terminée. Voir §8.8.
6. ~~Icônes dans les composants du DS — wrapper `<app-icon />` ?~~ — ✅ **Tranché : sans objet**. La décision Option B pour `primeicons` (§9.2) tranche : les composants du DS exposent `icon: input<string>('')` et rendent directement `<i class="{{ icon() }}"></i>`. Pas de wrapper `<app-icon />`, aucune abstraction supplémentaire. Cohérence avec les 1668 usages `pi pi-*` existants dans le code.
