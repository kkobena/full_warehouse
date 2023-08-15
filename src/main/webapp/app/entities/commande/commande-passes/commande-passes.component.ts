import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ICommande } from '../../../shared/model/commande.model';
import { ITEMS_PER_PAGE } from '../../../shared/constants/pagination.constants';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CommandeService } from '../commande.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../../shared/error.service';
import { DeliveryService } from '../delevery/delivery.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { ConfirmationService, LazyLoadEvent } from 'primeng/api';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { saveAs } from 'file-saver';
import { IOrderLine } from '../../../shared/model/order-line.model';
import { ICommandeResponse } from '../../../shared/model/commande-response.model';
import { CommandeImportResponseDialogComponent } from '../commande-import-response-dialog.component';
import { IResponseCommande } from '../../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from '../commande-en-cours-response-dialog.component';
import { ImportationNewCommandeComponent } from '../importation-new-commande.component';
import { IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryModalComponent } from '../delevery/form/delivery-modal.component';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';

@Component({
  selector: 'jhi-commande-passes',
  templateUrl: './commande-passes.component.html',
})
export class CommandePassesComponent implements OnInit {
  @Input() search = '';
  @Input() searchCommande = '';
  @Output() selectionLength: EventEmitter<number> = new EventEmitter<number>();
  protected commandes: ICommande[] = [];
  protected commandeSelected?: ICommande;
  protected totalItems = 0;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected index = 0;
  protected selectedFilter = 'PASSED';
  protected rowExpandMode = 'single';
  protected loading!: boolean;
  protected page = 0;
  protected selectedtypeSuggession = 'ALL';
  protected selections: ICommande[];
  protected fileDialog = false;
  protected ref!: DynamicDialogRef;

  constructor(
    protected commandeService: CommandeService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    protected deliveryService: DeliveryService,
    private spinner: NgxSpinnerService,
    private confirmationService: ConfirmationService,
    private dialogService: DialogService
  ) {}

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
        orderStatut: this.selectedFilter,
        typeSuggession: this.selectedtypeSuggession !== 'ALL' ? this.selectedtypeSuggession : undefined,
      })
      .subscribe({
        next: (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => this.onError(),
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
    this.commandeService.exportToCsv(commande.id!).subscribe(blod => saveAs(blod));
  }

  exportPdf(commande: ICommande): void {
    this.commandeService.exportToPdf(commande.id!).subscribe(blod => saveAs(blod));
  }

  orderLineTableColor(orderLine: IOrderLine): string {
    if (orderLine) {
      if (orderLine.costAmount !== orderLine.orderCostAmount) {
        return 'table-danger';
      } else if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) {
        return 'table-warning';
      }
    }
    return '';
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
    this.commandeService.importerReponseCommande(this.commandeSelected?.id!, formData).subscribe({
      next: res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');

        this.commandeService.fetchOrderLinesByCommandeId(this.commandeSelected?.id!).subscribe(ress => {
          this.commandeSelected!.orderLines = ress.body!;
        });

        this.openImporterReponseCommandeDialog(res.body!);
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

  onShowNewCommandeDialog(): void {
    this.ref = this.dialogService.open(ImportationNewCommandeComponent, {
      header: 'IMPORTATION DE NOUVELLE COMMANDE',
      width: '40%',
    });
    this.ref.onClose.subscribe((resp: ICommandeResponse) => {
      if (resp) {
        if (resp.items?.length === 0) {
          this.selectedFilter = 'PASSED';
        } else {
          this.selectedFilter = 'REQUESTED';
        }
        this.loadPage(0);
        this.openImportResponseDialogComponent(resp);
      }
    });
  }

  rollbackAll(): void {
    this.commandeService.rollbackCommandes(this.selections.map(e => e.id!)).subscribe(() => {
      this.loadPage();
      this.selections = [];
    });
  }

  confirmRollback(commande: ICommande): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous la retourner dans commande en cours ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.rollbackCommande(commande?.id!);
      },
      key: 'deleteCommande',
    });
  }

  rollbackCommande(commandeId: number): void {
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.rollback(commandeId).subscribe({
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

  gotoEntreeStockComponent(delivery: IDelivery): void {
    this.router.navigate(['/commande', delivery.id, 'stock-entry']);
  }

  onCreateBon(commande: ICommande): void {
    this.deliveryService.findByOrderReference(commande.orderRefernce).subscribe({
      next: (res: HttpResponse<IDelivery>) => {
        this.gotoEntreeStockComponent(res.body);
      },
      error: () => {
        this.ref = this.dialogService.open(DeliveryModalComponent, {
          data: { entity: null, commande },
          header: 'CREATION DU BON DE LIVRAISON',
          width: '40%',
        });
        this.ref.onClose.subscribe((delivery: IDelivery) => {
          if (delivery) {
            this.gotoEntreeStockComponent(delivery);
          }
        });
      },
    });
  }

  onSearch(): void {
    if (this.index == 0) {
      this.loadPage(0);
    }
  }

  lazyLoading(event: LazyLoadEvent): void {
    if (event) {
      this.page = event.first! / event.rows!;
      this.loading = true;
      this.commandeService
        .query({
          page: this.page,
          size: event.rows,
          search: this.search,
          searchCommande: this.searchCommande,
          orderStatut: this.selectedFilter,
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
        search: this.search,
        orderStatut: this.selectedFilter,
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
