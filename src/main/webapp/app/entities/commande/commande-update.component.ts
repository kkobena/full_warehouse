import { AfterViewInit, Component, ElementRef, inject, OnInit, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';

import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { Commande, ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { ProduitService } from '../produit/produit.service';
import { IProduit } from 'app/shared/model/produit.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { IFournisseur } from '../../shared/model/fournisseur.model';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { IOrderLine, OrderLine } from '../../shared/model/order-line.model';
import { MenuItem } from 'primeng/api';
import { ErrorService } from '../../shared/error.service';
import { saveAs } from 'file-saver';
import { IResponseCommande } from '../../shared/model/response-commande.model';
import { CommandeEnCoursResponseDialogComponent } from './commande-en-cours-response-dialog.component';
import { APPEND_TO, PRODUIT_COMBO_MIN_LENGTH, PRODUIT_NOT_FOUND } from '../../shared/constants/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { InputTextModule } from 'primeng/inputtext';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { SplitButtonModule } from 'primeng/splitbutton';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { FileUploadModule } from 'primeng/fileupload';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';
import { NgSelectModule } from '@ng-select/ng-select';
import { CommandCommonService } from './command-common.service';
import { SelectModule } from 'primeng/select';
import { InputGroup } from 'primeng/inputgroup';
import { InputGroupAddon } from 'primeng/inputgroupaddon';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SORT } from '../../shared/util/command-item-sort';
import { DeliveryModalComponent } from './delevery/form/delivery-modal.component';
import { DeliveryService } from './delevery/delivery.service';
import { OrderLineLotsComponent } from './lot/order-line-lots.component';
import { ButtonGroup } from 'primeng/buttongroup';
import { ListLotComponent } from './lot/list/list-lot.component';
import { FormLotComponent } from './lot/form-lot.component';
import { OrderStatut } from '../../shared/model/enumerations/order-statut.model';
import { EtiquetteComponent } from './delevery/etiquette/etiquette.component';
import { FloatLabel } from 'primeng/floatlabel';
import { Params } from '../../shared/model/enumerations/params.model';
import { ConfigurationService } from '../../shared/configuration.service';
import { EditProduitComponent } from './delevery/form/edit-produit/edit-produit.component';
import { showCommonModal } from '../sales/selling-home/sale-helper';
import { ConfirmDialogComponent } from '../../shared/dialog/confirm-dialog/confirm-dialog.component';
import { ToastAlertComponent } from '../../shared/toast-alert/toast-alert.component';
import { FileResponseModalComponent } from './file-response-modal/file-response-modal.component';
import { SpinnerComponent } from '../../shared/spinner/spinner.component';
import { CommandeId } from '../../shared/model/abstract-commande.model';
import { TauriPrinterService } from '../../shared/services/tauri-printer.service';
import { handleBlobForTauri } from '../../shared/util/tauri-util';

@Component({
  selector: 'jhi-commande-update',
  templateUrl: './commande-update.component.html',
  styleUrls: ['./commande-update.component.scss'],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    NgSelectModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TagModule,
    TableModule,
    RouterModule,
    SplitButtonModule,
    AutoCompleteModule,
    FileUploadModule,
    ToolbarModule,
    TooltipModule,
    SelectModule,
    InputGroup,
    InputGroupAddon,
    IconField,
    InputIcon,
    ButtonGroup,
    FloatLabel,
    ConfirmDialogComponent,
    ToastAlertComponent,
    SpinnerComponent,

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
  protected fournisseurBox = viewChild<any>('fournisseurBox');
  protected produitbox = viewChild<any>('produitbox');
  protected readonly sorts = SORT;
  protected readonly PRODUIT_NOT_FOUND = PRODUIT_NOT_FOUND;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly APPEND_TO = APPEND_TO;
  protected showLotBtn = false;
  private readonly commandeService = inject(CommandeService);
  private readonly produitService = inject(ProduitService);
  private readonly modalService = inject(NgbModal);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly errorService = inject(ErrorService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly deliveryService = inject(DeliveryService);
  private readonly commandCommonService = inject(CommandCommonService);
  private readonly router = inject(Router);
  private readonly configurationService = inject(ConfigurationService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly confimDialog = viewChild.required<ConfirmDialogComponent>('confirmDialog');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  private readonly tauriPrinterService = inject(TauriPrinterService);
  constructor() {
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

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ commande }) => {
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

  openDialog(message: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static' });
    modalRef.componentInstance.message = message;
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
      this.commandeService.deleteOrderLineById(orderLine.orderLineId).subscribe(() => {
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
    const ids = this.selectedEl.map(e => {
      return { id: e.id!, orderDate: e.orderDate! };
    });
    this.commandeService.deleteOrderLinesByIds(this.commande.commandeId, ids).subscribe(() => {
      this.refreshCommande();
      this.selectedEl = [];
      this.focusPrdoduitBox();
    });
  }

  protected exportPdf(): void {
    this.commandeService.exportToPdf(this.commande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'commande_en_cours');
      } else {
        window.open(URL.createObjectURL(blob));
      }
    });
  }

  protected onCreateBon(): void {
    showCommonModal(
      this.modalService,
      DeliveryModalComponent,
      {
        commande: this.commande,
        header: 'CREATION DU BON DE LIVRAISON',
      },
      commande => {
        if (commande) {
          this.reloadCommande();
        }
      },
      'lg',
    );
  }

  protected exportCSV(): void {
    this.commandeService.exportToCsv(this.commande.commandeId).subscribe(blob => {
      if (this.tauriPrinterService.isRunningInTauri()) {
        handleBlobForTauri(blob, 'commande_en_cours', 'csv');
      } else {
        saveAs(blob);
      }
    });
  }

  protected searchFn(event: any): void {
    this.searchValue = event.query;
    this.loadProduits();
  }

  protected onSelect(): void {
    this.focusAndSelectElement(this.quantityBox().nativeElement, 50);
  }

  protected onProviderSelect(): void {
    if (this.commande && this.commande.id) {
      this.changeGrossiste();
    } else {
      this.focusPrdoduitBox();
    }
  }

  protected focusPrdoduitBox(): void {
    if (this.produitbox()) {
      this.focusAndSelectElement(this.produitbox()?.inputEL.nativeElement, 50);
    }
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
        return 'pharma-row-danger';
      } else if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) {
        return 'pharma-row-warning';
      }
    }
    return '';
  }

  protected confirmDeleteAll(): void {
    this.confimDialog().onConfirm(
      () => this.deleteSelectedOrderLine(),
      'Suppression',
      'Voullez-vous supprimer toutes les lignes ?',
      null,
      () => this.updateProduitQtyBox(),
    );
  }

  protected confirmDeleteItem(item: IOrderLine): void {
    this.confimDialog().onConfirm(
      () => this.onDeleteOrderLineById(item),
      'Suppression',
      'Voullez-vous supprimer de la commande  ce produit ?',
      null,
      () => this.updateProduitQtyBox(),
    );
  }

  protected cancel(): void {
    this.onImporterReponseCommande();
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      {
        commandeSelected: this.commande,
        header: 'IMPORTER REPONSE',
      },
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      'lg',
    );
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

  protected onAddLot(deliveryItem: IOrderLine): void {
    const quantityReceived = deliveryItem.quantityReceived || deliveryItem.quantityRequested;
    if (quantityReceived > 1 || deliveryItem.lots.length > 0) {
      showCommonModal(
        this.modalService,
        ListLotComponent,
        {
          deliveryItem,
          header: `GESTION DE LOTS DE LA LIGNE ${deliveryItem.produitLibelle} [${deliveryItem.produitCip}]`,
        },
        result => {
          console.log(result);
        },
        'lg',
      );
    } else {
      showCommonModal(
        this.modalService,
        FormLotComponent,
        {
          entity: null,
          deliveryItem,
          header: 'Ajout de lot',
        },
        result => {
          console.log(result);
        },
        'lg',
      );
    }
    // this.ref.onClose.subscribe(() => this.onFilterReceiptItems());
  }

  protected onConfirmFinalize(): void {
    if (this.isCommandeFinalisee()) {
      this.confimDialog().onConfirm(() => this.onFinalize(), 'Finalisation de la commande', "Voullez-vous faire l'entrée en stock ?");
    }
  }

  protected confirmPrintTicket(commandeId: CommandeId): void {
    this.confimDialog().onConfirm(
      () =>
        this.printEtiquette({
          ...this.commande,
          commandeId: commandeId,
        }),
      'Impression',
      'Voullez-vous imprimer les étiquettes ?',
      null,
      () => {
        this.commande = null;
        this.previousState();
      },
    );
  }

  protected isLotActif(): void {
    const paramGestionLot = this.configurationService.getParamByKey(Params.APP_GESTION_LOT);
    if (paramGestionLot) {
      this.showLotBtn = Number(paramGestionLot.value) === 0;
    }
  }

  protected editLigneInfos(orderLine: IOrderLine): void {
    showCommonModal(
      this.modalService,
      EditProduitComponent,
      {
        deliveryItem: orderLine,
        delivery: this.commande,
        header: `EDITION DU PRODUIT ${orderLine.produitLibelle} [${orderLine.produitCip}]`,
      },
      result => {
        console.log(result);
      },
      'xl',
    );
  }

  protected showLotColumn(): boolean {
    return this.showLotBtn || this.orderLines.some(orderLine => orderLine.lots.length > 0);
  }

  private reloadCommande(): void {
    this.commandeService.find(this.commande.commandeId).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande.orderLines;
    });
  }

  private focusAndSelectElement(element: any, delay: number): void {
    setTimeout(() => {
      if (element) {
        element.focus();
        element.select();
      }
    }, delay);
  }

  private refreshCommande(): void {
    this.commandeService.find(this.commande.commandeId).subscribe(res => {
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

  private onSaveOrderLineSuccess(commandeId: CommandeId): void {
    this.commandeService.find(commandeId).subscribe(res => {
      this.commande = res.body;
      this.orderLines = this.commande.orderLines;
      this.updateProduitQtyBox();
    });
  }

  private onCommonError(error: any): void {
    this.openInfoDialog(this.errorService.getErrorMessage(error), 'error', 'Erreur');
  }

  private onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  private onError(): void {}

  private subscribeToSaveOrderLineResponse(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: (res: HttpResponse<ICommande>) => this.onSaveOrderLineSuccess(res.body?.commandeId),
      error: (err: any) => this.onCommonError(err),
    });
  }

  private openInfoDialog(message: string, severity: any, summary: string): void {
    this.alert().show(summary, message, severity);
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

  private showsPinner(): void {
    this.spinner().show();
  }

  private hidePinner(): void {
    this.spinner().hide();
  }

  private onFinalize(): void {
    this.showsPinner();
    this.deliveryService.finalizeSaisieEntreeStock(this.commande).subscribe({
      next: (res: HttpResponse<CommandeId>) => {
        this.hidePinner();
        this.confirmPrintTicket(res.body);
      },
      error: error => {
        this.onCommonError(error);
        this.hidePinner();
      },
    });
  }

  private changeGrossiste(): void {
    this.commande.fournisseurId = this.selectedProvider;
    this.commandeService.changeGrossiste(this.commande).subscribe({
      next: () => {
        this.onSaveOrderLineSuccess(this.commande.commandeId);
      },
      error: error => {
        this.onCommonError(error);
        this.onSaveOrderLineSuccess(this.commande.commandeId);
      },
    });
  }

  private subscribeUpdateOrderLineResponse(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveOrderLineSuccess(this.commande?.commandeId),
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
    showCommonModal(
      this.modalService,
      EtiquetteComponent,
      {
        entity: commande,
        header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${commande.receiptReference} ] `,
      },
      () => {
        this.commande = null;
        this.previousState();
      },
      'lg',
      null,
      () => {
        this.commande = null;
        this.previousState();
      },
    );
  }
}
