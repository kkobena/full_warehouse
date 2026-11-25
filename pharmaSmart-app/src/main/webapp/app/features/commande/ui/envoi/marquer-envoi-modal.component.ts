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
  templateUrl: './marquer-envoi-modal.component.html',
  styleUrls: ['./marquer-envoi-modal.component.scss'],
})
export class MarquerEnvoiModalComponent {
  reference?: string;
  fournisseur?: string;
  protected readonly modal = inject(NgbActiveModal);

  // EDI n'est pas proposé : ce canal s'horodate tout seul lors de l'envoi PharmaML, et le
  // choisir à la main laisserait croire à une transmission dont l'application n'a aucune trace.
  protected readonly canaux: AppPillOption[] = [
    {label: 'Téléphone', value: 'TELEPHONE', icon: 'pi pi-phone'},
    {label: 'Mail', value: 'MAIL', icon: 'pi pi-envelope'},
    {label: 'Bon imprimé', value: 'PDF', icon: 'pi pi-print'},
    {label: 'Autre', value: 'AUTRE', icon: 'pi pi-ellipsis-h'},
  ];

  protected readonly canal = signal<CanalEnvoi>('TELEPHONE');
}
