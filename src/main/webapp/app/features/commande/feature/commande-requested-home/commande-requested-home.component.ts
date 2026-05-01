import {Component, computed, DestroyRef, inject, Injector, OnInit, output, signal} from '@angular/core';
import {takeUntilDestroyed, toObservable} from '@angular/core/rxjs-interop';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {filter, finalize} from 'rxjs/operators';
import {HttpResponse} from '@angular/common/http';
import {saveAs} from 'file-saver';
import {ButtonModule} from 'primeng/button';
import {TooltipModule} from 'primeng/tooltip';
import {InputTextModule} from 'primeng/inputtext';
import {Toast} from 'primeng/toast';
import {TableModule} from 'primeng/table';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
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
import {CommandeRequestedAction, CommandeRequestedActionsComponent} from './commande-requested-actions.component';

@Component({
  selector: 'app-commande-requested-home',
  templateUrl: './commande-requested-home.component.html',
  styleUrls: ['./commande-requested-home.component.scss'],
  imports: [
    CommonModule,
    FormsModule,
    CommandeRequestedComponent,
    ButtonModule,
    TooltipModule,
    InputTextModule,
    DatePipe,
    Toast,
    TableModule,
    CommandeRequestedActionsComponent,
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

  // ── Sélection multiple (native PrimeNG) ───────────────────────────────────
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
    this.commandes().reduce((s, c) => s + (c.grossAmount ?? 0), 0),
  );

  // ── Row styling ────────────────────────────────────────────────────────────
  getRowClass(c: ICommande): Record<string, boolean> {
    return {
      'row-reliquat': !!c.reliquatDeCommandeId,
    };
  }

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

    toObservable(this.commandCommonService.pendingNewCommande, {injector: this.injector})
      .pipe(
        filter(v => v),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.commandCommonService.pendingNewCommande.set(false);
        this.onNouvelleCommande();
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

  onCommandeMenuAction(action: CommandeRequestedAction, c: ICommande): void {
    switch (action) {
      case 'editer':       this.onEditer(c); break;
      case 'receptionner': this.onReceptionner(c); break;
      case 'exportCsv':    this.exportCsv(c); break;
      case 'exportPdf':    this.exportPdf(c); break;
      case 'supprimer':    this.onSupprimerCommande(c); break;
    }
  }

  onSupprimerCommande(c: ICommande): void {
    this.confirmDialog.onConfirm(
      () => this.deleteCommande(c.commandeId),
      'Supprimer la commande',
      `Supprimer définitivement la commande ${c.orderReference ?? ''} ? Cette action est irréversible.`,
    );
  }

  exportCsv(c: ICommande): void {
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

  exportPdf(c: ICommande): void {
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

  onReceptionner(c: ICommande): void {
    this.commandeService.find(c.commandeId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(res => {
      if (!res.body) return;
      showCommonModal(
        this.modalService,
        DeliveryModalComponent,
        {commande: res.body, header: 'CREATION DU BON DE LIVRAISON'},
        () => {
          this.commandCommonService.pendingOpenDeliveryId.set(c.commandeId);
          this.commandCommonService.navigateToBonsLivraison();
        },
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

  // ── Sélection native PrimeNG ──────────────────────────────────────────────

  onSelectionChange(items: ICommande[]): void {
    this.selectionMultiple.set(items);
  }

  // ── Bulk actions ──────────────────────────────────────────────────────────

  onFusionner(): void {
    if (!this.canFusionner()) return;
    const ids = this.selectionMultiple().map(c => c.commandeId);

    this.commandeService
      .fusionner(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.selectionMultiple.set([]);
          this.loadPage(this.currentPage());
          this.notificationService.success('Commandes fusionnées avec succès', 'Succès');
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
