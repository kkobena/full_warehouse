import { NgClass, NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  TemplateRef,
  computed,
  contentChild,
  contentChildren,
  effect,
  input,
  model,
  output,
  signal,
  viewChildren,
} from '@angular/core';

import { DataGridCellDirective } from './data-grid-cell.directive';
import { DataGridDetailDirective } from './data-grid-detail.directive';
import {
  AppDataGridCellContext,
  AppDataGridCellEditingStartedEvent,
  AppDataGridCellEditingStoppedEvent,
  AppDataGridCellTemplateContext,
  AppDataGridCellEvent,
  AppDataGridCellValueChangedEvent,
  AppDataGridColumn,
  AppDataGridColumnResizeEvent,
  AppDataGridDetailContext,
  AppDataGridDetailTemplateContext,
  AppDataGridDetailToggleEvent,
  AppDataGridEditStartSource,
  AppDataGridEditStopSource,
  AppDataGridRowContext,
  AppDataGridRowEvent,
  AppDataGridSelectionMode,
  AppDataGridSize,
  AppDataGridSortEvent,
  AppDataGridSortState,
} from './data-grid.types';

interface ColumnResizeState {
  readonly columnId: string;
  readonly pointerId: number;
  readonly startX: number;
  readonly startWidth: number;
}

interface ActiveCell {
  readonly rowKey: PropertyKey;
  readonly columnId: string;
}

interface EditingCell<T> extends ActiveCell {
  readonly row: T;
  readonly rowIndex: number;
  readonly column: AppDataGridColumn<T>;
  readonly columnIndex: number;
  readonly oldValue: unknown;
}

/**
 * Grille native du Design System.
 *
 * Ce premier incrément couvre la lecture, le tri/filtre client, la sélection,
 * les templates riches, le master/detail, le viewport sticky et la navigation
 * clavier entre cellules. Le moteur d'édition de type tableur sera ajouté dans
 * l'incrément suivant.
 *
 * Exemple d'intégration dans un composant standalone :
 *
 * ```ts
 * interface Produit {
 *   id: number;
 *   libelle: string;
 *   stock: number;
 *   statut: string;
 * }
 *
 * @Component({
 *   standalone: true,
 *   imports: [DataGridComponent, DataGridCellDirective],
 *   templateUrl: './produit-list.component.html',
 * })
 * export class ProduitListComponent {
 *   readonly produits = signal<readonly Produit[]>([]);
 *   readonly selection = signal<Produit | Produit[] | null>(null);
 *
 *   readonly colonnes: readonly AppDataGridColumn<Produit>[] = [
 *     {
 *       id: 'libelle',
 *       field: 'libelle',
 *       header: 'Produit',
 *       sortable: true,
 *       pinned: 'left',
 *       width: 240,
 *       minWidth: 160,
 *       maxWidth: 400,
 *     },
 *     {
 *       id: 'stock',
 *       field: 'stock',
 *       header: 'Stock',
 *       type: 'number',
 *       editable: true,
 *       editorOptions: { min: 0 },
 *       align: 'right',
 *       width: 120,
 *       format: cell => `${cell.value} unités`,
 *     },
 *     {
 *       id: 'statut',
 *       field: 'statut',
 *       header: 'Statut',
 *       width: 140,
 *       resizable: false,
 *     },
 *   ];
 * }
 * ```
 *
 * ```html
 * <app-data-grid
 *   ariaLabel="Liste des produits"
 *   [columns]="colonnes"
 *   [rows]="produits()"
 *   [enableCellEditingOnBackspace]="true"
 *   [enterNavigatesVerticallyAfterEdit]="true"
 *   [singleClickEdit]="true"
 *   [stopEditingWhenCellsLoseFocus]="true"
 *   rowKey="id"
 *   size="small"
 *   [stripedRows]="true"
 *   [scrollable]="true"
 *   maxScrollHeight="calc(100vh - 18rem)"
 *   selectionMode="single"
 *   [selection]="selection()"
 *   (selectionChange)="selection.set($event)"
 * >
 *   <ng-template appDataGridCell="statut" let-cell>
 *     <app-badge [label]="cell.value" />
 *   </ng-template>
 * </app-data-grid>
 * ```
 *
 * `rowKey` doit identifier chaque ligne de façon stable. Une colonne est
 * redimensionnable par défaut ; `resizable: false` verrouille sa largeur.
 * L'identifiant de `appDataGridCell` doit correspondre à l'`id` de la colonne.
 * Navigation : flèches entre cellules, `Home`/`End` sur la ligne,
 * `Ctrl+Home`/`Ctrl+End` aux extrémités de la grille, `Entrée` verticalement et
 * `Tab`/`Shift+Tab` séquentiellement (Tab sort de la grille à ses limites).
 * Pour reproduire l'édition AG Grid de l'inventaire, utiliser
 * `[enableCellEditingOnBackspace]="true"`,
 * `[enterNavigatesVerticallyAfterEdit]="true"`, `[singleClickEdit]="true"` et
 * `[stopEditingWhenCellsLoseFocus]="true"`, puis déclarer `editable: true` sur
 * les colonnes concernées.
 */
@Component({
  selector: 'app-data-grid',
  imports: [NgClass, NgTemplateOutlet],
  templateUrl: './data-grid.component.html',
  styleUrl: './data-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataGridComponent<T = Record<string, unknown>> {
  private static nextGridId = 0;
  private readonly gridId = `app-data-grid-${++DataGridComponent.nextGridId}`;
  private readonly collator = new Intl.Collator('fr', { numeric: true, sensitivity: 'base' });

  readonly rows = input.required<readonly T[]>();
  readonly columns = input.required<readonly AppDataGridColumn<T>[]>();
  readonly rowKey = input.required<keyof T & string | ((row: T) => PropertyKey)>();

  readonly ariaLabel = input('Grille de données');
  readonly loading = input(false);
  readonly emptyMessage = input('Aucune donnée');
  readonly size = input<AppDataGridSize>('normal');
  readonly stripedRows = input(false);
  readonly showGridlines = input(false);
  readonly scrollable = input(true);
  readonly scrollHeight = input('');
  readonly maxScrollHeight = input('');
  readonly stickyHeader = input(true);
  readonly enableCellEditingOnBackspace = input(false);
  readonly enterNavigatesVerticallyAfterEdit = input(false);
  readonly singleClickEdit = input(false);
  readonly stopEditingWhenCellsLoseFocus = input(false);

  readonly selectionMode = input<AppDataGridSelectionMode>(null);
  readonly selection = model<T | T[] | null>(null);
  readonly showSelectionCheckboxes = input(false);
  readonly showHeaderCheckbox = input(false);
  readonly rowSelectable = input<(row: T) => boolean>(() => true);

  readonly quickFilter = input('');
  readonly quickFilterFields = input<readonly (keyof T & string)[]>([]);
  readonly rowClass = input<(context: AppDataGridRowContext<T>) => string | readonly string[] | null>();

  readonly detailExpandable = input<(row: T) => boolean>(() => true);
  readonly singleDetailExpansion = input(false);
  readonly expandedRows = model<ReadonlySet<PropertyKey>>(new Set());

  readonly cellClicked = output<AppDataGridCellEvent<T>>();
  readonly rowClicked = output<AppDataGridRowEvent<T>>();
  readonly sortChange = output<AppDataGridSortEvent>();
  readonly detailToggle = output<AppDataGridDetailToggleEvent<T>>();
  readonly columnResize = output<AppDataGridColumnResizeEvent>();
  readonly cellEditingStarted = output<AppDataGridCellEditingStartedEvent<T>>();
  readonly cellEditingStopped = output<AppDataGridCellEditingStoppedEvent<T>>();
  readonly cellValueChanged = output<AppDataGridCellValueChangedEvent<T>>();

  private readonly cellTemplateDirectives = contentChildren<DataGridCellDirective<T>>(DataGridCellDirective);
  private readonly detailTemplateDirective = contentChild<DataGridDetailDirective<T>>(DataGridDetailDirective);
  private readonly editorElements = viewChildren<ElementRef<HTMLInputElement>>('cellEditor');
  private readonly sortState = signal<AppDataGridSortState | null>(null);
  private readonly columnWidths = signal<ReadonlyMap<string, number>>(new Map());
  private readonly resizeState = signal<ColumnResizeState | null>(null);
  private readonly activeCell = signal<ActiveCell | null>(null);
  private readonly editingCell = signal<EditingCell<T> | null>(null);
  protected readonly editorValue = signal('');
  protected readonly editorError = signal<string | null>(null);

  protected readonly visibleColumns = computed(() => this.columns().filter(column => !column.hidden));
  protected readonly hasDetailTemplate = computed(() => Boolean(this.detailTemplateDirective()));
  protected readonly auxiliaryColumnCount = computed(() => (this.showSelectionCheckboxes() ? 1 : 0) + (this.hasDetailTemplate() ? 1 : 0));
  protected readonly renderedColumnCount = computed(() => this.visibleColumns().length + this.auxiliaryColumnCount());

  constructor() {
    effect(() => {
      const editing = this.editingCell();
      const editor = this.editorElements().at(0)?.nativeElement;
      if (!editing || !editor) {
        return;
      }
      editor.focus();
      if (editing.column.editorOptions?.selectOnEdit !== false) {
        editor.select();
      }
    });
  }

  private readonly sourceIndexByKey = computed(() => {
    const indexes = new Map<PropertyKey, number>();
    this.rows().forEach((row, index) => indexes.set(this.keyOf(row), index));
    return indexes;
  });

  private readonly cellTemplates = computed(() => {
    const templates = new Map<string, TemplateRef<AppDataGridCellTemplateContext<T>>>();
    for (const directive of this.cellTemplateDirectives()) {
      templates.set(directive.columnId(), directive.template);
    }
    return templates;
  });

  private readonly selectedKeys = computed(() => {
    const selection = this.selection();
    if (!selection) {
      return new Set<PropertyKey>();
    }
    const selectedRows = Array.isArray(selection) ? selection : [selection];
    return new Set(selectedRows.map(row => this.keyOf(row)));
  });

  private readonly filteredRows = computed(() => {
    const term = this.quickFilter().trim().toLocaleLowerCase('fr');
    if (!term) {
      return this.rows();
    }

    const explicitFields = this.quickFilterFields();
    const filterColumns = this.visibleColumns().filter(column => explicitFields.length === 0 || (column.field && explicitFields.includes(column.field)));

    return this.rows().filter((row, rowIndex) =>
      filterColumns.some(column => {
        const context = this.rowContext(row, rowIndex);
        return String(this.readValue(column, context) ?? '').toLocaleLowerCase('fr').includes(term);
      }),
    );
  });

  protected readonly displayedRows = computed(() => {
    const rows = this.filteredRows();
    const sort = this.sortState();
    if (!sort) {
      return rows;
    }

    const column = this.visibleColumns().find(candidate => candidate.id === sort.columnId);
    if (!column) {
      return rows;
    }

    return [...rows].sort((left, right) => {
      const leftIndex = this.sourceIndexByKey().get(this.keyOf(left)) ?? 0;
      const rightIndex = this.sourceIndexByKey().get(this.keyOf(right)) ?? 0;
      const leftValue = this.readValue(column, this.rowContext(left, leftIndex));
      const rightValue = this.readValue(column, this.rowContext(right, rightIndex));
      return this.compare(leftValue, rightValue) * sort.order;
    });
  });

  private readonly activeCellIsDisplayed = computed(() => {
    const active = this.activeCell();
    return Boolean(
      active &&
        this.visibleColumns().some(column => column.id === active.columnId) &&
        this.displayedRows().some(row => this.keyOf(row) === active.rowKey),
    );
  });

  protected readonly allDisplayedSelected = computed(() => {
    const selectableRows = this.displayedRows().filter(row => this.rowSelectable()(row));
    return selectableRows.length > 0 && selectableRows.every(row => this.selectedKeys().has(this.keyOf(row)));
  });

  protected readonly partiallyDisplayedSelected = computed(() => {
    const selectableRows = this.displayedRows().filter(row => this.rowSelectable()(row));
    const selectedCount = selectableRows.filter(row => this.selectedKeys().has(this.keyOf(row))).length;
    return selectedCount > 0 && selectedCount < selectableRows.length;
  });

  protected readonly tableClasses = computed(() => {
    const classes = ['table', 'app-data-grid__table'];
    if (this.stripedRows()) classes.push('table-striped');
    if (this.showGridlines()) classes.push('table-bordered');
    if (this.size() === 'small') classes.push('table-sm');
    if (this.size() === 'large') classes.push('app-data-grid__table--large');
    return classes.join(' ');
  });

  protected templateFor(columnId: string): TemplateRef<AppDataGridCellTemplateContext<T>> | null {
    return this.cellTemplates().get(columnId) ?? null;
  }

  protected detailTemplate(): TemplateRef<AppDataGridDetailTemplateContext<T>> | null {
    return this.detailTemplateDirective()?.template ?? null;
  }

  protected keyOf(row: T): PropertyKey {
    const keySource = this.rowKey();
    const key = typeof keySource === 'function' ? keySource(row) : row[keySource];
    if (typeof key !== 'string' && typeof key !== 'number' && typeof key !== 'symbol') {
      throw new Error('app-data-grid: rowKey doit retourner une clé string, number ou symbol');
    }
    return key;
  }

  protected trackRow = (_index: number, row: T): PropertyKey => this.keyOf(row);
  protected trackColumn = (_index: number, column: AppDataGridColumn<T>): string => column.id;

  protected rowContext(row: T, rowIndex: number): AppDataGridRowContext<T> {
    const rowKey = this.keyOf(row);
    return {
      row,
      rowIndex,
      rowKey,
      selected: this.selectedKeys().has(rowKey),
      expanded: this.expandedRows().has(rowKey),
    };
  }

  protected cellContext(row: T, rowIndex: number, column: AppDataGridColumn<T>): AppDataGridCellContext<T> {
    const rowContext = this.rowContext(row, rowIndex);
    return {
      ...rowContext,
      column,
      value: this.readValue(column, rowContext),
      toggleSelection: () => this.toggleSelection(row),
      toggleDetail: () => this.toggleDetail(row),
    };
  }

  protected detailContext(row: T, rowIndex: number): AppDataGridDetailContext<T> {
    return { ...this.rowContext(row, rowIndex), collapse: () => this.collapseDetail(row) };
  }

  protected cellTemplateContext(context: AppDataGridCellContext<T>): AppDataGridCellTemplateContext<T> {
    return { ...context, $implicit: context };
  }

  protected detailTemplateContext(context: AppDataGridDetailContext<T>): AppDataGridDetailTemplateContext<T> {
    return { ...context, $implicit: context };
  }

  protected displayValue(context: AppDataGridCellContext<T>): string {
    if (context.column.format) {
      return context.column.format(context);
    }
    return context.value == null ? '' : String(context.value);
  }

  protected resolvedCellClass(context: AppDataGridCellContext<T>): string | readonly string[] | null {
    return typeof context.column.cellClass === 'function' ? context.column.cellClass(context) : (context.column.cellClass ?? null);
  }

  protected resolvedTooltip(context: AppDataGridCellContext<T>): string | null {
    return typeof context.column.tooltip === 'function' ? context.column.tooltip(context) : (context.column.tooltip ?? null);
  }

  protected isSelected(row: T): boolean {
    return this.selectedKeys().has(this.keyOf(row));
  }

  protected isExpanded(row: T): boolean {
    return this.expandedRows().has(this.keyOf(row));
  }

  protected canExpand(row: T): boolean {
    return this.hasDetailTemplate() && this.detailExpandable()(row);
  }

  protected toggleSort(column: AppDataGridColumn<T>): void {
    if (!column.sortable) {
      return;
    }
    const current = this.sortState();
    const order: 1 | -1 = current?.columnId === column.id && current.order === 1 ? -1 : 1;
    this.sortState.set({ columnId: column.id, order });
    this.sortChange.emit({ columnId: column.id, field: column.field, order });
  }

  protected ariaSort(column: AppDataGridColumn<T>): 'ascending' | 'descending' | 'none' | null {
    if (!column.sortable) {
      return null;
    }
    const sort = this.sortState();
    if (sort?.columnId !== column.id) {
      return 'none';
    }
    return sort.order === 1 ? 'ascending' : 'descending';
  }

  protected onCellClick(
    event: MouseEvent,
    row: T,
    rowIndex: number,
    column: AppDataGridColumn<T>,
    columnIndex: number,
  ): void {
    this.cellClicked.emit({ row, rowKey: this.keyOf(row), rowIndex, column, originalEvent: event });
    if (this.singleClickEdit()) {
      this.startCellEdit(event, row, rowIndex, column, columnIndex, 'click');
    }
  }

  protected onCellDoubleClick(
    event: MouseEvent,
    row: T,
    rowIndex: number,
    column: AppDataGridColumn<T>,
    columnIndex: number,
  ): void {
    if (!this.singleClickEdit()) {
      this.startCellEdit(event, row, rowIndex, column, columnIndex, 'double-click');
    }
  }

  protected isActiveCell(row: T, column: AppDataGridColumn<T>, rowIndex: number, columnIndex: number): boolean {
    const active = this.activeCell();
    if (active && this.activeCellIsDisplayed()) {
      return active.rowKey === this.keyOf(row) && active.columnId === column.id;
    }
    return rowIndex === 0 && columnIndex === 0;
  }

  protected activateCell(row: T, column: AppDataGridColumn<T>): void {
    this.activeCell.set({ rowKey: this.keyOf(row), columnId: column.id });
  }

  protected isEditingCell(row: T, column: AppDataGridColumn<T>): boolean {
    const editing = this.editingCell();
    return Boolean(editing && editing.rowKey === this.keyOf(row) && editing.columnId === column.id);
  }

  protected editorType(column: AppDataGridColumn<T>): 'text' | 'number' {
    return column.editor ?? (column.type === 'number' ? 'number' : 'text');
  }

  protected updateEditorValue(event: Event): void {
    this.editorValue.set((event.target as HTMLInputElement).value);
    this.editorError.set(null);
  }

  protected onEditorKeydown(event: KeyboardEvent): void {
    const editing = this.editingCell();
    if (!editing) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.cancelCellEdit('escape', event.currentTarget as HTMLElement);
      return;
    }
    if (event.key === 'Enter') {
      event.preventDefault();
      event.stopPropagation();
      this.commitCellEdit('enter', event.currentTarget as HTMLElement, event.shiftKey ? -1 : 1);
      return;
    }
    if (event.key === 'Tab') {
      const columnCount = this.visibleColumns().length;
      const currentIndex = editing.rowIndex * columnCount + editing.columnIndex;
      const targetIndex = currentIndex + (event.shiftKey ? -1 : 1);
      const canNavigate = targetIndex >= 0 && targetIndex < this.displayedRows().length * columnCount;
      if (canNavigate) {
        event.preventDefault();
      }
      event.stopPropagation();
      this.commitCellEdit('tab', event.currentTarget as HTMLElement, event.shiftKey ? -1 : 1, true);
    }
  }

  protected onEditorBlur(event: FocusEvent): void {
    if (this.stopEditingWhenCellsLoseFocus() && this.editingCell()) {
      this.commitCellEdit('blur', event.currentTarget as HTMLElement);
    }
  }

  protected navigateCell(
    event: KeyboardEvent,
    rowIndex: number,
    columnIndex: number,
  ): void {
    const eventTarget = event.target as HTMLElement;
    if (eventTarget !== event.currentTarget && eventTarget.closest('button, input, select, textarea, a, [contenteditable="true"]')) {
      return;
    }
    const rowCount = this.displayedRows().length;
    const columnCount = this.visibleColumns().length;
    if (rowCount === 0 || columnCount === 0 || event.altKey || event.metaKey) {
      return;
    }

    if (event.key === 'Backspace' && this.enableCellEditingOnBackspace()) {
      const row = this.displayedRows()[rowIndex];
      const column = this.visibleColumns()[columnIndex];
      if (row && column && this.startCellEdit(event, row, rowIndex, column, columnIndex, 'backspace', true)) {
        event.preventDefault();
        event.stopPropagation();
      }
      return;
    }

    let targetRow = rowIndex;
    let targetColumn = columnIndex;
    let handled = true;

    switch (event.key) {
      case 'ArrowLeft':
        targetColumn = Math.max(0, columnIndex - 1);
        break;
      case 'ArrowRight':
        targetColumn = Math.min(columnCount - 1, columnIndex + 1);
        break;
      case 'ArrowUp':
        targetRow = Math.max(0, rowIndex - 1);
        break;
      case 'ArrowDown':
        targetRow = Math.min(rowCount - 1, rowIndex + 1);
        break;
      case 'Home':
        if (event.ctrlKey) targetRow = 0;
        targetColumn = 0;
        break;
      case 'End':
        if (event.ctrlKey) targetRow = rowCount - 1;
        targetColumn = columnCount - 1;
        break;
      case 'Enter':
        targetRow = Math.min(rowCount - 1, Math.max(0, rowIndex + (event.shiftKey ? -1 : 1)));
        break;
      case 'Tab': {
        const currentIndex = rowIndex * columnCount + columnIndex;
        const targetIndex = currentIndex + (event.shiftKey ? -1 : 1);
        if (targetIndex < 0 || targetIndex >= rowCount * columnCount) {
          handled = false;
          break;
        }
        targetRow = Math.floor(targetIndex / columnCount);
        targetColumn = targetIndex % columnCount;
        break;
      }
      default:
        handled = false;
    }

    if (!handled) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    const table = (event.currentTarget as HTMLElement).closest('table');
    this.focusCellAt(table, targetRow, targetColumn);
  }

  protected onRowClick(event: MouseEvent, row: T, rowIndex: number): void {
    this.rowClicked.emit({ row, rowKey: this.keyOf(row), rowIndex, originalEvent: event });
    if (this.selectionMode() && !(event.target as HTMLElement).closest('button, input, a, select, textarea')) {
      this.toggleSelection(row);
    }
  }

  protected toggleSelection(row: T): void {
    if (!this.rowSelectable()(row)) {
      return;
    }

    if (this.selectionMode() === 'single') {
      this.selection.set(this.isSelected(row) ? null : row);
      return;
    }

    const current = Array.isArray(this.selection()) ? [...(this.selection() as T[])] : [];
    const key = this.keyOf(row);
    const index = current.findIndex(candidate => this.keyOf(candidate) === key);
    if (index >= 0) {
      current.splice(index, 1);
    } else {
      current.push(row);
    }
    this.selection.set(current);
  }

  protected toggleAllDisplayed(): void {
    const displayed = this.displayedRows().filter(row => this.rowSelectable()(row));
    if (this.allDisplayedSelected()) {
      const displayedKeys = new Set(displayed.map(row => this.keyOf(row)));
      const current = Array.isArray(this.selection()) ? (this.selection() as T[]) : [];
      this.selection.set(current.filter(row => !displayedKeys.has(this.keyOf(row))));
      return;
    }

    const current = Array.isArray(this.selection()) ? [...(this.selection() as T[])] : [];
    const currentKeys = new Set(current.map(row => this.keyOf(row)));
    for (const row of displayed) {
      if (!currentKeys.has(this.keyOf(row))) {
        current.push(row);
      }
    }
    this.selection.set(current);
  }

  protected toggleDetail(row: T): void {
    if (!this.canExpand(row)) {
      return;
    }
    const key = this.keyOf(row);
    const next = this.singleDetailExpansion() ? new Set<PropertyKey>() : new Set(this.expandedRows());
    const expanded = !this.expandedRows().has(key);
    if (expanded) {
      next.add(key);
    } else {
      next.delete(key);
    }
    this.expandedRows.set(next);
    this.detailToggle.emit({ row, rowKey: key, expanded });
  }

  protected detailPanelId(row: T): string {
    const key = String(this.keyOf(row)).replace(/[^a-zA-Z0-9_-]/g, '-');
    return `${this.gridId}-detail-${key}`;
  }

  protected pinnedOffset(column: AppDataGridColumn<T>): number | null {
    if (!column.pinned) {
      return null;
    }
    const pinned = this.visibleColumns().filter(candidate => candidate.pinned === column.pinned);
    const index = pinned.findIndex(candidate => candidate.id === column.id);
    const preceding = column.pinned === 'left' ? pinned.slice(0, index) : pinned.slice(index + 1);
    return preceding.reduce((total, candidate) => total + this.effectiveWidth(candidate), 0);
  }

  protected columnWidth(column: AppDataGridColumn<T>): string | null {
    const resizedWidth = this.columnWidths().get(column.id);
    if (resizedWidth !== undefined) {
      return `${resizedWidth}px`;
    }
    if (column.width) {
      return `${column.width}px`;
    }
    if (column.resizable === false) {
      return `${this.effectiveWidth(column)}px`;
    }
    if (column.flex) {
      const totalFlex = this.visibleColumns().reduce((total, candidate) => total + (candidate.flex ?? 0), 0);
      return totalFlex > 0 ? `${(column.flex / totalFlex) * 100}%` : null;
    }
    return null;
  }

  protected columnMinWidth(column: AppDataGridColumn<T>): number | null {
    return column.resizable === false ? this.effectiveWidth(column) : (column.minWidth ?? null);
  }

  protected columnMaxWidth(column: AppDataGridColumn<T>): number | null {
    return column.resizable === false ? this.effectiveWidth(column) : (column.maxWidth ?? null);
  }

  protected isResizable(column: AppDataGridColumn<T>): boolean {
    return column.resizable !== false;
  }

  protected currentColumnWidth(column: AppDataGridColumn<T>): number {
    return this.effectiveWidth(column);
  }

  protected startColumnResize(event: PointerEvent, column: AppDataGridColumn<T>): void {
    if (!this.isResizable(column)) {
      return;
    }
    const handle = event.currentTarget as HTMLElement;
    const measuredWidth = handle.parentElement?.getBoundingClientRect().width ?? 0;
    handle.setPointerCapture(event.pointerId);
    this.resizeState.set({
      columnId: column.id,
      pointerId: event.pointerId,
      startX: event.clientX,
      startWidth: measuredWidth || this.effectiveWidth(column),
    });
    event.preventDefault();
    event.stopPropagation();
  }

  protected continueColumnResize(event: PointerEvent, column: AppDataGridColumn<T>): void {
    const state = this.resizeState();
    if (!state || state.columnId !== column.id || state.pointerId !== event.pointerId) {
      return;
    }
    this.writeColumnWidth(column, state.startWidth + event.clientX - state.startX);
  }

  protected finishColumnResize(event: PointerEvent, column: AppDataGridColumn<T>): void {
    const state = this.resizeState();
    if (!state || state.columnId !== column.id || state.pointerId !== event.pointerId) {
      return;
    }
    const handle = event.currentTarget as HTMLElement;
    if (handle.hasPointerCapture(event.pointerId)) {
      handle.releasePointerCapture(event.pointerId);
    }
    this.resizeState.set(null);
    this.columnResize.emit({ columnId: column.id, width: this.effectiveWidth(column), source: 'pointer' });
  }

  protected resizeColumnWithKeyboard(event: KeyboardEvent, column: AppDataGridColumn<T>): void {
    if (!this.isResizable(column)) {
      return;
    }
    if (event.key === 'Home') {
      this.resetColumnWidth(column, 'reset');
      event.preventDefault();
      return;
    }
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') {
      return;
    }
    const direction = event.key === 'ArrowRight' ? 1 : -1;
    const step = event.shiftKey ? 1 : 10;
    const width = this.writeColumnWidth(column, this.effectiveWidth(column) + direction * step);
    this.columnResize.emit({ columnId: column.id, width, source: 'keyboard' });
    event.preventDefault();
    event.stopPropagation();
  }

  protected resetColumnWidth(column: AppDataGridColumn<T>, source: 'reset' = 'reset'): void {
    this.columnWidths.update(widths => {
      const next = new Map(widths);
      next.delete(column.id);
      return next;
    });
    this.columnResize.emit({ columnId: column.id, width: this.effectiveWidth(column), source });
  }

  private startCellEdit(
    event: MouseEvent | KeyboardEvent,
    row: T,
    rowIndex: number,
    column: AppDataGridColumn<T>,
    columnIndex: number,
    source: AppDataGridEditStartSource,
    clearValue = false,
  ): boolean {
    const context = this.cellContext(row, rowIndex, column);
    const editable = typeof column.editable === 'function' ? column.editable(context) : column.editable;
    if (!editable || !column.field || this.isEditingCell(row, column)) {
      return false;
    }
    if (event instanceof MouseEvent && (event.target as HTMLElement).closest('button, input, select, textarea, a')) {
      return false;
    }

    const oldValue = context.value;
    this.activeCell.set({ rowKey: context.rowKey, columnId: column.id });
    this.editorValue.set(clearValue ? '' : String(oldValue ?? ''));
    this.editorError.set(null);
    this.editingCell.set({
      row,
      rowKey: context.rowKey,
      columnId: column.id,
      rowIndex,
      column,
      columnIndex,
      oldValue,
    });
    this.cellEditingStarted.emit({ row, rowKey: context.rowKey, rowIndex, column, oldValue, source, originalEvent: event });
    return true;
  }

  private commitCellEdit(
    source: Exclude<AppDataGridEditStopSource, 'escape'>,
    editor: HTMLElement,
    direction = 0,
    sequential = false,
  ): void {
    const editing = this.editingCell();
    if (!editing || !editing.column.field) {
      return;
    }
    const value = this.parseEditorValue(editing.column);
    if ('error' in value) {
      this.editorError.set(value.error);
      return;
    }

    const table = editor.closest('table');
    this.editingCell.set(null);
    this.editorError.set(null);
    if (!Object.is(editing.oldValue, value.value)) {
      this.cellValueChanged.emit({
        row: editing.row,
        rowKey: editing.rowKey,
        rowIndex: editing.rowIndex,
        column: editing.column,
        field: editing.column.field,
        oldValue: editing.oldValue,
        newValue: value.value,
        source,
      });
    }
    this.cellEditingStopped.emit({
      row: editing.row,
      rowKey: editing.rowKey,
      rowIndex: editing.rowIndex,
      column: editing.column,
      committed: true,
      source,
    });

    if (source === 'blur') {
      return;
    }
    if (sequential) {
      const columnCount = this.visibleColumns().length;
      const targetIndex = editing.rowIndex * columnCount + editing.columnIndex + direction;
      if (targetIndex >= 0 && targetIndex < this.displayedRows().length * columnCount) {
        this.focusCellAt(table, Math.floor(targetIndex / columnCount), targetIndex % columnCount);
      }
      return;
    }
    const targetRow = this.enterNavigatesVerticallyAfterEdit()
      ? Math.min(this.displayedRows().length - 1, Math.max(0, editing.rowIndex + direction))
      : editing.rowIndex;
    this.focusCellAt(table, targetRow, editing.columnIndex);
  }

  private cancelCellEdit(source: 'escape', editor: HTMLElement): void {
    const editing = this.editingCell();
    if (!editing) {
      return;
    }
    const table = editor.closest('table');
    this.editingCell.set(null);
    this.editorError.set(null);
    this.cellEditingStopped.emit({
      row: editing.row,
      rowKey: editing.rowKey,
      rowIndex: editing.rowIndex,
      column: editing.column,
      committed: false,
      source,
    });
    this.focusCellAt(table, editing.rowIndex, editing.columnIndex);
  }

  private parseEditorValue(column: AppDataGridColumn<T>): { readonly valid: true; readonly value: unknown } | { readonly valid: false; readonly error: string } {
    const rawValue = this.editorValue();
    let value: unknown = rawValue;
    if (this.editorType(column) === 'number') {
      value = rawValue.trim() === '' ? null : Number(rawValue);
      if (value !== null && (typeof value !== 'number' || !Number.isFinite(value))) {
        return { valid: false, error: 'Nombre invalide' };
      }
      if (typeof value === 'number' && column.editorOptions?.min !== undefined && value < column.editorOptions.min) {
        return { valid: false, error: `La valeur minimale est ${column.editorOptions.min}` };
      }
      if (typeof value === 'number' && column.editorOptions?.max !== undefined && value > column.editorOptions.max) {
        return { valid: false, error: `La valeur maximale est ${column.editorOptions.max}` };
      }
    }
    if (column.editorOptions?.maxLength !== undefined && rawValue.length > column.editorOptions.maxLength) {
      return { valid: false, error: `La longueur maximale est ${column.editorOptions.maxLength}` };
    }
    const validationError = column.editorOptions?.validate?.(value);
    return validationError ? { valid: false, error: validationError } : { valid: true, value };
  }

  private focusCellAt(table: HTMLTableElement | null, rowIndex: number, columnIndex: number): void {
    const row = this.displayedRows()[rowIndex];
    const column = this.visibleColumns()[columnIndex];
    if (!row || !column) {
      return;
    }
    this.activeCell.set({ rowKey: this.keyOf(row), columnId: column.id });
    table
      ?.querySelector<HTMLElement>(`[data-grid-row="${rowIndex}"][data-grid-column="${columnIndex}"]`)
      ?.focus({ preventScroll: false });
  }

  private collapseDetail(row: T): void {
    const key = this.keyOf(row);
    if (!this.expandedRows().has(key)) {
      return;
    }
    const next = new Set(this.expandedRows());
    next.delete(key);
    this.expandedRows.set(next);
    this.detailToggle.emit({ row, rowKey: key, expanded: false });
  }

  private readValue(column: AppDataGridColumn<T>, context: AppDataGridRowContext<T>): unknown {
    if (column.value) {
      return column.value(context);
    }
    return column.field ? context.row[column.field] : null;
  }

  private effectiveWidth(column: AppDataGridColumn<T>): number {
    return this.columnWidths().get(column.id) ?? column.width ?? column.minWidth ?? 120;
  }

  private writeColumnWidth(column: AppDataGridColumn<T>, requestedWidth: number): number {
    const min = column.minWidth ?? 60;
    const max = column.maxWidth ?? Number.POSITIVE_INFINITY;
    const width = Math.round(Math.min(Math.max(requestedWidth, min), max));
    this.columnWidths.update(widths => {
      const next = new Map(widths);
      next.set(column.id, width);
      return next;
    });
    return width;
  }

  private compare(left: unknown, right: unknown): number {
    if (left == null && right == null) return 0;
    if (left == null) return -1;
    if (right == null) return 1;
    if (typeof left === 'number' && typeof right === 'number') return left - right;
    if (left instanceof Date && right instanceof Date) return left.getTime() - right.getTime();
    return this.collator.compare(String(left), String(right));
  }
}
