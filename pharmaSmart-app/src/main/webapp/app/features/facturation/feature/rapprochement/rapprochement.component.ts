import { Component, computed, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { DecimalPipe } from "@angular/common";
import { finalize } from "rxjs/operators";

import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { ButtonModule } from "primeng/button";
import { DatePicker } from "primeng/datepicker";
import { FloatLabelModule } from "primeng/floatlabel";
import { MultiSelectModule } from "primeng/multiselect";
import { TableModule } from "primeng/table";
import { Toolbar } from "primeng/toolbar";
import { Toast } from "primeng/toast";

import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { ITiersPayant } from "../../../../shared/model";
import {
  IEtatRapprochement,
  ILigneRapprochement,
  IPaymentId,
  IRapprochementKpi,
  IReglementDto,
  IReglementParams,
  IResponseReglement
} from "../../data-access/models";
import { RapprochementApiService } from "../../data-access/services/rapprochement-api.service";
import { ReglementApiService } from "../../data-access/services/reglement-api.service";
import {
  ReglementRapprochementModalComponent
} from "../../ui/reglement-rapprochement-modal/reglement-rapprochement-modal.component";
import { RapprochementKpiBannerComponent } from "../../ui/rapprochement-kpi-banner/rapprochement-kpi-banner.component";
import { ErrorService } from "../../../../shared/error.service";
import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { BlobDownloadService } from "../../../../shared/services/blob-download.service";
import { ButtonGroup } from "primeng/buttongroup";
import { Tooltip } from "primeng/tooltip";

interface IStatutOption {
  label: string;
  value: string;
}

@Component({
  selector: "app-rapprochement",
  imports: [
    FormsModule,
    DecimalPipe,
    Toolbar,
    ButtonModule,
    DatePicker,
    FloatLabelModule,
    AutoCompleteModule,
    MultiSelectModule,
    TableModule,
    Toast,
    RapprochementKpiBannerComponent,
    ButtonGroup,
    Tooltip
  ],
  templateUrl: "./rapprochement.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./rapprochement.component.scss"
})
export class RapprochementComponent implements OnInit {
  protected readonly statutOptions: IStatutOption[] = [
    { label: "Payé", value: "PAID" },
    { label: "Partiel", value: "PARTIALLY_PAID" },
    { label: "Impayé", value: "NOT_PAID" }
  ];

  protected modelStartDate: Date;
  protected modelEndDate: Date = new Date();
  protected selectedStatut: string[] = [];
  protected tiersPayantSuggestions: ITiersPayant[] = [];
  protected selectedTiersPayants: ITiersPayant[] = [];

  protected readonly rapprochements = signal<IEtatRapprochement[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadingExport = signal(false);

  // Master/detail state
  protected readonly selectedRappr = signal<IEtatRapprochement | null>(null);
  protected readonly panelOpen = computed(() => this.selectedRappr() !== null);
  protected readonly today = new Date().toISOString().split("T")[0];
  protected readonly showHint = signal<boolean>(localStorage.getItem('rapprochement-hint-dismissed') !== '1');
  protected readonly kpi = computed<IRapprochementKpi | null>(() => {
    const data = this.rapprochements();
    if (data.length === 0) return null;
    const totalFacture = data.reduce((s, r) => s + (r.totalFacture ?? 0), 0);
    const totalRegle = data.reduce((s, r) => s + (r.totalRegle ?? 0), 0);
    const countLignesEnRetard = data.reduce(
      (s, r) => s + (r.lignes ?? []).filter(l => this.isLigneEnRetard(l)).length, 0
    );
    return {
      totalFacture,
      totalRegle,
      ecartTotal: totalFacture - totalRegle,
      tauxRecouvrement: totalFacture > 0 ? (totalRegle / totalFacture) * 100 : 0,
      countOrganismes: data.length,
      countLignesEnRetard
    };
  });

  private readonly api = inject(RapprochementApiService);
  /** Pour créer/annuler un règlement : délégation vers le service de règlement existant */
  private readonly reglementApi = inject(ReglementApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly modalService = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly tauriPrinterService = inject(TauriPrinterService);
  private readonly downloadService = inject(BlobDownloadService);

  constructor() {
    const d = new Date();
    d.setMonth(d.getMonth() - 1);
    this.modelStartDate = d;
  }

  ngOnInit(): void {
    this.onSearch();
  }
  dismissHint(): void {
    localStorage.setItem('rapprochement-hint-dismissed', '1');
    this.showHint.set(false);
  }

  onSearch(): void {
    this.loading.set(true);
    this.selectedRappr.set(null);
    this.api
      .query(this.buildParams())
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => this.rapprochements.set(res.body ?? []),
        error: () => this.notificationService.error("Erreur lors du chargement du rapprochement")
      });
  }

  onRowSelect(rappr: IEtatRapprochement): void {
    if (this.selectedRappr()?.tiersPayantName === rappr.tiersPayantName) {
      this.selectedRappr.set(null);
    } else {
      this.selectedRappr.set(rappr);
    }
  }

  closePanel(): void {
    this.selectedRappr.set(null);
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
        next: (blob) => {
          this.downloadService.downloadExcel(blob, "rapprochement");
        },
        error: () => this.notificationService.error("Erreur export Excel")
      });
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayantSuggestions = res.body ?? []));
  }

  openReglementModal(ligne: ILigneRapprochement): void {
    const ref = this.modalService.open(ReglementRapprochementModalComponent, {
      size: "lg",
      centered: true,
      backdrop: "static"
    });
    ref.componentInstance.ligne = ligne;

    ref.result.then(
      (params: IReglementParams) => {
        this.reglementApi.doReglement(params)
          .pipe(
            finalize(() => {
            }),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe({

            next: res => {
              if (res.body) {
                this.onPrintReceipt(res.body);
                this.onSearch();
              }
            },
            error: () => this.notificationService.error("Erreur lors de l'enregistrement du règlement")
          });
      },
      () => {
      }
    );
  }

  private onPrintReceipt(response: IResponseReglement): void {
    this.confirmDialog.onConfirm(
      () => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          this.printReceiptForTauri(response.id);
        } else {
          this.reglementApi.printReceipt(response.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              error: err =>
                this.notificationService.error(this.errorService.getErrorMessage(err), "Impression")
            });
        }

      },
      "Ticket règlement",
      "Voulez-vous imprimer le ticket ?"
    );
  }

  private printReceiptForTauri(paymentId: IPaymentId): void {
    this.reglementApi.getEscPosReceiptForTauri(paymentId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async (data: ArrayBuffer) => {
          try {
            await this.tauriPrinterService.printEscPosFromBuffer(data);
          } catch { /* silently ignore printer errors */
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Impression")
      });
  }

  onAnnulerReglement(ligne: ILigneRapprochement, reglement: IReglementDto): void {
    if (!reglement.id || !reglement.transactionDate) return;
    this.reglementApi.delete({ id: reglement.id, transactionDate: reglement.transactionDate })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          // Recharge l'état complet pour cohérence
          this.onSearch();
          this.notificationService.success("Règlement annulé");
        },
        error: () => this.notificationService.error("Erreur lors de l'annulation")
      });
  }

  isLigneEnRetard(ligne: ILigneRapprochement): boolean {
    return !!ligne.echeance && ligne.echeance < this.today && ligne.statut !== "PAID";
  }


  // KPI pour l'organisme sélectionné
  detailTaux(rappr: IEtatRapprochement): number {
    const tf = rappr.totalFacture ?? 0;
    return tf > 0 ? Math.round(((rappr.totalRegle ?? 0) / tf) * 100) : 0;
  }

  lignesEnRetard(rappr: IEtatRapprochement): number {
    return (rappr.lignes ?? []).filter(l => this.isLigneEnRetard(l)).length;
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

  private buildParams(): any {
    return {
      startDate: DATE_FORMAT_ISO_DATE(this.modelStartDate),
      endDate: DATE_FORMAT_ISO_DATE(this.modelEndDate),
      tiersPayantIds: this.selectedTiersPayants.map(t => t.id),
      statuts: this.selectedStatut ?? []
    };
  }


}
