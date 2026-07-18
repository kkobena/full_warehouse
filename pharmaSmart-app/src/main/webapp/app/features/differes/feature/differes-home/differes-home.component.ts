import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { FormsModule } from "@angular/forms";
import { DatePickerModule } from "primeng/datepicker";
import { FloatLabelModule } from "primeng/floatlabel";
import { SelectModule } from "primeng/select";
import { Toolbar } from "primeng/toolbar";
import { ButtonModule } from "primeng/button";

import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";

import { AbilityService } from "app/core/auth/ability.service";
import { DiffereStore } from "../../data-access/store/differe.store";
import { DiffereApiService } from "../../data-access/services/differe-api.service";
import { IDiffereSearchParams } from "../../data-access/models";
import { DiffereKpiBannerComponent } from "../../ui/differe-kpi-banner/differe-kpi-banner.component";
import { DiffereListComponent } from "../../ui/differe-list/differe-list.component";
import { DiffereDetailPanelComponent } from "../../ui/differe-detail-panel/differe-detail-panel.component";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";

type StatutDiffere = "PAYE" | "IMPAYE";

@Component({
  selector: "app-differes-home",
  imports: [
    FormsModule,
    Toolbar,
    ButtonModule,
    DatePickerModule,
    FloatLabelModule,
    SelectModule,
    DiffereKpiBannerComponent,
    DiffereListComponent,
    DiffereDetailPanelComponent
  ],
  templateUrl: "./differes-home.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./differes-home.component.scss"
})
export class DifferesHomeComponent implements OnInit {
  protected readonly store = inject(DiffereStore);
  protected readonly panelOpen = computed(() => this.store.panelOpen());

  private readonly ability = inject(AbilityService);
  protected readonly canExecute = this.ability.canSignal("execute", "differes");
  protected readonly canExport = this.ability.canSignal("export", "differes");

  // Toolbar state
  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected customerId: number | null = null;
  protected statut: StatutDiffere = "IMPAYE";
  protected readonly typesDifferes: { id: StatutDiffere; label: string }[] = [
    { id: "IMPAYE", label: "En cours" },
    { id: "PAYE", label: "Soldé" }
  ];
  protected loadingPdf = false;

  // Signal transmis à la liste
  protected readonly currentSearchParams = signal<IDiffereSearchParams | null>(null);

  // Hint premier usage
  protected readonly showHint = signal<boolean>(localStorage.getItem("differes-hint-dismissed") !== "1");

  private readonly differeApiService = inject(DiffereApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly blobDownload = inject(BlobDownloadService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngOnInit(): void {
    if (!this.store.clientsLoaded()) {
      this.loadClients();
    }
    this.onSearch();
  }

  onSearch(): void {
    this.currentSearchParams.set(this.buildParams());
  }

  exportPdf(): void {
    this.loadingPdf = true;
    this.differeApiService
      .exportListToPdf(this.buildParams())
      .pipe(
        finalize(() => (this.loadingPdf = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => {
          this.blobDownload.downloadPdf(blob, "differes");
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Export PDF")
      });
  }

  dismissHint(): void {
    localStorage.setItem("differes-hint-dismissed", "1");
    this.showHint.set(false);
  }

  private loadClients(): void {
    this.differeApiService
      .findClients()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.store.setClients(res.body ?? []),
        error: () => this.store.setClients([])
      });
  }

  private buildParams(): IDiffereSearchParams {
    const params: IDiffereSearchParams = {
      paymentStatuses: [this.statut],
      fromDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      toDate: DATE_FORMAT_ISO_DATE(this.modelEndDate)
    };
    if (this.customerId) {
      params.customerId = this.customerId;
    }
    this.store.setSearchParams(params);
    return params;
  }
}
