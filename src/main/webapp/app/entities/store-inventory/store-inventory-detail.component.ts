import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { GROUPING_BY, IStoreInventory, StoreInventoryExportRecord } from 'app/shared/model/store-inventory.model';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { IStoreInventoryLine } from '../../shared/model/store-inventory-line.model';
import { StoreInventoryLineService } from '../store-inventory-line/store-inventory-line.service';
import { IRayon } from '../../shared/model/rayon.model';
import { Storage } from '../storage/storage.model';
import { APPEND_TO, NOT_FOUND, PRODUIT_COMBO_MIN_LENGTH } from '../../shared/constants/pagination.constants';
import { RayonService } from '../rayon/rayon.service';
import { NgxSpinnerModule, NgxSpinnerService } from 'ngx-spinner';
import { StoreInventoryService } from './store-inventory.service';
import { ITEMS_PER_PAGE } from '../../config/pagination.constants';
import { WarehouseCommonModule } from '../../shared/warehouse-common/warehouse-common.module';
import { FormsModule } from '@angular/forms';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

@Component({
  selector: 'jhi-store-inventory-detail',
  templateUrl: './store-inventory-detail.component.html',
  standalone: true,
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
  ],
})
export class StoreInventoryDetailComponent implements OnInit {
  storeInventory: IStoreInventory | null = null;
  protected page = 0;
  protected loading!: boolean;
  protected itemsPerPage = 10;
  protected storeInventoryLines?: IStoreInventoryLine[] = [];
  protected totalItems = 0;
  protected searchValue?: string;
  protected selectedRayon: IRayon | null;
  protected selectedStorage: Storage | null;
  protected readonly filtres: any = [
    { name: 'GAP', label: 'Ecart' },
    { name: 'GAP_POSITIF', label: 'Ecart positif' },
    { name: 'GAP_NEGATIF', label: 'Ecart négatif' },
  ];
  protected ngbPaginationPage = 1;
  protected selectedfiltres: any | null;
  protected readonly PRODUIT_COMBO_MIN_LENGTH = PRODUIT_COMBO_MIN_LENGTH;
  protected readonly NOT_FOUND = NOT_FOUND;
  protected readonly APPEND_TO = APPEND_TO;
  protected rayons: IRayon[] = [];
  protected storages: Storage[];
  protected readonly ITEMS_PER_PAGE = ITEMS_PER_PAGE;

  constructor(
    protected rayonService: RayonService,
    protected activatedRoute: ActivatedRoute,
    protected storeInventoryLineService: StoreInventoryLineService,
    private spinner: NgxSpinnerService,
    protected storeInventoryService: StoreInventoryService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ storeInventory }) => {
      this.storeInventory = storeInventory;
      this.onSearch();
    });
  }

  previousState(): void {
    window.history.back();
  }

  onSearch(): void {
    this.loadPage();
  }

  loadPage(page?: number): void {
    const pageToLoad: number = page || this.page || 1;
    this.loading = true;
    this.storeInventoryLineService
      .queryItems({
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

  searchFn(event: any): void {
    this.loadRayons(event.query, null);
  }

  loadRayons(search: string, storageId?: number): void {
    this.rayonService
      .query({
        page: 0,
        size: 10,
        search,
        storageId,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => this.onLoadRayonSuccess(res.body));
  }

  onFilter(): void {
    this.onSearch();
  }

  onSelect(): void {
    this.loadPage(0);
  }

  exportPdf(): void {
    this.spinner.show();

    this.storeInventoryService.exportToPdf(this.buildPdfQuery()).subscribe({
      next: blod => {
        //   const fileName = DATE_FORMAT_DD_MM_YYYY_HH_MM_SS();
        //   saveAs(blod, 'inventaire_' + fileName);
        const blobUrl = URL.createObjectURL(blod);
        window.open(blobUrl);
        this.spinner.hide();
      },
      error: () => this.spinner.hide(),
    });
  }

  protected onSelectStrorage(evt: any): void {
    this.loadPage(0);
    this.loadRayons(null, evt.value.id);
  }

  protected onClear() {
    this.selectedRayon = null;
    this.onSearch();
  }

  protected onSuccess(data: IStoreInventoryLine[] | null, headers: HttpHeaders, page: number): void {
    this.totalItems = Number(headers.get('X-Total-Count'));
    this.page = page;
    this.storeInventoryLines = data || [];
    this.loading = false;
    this.ngbPaginationPage = this.page;
  }

  private buildPdfQuery(): StoreInventoryExportRecord {
    return {
      exportGroupBy: GROUPING_BY[0].name,
      filterRecord: {
        storeInventoryId: this.storeInventory?.id,
        search: this.searchValue,
        storageId: this.selectedStorage?.id,
        rayonId: this.selectedRayon?.id,
        selectedFilter: this.selectedfiltres ? this.selectedfiltres?.name : 'NONE',
      },
    };
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

  private onLoadRayonSuccess(rayons: IRayon[] | null): void {
    this.rayons = rayons || [];
  }
}
