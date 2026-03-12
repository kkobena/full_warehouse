import {Component, inject, OnInit} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';

import {
  CATEGORY_INVENTORY,
  InventoryCategory,
  IStoreInventory
} from 'app/shared/model/store-inventory.model';

import {ITEMS_PER_PAGE} from 'app/shared/constants/pagination.constants';
import {StoreInventoryService} from './store-inventory.service';
import {IUser, User} from '../../core/user/user.model';
import {UserService} from '../../core/user/user.service';
import {ConfirmationService, MessageService} from 'primeng/api';
import {InventoryFormComponent} from './inventory-form/inventory-form.component';
import {Router} from '@angular/router';
import {WarehouseCommonModule} from '../../shared/warehouse-common/warehouse-common.module';
import {MultiSelectModule} from 'primeng/multiselect';
import {CardModule} from 'primeng/card';
import {ToolbarModule} from 'primeng/toolbar';

import {ButtonModule} from 'primeng/button';
import {RippleModule} from 'primeng/ripple';
import {FormsModule} from '@angular/forms';
import {EnCoursComponent} from './en-cours/en-cours.component';
import {CloturesComponent} from './clotures/clotures.component';
import {Select} from 'primeng/select';
import {FloatLabel} from 'primeng/floatlabel';
import {showCommonModal} from '../sales/selling-home/sale-helper';
import {Tooltip} from "primeng/tooltip";

@Component({
  selector: 'jhi-store-inventory',
  templateUrl: './store-inventory.component.html',
  providers: [ConfirmationService, MessageService],
  styleUrls: ['./store-inventory.scss'],
  imports: [
    WarehouseCommonModule,
    MultiSelectModule,
    CardModule,
    ToolbarModule,
    ButtonModule,
    RippleModule,
    FormsModule,
    EnCoursComponent,
    CloturesComponent,
    Select,
    FloatLabel,
    Tooltip,
  ],
})
export class StoreInventoryComponent implements OnInit {
  protected storeInventories: IStoreInventory[];
  protected selectedRowIndex?: number;
  protected itemsPerPage: number;
  protected links: any;
  protected page: number;
  protected predicate: string;
  protected ascending: boolean;
  protected columnDefs: any[];
  protected rowData: any = [];
  protected event: any;
  protected search = '';
  protected fromDate: Date = new Date();
  protected toDate: Date = new Date();
  protected users: IUser[] = [];
  protected user?: IUser | null;
  protected active = 'CREATE';
  protected readonly menuTileAndIcon = [
    {title: 'Inventaires en cours', icon: 'pi pi-database', menuId: 'CREATE'},
    {title: 'Inventaires clôturés', icon: 'pi pi-lock', menuId: 'CLOSED'},
  ];

  protected categories: InventoryCategory[] = CATEGORY_INVENTORY;
  protected inventoryCategories?: InventoryCategory[];
  private storeInventoryService = inject(StoreInventoryService);
  private readonly modalService = inject(NgbModal);
  private userService = inject(UserService);
  private router = inject(Router);

  constructor() {
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

  protected get title(): string {
    return this.menuTileAndIcon.find(m => m.menuId === this.active)?.title || '';
  }

  protected get icon(): string {
    return this.menuTileAndIcon.find(m => m.menuId === this.active)?.icon || '';
  }

  cellClass(params: any): any {
    if (params.data.updated) {
      const ecart = Number(params.data.quantityOnHand) - Number(params.data.quantityInit);
      return ecart >= 0 ? {backgroundColor: 'lightgreen'} : {backgroundColor: 'lightcoral'};
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

  sort(): string[] {
    const result = [this.predicate + ',' + (this.ascending ? 'asc' : 'desc')];
    if (this.predicate !== 'id') {
      result.push('id');
    }
    return result;
  }

  formatNumber(number: any): string {
    return Math.floor(number.value)
      .toString()
      .replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1 ');
  }

  onCreateNew(): void {
    showCommonModal(
      this.modalService,
      InventoryFormComponent,
      {},
      (res: IStoreInventory) => {
        if (res) {
          this.goTo(res);
        }
      },
      'lg',
    );
  }

  goTo(entity: IStoreInventory): void {
    this.router.navigate(['/store-inventory', entity.id, 'edit']);
  }

  protected onSaveSuccess(): void {
    this.loadAll();
  }

  protected onSaveError(): void {
  }

  protected onSuccess(data: IStoreInventory[] | null): void {
    if (data) {
      this.storeInventories = data;
    }
  }

  protected loadAllUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.users.push({id: null, fullName: 'TOUT'});
      if (res.body) {
        this.users.push(...res.body);
      }
      this.user = {id: null, fullName: 'TOUT'};
    });
  }

  protected onSearch(): void {
  }

  protected onSelectUser(): void {
    this.onSearch();
  }
}
