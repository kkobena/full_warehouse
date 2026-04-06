import { Component, DestroyRef, inject, OnInit, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs/operators";

import { NgbActiveModal } from "@ng-bootstrap/ng-bootstrap";
import { AutoCompleteModule } from "primeng/autocomplete";
import { ButtonModule } from "primeng/button";
import { DatePicker } from "primeng/datepicker";
import { FloatLabelModule } from "primeng/floatlabel";
import { InputTextModule } from "primeng/inputtext";

import { DATE_FORMAT_ISO_DATE } from "../../../../shared/util/warehouse-util";
import { NotificationService } from "../../../../shared/services/notification.service";
import { TiersPayantService } from "../../../../entities/tiers-payant/tierspayant.service";
import { ITiersPayant } from "../../../../shared/model";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { IAvoir, IAvoirCommand } from "../../data-access/models";
import { AvoirApiService } from "../../data-access/services/avoir-api.service";

@Component({
  selector: "app-avoir-form-modal",
  imports: [
    FormsModule,
    AutoCompleteModule,
    ButtonModule,
    DatePicker,
    FloatLabelModule,
    InputTextModule
  ],
  templateUrl: "./avoir-form-modal.component.html",
  styleUrl: "./avoir-form-modal.component.scss"
})
export class AvoirFormModalComponent implements OnInit {
  prefillFactureId?: number;
  prefillFactureDate?: string;
  prefillTiersPayantId?: number;

  protected factureId: number | null = null;
  protected factureDate: Date | null = null;
  protected selectedTiersPayant: ITiersPayant | null = null;
  protected tiersPayantSuggestions: ITiersPayant[] = [];
  protected montantAvoir: number | null = null;
  protected montantTva: number | null = null;
  protected montantHt: number | null = null;
  protected motif = "";

  protected readonly saving = signal(false);

  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(AvoirApiService);
  private readonly tiersPayantService = inject(TiersPayantService);
  private readonly notificationService = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    if (this.prefillFactureId) this.factureId = this.prefillFactureId;
    if (this.prefillFactureDate) this.factureDate = new Date(this.prefillFactureDate);
  }

  isFormValid(): boolean {
    return (
      !!this.factureId &&
      !!this.factureDate &&
      !!this.selectedTiersPayant &&
      this.montantAvoir !== null &&
      this.montantAvoir > 0 &&
      !!this.motif.trim()
    );
  }

  searchTiersPayant(event: { query: string }): void {
    this.tiersPayantService
      .query({ page: 0, search: event.query, size: 10 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(res => (this.tiersPayantSuggestions = res.body ?? []));
  }

  onSave(): void {
    if (!this.isFormValid()) return;

    const command: IAvoirCommand = {
      factureId: this.factureId!,
      factureDate: DATE_FORMAT_ISO_DATE(this.factureDate!) ?? undefined,
      tiersPayantId: this.selectedTiersPayant!.id,
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
