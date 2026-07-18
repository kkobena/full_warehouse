import { Component, DestroyRef, inject, OnInit, signal, ChangeDetectionStrategy } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs/operators";

import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { BadgeModule } from "primeng/badge";
import { ButtonModule } from "primeng/button";
import { FloatLabelModule } from "primeng/floatlabel";
import { InputTextModule } from "primeng/inputtext";
import { DecimalPipe } from "@angular/common";

import { NotificationService } from "../../../../shared/services/notification.service";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { IAvoir, IAvoirCommand, IFacture } from "../../data-access/models";
import { AvoirApiService } from "../../data-access/services/avoir-api.service";
import { FactureApiService } from "../../data-access/services/facture-api.service";
import { InputNumber } from "primeng/inputnumber";

@Component({
  selector: "app-avoir-form-modal",
  imports: [
    DecimalPipe,
    FormsModule,
    AutoCompleteModule,
    BadgeModule,
    ButtonModule,
    FloatLabelModule,
    InputTextModule,
    InputNumber
  ],
  templateUrl: "./avoir-form-modal.component.html",
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: "./avoir-form-modal.component.scss"
})
export class AvoirFormModalComponent implements OnInit {
  prefillFacture?: IFacture;
  /** Montant pré-saisi dans le workspace — affiché en lecture seule dans la modal */
  prefillMontantAvoir?: number;

  protected selectedFacture: IFacture | null = null;
  protected factureSuggestions: IFacture[] = [];
  protected montantAvoir: number | null = null;
  protected montantTva: number | null = null;
  protected montantHt: number | null = null;
  protected motif = "";

  protected readonly saving = signal(false);

  get isFromFacture(): boolean {
    return !!this.prefillFacture;
  }

  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(AvoirApiService);
  private readonly factureApiService = inject(FactureApiService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  get isMontantReadonly(): boolean {
    return this.prefillMontantAvoir != null;
  }

  ngOnInit(): void {
    if (this.prefillFacture) {
      this.selectedFacture = this.prefillFacture;
    }
    if (this.prefillMontantAvoir != null) {
      this.montantAvoir = this.prefillMontantAvoir;
    }
  }

  protected isFormValid(): boolean {
    const montantRegle = this.selectedFacture?.montantRegle ?? 0;
    return (
      !!this.selectedFacture?.factureId &&
      this.montantAvoir !== null &&
      this.montantAvoir > 0 &&
      this.montantAvoir <= montantRegle &&
      !!this.motif.trim()
    );
  }

  protected searchFacture(event: { query: string }): void {
    const toIso = (d: Date) =>
      `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    const today = new Date();
    const fiveYearsAgo = new Date(today.getFullYear() - 1, today.getMonth(), today.getDate());
    this.factureApiService
      .query({ search: event.query, startDate: toIso(fiveYearsAgo), endDate: toIso(today), statuts: ['PAID', 'PARTIALLY_PAID'] })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.factureSuggestions = res.body ?? []));
  }

  getStatutLabel(statut: string): string {
    switch (statut) {
      case 'PAID': return 'Réglé';
      case 'PARTIALLY_PAID': return 'Partiel';
      case 'NOT_PAID': return 'Impayé';
      default: return statut ?? '—';
    }
  }

  getStatutSeverity(statut: string): 'success' | 'warn' | 'danger' | 'secondary' {
    switch (statut) {
      case 'PAID': return 'success';
      case 'PARTIALLY_PAID': return 'warn';
      case 'NOT_PAID': return 'danger';
      default: return 'secondary';
    }
  }

  protected onSave(): void {
    if (!this.isFormValid()) return;

    const command: IAvoirCommand = {
      factureId: this.selectedFacture!.factureItemId.id,
      factureDate: this.selectedFacture!.factureItemId.invoiceDate,
      montantAvoir: this.montantAvoir!,
      montantTva: this.montantTva ?? undefined,
      montantHt: this.montantHt ?? undefined,
      motif: this.motif.trim()
    };

    this.saving.set(true);
    this.api
      .create(command)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: res => {
          this.notificationService.success("Avoir créé avec succès");
          this.activeModal.close(res.body as IAvoir);
        },
        error: () => this.notificationService.error("Erreur lors de la création de l'avoir")
      });
  }
}
