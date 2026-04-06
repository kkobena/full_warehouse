import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { ToolbarModule } from 'primeng/toolbar';
import { FloatLabel } from 'primeng/floatlabel';
import { DividerModule } from 'primeng/divider';
import { WarehouseCommonModule } from '../../../shared/warehouse-common/warehouse-common.module';

import { IDailyCashRegisterReport } from 'app/shared/model/report/cash-register-report.model';
import { CashRegisterReportService } from '../services/cash-register-report.service';
import { BlobDownloadService } from "../../../shared/services/blob-download.service";

@Component({
  selector: 'jhi-cash-register-report',
  templateUrl: './cash-register-report.component.html',
  styleUrl: './cash-register-report.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    ButtonModule,
    DatePicker,
    ToolbarModule,
    FloatLabel,
    DividerModule,
    WarehouseCommonModule,
  ],
})
export default class CashRegisterReportComponent implements OnInit {
  dailyReports = signal<IDailyCashRegisterReport[]>([]);
  selectedDate = signal<Date>(new Date());
  isLoading = signal<boolean>(false);
  private readonly downloadService = inject(BlobDownloadService);
  private readonly cashRegisterService = inject(CashRegisterReportService);

  ngOnInit(): void {
    this.loadDailyReport();
  }

  loadDailyReport(): void {
    this.isLoading.set(true);
    const dateStr = this.formatDate(this.selectedDate());

    this.cashRegisterService.getDailyReport(dateStr).subscribe({
      next: (res: HttpResponse<IDailyCashRegisterReport[]>) => {
        this.dailyReports.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  onDateChange(): void {
    this.loadDailyReport();
  }

  exportToPdf(): void {
    const dateStr = this.formatDate(this.selectedDate());

    this.cashRegisterService.exportDailyReportToPdf(dateStr).subscribe(
      {
        next: (res: HttpResponse<Blob>) => {
          this.downloadService.downloadPdf(res.body,'cash-register-report');
        }
      }
    );
  }

  getTotalSales(): number {
    return this.dailyReports().reduce((sum, report) => sum + (report.totalSales ?? 0), 0);
  }

  getTotalDiscrepancy(): number {
    return this.dailyReports().reduce((sum, report) => sum + Math.abs(report.discrepancy ?? 0), 0);
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
