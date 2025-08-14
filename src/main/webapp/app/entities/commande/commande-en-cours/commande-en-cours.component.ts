import { Component, inject, input, OnInit, output, viewChild } from '@angular/core';
import { ICommande } from '../../../shared/model/commande.model';
import { IOrderLine } from '../../../shared/model/order-line.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { LazyLoadEvent } from 'primeng/api';
import { CommandeService } from '../commande.service';
import { Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { OrderStatut } from '../../../shared/model/enumerations/order-statut.model';
import { SpinerService } from '../../../shared/spiner.service';
import { finalize } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../../shared/dialog/confirm-dialog/confirm-dialog.component';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-commande-en-cours',
  templateUrl: './commande-en-cours.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TableModule,
    RouterModule,
    TooltipModule,
    ConfirmDialogComponent
  ]
})
export class CommandeEnCoursComponent implements OnInit {
  readonly search = input('');
  readonly searchCommande = input('');
  readonly selectionLength = output<number>();
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
  readonly itemsPerPage = ITEMS_PER_PAGE;
  readonly rowExpandMode: ExpandMode;
  private errorService = inject(ErrorService);
  private spinner = inject(SpinerService);
  private readonly selectedFilters = ['REQUESTED'];
  private readonly commandeService = inject(CommandeService);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);

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
        search: this.search(),
        searchCommande: this.searchCommande(),
        orderStatuts: this.selectedFilters,
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError()
      });
  }

  deleteCommande(commandeId: number): void {
    this.spinner.show();
    this.commandeService.delete(commandeId).pipe(finalize(() => this.spinner.hide())).subscribe({
      next: () => {
        this.loadPage();
      },
      error: error => {
        this.onCommonError(error);
      }
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
      this.commandeService.fetchOrderLinesByCommandeId(event.data.id).subscribe(res => {
        event.data.orderLines = res.body;
      });
    }
  }

  exportCSV(commande: ICommande): void {
    this.commandeService.exportToCsv(commande.id).subscribe(blod => saveAs(blod));
  }

  exportPdf(commande: ICommande): void {
    this.commandeService.exportToPdf(commande.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }

  orderLineTableColor(orderLine: IOrderLine): string {
    /* if (orderLine) {
       if (orderLine.costAmount !== orderLine.orderCostAmount) {
         return 'table-danger';
       } else if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) {
         return 'table-danger';
       }
     }*/
    return '';
  }

  fusionner(): void {
    const ids = this.selections.map(e => e.id);
    const fournisseursIdArray = this.selections.map(e => e.fournisseur.id);
    const firstId = fournisseursIdArray[0];
    const isSameProviderFn = (currentValue: number) => currentValue === firstId;
    const isSameProvider = fournisseursIdArray.every(isSameProviderFn);
    if (!isSameProvider) {
      this.openInfoDialog('Veillez sélectionner des commandes du même grossiste', 'alert alert-info');
    } else {
      this.spinner.show();
      this.commandeService.fusionner(ids).subscribe({
        next: () => {
          this.selections = [];
          this.loadPage();
          this.spinner.hide();
        },
        error: error => {
          this.onCommonError(error);
          this.spinner.hide();
        }
      });
    }
  }


  onShowFileDialog(commande: ICommande): void {
    this.commandeSelected = commande;
  }


  removeAll(): void {
    this.commandeService.deleteSelectedCommandes(this.selections.map(e => e.id)).subscribe(() => {
      this.loadPage();
      this.selections = [];
    });
  }

  confirmDelete(commande: ICommande): void {
    this.confimDialog().onConfirm(() => this.deleteCommande(commande.id), 'Suppression', 'Êtes-vous sûre de vouloir supprimer ?');
  }


  onSearch(): void {
    if (this.index == 0) {
      this.loadPage(0);
    }
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first / event.rows;
      this.loading = true;
      this.commandeService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search(),
          searchCommande: this.searchCommande(),
          orderStatuts: this.selectedFilters,
          typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined
        })
        .subscribe({
          next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError()
        });
    }
  }

  protected selectAllClik(): void {
    this.selectionLength.emit(this.selections.length);
  }

  protected onRowSelected(): void {
    this.selectionLength.emit(this.selections.length);
  }

  protected onRowUnselect(): void {
    this.selectionLength.emit(this.selections.length);
  }

  private onCommonError(error: any): void {
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'alert alert-danger');
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  protected onSuccess(data: ICommande[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.router.navigate(['/commande'], {
      queryParams: {
        page: this.page,
        size: this.itemsPerPage,
        search: this.search(),
        orderStatuts: this.selectedFilters,
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined
      }
    });

    this.commandes = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
