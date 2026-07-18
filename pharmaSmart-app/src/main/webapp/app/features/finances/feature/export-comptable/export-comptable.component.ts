import { Component, inject, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { DatePicker } from "primeng/datepicker";
import { FloatLabel } from "primeng/floatlabel";
import { CheckboxModule } from "primeng/checkbox";
import { BlobDownloadService, DownloadFormat } from "app/shared/services/blob-download.service";
import { ExportComptableApiService } from "../../data-access/services/export-comptable-api.service";

interface FormatOption {
  label: string;
  value: string;
  icon: string;
}

@Component({
  selector: "app-export-comptable",
  imports: [CommonModule, FormsModule, DatePicker, FloatLabel, CheckboxModule],
  templateUrl: "./export-comptable.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./export-comptable.component.scss"
})
export class ExportComptableComponent {
  startDate = signal<Date>(this.defaultStartDate());
  endDate = signal<Date>(new Date());
  isLoading = signal(false);

  includeVentes = true;
  includeAchats = true;
  includeMvtCaisse = true;
  includeTiersPayant = true;
  includeDifferes = true;
  includeTva = true;

  formatOptions: FormatOption[] = [
    { label: "Excel", value: "excel", icon: "pi pi-file-excel" },
    { label: "CSV SYSCOHADA", value: "csv", icon: "pi pi-file" },
    { label: "PDF récapitulatif", value: "pdf", icon: "pi pi-file-pdf" }
  ];

  private readonly api = inject(ExportComptableApiService);
  private readonly blobDownload = inject(BlobDownloadService);

  generateExport(format: string): void {
    const fmt = format as DownloadFormat;
    this.blobDownload.downloadFromObservable(
      this.api.export({
        startDate: this.formatDate(this.startDate()),
        endDate: this.formatDate(this.endDate()),
        format: fmt,
        ventes: this.includeVentes,
        achats: this.includeAchats,
        mvtCaisse: this.includeMvtCaisse,
        tiersPayant: this.includeTiersPayant,
        differes: this.includeDifferes,
        tva: this.includeTva
      }),
      `export-comptable-${Date.now()}`,
      fmt,
      () => this.isLoading.set(true),
      () => this.isLoading.set(false)
    );
  }

  private defaultStartDate(): Date {
    const d = new Date();
    d.setDate(1);
    return d;
  }

  private formatDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }
}
