import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Observable, Subscription } from 'rxjs';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { CATEGORY_INVENTORY, InventoryCategory, IStoreInventory } from 'app/shared/model/store-inventory.model';

import { ITEMS_PER_PAGE } from 'app/shared/constants/pagination.constants';
import { StoreInventoryService } from './store-inventory.service';
import { StoreInventoryDeleteDialogComponent } from './store-inventory-delete-dialog.component';
import { IUser, User } from '../../core/user/user.model';
import { UserService } from '../../core/user/user.service';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InventoryFormComponent } from './inventory-form/inventory-form.component';
import { Router } from '@angular/router';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { MultiSelectModule } from 'primeng/multiselect';
import { CardModule } from 'primeng/card';
import { ToolbarModule } from 'primeng/toolbar';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';
import { FormsModule } from '@angular/forms';
import { EnCoursComponent } from './en-cours/en-cours.component';
import { CloturesComponent } from './clotures/clotures.component';

@Component({
    selector: 'jhi-store-inventory',
    templateUrl: './store-inventory.component.html',
    providers: [ConfirmationService, DialogService, MessageService],
    styles: [
        `
      .table tr:hover {
        cursor: pointer;
      }

      .table .active {
        background-color: #95caf9 !important;
      }

      .ag-theme-alpine {
        min-height: 400px;
        height: 550px;
        max-height: 700px;
      }
    `,
    ],
    imports: [
        WarehouseCommonModule,
        MultiSelectModule,
        CardModule,
        ToolbarModule,
        DropdownModule,
        ButtonModule,
        RippleModule,
        FormsModule,
        EnCoursComponent,
        CloturesComponent,
        StoreInventoryDeleteDialogComponent,
        InventoryFormComponent,
    ]
})
export class StoreInventoryComponent implements OnInit {
  protected storeInventories: IStoreInventory[];
  protected eventSubscriber?: Subscription;
  protected selectedRowIndex?: number;
  protected itemsPerPage: number;
  protected links: any;
  protected page: number;
  protected predicate: string;
  protected ascending: boolean;
  protected readonly showUserCombo: boolean = false;
  protected columnDefs: any[];
  protected rowData: any = [];
  protected event: any;
  protected searchValue?: string;
  protected search: string = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected users: IUser[] = [];
  protected user?: IUser | null;
  protected active = 'CREATE';
  protected ref?: DynamicDialogRef;
  protected categories: InventoryCategory[] = CATEGORY_INVENTORY;
  protected inventoryCategories?: InventoryCategory[];

  constructor(
    protected storeInventoryService: StoreInventoryService,
    protected modalService: NgbModal,
    protected userService: UserService,
    private dialogService: DialogService,
    protected router: Router,
  ) {
    this.inventoryCategories = this.categories;
    this.storeInventories = [];
    this.itemsPerPage = ITEMS_PER_PAGE;
    this.page = 0;
    this.links = {
      last: 0,
    };
    this.predicate = 'id';
    this.ascending = true;
    this.columnDefs = [
      {
        headerName: 'Libellé',
        field: 'produitLibelle',
        sortable: true,
        filter: 'agTextColumnFilter',
        minWidth: 300,
        flex: 1.2,
      },
      {
        headerName: 'Stock initial',
        field: 'quantityInit',
        type: ['rightAligned', 'numericColumn'],
        editable: true,
        width: 120,
        valueFormatter: this.formatNumber,
      },
      {
        headerName: 'Quantité saisie',
        width: 140,
        field: 'quantityOnHand',
        editable: true,
        type: ['rightAligned', 'numericColumn'],
      },
      {
        headerName: 'Ecart',
        width: 80,
        type: ['rightAligned', 'numericColumn'],
        valueGetter: this.setGap,
        cellStyle: this.cellClass,
      },
    ];
  }

  cellClass(params: any): any {
    if (params.data.updated) {
      const ecart = Number(params.data.quantityOnHand) - Number(params.data.quantityInit);
      return ecart >= 0 ? { backgroundColor: 'lightgreen' } : { backgroundColor: 'lightcoral' };
    }
    return {};
  }

  setGap(params: any): number {
    if (params.data.updated) {
      return params.data.quantityOnHand - params.data.quantityInit;
    }
    return 0;
  }

  loadAll(): void {
    this.storeInventoryService.query({}).subscribe((res: HttpResponse<IStoreInventory[]>) => this.onSuccess(res.body));
  }

  reset(): void {
    this.page = 0;
    this.storeInventories = [];
    this.loadAll();
  }

  loadPage(page: number): void {
    this.page = page;
    this.loadAll();
  }

  ngOnInit(): void {
    this.loadAllUsers();
    this.loadAll();
  }

  trackId(index: number, item: IStoreInventory): number {
    // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
    return item.id!;
  }

  delete(storeInventory: IStoreInventory): void {
    const modalRef = this.modalService.open(StoreInventoryDeleteDialogComponent, {
      size: 'lg',
      backdrop: 'static',
    });
    modalRef.componentInstance.storeInventory = storeInventory;
  }

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  onFilterTextBoxChanged(event: any): void {
    this.searchValue = event.target.value;
    /*  if(this.produitSelected!==null && this.produitSelected!==undefined &&
        Number(event.target.value)!==0){

      }*/
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  onCreateNew(): void {
    this.ref = this.dialogService.open(InventoryFormComponent, {
      data: { entity: null },
      width: '40%',
      header: 'Nouvel inventaire',
    });
    this.ref.onClose.subscribe((res: IStoreInventory) => this.goTo(res));
  }

  goTo(entity: IStoreInventory): void {
    this.router.navigate(['/store-inventory', entity.id, 'edit']);
  }

  clickRow(storeInventory: IStoreInventory): void {
    this.selectedRowIndex = storeInventory.id;
    this.rowData = storeInventory.storeInventoryLines;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IStoreInventory>>): void {
    result.subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.loadAll();
  }

  protected onSaveError(): void {}

  protected onSuccess(data: IStoreInventory[] | null): void {
    if (data) {
      this.storeInventories = data;
    }
  }

  protected loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.users.push({ id: null, fullName: 'TOUT' });
      if (res.body) {
        this.users.push(...res.body);
      }
      this.user = { id: null, fullName: 'TOUT' };
    });
  }

  protected onSearch(): void {}

  protected onSelectUser(): void {
    this.onSearch();
  }
}
