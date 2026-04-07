import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbNavChangeEvent, NgbNavModule } from '@ng-bootstrap/ng-bootstrap';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { ChipModule } from 'primeng/chip';
import { Drawer } from 'primeng/drawer';
import { SplitButton } from 'primeng/splitbutton';
import { MenuItem } from 'primeng/api';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import {
  IRecapProduitVendu,
  IRecapProduitVenduRequestParam,
  IRecapProduitVenduSummary,
  SeuilFilterType,
  StockFilterType,
} from 'app/shared/model/report/recap-produit-vendu.model';
import { RecapProduitVenduService } from '../services/recap-produit-vendu.service';
import { formatCurrency } from 'app/shared/utils/format-utils';
import { DatePicker } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { Checkbox } from 'primeng/checkbox';
import { InputNumber } from 'primeng/inputnumber';
import { FloatLabel } from 'primeng/floatlabel';
import { RayonService } from '../../rayon/rayon.service';
import { IRayon } from '../../../shared/model/rayon.model';
import { IFournisseur } from '../../../shared/model/fournisseur.model';
import { FournisseurService } from '../../fournisseur/fournisseur.service';
import { UserService } from '../../../core/user/user.service';
import { IUser, User } from '../../../core/user/user.model';
import { Authority } from '../../../shared/constants/authority.constants';
import { SpinnerComponent } from '../../../shared/spinner/spinner.component';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';
import { BlobDownloadService } from '../../../shared/services/blob-download.service';
import { finalize } from "rxjs/operators";
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";

@Component({
  selector: 'jhi-recap-produit-vendu',
  templateUrl: './recap-produit-vendu.component.html',
  styleUrl: './recap-produit-vendu.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    NgbNavModule,
    TableModule,
    ButtonModule,
    SelectModule,
    ToolbarModule,
    ChipModule,
    Drawer,
    SplitButton,
    WarehouseCommonModule,
    DatePicker,
    InputTextModule,
    Checkbox,
    InputNumber,
    FloatLabel,
    SpinnerComponent,
    ToastAlertComponent,
  ],
})
export default class RecapProduitVenduComponent implements OnInit {
  protected products = signal<IRecapProduitVendu[]>([]);
  protected unsoldProducts = signal<IRecapProduitVendu[]>([]);
  protected summary = signal<IRecapProduitVenduSummary | null>(null);
  protected unsoldSummary = signal<IRecapProduitVenduSummary | null>(null);
  protected isLoading = signal<boolean>(false);
  protected filtersDrawerVisible = signal<boolean>(false);
  protected activeTab = signal<string>('vendus');

  // Basic Filters
  protected startDate = signal<Date | null>(new Date());
  protected endDate = signal<Date | null>(new Date());
  protected searchTerm = signal<string>('');

  // Advanced Filters
  protected startTime = signal<string | null>(null);
  protected endTime = signal<string | null>(null);
  protected selectedUser = signal<IUser | null>(null);
  protected selectedRayon = signal<IRayon | null>(null);
  protected selectedFournisseur = signal<IFournisseur | null>(null);
  protected selectedSeuilFilter = signal<SeuilFilterType | null>(null);
  protected seuilValue = signal<number | null>(null);
  protected selectedStockFilter = signal<StockFilterType | null>(null);
  protected stockValue = signal<number | null>(null);
  protected minQuantitySold = signal<number | null>(null);
  protected unitPriceLessThanPurchasePrice = signal<boolean>(false);
  protected suggerQuantitySold = signal<boolean>(false);

  protected seuilFilterOptions = signal<{ label: string; value: SeuilFilterType | null }[]>([
    { label: 'Aucun filtre', value: null },
    { label: 'Seuil Inferieur à', value: SeuilFilterType.LESS_THAN },
    { label: 'Seuil supérieur à', value: SeuilFilterType.GREATER_THAN },
    { label: 'Seuil égal à', value: SeuilFilterType.EQUAL_TO },
    { label: 'Seuil supérieur ou égal à', value: SeuilFilterType.GREATER_THAN_OR_EQUAL_TO },
    { label: 'Seuil inférieur ou égal à', value: SeuilFilterType.LESS_THAN_OR_EQUAL_TO },
    { label: 'Seuil mini atteint', value: SeuilFilterType.SEUIL_MINI_ATTEINT },
  ]);

  protected stockFilterOptions = signal<{ label: string; value: StockFilterType | null }[]>([
    { label: 'Aucun filtre', value: null },
    { label: 'Stock égal à', value: StockFilterType.EQUAL_TO },
    { label: 'Stock inférieur à', value: StockFilterType.LESS_THAN },
    { label: 'Stock supérieur à', value: StockFilterType.GREATER_THAN },
    { label: 'Stock Superieur ou égal', value: StockFilterType.GREATER_THAN_OR_EQUAL_TO },
    { label: 'Stock inférieur ou égal à', value: StockFilterType.LESS_THAN_OR_EQUAL_TO },
    { label: 'Stock différent de', value: StockFilterType.NOT_EQUAL_TO },
    { label: 'Rupture de stock', value: StockFilterType.OUT_OF_STOCK },
  ]);
  protected exportMenuItems = signal<MenuItem[]>([
    {
      label: 'Pdf',
      icon: 'pi pi-file-pdf',
      command: () => this.exportToPdf(),
    },
    {
      label: 'Excel',
      icon: 'pi pi-file-excel',
      command: () => this.exportToExcel(),
    },
    {
      label: 'CSV',
      icon: 'pi pi-file',
      command: () => this.exportToCsv(),
    },
  ]);
  protected rayonOptions = signal<IRayon[]>([]);
  protected fournisseurOptions = signal<IFournisseur[]>([]);
  protected userOptions = signal<IUser[]>([]);
  // Pagination - separate for each tab
  protected page = signal<number>(1);
  protected itemsPerPage = signal<number>(10);
  protected totalItems = signal<number>(0);
  private readonly translate = inject(TranslateService);
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly downloadService = inject(BlobDownloadService);
  protected formatCurrency = formatCurrency;
  private readonly recapService = inject(RecapProduitVenduService);
  private readonly rayonService = inject(RayonService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly userService = inject(UserService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  protected actionsMenuItems = signal<MenuItem[]>([
    {
      label: 'Créer Inventaire',
      icon: 'pi pi-warehouse',
      command: () => this.createInventory(),
    },
    {
      label: 'Créer suggestion des quantité vendues',
      icon: 'pi pi-lightbulb',
      command: () => {
        if (this.activeTab() === 'vendus') {
          this.suggerQuantitySold.set(true);
          this.createSuggestion();
        } else {
          this.suggerQuantitySold.set(false);
          this.alert().showWarn("Cette action est disponible uniquement dans l'onglet des produits vendus.");
        }
      },
    },
    {
      label: 'Créer suggestion des quantité reappro',
      icon: 'pi pi-lightbulb',
      command: () => this.createSuggestion(),
    },
  ]);
constructor() {
  this.translate.use('fr');
  this.translate.stream('primeng').subscribe(data => {
    this.primeNGConfig.setTranslation(data);
  });
}

  ngOnInit(): void {    this.loadRayons();
    this.loadUsers();
    this.loadFournisseur();

    // Load data for the current active tab
    this.loadCurrentTabData();
  }

  loadUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.userOptions.set(
        res.body.filter(u => u.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE) || u.authorities.includes(Authority.ADMIN)),
      );
    });
  }

  protected onTabChange(event: NgbNavChangeEvent): void {
    this.activeTab.set(event.nextId);
    this.page.set(1); // Reset pagination when switching tabs
    this.loadCurrentTabData();
  }

  protected loadRayons(): void {
    this.rayonService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IRayon[]>) => {
        this.rayonOptions.set(res.body || []);
      });
  }

  /**
   * Load data based on the active tab (vendus or invendus)
   */
  protected loadCurrentTabData(): void {
    if (this.activeTab() === 'invendus') {
      this.loadUnsoldProducts();
      this.loadUnsoldSummary();
    } else {
      this.loadSoldProducts();
      this.loadSoldSummary();
    }
  }

  /**
   * Load sold products data
   */
  protected loadSoldProducts(): void {
    this.isLoading.set(true);
    const requestParam = this.buildRequestParam();

    this.recapService
      .getRecapProduitVenduReport({
        ...requestParam,
        page: this.page() - 1,
        size: this.itemsPerPage(),
      })
      .subscribe({
        next: (res: HttpResponse<IRecapProduitVendu[]>) => {
          this.products.set(res.body ?? []);
          this.totalItems.set(Number(res.headers.get('X-Total-Count')) || 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  /**
   * Load sold products summary
   */
  protected loadSoldSummary(): void {
    const requestParam = this.buildRequestParam();
    this.recapService.getRecapProduitVenduSummary(requestParam).subscribe({
      next: (res: HttpResponse<IRecapProduitVenduSummary>) => {
        this.summary.set(res.body ?? null);
      },
    });
  }

  protected buildRequestParam(): IRecapProduitVenduRequestParam {
    return {
      startDate: DATE_FORMAT_ISO_DATE(this.startDate()),
      endDate: DATE_FORMAT_ISO_DATE(this.endDate()),
      startTime: this.startTime() || undefined,
      endTime: this.endTime() || undefined,
      userId: this.selectedUser()?.id || undefined,
      searchTerm: this.searchTerm() || undefined,
      rayonId: this.selectedRayon()?.id || undefined,
      fournisseurId: this.selectedFournisseur()?.id || undefined,
      seuilFilterType: this.selectedSeuilFilter() || undefined,
      stockFilterType: this.selectedStockFilter() || undefined,
      seuilValue: this.seuilValue() || undefined,
      stockValue: this.stockValue() || undefined,
      quantitySold: this.minQuantitySold() || undefined,
      unitPriceLessThanPurchasePrice: this.unitPriceLessThanPurchasePrice() || undefined,
      suggerQuantitySold: this.suggerQuantitySold() || undefined,
      isInvendu: this.getIsProduitVendu(),
    };
  }

  protected onFilterChange(): void {
    this.page.set(1);
    this.loadCurrentTabData();
  }

  protected onClearFilters(): void {
    // Basic filters
    this.startDate.set(new Date());
    this.endDate.set(new Date());
    this.searchTerm.set('');

    // Advanced filters
    this.startTime.set(null);
    this.endTime.set(null);
    this.selectedUser.set(null);
    this.selectedRayon.set(null);
    this.selectedFournisseur.set(null);
    this.selectedSeuilFilter.set(null);
    this.seuilValue.set(null);
    this.selectedStockFilter.set(null);
    this.stockValue.set(null);
    this.minQuantitySold.set(null);
    this.unitPriceLessThanPurchasePrice.set(false);
    this.suggerQuantitySold.set(false);

    this.page.set(1);
    this.loadCurrentTabData();
  }

  protected toggleFiltersDrawer(): void {
    this.filtersDrawerVisible.update(value => !value);
    if (!this.filtersDrawerVisible()) {
      this.loadCurrentTabData();
    }
  }

  /**
   * Handle pagination change for both tabs
   */
  protected onPageChange(event: any): void {
    if (event && typeof event.first === 'number' && typeof event.rows === 'number') {
      const newPage = Math.floor(event.first / event.rows) + 1;
      this.page.set(newPage);
      this.itemsPerPage.set(event.rows);
      this.loadCurrentTabData();
    }
  }

  /**
   * Export to PDF based on active tab
   */
  protected exportToPdf(): void {
    const isInvendu = this.activeTab() === 'invendus';
    const source$ = isInvendu
      ? this.recapService.exportInvenduToPdf(this.buildRequestParam())
      : this.recapService.exportToPdf(this.buildRequestParam());

    this.downloadService.downloadFromObservable(
      source$,
      isInvendu ? 'recap-produit-invendu' : 'recap-produit-vendu',
      'pdf',
      () => this.spinner().show(),
      () => this.spinner().hide(),
      () => this.alert().showError("Erreur lors de l'export PDF"),
    );
  }

  /**
   * Export to Excel based on active tab
   */
  protected exportToExcel(): void {
    const isInvendu = this.activeTab() === 'invendus';
    const source$ = isInvendu
      ? this.recapService.exportInvenduToExcel(this.buildRequestParam())
      : this.recapService.exportToExcel(this.buildRequestParam());

    this.downloadService.downloadFromObservable(
      source$,
      isInvendu ? 'recap-produit-invendu' : 'recap-produit-vendu',
      'excel',
      () => this.spinner().show(),
      () => this.spinner().hide(),
      () => this.alert().showError("Erreur lors de l'export Excel"),
    );
  }

  /**
   * Export to CSV based on active tab
   */
  protected exportToCsv(): void {
    const isInvendu = this.activeTab() === 'invendus';
    const source$ = isInvendu
      ? this.recapService.exportInvenduToCsv(this.buildRequestParam())
      : this.recapService.exportToCsv(this.buildRequestParam());

    this.downloadService.downloadFromObservable(
      source$,
      isInvendu ? 'recap-produit-invendu' : 'recap-produit-vendu',
      'csv',
      () => this.spinner().show(),
      () => this.spinner().hide(),
      () => this.alert().showError("Erreur lors de l'export CSV"),
    );
  }

  /**
   * Create suggestion based on active tab data
   */
  protected createSuggestion(): void {
    const isInvendu = this.activeTab() === 'invendus';

    if (isInvendu && !this.suggerQuantitySold()) {
      this.alert().showWarn('La création de suggestion de réapprovisionnement est disponible pour les produits invendus.');
    }

    const requestParam = this.buildRequestParam();
    this.spinner().show();

    this.recapService
      .createSuggestionFromRecap(requestParam)
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<number>) => {
          const productType = isInvendu ? 'invendus' : 'vendus';
          const suggestionType = this.suggerQuantitySold() ? 'quantités vendues' : 'réapprovisionnement';
          this.alert().showInfo(`${res.body} produit(s) ${productType} suggéré(s) pour ${suggestionType}`);
        },
        error: () => {
          this.alert().showError('Erreur lors de la création de la suggestion');
        },
      });
  }

  /**
   * Create inventory based on active tab data
   */
  protected createInventory(): void {
    const isInvendu = this.activeTab() === 'invendus';
    const productType = isInvendu ? 'invendus' : 'vendus';

    const requestParam = this.buildRequestParam();
    this.spinner().show();

    this.recapService
      .createInventoryFromRecap({
        ...requestParam,
        isInvendu,
      })
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<number>) => {
          this.alert().showInfo(`${res.body} produit(s) ${productType} pris en compte pour l'inventaire`);
        },
        error: () => {
          this.alert().showError("Erreur lors de la création de l'inventaire");
        },
      });
  }

  protected getMarginPercentage(product: IRecapProduitVendu): number {
    if (!product.totalSalesAmount || !product.totalPurchaseAmount) return 0;
    return ((product.totalSalesAmount - product.totalPurchaseAmount) / product.totalSalesAmount) * 100;
  }

  protected getMarginSeverity(margin: number): string {
    if (margin >= 30) return 'success';
    if (margin >= 20) return 'info';
    if (margin >= 10) return 'warn';
    return 'danger';
  }

  /**
   * Load unsold products data
   */
  protected loadUnsoldProducts(): void {
    this.isLoading.set(true);
    const requestParam = this.buildRequestParam();

    this.recapService
      .getRecapProduitInvenduReport({
        ...requestParam,
        page: this.page() - 1,
        size: this.itemsPerPage(),
      })
      .subscribe({
        next: (res: HttpResponse<IRecapProduitVendu[]>) => {
          this.unsoldProducts.set(res.body ?? []);
          this.totalItems.set(Number(res.headers.get('X-Total-Count')) || 0);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });
  }

  /**
   * Load unsold products summary
   */
  protected loadUnsoldSummary(): void {
    const requestParam = this.buildRequestParam();
    this.recapService.getRecapProduitInvenduSummary(requestParam).subscribe({
      next: (res: HttpResponse<IRecapProduitVenduSummary>) => {
        this.unsoldSummary.set(res.body ?? null);
      },
    });
  }

  private loadFournisseur(): void {
    this.fournisseurService
      .query({
        page: 0,
        size: 9999,
      })
      .subscribe((res: HttpResponse<IFournisseur[]>) => {
        this.fournisseurOptions.set(res.body || []);
      });
  }

  private getIsProduitVendu(): boolean {
    return this.activeTab() !== 'invendus';
  }
}
