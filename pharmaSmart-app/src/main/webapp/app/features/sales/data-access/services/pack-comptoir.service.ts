import { Injectable, inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Observable, catchError, concatMap, filter, from, of, take, timeout } from 'rxjs';
import { ProduitSearch } from '../../../../shared/model';
import { NotificationService } from '../../../../shared/services/notification.service';
import { PromotionApiService } from '../../../promotion/data-access/services/promotion-api.service';
import { IPackComptoir } from '../../../promotion/models/promotion.model';
import { SalesFacade } from '../facades/sales.facade';

/** Un article de pack peut attendre une décision du caissier (stock insuffisant, allergie) : on lui laisse le temps. */
const ARTICLE_TIMEOUT_MS = 60_000;

/**
 * Le scan d'un pack monté par l'officine (plan des promotions, §5.8) : l'étiquette collée sur
 * l'emballage porte le code du pack, et le comptoir ajoute ses articles au panier, chacun sur sa ligne
 * et à sa quantité. Le serveur reconnaît ensuite le pack complet et pose la remise — rien n'est
 * calculé ici.
 *
 * Partagé par les deux chemins d'un scan : le champ de recherche du comptoir, et le scanner global.
 */
@Injectable({ providedIn: 'root' })
export class PackComptoirService {
  private readonly promotionApi = inject(PromotionApiService);
  private readonly facade = inject(SalesFacade);
  private readonly notification = inject(NotificationService);
  private readonly evenements$ = toObservable(this.facade.lastEvent);

  /** Le pack en vigueur qui porte ce code ; nul s'il n'y en a pas (le code est alors simplement inconnu). */
  chercher(code: string): Observable<IPackComptoir | null> {
    return this.promotionApi.pack(code).pipe(catchError(() => of(null)));
  }

  /**
   * Les articles l'un après l'autre : le premier crée la vente, les suivants s'y ajoutent. Les envoyer
   * ensemble créerait plusieurs ventes. Chacun attend la confirmation du précédent.
   *
   * @param ajouterArticle le geste d'ajout ordinaire du comptoir, contrôles de stock et d'allergie compris
   */
  ajouter(pack: IPackComptoir, ajouterArticle: (produit: ProduitSearch, quantite: number) => void): void {
    let ajoutes = 0;
    from(pack.composants)
      .pipe(
        concatMap(composant => {
          // `productAddedSuccess$` rejoue le dernier évènement à l'abonnement : on attend un ajout
          // POSTÉRIEUR à cet envoi, reconnu à son numéro de séquence.
          const avant = this.facade.lastEvent()?.seq ?? 0;
          const confirme = this.evenements$.pipe(
            filter(e => e?.type === 'PRODUCT_ADDED' && e.seq > avant),
            take(1),
            timeout(ARTICLE_TIMEOUT_MS),
          );
          ajouterArticle(
            Object.assign(new ProduitSearch(), {
              id: composant.produitId,
              libelle: composant.libelle,
              regularUnitPrice: composant.regularUnitPrice,
              itemQty: composant.itemQty,
              parentId: composant.parentId ?? undefined,
            }),
            composant.quantite,
          );
          return confirme;
        }),
      )
      .subscribe({
        next: () => ajoutes++,
        error: () =>
          this.notification.warning(
            `Pack « ${pack.libelle} » incomplet : ${ajoutes} article(s) sur ${pack.composants.length} ajouté(s). Complétez-le à la main.`,
            'Scan',
          ),
      });
  }
}
