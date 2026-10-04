import { ModalDeplacableDirective } from 'app/shared/utils/modal-deplacable.directive';
import { Component, computed, DestroyRef, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ButtonComponent, CardComponent, InputNumberComponent, SelectComponent } from '../../../../shared/ui';
import { DeviseDirective, DevisePipe } from 'app/shared/utils/devise';
import { ErrorService } from 'app/shared/error.service';
import { RadioComponent } from 'app/shared/ui/radio/radio.component';
import {
  AvoirClientApiService,
  CloturerAvoirRequest,
  IAvoirClientDocument,
  ModeClotureAvoir,
} from '../../data-access/services/avoir-client-api.service';

@Component({
  selector: 'app-cloturer-avoir-modal',
  templateUrl: './cloturer-avoir-modal.component.html',
  styleUrl: './cloturer-avoir-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModalDeplacableDirective, DeviseDirective, DevisePipe, CommonModule, FormsModule, ButtonComponent, SelectComponent, InputNumberComponent, CardComponent, RadioComponent],
})
export class CloturerAvoirModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(AvoirClientApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly errorService = inject(ErrorService);

  document!: IAvoirClientDocument;

  /** Mode déjà choisi par l'appelant (gestes rapides du comptoir). */
  set modeInitial(mode: ModeClotureAvoir) {
    this.choisirMode(mode);
  }

  protected readonly modeCloture = signal<ModeClotureAvoir | null>(null);
  protected readonly commentaire = signal('');
  protected readonly isPartialUsage = signal(false);
  protected readonly montantPartiel = signal<number | null>(null);
  /** Remise du produit : 'tout' (défaut) ou 'partie' en unités. */
  protected readonly remise = signal<'tout' | 'partie'>('tout');
  protected readonly quantiteRemise = signal<number | null>(null);
  protected readonly estRemiseProduit = computed(() => this.modeCloture() === 'RETOUR_PRODUIT');
  protected readonly quantiteRestante = computed(() => this.document?.quantiteRestante ?? this.document?.quantite ?? 0);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly montantDisponible = computed(() => this.document?.montantRestant ?? this.document?.montant ?? 0);

  protected readonly montantEffectif = computed(() => {
    if (this.estRemiseProduit()) {
      const k = this.quantiteRemise();
      const quantite = this.document?.quantite ?? 0;
      if (this.remise() === 'tout' || !k || k >= this.quantiteRestante() || quantite <= 0) return this.montantDisponible();
      return Math.min(k * Math.floor((this.document?.montant ?? 0) / quantite), this.montantDisponible());
    }
    if (!this.isPartialUsage()) return this.montantDisponible();
    const partiel = this.montantPartiel();
    if (!partiel || partiel <= 0) return 0;
    return Math.min(partiel, this.montantDisponible());
  });

  protected readonly canConfirm = computed(() => {
    if (!this.modeCloture()) return false;
    if (this.estRemiseProduit() && this.remise() === 'partie') {
      const k = this.quantiteRemise();
      return k != null && k >= 1 && k <= this.quantiteRestante();
    }
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
    { label: 'Remboursement espèces', value: 'REMBOURSEMENT_ESPECES', icon: 'pi pi-money-bill', description: 'Le client est remboursé en espèces au guichet : une sortie de caisse est enregistrée (caisse ouverte exigée).' },
    { label: 'Remboursement CB', value: 'REMBOURSEMENT_CB', icon: 'pi pi-credit-card', description: 'Le montant est recrédité sur la carte bancaire du client : une sortie de caisse est enregistrée (caisse ouverte exigée).' },
    { label: 'Le produit est remis au client', value: 'RETOUR_PRODUIT', icon: 'pi pi-replay', description: 'Le produit est remis au client. Le stock est ajusté en conséquence.' },
    { label: 'Compensation vente', value: 'COMPENSATION_VENTE', icon: 'pi pi-arrow-right-arrow-left', description: 'Le montant est imputé directement sur une vente du client.' },
  ];

  /** Une remise se compte en unités, jamais en montant : changer de mode remet l'autre choix à zéro. */
  protected choisirMode(mode: ModeClotureAvoir | null): void {
    this.modeCloture.set(mode);
    if (mode === 'RETOUR_PRODUIT') {
      this.isPartialUsage.set(false);
      this.montantPartiel.set(null);
    } else {
      this.remise.set('tout');
      this.quantiteRemise.set(null);
    }
  }

  protected confirm(): void {
    const mode = this.modeCloture();
    if (!mode || !this.document?.id) return;
    this.error.set(null);
    this.loading.set(true);
    const request: CloturerAvoirRequest = {
      modeCloture: mode,
      commentaire: this.commentaire() || undefined,
      montantUtilise: !this.estRemiseProduit() && this.isPartialUsage() ? (this.montantPartiel() ?? undefined) : undefined,
      quantiteRemise: this.estRemiseProduit() && this.remise() === 'partie' ? (this.quantiteRemise() ?? undefined) : undefined,
    };
    this.api
      .cloturerAvoir(this.document.id, request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.activeModal.close();
        },
        error: err => {
          this.loading.set(false);
          // Le message du serveur dit pourquoi (« Aucune caisse ouverte… », stock insuffisant) : le caissier peut agir.
          this.error.set(this.errorService.getErrorMessage(err, 'Une erreur est survenue. Veuillez réessayer.'));
        },
      });
  }

  protected cancel(): void {
    this.activeModal.dismiss();
  }
}
