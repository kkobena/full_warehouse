import { Component, OnInit, ViewChild } from '@angular/core';
import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { CommandeService } from './commande.service';
import { OrderLineService } from '../order-line/order-line.service';
import { ProduitService } from '../produit/produit.service';
import { ActivatedRoute } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FournisseurService } from '../fournisseur/fournisseur.service';
import { ErrorService } from '../../shared/error.service';
import { ICommande } from '../../shared/model/commande.model';
import { IOrderLine } from '../../shared/model/order-line.model';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModules } from '@ag-grid-community/all-modules';
import { CommandeBtnComponent } from './btn/commande-btn.component';
import { ConfigurationService } from '../../shared/configuration.service';
import { GridApi, GridReadyEvent } from 'ag-grid-community';

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
  selectedFilter = 'ALL';
  filtres: any[] = [];
  search?: string;
  isSaving = false;
  windowWidth = 0;
  columnDefs: any[];
  @ViewChild('orderLineGrid') productGrid!: AgGridAngular;
  public modules: any[] = AllCommunityModules;
  defaultColDef: any;
  frameworkComponents: any;
  context: any;
  showLotBtn = true;
  private gridApi!: GridApi;
  constructor(
    protected commandeService: CommandeService,
    protected orderLineService: OrderLineService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    protected fournisseurService: FournisseurService,
    private confirmationService: ConfirmationService,
    private errorService: ErrorService,
    protected configurationService: ConfigurationService
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
        flex: 0.4,
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
        headerName: 'PA',
        field: 'orderCostAmount',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: this.formatNumber,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PA MACHINE',
        field: 'costAmount',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: this.formatNumber,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PU',
        field: 'orderUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: this.formatNumber,
        cellStyle: this.cellStyle,
      },
      {
        headerName: 'PU machine',
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.4,
        valueFormatter: this.formatNumber,
        cellStyle: this.cellStyle,
      },

      {
        headerName: 'Qté cmd',
        field: 'quantityRequested',
        type: ['rightAligned', 'numericColumn'],
        editable: false,
        sortable: true,
        flex: 0.3,
        valueFormatter: this.formatNumber,
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
        field: 'quantityUG',
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
    this.activatedRoute.data.subscribe(({ commande }) => {
      if (commande.id) {
        this.commande = commande;
        this.orderLines = commande.orderLines;
        this.receiptRefernce = commande.receiptRefernce;
        this.sequenceBon = commande.sequenceBon;
        this.receiptDate = commande.receiptDate;
      }
      this.findParamAddLot();
    });
  }
  onSearch(event: any): void {
    console.error(event.target.value);
    this.gridApi.setQuickFilter(event.target.value);
  }
  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }
  findParamAddLot(): void {
    this.configurationService.find('GESTION_LOT').subscribe(res => {
      if (res.body) {
        this.showLotBtn = Number(res.body.value) === 0;
      }
    });
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  cellClass(params: any): any {
    const orderLine = params.data;
    if (orderLine.quantityReceived) {
      const ecart = Math.abs(Number(orderLine.quantityRequested) - Number(orderLine.quantityReceived));
      if (ecart > 0) {
        return {
          backgroundColor: 'lightgreen',
          borderRight: 'solid 0.25px  rgb(150, 150, 200)',
          borderLeft: 'solid 0.25px  rgb(150, 150, 200)',
        };
      }
    }
    const toBeUpdated =
      orderLine.provisionalCode ||
      !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) ||
      !(orderLine.orderCostAmount === orderLine.costAmount);

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

  checkIfAlineToBeUpdated(orderLine: IOrderLine): boolean {
    console.error(orderLine);
    return (
      orderLine.provisionalCode ||
      !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) ||
      !(orderLine.orderCostAmount === orderLine.costAmount)
    );
  }

  cellStyle(params: any): any {
    const orderLine = params.data;
    const toBeUpdated =
      orderLine.provisionalCode ||
      !(orderLine.regularUnitPrice === orderLine.orderUnitPrice) ||
      !(orderLine.orderCostAmount === orderLine.costAmount);

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
    if (params.data.updated) {
      return params.data.quantityRequested - params.data.quantityReceived;
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
    console.error(params.newValue, params.oldValue, params.value);
  }

  editLigneInfos(orderLine: IOrderLine): void {
    console.error(orderLine);
  }

  onAddLot(orderLine: IOrderLine): void {}

  onUpdateCip(orderLine: IOrderLine, event: any): void {
    const cip = event.target.value;
    if (this.commande && cip !== '') {
      orderLine.produitCip = cip;
      this.commandeService.updateCip(orderLine).subscribe(() => {
        orderLine.provisionalCode = false;
      });
    }
  }

  onUpdateOrderCostAmount(orderLine: IOrderLine, event: any): void {
    const newOrderCostAmount = Number(event.target.value);
    if (this.commande && newOrderCostAmount > 0) {
      orderLine.orderCostAmount = newOrderCostAmount;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderCostAmount(orderLine));
    }
  }

  onUpdateOrderUnitPrice(orderLine: IOrderLine, event: any): void {
    const newOrderUnitPrice = Number(event.target.value);
    if (this.commande && newOrderUnitPrice > 0) {
      orderLine.orderUnitPrice = newOrderUnitPrice;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateOrderUnitPrice(orderLine));
    }
  }

  onUpdateQuantityReceived(orderLine: IOrderLine, event: any): void {
    const newQuantityRequested = Number(event.target.value);
    if (this.commande && newQuantityRequested > 0) {
      orderLine.quantityReceivedTmp = newQuantityRequested;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateQuantityReceived(orderLine));
    }
  }

  previousState(): void {
    window.history.back();
  }

  onUpdatequantityUG(orderLine: IOrderLine, event: any): void {
    const newQuantityUG = Number(event.target.value);
    if (this.commande && newQuantityUG > 0) {
      orderLine.quantityUG = newQuantityUG;
      this.subscribeToSaveOrderLineResponse(this.commandeService.updateQuantityUG(orderLine));
    }
  }

  protected subscribeToSaveOrderLineResponse(result: Observable<HttpResponse<ICommande>>): void {
    result.subscribe(
      res => this.onSaveOrderLineSuccess(res.body!),
      err => this.onCommonError(err)
    );
  }

  protected onSaveOrderLineSuccess(commande: ICommande): void {
    if (commande) {
      this.commandeService.find(commande.id!).subscribe(res => {
        this.commande = res.body;
        this.orderLines = this.commande?.orderLines!;
      });
    }
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
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
}
