import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { DecimalPipe } from "@angular/common";
import { finalize } from "rxjs/operators";

import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { ButtonModule } from "primeng/button";
import { DatePicker } from "primeng/datepicker";
import { FloatLabelModule } from "primeng/floatlabel";
import { SelectModule } from "primeng/select";
import { TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { Toast } from "primeng/toast";

import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { ITiersPayant } from "../../../../shared/model";
import { IAvoir } from "../../data-access/models";
import { AvoirApiService } from "../../data-access/services/avoir-api.service";
import { AvoirFormModalComponent } from "../../ui/avoir-form-modal/avoir-form-modal.component";

interface IStatutOption {
  label: string;
  value: string;
}

@Component({
  selector: "app-avoir",
  imports: [
    FormsModule,
    DecimalPipe,
    Toolbar,
    ButtonModule,
    DatePicker,
    FloatLabelModule,
    AutoCompleteModule,
    SelectModule,
    TableModule,
    Toast
  ],
  templateUrl: "./avoir.component.html",
  styleUrl: "./avoir.component.scss"
})
export class AvoirComponent implements OnInit {
  protected readonly statutOptions: IStatutOption[] = [
    { label: "Tous", value: "" },
    { label: "Brouillon", value: "DRAFT" },
    { label: "Émis", value: "EMIS" },
    { label: "Imputé", value: "IMPUTE" },
    { label: "Annulé", value: "ANNULE" }
  ];

  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected selectedStatut = "";
  protected tiersPayantSuggestions: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] = [];

  protected readonly avoirs = signal<IAvoir[]>([]);
  protected readonly loading = signal(false);

  private readonly api = inject(AvoirApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
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
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.avoirs.set(res.body ?? []),
        error: () => this.notificationService.error("Erreur lors du chargement des avoirs")
      });
  }

  openNouvelAvoir(prefillFactureId?: number, prefillFactureDate?: string, prefillTiersPayantId?: number): void {
    const ref = this.modalService.open(AvoirFormModalComponent, {
      size: "lg",
      centered: true,
      backdrop: "static"
    });
    ref.componentInstance.prefillFactureId = prefillFactureId;
    ref.componentInstance.prefillFactureDate = prefillFactureDate;
    ref.componentInstance.prefillTiersPayantId = prefillTiersPayantId;
    ref.result.then(
      (avoir: IAvoir) => {
        this.avoirs.set([avoir, ...this.avoirs()]);
        this.notificationService.success("Avoir créé");
      },
      () => {}
    );
  }

  onEmettre(avoir: IAvoir): void {
    if (!avoir.id) return;
    this.api
      .emettre(avoir.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.replaceAvoir(res.body!);
          this.notificationService.success("Avoir émis");
        },
        error: () => this.notificationService.error("Erreur lors de l'émission")
      });
  }

  onImputer(avoir: IAvoir): void {
    if (!avoir.id || !avoir.factureOrigineId || !avoir.factureOrigineDate) return;
    this.api
      .imputer(avoir.id, avoir.factureOrigineId, avoir.factureOrigineDate)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.avoirs.set(
            this.avoirs().map(a => (a.id === avoir.id ? { ...a, statut: "IMPUTE" } : a))
          );
          this.notificationService.success("Avoir imputé");
        },
        error: () => this.notificationService.error("Erreur lors de l'imputation")
      });
  }

  onAnnuler(avoir: IAvoir): void {
    if (!avoir.id) return;
    this.api
      .annuler(avoir.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.avoirs.set(
            this.avoirs().map(a => (a.id === avoir.id ? { ...a, statut: "ANNULE" } : a))
          );
          this.notificationService.success("Avoir annulé");
        },
        error: () => this.notificationService.error("Erreur lors de l'annulation")
      });
  }

  onDownloadPdf(avoir: IAvoir): void {
    if (!avoir.id) return;
    this.api
      .exportPdf(avoir.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => {
          const a = document.createElement("a");
          a.href = URL.createObjectURL(blob);
          a.download = `avoir-${avoir.numAvoir ?? avoir.id}.pdf`;
          a.click();
          URL.revokeObjectURL(a.href);
        },
        error: () => this.notificationService.error("Erreur lors du téléchargement PDF")
      });
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayantSuggestions = res.body ?? []));
  }

  getStatutBadgeClass(statut?: string): string {
    switch (statut) {
      case "DRAFT":
        return "badge bg-secondary";
      case "EMIS":
        return "badge bg-primary";
      case "IMPUTE":
        return "badge bg-success";
      case "ANNULE":
        return "badge bg-danger";
      default:
        return "badge bg-secondary";
    }
  }

  getStatutLabel(statut?: string): string {
    switch (statut) {
      case "DRAFT":
        return "Brouillon";
      case "EMIS":
        return "Émis";
      case "IMPUTE":
        return "Imputé";
      case "ANNULE":
        return "Annulé";
      default:
        return statut ?? "—";
    }
  }

  private buildParams(): any {
    return {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id),
      statut: this.selectedStatut || undefined
    };
  }

  private replaceAvoir(updated: IAvoir): void {
    this.avoirs.set(this.avoirs().map(a => (a.id === updated.id ? updated : a)));
  }
}
