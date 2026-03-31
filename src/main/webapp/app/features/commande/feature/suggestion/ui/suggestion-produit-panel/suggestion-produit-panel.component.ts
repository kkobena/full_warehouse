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
import { SuggestionProduitActionsComponent } from './suggestion-produit-actions.component';
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
    SuggestionProduitActionsComponent,
  ],
})
export class SuggestionProduitPanelComponent {
  // ─── Inputs ─────────────────────────────────────────────────────────────────
  fournisseur = input<FournisseurSuggestionSummary | null>(null);
  lignes = input<SuggestionLigneEnrichie[]>([]);
  loading = input(false);
  showPage = input(false);
  total = input(0);
  page = input(0);
  rows = input(0);
  exportingPdf = input(false);
  exportingCsv = input(false);

  // ─── ViewChild refs ──────────────────────────────────────────────────────────
  private readonly searchComp = viewChild<CommandeProductSearchComponent>('produitSearch');
  private readonly quantityBoxRef = viewChild<ElementRef>('quantityBox');

  // ─── Outputs ─────────────────────────────────────────────────────────────────
  commander = output<void>();
  commanderSelection = output<void>();
  /** Émet les lignes visibles après filtrage urgence — pour "Commander les filtrés". */
  commanderFiltre = output<SuggestionLigneEnrichie[]>();
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
    enableClickSelection: false,
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
      width: 110,
      editable: true,
      type: 'numericColumn',
      cellEditor: 'agNumberCellEditor',
      cellRenderer: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        if (!ligne) return String(params.value ?? 0);
        const qty: number = params.value ?? 0;
        const lock = ligne.quantiteModifieeManuel
          ? `<span class="ag-lock-icon" title="Quantité verrouillée (modifiée manuellement)">🔒</span> `
          : '';
        const colis = ligne.qteColis ?? 1;
        const qteMin = ligne.qteMinimaleCommande ?? 0;

        // Avertissement : qté non multiple du colisage
        const colisWarn = colis > 1 && qty % colis !== 0
          ? `<span class="ag-colis-warn" title="⚠ Non multiple du colisage (colis = ${colis})">⚠</span>`
          : '';
        // Avertissement : qté sous le minimum fournisseur
        const minWarn = qteMin > 0 && qty < qteMin
          ? `<span class="ag-min-warn" title="⚠ Qté sous le minimum fournisseur (min = ${qteMin})">🔻</span>`
          : '';
        // Badge colisage si contrainte configurée
        const colisBadge = colis > 1
          ? `<span class="ag-colis-badge" title="Colis de ${colis} unités">×${colis}</span>`
          : '';

        return `<div class="ag-qty-cell">${lock}${qty}${colisWarn}${minWarn}${colisBadge}</div>`;
      },
      cellStyle: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        if (!ligne) return null;
        const qty: number = params.value ?? 0;
        const colis = ligne.qteColis ?? 1;
        const qteMin = ligne.qteMinimaleCommande ?? 0;
        const hasColisError = colis > 1 && qty % colis !== 0;
        const hasMinError = qteMin > 0 && qty < qteMin;
        if (hasColisError || hasMinError) {
          return { backgroundColor: '#fff8e1', borderColor: '#f59e0b', color: '#92400e', fontWeight: 700 };
        }
        return ligne.quantiteModifieeManuel
          ? { backgroundColor: '#fffbeb', borderColor: '#f59e0b', fontWeight: 700, color: '#92400e' }
          : ligne.quantiteModifiee
            ? { backgroundColor: '#eff6ff', fontWeight: 600 }
            : null;
      },
      tooltipValueGetter: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        if (!ligne) return null;
        const parts: string[] = [];
        const colis = ligne.qteColis ?? 1;
        const qteMin = ligne.qteMinimaleCommande ?? 0;
        if (colis > 1) parts.push(`Colisage : ${colis} unités/colis`);
        if (qteMin > 0) parts.push(`Minimum commande : ${qteMin} unités`);
        if (ligne.quantiteModifieeManuel) parts.push('🔒 Quantité modifiée manuellement');
        return parts.length > 0 ? parts.join(' | ') : null;
      },
    },
    {
      field: 'joursRestants',
      headerName: 'Couv. act.',
      width: 90,
      sortable: true,
      type: 'numericColumn',
      valueFormatter: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        // Stock négatif ou nul → rupture confirmée, afficher "Rupture" plutôt que "0j"
        if (ligne && ligne.currentStock <= 0) return 'Rupture';
        return params.value != null ? `${params.value}j` : '—';
      },
      cellStyle: (params: any) => {
        const ligne: SuggestionLigneEnrichie = params.data;
        if (ligne && ligne.currentStock <= 0) return { color: '#dc3545', fontWeight: 700 };
        if (params.value == null) return null;
        if (params.value < 7) return { color: '#dc3545', fontWeight: 700 };
        if (params.value < 30) return { color: '#fd7e14' };
        return null;
      },
    },
    {
      // Simulation "après commande" — calculé depuis stock + quantite commandée + VMM
      headerName: 'Couv. après',
      colId: 'couv_apres',
      width: 95,
      sortable: false,
      type: 'numericColumn',
      tooltipValueGetter: () => 'Couverture estimée si la commande est reçue',
      valueGetter: (params: any) => {
        const l: SuggestionLigneEnrichie = params.data;
        if (!l || !l.consommationMensuelle) return null;
        const vals = Object.values(l.consommationMensuelle);
        if (vals.length === 0) return null;
        const vmm = vals.reduce((s, v) => s + v, 0) / vals.length;
        if (vmm <= 0) return null;
        // GREATEST(0, stock) : un stock négatif compensé par la commande donne le vrai résultat
        const stockBase = Math.max(0, l.currentStock);
        const stockApres = stockBase + l.quantite;
        return Math.round((stockApres / vmm) * 30);
      },
      valueFormatter: (params: any) => params.value != null ? `${params.value}j` : '—',
      cellStyle: (params: any) => {
        if (params.value == null) return { color: '#9ca3af' };
        if (params.value < 7) return { color: '#dc3545', fontWeight: 700 };
        if (params.value < 30) return { color: '#fd7e14' };
        return { color: '#16a34a', fontWeight: 600 };
      },
    },
    {
      field: 'prixAchat',
      headerName: 'P.A',
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
    colId: 'actions',
    width: 110,
    pinned: 'right' as const,
    sortable: false,
    resizable: false,
    suppressMovable: true,
    cellRenderer: SuggestionProduitActionsComponent,
  };

  protected readonly gridContext: {componentParent: SuggestionProduitPanelComponent} = {componentParent: this};

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

 // readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.rows())));

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
        // Réappliquer quickFilterText et filtre urgence au rechargement des lignes
        this.gridApi.setGridOption('quickFilterText', this.searchText());
        this.gridApi.onFilterChanged();
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

    // ── Colonne VMM + tendance (insérée juste avant les colonnes mois) ────────
    const vmmColDef: ColDef<SuggestionLigneEnrichie> | null = newKeys.length >= 2 ? {
      headerName: 'VMM ↑↓',
      colId: 'vmm_tendance',
      width: 90,
      sortable: true,
      type: 'numericColumn',
      valueGetter: (params: any) => {
        const conso: Record<string, number> | undefined = params.data?.consommationMensuelle;
        if (!conso) return null;
        const vals = Object.values(conso);
        if (vals.length === 0) return null;
        return Math.round(vals.reduce((s, v) => s + v, 0) / vals.length);
      },
      cellRenderer: (params: any) => {
        if (params.value == null) return '—';
        const conso: Record<string, number> | undefined = params.data?.consommationMensuelle;
        if (!conso) return String(params.value);
        const vals = Object.values(conso);
        if (vals.length < 2) return String(params.value);
        const moitie = Math.floor(vals.length / 2);
        const debut = vals.slice(0, moitie).reduce((s, v) => s + v, 0) / moitie;
        const fin = vals.slice(moitie).reduce((s, v) => s + v, 0) / (vals.length - moitie);
        const diff = fin - debut;
        const arrow = diff > 1 ? '↑' : diff < -1 ? '↓' : '↔';
        const color = diff > 1 ? '#16a34a' : diff < -1 ? '#dc2626' : '#6b7280';
        return `<span style="color:${color};font-weight:600">${params.value} ${arrow}</span>`;
      },
      tooltipValueGetter: (params: any) => {
        if (params.value == null) return 'Données insuffisantes';
        return `VMM moyen: ${params.value} unités/mois`;
      },
    } : null;

    const extraCols = vmmColDef ? [vmmColDef, ...moisCols] : moisCols;

    this.gridApi!.setGridOption('columnDefs', [
      ...this.baseColumnDefs,
      ...extraCols,
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

  onCellClicked(_event: CellClickedEvent): void {
    // Actions handled by SuggestionProduitActionsComponent renderer
  }

  // ── Méthodes appelées par SuggestionProduitActionsComponent ──────────────

  onResetLigne(ligne: SuggestionLigneEnrichie): void {
    this.resetQte.emit(ligne);
  }

  onComparerLigne(ligne: SuggestionLigneEnrichie): void {
    this.comparerRequest.emit(ligne);
  }

  onSupprimerLigne(ligne: SuggestionLigneEnrichie): void {
    this.ligneSupprimer.emit(ligne);
  }

  // ─── Pagination & filtres ────────────────────────────────────────────────────

  setUrgenceFilter(val: string): void {
    this.urgenceFilter.set(val);
    this._clearSelection();
    this.gridApi?.onFilterChanged();
    // Désactiver les checkboxes quand un filtre urgence est actif
    this.gridApi?.setGridOption('rowSelection', {
      mode: 'multiRow',
      checkboxes: val === 'TOUS',
      headerCheckbox: val === 'TOUS',
      enableClickSelection: false,
    });
    this.filterChange.emit({ search: this.searchText(), urgence: val });
  }

  /** Retourne les lignes actuellement visibles dans la grille (après filtres urgence + quickFilter). */
  getFilteredLignes(): SuggestionLigneEnrichie[] {
    const result: SuggestionLigneEnrichie[] = [];
    this.gridApi?.forEachNodeAfterFilter(node => {
      if (node.data) result.push(node.data as SuggestionLigneEnrichie);
    });
    return result;
  }

  /** Nombre de lignes filtrées — utilisé par le bouton "Commander les filtrés". */
  get nbFilteredLignes(): number {
    if (this.urgenceFilter() === 'TOUS') return this.lignes().length;
    return this.getFilteredLignes().length;
  }

  onCommanderFiltre(): void {
    const filtered = this.getFilteredLignes();
    if (filtered.length === 0) return;
    this.commanderFiltre.emit(filtered);
  }

  onSupprimerFiltrees(): void {
    const filtered = this.getFilteredLignes();
    if (filtered.length === 0) return;
    this.lignesSupprimer.emit(filtered);
  }

  onSearchInput(value: string): void {
    this.searchText.set(value);
    this._clearSelection();
    // Quick filter AG Grid — côté client, pas de rechargement backend
    this.gridApi?.setGridOption('quickFilterText', value);
  }

  // ─── Filtre externe AG Grid (urgence) ────────────────────────────────────
  readonly isExternalFilterPresent = (): boolean => this.urgenceFilter() !== 'TOUS';

  readonly doesExternalFilterPass = (node: any): boolean => {
    const ligne: SuggestionLigneEnrichie = node.data;
    if (!ligne) return true;
    const filter = this.urgenceFilter();
    return filter === 'TOUS' || ligne.niveauUrgence === filter;
  };

  goToPage(p: number): void {
  //  if (p < 0 || p >= this.totalPages()) return;
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



  canValider(): boolean {
    const s = this.fournisseur()?.statut;
    return s === 'GENEREE' || s === 'OPEN' || s === 'EN_ATTENTE_VALIDATION';
  }

}
