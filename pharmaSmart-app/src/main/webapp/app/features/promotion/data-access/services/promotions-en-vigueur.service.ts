import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, of } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IPromotionArticle } from '../../models/promotion.model';

/**
 * Les articles en promotion aujourd'hui dans le magasin de la session, partagés par le catalogue
 * et la grille des favoris du comptoir. Lecture ouverte à tous les rôles : le caissier voit le
 * badge sans gérer les promotions.
 *
 * Le badge informe, il ne décide pas : au comptoir, c'est le moteur du serveur qui pose la remise.
 */
@Injectable({ providedIn: 'root' })
export class PromotionsEnVigueurService {
  readonly parProduit = signal<ReadonlyMap<number, IPromotionArticle>>(new Map());

  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/promotions/en-vigueur';

  /** Relit la liste. Un échec vide les badges plutôt que d'en afficher de périmés. */
  charger(): void {
    this.http
      .get<IPromotionArticle[]>(this.resourceUrl)
      .pipe(catchError(() => of<IPromotionArticle[]>([])))
      .subscribe(articles => this.parProduit.set(new Map(articles.map(a => [a.produitId, a]))));
  }

  pour(produitId: number | null | undefined): IPromotionArticle | undefined {
    return produitId == null ? undefined : this.parProduit().get(produitId);
  }
}

/** « Opération été : 3 596 F au lieu de 4 495 F, dès 3 unités, jusqu'au 31/12/2026 ». */
export function decrirePromotion(article: IPromotionArticle, montant: (valeur: number) => string): string {
  const [annee, mois, jour] = article.dateFin.split('-');
  if (article.offre) {
    // Une offre N+M n'a pas de prix unitaire remisé : on la dit telle qu'en rayon.
    return `${article.libelle} : ${article.offre}, jusqu'au ${jour}/${mois}/${annee}`;
  }
  const seuil = article.quantiteMin > 1 ? `, dès ${article.quantiteMin} unités` : '';
  return (
    `${article.libelle} : ${montant(article.prixApresPromo)} au lieu de ${montant(article.prixReference)}` +
    `${seuil}, jusqu'au ${jour}/${mois}/${annee}`
  );
}
