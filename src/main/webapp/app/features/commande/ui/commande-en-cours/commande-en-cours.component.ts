import { Component, DestroyRef, inject, OnInit, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ICommande } from 'app/shared/model/commande.model';
import { ICommandeResponse } from 'app/shared/model/commande-response.model';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CommandeService } from '../../../../entities/commande/commande.service';
import { Router, RouterModule } from '@angular/router';
import { ErrorService } from 'app/shared/error.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { InputTextModule } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { OrderStatut } from 'app/shared/model/enumerations/order-statut.model';
import { finalize } from 'rxjs/operators';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { NotificationService } from 'app/shared/services/notification.service';
import { SpinnerComponent } from 'app/shared/spinner/spinner.component';
import { CommandeId } from 'app/shared/model/abstract-commande.model';
import { TauriPrinterService } from 'app/shared/services/tauri-printer.service';
import { handleBlobForTauri } from 'app/shared/util/tauri-util';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CommandCommonService } from '../../../../entities/commande/command-common.service';
import { ImportationNewCommandeComponent } from '../../../../entities/commande/importation-new-commande.component';
import { CommandeImportResponseDialogComponent } from '../../../../entities/commande/commande-import-response-dialog.component';
import { showCommonModal } from '../../../../entities/sales/selling-home/sale-helper';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-commande-en-cours',
  templateUrl: './commande-en-cours.component.html',
  styleUrl: './commande-en-cours.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    RouterModule,
    TooltipModule,
    TagModule,
    ToolbarModule,
    InputTextModule,
    IconField,
    InputIcon,
    SpinnerComponent,
  ],
})
export class CommandeEnCoursComponent implements OnInit {
  protected search = '';
  protected searchCommande = '';
  protected selectionLength = 0;
  readonly itemsPerPage = ITEMS_PER_PAGE;
  readonly rowExpandMode: ExpandMode;
  protected commandes: ICommande[] = [];
  protected commandeSelected?: ICommande;
  protected totalItems = 0;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected index = 0;
  protected loading!: boolean;
  protected page = 0;
  protected selectedtypeSuggession = 'ALL';
  protected selections: ICommande[];
  protected readonly REQUESTED = OrderStatut.REQUESTED;
  private readonly destroyRef = inject(DestroyRef);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly selectedFilters = ['REQUESTED'];
  private readonly commandeService = inject(CommandeService);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly tauriPrinter = inject(TauriPrinterService);
  private readonly modalService = inject(NgbModal);
  private readonly commandCommonService = inject(CommandCommonService);

  constructor() {
    this.rowExpandMode = 'single';
  }

  ngOnInit(): void {
    this.onSearch();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page;
    this.loading = true;
    this.commandeService
      .query({
        page: pageToLoad,
        size: this.itemsPerPage,
        search: this.search,
        searchCommande: this.searchCommande,
        orderStatuts: this.selectedFilters,
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
      })
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  deleteCommande(commandeId: CommandeId): void {
    this.spinner().show();
    this.commandeService
      .delete(commandeId)
      .pipe(
        finalize(() => this.spinner().hide()),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => this.loadPage(),
        error: err => this.onCommonError(err),
      });
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  onRowExpand(event: any): void {
    if (!event.data.orderLines) {
      this.commandeService
        .fetchOrderLinesByCommandeId(event.data.commandeId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: res => {
            event.data.orderLines = res.body;
          },
          error: err => this.onCommonError(err),
        });
    }
  }

  exportCSV(commande: ICommande): void {
    this.commandeService
      .exportToCsv(commande.commandeId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          if (this.tauriPrinter.isRunningInTauri()) {
            handleBlobForTauri(blob, 'commande_en_cours', 'csv');
          } else {
            saveAs(blob);
          }
        },
        error: err => this.onCommonError(err),
      });
  }

  exportPdf(commande: ICommande): void {
    this.commandeService
      .exportToPdf(commande.commandeId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          if (this.tauriPrinter.isRunningInTauri()) {
            handleBlobForTauri(blob, 'commande_en_cours');
          } else {
            window.open(URL.createObjectURL(blob));
          }
        },
        error: err => this.onCommonError(err),
      });
  }

  orderLineTableColor(orderLine: IOrderLine): string {
    return '';
  }

  fusionner(): void {
    const ids = this.selections.map(e => {
      return { id: e.id, orderDate: e.orderDate };
    });
    const fournisseursIdArray = this.selections.map(e => e.fournisseur.id);
    const firstId = fournisseursIdArray[0];
    const isSameProviderFn = (currentValue: number) => currentValue === firstId;
    const isSameProvider = fournisseursIdArray.every(isSameProviderFn);
    if (!isSameProvider) {
      this.notificationService.warning('Veillez sélectionner des commandes du même grossiste', 'Information');
    } else {
      this.spinner().show();
      this.commandeService
        .fusionner(ids)
        .pipe(
          finalize(() => this.spinner().hide()),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: () => {
            this.selections = [];
            this.loadPage();
          },
          error: err => this.onCommonError(err),
        });
    }
  }

  onShowFileDialog(commande: ICommande): void {
    this.commandeSelected = commande;
  }

  removeAll(): void {
    this.commandeService
      .deleteSelectedCommandes(this.selections.map(e => ({ id: e.id, orderDate: e.orderDate })))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loadPage();
          this.selections = [];
        },
        error: err => this.onCommonError(err),
      });
  }

  confirmDelete(commande: ICommande): void {
    this.confirmDialog.onConfirm(
      () => this.deleteCommande(commande.commandeId),
      'Suppression',
      'Êtes-vous sûre de vouloir supprimer ?',
    );
  }

  onSearch(): void {
    if (this.index == 0) {
      this.loadPage(0);
    }
  }

  onCreatNewCommande(): void {
    this.commandCommonService.updateCommand(null);
    this.router.navigate(['/commande/new']);
  }

  onShowNewCommandeDialog(): void {
    showCommonModal(
      this.modalService,
      ImportationNewCommandeComponent,
      { header: 'IMPORTATION DE NOUVELLE COMMANDE' },
      (reason: ICommandeResponse) => {
        this.loadPage(0);
        if (reason) {
          this.openImportResponseDialog(reason);
        }
      },
      'lg',
    );
  }

  confirmDeleteAll(): void {
    this.confirmDialog.onConfirm(
      () => this.removeAll(),
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer ?',
    );
  }

  private openImportResponseDialog(responseCommande: ICommandeResponse): void {
    const modalRef = this.modalService.open(CommandeImportResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
  }

  lazyLoading(event: TableLazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.commandeService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search,
          searchCommande: this.searchCommande,
          orderStatuts: this.selectedFilters,
          typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
        })
        .pipe(
          finalize(() => (this.loading = false)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe({
          next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
        });
    }
  }

  protected selectAllClik(): void {
    this.selectionLength = this.selections.length;
  }

  protected onRowSelected(): void {
    this.selectionLength = this.selections.length;
  }

  protected onRowUnselect(): void {
    this.selectionLength = this.selections.length;
  }

  protected onSuccess(data: ICommande[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/commande'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search,
        orderStatuts: this.selectedFilters,
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
      },
    });
    this.commandes = data || [];
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private onCommonError(error: any): void {
    this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
  }
}
