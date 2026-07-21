import { NGB_DATE_TO_ISO } from '../../../shared/util/warehouse-util';
import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";


import { IDailySalesSummary } from "app/shared/model/report/daily-sales-summary.model";
import { SalesSummaryReportService } from "../services/sales-summary-report.service";
import { NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import {
  BadgeComponent,
  ButtonComponent,
  DataTableComponent,
  SelectComponent,
  ToolbarComponent
} from '../../../shared/ui';
import { PharmaDatePickerComponent } from '../../../shared/date-picker/pharma-date-picker.component';

@Component({
  selector: "app-sales-summary",
  templateUrl: "./sales-summary.component.html",
  styleUrl: "./sales-summary.component.scss",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    CommonModule,
    FormsModule,
    BadgeComponent,
    ButtonComponent,
    DataTableComponent,
    SelectComponent,
    ToolbarComponent,
    PharmaDatePickerComponent
  ]
})
export default class SalesSummaryComponent implements OnInit {
  summaries = signal<IDailySalesSummary[]>([]);
  isLoading = signal<boolean>(false);
  startDate = signal<NgbDateStruct | null>(null);
  endDate = signal<NgbDateStruct | null>(null);
  selectedTypeVente = signal<string | null>(null);

  typeVenteOptions = [
    { label: "Tous", value: null },
    { label: "Vente ordonnancées (VO)", value: "ThirdPartySales" },
    { label: "Vente au comptant (VNO)", value: "CashSale" },
    { label: "Vente aux dépôts", value: "VenteDepot" }
  ];
  private readonly salesSummaryService = inject(SalesSummaryReportService);

  ngOnInit(): void {

    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    this.startDate.set({ year: firstDay.getFullYear(), month: firstDay.getMonth() + 1, day: firstDay.getDate() });
    this.endDate.set({ year: lastDay.getFullYear(), month: lastDay.getMonth() + 1, day: lastDay.getDate() });

    this.loadSummaries();
  }

  loadSummaries(): void {
    if (!this.startDate() || !this.endDate()) {
      return;
    }

    this.isLoading.set(true);
    const startDateStr = NGB_DATE_TO_ISO(this.startDate())!;
    const endDateStr = NGB_DATE_TO_ISO(this.endDate())!;
    const typeVente = this.selectedTypeVente();

    const request = typeVente
      ? this.salesSummaryService.getDailySalesSummaryByType(startDateStr, endDateStr, typeVente)
      : this.salesSummaryService.getDailySalesSummary(startDateStr, endDateStr);

    request.subscribe({
      next: (res: HttpResponse<IDailySalesSummary[]>) => {
        this.summaries.set(res.body ?? []);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.loadSummaries();
  }

  getTotalCA(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caTotal || 0), 0);
  }

  getTotalCANet(): number {
    return this.summaries().reduce((sum, item) => sum + (item.caNet || 0), 0);
  }

  getTotalVentes(): number {
    return this.summaries().reduce((sum, item) => sum + (item.nbVentes || 0), 0);
  }

  getAveragePanier(): number {
    const total = this.getTotalCA();
    const count = this.getTotalVentes();
    return count > 0 ? total / count : 0;
  }


  getSeverityForType(type: string | undefined): string {
    if (!type) return "secondary";
    switch (type) {
      case "VO":
        return "info";
      case "VNO":
        return "success";
      case "VENTES_DEPOTS":
        return "warn";
      default:
        return "secondary";
    }
  }
}
