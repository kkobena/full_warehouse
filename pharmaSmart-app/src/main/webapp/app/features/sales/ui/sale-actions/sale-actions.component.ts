import {ChangeDetectionStrategy, Component, input, output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {NgbTooltip} from '@ng-bootstrap/ng-bootstrap';
import {ButtonComponent} from '../../../../shared/ui';

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
  imports: [CommonModule, ButtonComponent, NgbTooltip],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SaleActionsComponent {
  // Inputs
  canSave = input(false);
  canPrint = input(false);
  canCancel = input(true);
  canSaveAsPresale = input(true);
  isPresale = input(false);
  isDevis = input(false);
  isSaving = input(false);
  saleType = input<'COMPTANT' | 'ASSURANCE' | 'CARNET'>('COMPTANT');
  showPrintButton = input(false);
  isSmallScreen = input(false); // Pour afficher icon-only sur mobile
  showButtonEnAttente = input(true);

  // Outputs
  save = output<void>();
  print = output<void>();
  cancel = output<void>();
  saveAsPresale = output<void>();
  savePresale = output<void>();
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
    if (this.isPresale() || this.isDevis()) {
      this.saveAsPresale.emit();
    }
  }

  onSavePresale(): void {
    if (this.isPresale()) {
      this.savePresale.emit();
    }
  }

  onPutOnHold(): void {
    if (!this.isPresale() && !this.isDevis()) {
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
    if (this.isPresale()) {
      return 'Passer en vente';
    }
    return this.isDevis() ? 'Enregistrer' : 'Finaliser';
  }

  getSavePresaleButtonLabel(): string {
    return 'Enregistrer';
  }

  getSaveButtonIcon(): string {
    if (this.isSaving()) {
      return 'pi pi-spin pi-spinner';
    }
    return this.isDevis() ? 'pi pi-file-edit' : 'pi pi-check';
  }

  getSavePresaleIcon(): string {
    if (this.isSaving()) {
      return 'pi pi-spin pi-spinner';
    }
    return 'pi pi-file-edit';
  }

  getSavePresaleSeverity(): 'success' | 'info' | 'warn' {
    return 'success';
  }

  getSaveButtonSeverity(): 'success' | 'info' | 'warn' {
    return this.isPresale() || this.isDevis() ? 'info' : 'success';
  }
}
