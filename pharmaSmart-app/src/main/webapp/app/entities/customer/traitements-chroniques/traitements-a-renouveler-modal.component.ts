import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { BadgeComponent, ButtonComponent, CardComponent, DataTableComponent } from 'app/shared/ui';
import { ErrorService } from 'app/shared/error.service';
import { NotificationService } from 'app/shared/services/notification.service';
import { CustomerService } from '../customer.service';
import { ITraitementARenouveler } from '../customer-fiche.model';
import { echeanceTraitement, libelleTraitement, SUIVI_TRAITEMENT } from './suivi-traitement';

/**
 * Patients dont un traitement chronique arrive à échéance, est en retard ou interrompu, les plus
 * urgents d'abord. Choisir une ligne ferme la liste et ouvre la fiche du patient.
 */
@Component({
  selector: 'app-traitements-a-renouveler-modal',
  templateUrl: './traitements-a-renouveler-modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, BadgeComponent, ButtonComponent, CardComponent, DataTableComponent],
})
export class TraitementsARenouvelerModalComponent implements OnInit {
  protected readonly lignes = signal<ITraitementARenouveler[]>([]);
  protected readonly chargement = signal(true);
  protected readonly suivis = SUIVI_TRAITEMENT;
  protected readonly libelle = libelleTraitement;
  protected readonly echeance = echeanceTraitement;

  protected readonly activeModal = inject(NgbActiveModal);
  private readonly customerService = inject(CustomerService);
  private readonly notificationService = inject(NotificationService);
  private readonly errorService = inject(ErrorService);

  ngOnInit(): void {
    this.customerService.traitementsARenouveler().subscribe({
      next: lignes => {
        this.lignes.set(lignes);
        this.chargement.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.chargement.set(false);
        this.notificationService.error(this.errorService.getErrorMessage(err, "Les renouvellements n'ont pas pu être chargés"));
      },
    });
  }

  protected ouvrirFiche(ligne: ITraitementARenouveler): void {
    this.activeModal.close(ligne.customerId);
  }
}
