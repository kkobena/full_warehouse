import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { ITopProduct } from 'app/shared/model/report/top-product.model';
import { TopProductsReportService } from '../services/top-products-report.service';
import { Tag } from 'primeng/tag';
import { PrimeNG } from 'primeng/config';
import { TranslateService } from '@ngx-translate/core';
import { DATE_FORMAT_ISO_DATE, retriveMonthLabel } from '../../../shared/util/warehouse-util';

interface ITopProductRanked extends ITopProduct {
  rankDelta: number | null; // null = produit nouveau ce mois (absent M-1)
}

@Component({
  selector: 'jhi-top-products',
  templateUrl: './top-products.component.html',
  styleUrl: './top-products.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DatePicker,
    InputNumberModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    NgbNavModule,
    WarehouseCommonModule,
    Tag,
  ],
})
export default class TopProductsComponent implements OnInit {
  protected topProductsByRevenue = signal<ITopProductRanked[]>([]);
  protected topProductsByQuantity = signal<ITopProductRanked[]>([]);
  protected isLoading = signal<boolean>(false);
  protected selectedMonth = signal<Date | null>(null);
  protected limit = signal<number>(20);

  protected limitOptions = [
    { label: 'Top 10', value: 10 },
    { label: 'Top 20', value: 20 },
    { label: 'Top 50', value: 50 },
    { label: 'Top 100', value: 100 },
  ];
  private readonly primeNGConfig = inject(PrimeNG);
  private readonly translate = inject(TranslateService);
  private readonly topProductsService = inject(TopProductsReportService);

  ngOnInit(): void {
    this.translate.use('fr');
    this.translate.stream('primeng').subscribe(data => {
      this.primeNGConfig.setTranslation(data);
    });
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    this.selectedMonth.set(firstDayOfMonth);
    this.loadTopProducts();
  }

  protected loadTopProducts(): void {
    if (!this.selectedMonth()) return;

    this.isLoading.set(true);
    const monthStr = this.formatDate(this.selectedMonth()!);
    const prevDate = new Date(this.selectedMonth()!);
    prevDate.setMonth(prevDate.getMonth() - 1);
    const prevMonthStr = this.formatDate(prevDate);
    const limit = this.limit();

    forkJoin({
      currRevenue: this.topProductsService.getTopProductsByRevenue(monthStr, limit),
      prevRevenue: this.topProductsService.getTopProductsByRevenue(prevMonthStr, limit),
      currQty: this.topProductsService.getTopProductsByQuantity(monthStr, limit),
      prevQty: this.topProductsService.getTopProductsByQuantity(prevMonthStr, limit),
    }).subscribe({
      next: ({ currRevenue, prevRevenue, currQty, prevQty }) => {
        const prevRevenueRanks = this.buildRankMap(prevRevenue.body ?? []);
        const prevQtyRanks = this.buildRankMap(prevQty.body ?? []);
        this.topProductsByRevenue.set(this.applyRankDeltas(currRevenue.body ?? [], prevRevenueRanks));
        this.topProductsByQuantity.set(this.applyRankDeltas(currQty.body ?? [], prevQtyRanks));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  protected onFilterChange(): void {
    this.loadTopProducts();
  }

  protected getTotalRevenue(): number {
    return this.topProductsByRevenue().reduce((sum, p) => sum + (p.caGenere || 0), 0);
  }

  protected getTotalQuantity(): number {
    return this.topProductsByQuantity().reduce((sum, p) => sum + (p.qteVendue || 0), 0);
  }

  protected getTotalSales(): number {
    return this.topProductsByRevenue().reduce((sum, p) => sum + (p.nbVentes || 0), 0);
  }

  private buildRankMap(products: ITopProduct[]): Map<number, number> {
    const map = new Map<number, number>();
    products.forEach((p, i) => { if (p.produitId) map.set(p.produitId, i + 1); });
    return map;
  }

  private applyRankDeltas(products: ITopProduct[], prevRanks: Map<number, number>): ITopProductRanked[] {
    return products.map((p, i) => ({
      ...p,
      rankDelta: p.produitId != null && prevRanks.has(p.produitId)
        ? prevRanks.get(p.produitId)! - (i + 1)
        : null,
    }));
  }

  private formatDate(date: Date): string {
    return DATE_FORMAT_ISO_DATE(date);
  }

  getMonthLabel(): string {
    if (!this.selectedMonth()) return '';
    return retriveMonthLabel(this.selectedMonth());
  }
}
