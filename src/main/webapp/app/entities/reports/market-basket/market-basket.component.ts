import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpResponse } from '@angular/common/http';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DatePickerModule } from 'primeng/datepicker';
import { InputNumberModule } from 'primeng/inputnumber';
import { Tag } from 'primeng/tag';

// Services and Models
import { MarketBasketService } from '../services/market-basket.service';
import { IProductAssociation, IMarketBasketSummary } from 'app/shared/model/report/market-basket.model';
import { FloatLabel } from 'primeng/floatlabel';

@Component({
  selector: 'jhi-market-basket',
  standalone: true,
  imports: [CommonModule, FormsModule, ButtonModule, CardModule, SelectModule, ToolbarModule, DatePickerModule, InputNumberModule, Tag, FloatLabel],
  templateUrl: './market-basket.component.html',
  styleUrl: './market-basket.component.scss',
})
export default class MarketBasketComponent implements OnInit {
  private marketBasketService = inject(MarketBasketService);

  // Signals
  associations = signal<IProductAssociation[]>([]);
  summary = signal<IMarketBasketSummary | null>(null);
  isLoading = signal<boolean>(false);

  // Filter values
  startDate: Date = new Date(new Date().setMonth(new Date().getMonth() - 6));
  endDate: Date = new Date();
  minSupport: number = 1.0;
  minConfidence: number = 10.0;
  limit: number = 50;

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);

    const startDateStr = this.formatDate(this.startDate);
    const endDateStr = this.formatDate(this.endDate);

    // Load associations
    this.marketBasketService.getProductAssociations(startDateStr, endDateStr, this.minSupport, this.minConfidence, this.limit).subscribe({
      next: (res: HttpResponse<IProductAssociation[]>) => {
        this.associations.set(res.body ?? []);
      },
      error: () => {
        this.associations.set([]);
      },
    });

    // Load summary
    this.marketBasketService.getSummary(startDateStr, endDateStr).subscribe({
      next: (res: HttpResponse<IMarketBasketSummary>) => {
        this.summary.set(res.body ?? null);
        this.isLoading.set(false);
      },
      error: () => {
        this.summary.set(null);
        this.isLoading.set(false);
      },
    });
  }

  onFilterChange(): void {
    this.loadData();
  }

  formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatNumber(value: number | undefined): string {
    if (!value) return '0';
    return new Intl.NumberFormat('fr-FR').format(value);
  }

  formatPercent(value: number | undefined): string {
    if (!value) return '0.00';
    return value.toFixed(2);
  }

  formatDecimal(value: number | undefined): string {
    if (!value) return '0.00';
    return value.toFixed(2);
  }

  getLiftSeverity(lift: number | undefined): 'success' | 'warn' | 'danger' | 'secondary' {
    if (!lift) return 'secondary';
    if (lift >= 2.0) return 'success';
    if (lift >= 1.0) return 'warn';
    return 'danger';
  }

  getConfidenceSeverity(confidence: number | undefined): 'success' | 'warn' | 'danger' | 'secondary' {
    if (!confidence) return 'secondary';
    if (confidence >= 50) return 'success';
    if (confidence >= 25) return 'warn';
    return 'danger';
  }

  getLiftLabel(lift: number | undefined): string {
    if (!lift) return 'Neutre';
    if (lift >= 2.0) return 'Très forte';
    if (lift >= 1.5) return 'Forte';
    if (lift >= 1.0) return 'Positive';
    return 'Faible';
  }
}
