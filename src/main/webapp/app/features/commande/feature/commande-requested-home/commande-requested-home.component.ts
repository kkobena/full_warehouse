import {Component, computed, DestroyRef, inject, Injector, OnInit, output, signal, viewChild} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {CommonModule, DatePipe, DecimalPipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {filter, finalize} from 'rxjs/operators';
import {HttpResponse} from '@angular/common/http';
import {saveAs} from 'file-saver';
import {ButtonModule} from 'primeng/button';
import {TagModule} from 'primeng/tag';
import {TooltipModule} from 'primeng/tooltip';
import {InputTextModule} from 'primeng/inputtext';
import {Toast} from 'primeng/toast';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {AgGridAngular} from 'ag-grid-angular';
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
import {ICommande} from 'app/shared/model/commande.model';
import {CommandeId} from 'app/shared/model/abstract-commande.model';
import {CommandeService} from '../../../../entities/commande/commande.service';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {NotificationService} from '../../../../shared/services/notification.service';
import {TauriPrinterService} from '../../../../shared/services/tauri-printer.service';
import {handleBlobForTauri} from '../../../../shared/util/tauri-util';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {ImportationNewCommandeComponent} from '../../../../entities/commande/importation-new-commande.component';
import {ICommandeResponse} from '../../../../shared/model/commande-response.model';
import {CommandeImportResponseDialogComponent} from '../../../../entities/commande/commande-import-response-dialog.component';
import {CommandeRequestedComponent} from '../commande-requested/commande-requested.component';
import {CommandCommonService} from '../../../../entities/commande/command-common.service';
import {DeliveryModalComponent} from '../../ui/delivery/delivery-modal/delivery-modal.component';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

@Component({
  selector: 'app-commande-requested-home',
  templateUrl: './commande-requested-home.component.html',
  styleUrls: ['./commande-requested-home.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    CommandeRequestedComponent,
    ButtonModule,
    TagModule,
    TooltipModule,
    InputTextModule,
    DecimalPipe,
    DatePipe,
    Toast,
    AgGridAngular,
  ],
})
export class CommandeRequestedHomeComponent implements OnInit {

  readonly countChange = output<number>();

  // ── État master/detail ────────────────────────────────────────────────────
  readonly editingCommande = signal<ICommande | null>(null);

  // ── Liste ─────────────────────────────────────────────────────────────────
  readonly commandes = signal<ICommande[]>([]);
  readonly loading = signal(false);
  readonly totalItems = signal(0);
  readonly currentPage = signal(0);
  readonly rows = 30;

  // ── Recherche ─────────────────────────────────────────────────────────────
  protected searchText = '';

  // ── Sélection multiple (bulk actions) ─────────────────────────────────────
  readonly selectionMultiple = signal<ICommande[]>([]);
  readonly selectionCount = computed(() => this.selectionMultiple().length);
  readonly canFusionner = computed(() => {
    const sel = this.selectionMultiple();
    if (sel.length < 2) return false;
    const fId = sel[0].fournisseur?.id;
    return sel.every(c => c.fournisseur?.id === fId);
  });

  // ── Totaux page courante ───────────────────────────────────────────────────
  readonly totalAmount = computed(() =>
    this.commandes().reduce((s, c) => s + (c.grossAmount ?? 0), 0)
  );

  // ── AG Grid ───────────────────────────────────────────────────────────────
  protected readonly theme = themeAlpine;
  protected readonly defaultColDef: ColDef = { resizable: true, sortable: false, suppressHeaderMenuButton: true };
  protected readonly getRowId: GetRowIdFunc<ICommande> = p => `${p.data.id}_${p.data.orderDate}`;
  protected readonly rowClassRules: RowClassRules<ICommande> = {
    'row-reliquat': p => !!p.data?.reliquatDeCommandeId,
    'row-selected': p => !!p.data && this.isSelected(p.data),
  };
  private readonly gridRef = viewChild(AgGridAngular);

  protected readonly colDefs: ColDef<ICommande>[] = [
    {
      colId: 'select',
      headerName: '',
      width: 36,
      sortable: false,
      cellRenderer: (p: any) => {
        if (!p.data) return '';
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
      width: 135,
      sortable: true,
      valueFormatter: p => p.value
        ? new Date(p.value as string).toLocaleString('fr-FR', {dateStyle: 'short', timeStyle: 'short'})
        : '—',
    },
    {
      colId: 'fournisseur',
      headerName: 'Fournisseur',
      flex: 1,
      minWidth: 140,
      valueGetter: p => p.data?.fournisseur?.libelle ?? '—',
    },
    {
      field: 'orderReference',
      headerName: 'Référence',
      width: 145,
      cellStyle: {fontFamily: 'monospace', fontSize: '12px'},
    },
    {
      field: 'itemSize',
      headerName: 'Lignes',
      width: 70,
      type: 'numericColumn',
    },
    {
      field: 'grossAmount',
      headerName: 'Montant HT',
      width: 120,
      type: 'numericColumn',
      valueFormatter: p => p.value != null ? Number(p.value).toLocaleString('fr-FR') : '—',
    },
    {
      colId: 'reliquat',
      headerName: '',
      width: 85,
      cellRenderer: (p: any) => {
        if (!p.data?.reliquatDeCommandeId) return '';
        return `<span style="padding:2px 6px;border-radius:10px;font-size:0.68rem;font-weight:700;background:#fef3c7;color:#92400e">RELIQUAT</span>`;
      },
    },
    {
      colId: 'actions',
      headerName: '',
      width: 115,
      sortable: false,
      cellRenderer: (p: any) => {
        if (!p.data) return '';
        return `<span style="display:flex;align-items:center;gap:2px">
          <button data-action="receive" title="Réceptionner" style="background:rgba(5,150,105,0.1);color:#059669;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-inbox"></i></button>
          <button data-action="csv" title="Export CSV" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-file-excel"></i></button>
          <button data-action="pdf" title="Imprimer" style="background:none;border:none;cursor:pointer;color:#6c757d;font-size:12px;padding:2px 4px"><i class="pi pi-print"></i></button>
          <button data-action="delete" title="Supprimer" style="background:rgba(220,53,69,0.1);color:#dc3545;border:none;border-radius:4px;padding:3px 6px;cursor:pointer;font-size:12px"><i class="pi pi-trash"></i></button>
        </span>`;
      },
    },
  ];

  private readonly commandeService = inject(CommandeService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly commandCommonService = inject(CommandCommonService);

  ngOnInit(): void {
    this.loadPage(0);

    toObservable(this.commandCommonService.pendingOpenCommandeId, {injector: this.injector})
      .pipe(
        filter(id => id != null),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(pending => {
        this.commandCommonService.pendingOpenCommandeId.set(null);
        this.commandeService.find(pending!).subscribe(res => {
          if (res.body) {
            this.editingCommande.set(res.body);
            this.loadPage(0);
          }
        });
      });
  }

  loadPage(page = 0): void {
    this.loading.set(true);
    this.commandeService
      .query({
        page,
        size: this.rows,
        search: this.searchText,
        orderStatuts: ['REQUESTED'],
      })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => {
          this.commandes.set(res.body ?? []);
          const total = Number(res.headers.get('X-Total-Count') ?? 0);
          this.totalItems.set(total);
          this.currentPage.set(page);
          this.countChange.emit(total);
        },
        error: () => this.notificationService.error('Erreur lors du chargement des commandes', 'Erreur'),
      });
  }

  // ── Navigation master/detail ──────────────────────────────────────────────

  onEditer(c: ICommande): void {
    this.editingCommande.set(c);
  }

  onNouvelleCommande(): void {
    // Commande vide — commande-requested gère la création si commandeId absent
    this.editingCommande.set({} as ICommande);
  }

  onRetour(): void {
    this.editingCommande.set(null);
    this.loadPage(this.currentPage());
  }

  onCommandeChange(c: ICommande | null): void {
    if (c) {
      this.editingCommande.set(c);
    }
  }

  // ── Actions unitaires ─────────────────────────────────────────────────────

  onSupprimerCommande(c: ICommande, event: MouseEvent): void {
    event.stopPropagation();
    this.confirmDialog.onConfirm(
      () => this.deleteCommande(c.commandeId),
      'Supprimer la commande',
      `Supprimer définitivement la commande ${c.orderReference ?? ''} ? Cette action est irréversible.`,
    );
  }

  exportCsv(c: ICommande, event: MouseEvent): void {
    event.stopPropagation();
    this.commandeService.exportToCsv(c.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(blob, 'commande', 'csv');
        } else {
          saveAs(blob);
        }
      },
      error: () => this.notificationService.error('Erreur export CSV', 'Erreur'),
    });
  }

  exportPdf(c: ICommande, event: MouseEvent): void {
    event.stopPropagation();
    this.commandeService.exportToPdf(c.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        if (this.tauriPrinter.isRunningInTauri()) {
          handleBlobForTauri(blob, 'commande');
        } else {
          window.open(URL.createObjectURL(blob));
        }
      },
      error: () => this.notificationService.error('Erreur export PDF', 'Erreur'),
    });
  }

  onReceptionner(c: ICommande, event: MouseEvent): void {
    event.stopPropagation();
    this.commandeService.find(c.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      if (!res.body) return;
      showCommonModal(
        this.modalService,
        DeliveryModalComponent,
        {commande: res.body, header: 'CREATION DU BON DE LIVRAISON'},
        () => this.loadPage(this.currentPage()),
        'lg',
      );
    });
  }

  // ── Importation (modale existante) ────────────────────────────────────────

  onImportation(): void {
    showCommonModal(
      this.modalService,
      ImportationNewCommandeComponent,
      {header: 'IMPORTATION DE NOUVELLE COMMANDE'},
      (reason: ICommandeResponse) => {
        this.loadPage(0);
        if (reason) {
          this.openImportResponseDialog(reason);
        }
      },
      'lg',
    );
  }

  // ── Bulk actions ──────────────────────────────────────────────────────────

  isSelected(c: ICommande): boolean {
    return this.selectionMultiple().some(s => s.id === c.id);
  }

  toggleSelect(c: ICommande, event: MouseEvent): void {
    event.stopPropagation();
    this.selectionMultiple.update(sel => {
      const exists = sel.some(s => s.id === c.id);
      return exists ? sel.filter(s => s.id !== c.id) : [...sel, c];
    });
    this.gridRef()?.api.refreshCells({columns: ['select'], force: true});
  }

  onCellClicked(event: CellClickedEvent<ICommande>): void {
    if (!event.data) return;
    const action = (event.event?.target as HTMLElement)?.closest('[data-action]')?.getAttribute('data-action');
    if (action === 'select') {
      this.toggleSelect(event.data, event.event as MouseEvent);
    } else if (action === 'receive') {
      this.onReceptionner(event.data, event.event as MouseEvent);
    } else if (action === 'csv') {
      this.exportCsv(event.data, event.event as MouseEvent);
    } else if (action === 'pdf') {
      this.exportPdf(event.data, event.event as MouseEvent);
    } else if (action === 'delete') {
      this.onSupprimerCommande(event.data, event.event as MouseEvent);
    } else if (!action) {
      this.onEditer(event.data);
    }
  }

  onFusionner(): void {
    if (!this.canFusionner()) return;
    const ids = this.selectionMultiple().map(c => ({id: c.id, orderDate: c.orderDate}));
    this.commandeService
      .fusionner(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.selectionMultiple.set([]);
          this.loadPage(this.currentPage());
        },
        error: () => this.notificationService.error('Erreur lors de la fusion', 'Erreur'),
      });
  }

  onSupprimerSelection(): void {
    const count = this.selectionCount();
    this.confirmDialog.onConfirm(
      () => {
        const ids = this.selectionMultiple().map(c => ({id: c.id, orderDate: c.orderDate}));
        this.commandeService
          .deleteSelectedCommandes(ids)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.selectionMultiple.set([]);
              this.loadPage(this.currentPage());
            },
            error: () => this.notificationService.error('Erreur lors de la suppression', 'Erreur'),
          });
      },
      'Supprimer les commandes',
      `Supprimer définitivement ${count} commande(s) ? Cette action est irréversible.`,
    );
  }

  // ── Pagination ────────────────────────────────────────────────────────────

  readonly totalPages = computed(() => Math.ceil(this.totalItems() / this.rows));

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.loadPage(page);
    }
  }

  // ── Privé ─────────────────────────────────────────────────────────────────

  private deleteCommande(commandeId: CommandeId): void {
    this.commandeService.delete(commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadPage(this.currentPage()),
      error: () => this.notificationService.error('Erreur lors de la suppression', 'Erreur'),
    });
  }

  private openImportResponseDialog(response: ICommandeResponse): void {
    this.modalService.open(CommandeImportResponseDialogComponent, {size: 'xl', scrollable: true}).componentInstance.response = response;
  }
}
