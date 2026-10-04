import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { ProduitSearch } from '../../../../shared/model';

/** Un équivalent proposé au comptoir : un produit ajoutable au panier, et la nature de son lien avec l'original. */
export interface SubstitutPropose {
  produit: ProduitSearch;
  /** `THERAPEUTIQUE` relève d'un avis du pharmacien ; `GENERIQUE` est le cas courant. */
  typeSubstitut: 'GENERIQUE' | 'THERAPEUTIQUE';
  typeSubstitutLibelle: string;
}

@Injectable({ providedIn: 'root' })
export class SubstitutsApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/produits';

  /** Les substituts du produit qui ont du stock au rayon : génériques d'abord, du moins cher au plus cher. */
  lireDisponibles(produitId: number): Observable<SubstitutPropose[]> {
    return this.http.get<SubstitutPropose[]>(`${this.resourceUrl}/${produitId}/substituts-disponibles`);
  }
}
