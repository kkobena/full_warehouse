import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { DatePicker } from "primeng/datepicker";
import { TableModule } from "primeng/table";
import { ToolbarModule } from "primeng/toolbar";
import { SelectModule } from "primeng/select";
import { FloatLabel } from "primeng/floatlabel";
import { DeclarationTvaApiService } from "../../data-access/services/declaration-tva-api.service";
import { IDeclarationTvaSummary } from "../../data-access/models";
import { formatCurrency } from "app/shared/utils/format-utils";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

interface TypeVenteOption {
  label: string;
  value: string;
}

@Component({
  selector: "app-declaration-tva",
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    DatePicker,
    TableModule,
    ToolbarModule,
    SelectModule,
    FloatLabel
  ],
  templateUrl: "./declaration-tva.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./declaration-tva.component.scss"
})
export class DeclarationTvaComponent implements OnInit {
  startDate = signal<Date>(this.defaultStartDate());
  endDate = signal<Date>(new Date());
  selectedType = signal<string>("");
  summary = signal<IDeclarationTvaSummary | null>(null);
  isLoading = signal(false);

  formatCurrency = formatCurrency;

  typeOptions: TypeVenteOption[] = [
    { label: "Toutes les ventes", value: "" },
    { label: "Ventes comptant", value: "COMPTANT" },
    { label: "Tiers payant", value: "ASSURANCE" },
    { label: "Différés", value: "DIFFERE" }
  ];

  private readonly api = inject(DeclarationTvaApiService);
  private readonly blobDownload = inject(BlobDownloadService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.api
      .getDeclaration({
        startDate: this.formatDate(this.startDate()),
        endDate: this.formatDate(this.endDate()),
        typeTva: this.selectedType() || undefined
      })
      .subscribe({
        next: res => {
          this.summary.set(res.body);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
        }
      });
  }

  exportPdf(): void {
    this.api
      .exportToPdf({
        startDate: this.formatDate(this.startDate()),
        endDate: this.formatDate(this.endDate()),
        typeTva: this.selectedType() || undefined
      })
      .subscribe(res => {
        this.blobDownload.downloadPdf(res.body, "declaration-tva-");
      });
  }

  private defaultStartDate(): Date {
    const d = new Date();
    d.setDate(1);
    return d;
  }

  private formatDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  }
}
