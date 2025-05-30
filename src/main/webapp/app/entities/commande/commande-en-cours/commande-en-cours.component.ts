import { Component, inject, input, OnInit, output } from '@angular/core';
import { ICommande } from '../../../shared/model/commande.model';
import { IOrderLine } from '../../../shared/model/order-line.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CommandeService } from '../commande.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { DeliveryService } from '../delevery/delivery.service';
import { ProduitService } from '../../produit/produit.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { CommandeImportResponseDialogComponent } from '../commande-import-response-dialog.component';
import { IResponseCommande } from '../../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from '../commande-en-cours-response-dialog.component';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryModalComponent } from '../delevery/form/delivery-modal.component';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { acceptButtonProps, rejectButtonProps } from '../../../shared/util/modal-button-props';
import { OrderStatut } from '../../../shared/model/enumerations/order-statut.model';

export type ExpandMode = 'single' | 'multiple';

@Component({
  selector: 'jhi-commande-en-cours',
  templateUrl: './commande-en-cours.component.html',
  imports: [
    WarehouseCommonModule,
    ButtonModule,
    TableModule,
    NgxSpinnerModule,
    RouterModule,
    RippleModule,
    DynamicDialogModule,
    TooltipModule,
  ],
})
export class CommandeEnCoursComponent implements OnInit {
  readonly search = input('');
  readonly searchCommande = input('');
  readonly selectionLength = output<number>();

  protected commandes: ICommande[] = [];
  protected commandeSelected?: ICommande;
  protected totalItems = 0;
  private readonly itemsPerPage = ITEMS_PER_PAGE;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected index = 0;
  private readonly rowExpandMode: ExpandMode;
  protected loading!: boolean;
  protected page = 0;
  protected selectedtypeSuggession = 'ALL';
  protected selections: ICommande[];
  protected fileDialog = false;
  protected ref!: DynamicDialogRef;
  protected readonly REQUESTED = OrderStatut.REQUESTED;
  private errorService = inject(ErrorService);
  private spinner = inject(NgxSpinnerService);
  private confirmationService = inject(ConfirmationService);
  private dialogService = inject(DialogService);
  private readonly selectedFilters = ['REQUESTED'];
  private readonly commandeService = inject(CommandeService);

  private readonly router = inject(Router);
  private readonly modalService = inject(NgbModal);
  private readonly deliveryService = inject(DeliveryService);

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
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
      });
  }

  deleteCommande(commandeId: number): void {
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.delete(commandeId).subscribe({
      next: () => {
        this.loadPage();
        this.spinner.hide('gestion-commande-spinner');
      },
      error: error => {
        this.onCommonError(error);
        this.spinner.hide('gestion-commande-spinner');
      },
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
      this.spinner.show('gestion-commande-spinner');
      this.commandeService.fusionner(ids).subscribe({
        next: () => {
          this.selections = [];
          this.loadPage();
          this.spinner.hide('gestion-commande-spinner');
        },
        error: error => {
          this.onCommonError(error);
          this.spinner.hide('gestion-commande-spinner');
        },
      });
    }
  }

  cancel(): void {
    this.fileDialog = false;
  }

  onShowFileDialog(commande: ICommande): void {
    this.fileDialog = true;
    this.commandeSelected = commande;
  }

  onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('commande', file, file.name);
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.importerReponseCommande(this.commandeSelected.id, formData).subscribe({
      next: res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');
        this.commandeService.fetchOrderLinesByCommandeId(this.commandeSelected.id).subscribe(ress => {
          this.commandeSelected.orderLines = ress.body!;
        });
        this.openImporterReponseCommandeDialog(res.body);
      },
      error: error => {
        this.spinner.hide('gestion-commande-spinner');
        this.onCommonError(error);
      },
    });
  }

  openImportResponseDialogComponent(responseCommande: ICommandeResponse): void {
    const modalRef = this.modalService.open(CommandeImportResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
  }

  openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeEnCoursResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.commandeSelected;
  }

  removeAll(): void {
    this.commandeService.deleteSelectedCommandes(this.selections.map(e => e.id)).subscribe(() => {
      this.loadPage();
      this.selections = [];
    });
  }

  confirmDelete(commande: ICommande): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette commande  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.deleteCommande(commande.id),
      key: 'deleteCommande',
    });
  }

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/commande', delivery.id, 'stock-entry']);
  }

  onPasserEnCours(commande: ICommande): void {
    this.commandeService.closeCommandeEnCours(commande.id).subscribe({
      next: () => {
        this.loadPage();
      },
      error: error => {
        this.onCommonError(error);
      },
    });
  }

  onCreateBon(commande: ICommande): void {
    this.ref = this.dialogService.open(DeliveryModalComponent, {
      data: { commande },
      header: 'CREATION DU BON DE LIVRAISON',
      width: '40%',
    });
    this.ref.onClose.subscribe((delivery: IDelivery) => {
      if (delivery) {
        this.gotoEntreeStockComponent(delivery);
      }
    });
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
          typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
        })
        .subscribe({
          next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, this.page),
          error: () => this.onError(),
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

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        error: () => this.openInfoDialog(error.error.title, 'alert alert-danger'),
      });
    }
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
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
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
      },
    });

    this.commandes = data || [];
    this.loading = false;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }
}
