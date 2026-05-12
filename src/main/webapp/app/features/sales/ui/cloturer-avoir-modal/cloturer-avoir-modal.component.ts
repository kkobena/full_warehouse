import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { InputNumber } from 'primeng/inputnumber';
import {
  AvoirClientApiService,
  CloturerAvoirRequest,
  IAvoirClientDocument,
  ModeClotureAvoir,
} from '../../data-access/services/avoir-client-api.service';
import { Textarea } from 'primeng/textarea';

@Component({
  selector: 'app-cloturer-avoir-modal',
  templateUrl: './cloturer-avoir-modal.component.html',
  styleUrl: './cloturer-avoir-modal.component.scss',
  imports: [CommonModule, FormsModule, Button, Select, Textarea, InputNumber],
})
export class CloturerAvoirModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(AvoirClientApiService);
  private readonly destroyRef = inject(DestroyRef);

  document!: IAvoirClientDocument;

  protected readonly modeCloture = signal<ModeClotureAvoir | null>(null);
  protected readonly commentaire = signal('');
  protected readonly isPartialUsage = signal(false);
  protected readonly montantPartiel = signal<number | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly montantDisponible = computed(() => this.document?.montantRestant ?? this.document?.montant ?? 0);

  protected readonly montantEffectif = computed(() => {
    if (!this.isPartialUsage()) return this.montantDisponible();
    const partiel = this.montantPartiel();
    if (!partiel || partiel <= 0) return 0;
    return Math.min(partiel, this.montantDisponible());
  });

  protected readonly canConfirm = computed(() => {
    if (!this.modeCloture()) return false;
    if (this.isPartialUsage() && (this.montantPartiel() == null || (this.montantPartiel() ?? 0) <= 0)) return false;
    return true;
  });

  protected readonly selectedModeInfo = computed(() => {
    const mode = this.modeCloture();
    if (!mode) return null;
    const opt = this.modeClotureOptions.find(o => o.value === mode);
    return opt ? { icon: opt.icon, description: opt.description } : null;
  });

  protected readonly modeClotureOptions: { label: string; value: ModeClotureAvoir; icon: string; description: string }[] = [
    { label: 'Remboursement espèces', value: 'REMBOURSEMENT_ESPECES', icon: 'pi pi-money-bill', description: 'Le client est remboursé en espèces au guichet.' },
    { label: 'Remboursement CB', value: 'REMBOURSEMENT_CB', icon: 'pi pi-credit-card', description: 'Le montant est recrédité sur la carte bancaire du client.' },
    { label: 'Le produit est remis au client', value: 'RETOUR_PRODUIT', icon: 'pi pi-replay', description: 'Le produit est remis au client. Le stock est ajusté en conséquence.' },
    { label: 'Compensation vente', value: 'COMPENSATION_VENTE', icon: 'pi pi-arrow-right-arrow-left', description: 'Le montant est imputé directement sur une vente du client.' },
  ];

  protected confirm(): void {
    const mode = this.modeCloture();
    if (!mode || !this.document?.id) return;
    this.error.set(null);
    this.loading.set(true);
    const request: CloturerAvoirRequest = {
      modeCloture: mode,
      commentaire: this.commentaire() || undefined,
      montantUtilise: this.isPartialUsage() ? (this.montantPartiel() ?? undefined) : undefined,
    };
    this.api
      .cloturerAvoir(this.document.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.activeModal.close();
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Une erreur est survenue. Veuillez réessayer.');
        },
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
