import { Component, inject, signal, computed } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { RemiseCacheService } from '../../../../entities/sales/service/remise-cache.service';
import { Remise } from '../../../../shared/model/remise.model';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

/**
 * Modal de sélection d'une remise
 * Utilisé dans COMPTANT, CARNET, ASSURANCE
 */
@Component({
  selector: 'app-remise-selection-modal',
  templateUrl: './remise-selection-modal.component.html',
  styleUrls: ['./remise-selection-modal.component.scss'],
  imports: [CommonModule, FormsModule, SelectModule, ButtonModule],
})
export class RemiseSelectionModalComponent {
  readonly activeModal = inject(NgbActiveModal);
  private readonly remiseCacheService = inject(RemiseCacheService);

  // Signals
  readonly selectedRemise = signal<Remise | null>(null);
  readonly remises = computed(() => this.remiseCacheService.remises());

  /**
   * Confirmer la sélection
   */
  confirm(): void {
    if (this.selectedRemise()) {
      this.activeModal.close(this.selectedRemise());
    }
  }

  /**
   * Annuler
   */
  cancel(): void {
    this.activeModal.dismiss();
  }

  /**
   * Sélectionner une remise
   */
  onSelectRemise(remise: Remise | null): void {
    this.selectedRemise.set(remise);
  }
}
