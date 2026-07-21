import { Component, inject, OnInit, output, signal, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import DashboardCAComponent from 'app/entities/reports/dashboard-ca/dashboard-ca.component';
import { FinancesDashboardApiService } from '../../data-access/services/finances-dashboard-api.service';
import { IFinancesSummary } from '../../data-access/models';
import { formatCurrency } from 'app/shared/utils/format-utils';

@Component({
  selector: 'app-finances-dashboard',
  imports: [CommonModule, RouterLink, DashboardCAComponent],
  templateUrl: './finances-dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './finances-dashboard.component.scss',
})
export class FinancesDashboardComponent implements OnInit {
  readonly navigateToTab = output<string>();

  summary = signal<IFinancesSummary | null>(null);
  isLoading = signal(false);

  formatCurrency = formatCurrency;

  private readonly api = inject(FinancesDashboardApiService);

  ngOnInit(): void {
    this.loadSummary();
  }

  loadSummary(): void {
    this.isLoading.set(true);
    this.api.getSummaryFinances().subscribe({
      next: res => {
        this.summary.set(res.body);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  goToComptesFournisseurs(): void {
    this.navigateToTab.emit('comptes-fournisseurs');
  }
}
