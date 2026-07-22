import { NgTemplateOutlet } from '@angular/common';
import { Component, TemplateRef, ViewEncapsulation, computed, contentChild, input, linkedSignal, model, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbPagination } from '@ng-bootstrap/ng-bootstrap';

import { SelectComponent } from '../select/select.component';

import { AppTableColumn, AppTableHost, AppTableLazyLoadEvent } from './table.types';

/**
 * Tableau du Design System — remplace `p-table`. Rend une `<table>` Bootstrap 5.
 *
 * L'API reprend délibérément le vocabulaire de `p-table` (`value`, `paginator`, `rows`,
 * `lazy`, `dataKey`, templates `#header` / `#body` / `#emptymessage`…) pour que la
 * substitution sur les ~144 écrans concernés se limite au nom de la balise.
 *
 * **Deux modes.** En client-side (défaut), la table trie et pagine elle-même le tableau
 * `value`. En `lazy`, elle n'affiche que ce qu'on lui donne et signale ses besoins via
 * `(onLazyLoad)` — c'est au parent de recharger.
 *
 * Prend aussi en charge le regroupement de lignes (`groupRowsBy` + template `#groupheader`,
 * équivalent de `rowGroupMode="subheader"`), les colonnes figées (directive
 * `[appFrozenColumn]`, équivalent de `pFrozenColumn`) et l'édition en ligne (composant
 * `<app-editable-cell>`, équivalent de `pEditableColumn` / `p-celleditor`).
 *
 * **Master/detail** : `[appRowToggler]` (posé sur un bouton de la ligne) déplie le
 * template `#expandedrow`, équivalent de `pRowToggler`. Sa `<td>` hérite d'un padding à
 * zéro (convention partagée par les écrans encore sur `p-table`) : pour un contenu qui a
 * besoin d'air (ex. un `<app-card>` de détail), l'envelopper dans un
 * `<div class="app-table__expanded-content">` plutôt que de le poser sur la `<td>` elle-même.
 *
 * @example
 * <app-data-table [value]="produits()" [paginator]="true" [rows]="10" [stripedRows]="true">
 *   <ng-template #header>
 *     <tr><th appSortableHeader="libelle">Libellé</th><th>Prix</th></tr>
 *   </ng-template>
 *   <ng-template #body let-produit>
 *     <tr><td>{{ produit.libelle }}</td><td>{{ produit.prix }}</td></tr>
 *   </ng-template>
 * </app-data-table>
 */
@Component({
  selector: 'app-data-table',
  imports: [NgTemplateOutlet, FormsModule, NgbPagination, SelectComponent],
  providers: [{ provide: AppTableHost, useExisting: DataTableComponent }],
  templateUrl: './data-table.component.html',
  styleUrl: './data-table.component.scss',
  // `#header`/`#body`/`#groupheader`… sont déclarés dans le template de l'écran appelant,
  // pas dans celui-ci : rendus via `ngTemplateOutlet`, leurs éléments portent l'attribut de
  // scoping du composant appelant, pas celui de `DataTableComponent`. Sous encapsulation
  // Emulated, les règles ci-dessous (`.app-sortable`, `.app-frozen-column`, colonne figée,
  // en-tête collant…) ne les atteindraient donc jamais. Même choix que `pharma-date-picker`.
  encapsulation: ViewEncapsulation.None,
})
export class DataTableComponent<T = Record<string, unknown>> implements AppTableHost {
  readonly value = input.required<readonly T[]>();

  /** Colonnes transmises au template `#header` ; facultatif si le `<thead>` est écrit à la main. */
  readonly columns = input<readonly AppTableColumn<T>[]>([]);

  readonly loading = input<boolean>(false);

  readonly paginator = input<boolean>(false);

  readonly rows = model<number>(10);

  readonly rowsPerPageOptions = input<readonly number[]>([]);

  /** Nombre total côté serveur. Requis en mode `lazy`. */
  readonly totalRecords = input<number | undefined>(undefined);

  readonly lazy = input<boolean>(false);

  /** Index de la première ligne affichée. Piloté par le parent en mode `lazy`. */
  readonly first = model<number>(0);

  /** Propriété identifiante — sert au suivi des lignes, à la sélection et au dépliage. */
  readonly dataKey = input<string>('');

  readonly selectionMode = input<'single' | 'multiple' | null>(null);

  readonly selection = model<T | T[] | null>(null);

  readonly stripedRows = input<boolean>(false);

  readonly showGridlines = input<boolean>(false);

  readonly size = input<'small' | 'normal' | 'large'>('normal');

  /** Fige l'en-tête et fait défiler le corps ; à combiner avec `scrollHeight`. */
  readonly scrollable = input<boolean>(false);

  readonly scrollHeight = input<string>('');

  readonly showCurrentPageReport = input<boolean>(false);

  /** Gabarit du compteur. Jetons `{first}`, `{last}`, `{totalRecords}`, comme `p-table`. */
  readonly currentPageReportTemplate = input<string>('Affichage de {first} à {last} sur {totalRecords} entrées');

  /** Texte affiché quand aucun template `#emptymessage` n'est fourni. */
  readonly emptyMessage = input<string>('Aucune donnée');

  readonly onLazyLoad = output<AppTableLazyLoadEvent>();

  readonly onPage = output<{ first: number; rows: number }>();

  readonly onSort = output<{ field: string; order: 1 | -1 }>();

  readonly onRowSelect = output<{ data: T; originalEvent: Event }>();

  readonly onRowUnselect = output<{ data: T; originalEvent: Event }>();

  protected readonly headerTemplate = contentChild<TemplateRef<unknown>>('header');
  protected readonly bodyTemplate = contentChild<TemplateRef<unknown>>('body');
  protected readonly footerTemplate = contentChild<TemplateRef<unknown>>('footer');
  protected readonly captionTemplate = contentChild<TemplateRef<unknown>>('caption');
  protected readonly emptyMessageTemplate = contentChild<TemplateRef<unknown>>('emptymessage');
  protected readonly expandedRowTemplate = contentChild<TemplateRef<unknown>>('expandedrow');
  protected readonly groupHeaderTemplate = contentChild<TemplateRef<unknown>>('groupheader');

  /**
   * Propriété de regroupement — reprend `groupRowsBy` de `p-table` (mode `rowGroupMode="subheader"`).
   * Suppose les lignes déjà contiguës sur ce champ (via `initialSortField` ou un tri serveur en
   * mode `lazy`) : comme `p-table`, la table ne re-trie pas pour regrouper, elle détecte juste
   * les ruptures de valeur dans l'ordre déjà affiché.
   */
  readonly groupRowsBy = input<string>('');

  /** Tri initial, avant toute interaction — reprend `sortField`/`sortOrder` de `p-table`. */
  readonly initialSortField = input<string>('');
  readonly initialSortOrder = input<1 | -1>(1);

  // `linkedSignal`, pas `signal(this.initialSortField())` : un signal d'entrée n'est
  // garanti résolu qu'après la construction, un `signal()` initialisé dans un champ de
  // classe capturerait sa valeur par défaut. `linkedSignal` seed correctement depuis
  // l'entrée tout en restant réinscriptible ensuite par `toggleSort`.
  private readonly _sortField = linkedSignal<string>(() => this.initialSortField());
  private readonly _sortOrder = linkedSignal<1 | -1>(() => this.initialSortOrder());
  private readonly expandedKeys = signal<ReadonlySet<unknown>>(new Set());

  readonly sortField = this._sortField.asReadonly();
  readonly sortOrder = this._sortOrder.asReadonly();

  /**
   * Champs inspectés par `filterGlobal`. Vide = toutes les propriétés de la ligne.
   * Reprend `globalFilterFields` de `p-table`.
   */
  readonly globalFilterFields = input<readonly string[]>([]);

  /** Total des enregistrements : fourni par le serveur en `lazy`, déduit sinon. */
  protected readonly recordCount = computed(() => this.totalRecords() ?? this.filteredRows().length);

  protected readonly pageCount = computed(() => (this.rows() > 0 ? Math.ceil(this.recordCount() / this.rows()) : 0));

  protected readonly currentPage = computed(() => (this.rows() > 0 ? Math.floor(this.first() / this.rows()) + 1 : 1));

  /** Terme du filtre global, posé par `filterGlobal()`. */
  private readonly globalFilter = signal<string>('');

  private readonly filteredRows = computed(() => {
    const rows = this.value();
    const term = this.globalFilter().trim().toLowerCase();

    // En `lazy`, `value` est la page renvoyée par le serveur : filtrer ici masquerait des
    // lignes de la page courante sans toucher aux autres, et le total afficherait un
    // décompte faux. Le filtrage doit alors être fait côté serveur.
    if (!term || this.lazy()) {
      return rows;
    }

    const fields = this.globalFilterFields();
    return rows.filter(row => {
      const record = row as Record<string, unknown>;
      const keys = fields.length ? fields : Object.keys(record);
      return keys.some(key => String(record[key] ?? '').toLowerCase().includes(term));
    });
  });

  private readonly sortedRows = computed(() => {
    const rows = this.filteredRows();
    const field = this._sortField();
    // En `lazy`, le tri est fait par le serveur : retrier ici mélangerait la page courante.
    if (this.lazy() || !field) {
      return rows;
    }
    const order = this._sortOrder();
    return [...rows].sort((left, right) => this.compare(this.readField(left, field), this.readField(right, field)) * order);
  });

  protected readonly displayedRows = computed(() => {
    const rows = this.sortedRows();
    // En `lazy`, `value` est déjà la page courante.
    if (this.lazy() || !this.paginator()) {
      return rows;
    }
    const start = this.first();
    return rows.slice(start, start + this.rows());
  });

  /**
   * Colspan du message « vide » et de la ligne de chargement.
   *
   * La quasi-totalité des écrans écrit son `<thead>` à la main via le template `#header`
   * plutôt que de fournir `[columns]` — `columns()` est alors vide. Un repli à `1` posait
   * le message dans la seule première colonne au lieu de l'étaler sur toute la largeur du
   * tableau. Les navigateurs plafonnent un `colspan` excessif au nombre réel de colonnes
   * de la ligne, donc une valeur large ici couvre le cas générique sans obliger chaque
   * écran à déclarer `[columns]`.
   */
  protected readonly columnCount = computed(() => this.columns().length || 100);

  protected readonly tableClasses = computed(() => {
    const classes = ['table', 'app-table'];
    if (this.stripedRows()) classes.push('table-striped');
    if (this.showGridlines()) classes.push('table-bordered');
    if (this.selectionMode()) classes.push('table-hover');
    if (this.size() === 'small') classes.push('table-sm');
    return classes.join(' ');
  });

  protected readonly currentPageReport = computed(() => {
    const total = this.recordCount();
    if (total === 0) {
      return this.currentPageReportTemplate().replace('{first}', '0').replace('{last}', '0').replace('{totalRecords}', '0');
    }
    const first = this.first() + 1;
    const last = Math.min(this.first() + this.rows(), total);
    return this.currentPageReportTemplate()
      .replace('{first}', String(first))
      .replace('{last}', String(last))
      .replace('{totalRecords}', String(total));
  });

  // --- AppTableHost : consommé par [appSortableHeader] et [appRowToggler] ---

  toggleSort(field: string): void {
    // Même champ → on inverse le sens ; nouveau champ → tri ascendant.
    const order: 1 | -1 = this._sortField() === field && this._sortOrder() === 1 ? -1 : 1;
    this._sortField.set(field);
    this._sortOrder.set(order);
    // Un changement de tri renvoie à la première page, sinon on regarde une page vide.
    this.first.set(0);

    this.onSort.emit({ field, order });
    this.emitLazyLoad();
  }

  isExpanded(row: unknown): boolean {
    return this.expandedKeys().has(this.keyOf(row));
  }

  toggleRow(row: unknown): void {
    const key = this.keyOf(row);
    this.expandedKeys.update(keys => {
      const next = new Set(keys);
      if (!next.delete(key)) {
        next.add(key);
      }
      return next;
    });
  }

  // --- Interactions ---

  protected goToPage(page: number): void {
    const first = (page - 1) * this.rows();
    this.first.set(first);
    this.onPage.emit({ first, rows: this.rows() });
    this.emitLazyLoad();
  }

  protected changePageSize(value: unknown): void {
    const size = Number(value);
    if (!size) {
      return;
    }
    this.rows.set(size);
    // Changer la taille de page invalide l'offset courant.
    this.first.set(0);
    this.onPage.emit({ first: 0, rows: size });
    this.emitLazyLoad();
  }

  /** Appelé par `[appSelectableRow]`, posée sur le `<tr>` du template `#body`. */
  selectRow(row: unknown, event: Event): void {
    const mode = this.selectionMode();
    if (!mode) {
      return;
    }
    const typed = row as T;

    if (mode === 'single') {
      const alreadySelected = this.isSelected(typed);
      this.selection.set(alreadySelected ? null : typed);
      (alreadySelected ? this.onRowUnselect : this.onRowSelect).emit({ data: typed, originalEvent: event });
      return;
    }

    const current = Array.isArray(this.selection()) ? [...(this.selection() as T[])] : [];
    const index = current.findIndex(item => this.keyOf(item) === this.keyOf(typed));
    if (index === -1) {
      current.push(typed);
      this.onRowSelect.emit({ data: typed, originalEvent: event });
    } else {
      current.splice(index, 1);
      this.onRowUnselect.emit({ data: typed, originalEvent: event });
    }
    this.selection.set(current);
  }

  /**
   * Bascule une ligne, appelée par `[appRowCheckbox]`.
   *
   * Distincte de `selectRow` : celle-ci n'agit qu'en présence d'un `selectionMode` et
   * émet `onRowSelect`/`onRowUnselect`, car elle traduit un clic sur la ligne. Une case à
   * cocher est explicite — elle doit fonctionner même sans `selectionMode`, sinon poser
   * une colonne de sélection obligerait à activer aussi la sélection au clic.
   */
  toggleSelection(row: unknown): void {
    const typed = row as T;
    const current = Array.isArray(this.selection()) ? [...(this.selection() as T[])] : [];
    const index = current.findIndex(item => this.keyOf(item) === this.keyOf(typed));

    if (index === -1) {
      current.push(typed);
    } else {
      current.splice(index, 1);
    }

    this.selection.set(current);
  }

  /**
   * Filtre les lignes sur un terme libre — remplace `filterGlobal` de `p-table`.
   *
   * Appelable depuis le template via une référence : `#table` puis
   * `table.filterGlobal($any($event.target).value)`.
   *
   * Le second paramètre de `p-table` (`'contains'`) n'est pas repris : c'était le seul
   * mode utilisé dans l'application, et il est ici le comportement unique.
   *
   * ⚠ Sans effet en mode `lazy` : la table ne détient que la page courante, filtrer
   * localement masquerait des lignes sans toucher aux autres et fausserait le total.
   */
  filterGlobal(term: string): void {
    this.globalFilter.set(term ?? '');
    // Un filtre qui rétrécit le jeu doit ramener à la première page.
    this.first.set(0);
  }

  isAllSelected(): boolean {
    const rows = this.value();
    if (!rows.length) {
      return false;
    }
    return rows.every(row => this.isSelected(row));
  }

  isPartiallySelected(): boolean {
    const rows = this.value();
    return rows.some(row => this.isSelected(row)) && !this.isAllSelected();
  }

  /**
   * Porte sur les lignes **affichées**, pas sur l'ensemble du jeu de données : en mode
   * `lazy`, la table ne connaît que la page courante. C'est aussi le comportement de
   * `p-tableheadercheckbox`.
   */
  toggleAll(): void {
    this.selection.set(this.isAllSelected() ? [] : [...this.value()]);
  }

  isSelected(row: unknown): boolean {
    const selection = this.selection();
    if (selection === null || selection === undefined) {
      return false;
    }
    const key = this.keyOf(row);
    return Array.isArray(selection) ? selection.some(item => this.keyOf(item) === key) : this.keyOf(selection) === key;
  }

  protected trackRow(index: number, row: T): unknown {
    return this.dataKey() ? this.keyOf(row) : index;
  }

  /** Vrai si `row` ouvre un nouveau groupe — sa valeur de `groupRowsBy` diffère de la ligne précédente. */
  protected isGroupHeader(row: T, index: number): boolean {
    const field = this.groupRowsBy();
    if (!field) {
      return false;
    }
    if (index === 0) {
      return true;
    }
    const previous = this.displayedRows()[index - 1];
    return this.readField(row, field) !== this.readField(previous, field);
  }

  // --- Utilitaires ---

  private emitLazyLoad(): void {
    if (!this.lazy()) {
      return;
    }
    this.onLazyLoad.emit({
      first: this.first(),
      rows: this.rows(),
      sortField: this._sortField() || undefined,
      sortOrder: this._sortOrder(),
    });
  }

  /** Identifiant de ligne : la propriété `dataKey` si elle est définie, l'objet lui-même sinon. */
  private keyOf(row: unknown): unknown {
    const key = this.dataKey();
    return key && row && typeof row === 'object' ? (row as Record<string, unknown>)[key] : row;
  }

  private readField(row: T, field: string): unknown {
    return row && typeof row === 'object' ? (row as Record<string, unknown>)[field] : undefined;
  }

  private compare(left: unknown, right: unknown): number {
    if (left === right) return 0;
    if (left === null || left === undefined) return -1;
    if (right === null || right === undefined) return 1;
    if (typeof left === 'number' && typeof right === 'number') {
      return left - right;
    }
    // `localeCompare` gère les accents, indispensable sur des libellés français.
    return String(left).localeCompare(String(right), 'fr', { numeric: true });
  }
}
