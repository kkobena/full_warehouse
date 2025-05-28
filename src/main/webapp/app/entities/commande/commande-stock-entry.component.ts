import {Component, inject, input, OnInit} from '@angular/core';
import {ConfirmationService} from 'primeng/api';
import {DialogService, DynamicDialogRef} from 'primeng/dynamicdialog';
import {CommandeService} from './commande.service';
import {ProduitService} from '../produit/produit.service';
import {ActivatedRoute, Router} from '@angular/router';
import {NgbModal, NgbTooltipModule} from '@ng-bootstrap/ng-bootstrap';
import {IOrderLine} from '../../shared/model/order-line.model';
import {Observable} from 'rxjs';
import {AlertInfoComponent} from '../../shared/alert/alert-info.component';
import {ConfigurationService} from '../../shared/configuration.service';
import {checkIfRomToBeUpdated, formatNumberToString} from '../../shared/util/warehouse-util';
import {NgxSpinnerModule, NgxSpinnerService} from 'ngx-spinner';
import {Params} from '../../shared/model/enumerations/params.model';
import {FormLotComponent} from './lot/form-lot.component';
import {ListLotComponent} from './lot/list/list-lot.component';
import {DeliveryService} from './delevery/delivery.service';
import {IDelivery} from '../../shared/model/delevery.model';
import {IDeliveryItem} from '../../shared/model/delivery-item';
import {DeliveryModalComponent} from './delevery/form/delivery-modal.component';
import {EditProduitComponent} from './delevery/form/edit-produit/edit-produit.component';
import {EtiquetteComponent} from './delevery/etiquette/etiquette.component';
import {WarehouseCommonModule} from '../../shared/warehouse-common/warehouse-common.module';
import {FormsModule} from '@angular/forms';
import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {ConfirmDialogModule} from 'primeng/confirmdialog';
import {NgSelectModule} from '@ng-select/ng-select';
import {InputTextModule} from 'primeng/inputtext';
import {AG_GRID_LOCALE_FR} from '@ag-grid-community/locale';
import {CommandeBtnComponent} from './btn/commande-btn.component';
import {ReceiptStatusComponent} from './status/receipt-status.component';
import {
  AllCommunityModule,
  ClientSideRowModelModule,
  GridApi,
  GridReadyEvent,
  ModuleRegistry,
  provideGlobalGridOptions,
  RowModelType,
  themeAlpine,
} from 'ag-grid-community';

import dayjs from 'dayjs';
import {TvaService} from '../tva/tva.service';
import {HttpResponse} from '@angular/common/http';
import {ITva} from '../../shared/model/tva.model';
import {acceptButtonProps, rejectButtonProps} from '../../shared/util/modal-button-props';
import {AgGridAngular} from 'ag-grid-angular';
import {IconField} from 'primeng/iconfield';
import {InputIcon} from 'primeng/inputicon';
import {Select} from 'primeng/select';
import {Toolbar} from 'primeng/toolbar';
import {AbstractCommande} from "../../shared/model/abstract-commande.model";

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);
provideGlobalGridOptions({theme: themeAlpine});

@Component({
  selector: 'jhi-commande-stock-entry',
  templateUrl: './commande-stock-entry.component.html',
  providers: [ConfirmationService, DialogService],
  imports: [
    WarehouseCommonModule,
    NgbTooltipModule,
    FormsModule,
    NgSelectModule,
    ButtonModule,
    RippleModule,
    NgxSpinnerModule,
    ConfirmDialogModule,
    InputTextModule,
    AgGridAngular,
    IconField,
    InputIcon,
    Select,
    Toolbar,
  ],
})
export class CommandeStockEntryComponent implements OnInit {
  abstractCommande = input<AbstractCommande>(null);
  protected rowModelType: RowModelType = 'clientSide';
  protected pagination = true;
  /*   paginationPageSizeSelector = [5, 10, 20, 50, 100];
    paginationPageSize = 5; */
  protected readonly themeClass = 'ag-theme-quartz';
  protected readonly animateRows: boolean = true;
  protected AG_GRID_LOCALE_FR = AG_GRID_LOCALE_FR
  protected delivery?: IDelivery | null = null;
  protected orderLines: IOrderLine[] = [];
  protected receiptItems: IDeliveryItem[] = [];
  protected rowHeight = 46;
  protected selectedFilter = 'ALL';
  protected filtres: any[] = [];
  protected search?: string;
  protected isSaving = false;
  protected columnDefs: any[];
  protected defaultColDef: any;
  protected frameworkComponents: any;
  protected context: any;
  protected showLotBtn = true;
  protected disableActionBtn = false;
  protected showEditBtn = true;
  private ref?: DynamicDialogRef;
  private readonly tvaService = inject(TvaService);
  private readonly commandeService = inject(CommandeService);
  private readonly service = inject(DeliveryService);
  private readonly produitService = inject(ProduitService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly modalService = inject(NgbModal);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly configurationService = inject(ConfigurationService);
  private readonly router = inject(Router);
  private readonly dialogService = inject(DialogService);
  private readonly spinner = inject(NgxSpinnerService);

  private gridApi!: GridApi<IDeliveryItem>;
  private tvas: number[] = [];

  constructor() {
    this.filtres = [
      {label: "Prix d'achat differents", value: 'NOT_EQUAL'},
      {label: 'Prix de vente différent', value: 'PU_NOT_EQUAL'},
      {label: 'Code cip  à mettre à jour', value: 'PROVISOL_CIP'},
      {label: 'Tous', value: 'ALL'},
    ];
  }

  ngOnInit(): void {

    this.delivery = this.abstractCommande();
    if (this.delivery && this.delivery.id) {
      this.receiptItems = this.delivery.orderLines;
      this.enabledBtnValidate();
    } else {
      this.activatedRoute.data.subscribe({
        next: data => {
          const delivery = data.delivery;
          if (delivery.id) {
            this.delivery = delivery;
            this.receiptItems = delivery.receiptItems;
            this.enabledBtnValidate();
          }
        },
      });
    }


    this.findParamAddLot();
    this.tvaService.query().subscribe({
      next: (res: HttpResponse<ITva[]>) => {
        this.tvas = res.body.map(tva => tva.taux);
      },
      complete: () => {
        this.columnDefs = [
          {
            headerName: '#',
            flex: 0.2,
            valueGetter: (params: any) => params.node.rowIndex + 1,
          },

          {
            // editable: (params) => params.data.year == 2012
            headerName: 'Code cip',
            field: 'fournisseurProduitCip',
            sortable: true,
            flex: 0.3,
          },
          {
            headerName: 'Libellé',
            field: 'fournisseurProduitLibelle',
            sortable: true,
            flex: 0.8,
            //  valueGetter: 'data.firstName',
          },
          /*  {
             headerName: 'Stock',
             field: 'initStock',
             type: ['rightAligned', 'numericColumn'],
             valueFormatter: formatNumberToString,
             //  cellStyle: this.cellStyle,
             flex: 0.3,
           }, */
          {
            headerName: 'PA.M',
            field: 'costAmount',
            type: ['rightAligned', 'numericColumn'],
            editable: false,
            sortable: true,
            flex: 0.3,
            valueFormatter: formatNumberToString,
          },
          {
            headerName: 'PA',
            field: 'orderCostAmount',
            type: ['rightAligned', 'numericColumn'],
            editable: false,
            sortable: true,
            flex: 0.3,
            valueFormatter: formatNumberToString,
          },
          {
            headerName: 'PU.M',
            field: 'regularUnitPrice',
            type: ['rightAligned', 'numericColumn'],
            editable: false,
            sortable: false,
            flex: 0.3,
            valueFormatter: formatNumberToString,
          },
          {
            headerName: 'PU',
            field: 'orderUnitPrice',
            type: ['rightAligned', 'numericColumn'],
            editable: true,
            sortable: true,
            flex: 0.4,
            valueFormatter: formatNumberToString,
          },

          {
            headerName: 'Qté livrée',
            flex: 0.4,
            field: 'quantityReceivedTmp',
            editable: true,
            sortable: true,
            type: ['rightAligned', 'numericColumn'],
          },
          {
            headerName: 'Qté Ug',
            flex: 0.3,
            field: 'ugQuantity',
            editable: true,
            sortable: true,
            type: ['rightAligned', 'numericColumn'],
          },
          {
            headerName: 'Tva',
            cellEditor: 'agSelectCellEditor',
            field: 'tva',
            type: ['rightAligned'],
            editable: true,
            sortable: false,
            flex: 0.3,
            cellEditorParams: {
              min: 0,
              values: this.tvas,
            },
          },
          /*  {
              headerName: 'Date péremption',
              field: 'datePeremption',
              cellRenderer: (data: any) => {
                return data.value ? dayjs(data.value).format('MM/DD/YYYY') : '';
              },
              editable: true,
              sortable: false,
              flex: 0.5,
              cellEditor: 'agDateStringCellEditor',
              cellEditorParams: {
                min: DATE_FORMAT_ISO_DATE(new Date()),
              },
            },*/

          /*  {
             field: 'Status',
             cellRenderer: 'statusCellRenderer',
             flex: 0.2,
             hide: false,
             suppressToolPanel: false,
             cellStyle: this.statusCellStyle,
           }, */
          {
            field: ' ',
            cellRenderer: 'btnCellRenderer',
            flex: 0.3,
            hide: false,
            suppressToolPanel: false,
            cellStyle: this.btnCellStyle,
          },
        ];
      },
    });
    this.defaultColDef = {
      enableCellChangeFlash: true,
    };
    this.frameworkComponents = {
      btnCellRenderer: CommandeBtnComponent,
      statusCellRenderer: ReceiptStatusComponent,
    };
    this.context = {componentParent: this};
  }

  onSearch(event: any): void {
    this.gridApi.setGridOption('quickFilterText', event.target.value);
  }

  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }

  findParamAddLot(): void {
    const paramGesntionLot = this.configurationService.getParamByKey(Params.APP_GESTION_LOT);
    if (paramGesntionLot) {
      this.showLotBtn = Number(paramGesntionLot.value) === 0;
    }
  }

  statusCellStyle(): any {
    return {
      paddingTop: '10px',
      paddingLeft: '1px',
      paddingRight: '1px',
    };
  }

  cellClass(params: any): any {
    const deliveryItem = params.data as IDeliveryItem;
    if (deliveryItem.quantityReceived !== deliveryItem.quantityRequested) {
      const ecart = Math.abs(Number(deliveryItem.quantityRequested) - Number(deliveryItem.quantityReceived));
      if (ecart > 0) {
        if (Number(deliveryItem.quantityRequested) < Number(deliveryItem.quantityReceived)) {
          return {
            backgroundColor: '#FFC107',
            borderRight: 'solid 0.25px  rgb(150, 150, 200)',
            borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
          };
        } else {
          return {
            backgroundColor: 'lightgreen',
            borderRight: 'solid 0.25px  rgb(150, 150, 200)',
            borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
          };
        }
      }
    }
    const toBeUpdated = checkIfRomToBeUpdated(deliveryItem);

    let backgroundColor = '';
    if (toBeUpdated) {
      backgroundColor = '#b8daff';
      return {
        borderRight: 'solid 0.25px  rgb(184, 218, 255)',
        borderLeft: 'solid 0.25px  rgb(184, 218, 255)',
        backgroundColor,
      };
    } else {
      return {
        borderRight: 'solid 0.25px  rgb(150, 150, 200)',
        borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
      };
    }
  }

  cellStyle(params: any): any {
    const deliveryItem = params.data as IDeliveryItem;
    const toBeUpdated = checkIfRomToBeUpdated(deliveryItem);

    let backgroundColor = '';
    if (toBeUpdated) {
      backgroundColor = '#b8daff';
      return {
        borderRight: 'solid 0.25px  rgb(184, 218, 255)',
        borderLeft: 'solid 0.25px  rgb(184, 218, 255)',
        backgroundColor,
      };
    } else {
      return {
        borderRight: 'solid 0.25px  rgb(150, 150, 200)',
        borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
      };
    }
  }

  btnCellStyle(params: any): any {
    const toBeUpdated = false;
    let backgroundColor = '';
    const padding = {paddingLeft: 0, paddingRight: 0};
    if (toBeUpdated) {
      backgroundColor = '#b8daff';
      return {
        borderRight: 'solid 0.25px  rgb(184, 218, 255)',
        borderLeft: 'solid 0.25px  rgb(184, 218, 255)',
        textAlign: 'right',
        backgroundColor,
        ...padding,
      };
    } else {
      return {
        borderRight: 'solid 0.25px  rgb(150, 150, 200)',
        textAlign: 'right',
        borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
        ...padding,
      };
    }
  }

  setGap(params: any): number {
    const deliveryItem = params.data as IDeliveryItem;
    if (deliveryItem.quantityReceived !== deliveryItem.quantityRequested) {
      console.error('deliveryItem.quantityReceived', deliveryItem.quantityReceived);
      return deliveryItem.quantityRequested - deliveryItem.quantityReceived;
    }
    return 0;
  }

  stockOnHandcellStyle(params: any): any {
    if (params.data.updated) {
      return {backgroundColor: '#c6c6c6'};
    }
  }

  onFilterReceiptItems(): void {
    this.service.find(this.delivery.id).subscribe({
      next: res => {
        this.delivery = res.body;
        if (this.selectedFilter === 'PROVISOL_CIP') {
          this.receiptItems = this.delivery.orderLines.filter((item: IDeliveryItem) => item.fournisseurProduitCip.length === 0);
        } else if (this.selectedFilter === 'NOT_EQUAL') {
          this.receiptItems = this.delivery.orderLines.filter((item: IDeliveryItem) => item.orderCostAmount !== item.costAmount);
        } else if (this.selectedFilter === 'PU_NOT_EQUAL') {
          this.receiptItems = this.delivery.orderLines.filter((item: IDeliveryItem) => item.regularUnitPrice !== item.orderUnitPrice);
        } else {
          this.receiptItems = this.delivery.orderLines;
        }
      },
    });
  }

  orderLineTableColor(deliveryItem: IDeliveryItem): string {
    if (deliveryItem) {
      if (deliveryItem.costAmount !== deliveryItem.orderCostAmount) {
        return 'table-danger';
      } else if (deliveryItem.regularUnitPrice !== deliveryItem.orderUnitPrice) {
        return 'table-warning';
      }
    }
    return '';
  }

  onCellValueChanged(params: any): void {
    switch (params.column.colId) {
      case 'ugQuantity':
        this.onUpdatequantityUG(params);
        break;
      case 'quantityReceivedTmp':
        this.onUpdateQuantityReceived(params);
        break;
      case 'orderUnitPrice':
        this.onUpdateOrderUnitPrice(params);
        break;
      case 'orderCostAmount':
        this.onUpdateOrderCostAmount(params);
        break;
      case 'fournisseurProduitCip':
        this.onUpdateCip(params);
        break;
      case 'tva':
        this.onTvaUpdate(params);
        break;
      case 'datePeremption':
        this.onDatePeremptionUpdate(params);
        break;
      default:
        break;
    }
  }

  editLigneInfos(deliveryItem: IDeliveryItem): void {
    this.ref = this.dialogService.open(EditProduitComponent, {
      data: {deliveryItem, delivery: this.delivery},
      width: '70%',
      header: `EDITION DU PRODUIT ${deliveryItem.fournisseurProduitLibelle} [${deliveryItem.fournisseurProduitCip}]`,
    });

    this.ref.onClose.subscribe(() => this.onFilterReceiptItems());
  }

  /*
   utiliser le component pour ajouter un lot
   */
  onAddLot(deliveryItem: IDeliveryItem): void {
    const quantityReceived = deliveryItem.quantityReceived || deliveryItem.quantityRequested;
    if (quantityReceived > 1 || deliveryItem.lots.length > 0) {
      this.ref = this.dialogService.open(ListLotComponent, {
        data: {deliveryItem},
        width: '60%',
        header: `GESTION DE LOTS DE LA LIGNE ${deliveryItem.fournisseurProduitLibelle} [${deliveryItem.fournisseurProduitCip}]`,
      });
    } else {
      this.ref = this.dialogService.open(FormLotComponent, {
        data: {entity: null, deliveryItem},
        width: '40%',
        header: 'Ajout de lot',
      });
    }
    this.ref.onClose.subscribe(() => this.onFilterReceiptItems());
  }

  onUpdateCip(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    this.service.updateCip(deliveryItem).subscribe({
      next: () => {
        deliveryItem.provisionalCode = false;
        this.reloadDelivery();
      },
      error: err => this.onEditCellError(err),
    });
  }

  onUpdateOrderCostAmount(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const orderCostAmount = Number(deliveryItem.orderCostAmount);
    if (Number(orderCostAmount) > 0) {
      this.subscribeOnEditCellResponse(this.service.updateOrderCostAmount(deliveryItem));
    } else {
      this.onFilterReceiptItems();
    }
  }

  onUpdateOrderUnitPrice(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const orderUnitPrice = Number(deliveryItem.orderUnitPrice);
    if (orderUnitPrice > 0) {
      this.subscribeOnEditCellResponse(this.service.updateOrderUnitPriceOnStockEntry(deliveryItem));
    } else {
      this.reloadDelivery();
    }
  }

  onUpdatequantityUG(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const newQuantityUG = Number(deliveryItem.ugQuantity);
    if (newQuantityUG > -1) {
      deliveryItem.quantityUG = newQuantityUG;
      deliveryItem.ugQuantity = newQuantityUG;
      this.subscribeOnEditCellResponse(this.service.updateQuantityUG(deliveryItem));
    } else {
      this.reloadDelivery();
    }
  }

  onUpdateQuantityReceived(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const newQuantityReceived = Number(deliveryItem.quantityReceivedTmp);
    if (newQuantityReceived >= 0) {
      deliveryItem.quantityReceived = newQuantityReceived;
      this.subscribeOnEditCellResponse(this.service.updateQuantityReceived(deliveryItem));
    } else {
      this.reloadDelivery();
    }
  }

  onTvaUpdate(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const newTva = Number(deliveryItem.tva);
    if (newTva >= 0) {
      deliveryItem.tva = newTva;
      this.subscribeOnEditCellResponse(this.service.updateTva(deliveryItem));
    } else {
      this.reloadDelivery();
    }
  }

  onDatePeremptionUpdate(params: any): void {
    const deliveryItem = params.data as IDeliveryItem;
    const newDatePeremption = dayjs(deliveryItem.datePeremption);
    if (newDatePeremption.isValid()) {
      deliveryItem.datePeremptionTmp = newDatePeremption.format('YYYY-MM-DD');
      this.subscribeOnEditCellResponse(this.service.updateDatePeremption(deliveryItem));
    } else {
      this.reloadDelivery();
    }
  }

  previousState(): void {
    window.history.back();
  }

  enabledBtnValidate(): void {
    this.disableActionBtn = false;
  }


  onFinalize(): void {
    this.showsPinner();
    this.service.finalizeSaisieEntreeStock(this.delivery).subscribe({
      next: () => {
        this.hidePinner();
        this.confirmPrintTicket(this.delivery);
      },
      error: error => {
        this.onCommonError(error);
        this.hidePinner();
      },
    });
  }

  confirmPrintTicket(delivery: IDelivery): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous imprimer les étiquettes ?',
      header: 'IMPRESSION',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.printEtiquette(delivery),
      reject: () => {
        this.previousState();
      },
      key: 'printTicket',
    });
  }

  printEtiquette(delivery: IDelivery): void {
    this.ref = this.dialogService.open(EtiquetteComponent, {
      data: {entity: delivery},
      width: '40%',
      header: `IMPRIMER LES ETIQUETTES DU BON DE LIVRAISON [ ${delivery.receiptRefernce} ] `,
    });
    this.ref.onDestroy.subscribe(() => {
      this.delivery = null;
      this.previousState();
    });
    this.ref.onClose.subscribe(() => {
      this.delivery = null;
      this.previousState();
    });
  }

  onSaveOrderLineSuccess(): void {
    this.reloadDelivery();
  }

  onSaveError(): void {
    this.isSaving = false;
  }

  onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      this.openInfoDialog(error, 'alert alert-danger');
    }
  }

  openInfoDialog(error: any, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = error?.error?.message || 'Une erreur est survenue';
    modalRef.componentInstance.infoClass = infoClass;
  }

  showsPinner(): void {
    this.spinner.show();
  }

  hidePinner(): void {
    this.spinner.hide();
  }


  reloadDelivery(): void {
    this.onFilterReceiptItems();
  }

  onEditDelivery(): void {
    this.ref = this.dialogService.open(DeliveryModalComponent, {
      data: {commande: this.delivery},
      header: 'MODIFICATION DU BON DE LIVRAISON',
      width: '40%',
    });
    this.ref.onClose.subscribe((delivery: IDelivery) => {
      if (delivery) {
        this.delivery = delivery;
      }
    });
  }

  private subscribeOnEditCellResponse(result: Observable<{}>): void {
    result.subscribe({
      next: () => this.reloadDelivery(), // see how to commit row insted reload
      error: err => this.onEditCellError(err),
    });
  }

  private onEditCellError(error: any): void {
    this.onCommonError(error);
    this.onFilterReceiptItems(); // se how to reset ROW
  }
}
