import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {AppPillOption, ButtonComponent, PillSelectorComponent} from 'app/shared/ui';
import {CanalEnvoi} from 'app/shared/model/abstract-commande.model';

/**
 * Demande par quel moyen la commande est partie, avant d'horodater son envoi.
 *
 * Le canal est demandé plutôt que deviné : l'officine qui ne fait pas d'EDI commande par mail ou
 * par téléphone, et c'est lui qui dira où retrouver la trace de l'envoi si le grossiste prétend
 * n'avoir rien reçu.
 */
@Component({
  selector: 'app-marquer-envoi-modal',
  imports: [FormsModule, PillSelectorComponent, ButtonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="modal-header">
      <h5 class="modal-title">Marquer la commande comme envoyée</h5>
      <button type="button" class="btn-close" aria-label="Fermer" (click)="modal.dismiss()"></button>
    </div>
    <div class="modal-body">
      @if (reference) {
        <p class="mb-2">
          Commande <span class="pharma-code">{{ reference }}</span>
          @if (fournisseur) {
            — {{ fournisseur }}
          }
        </p>
      }
      <p class="text-secondary small mb-3">
        La date d'envoi sert de point de départ au délai de livraison et à l'alerte de retard.
      </p>
      <label class="form-label">Canal d'envoi</label>
      <app-pill-selector
        [items]="canaux"
        [ngModel]="canal()"
        (selectionChange)="canal.set($any($event))"
        ariaLabel="Canal d'envoi"
      />
    </div>
    <div class="modal-footer">
      <app-button label="Annuler" severity="secondary" [text]="true" (clicked)="modal.dismiss()" />
      <app-button label="Marquer comme envoyée" icon="pi pi-send" (clicked)="modal.close(canal())" />
    </div>
  `,
})
export class MarquerEnvoiModalComponent {
  protected readonly modal = inject(NgbActiveModal);

  reference?: string;
  fournisseur?: string;

  // EDI n'est pas proposé : ce canal s'horodate tout seul lors de l'envoi PharmaML, et le
  // choisir à la main laisserait croire à une transmission dont l'application n'a aucune trace.
  protected readonly canaux: AppPillOption[] = [
    {label: 'Mail', value: 'MAIL', icon: 'pi pi-envelope'},
    {label: 'Bon imprimé', value: 'PDF', icon: 'pi pi-print'},
    {label: 'Téléphone', value: 'TELEPHONE', icon: 'pi pi-phone'},
    {label: 'Autre', value: 'AUTRE', icon: 'pi pi-ellipsis-h'},
  ];

  protected readonly canal = signal<CanalEnvoi>('MAIL');
}
