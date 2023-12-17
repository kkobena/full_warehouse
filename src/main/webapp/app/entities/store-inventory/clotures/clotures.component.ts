import { Component, Input, OnInit } from '@angular/core';
import {
  GROUPING_BY,
  InventoryCategory,
  InventoryStatut,
  IStoreInventory,
  StoreInventoryExportRecord,
} from '../../../shared/model/store-inventory.model';
import { IUser } from '../../../core/user/user.model';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { NgxSpinnerService } from 'ngx-spinner';
import { StoreInventoryService } from '../store-inventory.service';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { saveAs } from 'file-saver';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { AlertInfoComponent } from '../../../shared/alert/alert-info.component';
import { DATE_FORMAT_DD_MM_YYYY_HH_MM_SS } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-clotures',
  templateUrl: './clotures.component.html',
})
export class CloturesComponent implements OnInit {
  @Input() inventoryCategories: InventoryCategory[];
  @Input() user?: IUser | null;
  protected statuts: InventoryStatut[] = ['CLOSED'];
  protected page!: number;
  protected predicate!: string;
  protected ascending!: boolean;
  protected ngbPaginationPage = 1;
  protected itemsPerPage = ITEMS_PER_PAGE;
  protected rowData: IStoreInventory[] = [];
  protected totalItems = 0;

  constructor(
    private spinner: NgxSpinnerService,
    private storeInventoryService: StoreInventoryService,
    protected router: Router,
    protected modalService: NgbModal
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage();
  }

  exportPdf(storeInventory: IStoreInventory): void {
    this.spinner.show();
    this.storeInventoryService.exportToPdf(this.buildPdfQuery(storeInventory?.id)).subscribe(blod => {
      const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
      saveAs(blod, 'inventaire_' + fileName);
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
      userId: this.user?.id,
      inventoryCategories: this.inventoryCategories.map(e => e.name),
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
