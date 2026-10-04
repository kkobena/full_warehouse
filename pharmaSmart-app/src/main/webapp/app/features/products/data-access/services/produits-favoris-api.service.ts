import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { SERVER_API_URL } from "app/app.constants";
import { ProduitSearch } from "app/shared/model";

/** Un produit à épingler, proposé d'après les ventes des 30 derniers jours au comptoir (sans ordonnance, hors déjà épinglés). */
export interface FavoriSuggere {
  produit: ProduitSearch;
  /** Nombre de ventes (tickets) distinctes où le produit figure. */
  nbVentes: number;
  qteVendue: number;
}

/**
 * Produits favoris du comptoir : la grille de la vente comptant. La lire est ouvert à tout caissier ; épingler, retirer,
 * ordonner et lire les suggestions relèvent du catalogue.
 */
@Injectable({ providedIn: "root" })
export class ProduitsFavorisApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + "api/produits-favoris";

  /** Les favoris du magasin, au format de la recherche (prix et stock du rayon compris), dans l'ordre de la grille. */
  lister(): Observable<ProduitSearch[]> {
    return this.http.get<ProduitSearch[]>(this.resourceUrl);
  }

  /** Les produits les plus vendus au comptoir qui ne sont pas encore épinglés : de quoi remplir la grille sans la chercher. */
  suggerer(limite = 12): Observable<FavoriSuggere[]> {
    return this.http.get<FavoriSuggere[]>(`${this.resourceUrl}/suggestions`, { params: new HttpParams().set("limit", limite) });
  }

  ajouter(produitId: number): Observable<void> {
    return this.http.put<void>(`${this.resourceUrl}/${produitId}`, null);
  }

  retirer(produitId: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${produitId}`);
  }

  reordonner(produitIds: number[]): Observable<void> {
    return this.http.put<void>(`${this.resourceUrl}/ordre`, produitIds);
  }
}
