import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { ButtonComponent, CardComponent, SelectSearchComponent } from 'app/shared/ui';
import { IProduit } from 'app/shared/model/produit.model';
import { NotificationService } from 'app/shared/services/notification.service';
import { ErrorService } from 'app/shared/error.service';
import { IDci } from '../../../dci/models/dci.model';
import { DciApiService } from '../../../dci/data-access/services/dci-api.service';
import { DciService } from '../../../../entities/dci/dci.service';

/**
 * Rattache une sélection de produits à une substance active.
 *
 * <p>La DCI se saisissait produit par produit, dans sa fiche. Or elle s'attribue par familles
 * entières — tous les paracétamols, toutes les amoxicillines — et un catalogue repris d'un
 * autre logiciel arrive presque toujours sans elle. Ouvrir cent fiches est le genre de tâche
 * qu'on ne fait jamais : la substitution générique reste alors muette, faute de DCI.
 *
 * <p>La modal ne demande donc qu'une chose — LAQUELLE — et rappelle sur quoi elle va porter.
 */
@Component({
  selector: 'app-dci-assignation-modal',
  imports: [FormsModule, ButtonComponent, CardComponent, SelectSearchComponent],
  templateUrl: './dci-assignation-modal.component.html',
  styleUrls: ['./dci-assignation-modal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DciAssignationModalComponent {
  /** Produits à rattacher — posés par l'appelant. */
  produits: IProduit[] = [];

  protected readonly dci = signal<IDci | null>(null);
  protected readonly suggestions = signal<IDci[]>([]);
  protected readonly enCours = signal(false);

  /** Les premiers libellés seulement : la modal doit tenir à l'écran. */
  protected readonly apercu = computed(() => this.produits.slice(0, 8));

  private readonly activeModal = inject(NgbActiveModal);
  private readonly api = inject(DciApiService);
  private readonly rechercheService = inject(DciService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  protected cancel(): void {
    this.activeModal.dismiss();
  }

  /** Les DCI se cherchent au serveur ; le référentiel en compte plusieurs milliers. */
  protected chercher(terme: string): void {
    this.rechercheService.queryUnpaged({ search: terme }).subscribe({
      next: res => this.suggestions.set((res.body ?? []) as IDci[]),
    });
  }

  protected valider(): void {
    const cible = this.dci();
    if (!cible?.id) {
      return;
    }
    this.enCours.set(true);
    this.api.rattacherProduits(cible.id, this.produits.map(produit => produit.id!)).subscribe({
      next: res => {
        this.enCours.set(false);
        this.activeModal.close({ dci: cible, nombre: res.body ?? 0 });
      },
      error: err => {
        this.enCours.set(false);
        this.notificationService.error(
          this.errorService.getErrorMessage(err, "L'affectation de la DCI a échoué."),
          'Affectation impossible',
        );
      },
    });
  }
}
