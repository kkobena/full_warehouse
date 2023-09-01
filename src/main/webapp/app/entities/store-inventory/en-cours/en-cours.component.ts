import { Component, Input, OnInit } from '@angular/core';
import { InventoryCategory, InventoryStatut, IStoreInventory } from '../../../shared/model/store-inventory.model';
import { IUser } from '../../../core/user/user.model';
import { StoreInventoryService } from '../store-inventory.service';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ITEMS_PER_PAGE } from '../../../config/pagination.constants';
import { Router } from '@angular/router';
import { saveAs } from 'file-saver';
import { ConfirmationService } from 'primeng/api';
import { NgxSpinnerService } from 'ngx-spinner';

@Component({
  selector: 'jhi-en-cours',
  templateUrl: './en-cours.component.html',
  styleUrls: ['./en-cours.component.scss'],
})
export class EnCoursComponent implements OnInit {
  @Input() inventoryCategories: InventoryCategory[];
  @Input() user?: IUser | null;
  protected statuts: InventoryStatut[] = ['CREATE', 'PROCESSING'];
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
    private confirmationService: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loadPage();
  }

  exportPdf(storeInventory: IStoreInventory): void {
    this.storeInventoryService.exportToPdf(storeInventory.id).subscribe(blod => saveAs(blod));
  }

  confirmDelete(storeInventory: IStoreInventory): void {
    this.confirmationService.confirm({
      message: ' Voullez-vous supprimer cette ligne  ?',
      header: ' SUPPRESSION',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(storeInventory?.id),
      key: 'delete',
    });
  }

  confirmSave(storeInventory: IStoreInventory): void {
    this.confirmationService.confirm({
      message: "Voullez-vous clôturer l'inventaire ?",
      header: ' CLOTURE',
      icon: 'pi pi-info-circle',
      accept: () => this.delete(storeInventory?.id),
      key: 'saveAll',
    });
  }

  delete(id: number): void {
    this.spinner.show();
    this.storeInventoryService.delete(id).subscribe({
      next: () => {
        this.loadPage();
        this.spinner.hide();
      },
      error: error => {
        this.onError();
        this.spinner.hide();
      },
    });
  }

  close(id: number): void {
    this.spinner.show();
    this.storeInventoryService.close(id).subscribe({
      next: () => {
        this.loadPage();
        this.spinner.hide();
      },
      error: error => {
        this.onError();
        this.spinner.hide();
      },
    });
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
}
