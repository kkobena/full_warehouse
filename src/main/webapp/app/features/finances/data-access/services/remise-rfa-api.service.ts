import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IRemiseRfaFournisseur, IAvoirFournisseur } from '../models';

@Injectable({ providedIn: 'root' })
export class RemiseRfaApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/remises-rfa';
  private readonly http = inject(HttpClient);

  getRfaFournisseurs(): Observable<HttpResponse<IRemiseRfaFournisseur[]>> {
    return this.http.get<IRemiseRfaFournisseur[]>(this.resourceUrl, { observe: 'response' });
  }

  getAvoirsFournisseurs(): Observable<HttpResponse<IAvoirFournisseur[]>> {
    return this.http.get<IAvoirFournisseur[]>(`${this.resourceUrl}/avoirs`, { observe: 'response' });
  }
}
