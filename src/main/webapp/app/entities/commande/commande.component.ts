import { Component, OnDestroy, OnInit } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ICommande } from 'app/shared/model/commande.model';
import { DETAIL_PER_PAGE, ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { CommandeService } from './commande.service';
import { IOrderLine } from 'app/shared/model/order-line.model';
import { OrderLineService } from '../order-line/order-line.service';
import { ProduitService } from '../produit/produit.service';
import { ConfirmationService, LazyLoadEvent, MenuItem } from 'primeng/api';
import { ISales } from '../../shared/model/sales.model';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { ErrorService } from '../../shared/error.service';
import { NgxSpinnerService } from 'ngx-spinner';
import { saveAs } from 'file-saver';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { CommandeImportResponseDialogComponent } from './commande-import-response-dialog.component';
import { ICommandeResponse } from '../../shared/model/commande-response.model';
import { FormProduitFournisseurComponent } from '../produit/form-produit-fournisseur/form-produit-fournisseur.component';
import { IFournisseurProduit } from '../../shared/model/fournisseur-produit.model';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ImportationNewCommandeComponent } from './importation-new-commande.component';

@Component({
  selector: 'jhi-commande',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
      }

      .active {
        background-color: #95caf9 !important;
      }

      .master {
        padding: 14px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }

      .ag-theme-alpine {
        max-height: 700px;
        height: 600px;
        min-height: 500px;
      }
    `,
  ],
  templateUrl: './commande.component.html',
  providers: [ConfirmationService, DialogService],
})
export class CommandeComponent implements OnInit, OnDestroy {
  commandes: ICommande[] = [];
  commandeSelected?: ICommande;
  selectedRowIndex?: number;
  selectedRowOrderLines?: IOrderLine[] = [];
  eventSubscriber?: Subscription;
  totalItems = 0;
  totalDetail = 0;
  detaimPerPage = DETAIL_PER_PAGE;
  itemsPerPage = ITEMS_PER_PAGE;
  search = '';
  pageItem!: number;
  predicate!: string;
  ascending!: boolean;
  ngbPaginationPage = 1;
  tooltipPosition = 'left';
  index = 0;
  filtres: any[] = [];
  selectedFilter = 'REQUESTED';
  commandebuttons: MenuItem[];
  rowExpandMode = 'single';
  loading!: boolean;
  page = 0;
  selectedtypeSuggession = 'ALL';
  typeSuggessions: any[] = [];
  selections: ICommande[];
  searchCommande = '';
  fileDialog = false;
  modelSelected!: string;
  models: any[];
  commandeFournisseur?: IFournisseur;
  ref!: DynamicDialogRef;

  constructor(
    protected commandeService: CommandeService,
    protected activatedRoute: ActivatedRoute,
    protected router: Router,
    protected eventManager: JhiEventManager,
    protected modalService: NgbModal,
    private errorService: ErrorService,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService,
    private spinner: NgxSpinnerService,
    private confirmationService: ConfirmationService,
    private dialogService: DialogService
  ) {
    this.selections = [];
    this.models = [
      { label: 'LABOREX', value: 'LABOREX' },
      { label: 'COPHARMED', value: 'COPHARMED' },
      { label: 'DPCI', value: 'DPCI' },
      { label: 'TEDIS', value: 'TEDIS' },
    ];
    this.filtres = [
      { label: 'Commande en cours', value: 'REQUESTED' },
      { label: 'Commande passées', value: 'PASSED' },
      { label: 'Commande reçues', value: 'RECEIVED' },
    ];
    this.typeSuggessions = [
      { label: 'Tous', value: 'ALL' },
      { label: 'Auto', value: 'AUTO' },
      { label: 'Manuelle', value: 'MANUELLE' },
    ];

    this.commandebuttons = [
      /*  {
        label: 'Excel',
        icon: 'pi pi-file-excel'

      },*/
      {
        label: 'Csv',
        icon: 'pi pi-folder-open',
      },
    ];
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
      .subscribe(
        (res: HttpResponse<ISales[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        () => this.onError()
      );
  }

  ngOnInit(): void {
    this.loadPage();
  }

  onSearch(): void {
    this.loadPage(0);
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
        .subscribe(
          (res: HttpResponse<ICommande[]>) => this.onSuccess(res.body, res.headers, this.page),
          () => this.onError()
        );
    }
  }

  ngOnDestroy(): void {
    if (this.eventSubscriber) {
      this.eventManager.destroy(this.eventSubscriber);
    }
  }

  registerChangeInCommandes(): void {
    this.eventSubscriber = this.eventManager.subscribe('commandeListModification', () => this.loadPage());
  }

  delete(commandeId: number): void {
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.delete(commandeId).subscribe(
      () => {
        this.loadPage();
        this.spinner.hide('gestion-commande-spinner');
      },
      error => {
        this.onCommonError(error);
        this.spinner.hide('gestion-commande-spinner');
      }
    );
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'updatedAt') {
      result.push('updatedAt');
    }
    return result;
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
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

  fusionner(): void {
    const ids = this.selections.map(e => e.id!);
    const fournisseursIdArray = this.selections.map(e => e.fournisseur?.id!);
    const firstId = fournisseursIdArray[0];
    const isSameProviderFn = (currentValue: number) => currentValue === firstId;
    const isSameProvider = fournisseursIdArray.every(isSameProviderFn);
    if (!isSameProvider) {
      this.openInfoDialog('Veillez sélectionner des commandes du même grossiste', 'alert alert-info');
    } else {
      this.spinner.show('gestion-commande-spinner');
      this.commandeService.fusionner(ids).subscribe(
        () => {
          this.selections = [];
          this.loadPage();
          this.spinner.hide('gestion-commande-spinner');
        },
        error => {
          this.onCommonError(error);
          this.spinner.hide('gestion-commande-spinner');
        }
      );
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
    this.commandeService.importerReponseCommande(this.commandeSelected?.id!, formData).subscribe(
      res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');

        this.commandeService.fetchOrderLinesByCommandeId(this.commandeSelected?.id!).subscribe(ress => {
          this.commandeSelected!.orderLines = ress.body!;
        });

        this.openImporterReponseCommandeDialog(res.body!);
      },
      error => {
        this.spinner.hide('gestion-commande-spinner');
        this.onCommonError(error);
      }
    );
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
        this.loadPage(0); //affichier la tab correspondante
        this.openImportResponseDialogComponent(resp);
      }
    });
  }

  confirmDeleteSelectedRows(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer ces commandes  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => {
        this.commandeService.deleteSelectedCommandes(this.selections.map(e => e.id!)).subscribe(() => {
          this.loadPage();
          this.selections = [];
        });
      },
      key: 'deleteCommande',
    });
  }

  confirmDelete(commande: ICommande): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette commande  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(commande?.id!),
      key: 'deleteCommande',
    });
  }

  test(): void {
    this.commandeService.test({ model: 'TEDIS' }).subscribe(() => {
      console.error('===============================');
    });
  }

  onNewCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];

    formData.append('commande', file, file.name);
    this.spinner.show('gestion-commande-spinner');
    this.commandeService.uploadNewCommande(this.commandeFournisseur?.id!, this.modelSelected, formData).subscribe(
      res => {
        this.cancel();
        this.spinner.hide('gestion-commande-spinner');
        if (res.body && res.body.items?.length === 0) {
          this.selectedFilter = 'PASSED';
        } else {
          this.selectedFilter = 'REQUESTED';
        }
        this.loadPage(0); //affichier la tab correspondante
        this.openImportResponseDialogComponent(res.body!);
      },
      error => {
        this.spinner.hide('gestion-commande-spinner');
        this.onCommonError(error);
      }
    );
  }
  onClickLink(commande: ICommande): void {
    this.commandeService.getRuptureCsv(commande.orderRefernce!).subscribe(
      blod => saveAs(blod),
      error => this.onCommonError(error)
    );
  }

  protected onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe(
        translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
        },
        () => this.openInfoDialog(error.error.title, 'alert alert-danger')
      );
    }
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
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

  protected onOrderLineError(): void {}
}
