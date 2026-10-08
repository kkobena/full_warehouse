import {inject, Injectable} from '@angular/core';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {catchError, from, map, Observable, of, switchMap} from 'rxjs';
import {CustomerService} from 'app/entities/customer/customer.service';
import {IAlerteSante, IControleResultat} from 'app/entities/customer/customer-fiche.model';
import {NotificationService} from '../../../../shared/services/notification.service';
import {AlerteSanteModalComponent} from '../../ui/alerte-sante-modal/alerte-sante-modal.component';
import {ControleOrdonnanceModalComponent} from '../../ui/controle-ordonnance-modal/controle-ordonnance-modal.component';

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

  /**
   * Contrôle d'ordonnance du panier une fois le produit ajouté en pensée (lot 2) : interactions,
   * redondances, contre-indications. Seules les alertes qui concernent CE produit s'affichent, pour ne
   * pas répéter celles déjà prises en compte aux ajouts précédents. Émet `true` si l'ajout peut se faire.
   */
  controler(customerId: number | undefined | null, produit: {id?: number; libelle?: string}, panierIds: number[]): Observable<boolean> {
    if (!customerId || !produit?.id) {
      return of(true);
    }
    const produitIds = Array.from(new Set([...panierIds, produit.id]));
    return this.customerService.controlerPanier(customerId, produitIds).pipe(
      // Un contrôle indisponible ne doit ni arrêter ni gêner le comptoir : on laisse passer, sans message.
      catchError(() => of(null as IControleResultat | null)),
      switchMap(resultat => {
        if (!resultat) {
          return of(true);
        }
        const alertes = resultat.alertes.filter(a => a.produitIds.includes(produit.id as number));
        if (alertes.length === 0) {
          return of(true);
        }
        // Rien ne bloque sauf un niveau paramétré bloquant : les autres alertes sont de simples avertissements.
        if (!alertes.some(a => a.bloquant)) {
          alertes.forEach(a => this.notificationService.warning(a.message + (a.conduite ? ` — ${a.conduite}` : ''), 'Contrôle ordonnance'));
          return of(true);
        }
        const modalRef = this.modalService.open(ControleOrdonnanceModalComponent, {backdrop: 'static', centered: true, size: 'lg'});
        modalRef.componentInstance.customerId = customerId;
        modalRef.componentInstance.produitLibelle = produit.libelle ?? '';
        modalRef.componentInstance.produitIds = produitIds;
        modalRef.componentInstance.alertes = alertes;
        modalRef.componentInstance.limite = resultat.limite;
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
