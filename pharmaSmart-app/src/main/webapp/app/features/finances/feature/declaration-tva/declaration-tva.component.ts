import { Component, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { NgbDateStruct } from "@ng-bootstrap/ng-bootstrap";
import { ButtonComponent, DataTableComponent, SelectComponent, ToolbarComponent } from "../../../../shared/ui";
import { PharmaDatePickerComponent } from "../../../../shared/date-picker/pharma-date-picker.component";
import { NGB_DATE_TO_ISO, TODAY_NGB_DATE } from "../../../../shared/util/warehouse-util";
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
    ButtonComponent,
    PharmaDatePickerComponent,
    DataTableComponent,
    ToolbarComponent,
    SelectComponent
  ],
  templateUrl: "./declaration-tva.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./declaration-tva.component.scss"
})
export class DeclarationTvaComponent implements OnInit {
  startDate = signal<NgbDateStruct>(this.defaultStartDate());
  endDate = signal<NgbDateStruct>(TODAY_NGB_DATE());
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
        startDate: NGB_DATE_TO_ISO(this.startDate()),
        endDate: NGB_DATE_TO_ISO(this.endDate()),
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
        startDate: NGB_DATE_TO_ISO(this.startDate()),
        endDate: NGB_DATE_TO_ISO(this.endDate()),
        typeTva: this.selectedType() || undefined
      })
      .subscribe(res => {
        this.blobDownload.downloadPdf(res.body, "declaration-tva-");
      });
  }

  private defaultStartDate(): NgbDateStruct {
    const d = new Date();
    d.setDate(1);
    return { year: d.getFullYear(), month: d.getMonth() + 1, day: d.getDate() };
  }
}
