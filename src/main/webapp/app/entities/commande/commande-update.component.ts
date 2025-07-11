import {AfterViewInit, Component, ElementRef, inject, OnInit, viewChild} from '@angular/core';
import {HttpResponse} from '@angular/common/http';

import {ActivatedRoute, Router, RouterModule} from '@angular/router';
import {Observable} from 'rxjs';
import {Commande, ICommande} from 'app/shared/model/commande.model';
import {CommandeService} from './commande.service';
import {ProduitService} from '../produit/produit.service';
import {IProduit} from 'app/shared/model/produit.model';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {AlertInfoComponent} from 'app/shared/alert/alert-info.component';
import {IFournisseur} from '../../shared/model/fournisseur.model';
import {FournisseurService} from '../fournisseur/fournisseur.service';
import {IOrderLine, OrderLine} from '../../shared/model/order-line.model';
import {ConfirmationService, MenuItem, MessageService} from 'primeng/api';
import {ErrorService} from '../../shared/error.service';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {saveAs} from 'file-saver';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {IResponseCommande} from '../../shared/model/response-commande.model';
import {CommandeEnCoursResponseDialogComponent} from './commande-en-cours-response-dialog.component';
import {APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND} from '../../shared/constants/pagination.constants';
import {WarehouseCommonModule} from '../../shared/warehouse-common/warehouse-common.module';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {TableModule} from 'primeng/table';
import {SplitButtonModule} from 'primeng/splitbutton';
import {AutoCompleteModule} from 'primeng/autocomplete';
import {FileUploadModule} from 'primeng/fileupload';
import {DropdownModule} from 'primeng/dropdown';
import {DialogModule} from 'primeng/dialog';
import {ToolbarModule} from 'primeng/toolbar';
import {TooltipModule} from 'primeng/tooltip';
import {NgSelectModule} from '@ng-select/ng-select';
import {ToastModule} from 'primeng/toast';
import {CommandCommonService} from './command-common.service';
import {acceptButtonProps, rejectButtonProps} from '../../shared/util/modal-button-props';
import {Select} from 'primeng/select';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {SORT} from '../../shared/util/command-item-sort';
import {DeliveryModalComponent} from './delevery/form/delivery-modal.component';
import {DeliveryService} from './delevery/delivery.service';
import {OrderLineLotsComponent} from './lot/order-line-lots.component';
import {ButtonGroup} from 'primeng/buttongroup';
import {Panel} from 'primeng/panel';
import {ListLotComponent} from './lot/list/list-lot.component';
import {FormLotComponent} from './lot/form-lot.component';
import {OrderStatut} from '../../shared/model/enumerations/order-statut.model';
import {EtiquetteComponent} from './delevery/etiquette/etiquette.component';
import {FloatLabel} from 'primeng/floatlabel';
import {Params} from '../../shared/model/enumerations/params.model';
import {ConfigurationService} from '../../shared/configuration.service';
import {EditProduitComponent} from './delevery/form/edit-produit/edit-produit.component';

@Component({
  selector: 'jhi-commande-update',

  templateUrl: './commande-update.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    NgSelectModule,
    ButtonModule,
    RippleModule,
    NgxSpinnerModule,
    ConfirmDialogModule,
    InputTextModule,
    TagModule,
    TableModule,
    RouterModule,
    SplitButtonModule,
    AutoCompleteModule,
    FileUploadModule,
    DropdownModule,
    DialogModule,
    ToolbarModule,
    TooltipModule,
    ToastModule,
    Select,
    InputGroup,
    InputGroupAddon,
    IconField,
    InputIcon,
    ButtonGroup,
    Panel,
    FloatLabel,
  ],
})
export class CommandeUpdateComponent implements OnInit, AfterViewInit {
  protected readonly RECEIVED = OrderStatut.RECEIVED;
  protected isSaving = false;
  protected produits: IProduit[] = [];
  protected commande?: ICommande | null = null;
  protected orderLines: IOrderLine[] = [];
  protected searchValue?: string;
  protected search?: string;
  protected produitSelected?: IProduit | null = null;
  protected selectedProvider?: number | null = null;
  protected fournisseurs: IFournisseur[] = [];
  protected quantiteSaisie = 1;
  protected quantityBox = viewChild.required<ElementRef>('quantityBox');
  protected filtres: any[] = [];
  protected selectedFilter = 'ALL';
  protected tris = 'UPDATE';
  protected selectedEl: IOrderLine[];
  protected commandebuttons: MenuItem[];
  protected exportbuttons: MenuItem[];
  protected fileDialog = false;
  protected fournisseurBox = viewChild.required<any>('fournisseurBox');
  protected produitbox = viewChild.required<any>('produitbox');
  protected readonly sorts = SORT;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected ref!: DynamicDialogRef;
  protected readonly APPEND_TO = APPEND_TO;
  protected showLotBtn = false;
  private readonly commandeService = inject(CommandeService);
  private readonly produitService = inject(ProduitService);
  private readonly modalService = inject(NgbModal);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = inject(NgxSpinnerService);
  private readonly messageService = inject(MessageService);
  private readonly deliveryService = inject(DeliveryService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly dialogService = inject(DialogService);
  private readonly router = inject(Router);
  private readonly configurationService = inject(ConfigurationService);
  private readonly activatedRoute = inject(ActivatedRoute);

  constructor() {
    this.selectedEl = [];
    this.filtres = [
      {label: "Prix d'achat differents", value: 'NOT_EQUAL'},
      {label: 'Code cip  à mettre à jour', value: 'PROVISOL_CIP'},
      {label: 'Tous', value: 'ALL'},
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

    this.exportbuttons = [
      {
        label: 'PDF',
        icon: 'pi pi-file-pdf',
        command: () => this.exportPdf(),
      },
      {
        label: 'CSV',
        icon: 'pi pi-file-excel',
        command: () => this.exportCSV(),
      },
    ];
  }

  protected onShowLots(orderLine: IOrderLine): void {
    const modalRef = this.modalService.open(OrderLineLotsComponent, {
      backdrop: 'static',
      size: 'lg',
    });
    modalRef.componentInstance.lots = orderLine.lots;
    modalRef.componentInstance.produitLibelle = orderLine?.produitLibelle;
  }

  protected onEditProduit(produitId: number): void {
    this.commandCommonService.updateCommand(this.commande);
    this.router.navigate(['produit', produitId, 'edit']);
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({commande}) => {
      if (commande?.id) {
        this.commande = commande;
        this.orderLines = this.commande.orderLines;
        this.selectedProvider = this.commande.fournisseurId;
      } else if (this.commandCommonService.currentCommand()?.id) {
        this.commande = this.commandCommonService.currentCommand();
        this.orderLines = this.commande.orderLines;
        this.selectedProvider = this.commande.fournisseurId;
      }
      if (this.isClosedCommande()) {
        this.commande = null;
        this.router.navigate(['/commande']);
      }
    });
    this.loadFournisseurs();
    this.isLotActif();
  }

  protected filtreFournisseur(event: any): void {
    this.loadFournisseurs(event.target.value);
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onQuantityBoxAction(event: any): void {
    const qytMvt = Number(event.target.value);
    if (qytMvt <= 0) {
      return;
    }
    this.onAddOrderLine(qytMvt);
  }

  protected onQuantity(): void {
    const qytMvt = Number(this.quantityBox().nativeElement.value);
    if (qytMvt <= 0) {
      return;
    }
    this.onAddOrderLine(qytMvt);
  }

  protected onAddOrderLine(qytMvt: number): void {
    if (this.produitSelected) {
      if (this.selectedProvider) {
        if (this.commande?.id !== undefined) {
          this.subscribeToSaveOrderLineResponse(
            this.commandeService.createOrUpdateOrderLine(this.createOrderLine(this.produitSelected, qytMvt)),
          );
        } else {
          this.subscribeToSaveOrderLineResponse(this.commandeService.create(this.createCommande(this.produitSelected, qytMvt)));
        }
      } else {
        this.openInfoDialog('Veuillez selectionner un fournisseur', 'error', 'Erreur');
      }
    }
  }

  protected onUpdateQuantityRequested(orderLine: IOrderLine, event: any): void {
    const newQuantityRequested = Number(event.target.value);
    if (newQuantityRequested > 0) {
      orderLine.quantityRequested = newQuantityRequested;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateQuantityRequested(orderLine));
    }
  }

  protected onUpdateFreeQtyRequested(orderLine: IOrderLine, event: any): void {
    const newfreeQty = Number(event.target.value);
    if (newfreeQty >= 0) {
      orderLine.freeQty = newfreeQty;
      this.subscribeUpdateOrderLineResponse(this.commandeService.updateQuantityUG(orderLine));
    }
  }

  protected onUpdateOrderCostAmount(orderLine: IOrderLine, event: any): void {
    const newOrderCostAmount = Number(event.target.value);
    if (this.commande && newOrderCostAmount > 0) {
      orderLine.orderCostAmount = newOrderCostAmount;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  protected onUpdateCip(orderLine: IOrderLine, event: any): void {
    const cip = event.target.value;
    if (this.commande && cip !== '') {
      orderLine.produitCip = cip;
      this.commandeService.updateCip(orderLine).subscribe(() => {
        this.refreshCommande();
      });
    }
  }

  protected onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const newOrderUnitPrice = Number(event.target.value);
    if (this.commande && newOrderUnitPrice > 0) {
      orderLine.orderUnitPrice = newOrderUnitPrice;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderUnitPrice(orderLine));
    }
  }

  protected onDeleteOrderLineById(orderLine: IOrderLine): void {
    if (this.commande) {
      this.commandeService.deleteOrderLineById(orderLine.id).subscribe(() => {
        this.refreshCommande();
      });
    }
  }

  protected save(): void {
    if (!this.orderLines.some(this.checkQuantityNotSet)) {
      this.isSaving = true;
    } else {
      this.openDialog('Veuillez saisir les quantités de tous les produits séléctionnés');
    }
  }

  protected deleteSelectedOrderLine(): void {
    const ids = this.selectedEl.map(e => e.id);
    this.commandeService.deleteOrderLinesByIds(this.commande.id, ids).subscribe(() => {
      this.refreshCommande();
      this.selectedEl = [];
      this.focusPrdoduitBox();
    });
  }

  protected exportPdf(): void {
    this.commandeService.exportToPdf(this.commande.id).subscribe(blod => {
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
    });
  }


  protected onCreateBon(): void {
    this.ref = this.dialogService.open(DeliveryModalComponent, {
      data: {commande: this.commande},
      header: 'CREATION DU BON DE LIVRAISON',
      width: '40%',
    });
    this.ref.onClose.subscribe((commande: ICommande) => {
      if (commande) {
        this.reloadCommande();
      }
    });
  }

  private reloadCommande(): void {
    this.commandeService.find(this.commande.id).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande.orderLines;
    });
  }

  protected exportCSV(): void {
    this.commandeService.exportToCsv(this.commande.id).subscribe(blod => saveAs(blod));
  }

  protected searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  protected onSelect(): void {
    setTimeout(() => {
      const el = this.quantityBox().nativeElement;
      el.focus();
      el.select();
    }, 50);
  }

  protected onProviderSelect(): void {
    if (this.commande && this.commande.id) {
      this.changeGrossiste();
    } else {
      this.focusPrdoduitBox();
    }
  }

  protected focusPrdoduitBox(): void {
    setTimeout(() => {
      this.produitbox().inputEL.nativeElement.focus();
      this.produitbox().inputEL.nativeElement.select();
    }, 50);
  }

  protected loadProduits(): void {
    this.produitService
      .queryLite({
        page: 0,
        size: 5,
        withdetail: false,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  protected onFilterCommandeLines(): void {
    const query = {
      commandeId: this.commande.id,
      search: this.search,
      filterCommaneEnCours: this.selectedFilter,
      orderBy: this.tris,
    };
    this.commandeService.filterCommandeLines(query).subscribe(res => {
      this.orderLines = res.body!;
    });
  }

  protected checkQuantityNotSet(orderLine: IOrderLine): boolean {
    return orderLine.quantityRequested === 0;
  }

  openDialog(message: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {backdrop: 'static'});
    modalRef.componentInstance.message = message;
  }

  protected loadFournisseurs(search?: string): void {
    const query: string = search || '';
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

  protected orderLineTableColor(orderLine: IOrderLine): string {
    if (orderLine) {
      if (orderLine.costAmount !== orderLine.orderCostAmount) {
        return 'table-danger';
      } else if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) {
        return 'table-warning';
      }
    }
    return '';
  }

  protected confirmDeleteAll(): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer supprimer toutes les lignes ?',
      header: 'SUPPRESSION ',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.deleteSelectedOrderLine(),
      reject: () => {
        this.updateProduitQtyBox();
      },
      key: 'deleteAll',
    });
  }

  protected confirmDeleteItem(item: IOrderLine): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer de la commande  ce produit ?',
      header: 'SUPPRESSION DE PRODUIT ',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.onDeleteOrderLineById(item),
      reject: () => {
        this.updateProduitQtyBox();
      },
      key: 'deleteItem',
    });
  }


  protected cancel(): void {
    this.fileDialog = false;
  }

  protected onImporterReponseCommande(event: any): void {
    const formData: FormData = new FormData();
    const file = event.files[0];
    formData.append('commande', file, file.name);

    this.showsPinner('commandeEnCourspinner');
    this.commandeService.importerReponseCommande(this.commande.id, formData).subscribe({
      next: res => {
        this.hidePinner('commandeEnCourspinner');
        this.refreshCommande();
        this.cancel();
        this.openImporterReponseCommandeDialog(res.body);
      },
      error: error => {
        this.hidePinner('commandeEnCourspinner');

        this.onCommonError(error);
      },
    });
  }

  protected openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeEnCoursResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.commande;
  }

  protected resetAll(): void {
    this.commande = null;
    this.orderLines = [];
    this.selectedProvider = null;
    this.produitSelected = null;
    // this.fournisseurDisabled = false;
    this.selectedFilter = 'ALL';
    setTimeout(() => {
      this.fournisseurBox()?.inputEL?.nativeElement.focus();
    }, 50);
  }

  ngAfterViewInit(): void {
    if (this.commande && this.commande.id) {
      this.focusPrdoduitBox();
    } else {
      setTimeout(() => {
        this.fournisseurBox()?.inputEL?.nativeElement?.focus();
      }, 100);
    }
  }

  private refreshCommande(): void {
    this.commandeService.find(this.commande.id).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande.orderLines;
      this.focusPrdoduitBox();
    });
  }

  private updateProduitQtyBox(): void {
    if (this.isCommandeEnCours()) {
      if (this.quantityBox()) {
        this.quantityBox().nativeElement.value = 1;
      }
      this.produitSelected = null;
      this.focusPrdoduitBox();
    }
  }


  private onSaveOrderLineSuccess(commandeId: number): void {
    this.commandeService.find(commandeId).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande.orderLines;
      this.updateProduitQtyBox();
    });
  }


  private onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'error', 'Erreur');
    } else {
      this.errorService.getErrorMessageTranslation(error.error.errorKey).subscribe({
        next: translatedErrorMessage => {
          this.openInfoDialog(translatedErrorMessage, 'error', 'Erreur');
        },
        error: () => this.openInfoDialog(this.errorService.getErrorMessage(error), 'error', 'Erreur'),
      });
    }
  }

  private onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private onError(): void {
  }

  private subscribeToSaveOrderLineResponse(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: (res: HttpResponse<ICommande>) => this.onSaveOrderLineSuccess(res.body?.id),
      error: (err: any) => this.onCommonError(err),
    });
  }

  private openInfoDialog(message: string, severity: string, summary: string): void {
    // const modalRef = this.modalService.open(AlertInfoComponent, {
    //   backdrop: 'static',
    //   centered: true,
    // });
    // modalRef.componentInstance.message = message;
    // modalRef.componentInstance.infoClass = infoClass;
    this.messageService.add({severity, summary, detail: message});
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
            fournisseurId: this.selectedProvider,
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

  protected onAddLot(deliveryItem: IOrderLine): void {
    const quantityReceived = deliveryItem.quantityReceived || deliveryItem.quantityRequested;
    if (quantityReceived > 1 || deliveryItem.lots.length > 0) {
      this.ref = this.dialogService.open(ListLotComponent, {
        data: {deliveryItem},
        width: '60%',
        header: `GESTION DE LOTS DE LA LIGNE ${deliveryItem.produitLibelle} [${deliveryItem.produitCip}]`,
      });
    } else {
      this.ref = this.dialogService.open(FormLotComponent, {
        data: {entity: null, deliveryItem},
        width: '40%',
        header: 'Ajout de lot',
      });
    }
    // this.ref.onClose.subscribe(() => this.onFilterReceiptItems());
  }

  protected onConfirmFinalize(): void {
    if (this.isCommandeFinalisee()) {
      this.confirmationService.confirm({
        message: "Voullez-vous faire l'entrée en stock ?",
        header: 'FINALISATION DE LA COMMANDE',
        icon: 'pi pi-info-circle',
        rejectButtonProps: rejectButtonProps(),
        acceptButtonProps: acceptButtonProps(),
        accept: () => this.onFinalize(),

        key: 'stayThere',
      });
    }
  }

  private onFinalize(): void {
    this.showsPinner('commandeEnCourspinner');
    this.deliveryService.finalizeSaisieEntreeStock(this.commande).subscribe({
      next: () => {
        this.hidePinner('commandeEnCourspinner');
        this.confirmPrintTicket(this.commande);
      },
      error: error => {
        this.onCommonError(error);
        this.hidePinner('commandeEnCourspinner');
      },
    });
  }

  protected confirmPrintTicket(commande: ICommande): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous imprimer les étiquettes ?',
      header: 'IMPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.printEtiquette(commande),
      reject: () => {
        this.commande = null;
        this.previousState();
      },
      key: 'stayThere',
    });
  }


  protected isLotActif(): void {
    const paramGestionLot = this.configurationService.getParamByKey(Params.APP_GESTION_LOT);
    if (paramGestionLot) {
      this.showLotBtn = Number(paramGestionLot.value) === 0;
    }
  }

  protected editLigneInfos(orderLine: IOrderLine): void {
    this.ref = this.dialogService.open(EditProduitComponent, {
      data: {deliveryItem: orderLine, delivery: this.commande},
      width: '70%',
      header: `EDITION DU PRODUIT ${orderLine.produitLibelle} [${orderLine.produitCip}]`,
    });

    //this.ref.onClose.subscribe(() => this.onFilterReceiptItems());
  }

  protected showLotColumn(): boolean {
    return this.showLotBtn || this.orderLines.some(orderLine => orderLine.lots.length > 0);
  }

  private changeGrossiste(): void {
    this.commande.fournisseurId = this.selectedProvider;
    this.commandeService.changeGrossiste(this.commande).subscribe({
      next: () => {
        this.onSaveOrderLineSuccess(this.commande.id);
      },
      error: error => {
        this.onCommonError(error);
        this.onSaveOrderLineSuccess(this.commande.id);
      },
    });
  }

  private subscribeUpdateOrderLineResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveOrderLineSuccess(this.commande?.id),
      error: (err: any) => this.onCommonError(err),
    });
  }

  private isCommandeEnCours(): boolean {
    return (
      this.commande === null ||
      this.commande.id === undefined ||
      this.commande.id === null ||
      this.commande.orderStatus === OrderStatut.REQUESTED
    );
  }

  private isCommandeFinalisee(): boolean {
    return this.commande && this.commande.orderStatus === OrderStatut.RECEIVED;
  }

  private isClosedCommande(): boolean {
    return this.commande && this.commande.orderStatus === OrderStatut.CLOSED;
  }

  private printEtiquette(commande: ICommande): void {
    this.ref = this.dialogService.open(EtiquetteComponent, {
      data: {entity: commande},
      width: '40%',
      header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${commande.receiptReference} ] `,
    });
    this.ref.onDestroy.subscribe(() => {
      this.commande = null;
      this.previousState();
    });
    this.ref.onClose.subscribe(() => {
      this.commande = null;
      this.previousState();
    });
  }

}
