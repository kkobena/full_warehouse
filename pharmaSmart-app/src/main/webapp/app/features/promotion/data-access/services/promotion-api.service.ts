import { HttpClient, HttpResourceRef, httpResource } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IBilanPromotion, IChoixPerimetre, IPackComptoir, IPerimetre, IPromotion, IPromotionForm } from '../../models/promotion.model';

/**
 * Accès aux promotions du magasin de l'utilisateur.
 *
 * Même partage que les autres référentiels : la LECTURE de la liste passe par `httpResource`
 * (rechargée par `reload()` après une écriture), les ÉCRITURES par `HttpClient`, parce que ce sont
 * des gestes ponctuels et non un état dérivé.
 */
@Injectable({ providedIn: 'root' })
export class PromotionApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/promotions';

  /** La liste n'est pas paginée : un magasin compte quelques dizaines d'opérations par an. */
  listResource(): HttpResourceRef<IPromotion[]> {
    return httpResource<IPromotion[]>(() => this.resourceUrl, { defaultValue: [] });
  }

  /** Les rayons du magasin et les familles, pour remplir le périmètre d'un geste. */
  choixPerimetre(): Observable<IChoixPerimetre> {
    return this.http.get<IChoixPerimetre>(`${this.resourceUrl}/perimetre/choix`);
  }

  /** Les articles d'un rayon ou d'une famille ; rien n'est enregistré. */
  perimetre(critere: { rayonId: number } | { familleId: number }): Observable<IPerimetre> {
    return this.http.get<IPerimetre>(`${this.resourceUrl}/perimetre`, { params: critere });
  }

  /** Le pack scanné au comptoir : ses articles, à ajouter au panier. Erreur si aucun pack en vigueur ne porte ce code. */
  pack(code: string): Observable<IPackComptoir> {
    return this.http.get<IPackComptoir>(`${this.resourceUrl}/pack`, { params: { code } });
  }

  /** Ce que l'opération a coûté, ce qu'elle a fait vendre, et l'effet sur la marge de ses articles. */
  bilan(id: number): Observable<IBilanPromotion> {
    return this.http.get<IBilanPromotion>(`${this.resourceUrl}/${id}/bilan`);
  }

  /** Le bilan en CSV, une ligne par article (droit d'export). */
  bilanCsv(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/bilan/csv`, { responseType: 'blob' });
  }

  find(id: number): Observable<IPromotion> {
    return this.http.get<IPromotion>(`${this.resourceUrl}/${id}`);
  }

  create(form: IPromotionForm): Observable<IPromotion> {
    return this.http.post<IPromotion>(this.resourceUrl, form);
  }

  update(id: number, form: IPromotionForm): Observable<IPromotion> {
    return this.http.put<IPromotion>(`${this.resourceUrl}/${id}`, form);
  }

  valider(id: number): Observable<IPromotion> {
    return this.http.put<IPromotion>(`${this.resourceUrl}/${id}/valider`, null);
  }

  suspendre(id: number): Observable<IPromotion> {
    return this.http.put<IPromotion>(`${this.resourceUrl}/${id}/suspendre`, null);
  }

  annuler(id: number): Observable<IPromotion> {
    return this.http.put<IPromotion>(`${this.resourceUrl}/${id}/annuler`, null);
  }

  /** Étiquettes de rayon « PROMO » : une par article remisé, `qty` fois chacune ; pour un pack, l'étiquette de son emballage. */
  etiquettes(id: number, qty: number, startAt: number, format?: string): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes`, {
      params: { qty: qty.toString(), startAt: startAt.toString(), ...(format ? { format } : {}) },
      responseType: 'blob',
    });
  }

  etiquettesZpl(id: number, qty: number, format?: string, resolution?: string): Observable<string> {
    return this.http.get(`${this.resourceUrl}/${id}/etiquettes/zpl`, {
      params: { qty: qty.toString(), ...(format ? { format } : {}), ...(resolution ? { resolution } : {}) },
      responseType: 'text',
    });
  }
}
