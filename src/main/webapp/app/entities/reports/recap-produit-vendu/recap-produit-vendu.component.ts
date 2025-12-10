import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
import { finalize } from 'rxjs/operators';
import { ToastAlertComponent } from '../../../shared/toast-alert/toast-alert.component';
import { DATE_FORMAT_ISO_DATE } from '../../../shared/util/warehouse-util';

@Component({
  selector: 'jhi-recap-produit-vendu',
  templateUrl: './recap-produit-vendu.component.html',
  styleUrl: './recap-produit-vendu.component.scss',
  imports: [
    CommonModule,
    FormsModule,
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
  protected summary = signal<IRecapProduitVenduSummary | null>(null);
  protected isLoading = signal<boolean>(false);
  protected helpDrawerVisible = signal<boolean>(false);
  protected filtersDrawerVisible = signal<boolean>(false);

  // Basic Filters
  protected startDate = signal<Date | null>(this.getFirstDayOfMonth());
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

  protected seuilFilterOptions = signal<Array<{ label: string; value: SeuilFilterType | null }>>([
    { label: 'Aucun filtre', value: null },
    { label: 'Seuil Inferieur à', value: SeuilFilterType.LESS_THAN },
    { label: 'Seuil supérieur à', value: SeuilFilterType.GREATER_THAN },
    { label: 'Seuil égal à', value: SeuilFilterType.EQUAL_TO },
    { label: 'Seuil supérieur ou égal à', value: SeuilFilterType.GREATER_THAN_OR_EQUAL_TO },
    { label: 'Seuil inférieur ou égal à', value: SeuilFilterType.LESS_THAN_OR_EQUAL_TO },
    { label: 'Seuil mini atteint', value: SeuilFilterType.SEUIL_MINI_ATTEINT },
  ]);

  protected stockFilterOptions = signal<Array<{ label: string; value: StockFilterType | null }>>([
    { label: 'Aucun filtre', value: null },
    { label: 'Stock égal à', value: StockFilterType.EQUAL_TO },
    { label: 'Stock inférieur à', value: StockFilterType.LESS_THAN },
    { label: 'Stock supérieur à', value: StockFilterType.GREATER_THAN },
    { label: 'Stock Superieur ou égal', value: StockFilterType.GREATER_THAN_OR_EQUAL_TO },
    { label: 'Stock inférieur ou égal à', value: StockFilterType.LESS_THAN_OR_EQUAL_TO },
    { label: 'Stock différent de', value: StockFilterType.NOT_EQUAL_TO },
    { label: 'Rupture de stock', value: StockFilterType.OUT_OF_STOCK },
  ]);

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
        this.suggerQuantitySold.set(true);
        this.createSuggestion();
      },
    },
    {
      label: 'Créer suggestion des quantité reappro',
      icon: 'pi pi-lightbulb',
      command: () => this.createSuggestion(),
    },
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

  // Pagination
  protected page = 1;
  protected itemsPerPage = 20;
  protected totalItems = 0;
  // Format methods
  protected formatCurrency = formatCurrency;
  private readonly recapService = inject(RecapProduitVenduService);
  private readonly rayonService = inject(RayonService);
  private readonly fournisseurService = inject(FournisseurService);
  private readonly userService = inject(UserService);
  private readonly spinner = viewChild.required<SpinnerComponent>('spinner');
  private readonly alert = viewChild.required<ToastAlertComponent>('alert');
  ngOnInit(): void {
    this.loadRayons();
    this.loadUsers();
    this.loadFournisseur();

    this.loadData();
  }
  loadUsers(): void {
    this.userService.query().subscribe((res: HttpResponse<User[]>) => {
      this.userOptions.set(
        res.body.filter(u => u.authorities.includes(Authority.ROLE_RESPONSABLE_COMMANDE) || u.authorities.includes(Authority.ADMIN)),
      );
    });
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

  protected loadData(): void {
    this.isLoading.set(true);
    const requestParam = this.buildRequestParam();

    this.recapService
      .getRecapProduitVenduReport({
        ...requestParam,
        page: this.page - 1,
        size: this.itemsPerPage,
      })
      .subscribe({
        next: (res: HttpResponse<IRecapProduitVendu[]>) => {
          this.products.set(res.body ?? []);
          this.totalItems = Number(res.headers.get('X-Total-Count')) || 0;
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        },
      });

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
    };
  }

  protected onFilterChange(): void {
    this.page = 1;
    this.loadData();
  }

  protected onClearFilters(): void {
    // Basic filters
    this.startDate.set(this.getFirstDayOfMonth());
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

    this.loadData();
  }

  protected toggleFiltersDrawer(): void {
    this.filtersDrawerVisible.update(value => !value);
  }

  protected getActiveFiltersCount(): number {
    let count = 0;
    if (this.searchTerm()) count++;
    if (this.startTime()) count++;
    if (this.endTime()) count++;
    if (this.selectedUser()) count++;
    if (this.selectedRayon()) count++;
    if (this.selectedFournisseur()) count++;
    if (this.selectedSeuilFilter()) count++;
    if (this.selectedStockFilter()) count++;
    if (this.minQuantitySold()) count++;
    if (this.unitPriceLessThanPurchasePrice()) count++;
    if (this.suggerQuantitySold()) count++;
    return count;
  }

  protected onPageChange(event: any): void {
    this.page = event.page + 1;
    this.itemsPerPage = event.rows;
    this.loadData();
  }

  protected exportToPdf(): void {
    const requestParam = this.buildRequestParam();
    this.spinner().show();
    this.recapService
      .exportToPdf(requestParam)
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<Blob>) => {
          if (res.body) {
            const blob = new Blob([res.body], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `recap-produit-vendu-${new Date().getTime()}.pdf`;
            link.click();
            window.URL.revokeObjectURL(url);
          }
        },
      });
  }

  protected exportToExcel(): void {
    const requestParam = this.buildRequestParam();
    this.spinner().show();
    this.recapService
      .exportToExcel(requestParam)
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<Blob>) => {
          if (res.body) {
            const blob = new Blob([res.body], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `recap-produit-vendu-${new Date().getTime()}.xlsx`;
            link.click();
            window.URL.revokeObjectURL(url);
          }
        },
      });
  }

  protected exportToCsv(): void {
    const requestParam = this.buildRequestParam();
    this.spinner().show();
    this.recapService
      .exportToCsv(requestParam)
      .pipe(finalize(() => this.spinner().hide()))
      .subscribe({
        next: (res: HttpResponse<Blob>) => {
          if (res.body) {
            const blob = new Blob([res.body], { type: 'text/csv' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `recap-produit-vendu-${new Date().getTime()}.csv`;
            link.click();
            window.URL.revokeObjectURL(url);
          }
        },
      });
  }

  protected createSuggestion(): void {
    const requestParam = this.buildRequestParam();
    this.recapService.createSuggestionFromRecap(requestParam).subscribe({
      next: (res: HttpResponse<number>) => {
        this.alert().showInfo(`Nombre de produits suggérés ${res.body}`);
      },
    });
  }

  protected createInventory(): void {
    const requestParam = this.buildRequestParam();
    this.recapService.createInventoryFromRecap(requestParam).subscribe({
      next: (res: HttpResponse<number>) => {
        this.alert().showInfo(`Nombre de produits pris en compte ${res.body}`);
      },
    });
  }

  protected toggleHelpDrawer(): void {
    this.helpDrawerVisible.update(value => !value);
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

  private getFirstDayOfMonth(): Date {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  }
}
