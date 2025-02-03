import { Component, OnInit, input, inject } from '@angular/core';
import {
  GROUPING_BY,
  InventoryCategory,
  InventoryStatut,
  IStoreInventory,
  StoreInventoryExportRecord,
} from '../../../shared/model/store-inventory.model';
import { IUser } from '../../../core/user/user.model';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { StoreInventoryService } from '../store-inventory.service';
import { Router, RouterModule } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';

@Component({
    selector: 'jhi-clotures',
    templateUrl: './clotures.component.html',
    imports: [WarehouseCommonModule, ButtonModule, RippleModule, TooltipModule, ToastModule, NgxSpinnerModule, TableModule, RouterModule]
})
export class CloturesComponent implements OnInit {
  private spinner = inject(NgxSpinnerService);
  private storeInventoryService = inject(StoreInventoryService);
  protected router = inject(Router);
  protected modalService = inject(NgbModal);

  readonly inventoryCategories = input<InventoryCategory[]>();
  readonly user = input<IUser | null>();
  protected statuts: InventoryStatut[] = ['CLOSED'];
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected rowData: IStoreInventory[] = [];
  protected totalItems = 0;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {}

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage();
  }

  exportPdf(storeInventory: IStoreInventory): void {
    this.spinner.show();
    this.storeInventoryService.exportToPdf(this.buildPdfQuery(storeInventory.id)).subscribe(blod => {
      // const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
      // saveAs(blod, 'inventaire_' + fileName);
      const blobUrl = URL.createObjectURL(blod);
      window.open(blobUrl);
      this.spinner.hide();
    });
  }

  protected openInfoDialog(message: string, infoClass: string): void {
    const modalRef = this.modalService.open(AlertInfoComponent, {
      backdrop: 'static',
      centered: true,
    });
    modalRef.componentInstance.message = message;
    modalRef.componentInstance.infoClass = infoClass;
  }

  protected onSuccess(data: IStoreInventory[] | null, headers: HttpHeaders, page: number, navigate: boolean): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    if (navigate) {
      this.router.navigate(['/store-inventory'], {
        queryParams: this.buildQuery(page),
      });
    }
    this.rowData = data || [];
    this.ngbPaginationPage = this.page;
  }

  protected onError(): void {
    this.ngbPaginationPage = this.page ?? 1;
  }

  private loadPage(page?: number, dontNavigate?: boolean): void {
    const pageToLoad: number = page || this.page || 1;

    this.storeInventoryService.query(this.buildQuery(page)).subscribe({
      next: (res: HttpResponse<IStoreInventory[]>) => this.onSuccess(res.body, res.headers, pageToLoad, !dontNavigate),
      error: () => this.onError(),
    });
  }

  private buildQuery(page?: number): any {
    const pageToLoad: number = page || this.page || 1;
    return {
      page: pageToLoad - 1,
      size: this.itemsPerPage,
      userId: this.user().id,
      inventoryCategories: this.inventoryCategories().map(e => e.name),
      statuts: this.statuts,
    };
  }

  private buildPdfQuery(storeInventoryId: number): StoreInventoryExportRecord {
    return {
      exportGroupBy: GROUPING_BY[0].name,
      filterRecord: {
        storeInventoryId,
        selectedFilter: 'NONE',
      },
    };
  }
}
