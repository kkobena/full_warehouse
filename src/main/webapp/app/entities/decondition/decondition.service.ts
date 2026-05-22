import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { IDecondition } from 'app/shared/model/decondition.model';

type EntityResponseType = HttpResponse<IDecondition>;
type EntityArrayResponseType = HttpResponse<IDecondition[]>;

@Injectable({ providedIn: 'root' })
export class DeconditionService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/deconditions';

  create(decondition: IDecondition): Observable<EntityResponseType> {
    return this.http.post<IDecondition>(this.resourceUrl, decondition, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IDecondition[]>(this.resourceUrl, { params: options, observe: 'response' });
  }
}
