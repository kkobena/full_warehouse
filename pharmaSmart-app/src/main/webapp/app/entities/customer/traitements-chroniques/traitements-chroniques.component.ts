import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal, untracked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbModal, NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent } from 'app/shared/ui';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { NgbConfirmDialogService } from 'app/shared/dialog/ngb-confirm-dialog/ngb-confirm-dialog.directive';
import { showCommonModal } from '../../sales/selling-home/sale-helper';
import { CustomerService } from '../customer.service';
import { ITraitementChronique } from '../customer-fiche.model';
import { TraitementChroniqueFormComponent } from './traitement-chronique-form.component';
import { echeanceTraitement, libelleTraitement, SUIVI_TRAITEMENT } from './suivi-traitement';

/** Traitements chroniques du patient et leur échéance . */
@Component({
  selector: 'app-traitements-chroniques',
  templateUrl: './traitements-chroniques.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, NgbTooltip, BadgeComponent, ButtonComponent, DataTableComponent, DetailGridComponent, DetailSectionComponent],
})
export class TraitementsChroniquesComponent {
  readonly customerId = input.required<number>();
  readonly canEdit = input<boolean>(false);
  /** Liste à jour après chaque lecture ou modification : alimente les pastilles de la fiche. */
  readonly changed = output<ITraitementChronique[]>();

  protected readonly traitements = signal<ITraitementChronique[]>([]);
  protected readonly suivis = SUIVI_TRAITEMENT;
  protected readonly libelle = libelleTraitement;
  protected readonly echeance = echeanceTraitement;

  private readonly customerService = inject(CustomerService);
  private readonly modalService = inject(NgbModal);
  private readonly confirmDialog = inject(NgbConfirmDialogService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  constructor() {
    effect(() => {
      const id = this.customerId();
      untracked(() => this.charger(id));
    });
  }

  protected declarer(): void {
    this.ouvrirFormulaire(null);
  }

  protected modifier(traitement: ITraitementChronique): void {
    this.ouvrirFormulaire(traitement);
  }

  protected basculer(traitement: ITraitementChronique): void {
    const arreter = traitement.actif;
    this.confirmDialog.onConfirm(
      () =>
        this.customerService
          .modifierTraitement(this.customerId(), traitement.id, {
            dciId: traitement.dciId,
            produitId: traitement.produitId,
            dosage: traitement.dosage,
            posologie: traitement.posologie,
            dureeJours: traitement.dureeJours,
            dateOrdonnance: traitement.dateOrdonnance,
            dateFinOrdonnance: traitement.dateFinOrdonnance,
            note: traitement.note,
            actif: !arreter,
          })
          .subscribe({
            next: () => this.charger(this.customerId()),
            error: (err: HttpErrorResponse) => this.notificationService.error(this.errorService.getErrorMessage(err, "L'opération a échoué")),
          }),
      arreter ? 'ARRÊT DU TRAITEMENT' : 'REPRISE DU TRAITEMENT',
      arreter
        ? `Arrêter le suivi de ${libelleTraitement(traitement)} ? Il restera visible, sans rappel de renouvellement.`
        : `Reprendre le suivi de ${libelleTraitement(traitement)} ?`,
    );
  }

  private ouvrirFormulaire(traitement: ITraitementChronique | null): void {
    showCommonModal(
      this.modalService,
      TraitementChroniqueFormComponent,
      { customerId: this.customerId(), traitement },
      (resultat: ITraitementChronique) => {
        if (resultat) {
          this.charger(this.customerId());
        }
      },
      'lg',
    );
  }

  private charger(customerId: number): void {
    this.customerService.traitementsChroniques(customerId).subscribe({
      next: traitements => {
        this.traitements.set(traitements);
        this.changed.emit(traitements);
      },
      error: (err: HttpErrorResponse) =>
        this.notificationService.error(this.errorService.getErrorMessage(err, "Les traitements chroniques n'ont pas pu être chargés")),
    });
  }
}
