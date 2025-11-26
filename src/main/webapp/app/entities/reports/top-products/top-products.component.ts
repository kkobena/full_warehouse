import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
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

@Component({
  selector: 'jhi-top-products',
  templateUrl: './top-products.component.html',
  styleUrl: './top-products.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    Card,
    DatePicker,
    InputNumberModule,
    SelectModule,
    ToolbarModule,
    DividerModule,
    NgbNavModule,
    WarehouseCommonModule,
    Tag
  ]
})
export default class TopProductsComponent implements OnInit {
  topProductsByRevenue = signal<ITopProduct[]>([]);
  topProductsByQuantity = signal<ITopProduct[]>([]);
  isLoading = signal<boolean>(false);
  selectedMonth = signal<Date | null>(null);
  limit = signal<number>(20);

  limitOptions = [
    { label: 'Top 10', value: 10 },
    { label: 'Top 20', value: 20 },
    { label: 'Top 50', value: 50 },
    { label: 'Top 100', value: 100 },
  ];

  private readonly topProductsService = inject(TopProductsReportService);

  ngOnInit(): void {
    // Set default month to current month
    const now = new Date();
    const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    this.selectedMonth.set(firstDayOfMonth);

    this.loadTopProducts();
  }

  loadTopProducts(): void {
    if (!this.selectedMonth()) {
      return;
    }

    this.isLoading.set(true);
    const monthStr = this.formatDate(this.selectedMonth()!);
    const limit = this.limit();

    // Load both reports in parallel
    this.topProductsService.getTopProductsByRevenue(monthStr, limit).subscribe({
      next: (res: HttpResponse<ITopProduct[]>) => {
        this.topProductsByRevenue.set(res.body ?? []);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });

    this.topProductsService.getTopProductsByQuantity(monthStr, limit).subscribe({
      next: (res: HttpResponse<ITopProduct[]>) => {
        this.topProductsByQuantity.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.loadTopProducts();
  }

  getTotalRevenue(): number {
    return this.topProductsByRevenue().reduce((sum, product) => sum + (product.caGenere || 0), 0);
  }

  getTotalQuantity(): number {
    return this.topProductsByQuantity().reduce((sum, product) => sum + (product.qteVendue || 0), 0);
  }

  getTotalSales(): number {
    return this.topProductsByRevenue().reduce((sum, product) => sum + (product.nbVentes || 0), 0);
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  getMonthLabel(): string {
    if (!this.selectedMonth()) return '';
    const month = this.selectedMonth()!;
    return month.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
  }
}
