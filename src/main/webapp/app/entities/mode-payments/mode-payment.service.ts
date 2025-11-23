import { inject, Injectable } from '@angular/core';
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
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/payment-modes';

  update(paymentMode: IPaymentMode, qrCodeFile?: File | null): Observable<EntityResponseType> {
    const formData = new FormData();
    formData.append('libelle', paymentMode.libelle || '');
    formData.append('order', paymentMode.order?.toString() || '0');

    if (qrCodeFile) {
      formData.append('qrCodeFile', qrCodeFile);
    }

    return this.http.put<IPaymentMode>(`${this.resourceUrl}/${paymentMode.code}`, formData, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<IPaymentMode[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  find(code: string): Observable<EntityResponseType> {
    return this.http.get<IPaymentMode>(`${this.resourceUrl}/${code}`, { observe: 'response' });
  }

  removeQrCode(code: string): Observable<EntityResponseType> {
    return this.http.delete<IPaymentMode>(`${this.resourceUrl}/${code}/qr-code`, { observe: 'response' });
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }
}
