import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOptions } from 'app/shared/util/request-util';
import { IRetourDepot } from 'app/shared/model/retour-depot.model';

type EntityResponseType = HttpResponse<IRetourDepot>;
type EntityArrayResponseType = HttpResponse<IRetourDepot[]>;

@Injectable({ providedIn: 'root' })
export class RetourDepotService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/retour-depots';

  create(retourDepot: IRetourDepot): Observable<EntityResponseType> {
    return this.http.post<IRetourDepot>(this.resourceUrl, retourDepot, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<IRetourDepot[]>(this.resourceUrl, {
      params: options,
      observe: 'response'
    });
  }


}
