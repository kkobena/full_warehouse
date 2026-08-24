import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DataGridCellDirective } from './data-grid-cell.directive';
import { DataGridDetailDirective } from './data-grid-detail.directive';
import { DataGridComponent } from './data-grid.component';
import { AppDataGridCellValueChangedEvent, AppDataGridColumn, AppDataGridColumnResizeEvent } from './data-grid.types';

interface ProductRow {
  id: number;
  name: string;
  stock: number;
  status: string;
}

@Component({
  standalone: true,
  imports: [DataGridComponent, DataGridCellDirective, DataGridDetailDirective],
  template: `
    <app-data-grid
      ariaLabel="Stock produits"
      [columns]="columns"
      [enableCellEditingOnBackspace]="true"
      [enterNavigatesVerticallyAfterEdit]="true"
      [singleClickEdit]="true"
      [stopEditingWhenCellsLoseFocus]="true"
      (columnResize)="lastResize.set($event)"
      (cellValueChanged)="lastValueChange.set($event)"
      [expandedRows]="expandedRows()"
      (expandedRowsChange)="expandedRows.set($event)"
      [loading]="loading()"
      [maxScrollHeight]="'20rem'"
      [quickFilter]="filter()"
      [quickFilterFields]="['name']"
      rowKey="id"
      [rows]="rows()"
      [selection]="selection()"
      (selectionChange)="selection.set($event)"
      selectionMode="multiple"
      [showHeaderCheckbox]="true"
      [showSelectionCheckboxes]="true"
      [showGridlines]="true"
      [stripedRows]="true"
    >
      <ng-template appDataGridCell="status" let-cell>
        <strong class="status-template">{{ cell.value }}</strong>
      </ng-template>

      <ng-template appDataGridDetail let-detail>
        <div class="detail-template">Détail de {{ detail.row.name }}</div>
      </ng-template>
    </app-data-grid>
  `,
})
class TestHostComponent {
  readonly rows = signal<readonly ProductRow[]>([
    { id: 1, name: 'Zinc', stock: 3, status: 'Faible' },
    { id: 2, name: 'Aspirine', stock: 15, status: 'Normal' },
  ]);
  readonly selection = signal<ProductRow | ProductRow[] | null>(null);
  readonly expandedRows = signal<ReadonlySet<PropertyKey>>(new Set());
  readonly filter = signal('');
  readonly loading = signal(false);
  readonly lastResize = signal<AppDataGridColumnResizeEvent | null>(null);
  readonly lastValueChange = signal<AppDataGridCellValueChangedEvent<ProductRow> | null>(null);

  readonly columns: readonly AppDataGridColumn<ProductRow>[] = [
    { id: 'name', field: 'name', header: 'Produit', sortable: true, pinned: 'left', width: 180, minWidth: 160, maxWidth: 200 },
    {
      id: 'stock',
      field: 'stock',
      header: 'Stock',
      type: 'number',
      editable: true,
      editorOptions: { min: 0 },
      align: 'right',
      format: context => `${context.value} unités`,
    },
    { id: 'status', field: 'status', header: 'État', cellClass: 'status-cell', width: 140, resizable: false },
  ];
}

describe('DataGridComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TestHostComponent] }).compileComponents();
  });

  it('rend une grille sémantique, les valeurs formatées et un template de cellule', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const grid = host.querySelector('table')!;

    expect(grid.getAttribute('role')).toBe('grid');
    expect(grid.getAttribute('aria-label')).toBe('Stock produits');
    expect(grid.querySelector('thead > tr')?.classList).toContain('pharma-table-head');
    expect(host.querySelectorAll('tbody > tr:not(.app-data-grid__detail-row)')).toHaveLength(2);
    expect(host.textContent).toContain('3 unités');
    expect(host.querySelector('.status-template')?.textContent).toContain('Faible');
    expect(host.querySelector('.status-cell')).not.toBeNull();
  });

  it('trie sans muter le tableau reçu', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    const originalRows = fixture.componentInstance.rows();
    fixture.detectChanges();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.app-data-grid__sort-button')!.click();
    fixture.detectChanges();

    const firstRow = (fixture.nativeElement as HTMLElement).querySelector('tbody > tr')!;
    expect(firstRow.textContent).toContain('Aspirine');
    expect(fixture.componentInstance.rows()).toBe(originalRows);
    expect(fixture.componentInstance.rows()[0].name).toBe('Zinc');
  });

  it('applique le filtre rapide sur les champs déclarés', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.filter.set('aspi');
    fixture.detectChanges();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody > tr');
    expect(rows).toHaveLength(1);
    expect(rows[0].textContent).toContain('Aspirine');
  });

  it('sélectionne une ligne et toute la vue avec les cases dédiées', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const checkboxes = host.querySelectorAll<HTMLInputElement>('input[type="checkbox"]');

    checkboxes[1].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selection()).toEqual([fixture.componentInstance.rows()[0]]);

    checkboxes[0].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.selection()).toHaveLength(2);
  });

  it('ouvre et ferme un détail projeté par clé stable', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    host.querySelector<HTMLButtonElement>('.app-data-grid__detail-toggle')!.click();
    fixture.detectChanges();
    expect(host.querySelector('.detail-template')?.textContent).toContain('Zinc');
    expect(fixture.componentInstance.expandedRows().has(1)).toBe(true);

    host.querySelector<HTMLButtonElement>('.app-data-grid__detail-toggle')!.click();
    fixture.detectChanges();
    expect(host.querySelector('.detail-template')).toBeNull();
  });

  it('configure le viewport, le header fixe et la colonne figée', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;

    expect(host.querySelector('.app-data-grid__scroller')?.getAttribute('style')).toContain('max-height: 20rem');
    expect(host.querySelector('thead')?.classList).toContain('app-data-grid__head--sticky');
    expect(host.querySelector('th.app-data-grid__cell--pinned-left')).not.toBeNull();
  });

  it('navigue automatiquement entre les cellules au clavier', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const cell = (row: number, column: number): HTMLElement =>
      host.querySelector<HTMLElement>(`[data-grid-row="${row}"][data-grid-column="${column}"]`)!;
    const press = (target: HTMLElement, key: string, options: KeyboardEventInit = {}): boolean =>
      target.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...options }));

    cell(0, 0).focus();
    expect(document.activeElement).toBe(cell(0, 0));

    expect(press(cell(0, 0), 'ArrowRight')).toBe(false);
    expect(document.activeElement).toBe(cell(0, 1));

    press(cell(0, 1), 'ArrowDown');
    expect(document.activeElement).toBe(cell(1, 1));

    press(cell(1, 1), 'Home');
    expect(document.activeElement).toBe(cell(1, 0));

    press(cell(1, 0), 'End', { ctrlKey: true });
    expect(document.activeElement).toBe(cell(1, 2));

    press(cell(1, 2), 'Tab', { shiftKey: true });
    expect(document.activeElement).toBe(cell(1, 1));

    fixture.detectChanges();
    expect(cell(1, 1).tabIndex).toBe(0);
    expect(cell(0, 0).tabIndex).toBe(-1);
  });

  it('laisse Tab sortir de la grille à ses limites', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const lastCell = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[data-grid-row="1"][data-grid-column="2"]')!;

    lastCell.focus();
    const propagated = lastCell.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }));

    expect(propagated).toBe(true);
  });

  it('édite au clic puis valide et navigue verticalement avec Entrée', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const firstStockCell = host.querySelector<HTMLElement>('[data-grid-row="0"][data-grid-column="1"]')!;

    firstStockCell.click();
    fixture.detectChanges();
    const editor = firstStockCell.querySelector<HTMLInputElement>('.app-data-grid__editor')!;
    expect(editor).not.toBeNull();
    expect(editor.type).toBe('number');

    editor.value = '8';
    editor.dispatchEvent(new Event('input', { bubbles: true }));
    editor.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(fixture.componentInstance.lastValueChange()).toMatchObject({ field: 'stock', oldValue: 3, newValue: 8, source: 'enter' });
    expect(document.activeElement).toBe(host.querySelector('[data-grid-row="1"][data-grid-column="1"]'));
  });

  it('ouvre et vide une cellule éditable avec Retour arrière', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const cell = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[data-grid-row="0"][data-grid-column="1"]')!;
    cell.focus();

    cell.dispatchEvent(new KeyboardEvent('keydown', { key: 'Backspace', bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(cell.querySelector<HTMLInputElement>('.app-data-grid__editor')?.value).toBe('');
  });

  it('valide automatiquement quand l’éditeur perd le focus', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const cell = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[data-grid-row="0"][data-grid-column="1"]')!;
    cell.click();
    fixture.detectChanges();
    const editor = cell.querySelector<HTMLInputElement>('.app-data-grid__editor')!;
    editor.value = '9';
    editor.dispatchEvent(new Event('input', { bubbles: true }));

    editor.dispatchEvent(new FocusEvent('blur'));
    fixture.detectChanges();

    expect(fixture.componentInstance.lastValueChange()).toMatchObject({ newValue: 9, source: 'blur' });
    expect(cell.querySelector('.app-data-grid__editor')).toBeNull();
  });

  it('redimensionne une colonne au clavier et rétablit sa largeur initiale', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const productHeader = Array.from(host.querySelectorAll<HTMLTableCellElement>('th'))
      .find(header => header.textContent?.includes('Produit'))!;
    const handle = productHeader.querySelector<HTMLElement>('.app-data-grid__resize-handle')!;

    handle.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();

    expect(productHeader.style.width).toBe('190px');
    expect(fixture.componentInstance.lastResize()).toEqual({ columnId: 'name', width: 190, source: 'keyboard' });

    handle.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
    fixture.detectChanges();

    expect(productHeader.style.width).toBe('180px');
    expect(fixture.componentInstance.lastResize()).toEqual({ columnId: 'name', width: 180, source: 'reset' });
  });

  it('redimensionne au pointeur, capture le geste et respecte les bornes', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const productHeader = Array.from(host.querySelectorAll<HTMLTableCellElement>('th'))
      .find(header => header.textContent?.includes('Produit'))!;
    const handle = productHeader.querySelector<HTMLElement>('.app-data-grid__resize-handle')!;
    Object.defineProperty(productHeader, 'getBoundingClientRect', { value: () => ({ width: 180 }) });
    Object.defineProperties(handle, {
      setPointerCapture: { value: jest.fn() },
      hasPointerCapture: { value: jest.fn(() => true) },
      releasePointerCapture: { value: jest.fn() },
    });
    const pointerEvent = (type: string, clientX: number): Event => {
      const event = new MouseEvent(type, { bubbles: true, clientX });
      Object.defineProperty(event, 'pointerId', { value: 7 });
      return event;
    };

    handle.dispatchEvent(pointerEvent('pointerdown', 100));
    handle.dispatchEvent(pointerEvent('pointermove', 150));
    fixture.detectChanges();
    expect(productHeader.style.width).toBe('200px');

    handle.dispatchEvent(pointerEvent('pointerup', 150));
    fixture.detectChanges();

    expect(handle.setPointerCapture).toHaveBeenCalledWith(7);
    expect(handle.releasePointerCapture).toHaveBeenCalledWith(7);
    expect(fixture.componentInstance.lastResize()).toEqual({ columnId: 'name', width: 200, source: 'pointer' });
  });

  it('verrouille une colonne fixe et ne lui rend aucune poignée', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    const statusHeader = Array.from(host.querySelectorAll<HTMLTableCellElement>('th'))
      .find(header => header.textContent?.includes('État'))!;

    expect(statusHeader.style.width).toBe('140px');
    expect(statusHeader.style.minWidth).toBe('140px');
    expect(statusHeader.style.maxWidth).toBe('140px');
    expect(statusHeader.querySelector('.app-data-grid__resize-handle')).toBeNull();
  });

  it('affiche les états vide et chargement', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.componentInstance.rows.set([]);
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.querySelector('.app-data-grid__empty')?.textContent).toContain('Aucune donnée');

    fixture.componentInstance.loading.set(true);
    fixture.detectChanges();
    expect(host.querySelector('[role="status"]')?.textContent).toContain('Chargement');
    expect(host.querySelector('table')?.getAttribute('aria-busy')).toBe('true');
  });
});
