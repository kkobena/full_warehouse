import { Component, OnInit, ViewChild } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { CommandeService } from './commande.service';
import { OrderLineService } from '../order-line/order-line.service';
import { ProduitService } from '../produit/produit.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { ICommande } from '../../shared/model/commande.model';
import { IOrderLine } from '../../shared/model/order-line.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { AgGridAngular } from 'ag-grid-angular';
import { CommandeBtnComponent } from './btn/commande-btn.component';
import { ConfigurationService } from '../../shared/configuration.service';
import { GridApi, GridReadyEvent } from 'ag-grid-community';
import { checkIfAlineToBeUpdated, formatNumberToString } from '../../shared/util/warehouse-util';
import { NgxSpinnerService } from 'ngx-spinner';
import moment from 'moment';
import { DATE_FORMAT } from '../../shared/constants/input.constants';
import { Params } from '../../shared/model/enumerations/params.model';
import { FormLotComponent } from './lot/form-lot.component';
import { ListLotComponent } from './lot/list/list-lot.component';

@Component({
  selector: 'jhi-commande-stock-entry',
  styles: [``],
  templateUrl: './commande-stock-entry.component.html',
  providers: [ConfirmationService, DialogService],
})
export class CommandeStockEntryComponent implements OnInit {
  commande?: ICommande | null = null;
  orderLines: IOrderLine[] = [];
  receiptRefernce: string | null = null;
  sequenceBon: string | null = null;
  receiptAmount: number | null = null;
  taxAmount: number | null = null;
  receiptDate = new Date();
  maxDate = new Date();
  selectedFilter = 'ALL';
  filtres: any[] = [];
  search?: string;
  isSaving = false;
  windowWidth = 0;
  columnDefs: any[];
  @ViewChild('orderLineGrid') productGrid!: AgGridAngular;
  defaultColDef: any;
  frameworkComponents: any;
  context: any;
  showLotBtn = true;
  disableActionBtn = true;
  ref?: DynamicDialogRef;
  private gridApi!: GridApi;

  constructor(
    protected commandeService: CommandeService,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    private confirmationService: ConfirmationService,
    private errorService: ErrorService,
    protected configurationService: ConfigurationService,
    private spinner: NgxSpinnerService,
    protected router: Router,
    private dialogService: DialogService
  ) {
    this.filtres = [
      { label: "Prix d'achat differents", value: 'NOT_EQUAL' },
      { label: 'Code cip  à mettre à jour', value: 'PROVISOL_CIP' },
      { label: 'Tous', value: 'ALL' },
    ];
    this.columnDefs = [
      {
        headerName: '#',
        flex: 0.1,
        cellStyle: this.cellStyle,
        valueGetter: (params: any) => params.node.rowIndex + 1,
      },
      {
        headerName: 'Code cip',
        field: 'produitCip',
        sortable: true,
        cellStyle: this.cellStyle,
        filter: 'agTextColumnFilter',
        flex: 0.5,
      },
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        //  filter: 'agTextColumnFilter',
        flex: 1.2,
        cellStyle: this.cellStyle,
        //  valueGetter: 'data.firstName',
        //editable: (params) => params.data.year == 2012
      },

      {
        headerName: 'PA.M',
        field: 'costAmount',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: formatNumberToString,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PA',
        field: 'orderCostAmount',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.3,
        valueFormatter: formatNumberToString,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PU.M',
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: formatNumberToString,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PU',
        field: 'orderUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        sortable: true,
        flex: 0.4,
        valueFormatter: formatNumberToString,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'Qté cmd',
        field: 'quantityRequested',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.3,
        valueFormatter: formatNumberToString,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'Qté livréé',
        flex: 0.4,
        field: 'quantityReceivedTmp',
        editable: true,
        sortable: true,
        type: ['rightAligned', 'numericColumn'],
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'Qté Ug',
        flex: 0.4,
        field: 'ugQuantity',
        editable: true,
        sortable: true,
        type: ['rightAligned', 'numericColumn'],
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'Ecart',
        sortable: true,
        flex: 0.3,
        type: ['rightAligned', 'numericColumn'],
        valueGetter: this.setGap,
        cellStyle: this.cellClass,
      },
      {
        field: ' ',
        cellRenderer: 'btnCellRenderer',
        flex: 0.4,
        hide: false,
        suppressToolPanel: false,
        cellStyle: this.btnCellStyle,
      },
    ];
    this.defaultColDef = {
      // flex: 1,
      // cellClass: 'align-right',
      enableCellChangeFlash: true,
      //   resizable: true,
      /* valueFormatter: function (params) {
         return formatNumber(params.value);
       },*/
    };
    this.frameworkComponents = {
      btnCellRenderer: CommandeBtnComponent,
    };
    this.context = { componentParent: this };
  }

  ngOnInit(): void {
    this.maxDate = new Date();
    this.activatedRoute.data.subscribe({
      next: data => {
        const commande = data.commande;
        if (commande.id) {
          this.commande = commande;
          this.orderLines = commande.orderLines;
          this.receiptRefernce = commande.receiptRefernce;
          this.sequenceBon = commande.sequenceBon;
          this.receiptDate = commande.receiptDate ? new Date(commande.receiptDate.format(DATE_FORMAT)) : new Date();
          this.receiptAmount = commande.receiptAmount;
          this.taxAmount = commande.taxAmount > 0 ? commande.taxAmount : null;
          this.enabledBtnValidate();
        }
      },
    });
    this.findParamAddLot();
  }

  onSearch(event: any): void {
    this.gridApi.setQuickFilter(event.target.value);
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

  cellClass(params: any): any {
    const orderLine = params.data as IOrderLine;
    if (orderLine.quantityReceived) {
      const ecart = Math.abs(Number(orderLine.quantityRequested) - Number(orderLine.quantityReceived));
      if (ecart > 0) {
        if (Number(orderLine.quantityRequested) < Number(orderLine.quantityReceived)) {
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
    const toBeUpdated = checkIfAlineToBeUpdated(orderLine);

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
    const orderLine = params.data as IOrderLine;
    const toBeUpdated = checkIfAlineToBeUpdated(orderLine);

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
    //  const orderLine = params.data;
    const toBeUpdated = false; // orderLine.provisionalCode || !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) || !(orderLine.orderCostAmount === orderLine.costAmount);
    let backgroundColor = '';
    if (toBeUpdated) {
      backgroundColor = '#b8daff';
      return {
        borderRight: 'solid 0.25px  rgb(184, 218, 255)',
        borderLeft: 'solid 0.25px  rgb(184, 218, 255)',
        textAlign: 'right',
        backgroundColor,
      };
    } else {
      return {
        borderRight: 'solid 0.25px  rgb(150, 150, 200)',
        textAlign: 'right',
        borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
      };
    }
  }

  setGap(params: any): number {
    const orderLine = params.data as IOrderLine;
    if (orderLine.quantityReceived) {
      return orderLine.quantityRequested - orderLine.quantityReceived;
    }
    return 0;
  }

  stockOnHandcellStyle(params: any): any {
    if (params.data.updated) {
      return { backgroundColor: '#c6c6c6' };
    }
    return;
  }

  onFilterCommandeLines(): void {
    const query = {
      commandeId: this.commande?.id,
      search: this.search,
      filterCommaneEnCours: this.selectedFilter,
      size: 99999,
      orderBy: 'PRODUIT_LIBELLE',
    };
    this.commandeService.filterCommandeLines(query).subscribe(res => {
      this.orderLines = res.body!;
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

  onCellValueChanged(params: any): void {
    // console.error(params.newValue, params.oldValue, params.value, params.column.colId, params.data);
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
      case 'produitCip':
        this.onUpdateCip(params.data);
        break;
      default:
        break;
    }
  }

  editLigneInfos(orderLine: IOrderLine): void {
    this.gotoProduitPageEdit(orderLine.produitId);
  }

  onAddLot(orderLine: IOrderLine): void {
    const quantityReceived = orderLine.quantityReceived || orderLine.quantityRequested;
    if (quantityReceived > 1) {
      this.ref = this.dialogService.open(ListLotComponent, {
        data: { orderLine, commandeId: this.commande.id },
        width: '60%',
        header: `GESTION DE LOTS DE LA LIGNE ${orderLine.produitLibelle} [${orderLine.produitCodeEan}]`,
      });
    } else {
      this.ref = this.dialogService.open(FormLotComponent, {
        data: { entity: null, orderLine, commandeId: this.commande.id },
        width: '40%',
        header: 'Ajout de lot',
      });
    }
    this.ref.onClose.subscribe(() => this.reloadCommande(this.commande));
  }

  onUpdateCip(data: any): void {
    const orderLine = data as IOrderLine;
    this.commandeService.updateCip(orderLine).subscribe(() => {
      orderLine.provisionalCode = false;
    });
  }

  onUpdateOrderCostAmount(params: any): void {
    const orderLine = params.data as IOrderLine;
    const orderCostAmount = Number(orderLine.orderCostAmount);
    if (Number(orderCostAmount) > 0) {
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderCostAmount(orderLine));
    } else {
      this.reloadCommande(this.commande);
    }
  }

  onUpdateOrderUnitPrice(params: any): void {
    const orderLine = params.data as IOrderLine;
    const orderUnitPrice = Number(orderLine.orderUnitPrice);
    if (this.commande && orderUnitPrice > 0) {
      this.subscribeOnEditCellResponse(this.commandeService.updateOrderUnitPriceOnStockEntry(orderLine));
    } else {
      this.reloadCommande(this.commande);
    }
  }

  onUpdatequantityUG(params: any): void {
    const orderLine = params.data as IOrderLine;
    const newQuantityUG = Number(orderLine.ugQuantity);
    if (newQuantityUG > 0) {
      orderLine.quantityUG = newQuantityUG;
      orderLine.ugQuantity = newQuantityUG;
      this.subscribeOnEditCellResponse(this.commandeService.updateQuantityUG(orderLine));
    } else {
      this.reloadCommande(this.commande);
    }
  }

  onUpdateQuantityReceived(params: any): void {
    const orderLine = params.data as IOrderLine;
    const newQuantityReceived = Number(orderLine.quantityReceivedTmp);
    if (newQuantityReceived >= 0) {
      orderLine.quantityReceived = newQuantityReceived;
      this.subscribeOnEditCellResponse(this.commandeService.updateQuantityReceived(orderLine));
    } else {
      this.reloadCommande(this.commande);
    }
  }

  previousState(): void {
    window.history.back();
  }

  enabledBtnValidate(): void {
    this.disableActionBtn =
      !this.taxAmount ||
      this.taxAmount < 0 ||
      !this.receiptAmount ||
      this.receiptAmount <= 0 ||
      this.receiptAmount != this.commande?.grossAmount ||
      !this.receiptRefernce ||
      !this.receiptDate;
  }

  onSave(): void {
    this.showsPinner();

    this.buildCommande();
    this.commandeService.sauvegarderSaisieEntreeStock(this.commande).subscribe({
      next: res => {
        this.hidePinner();
        this.reloadCommande(res.body);
      },
      error: error => {
        this.onCommonError(error);
        this.hidePinner();
      },
    });
  }

  onFinalize(): void {
    this.showsPinner();
    this.buildCommande();
    this.commandeService.finalizeSaisieEntreeStock(this.commande).subscribe({
      next: () => {
        this.hidePinner();
        this.commande = null;
        this.previousState();
      },
      error: error => {
        this.onCommonError(error);
        this.hidePinner();
      },
    });
  }

  subscribeToSaveOrderLineResponse(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe({
      next: res => this.onSaveOrderLineSuccess(res.body!),
      error: err => this.onCommonError(err),
    });
  }

  onSaveOrderLineSuccess(commande: ICommande): void {
    this.reloadCommande(commande);
  }

  onSaveError(): void {
    this.isSaving = false;
  }

  onCommonError(error: any): void {
    if (error.error && error.error.status === 500) {
      this.openInfoDialog('Erreur applicative', 'alert alert-danger');
    } else {
      if (error?.error?.errorKey) {
        this.errorService.getErrorMessageTranslation(error?.error?.errorKey).subscribe({
          next: translatedErrorMessage => {
            this.openInfoDialog(translatedErrorMessage, 'alert alert-danger');
          },
          error: () => this.openInfoDialog(error, 'alert alert-danger'),
        });
      } else {
        this.openInfoDialog(error, 'alert alert-danger');
      }
    }
  }

  openInfoDialog(error: any, infoClass: string): void {
    console.error(error, 'deux');
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = error?.error?.title || 'Une erreur est survenue';
    modalRef.componentInstance.infoClass = infoClass;
  }

  showsPinner(): void {
    this.spinner.show();
  }

  hidePinner(): void {
    this.spinner.hide();
  }

  reloadCommande(commande: ICommande): void {
    if (commande) {
      this.commandeService.findSaisieEntreeStock(commande.id!).subscribe(res => {
        this.commande = res.body;
        this.orderLines = this.commande?.orderLines!;
      });
    }
  }

  buildCommande(): void {
    this.commande.receiptAmount = this.receiptAmount;
    this.commande.taxAmount = this.taxAmount;
    this.commande.sequenceBon = this.sequenceBon;
    this.commande.receiptDate = moment(this.receiptDate.toISOString());
    this.commande.receiptRefernce = this.receiptRefernce;
  }

  gotoProduitPageEdit(id: number): void {
    this.router.navigate(['/produit', id, 'edit'], { relativeTo: this.activatedRoute });
  }

  private subscribeOnEditCellResponse(result: Observable<{}>): void {
    result.subscribe({
      next: () => this.reloadCommande(this.commande),
      error: err => this.onEditCellError(err),
    });
  }

  private onEditCellError(error: any): void {
    this.onCommonError(error);
    this.reloadCommande(this.commande);
  }
}
