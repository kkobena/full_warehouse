import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { ISales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer/customer.service';
import { IProduit } from 'app/shared/model/produit.model';
import { ProduitService } from '../produit/produit.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PackDialogueComponent } from './pack-dialogue.component';
import { AlertInfoComponent } from 'app/shared/alert/alert-info.component';
import { SalesLineService } from '../sales-line/sales-line.service';
import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { saveAs } from 'file-saver';
import { BtnRemoveComponent } from './btn-remove/btn-remove.component';

type SelectableEntity = ICustomer | IProduit;

@Component({
  selector: 'jhi-sales-update',
  styles: [
    `
      .table tr:hover {
        cursor: pointer;
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

      .card-body {
        padding: 0;
      }

      .card-header {
        padding: 0.5rem 0.25rem;
      }

      .list-group {
        border-radius: 0;
      }

      .produit-card {
        padding-left: 5px;
        padding-right: 5px;
        padding-top: 5px;
        padding-bottom: 5px;
        /* cursor: pointer;*/
      }

      .bg-info,
      .bg-secondary,
      .bg-primary {
        color: #fff;
      }

      .master {
        padding: 14px 12px;
        border-radius: 12px;
        box-shadow: 0 4px 8px rgb(0 0 0 / 16%);
        justify-content: space-between;
      }

      .produit-row {
        max-height: 600px;
        overflow-y: scroll;
      }

      .list-group-item {
        color: #777 !important;
        padding: 0.5rem 0.5rem;
        cursor: pointer;
      }
    `,
  ],
  templateUrl: './sales-update.component.html',
})
export class SalesUpdateComponent implements OnInit {
  isSaving = false;
  customers: ICustomer[] = [];
  produits?: IProduit[] = [];
  searchValue?: string;
  imagesPath!: string;
  customerSelected!: ICustomer | null;
  selectedRowIndex?: number;
  produitsSelected?: IProduit[] = [];
  sale?: ISales | null;
  columnDefs: any[];
  rowData: any = [];
  base64!: string;
  defaultColDef: any;
  frameworkComponents: any;
  context: any;
  constructor(
    protected salesService: SalesService,
    protected customerService: CustomerService,
    protected produitService: ProduitService,
    protected activatedRoute: ActivatedRoute,
    protected modalService: NgbModal,
    protected saleItemService: SalesLineService
  ) {
    this.imagesPath = 'data:image/';
    this.base64 = ';base64,';
    this.selectedRowIndex = 0;
    this.searchValue = '';
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 2,
      },
      {
        headerName: 'Quantité',
        width: 100,
        field: 'quantitySold',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },

      {
        headerName: 'Prix unitaire',
        width: 150,
        field: 'regularUnitPrice',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        cellEditorParams: {
          color: 'red',
        },
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Montant',
        width: 100,
        field: 'salesAmount',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: this.formatNumber,
      },
      {
        field: ' ',
        cellRenderer: 'btnCellRenderer',
        width: 50,
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
      btnCellRenderer: BtnRemoveComponent,
    };
    this.context = { componentParent: this };
  }

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ sales }) => {
      if (!sales.id) {
        const today = moment().startOf('day');
        sales.createdAt = today;
        sales.updatedAt = today;
      }

      this.customerService.queryVente().subscribe((res: HttpResponse<ICustomer[]>) => (this.customers = res.body || []));
      this.loadProduits();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(this.salesService.save(this.sale!));
  }

  saveAntPrint(): void {
    this.isSaving = true;
    this.subscribeToFinalyseResponse(this.salesService.save(this.sale!));
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onSaveSuccess(res.body),
      () => this.onSaveError()
    );
  }

  protected onSaveSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.sale = sale!;
    this.rowData = this.sale.salesLines;
  }

  protected onSaveError(): void {
    this.isSaving = false;
    const message = 'Une erruer est survenue';
    this.openInfoDialog(message, 'alert alert-danger');
  }

  trackId(index: number, item: IProduit): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  trackById(index: number, item: SelectableEntity): any {
    return item.id;
  }

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
    this.loadProduits();
  }

  loadProduits(): void {
    this.produitService
      .query({
        page: 0,
        size: 9999,
        withdetail: true,
        search: this.searchValue,
      })
      .subscribe((res: HttpResponse<any[]>) => this.onProduitSuccess(res.body));
  }

  protected onProduitSuccess(data: IProduit[] | null): void {
    this.produits = data || [];
  }

  clickRow(item: IProduit): void {
    this.selectedRowIndex = item.id;
  }

  onAddPack(item: IProduit): void {
    if (this.customerSelected === null || this.customerSelected === undefined) {
      const message = ' Veuillez choisir le client';
      this.openInfoDialog(message, 'alert alert-info');
    } else {
      const modalRef = this.modalService.open(PackDialogueComponent, {
        backdrop: 'static',
        centered: true,
        size: '50%',
      });
      modalRef.componentInstance.produit = item;
      modalRef.componentInstance.sale = this.sale;
      modalRef.componentInstance.customer = this.customerSelected;
      modalRef.result.then(res => {
        this.sale = res;
        this.subscribeToSaveResponse(this.salesService.find(res.id));
      });
    }
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  onCellValueChanged(params: any): void {
    if (Number(params.data.quantitySold) > params.data.quantityStock) {
      this.openInfoDialog('La quantité saisie est supérieure à la quantité stock du produit', 'alert alert-danger');
    } else {
      this.subscribeToSaveLineResponse(this.saleItemService.update(this.createFromForm(params.data)));
    }
  }

  protected subscribeToSaveLineResponse(result: Observable<HttpResponse<ISalesLine>>): void {
    result.subscribe(
      (res: HttpResponse<ISalesLine>) => this.subscribeToSaveResponse(this.salesService.find(res.body?.saleId!)),
      () => this.onSaveError()
    );
  }

  openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, { backdrop: 'static', centered: true });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  private createFromForm(salesLine: ISalesLine): ISalesLine {
    return {
      ...new SalesLine(),
      produitId: salesLine.produit?.id,
      regularUnitPrice: salesLine.regularUnitPrice,
      id: salesLine.id,
      saleId: salesLine.saleId,
      quantitySold: salesLine.quantitySold,
    };
  }

  protected onFinalyseSuccess(sale: ISales | null): void {
    this.isSaving = false;
    this.print(sale);
  }

  protected subscribeToFinalyseResponse(result: Observable<HttpResponse<ISales>>): void {
    result.subscribe(
      (res: HttpResponse<ISales>) => this.onFinalyseSuccess(res.body),
      () => this.onSaveError()
    );
  }

  print(sale: ISales | null): void {
    if (sale !== null && sale !== undefined) {
      this.salesService.print(sale.id!).subscribe(blod => saveAs(blod));
      this.sale = null;
      this.loadProduits();
      this.customerSelected = null;
    }
  }

  removeLine(data: any): void {
    this.saleItemService.delete(data.id).subscribe(() => {
      this.refresh();
    });
  }

  protected refresh(): void {
    this.subscribeToSaveResponse(this.salesService.find(this.sale?.id!));
  }
}
