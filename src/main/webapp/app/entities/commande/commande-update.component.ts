import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { Commande, ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { OrderLineService } from '../order-line/order-line.service';
import { ProduitService } from '../produit/produit.service';
import { IProduit } from 'app/shared/model/produit.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { IOrderLine, OrderLine } from '../../shared/model/order-line.model';
import { ConfirmationService, MenuItem } from 'primeng/api';
import { ErrorService } from '../../shared/error.service';
import { DialogService } from 'primeng/dynamicdialog';
import { saveAs } from 'file-saver';
import { NgxSpinnerService } from 'ngx-spinner';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../shared/constants/pagination.constants';

@Component({
  selector: 'jhi-commande-update',
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

      .commande-toolbar {
        margin-bottom: 2px !important;
      }

      .p-toolbar-group-left .col-md-4,
      .p-toolbar-group-left .col-sm-2,
      .p-toolbar-group-left .col-md-6 {
        padding-right: 0;
        padding-left: 3px;
      }

      .p-toolbar-group-left .row {
        margin-left: 0.1rem;
      }
    `,
  ],
  templateUrl: './commande-update.component.html',
  providers: [ConfirmationService, DialogService],
})
export class CommandeUpdateComponent implements OnInit {
  isSaving = false;
  produits: IProduit[] = [];
  commande?: ICommande | null = null;
  orderLines: IOrderLine[] = [];
  searchValue?: string;
  search?: string;
  produitSelected?: IProduit | null = null;
  selectedProvider?: number | null = null;
  fournisseurs: IFournisseur[] = [];
  quantiteSaisie = 1;

  @ViewChild('quantyBox', { static: false })
  quantyBox?: ElementRef;
  @ViewChild('produitbox', { static: true })
  produitbox?: any;
  @ViewChild('fournisseurBox')
  fournisseurBox?: any;
  filtres: any[] = [];
  selectedFilter = 'ALL';
  fournisseurDisabled = false;
  selectedEl: IOrderLine[];
  commandebuttons: MenuItem[];
  fileDialog = false;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly APPEND_TO = APPEND_TO;

  constructor(
    protected commandeService: CommandeService,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    protected fournisseurService: FournisseurService,
    private confirmationService: ConfirmationService,
    private errorService: ErrorService,
    private spinner: NgxSpinnerService
  ) {
    this.selectedEl = [];
    this.filtres = [
      { label: "Prix d'achat differents", value: 'NOT_EQUAL' },
      { label: 'Code cip  à mettre à jour', value: 'PROVISOL_CIP' },
      { label: 'Tous', value: 'ALL' },
    ];
    this.commandebuttons = [
      {
        label: 'IMPORTER REPONSE',
        icon: 'pi pi-upload',
        command: () => (this.fileDialog = true),
      },
      {
        label: 'EXTRANET',
        icon: 'pi pi-wifi',
      },
      {
        label: 'PHARMA-ML',
        icon: 'pi pi-desktop',
      },
    ];
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ commande }) => {
      if (commande.id) {
        this.commande = commande;
        this.orderLines = commande.orderLines;
        this.fournisseurDisabled = true;
        this.selectedProvider = this.commande.fournisseurId;
      }
    });
    this.loadFournisseurs();
  }

  filtreFournisseur(event: any): void {
    this.loadFournisseurs(event.target.value);
  }

  previousState(): void {
    window.history.back();
  }

  onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) return;
    this.onAddOrderLine(qytMvt);
  }

  onQuantity(): void {
    const qytMvt = Number(this.quantyBox?.nativeElement.value);
    if (qytMvt <= 0) return;
    this.onAddOrderLine(qytMvt);
  }

  onAddOrderLine(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.commande && this.commande.id !== undefined) {
        this.subscribeToSaveOrderLineResponse(
          this.commandeService.createOrUpdateOrderLine(this.createOrderLine(this.produitSelected, qytMvt))
        );
      } else {
        this.subscribeToSaveOrderLineResponse(this.commandeService.create(this.createCommande(this.produitSelected, qytMvt)));
      }
    }
  }

  onUpdateQuantityRequested(orderLine: IOrderLine, event: any): void {
    const newQuantityRequested = Number(event.target.value);
    if (this.commande && newQuantityRequested > 0) {
      orderLine.quantityRequested = newQuantityRequested;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateQuantityRequested(orderLine));
    }
  }

  onUpdateOrderCostAmount(orderLine: IOrderLine, event: any): void {
    const newOrderCostAmount = Number(event.target.value);
    if (this.commande && newOrderCostAmount > 0) {
      orderLine.orderCostAmount = newOrderCostAmount;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  onUpdateCip(orderLine: IOrderLine, event: any): void {
    const cip = event.target.value;
    if (this.commande && cip !== '') {
      orderLine.produitCip = cip;
      this.commandeService.updateCip(orderLine).subscribe(() => {
        this.refreshCommande();
      });
    }
  }

  onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const newOrderUnitPrice = Number(event.target.value);
    if (this.commande && newOrderUnitPrice > 0) {
      orderLine.orderUnitPrice = newOrderUnitPrice;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderUnitPrice(orderLine));
    }
  }

  onDeleteOrderLineById(orderLine: IOrderLine): void {
    if (this.commande) {
      this.commandeService.deleteOrderLineById(orderLine.id!).subscribe(() => {
        this.refreshCommande();
      });
    }
  }

  save(): void {
    if (!this.orderLines.some(this.checkQuantityNotSet)) {
      this.isSaving = true;
    } else {
      this.openDialog('Veuillez saisir les quantités de tous les produits séléctionnés');
    }
  }

  deleteSelectedOrderLine(): void {
    const ids = this.selectedEl.map(e => e.id!);
    this.commandeService.deleteOrderLinesByIds(this.commande?.id!, ids).subscribe(() => {
      this.refreshCommande();
      this.selectedEl = [];
      this.focusPrdoduitBox();
    });
  }

  exportPdf(): void {
    this.commandeService.exportToPdf(this.commande?.id!).subscribe(blod => saveAs(blod));
  }

  onCloseCurrentCommande(): void {
    this.spinner.show('commandeEnCourspinner');
    this.commandeService.closeCommandeEnCours(this.commande?.id!).subscribe({
      next: () => {
        this.spinner.hide('commandeEnCourspinner');
        this.confirmStay();
      },
      error: error => {
        this.onCommonError(error);
        this.spinner.hide('commandeEnCourspinner');
      },
    });
  }

  onMettreAttente(): void {
    this.confirmStay();
  }

  exportCSV(): void {
    this.commandeService.exportToCsv(this.commande?.id!).subscribe(blod => saveAs(blod));
  }

  searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  produitComponentSearch(term: string, item: IProduit): boolean {
    return !!item;
  }

  onSelect(): void {
    setTimeout(() => {
      const el = this.quantyBox?.nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  onProviderSelect(): void {
    this.focusPrdoduitBox();
  }

  focusPrdoduitBox(): void {
    setTimeout(() => {
      this.produitbox.inputEL.nativeElement.focus();
      this.produitbox.inputEL.nativeElement.select();
    }, 50);
  }

  loadProduits(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 5,
        withdetail: false,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  onFilterCommandeLines(): void {
    const query = {
      commandeId: this.commande?.id,
      search: this.search,
      filterCommaneEnCours: this.selectedFilter,
    };
    this.commandeService.filterCommandeLines(query).subscribe(res => {
      this.orderLines = res.body!;
    });
  }

  checkQuantityNotSet(orderLine: IOrderLine): boolean {
    return orderLine.quantityRequested === 0;
  }

  openDialog(message: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static' });
    modalRef.componentInstance.message = message;
  }

  loadFournisseurs(search?: String): void {
    const query: String = search || '';
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
        search: query,
      })
      .subscribe({
        next: (res: HttpResponse<IFournisseur[]>) => (this.fournisseurs = res.body!),
        error: () => this.onError(),
      });
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

  confirmDeleteItem(item: IOrderLine): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous detacher  ce produit ?',
      header: 'RUPPRESSION DE PRODUIT ',
      icon: 'pi pi-info-circle',
      accept: () => this.onDeleteOrderLineById(item),
      reject: () => {
        this.updateProduitQtyBox();
      },
      key: 'deleteItem',
    });
  }

  confirmStay(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous rester sur la page ?',
      header: ' INFORMATION',
      icon: 'pi pi-info-circle',
      accept: () => this.resetAll(),
      reject: () => {
        this.previousState();
      },
      key: 'stayThere',
    });
  }

  cancel(): void {
    this.fileDialog = false;
  }

  onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];

    formData.append('commande', file, file.name);

    this.showsPinner('commandeEnCourspinner');
    this.commandeService.importerReponseCommande(this.commande?.id!, formData).subscribe({
      next: res => {
        this.hidePinner('commandeEnCourspinner');
        this.refreshCommande();
        this.cancel();
        this.openImporterReponseCommandeDialog(res.body!);
      },
      error: error => {
        this.hidePinner('commandeEnCourspinner');

        this.onCommonError(error);
      },
    });
  }

  openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeEnCoursResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.commande;
  }

  resetAll(): void {
    this.commande = null;
    this.orderLines = [];
    this.selectedProvider = null;
    this.produitSelected = null;
    this.fournisseurDisabled = false;
    this.selectedFilter = 'ALL';
    setTimeout(() => {
      //  this.fournisseurBox.focus();
      this.fournisseurBox.inputEL.nativeElement.focus();
    }, 100);
  }

  protected refreshCommande(): void {
    this.commandeService.find(this.commande?.id!).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande?.orderLines!;
      this.focusPrdoduitBox();
    });
  }

  protected updateProduitQtyBox(): void {
    if (this.quantyBox) {
      this.quantyBox.nativeElement.value = 1;
    }
    this.produitSelected = null;
    this.focusPrdoduitBox();
    if (this.commande && this.commande.id) {
      this.fournisseurDisabled = true;
    }
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
  }

  protected onSaveOrderLineSuccess(commande: ICommande): void {
    if (commande) {
      this.commandeService.find(commande.id!).subscribe(res => {
        this.commande = res.body;
        this.orderLines = this.commande?.orderLines!;
        this.updateProduitQtyBox();
      });
    }
  }

  protected onSaveError(): void {
    this.isSaving = false;
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

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  protected onError(): void {}

  protected subscribeToSaveOrderLineResponse(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: (res: HttpResponse<ICommande>) => this.onSaveOrderLineSuccess(res.body!),
      error: (err: any) => this.onCommonError(err),
    });
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  private createOrderLine(produit: IProduit, quantityRequested: number): IOrderLine {
    return {
      ...new OrderLine(),
      produitId: produit.id,
      totalQuantity: produit.totalQuantity,
      commande:
        this.commande?.id !== undefined
          ? this.commande
          : {
              ...new Commande(),
              fournisseurId: this.commande.fournisseurId,
            },
      quantityRequested,
    };
  }

  private createCommande(produit: IProduit, quantityRequested: number): ICommande {
    return {
      ...new Commande(),
      fournisseurId: this.selectedProvider,
      orderLines: [this.createOrderLine(produit, quantityRequested)],
    };
  }

  private showsPinner(spinnerName: string): void {
    this.spinner.show(spinnerName);
  }

  private hidePinner(spinnerName: string): void {
    this.spinner.hide(spinnerName);
  }
}
