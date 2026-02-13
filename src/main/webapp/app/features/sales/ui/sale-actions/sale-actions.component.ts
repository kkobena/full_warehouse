import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { ButtonGroup } from 'primeng/buttongroup';

/**
 * Composant de présentation : Boutons d'action vente
 *
 * Responsabilités :
 * - Afficher boutons d'action (Sauver, Imprimer, Annuler, etc.)
 * - Gérer états désactivés selon contexte
 * - Afficher indicateur de sauvegarde en cours
 *
 * Composant pur - Pas de logique métier (OnPush)
 */
@Component({
  selector: 'app-sale-actions',
  templateUrl: './sale-actions.component.html',
  styleUrl: './sale-actions.component.scss',
  imports: [CommonModule, ButtonModule, TooltipModule, ButtonGroup],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaleActionsComponent {
  // Inputs
  canSave = input(false);
  canPrint = input(false);
  canCancel = input(true);
  canSaveAsPresale = input(true);
  isPresale = input(false);
  isSaving = input(false);
  saleType = input<'COMPTANT' | 'ASSURANCE' | 'CARNET'>('COMPTANT');
  showPrintButton = input(false);
  isSmallScreen = input(false); // Pour afficher icon-only sur mobile

  // Outputs
  save = output<void>();
  print = output<void>();
  cancel = output<void>();
  saveAsPresale = output<void>();
  saveAndPrint = output<void>();
  putOnHold = output<void>();

  // Méthodes pour les événements UI
  onSave(): void {
    if (!this.isSaving() && this.canSave()) {
      this.save.emit();
    }
  }

  onPrint(): void {
    if (this.canPrint()) {
      this.print.emit();
    }
  }

  onCancel(): void {
    if (this.canCancel()) {
      this.cancel.emit();
    }
  }

  onSaveAsPresale(): void {
    if (this.isPresale()) {
      this.saveAsPresale.emit();
    }
  }

  onPutOnHold(): void {
    if (!this.isPresale()) {
      this.putOnHold.emit();
    }
  }

  onSaveAndPrint(): void {
    if (!this.isSaving() && this.canSave()) {
      this.saveAndPrint.emit();
    }
  }

  // Helper methods
  getSaveButtonLabel(): string {
    return this.isPresale() ? 'Finaliser la prévente' : 'Enregistrer';
  }

  getSaveButtonIcon(): string {
    return this.isSaving() ? 'pi pi-spin pi-spinner' : 'pi pi-check';
  }

  getSaveButtonSeverity(): 'success' | 'info' {
    return this.isPresale() ? 'info' : 'success';
  }
}
