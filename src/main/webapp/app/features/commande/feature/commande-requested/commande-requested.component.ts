import {AfterViewInit, Component, ElementRef, inject, input, OnInit, output, viewChild} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Commande, ICommande} from 'app/shared/model/commande.model';
import {CommandeService} from '../../../../entities/commande/commande.service';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {IFournisseur} from '../../../../shared/model/fournisseur.model';
import {FournisseurService} from '../../../../entities/fournisseur/fournisseur.service';
import {IOrderLine, OrderLine} from '../../../../shared/model/order-line.model';
import {ErrorService} from '../../../../shared/error.service';
import {IResponseCommande} from '../../../../shared/model/response-commande.model';
import {CommandeEnCoursResponseDialogComponent} from '../../../../entities/commande/commande-en-cours-response-dialog.component';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {InputTextModule} from 'primeng/inputtext';
import {TagModule} from 'primeng/tag';
import {TableModule} from 'primeng/table';
import {FileUploadModule} from 'primeng/fileupload';
import {TooltipModule} from 'primeng/tooltip';
import {SelectModule} from 'primeng/select';
import {InputGroup} from 'primeng/inputgroup';
import {InputGroupAddon} from 'primeng/inputgroupaddon';
import {ButtonGroup} from 'primeng/buttongroup';
import {Toast} from 'primeng/toast';
import {DeliveryModalComponent} from '../../ui/delivery/delivery-modal/delivery-modal.component';
import {ListLotComponent} from '../../ui/lot/list/list-lot.component';
import {FormLotComponent} from '../../ui/lot/form/form-lot.component';
import {EditProduitComponent} from '../../ui/delivery/edit-produit/edit-produit.component';
import {FileResponseModalComponent} from '../../ui/file-response-modal/file-response-modal.component';
import {showCommonModal} from '../../../../entities/sales/selling-home/sale-helper';
import {CommandeId} from '../../../../shared/model/abstract-commande.model';
import {Params} from '../../../../shared/model/enumerations/params.model';
import {ConfigurationService} from '../../../../shared/configuration.service';
import {NotificationService} from '../../../../shared/services/notification.service';
import {NgbConfirmDialogService} from '../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import {PharmamlHomeComponent} from '../pharmaml';
import {ProduitSearch} from '../../../../shared/model';
import {CommandeProductSearchComponent} from '../../ui/commande-product-search/commande-product-search.component';
import {CommandeStatusBarComponent} from '../../ui/commande-status-bar/commande-status-bar.component';
import {ImportSuggestionModalComponent} from '../../ui/import-suggestion/import-suggestion-modal.component';
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-commande-requested',
  templateUrl: './commande-requested.component.html',
  styleUrls: ['./commande-requested.component.scss'],
  host: {
    '(window:keydown.F9)': 'onKeyF9($event)',
    '(window:keydown.escape)': 'onKeyEscape($event)',
  },
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    RippleModule,
    InputTextModule,
    TagModule,
    TableModule,
    CommandeProductSearchComponent,
    FileUploadModule,
    TooltipModule,
    SelectModule,
    InputGroup,
    InputGroupAddon,
    ButtonGroup,
    Toast,
    PharmamlHomeComponent,
    CommandeStatusBarComponent,
  ],
})
export class CommandeRequestedComponent implements OnInit, AfterViewInit {
  commande = input<ICommande | null | undefined>(null);
  commandeChange = output<ICommande | null>();

  protected orderLines: IOrderLine[] = [];
  protected fournisseurs: IFournisseur[] = [];
  protected selectedEl: IOrderLine[] = [];
  protected quantiteSaisie = 1;
  protected produitSelected: ProduitSearch | null = null;
  protected selectedProvider?: number | null = null;
  protected showLotBtn = false;
  protected seuilMontantCommande = 0;
  protected currentCommande?: ICommande | null = null;

  protected readonly quantityBox = viewChild.required<ElementRef>('quantityBox');
  protected readonly fournisseurBox = viewChild<any>('fournisseurBox');
  protected readonly productSearch = viewChild.required<CommandeProductSearchComponent>('productSearch');

  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly commandeService = inject(CommandeService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly modalService = inject(NgbModal);
  private readonly errorService = inject(ErrorService);
  private readonly configurationService = inject(ConfigurationService);

  ngOnInit(): void {
    const c = this.commande();
    if (c?.id) {
      this.currentCommande = c;
      this.orderLines = c.orderLines ?? [];
      this.selectedProvider = c.fournisseurId;
    }
    this.loadFournisseurs();
    this.isLotActif();
    this.loadSeuilMontant();
  }

  ngAfterViewInit(): void {
    if (this.currentCommande?.id) {
      this.focusProduitBox();
    } else {
      setTimeout(() => this.fournisseurBox()?.inputEL?.nativeElement?.focus(), 100);
    }
  }

  protected previousState(): void {
    window.history.back();
  }

  protected onQuantityBoxAction(event: any): void {
    const qty = Number(event.target.value);
    if (qty > 0) this.onAddOrderLine(qty);
  }

  protected onQuantity(): void {
    const qty = Number(this.quantityBox().nativeElement.value);
    if (qty > 0) this.onAddOrderLine(qty);
  }

  protected onAddOrderLine(quantityRequested: number): void {
    if (!this.produitSelected) return;
    if (!this.selectedProvider) {
      this.notificationService.error('Veuillez selectionner un fournisseur', 'Erreur');
      return;
    }
    if (this.currentCommande?.id !== undefined) {
      this.subscribeToSaveOrderLine(
        this.commandeService.createOrUpdateOrderLine(this.buildOrderLine(this.produitSelected, quantityRequested)),
      );
    } else {
      this.subscribeToSaveOrderLine(this.commandeService.create(this.buildCommande(this.produitSelected, quantityRequested)));
    }
  }

  protected onUpdateQuantityRequested(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty > 0) {
      orderLine.quantityRequested = qty;
      this.subscribeToSaveOrderLine(this.commandeService.updateQuantityRequested(orderLine));
    }
  }

  protected onUpdateFreeQtyRequested(orderLine: IOrderLine, event: any): void {
    const qty = Number(event.target.value);
    if (qty >= 0) {
      orderLine.freeQty = qty;
      this.subscribeUpdateOrderLine(this.commandeService.updateQuantityUG(orderLine));
    }
  }

  protected onUpdateOrderCostAmount(orderLine: IOrderLine, event: any): void {
    const amount = Number(event.target.value);
    if (this.currentCommande && amount > 0) {
      orderLine.orderCostAmount = amount;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  protected onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const price = Number(event.target.value);
    if (this.currentCommande && price > 0) {
      orderLine.orderUnitPrice = price;
      this.subscribeToSaveOrderLine(this.commandeService.updateOrderUnitPrice(orderLine));
    }
  }

  protected onDeleteOrderLine(orderLine: IOrderLine): void {
    if (this.currentCommande) {
      this.commandeService.deleteOrderLineById(orderLine.orderLineId).subscribe(() => this.refreshCommande());
    }
  }

  protected confirmDeleteItem(item: IOrderLine): void {
    this.confirmDialog.onConfirm(
      () => this.onDeleteOrderLine(item),
      'Suppression',
      'Voullez-vous supprimer de la commande ce produit ?',
      null,
      () => this.resetProductInput(),
    );
  }

  protected confirmDeleteAll(): void {
    this.confirmDialog.onConfirm(
      () => this.deleteSelectedLines(),
      'Suppression',
      'Voullez-vous supprimer toutes les lignes ?',
      null,
      () => this.resetProductInput(),
    );
  }

  protected onCreateBon(): void {
    const montant = this.currentCommande?.grossAmount ?? 0;
    if (this.seuilMontantCommande > 0 && montant > this.seuilMontantCommande) {
      const fmt = (n: number): string => n.toLocaleString('fr-FR');
      this.confirmDialog.onConfirm(
        () => this.doCreateBon(),
        'Validation titulaire requise',
        `Le montant total de cette commande (${fmt(montant)} F) dépasse le seuil autorisé (${fmt(this.seuilMontantCommande)} F).\n\nLa validation du pharmacien titulaire est requise.\nVoulez-vous continuer ?`,
      );
    } else {
      this.doCreateBon();
    }
  }

  private doCreateBon(): void {
    showCommonModal(
      this.modalService,
      DeliveryModalComponent,
      {commande: this.currentCommande, header: 'CREATION DU BON DE LIVRAISON'},
      commande => {
        if (commande) this.reloadCommande();
      },
      'lg',
    );
  }

  protected onImportSuggestion(): void {
    const modalRef = this.modalService.open(ImportSuggestionModalComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.commandeId = this.currentCommande!.commandeId;
    modalRef.componentInstance.fournisseurId = this.currentCommande!.fournisseurId;
    modalRef.result.then(
      imported => { if (imported) this.refreshCommande(); },
      () => {},
    );
  }

  protected onImporterReponseCommande(): void {
    showCommonModal(
      this.modalService,
      FileResponseModalComponent,
      {commandeSelected: this.currentCommande, header: 'IMPORTER REPONSE'},
      (responseCommande: IResponseCommande) => {
        if (responseCommande) {
          this.refreshCommande();
          this.openImporterReponseCommandeDialog(responseCommande);
        }
      },
      'lg',
    );
  }

  protected onProductSelected(product: ProduitSearch | null): void {
    this.produitSelected = product;
    if (product) {
      this.focusAndSelect(this.quantityBox().nativeElement, 50);
    }
  }

  protected onProductScanned(product: ProduitSearch): void {
    this.produitSelected = product;
    this.focusAndSelect(this.quantityBox().nativeElement, 50);
  }

  protected onProviderSelect(): void {
    if (this.currentCommande?.id) {
      this.changeGrossiste();
    } else {
      this.focusProduitBox();
    }
  }

  protected orderLineTableColor(orderLine: IOrderLine): string {
    if (!orderLine) return '';
    if (orderLine.costAmount !== orderLine.orderCostAmount) return 'pharma-row-danger';
    if (orderLine.regularUnitPrice !== orderLine.orderUnitPrice) return 'pharma-row-warning';
    return '';
  }

  protected showLotColumn(): boolean {
    return this.showLotBtn || this.orderLines.some(l => l.lots.length > 0);
  }

  protected onAddLot(orderLine: IOrderLine): void {
    const qty = orderLine.quantityReceived || orderLine.quantityRequested;
    if (qty > 1 || orderLine.lots.length > 0) {
      showCommonModal(
        this.modalService,
        ListLotComponent,
        {deliveryItem: orderLine, header: `GESTION DE LOTS DE LA LIGNE ${orderLine.produitLibelle} [${orderLine.produitCip}]`},
        null,
        'lg',
      );
    } else {
      showCommonModal(
        this.modalService,
        FormLotComponent,
        {entity: null, deliveryItem: orderLine, header: 'Ajout de lot'},
        null,
        'lg',
      );
    }
  }

  protected editLigneInfos(orderLine: IOrderLine): void {
    showCommonModal(
      this.modalService,
      EditProduitComponent,
      {
        deliveryItem: orderLine,
        delivery: this.currentCommande,
        header: `EDITION DU PRODUIT ${orderLine.produitLibelle} [${orderLine.produitCip}]`,
      },
      null,
      'xl',
    );
  }

  protected focusProduitBox(): void {
    this.productSearch()?.getFocus();
  }

  /** F9 : déclenche "Créer le bon de livraison" si une commande est en cours */
  protected onKeyF9(event: KeyboardEvent): void {
    event.preventDefault();
    if (this.currentCommande?.id) {
      this.onCreateBon();
    }
  }

  /** Echap : remet le focus sur la recherche produit */
  protected onKeyEscape(event: KeyboardEvent): void {
    // Ne pas interférer si une modale est ouverte
    if (document.querySelector('.modal.show')) return;
    event.preventDefault();
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private openImporterReponseCommandeDialog(responseCommande: IResponseCommande): void {
    const modalRef = this.modalService.open(CommandeEnCoursResponseDialogComponent, {
      size: 'xl',
      scrollable: true,
      backdrop: 'static',
    });
    modalRef.componentInstance.responseCommande = responseCommande;
    modalRef.componentInstance.commande = this.currentCommande;
  }

  private loadFournisseurs(search = ''): void {
    this.fournisseurService
      .query({page: 0, size: 9999, search})
      .subscribe({next: (res: HttpResponse<IFournisseur[]>) => (this.fournisseurs = res.body!)});
  }

  private isLotActif(): void {
    const param = this.configurationService.getParamByKey(Params.APP_GESTION_LOT);
    if (param) this.showLotBtn = Number(param.value) === 0;
  }

  private loadSeuilMontant(): void {
    const param = this.configurationService.getParamByKey(Params.APP_SEUIL_MONTANT_COMMANDE);
    if (param) this.seuilMontantCommande = Number(param.value) || 0;
  }

  private reloadCommande(): void {
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines;
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private refreshCommande(): void {
    this.commandeService.find(this.currentCommande.commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines;
      this.focusProduitBox();
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private onSaveSuccess(commandeId: CommandeId): void {
    this.commandeService.find(commandeId).subscribe(res => {
      this.currentCommande = res.body;
      this.orderLines = this.currentCommande.orderLines;
      this.resetProductInput();
      this.commandeChange.emit(this.currentCommande);
    });
  }

  private resetProductInput(): void {
    if (this.quantityBox()) this.quantityBox().nativeElement.value = 1;
    this.produitSelected = null;
    this.productSearch()?.reset();
    this.focusProduitBox();
  }

  private deleteSelectedLines(): void {
    const ids = this.selectedEl.map(e => ({id: e.id, orderDate: e.orderDate}));
    this.commandeService.deleteOrderLinesByIds(this.currentCommande.commandeId, ids).subscribe(() => {
      this.selectedEl = [];
      this.refreshCommande();
    });
  }

  private changeGrossiste(): void {
    this.currentCommande.fournisseurId = this.selectedProvider;
    this.commandeService.changeGrossiste(this.currentCommande).subscribe({
      next: () => this.onSaveSuccess(this.currentCommande.commandeId),
      error: error => {
        this.notificationService.error(this.errorService.getErrorMessage(error), 'Erreur');
        this.onSaveSuccess(this.currentCommande.commandeId);
      },
    });
  }

  private subscribeToSaveOrderLine(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: res => this.onSaveSuccess(res.body?.commandeId),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
    });
  }

  private subscribeUpdateOrderLine(result: Observable<HttpResponse<{}>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(this.currentCommande?.commandeId),
      error: err => this.notificationService.error(this.errorService.getErrorMessage(err), 'Erreur'),
    });
  }

  private buildOrderLine(produit: ProduitSearch, quantityRequested: number): IOrderLine {
    return {
      ...new OrderLine(),
      produitId: produit.id,
      totalQuantity: produit.totalQuantity,
      commande:
        this.currentCommande?.id !== undefined
          ? this.currentCommande
          : {...new Commande(), fournisseurId: this.selectedProvider},
      quantityRequested,
    };
  }

  private buildCommande(produit: ProduitSearch, quantityRequested: number): ICommande {
    return {
      ...new Commande(),
      fournisseurId: this.selectedProvider,
      orderLines: [this.buildOrderLine(produit, quantityRequested)],
    };
  }

  private focusAndSelect(element: any, delay: number): void {
    setTimeout(() => {
      if (element) {
        element.focus();
        element.select();
      }
    }, delay);
  }
}
