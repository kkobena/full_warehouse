import { HttpClient, HttpResourceRef, HttpResponse, httpResource } from '@angular/common/http';
import { inject, Injectable, Signal } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IResponseDto } from '../../../../shared/util/response-dto';
import { IDci, IDciProduit } from '../../models/dci.model';

export interface DciQuery {
  page: number;
  size: number;
  search: string;
}

/**
 * Accès aux DCI.
 *
 * La LECTURE passe par `httpResource` : la requête se recalcule d'elle-même quand
 * les signaux de recherche ou de pagination changent, ce qui évite d'orchestrer
 * les abonnements à la main.
 *
 * Les ÉCRITURES restent en `HttpClient` : ce sont des actions ponctuelles
 * déclenchées par l'utilisateur, pas un état dérivé. Une ressource réactive y
 * rendrait le moment de l'envoi implicite, ce qu'on ne veut pas pour un POST.
 */
@Injectable({ providedIn: 'root' })
export class DciApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/dci';

  /**
   * Ressource paginée, recalculée à chaque changement de `query`.
   *
   * Le total ne figure pas dans le corps mais dans l'en-tête `X-Total-Count`, posé
   * par `PaginationUtil` côté serveur : il se lit via `ref.headers()`, d'où
   * l'utilitaire {@link totalFromHeaders} plutôt qu'une seconde requête.
   */
  pagedResource(query: Signal<DciQuery>): HttpResourceRef<IDci[]> {
    return httpResource<IDci[]>(
      () => ({
        url: this.resourceUrl,
        params: {
          page: query().page,
          size: query().size,
          search: query().search,
        },
      }),
      { defaultValue: [] },
    );
  }

  /** Lit le total paginé depuis l'en-tête de la ressource. */
  totalFromHeaders(ref: HttpResourceRef<IDci[]>): number {
    const brut = ref.headers()?.get('X-Total-Count');
    return brut ? Number(brut) : ref.value().length;
  }

  /** Produits portant cette DCI. Alimente le panneau de détail. */
  produits(dciId: number): Observable<IDciProduit[]> {
    return this.http.get<IDciProduit[]>(`${this.resourceUrl}/${dciId}/produits`);
  }

  create(dci: IDci): Observable<HttpResponse<IDci>> {
    return this.http.post<IDci>(this.resourceUrl, dci, { observe: 'response' });
  }

  update(dci: IDci): Observable<HttpResponse<IDci>> {
    return this.http.put<IDci>(this.resourceUrl, dci, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<object>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  /** Import CSV « code;libelle ». La part doit s'appeler `importcsv` côté serveur. */
  uploadFile(file: FormData): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
