import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { SelectModule } from 'primeng/select';
import { ToolbarModule } from 'primeng/toolbar';
import { DividerModule } from 'primeng/divider';
import { Tag } from 'primeng/tag';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { ICustomerSegmentation, CustomerClassification } from 'app/shared/model/report/customer-segmentation.model';
import { CustomerSegmentationReportService } from '../services/customer-segmentation-report.service';

@Component({
  selector: 'jhi-customer-segmentation',
  templateUrl: './customer-segmentation.component.html',
  styleUrl: './customer-segmentation.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    Card,
    SelectModule,
    ToolbarModule,
    DividerModule,
    WarehouseCommonModule,
    Tag
  ]
})
export default class CustomerSegmentationComponent implements OnInit {
  customers = signal<ICustomerSegmentation[]>([]);
  classificationCounts = signal<{ [key in CustomerClassification]?: number }>({});
  isLoading = signal<boolean>(false);
  selectedClassification = signal<CustomerClassification | null>(null);
  showChampionsOnly = signal<boolean>(false);
  showAtRiskOnly = signal<boolean>(false);

  classificationOptions = [
    { label: 'Toutes les classifications', value: null },
    { label: 'Champions', value: CustomerClassification.CHAMPION },
    { label: 'Fidèles', value: CustomerClassification.LOYAL },
    { label: 'Gros dépensiers', value: CustomerClassification.BIG_SPENDER },
    { label: 'Actifs', value: CustomerClassification.ACTIVE },
    { label: 'À risque', value: CustomerClassification.AT_RISK },
    { label: 'Besoin d\'attention', value: CustomerClassification.NEED_ATTENTION },
    { label: 'Inactifs', value: CustomerClassification.INACTIVE },
  ];

  private readonly customerSegmentationService = inject(CustomerSegmentationReportService);

  ngOnInit(): void {
    this.loadCustomers();
    this.loadClassificationCounts();
  }

  loadCustomers(): void {
    this.isLoading.set(true);
    const classification = this.selectedClassification();
    const championsOnly = this.showChampionsOnly();
    const atRiskOnly = this.showAtRiskOnly();

    let request;
    if (championsOnly) {
      request = this.customerSegmentationService.getChampionCustomers();
    } else if (atRiskOnly) {
      request = this.customerSegmentationService.getAtRiskCustomers();
    } else if (classification) {
      request = this.customerSegmentationService.getCustomersByClassification(classification);
    } else {
      request = this.customerSegmentationService.getAllCustomerSegmentation();
    }

    request.subscribe({
      next: (res: HttpResponse<ICustomerSegmentation[]>) => {
        this.customers.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  loadClassificationCounts(): void {
    this.customerSegmentationService.getCustomerCountByClassification().subscribe({
      next: (res: HttpResponse<{ [key in CustomerClassification]: number }>) => {
        this.classificationCounts.set(res.body ?? {});
      },
      error: () => {
        console.error('Error loading classification counts');
      },
    });
  }

  onFilterChange(): void {
    if (this.showChampionsOnly() || this.showAtRiskOnly()) {
      this.selectedClassification.set(null);
    }
    this.loadCustomers();
  }

  onChampionsToggle(): void {
    this.showChampionsOnly.update(value => !value);
    if (this.showChampionsOnly()) {
      this.showAtRiskOnly.set(false);
    }
    this.onFilterChange();
  }

  onAtRiskToggle(): void {
    this.showAtRiskOnly.update(value => !value);
    if (this.showAtRiskOnly()) {
      this.showChampionsOnly.set(false);
    }
    this.onFilterChange();
  }

  onClearFilters(): void {
    this.selectedClassification.set(null);
    this.showChampionsOnly.set(false);
    this.showAtRiskOnly.set(false);
    this.loadCustomers();
  }

  exportToPdf(): void {
    this.customerSegmentationService.exportCustomerSegmentationToPdf().subscribe({
      next: (res: HttpResponse<Blob>) => {
        if (res.body) {
          const blob = new Blob([res.body], { type: 'application/pdf' });
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `segmentation-clients-${new Date().getTime()}.pdf`;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: () => {
        console.error('Error exporting PDF');
      },
    });
  }

  getTotalCustomers(): number {
    return this.customers().length;
  }

  getTotalSpent(): number {
    return this.customers().reduce((sum, item) => sum + (item.totalSpentLastYear || 0), 0);
  }

  getAverageBasket(): number {
    const customers = this.customers().filter(c => c.avgBasketValue && c.avgBasketValue > 0);
    if (customers.length === 0) return 0;
    const sum = customers.reduce((acc, c) => acc + (c.avgBasketValue || 0), 0);
    return sum / customers.length;
  }

  getAverageRFMScore(): number {
    const customers = this.customers().filter(c => c.rfmSegment);
    if (customers.length === 0) return 0;
    const sum = customers.reduce((acc, c) => acc + (c.rfmSegment || 0), 0);
    return sum / customers.length;
  }

  getClassificationSeverity(classification: CustomerClassification | undefined): string {
    if (!classification) return 'secondary';
    switch (classification) {
      case CustomerClassification.CHAMPION:
        return 'success';
      case CustomerClassification.LOYAL:
        return 'success';
      case CustomerClassification.BIG_SPENDER:
        return 'info';
      case CustomerClassification.ACTIVE:
        return 'info';
      case CustomerClassification.AT_RISK:
        return 'warn';
      case CustomerClassification.NEED_ATTENTION:
        return 'warn';
      case CustomerClassification.INACTIVE:
        return 'danger';
      default:
        return 'secondary';
    }
  }

  getClassificationLabel(classification: CustomerClassification | undefined): string {
    if (!classification) return 'N/A';
    switch (classification) {
      case CustomerClassification.CHAMPION:
        return 'Champion';
      case CustomerClassification.LOYAL:
        return 'Fidèle';
      case CustomerClassification.BIG_SPENDER:
        return 'Gros dépensier';
      case CustomerClassification.ACTIVE:
        return 'Actif';
      case CustomerClassification.AT_RISK:
        return 'À risque';
      case CustomerClassification.NEED_ATTENTION:
        return 'Besoin d\'attention';
      case CustomerClassification.INACTIVE:
        return 'Inactif';
      default:
        return classification;
    }
  }

  getRFMScoreSeverity(score: number | undefined): string {
    if (!score) return 'secondary';
    if (score >= 9) return 'success';
    if (score >= 6) return 'info';
    if (score >= 3) return 'warn';
    return 'danger';
  }

  getRecencySeverity(days: number | undefined): string {
    if (!days) return 'secondary';
    if (days <= 30) return 'success';
    if (days <= 90) return 'info';
    if (days <= 180) return 'warn';
    return 'danger';
  }
}
