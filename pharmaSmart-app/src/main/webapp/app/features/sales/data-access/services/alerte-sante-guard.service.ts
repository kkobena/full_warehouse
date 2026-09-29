import {inject, Injectable} from '@angular/core';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {catchError, from, map, Observable, of, switchMap} from 'rxjs';
import {CustomerService} from 'app/entities/customer/customer.service';
import {IAlerteSante} from 'app/entities/customer/customer-fiche.model';
import {NotificationService} from '../../../../shared/services/notification.service';
import {AlerteSanteModalComponent} from '../../ui/alerte-sante-modal/alerte-sante-modal.component';

/**
 * Contrôle santé à l'ajout d'un produit (fiche client, lot 2).
 *
 * <p>Une allergie à une molécule du produit ouvre une modale bloquante : le produit n'est ajouté
 * que si la dérogation est enregistrée. Grossesse et allaitement ne font qu'un rappel, affiché une
 * fois par client pour ne pas lasser au fil des lignes.
 */
@Injectable({providedIn: 'root'})
export class AlerteSanteGuardService {
  private readonly customerService = inject(CustomerService);
  private readonly modalService = inject(NgbModal);
  private readonly notificationService = inject(NotificationService);
  private readonly rappelsAffiches = new Set<string>();

  /** Émet `true` si le produit peut être ajouté à la vente de ce client. */
  verifier(customerId: number | undefined | null, produit: {id?: number; libelle?: string}): Observable<boolean> {
    if (!customerId || !produit?.id) {
      return of(true);
    }
    return this.customerService.alertesSante(customerId, produit.id).pipe(
      // Un contrôle indisponible ne doit pas arrêter le comptoir : on le signale et on laisse passer.
      catchError(() => {
        this.notificationService.warning('Vérification des allergies indisponible : contrôlez le dossier du client.', 'Alerte santé');
        return of([] as IAlerteSante[]);
      }),
      switchMap(alertes => {
        const rappels = alertes.filter(a => a.niveau === 'INFO');
        const bloquantes = alertes.filter(a => a.niveau === 'BLOQUANTE');
        if (bloquantes.length === 0) {
          this.afficherRappels(customerId, rappels);
          return of(true);
        }
        // Avec une modale ouverte, les rappels y figurent : un toast par-dessus en masquerait le formulaire.
        rappels.forEach(rappel => this.rappelsAffiches.add(`${customerId}:${rappel.type}`));
        const modalRef = this.modalService.open(AlerteSanteModalComponent, {backdrop: 'static', centered: true, size: 'md'});
        modalRef.componentInstance.customerId = customerId;
        modalRef.componentInstance.produitId = produit.id;
        modalRef.componentInstance.produitLibelle = produit.libelle ?? '';
        modalRef.componentInstance.alertes = bloquantes;
        modalRef.componentInstance.rappels = rappels;
        return from(modalRef.result).pipe(
          map(result => result === true),
          catchError(() => of(false))
        );
      })
    );
  }

  private afficherRappels(customerId: number, rappels: IAlerteSante[]): void {
    rappels.forEach(rappel => {
      const cle = `${customerId}:${rappel.type}`;
      if (!this.rappelsAffiches.has(cle)) {
        this.rappelsAffiches.add(cle);
        this.notificationService.warning(rappel.message, 'Rappel santé');
      }
    });
  }
}
