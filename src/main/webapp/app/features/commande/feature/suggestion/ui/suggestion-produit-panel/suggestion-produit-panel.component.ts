import { Component, computed, effect, ElementRef, input, output, signal, viewChild } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InputTextModule } from 'primeng/inputtext';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import {
  AllCommunityModule,
  CellClickedEvent,
  CellValueChangedEvent,
  ClientSideRowModelModule,
  ColDef,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  RowSelectionOptions,
  themeAlpine,
} from 'ag-grid-community';
import { AgGridAngular } from 'ag-grid-angular';
import { FournisseurSuggestionSummary, SuggestionLigneEnrichie } from '../../data-access/suggestion-enrichie.model';
import { CommandeProductSearchComponent } from '../../../../ui/commande-product-search/commande-product-search.component';
import { ProduitSearch } from 'app/shared/model';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-suggestion-produit-panel',
  templateUrl: './suggestion-produit-panel.component.html',
  styleUrls: ['./suggestion-produit-panel.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    InputTextModule,
    AgGridAngular,
    DecimalPipe,
    CommandeProductSearchComponent,
    InputGroupModule,
    InputGroupAddonModule,
  ],
})
export class SuggestionProduitPanelComponent {
  // ─── Inputs ─────────────────────────────────────────────────────────────────
  fournisseur = input<FournisseurSuggestionSummary | null>(null);
  lignes = input<SuggestionLigneEnrichie[]>([]);
  loading = input(false);
  total = input(0);
  page = input(0);
  rows = input(0);

  // ─── ViewChild refs ──────────────────────────────────────────────────────────
  private readonly searchComp = viewChild<CommandeProductSearchComponent>('produitSearch');
  private readonly quantityBoxRef = viewChild<ElementRef>('quantityBox');

  // ─── Outputs ─────────────────────────────────────────────────────────────────
  commander = output<void>();
  commanderSelection = output<void>();
  quantiteChanged = output<{ ligne: SuggestionLigneEnrichie; qte: number }>();
  selectionChanged = output<SuggestionLigneEnrichie[]>();
  resetQte = output<SuggestionLigneEnrichie>();
  sanitizeRequest = output<void>();
  filterChange = output<{ search: string; urgence: string }>();
  pageChange = output<{ page: number; rows: number }>();
  validerRequest = output<void>();
  rejeterRequest = output<void>();
  comparerRequest = output<SuggestionLigneEnrichie>();
  exportPdfRequest = output<void>();
  exportCsvRequest = output<void>();
  dispoRequest = output<void>();
  ligneSupprimer = output<SuggestionLigneEnrichie>();
  lignesSupprimer = output<SuggestionLigneEnrichie[]>();
  ajouterProduit = output<{ produitId: number; fournisseurProduitId: number; quantite: number }>();

  // ─── AG Grid ─────────────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;
  private gridApi: GridApi | null = null;
  /** Clés mois courantes — évite de reconstruire les colonnes si elles n'ont pas changé. */
  private currentMoisKeys: string[] = [];

  readonly rowSelection: RowSelectionOptions = {
    mode: 'multiRow',
    checkboxes: true,
    headerCheckbox: true,
    enableClickSelection: true,
  };

  // ── Colonnes statiques (gauche) ───────────────────────────────────────────
  private readonly baseColumnDefs: ColDef<SuggestionLigneEnrichie>[] = [
    {
      field: 'codeCip',
      headerName: 'CIP',
      width: 110,
      sortable: true,
      cellStyle: { fontFamily: 'monospace', fontSize: '12px' },
    },
    {
      field: 'libelle',
      headerName: 'Produit',
      flex: 2,
      minWidth: 160,
      sortable: true,
    },
    {
      field: 'currentStock',
      headerName: 'Stock',
      width: 75,
      sortable: true,
      type: 'numericColumn',
      cellStyle: (params: any) =>
        params.value === 0
          ? { color: '#dc3545', fontWeight: 700 }
          : params.value <= 2
            ? { color: '#fd7e14', fontWeight: 600 }
            : null,
    },
    {
      field: 'quantite',
      headerName: 'Qté',
      width: 90,
      editable: true,
      type: 'numericColumn',
      cellEditor: 'agNumberCellEditor',
      cellRenderer: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        const lock = ligne?.quantiteModifieeManuel
          ? `<span class="ag-lock-icon" title="Quantité verrouillée (modifiée manuellement)">🔒</span> `
          : '';
        return `${lock}${params.value ?? 0}`;
      },
      cellStyle: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        return ligne?.quantiteModifieeManuel
          ? { backgroundColor: '#fffbeb', borderColor: '#f59e0b', fontWeight: 700, color: '#92400e' }
          : ligne?.quantiteModifiee
            ? { backgroundColor: '#eff6ff', fontWeight: 600 }
            : null;
      },
    },
    {
      field: 'joursRestants',
      headerName: 'Couv. (j)',
      width: 90,
      sortable: true,
      type: 'numericColumn',
      valueFormatter: (params: any) => params.value != null ? `${params.value}j` : '—',
      cellStyle: (params: any) => {
        if (params.value == null) return null;
        if (params.value < 7) return { color: '#dc3545', fontWeight: 700 };
        if (params.value < 30) return { color: '#fd7e14' };
        return null;
      },
    },
    {
      field: 'prixAchat',
      headerName: 'P.A (FCFA)',
      width: 110,
      sortable: true,
      type: 'numericColumn',
      valueFormatter: (params: any) =>
        params.value != null ? Number(params.value).toLocaleString('fr-FR') : '—',
    },
  ];

  // ── Colonne actions (toujours épinglée à droite) ──────────────────────────
  private readonly actionsColumnDef: ColDef<SuggestionLigneEnrichie> = {
    headerName: '',
    width: 110,
    pinned: 'right' as const,
    sortable: false,
    resizable: false,
    suppressMovable: true,
    cellRenderer: (params: any) => {
      const ligne: SuggestionLigneEnrichie = params.data;
      if (!ligne) return '';
      const resetBtn = ligne.quantiteModifieeManuel
        ? `<button class="ag-action-btn ag-action-reset" data-action="reset" title="Déverrouiller — laisser SEMOIS recalculer la quantité">🔓</button>`
        : '';
      const compareBtn = ligne.produitId
        ? `<button class="ag-action-btn ag-action-compare" data-action="compare" title="Comparer les fournisseurs">🔍</button>`
        : '';
      const deleteBtn = `<button class="ag-action-btn ag-action-delete" data-action="delete" title="Retirer de la suggestion">🗑</button>`;
      return `<div class="ag-actions-cell">${resetBtn}${compareBtn}${deleteBtn}</div>`;
    },
  };

  /** Colonnes initiales (sans mois) — mises à jour dès que rowData arrive. */
  columnDefs: ColDef<SuggestionLigneEnrichie>[] = [
    ...this.baseColumnDefs,
    this.actionsColumnDef,
  ];

  readonly defaultColDef: ColDef = {
    resizable: true,
    suppressMovable: false,
    filter: false,
  };

  // ─── Local UI state ──────────────────────────────────────────────────────────
  readonly searchText = signal('');
  readonly urgenceFilter = signal<string>('TOUS');
  readonly selectedLignes = signal<SuggestionLigneEnrichie[]>([]);
  readonly produitEnCours = signal<ProduitSearch | null>(null);
  quantiteSaisie = 1;

  // ─── Computed ────────────────────────────────────────────────────────────────
  readonly lignesSelectionnees = computed(() => this.selectedLignes());

  readonly montantSelection = computed(() =>
    this.selectedLignes().reduce((s, l) => s + l.quantite * l.prixAchat, 0),
  );

  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.rows())));

  readonly rowClassRules = {
    'ag-row-urgent': (params: any) => params.data?.niveauUrgence === 'URGENT',
    'ag-row-normal': (params: any) => params.data?.niveauUrgence === 'NORMAL',
  };

  readonly getRowId = (params: any): string =>
    String(params.data?.id ?? `${params.data?.produitId}-${params.data?.libelle}`);

  constructor() {
    effect(() => {
      const data = this.lignes();
      if (this.gridApi) {
        this.syncMoisColumns(data);
        this.gridApi.setGridOption('rowData', data);
      }
    });
  }

  onGridReady(event: GridReadyEvent): void {
    this.gridApi = event.api;
    this.syncMoisColumns(this.lignes());
    this.gridApi.setGridOption('rowData', this.lignes());
  }

  // ── Génération dynamique des colonnes consommationMensuelle ───────────────
  private syncMoisColumns(data: SuggestionLigneEnrichie[]): void {
    const first = data.find(l => l.consommationMensuelle && Object.keys(l.consommationMensuelle).length > 0);
    const newKeys = first ? Object.keys(first.consommationMensuelle!) : [];
    if (JSON.stringify(newKeys) === JSON.stringify(this.currentMoisKeys)) return;
    this.currentMoisKeys = newKeys;

    const moisCols: ColDef<SuggestionLigneEnrichie>[] = newKeys.map(mois => ({
      headerName: mois,
      colId: `conso_${mois}`,
      width: 65,
      sortable: false,
      resizable: true,
      type: 'numericColumn',
      valueGetter: (params: any) => params.data?.consommationMensuelle?.[mois] ?? 0,
      valueFormatter: (params: any) => params.value > 0 ? String(params.value) : '—',
      cellStyle: { fontSize: '11px', color: '#6b7280' },
    }));

    this.gridApi!.setGridOption('columnDefs', [
      ...this.baseColumnDefs,
      ...moisCols,
      this.actionsColumnDef,
    ]);
  }

  onSelectionChanged(): void {
    const selected = this.gridApi?.getSelectedRows() as SuggestionLigneEnrichie[] ?? [];
    this.selectedLignes.set(selected);
    this.selectionChanged.emit(selected);
  }

  onCellValueChanged(event: CellValueChangedEvent): void {
    if (event.column.getColId() !== 'quantite') return;
    const ligne = event.data as SuggestionLigneEnrichie;
    const qte = Number(event.newValue);
    if (!Number.isFinite(qte) || qte < 0) {
      event.node.setDataValue('quantite', event.oldValue);
      return;
    }
    this.quantiteChanged.emit({ ligne, qte });
  }

  onCellClicked(event: CellClickedEvent): void {
    const target = event.event?.target as HTMLElement;
    const actionEl = target.closest('[data-action]');
    if (!actionEl) return;
    const action = actionEl.getAttribute('data-action');
    const ligne = event.data as SuggestionLigneEnrichie;
    if (!ligne) return;
    switch (action) {
      case 'reset':
        this.resetQte.emit(ligne);
        // Optimistic update local — effacé visuellement avant retour API
        event.node.setDataValue('quantiteModifieeManuel', false);
        event.node.setDataValue('quantiteModifiee', false);
        break;
      case 'compare':
        this.comparerRequest.emit(ligne);
        break;
      case 'delete':
        this.ligneSupprimer.emit(ligne);
        break;
    }
  }

  // ─── Pagination & filtres ────────────────────────────────────────────────────

  setUrgenceFilter(val: string): void {
    this.urgenceFilter.set(val);
    this._clearSelection();
    this.filterChange.emit({ search: this.searchText(), urgence: val });
  }

  onSearchInput(value: string): void {
    this.searchText.set(value);
    this._clearSelection();
    this.filterChange.emit({ search: value, urgence: this.urgenceFilter() });
  }

  goToPage(p: number): void {
    if (p < 0 || p >= this.totalPages()) return;
    this._clearSelection();
    this.pageChange.emit({ page: p, rows: this.rows() });
  }

  private _clearSelection(): void {
    this.gridApi?.deselectAll();
    this.selectedLignes.set([]);
  }

  // ─── Bulk delete sélection ───────────────────────────────────────────────────

  onSupprimerSelection(): void {
    this.lignesSupprimer.emit(this.selectedLignes());
  }

  // ─── Ajout produit via autocomplete + quantité ───────────────────────────────

  onProduitSelected(produit: ProduitSearch | null): void {
    if (!produit) {
      this.produitEnCours.set(null);
      return;
    }
    this.produitEnCours.set(produit);
    this.quantiteSaisie = 1;
    // Laisser l'overlay autocomplete se fermer avant de prendre le focus
    setTimeout(() => {
      const el = this.quantityBoxRef()?.nativeElement;
      el?.focus();
      el?.select();
    }, 80);
  }

  onAjouter(): void {
    const produit = this.produitEnCours();
    if (!produit) return;
    const produitId = produit.id;
    const fournisseurProduitId = produit.fournisseurProduit?.id;
    if (!produitId || !fournisseurProduitId) return;
    const quantite = Number(this.quantiteSaisie) || 1;
    this.ajouterProduit.emit({ produitId, fournisseurProduitId, quantite });
    this.produitEnCours.set(null);
    this.quantiteSaisie = 1;
    requestAnimationFrame(() => {
      this.searchComp()?.reset();
      this.searchComp()?.getFocus();
    });
  }

  // ─── Helpers UI ─────────────────────────────────────────────────────────────

  statutSeverity(statut: string | undefined): 'success' | 'warn' | 'danger' | 'secondary' | 'info' {
    switch (statut) {
      case 'VALIDEE': return 'success';
      case 'EN_ATTENTE_VALIDATION': return 'warn';
      case 'COMMANDEE': return 'info';
      default: return 'secondary';
    }
  }

  canValider(): boolean {
    const s = this.fournisseur()?.statut;
    return s === 'GENEREE' || s === 'OPEN' || s === 'EN_ATTENTE_VALIDATION';
  }

  canRejeter(): boolean {
    const s = this.fournisseur()?.statut;
    return s === 'GENEREE' || s === 'OPEN' || s === 'EN_ATTENTE_VALIDATION' || s === 'VALIDEE';
  }

  getRowClass(params: any): string {
    const ligne: SuggestionLigneEnrichie = params.data;
    if (ligne?.niveauUrgence === 'URGENT') return 'ag-row-urgent';
    if (ligne?.niveauUrgence === 'NORMAL') return 'ag-row-normal';
    return '';
  }
}
