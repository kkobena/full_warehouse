import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { GROUPING_BY, IStoreInventory, ItemsCountRecord, StoreInventoryExportRecord } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';

import { StoreInventoryLineService } from '../store-inventory-line/store-inventory-line.service';
import { APPEND_TO, NOT_FOUND, PRODUIT_COMBO_MIN_LENGTH } from '../../shared/constants/pagination.constants';
import { RayonService } from '../rayon/rayon.service';
import { IRayon } from '../../shared/model/rayon.model';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Storage } from '../storage/storage.model';
import { StorageService } from '../storage/storage.service';
import { IStoreInventoryLine } from '../../shared/model/store-inventory-line.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ErrorService } from '../../shared/error.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { formatNumberToString } from '../../shared/util/warehouse-util';
import { AlertInfoComponent } from '../../shared/alert/alert-info.component';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ClientSideRowModelModule, GridApi, GridReadyEvent, ModuleRegistry, themeAlpine } from 'ag-grid-community';
import { InputTextModule } from 'primeng/inputtext';
import { Authority } from '../../shared/constants/authority.constants';
import { HasAuthorityService } from '../sales/service/has-authority.service';
import { acceptButtonProps, rejectButtonProps } from '../../shared/util/modal-button-props';
import { Select } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';

ModuleRegistry.registerModules([AllCommunityModule, ClientSideRowModelModule]);

//provideGlobalGridOptions({ theme: themeQuartz });

@Component({
  selector: 'jhi-store-inventory-update',
  templateUrl: './store-inventory-update.component.html',
  providers: [ConfirmationService, DialogService, MessageService],
  imports: [
    WarehouseCommonModule,
    FormsModule,
    RouterModule,
    DividerModule,
    NgxSpinnerModule,
    DropdownModule,
    AutoCompleteModule,
    TableModule,
    ButtonModule,
    RippleModule,
    ToastModule,
    ConfirmDialogModule,
    AgGridAngular,
    InputTextModule,
    Select,
    Toolbar,
    IconField,
    InputIcon,
  ],
})
export class StoreInventoryUpdateComponent implements OnInit {
  @ViewChild('itemsGrid') productGrid!: AgGridAngular;
  hasAuthorityService = inject(HasAuthorityService);
  protected isSaving = false;
  protected defaultColDef: any;
  protected context: any;
  protected storages: Storage[];
  protected rayons: IRayon[] = [];
  protected storeInventoryLines?: IStoreInventoryLine[] = [];
  protected selectedRayon: IRayon | null;
  protected selectedStorage: Storage | null;
  protected columnDefs: any[];
  protected event: any;
  protected totalItems = 0;
  protected searchValue?: string;
  protected storeInventory?: IStoreInventory;
  protected readonly APPEND_TO = APPEND_TO;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly NOT_FOUND = NOT_FOUND;
  protected readonly filtres: any = [
    { name: 'UPDATED', label: 'Mise à jour' },
    { name: 'NOT_UPDATED', label: 'Pas mise à jour' },
    { name: 'GAP', label: 'Ecart' },
    { name: 'GAP_POSITIF', label: 'Ecart positif' },
    { name: 'GAP_NEGATIF', label: 'Ecart négatif' },
  ];
  protected selectedfiltres: any | null;
  protected page = 0;
  protected loading!: boolean;
  protected itemsPerPage = 15;
  protected gridApi!: GridApi;
  protected search?: string;
  protected ngbPaginationPage = 1;
  protected readonly showFilterCombox: boolean = true;
  protected readonly theme = themeAlpine.withParams({
    /*  /!* Low spacing = very compact *!/ spacing: 2,
     /!* Changes the color of the grid text *!/
     foregroundColor: 'rgb(14, 68, 145)',
     /!* Changes the color of the grid background *!/
     backgroundColor: 'rgb(241, 247, 255)',
     /!* Changes the header color of the top row *!/
     headerBackgroundColor: 'rgb(228, 237, 250)',
     /!* Changes the hover color of the row*!/
     rowHoverColor: 'rgb(216, 226, 255)', */
  });
  protected storeInventoryLineService = inject(StoreInventoryLineService);
  protected activatedRoute = inject(ActivatedRoute);
  protected rayonService = inject(RayonService);
  protected modalService = inject(NgbModal);
  private storeInventoryService = inject(StoreInventoryService);
  private storageService = inject(StorageService);
  private errorService = inject(ErrorService);
  private spinner = inject(NgxSpinnerService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);

  constructor() {
    this.columnDefs = [
      {
        headerName: 'Cip',
        field: 'produitCip',
        sortable: false,
        flex: 0.4,
      },
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: false,
        minWidth: 300,
        flex: 1.2,
      },

      {
        headerName: 'Quantité',
        flex: 0.5,
        field: 'quantityOnHand',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
        cellStyle: this.stockOnHandcellStyle,
      },
      /*  {
          field: 'updated',
          cellRenderer: 'statusInvCellRenderer',
          flex: 0.4,
          hide: true,
          suppressToolPanel: false,
        },*/
      {
        headerName: 'Stock',
        hide: !this.hasAuthorityService.hasAuthorities(Authority.PR_VOIR_STOCK_INVENTAIRE),
        field: 'quantityInit',
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
        //  cellStyle: this.cellStyle,
        flex: 0.3,
      },
      {
        headerName: 'Ecart',
        field: 'gap',
        flex: 0.2,
        type: ['rightAligned', 'numericColumn'],
        valueFormatter: formatNumberToString,
        cellStyle: this.cellClass,
      },
    ];
    this.defaultColDef = {
      // flex: 1,
      // cellClass: 'align-right',
      enableCellChangeFlash: true,
    };
    /* this.frameworkComponents = {
       statusInvCellRenderer: InventoryStatusComponent,
     };*/
    this.context = { componentParent: this };
  }

  ngOnInit(): void {
    const width = window.innerWidth;
    if (width < 1800) {
      this.itemsPerPage = 10;
    }

    this.activatedRoute.data.subscribe(({ storeInventory }) => {
      this.storeInventory = storeInventory;
      if (this.storeInventory) {
        this.loadStorage();
        this.onSearch();
      }
    });
  }

  previousState(): void {
    window.history.back();
  }

  onGridReady(params: GridReadyEvent): void {
    this.gridApi = params.api;
  }

  onSelect(): void {
    this.loadPage(0);
  }

  confirmClose(): void {
    this.confirmationService.confirm({
      message: "Voullez-vous clôturer l'inventaire ?",
      header: ' CLOTURE',
      icon: 'pi pi-info-circle',
      rejectButtonProps: rejectButtonProps(),
      acceptButtonProps: acceptButtonProps(),
      accept: () => this.close(),
      key: 'saveAll',
    });
  }

  close(): void {
    if (this.storeInventory.id != null) {
      this.isSaving = true;
      this.spinner.show();
      this.storeInventoryService.close(this.storeInventory.id).subscribe({
        next: (res: HttpResponse<ItemsCountRecord>) => {
          this.spinner.hide();

          this.messageService.add({
            severity: 'success',
            summary: 'Success',
            detail: `Nombre de produits de l'inventaire ${res.body.count}`,
          });
          this.isSaving = false;
          setTimeout(() => {
            this.previousState();
          }, 1000);
        },
        error: error => {
          this.onCloseError(error);
          this.spinner.hide();
        },
      });
    }
  }

  onCellValueChanged(params: any): void {
    const item: IStoreInventoryLine = params.data as IStoreInventoryLine;
    if (!this.isCharNumeric(params.newValue)) {
      this.editCurrentCell(params);
    } else {
      item.storeInventoryId = this.storeInventory.id;
      this.subscribeToSaveItemResponse(params, this.storeInventoryLineService.update(item));
    }
  }

  cellClass(params: any): any {
    const item: IStoreInventoryLine = params.data as IStoreInventoryLine;
    if (item.updated) {
      return item.gap >= 0 ? { 'background-color': 'lightgreen' } : { 'background-color': 'lightcoral' };
    }
  }

  stockOnHandcellStyle(params: any): any {
    if (params.data.updated) {
      return { backgroundColor: '#c6c6c6' };
    }
  }

  searchFn(event: any): void {
    this.loadRayons(event.query, null);
  }

  loadRayons(search: string, storageId?: number): void {
    this.rayonService
      .query({
        page: 0,
        size: 999,
        search,
        storageId,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => this.onLoadRayonSuccess(res.body));
  }

  onFilter(): void {
    this.onSearch();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.loading = true;
    this.storeInventoryLineService
      .query({
        page: pageToLoad - 1,
        size: this.itemsPerPage,
        ...this.buildQuery(),
      })
      .subscribe({
        next: (res: HttpResponse<IStoreInventoryLine[]>) => this.onSuccess(res.body, res.headers, pageToLoad),
        error: () => {
          this.loading = false;
          this.storeInventoryLines = [];
        },
      });
  }

  exportPdf(): void {
    this.spinner.show();

    this.storeInventoryService.exportToPdf(this.buildPdfQuery()).subscribe(blod => {
      // const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
      // saveAs(blod, 'inventaire_' + fileName);
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
      this.spinner.hide();
    });
  }

  protected editCurrentCell(params: any): void {
    this.gridApi.stopEditing(true);
    params.data.quantityOnHand = params.oldValue;
    this.gridApi.startEditingCell({
      rowIndex: params.rowIndex,
      colKey: 'quantityOnHand',
    });
  }

  protected onSuccess(data: IStoreInventoryLine[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.storeInventoryLines = data || [];
    this.loading = false;
    this.ngbPaginationPage = this.page;
  }

  protected onSearch(): void {
    this.loadPage();
  }

  protected onClear(): void {
    this.selectedRayon = null;
    this.onSearch();
  }

  protected onSelectStrorage(evt: any): void {
    this.loadPage(0);
    this.loadRayons(null, evt.value.id);
  }

  protected onSaveSuccess(): void {
    this.isSaving = false;
    this.previousState();
  }

  protected onSaveError(): void {
    this.isSaving = false;
  }

  protected onLoadRayonSuccess(rayons: IRayon[] | null): void {
    this.rayons = rayons || [];
  }

  protected onUpadateIem(params: any, storeInventoryLine: IStoreInventoryLine): void {
    if (storeInventoryLine) {
      this.goToNextPage(params);
    }
  }

  protected onSaveItemError(params: any): void {
    this.editCurrentCell(params);
  }

  protected goToNextPage(params: any): void {
    const rowIndex = params.rowIndex;

    if (rowIndex === this.itemsPerPage - 1) {
      this.loadPage(this.ngbPaginationPage + 1);
      setTimeout(() => {
        this.gridApi.startEditingCell({
          rowIndex: 0,
          colKey: 'quantityOnHand',
        });
      }, 100);
    } else {
      this.loadPage(this.ngbPaginationPage);
    }
  }

  protected subscribeToSaveItemResponse(params: any, result: Observable<HttpResponse<IStoreInventoryLine>>): void {
    result.subscribe({
      next: (res: HttpResponse<IStoreInventoryLine>) => this.onUpadateIem(params, res.body),

      error: () => this.onSaveItemError(params),
    });
  }

  private buildPdfQuery(): StoreInventoryExportRecord {
    return {
      exportGroupBy: GROUPING_BY[0].name,
      filterRecord: {
        storeInventoryId: this.storeInventory?.id,
        search: this.searchValue,
        storageId: this.selectedStorage?.id,
        rayonId: this.selectedRayon?.id,
        selectedFilter: this.selectedfiltres ? this.selectedfiltres.name : 'NONE',
      },
    };
  }

  private openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  private onCloseError(error: any): void {
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

  private isCharNumeric(charStr: string): boolean {
    return !!/\d/.test(charStr);
  }

  private buildQuery(): any {
    return {
      storeInventoryId: this.storeInventory?.id,
      search: this.searchValue,
      storageId: this.selectedStorage?.id,
      rayonId: this.selectedRayon?.id,
      selectedFilter: this.selectedfiltres ? this.selectedfiltres.name : 'NONE',
    };
  }

  private loadStorage(): void {
    this.storageService.query().subscribe((res: HttpResponse<Storage[]>) => {
      this.storages = res.body || [];
    });
  }
}
