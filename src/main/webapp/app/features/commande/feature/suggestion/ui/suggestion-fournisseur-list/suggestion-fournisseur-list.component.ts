import { Component, computed, effect, input, output, signal, viewChild } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FournisseurSuggestionSummary } from '../../data-access/suggestion-enrichie.model';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  CellClickedEvent,
  ClientSideRowModelModule,
  ColDef,
  GetRowIdFunc,
  ModuleRegistry,
  RowClassRules,
  themeAlpine,
} from 'ag-grid-community';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-suggestion-fournisseur-list',
  templateUrl: './suggestion-fournisseur-list.component.html',
  styleUrls: ['./suggestion-fournisseur-list.component.scss'],
  imports: [CommonModule, DecimalPipe, AgGridAngular],
})
export class SuggestionFournisseurListComponent {
  // ── Inputs / Outputs ───────────────────────────────────────────────────────
  fournisseurs = input<FournisseurSuggestionSummary[]>([]);
  selected     = input<FournisseurSuggestionSummary | null>(null);
  loading      = input(false);

  fournisseurSelected        = output<FournisseurSuggestionSummary>();
  supprimerSuggestionRequest = output<number>();
  supprimerSelectionRequest  = output<number[]>();
  fusionnerRequest           = output<number[]>();
  validerRequest             = output<FournisseurSuggestionSummary>();
  exportPdfRequest           = output<FournisseurSuggestionSummary>();
  exportCsvRequest           = output<FournisseurSuggestionSummary>();
  commanderRequest           = output<FournisseurSuggestionSummary>();
  selectionCountChange       = output<number>();
  canFusionnerChange         = output<boolean>();

  // ── Sélection multiple (bulk actions) ─────────────────────────────────────
  readonly selectionMultiple = signal<FournisseurSuggestionSummary[]>([]);
  readonly selectionCount    = computed(() => this.selectionMultiple().length);
  readonly canFusionner      = computed(() => {
    const sel = this.selectionMultiple();
    if (sel.length < 2) return false;
    const fId = sel[0].fournisseurId;
    return sel.every(f => f.fournisseurId === fId);
  });

  // ── Totaux ─────────────────────────────────────────────────────────────────
  readonly totalMontant = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.montantEstime, 0)
  );
  readonly totalUrgents = computed(() =>
    this.fournisseurs().reduce((s, f) => s + f.nbUrgents, 0)
  );

  // ── AG Grid ────────────────────────────────────────────────────────────────
  protected readonly theme        = themeAlpine;
  protected readonly defaultColDef: ColDef = { resizable: true, sortable: false, suppressHeaderMenuButton: true };
  protected readonly getRowId: GetRowIdFunc<FournisseurSuggestionSummary> =
    p => String(p.data.suggestionId ?? p.data.fournisseurId);

  protected readonly rowClassRules: RowClassRules<FournisseurSuggestionSummary> = {
    'row-active':    p => !!p.data && this.selected()?.suggestionId === p.data.suggestionId,
    'row-urgent':    p => !!p.data && p.data.nbUrgents > 0 && this.selected()?.suggestionId !== p.data.suggestionId,
    'row-selected':  p => !!p.data && this.isSelected(p.data),
  };

  private readonly gridRef = viewChild(AgGridAngular);

  constructor() {
    // Redessine les lignes quand la sélection active ou la multi-sélection change
    effect(() => {
      this.selected();
      this.selectionMultiple();
      this.gridRef()?.api?.redrawRows();
    });
  }

  protected readonly colDefs: ColDef<FournisseurSuggestionSummary>[] = [
    {
      colId: 'select',
      headerName: '',
      width: 36,
      sortable: false,
      cellRenderer: (p: any) => {
        if (!p.data?.suggestionId) return '';
        const checked = this.isSelected(p.data);
        return `<span data-action="select"
          style="display:flex;align-items:center;justify-content:center;height:100%;cursor:pointer;padding:0 4px">
          <i class="pi ${checked ? 'pi-check-square' : 'pi-stop'}"
             style="font-size:0.875rem;color:${checked ? '#5b89a6' : '#9ca3af'}"></i>
        </span>`;
      },
    },
    {
      field: 'updatedAt',
      headerName: 'Date',
      width: 120,
      sortable: true,
      valueFormatter: p => p.value
        ? new Date(p.value as string).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' })
        : '—',
    },
    {
      colId: 'libelle',
      headerName: 'Fournisseur',
      flex: 1,
      minWidth: 130,
      cellRenderer: (p: any) => {
        if (!p.data) return '';
        const urgent = p.data.nbUrgents > 0
          ? `<i class="pi pi-bolt" style="font-size:0.72rem;color:#dc3545;margin-right:3px" title="${p.data.nbUrgents} urgent(s)"></i>`
          : '';
        return `<span style="display:inline-flex;align-items:center;gap:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
          ${urgent}<span title="${p.data.libelle}">${p.data.libelle}</span>
        </span>`;
      },
    },
    {
      field: 'nbProduits',
      headerName: 'Produits',
      width: 80,
      type: 'numericColumn',
    },
    {
      field: 'nbUrgents',
      headerName: 'Urgents',
      width: 75,
      type: 'numericColumn',
      cellRenderer: (p: any) => {
        if (!p.data || p.data.nbUrgents === 0) return '<span style="color:#9ca3af">—</span>';
        return `<span style="font-weight:700;color:#dc3545">${p.data.nbUrgents}</span>`;
      },
    },
    {
      field: 'montantEstime',
      headerName: 'Montant estimé',
      width: 125,
      type: 'numericColumn',
      valueFormatter: p => p.value != null ? `${Number(p.value).toLocaleString('fr-FR')} F` : '—',
    },
    {
      field: 'statut',
      headerName: 'Statut',
      width: 120,
      cellRenderer: (p: any) => {
        if (!p.value) return '';
        const colors: Record<string, string> = {
          VALIDEE:               'background:#d1fae5;color:#065f46',
          EN_ATTENTE_VALIDATION: 'background:#fef3c7;color:#92400e',
          COMMANDEE:             'background:#dbeafe;color:#1e40af',
        };
        const style = colors[p.value] ?? 'background:#f3f4f6;color:#4b5563';
        const labels: Record<string, string> = {
          VALIDEE: 'Validée', EN_ATTENTE_VALIDATION: 'En attente', COMMANDEE: 'Commandée',
        };
        return `<span style="padding:2px 8px;border-radius:10px;font-size:0.68rem;font-weight:700;${style}">${labels[p.value] ?? p.value}</span>`;
      },
    },
    {
      colId: 'actions',
      headerName: '',
      width: 150,
      sortable: false,
      cellRenderer: (p: any) => {
        if (!p.data?.suggestionId) return '';
        const validerBtn = p.data.statut !== 'VALIDEE'
          ? `<button data-action="valider" title="Valider" style="background:rgba(5,150,105,0.1);color:#059669;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-check"></i></button>`
          : '';
        return `<span style="display:flex;align-items:center;gap:2px">
          ${validerBtn}
          <button data-action="commander" title="Commander" style="background:rgba(91,137,166,0.1);color:#5b89a6;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-shopping-cart"></i></button>
          <button data-action="pdf" title="PDF" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-file-pdf"></i></button>
          <button data-action="csv" title="CSV" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-file-excel"></i></button>
          <button data-action="delete" title="Supprimer" style="background:rgba(220,53,69,0.1);color:#dc3545;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-trash"></i></button>
        </span>`;
      },
    },
  ];

  onCellClicked(event: CellClickedEvent<FournisseurSuggestionSummary>): void {
    if (!event.data) return;
    const action = (event.event?.target as HTMLElement)?.closest('[data-action]')?.getAttribute('data-action');
    if (action === 'select') {
      this.toggleSelect(event.data, event.event as MouseEvent);
    } else if (action === 'valider') {
      event.event?.stopPropagation();
      this.validerRequest.emit(event.data);
    } else if (action === 'commander') {
      event.event?.stopPropagation();
      this.commanderRequest.emit(event.data);
    } else if (action === 'pdf') {
      event.event?.stopPropagation();
      this.exportPdfRequest.emit(event.data);
    } else if (action === 'csv') {
      event.event?.stopPropagation();
      this.exportCsvRequest.emit(event.data);
    } else if (action === 'delete') {
      event.event?.stopPropagation();
      if (event.data.suggestionId) this.supprimerSuggestionRequest.emit(event.data.suggestionId);
    } else if (!action) {
      this.fournisseurSelected.emit(event.data);
    }
  }

  // ── Sélection ──────────────────────────────────────────────────────────────

  isSelected(f: FournisseurSuggestionSummary): boolean {
    return this.selectionMultiple().some(s => s.suggestionId === f.suggestionId && s.suggestionId != null);
  }

  toggleSelect(f: FournisseurSuggestionSummary, event: MouseEvent): void {
    event.stopPropagation();
    if (!f.suggestionId) return;
    this.selectionMultiple.update(sel => {
      const exists = sel.some(s => s.suggestionId === f.suggestionId);
      return exists ? sel.filter(s => s.suggestionId !== f.suggestionId) : [...sel, f];
    });
    this.gridRef()?.api.refreshCells({ columns: ['select'], force: true });
    this.selectionCountChange.emit(this.selectionMultiple().length);
    this.canFusionnerChange.emit(this.canFusionner());
  }

  // ── Bulk actions ───────────────────────────────────────────────────────────

  onFusionner(): void {
    const ids = this.selectionMultiple().map(f => f.suggestionId!).filter(id => id != null);
    this.fusionnerRequest.emit(ids);
    this.selectionMultiple.set([]);
    this.selectionCountChange.emit(0);
    this.canFusionnerChange.emit(false);
  }

  onSupprimerSelection(): void {
    const ids = this.selectionMultiple().map(f => f.suggestionId!).filter(id => id != null);
    this.supprimerSelectionRequest.emit(ids);
    this.selectionMultiple.set([]);
    this.selectionCountChange.emit(0);
    this.canFusionnerChange.emit(false);
  }
}
