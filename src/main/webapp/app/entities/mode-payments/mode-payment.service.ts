import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../app.constants';
import { createRequestOption } from '../../shared/util/request-util';
import { IResponseDto } from '../../shared/util/response-dto';
import { IPaymentMode } from '../../shared/model/payment-mode.model';

type EntityResponseType = HttpResponse<IPaymentMode>;
type EntityArrayResponseType = HttpResponse<IPaymentMode[]>;

@Injectable({
  providedIn: 'root',
})
export class ModePaymentService {
  public resourceUrl = SERVER_API_URL + 'api/payment-modes';

  constructor(protected http: HttpClient) {}

  update(tableau: IPaymentMode): Observable<EntityResponseType> {
    return this.http.put<IPaymentMode>(this.resourceUrl, tableau, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IPaymentMode[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
