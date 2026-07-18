import { Component, computed, DestroyRef, effect, inject, input, signal, viewChild, ChangeDetectionStrategy } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs/operators";
import { FormsModule } from "@angular/forms";
import { ButtonModule } from "primeng/button";
import { TableHeaderCheckbox, TableModule } from "primeng/table";
import { TooltipModule } from "primeng/tooltip";
import { InputTextModule } from "primeng/inputtext";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";


import { NgbConfirmDialogService } from "../../../../shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive";
import { NotificationService } from "../../../../shared/services/notification.service";
import { ErrorService } from "../../../../shared/error.service";
import { TauriPrinterService } from "../../../../shared/services/tauri-printer.service";
import { ITEMS_PER_PAGE } from "../../../../shared/constants/pagination.constants";

import { FactureApiService } from "../../data-access/services/facture-api.service";
import { ReglementApiService } from "../../data-access/services/reglement-api.service";
import {
  IDossierFactureProjection,
  IFactureId,
  IPaymentId,
  IReglementFactureDossier,
  IReglementParams,
  IResponseReglement,
  ModeEditionReglement
} from "../../data-access/models";
import { ReglementFormComponent } from "../reglement-form/reglement-form.component";
import { CommonModule } from "@angular/common";

export type ReglementMode = "INDIVIDUEL" | "GROUPE";

@Component({
  selector: "app-reglement-workspace",
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TableModule,
    TooltipModule,
    InputTextModule,
    IconField,
    InputIcon,
    ReglementFormComponent
  ],
  templateUrl: "./reglement-workspace.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./reglement-workspace.component.scss"
})
export class ReglementWorkspaceComponent {
  readonly mode = input.required<ReglementMode>();
  readonly reglementFactureDossiers = input<IReglementFactureDossier[]>([]);
  readonly dossierFactureProjection = input<IDossierFactureProjection | null>(null);

  protected reglementFactureDossiersSignal = signal<IReglementFactureDossier[]>([]);
  protected dossierFactureProjectionSignal = signal<IDossierFactureProjection | null>(null);
  protected factureDossierSelectionnes = signal<IReglementFactureDossier[]>([]);
  protected readonly partialPayment = signal(false);
  protected isSaving = false;
  protected readonly ModeEditionReglement = ModeEditionReglement;

  protected montantAPayer = computed(() =>
    this.factureDossierSelectionnes().reduce((acc, d) => acc + this.computeMontantRestant(d), 0) || 0
  );
  protected dossierIds = computed(() =>
    this.factureDossierSelectionnes().map(d => d.id)
  );
  /** IDs des dossiers éditables (mode partiel + sélectionnés) — lu directement dans le template pour le tracking signal */
  protected readonly editableIds = computed(() => {
      return this.partialPayment()
        ? new Set(this.factureDossierSelectionnes().map(r => r.id))
        : new Set<number>();
    }
  );

  protected checkbox = viewChild<TableHeaderCheckbox>("checkbox");
  protected reglementFormComponent = viewChild(ReglementFormComponent);

  private readonly destroyRef = inject(DestroyRef);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly reglementApiService = inject(ReglementApiService);
  private readonly tauriPrinterService = inject(TauriPrinterService);

  constructor() {
    // Sync dossiers depuis l'input parent (chargé après le tab click)
    effect(() => {
      const dossiers = this.reglementFactureDossiers();
      if (dossiers.length > 0) {
        this.reglementFactureDossiersSignal.set(dossiers);
      }
    });

    // Sync projection + mise à jour du montant dans le formulaire
    effect(() => {
      const projection = this.dossierFactureProjection();
      if (projection) {
        this.dossierFactureProjectionSignal.set(projection);
      }
      const montant = this.montantAPayer();
      const form = this.reglementFormComponent();
      if (form) {
        form.cashInput.setValue(montant > 0 ? montant : this.montantAttendu);
      }
    });
  }

  get totalAmount(): number {
    return this.partialPayment() ? this.montantAPayer() : this.montantAttendu;
  }

  private get montantAttendu(): number {
    const p = this.dossierFactureProjectionSignal();
    return p ? p.montantTotal - p.montantDetailRegle : null;
  }

  onPartielReglement(isPartial: boolean): void {
    this.partialPayment.set(isPartial);
    this.factureDossierSelectionnes.set([]);
    if (this.mode() === "GROUPE") {
      this.reglementFactureDossiersSignal.update(dossiers =>
        dossiers.map(d => ({ ...d, montantVerse: d.montantTotal - d.montantDetailRegle }))
      );
    }
  }

  onSelectChange(event: any): void {
    this.factureDossierSelectionnes.set(event as IReglementFactureDossier[]);
  }

  onMontantVerseChange(item: IReglementFactureDossier): void {
    this.factureDossierSelectionnes.set(
      this.factureDossierSelectionnes().map(d => (d.id === item.id ? item : d))
    );
  }

  onSaveReglement(params: IReglementParams): void {
    this.isSaving = true;
    this.reglementApiService
      .doReglement(this.buildReglementParams(params))
      .pipe(
        finalize(() => (this.isSaving = false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: res => {
          if (res.body) {
            this.onPrintReceipt(res.body);
          }
        },
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Erreur règlement")
      });
  }

  private onPrintReceipt(response: IResponseReglement): void {
    this.confirmDialog.onConfirm(
      () => {
        if (this.tauriPrinterService.isRunningInTauri()) {
          this.printReceiptForTauri(response.id);
        } else {
          this.reglementApiService.printReceipt(response.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              error: err =>
                this.notificationService.error(this.errorService.getErrorMessage(err), "Impression")
            });
        }
        this.reset(response);
      },
      "Ticket règlement",
      "Voulez-vous imprimer le ticket ?"
    );
  }

  private printReceiptForTauri(paymentId: IPaymentId): void {
    this.reglementApiService.getEscPosReceiptForTauri(paymentId)
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

  private buildReglementParams(params: IReglementParams): IReglementParams {
    if (this.mode() === "GROUPE") {
      return {
        ...params,
        mode: this.partialPayment() ? ModeEditionReglement.GROUPE_PARTIEL : ModeEditionReglement.GROUPE_TOTAL,
        ligneSelectionnes: this.partialPayment()
          ? this.factureDossierSelectionnes().map(d => ({
            id: d.id,
            montantAttendu: d.montantTotal - d.montantPaye,
            montantVerse: d.montantVerse,
            montantFacture: this.dossierFactureProjectionSignal()?.montantTotal
          }))
          : []
      };
    }
    return {
      ...params,
      mode: this.partialPayment() ? ModeEditionReglement.FACTURE_PARTIEL : ModeEditionReglement.FACTURE_TOTAL,
      dossierIds: this.dossierIds()
    };
  }

  private computeMontantRestant(d: IReglementFactureDossier): number {
    return this.mode() === "GROUPE" ? (d.montantVerse ?? 0) : (d.montantTotal - d.montantPaye);
  }

  private reset(response: IResponseReglement): void {
    this.factureDossierSelectionnes.set([]);
    this.reglementFormComponent()?.reset();
    if (response.total) {
      this.dossierFactureProjectionSignal.set(null);
      this.reglementFactureDossiersSignal.set([]);
      this.reglementFormComponent()?.cashInput.setValue(null);
    } else {
      const factureId: IFactureId = {
        id: this.dossierFactureProjectionSignal().id,
        invoiceDate: this.dossierFactureProjectionSignal().invoiceDate
      };
      this.loadDossierProjection(this.dossierFactureProjectionSignal().factureItemId);
      this.reloadDossiers(factureId);
    }
  }

  private loadDossierProjection(factureId: IFactureId): void {
    this.factureApiService
      .findDossierFactureProjection(factureId, { isGroup: this.mode() === "GROUPE" })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => this.dossierFactureProjectionSignal.set(res.body),
        error: err =>
          this.notificationService.error(this.errorService.getErrorMessage(err), "Chargement facture")
      });
  }

  private reloadDossiers(factureId: IFactureId): void {
    const typeFacture = this.mode() === "GROUPE" ? "groupes" : "individuelle";
    this.factureApiService
      .findDossierReglement(factureId, typeFacture, { page: 0, size: ITEMS_PER_PAGE })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.reglementFactureDossiersSignal.set(res.body ?? []);
          this.reglementFormComponent()?.cashInput.setValue(this.montantAttendu);
        },
        error: () => {
          this.reglementFactureDossiersSignal.set([]);
          this.dossierFactureProjectionSignal.set(null);
        }
      });
  }
}
