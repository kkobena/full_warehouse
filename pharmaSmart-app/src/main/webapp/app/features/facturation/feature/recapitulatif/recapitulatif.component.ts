import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { DatePipe, DecimalPipe } from "@angular/common";
import { finalize } from "rxjs/operators";


import { NotificationService } from "../../../../shared/services/notification.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { ITiersPayant } from "../../../../shared/model";
import {
  IRecapitulatifKpi,
  IRecapitulatifMensuelDto,
  IRecapitulatifMensuelRow,
  IRecapitulatifParams
} from "../../data-access/models";
import { RecapitulatifApiService } from "../../data-access/services/recapitulatif-api.service";
import { RecapitulatifKpiBannerComponent } from "../../ui/recapitulatif-kpi-banner/recapitulatif-kpi-banner.component";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import {
  ButtonComponent,
  DataTableComponent,
  FloatLabelComponent,
  MultiSelectComponent,
  SelectComponent,
  ToolbarComponent
} from "../../../../shared/ui";

interface IMoisOption {
  label: string;
  value: number;
}

interface ITypeFactureOption {
  label: string;
  value: string;
}

interface IAnneeOption {
  label: string;
  value: number;
}

@Component({
  selector: "app-recapitulatif",
  imports: [
    FormsModule,
    DecimalPipe,
    RecapitulatifKpiBannerComponent,
    DatePipe,
    ButtonComponent,
    DataTableComponent,
    FloatLabelComponent,
    MultiSelectComponent,
    SelectComponent,
    ToolbarComponent
  ],
  templateUrl: "./recapitulatif.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./recapitulatif.component.scss"
})
export class RecapitulatifComponent implements OnInit {
  protected readonly moisOptions: IMoisOption[] = [
    { label: "Janvier", value: 1 }, { label: "Février", value: 2 },
    { label: "Mars", value: 3 }, { label: "Avril", value: 4 },
    { label: "Mai", value: 5 }, { label: "Juin", value: 6 },
    { label: "Juillet", value: 7 }, { label: "Août", value: 8 },
    { label: "Septembre", value: 9 }, { label: "Octobre", value: 10 },
    { label: "Novembre", value: 11 }, { label: "Décembre", value: 12 }
  ];

  protected readonly typeFactureOptions: ITypeFactureOption[] = [
    { label: "Tous", value: "" },
    { label: "Individuelle", value: "INDIVIDUAL" },
    { label: "Groupée", value: "GROUPED" }
  ];

  protected readonly anneeOptions: IAnneeOption[] = this.generateAnneeOptions();

  protected annee: number = Math.max(new Date().getFullYear(), 2026);
  protected selectedMois: number = new Date().getMonth() + 1;
  protected selectedTypeFacture = "";
  protected tiersPayantSuggestions: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] = [];

  protected readonly rows = signal<IRecapitulatifMensuelDto[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadingExport = signal(false);
  protected readonly loadingPdf = signal(false);

  // Master/detail state
  protected readonly selectedRow = signal<IRecapitulatifMensuelDto | null>(null);
  protected readonly panelOpen = computed(() => this.selectedRow() !== null);
  protected readonly showHint = signal<boolean>(localStorage.getItem('recapitulatif-hint-dismissed') !== '1');
  protected readonly kpi = computed<IRecapitulatifKpi | null>(() => {
    const data = this.rows();
    if (data.length === 0) return null;
    const totalFacture = data.reduce((s, r) => s + (r.totalFacture ?? 0), 0);
    const totalRegle = data.reduce((s, r) => s + (r.totalRegle ?? 0), 0);
    return {
      totalFacture,
      totalRegle,
      totalRestant: data.reduce((s, r) => s + (r.soldeActuel ?? 0), 0),
      soldeCumule: data.reduce((s, r) => s + (r.soldeCumule ?? 0), 0),
      tauxRecouvrement: totalFacture > 0 ? (totalRegle / totalFacture) * 100 : 0,
      countFactures: data.reduce((s, r) => s + (r.nombreFactures ?? 0), 0),
      countImpayees: data.reduce((s, r) => s + (r.nombreImpayees ?? 0), 0)
    };
  });

  private readonly api = inject(RecapitulatifApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly downloadDocumentService = inject(BlobDownloadService);

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loading.set(true);
    this.selectedRow.set(null);
    this.api
      .query(this.buildParams())
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.rows.set(res.body ?? []),
        error: () => this.notificationService.error("Erreur lors du chargement du récapitulatif")
      });
  }

  onRowSelect(row: IRecapitulatifMensuelDto): void {
    if (this.selectedRow()?.tiersPayantId !== row.tiersPayantId) {
      this.selectedRow.set(row);
    }
  }

  closePanel(): void {
    this.selectedRow.set(null);
  }
  dismissHint(): void {
    localStorage.setItem('recapitulatif-hint-dismissed', '1');
    this.showHint.set(false);
  }

  onExportExcel(): void {
    this.loadingExport.set(true);
    this.api
      .exportExcel(this.buildParams())
      .pipe(
        finalize(() => this.loadingExport.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadExcel(blob, 'recapitulatif'),
        error: () => this.notificationService.error("Erreur export Excel")
      });
  }

  onExportPdf(): void {
    this.loadingPdf.set(true);
    this.api
      .exportPdf(this.buildParams())
      .pipe(
        finalize(() => this.loadingPdf.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: blob => this.downloadDocumentService.downloadPdf(blob, 'recapitulatif'),
        error: () => this.notificationService.error("Erreur export PDF")
      });
  }

  searchTiersPayant(query: string): void {
    this.tiersPayantService
      .query({ page: 0, search: query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayantSuggestions = res.body ?? []));
  }

  statutLabel(statut?: string): string {
    switch (statut) {
      case "PAID":
        return "Payé";
      case "NOT_PAID":
        return "Impayé";
      case "PARTIALLY_PAID":
        return "Partiel";
      default:
        return statut ?? "—";
    }
  }

  statutClass(statut?: string): string {
    switch (statut) {
      case "PAID":
        return "bg-success";
      case "NOT_PAID":
        return "bg-danger";
      case "PARTIALLY_PAID":
        return "bg-warning text-dark";
      default:
        return "bg-secondary";
    }
  }

  get totalFacture(): number {
    return this.kpi()?.totalFacture ?? 0;
  }

  get totalRegle(): number {
    return this.kpi()?.totalRegle ?? 0;
  }

  get totalRestant(): number {
    return this.kpi()?.totalRestant ?? 0;
  }

  get tauxGlobal(): number {
    return Math.round(this.kpi()?.tauxRecouvrement ?? 0);
  }

  detailTauxRecouvrement(row: IRecapitulatifMensuelDto): number {
    const tf = row.totalFacture ?? 0;
    return tf > 0 ? Math.round(((row.totalRegle ?? 0) / tf) * 100) : 0;
  }

  detailLignes(row: IRecapitulatifMensuelDto): IRecapitulatifMensuelRow[] {
    return row.lignes ?? [];
  }

  private generateAnneeOptions(): IAnneeOption[] {
    const years: IAnneeOption[] = [];
    const minYear = 2026;
    const actualYear = new Date().getFullYear();
    const topYear = Math.max(actualYear, minYear);

    for (let i = topYear; i >= minYear; i--) {
      years.push({ label: i.toString(), value: i });
    }
    return years;
  }

  private buildParams(): IRecapitulatifParams {
    return {
      annee: this.annee,
      mois: this.selectedMois,
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id),
      typeFacture: this.selectedTypeFacture || undefined
    };
  }


}
