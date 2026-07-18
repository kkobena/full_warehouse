import { Component, computed, DestroyRef, inject, OnInit, signal, TemplateRef, ViewChild, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { DecimalPipe } from "@angular/common";
import { finalize } from "rxjs/operators";

import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { BadgeModule } from "primeng/badge";
import { ButtonModule } from "primeng/button";
import { DatePicker } from "primeng/datepicker";
import { FloatLabelModule } from "primeng/floatlabel";
import { InputTextModule } from "primeng/inputtext";
import { SelectModule } from "primeng/select";
import { TableModule } from "primeng/table";
import { SplitButton } from "primeng/splitbutton";
import { Toolbar } from "primeng/toolbar";
import { Toast } from "primeng/toast";

import { MenuItem } from "primeng/api";
import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { ITiersPayant } from "../../../../shared/model";
import { IAvoir, IFacture } from "../../data-access/models";
import { AvoirApiService } from "../../data-access/services/avoir-api.service";
import { FactureApiService } from "../../data-access/services/facture-api.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { AvoirFormModalComponent } from "../../ui/avoir-form-modal/avoir-form-modal.component";
import { TranslateService } from "@ngx-translate/core";
import { PrimeNG } from "primeng/config";
import { Tooltip } from "primeng/tooltip";

interface IStatutOption {
  label: string;
  value: string;
}

interface IKpiGroup {
  count: number;
  total: number;
}

@Component({
  selector: "app-avoir",
  imports: [
    FormsModule,
    DecimalPipe,
    SplitButton,
    Toolbar,
    BadgeModule,
    ButtonModule,
    DatePicker,
    FloatLabelModule,
    InputTextModule,
    AutoCompleteModule,
    SelectModule,
    TableModule,
    Toast,
    Tooltip
  ],
  templateUrl: "./avoir.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./avoir.component.scss",
})
export class AvoirComponent implements OnInit {
  @ViewChild("imputerModal") imputerModalTpl!: TemplateRef<any>;
  @ViewChild("annulerModal") annulerModalTpl!: TemplateRef<any>;

  protected readonly statutOptions: IStatutOption[] = [
    { label: "Tous", value: "" },
    { label: "Brouillon", value: "DRAFT" },
    { label: "Émis", value: "EMIS" },
    { label: "Imputé", value: "IMPUTE" },
    { label: "Annulé", value: "ANNULE" },
  ];

  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected selectedStatut = "";
  protected numAvoirSearch = "";
  protected tiersPayantSuggestions: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] = [];

  protected readonly avoirs = signal<IAvoir[]>([]);
  protected readonly loading = signal(false);

  // Imputation dialog
  protected currentImputerAvoir: IAvoir | null = null;
  protected selectedTargetFacture: IFacture | null = null;
  protected factureCibleSuggestions: IFacture[] = [];

  // Annulation dialog
  protected currentAnnulerAvoir: IAvoir | null = null;
  protected motifAnnulation = "";

  protected readonly kpi = computed<Record<string, IKpiGroup>>(() => {
    const groups: Record<string, IKpiGroup> = {
      DRAFT:  { count: 0, total: 0 },
      EMIS:   { count: 0, total: 0 },
      IMPUTE: { count: 0, total: 0 },
      ANNULE: { count: 0, total: 0 },
    };
    this.avoirs().forEach(a => {
      const s = a.statut ?? "DRAFT";
      if (groups[s]) {
        groups[s].count++;
        groups[s].total += a.montantAvoir ?? 0;
      }
    });
    return groups;
  });

  protected exportingExcel = false;
  protected exportingListPdf = false;
  protected exportingPdf = signal<number | null>(null);

  protected readonly exportMenuItems: MenuItem[] = [
    {
      label: 'Excel',
      icon: 'pi pi-file-excel',
      command: () => this.onExportExcel(),
    },
    {
      label: 'PDF',
      icon: 'pi pi-file-pdf',
      command: () => this.onExportListPdf(),
    },
  ];

  private readonly api = inject(AvoirApiService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly notificationService = inject(NotificationService);
  private readonly downloadService = inject(BlobDownloadService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    const translate = inject(TranslateService);
    const primeNGConfig = inject(PrimeNG);
    translate.use("fr");
    translate.stream("primeng")
      .pipe(takeUntilDestroyed())
      .subscribe({ next: data => primeNGConfig.setTranslation(data) });

    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngOnInit(): void {
    this.onSearch();
  }

  onSearch(): void {
    this.loading.set(true);
    this.api
      .query(this.buildParams())
      .pipe(finalize(() => this.loading.set(false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.avoirs.set(res.body ?? []),
        error: () => this.notificationService.error("Erreur lors du chargement des avoirs"),
      });
  }

  openNouvelAvoir(prefillFactureId?: number, prefillFactureDate?: string, prefillTiersPayantId?: number): void {
    const ref = this.modalService.open(AvoirFormModalComponent, { size: "lg", centered: true, backdrop: "static" });
    ref.componentInstance.prefillFactureId = prefillFactureId;
    ref.componentInstance.prefillFactureDate = prefillFactureDate;
    ref.componentInstance.prefillTiersPayantId = prefillTiersPayantId;
    ref.result.then(
      (avoir: IAvoir) => {
        this.avoirs.set([avoir, ...this.avoirs()]);
        this.notificationService.success("Avoir créé");
      },
      () => {},
    );
  }

  onEmettre(avoir: IAvoir): void {
    if (!avoir.id) return;
    this.api.emettre(avoir.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.replaceAvoir(res.body!);
        this.notificationService.success("Avoir émis");
      },
      error: () => this.notificationService.error("Erreur lors de l'émission"),
    });
  }

  onOpenImputer(avoir: IAvoir): void {
    this.currentImputerAvoir = avoir;
    this.selectedTargetFacture = null;
    this.factureCibleSuggestions = [];
    const ref = this.modalService.open(this.imputerModalTpl, { size: "md", centered: true, backdrop: "static" });
    ref.result.then(() => this.doImputer()).catch(() => {});
  }

  private doImputer(): void {
    const avoir = this.currentImputerAvoir;
    const facture = this.selectedTargetFacture;
    if (!avoir?.id || !facture?.factureItemId) return;
    this.api.imputer(avoir.id, facture.factureItemId.id, facture.factureItemId.invoiceDate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.avoirs.set(this.avoirs().map(a => a.id === avoir.id ? { ...a, statut: "IMPUTE" } : a));
          this.notificationService.success("Avoir imputé");
        },
        error: () => this.notificationService.error("Erreur lors de l'imputation"),
      });
  }

  onOpenAnnuler(avoir: IAvoir): void {
    this.currentAnnulerAvoir = avoir;
    this.motifAnnulation = "";
    const ref = this.modalService.open(this.annulerModalTpl, { size: "sm", centered: true });
    ref.result.then((motif: string) => this.doAnnuler(avoir, motif)).catch(() => {});
  }

  private doAnnuler(avoir: IAvoir, motif: string): void {
    if (!avoir.id) return;
    this.api.annuler(avoir.id, motif).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.avoirs.set(this.avoirs().map(a => a.id === avoir.id ? { ...a, statut: "ANNULE" } : a));
        this.notificationService.success("Avoir annulé");
      },
      error: () => this.notificationService.error("Erreur lors de l'annulation"),
    });
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayantSuggestions = res.body ?? []));
  }

  searchFactureCible(event: { query: string }): void {
    const tp = this.currentImputerAvoir?.tiersPayantId;
    const toIso = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    const today = new Date();
    const twoYearsAgo = new Date(today.getFullYear() - 2, today.getMonth(), today.getDate());
    this.factureApiService
      .query({
        search: event.query,
        startDate: toIso(twoYearsAgo),
        endDate: toIso(today),
        statuts: ["PARTIALLY_PAID"],
        tiersPayantIds: tp ? [tp] : [],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.factureCibleSuggestions = res.body ?? []));
  }

  canConfirmImputer(): boolean {
    if (!this.selectedTargetFacture || !this.currentImputerAvoir) return false;
    return (this.currentImputerAvoir.montantAvoir ?? 0) <= (this.selectedTargetFacture.montantRestant ?? 0);
  }

  getStatutSeverity(statut?: string): "secondary" | "info" | "success" | "danger" | "warn" {
    switch (statut) {
      case "DRAFT":  return "secondary";
      case "EMIS":   return "info";
      case "IMPUTE": return "success";
      case "ANNULE": return "danger";
      default:       return "secondary";
    }
  }

  getStatutLabel(statut?: string): string {
    switch (statut) {
      case "DRAFT":  return "Brouillon";
      case "EMIS":   return "Émis";
      case "IMPUTE": return "Imputé";
      case "ANNULE": return "Annulé";
      default:       return statut ?? "—";
    }
  }

  onExportExcel(): void {
    this.exportingExcel = true;
    this.api.exportExcel(this.buildParams())
      .pipe(finalize(() => (this.exportingExcel = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.downloadService.downloadExcel(blob, `avoirs_${DATE_FORMAT_ISO_DATE(new Date())}`),
        error: () => this.notificationService.error("Erreur lors de l'export Excel"),
      });
  }

  onExportListPdf(): void {
    this.exportingListPdf = true;
    this.api.exportListPdf(this.buildParams())
      .pipe(finalize(() => (this.exportingListPdf = false)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.downloadService.downloadPdf(blob, `avoirs_${DATE_FORMAT_ISO_DATE(new Date())}`),
        error: () => this.notificationService.error("Erreur lors de l'export PDF"),
      });
  }

  onExportPdf(avoir: IAvoir): void {
    if (!avoir.id) return;
    this.exportingPdf.set(avoir.id);
    this.api.exportPdf(avoir.id)
      .pipe(finalize(() => this.exportingPdf.set(null)), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.downloadService.downloadPdf(blob, `avoir_${avoir.numAvoir ?? avoir.id}`),
        error: () => this.notificationService.error("Erreur lors de l'export PDF"),
      });
  }

  private buildParams(): any {
    return {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id),
      statut: this.selectedStatut || undefined,
      numAvoir: this.numAvoirSearch || undefined,
    };
  }

  private replaceAvoir(updated: IAvoir): void {
    this.avoirs.set(this.avoirs().map(a => (a.id === updated.id ? updated : a)));
  }
}
