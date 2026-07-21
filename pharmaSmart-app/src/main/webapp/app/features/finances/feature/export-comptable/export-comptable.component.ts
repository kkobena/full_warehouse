import { Component, inject, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { CheckboxComponent } from "../../../../shared/ui";
import { TODAY_NGB_DATE, NGB_DATE_TO_ISO } from "../../../../shared/util/warehouse-util";
import { BlobDownloadService, DownloadFormat } from "app/shared/services/blob-download.service";
import { ExportComptableApiService } from "../../data-access/services/export-comptable-api.service";

interface FormatOption {
  label: string;
  value: string;
  icon: string;
}

@Component({
  selector: "app-export-comptable",
  imports: [CommonModule, FormsModule, PharmaDatePickerComponent, CheckboxComponent],
  templateUrl: "./export-comptable.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./export-comptable.component.scss"
})
export class ExportComptableComponent {
  startDate = signal<NgbDateStruct>(this.defaultStartDate());
  endDate = signal<NgbDateStruct>(TODAY_NGB_DATE());
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
        startDate: NGB_DATE_TO_ISO(this.startDate()),
        endDate: NGB_DATE_TO_ISO(this.endDate()),
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

  private defaultStartDate(): NgbDateStruct {
    const d = new Date();
    d.setDate(1);
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }
}
