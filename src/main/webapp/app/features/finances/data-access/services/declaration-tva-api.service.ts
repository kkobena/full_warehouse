import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import { IDeclarationTvaSummary, IDeclarationTvaParams } from '../models';

@Injectable({ providedIn: 'root' })
export class DeclarationTvaApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/taxe-report';
  private readonly http = inject(HttpClient);

  getDeclaration(params: IDeclarationTvaParams): Observable<HttpResponse<IDeclarationTvaSummary>> {
    let httpParams = new HttpParams()
      .set('startDate', params.startDate)
      .set('endDate', params.endDate);
    if (params.typeTva) {
      httpParams = httpParams.set('typeTva', params.typeTva);
    }
    return this.http.get<IDeclarationTvaSummary>(`${this.resourceUrl}/declaration`, {
      params: httpParams,
      observe: 'response',
    });
  }

  exportToPdf(params: IDeclarationTvaParams): Observable<HttpResponse<Blob>> {
    let httpParams = new HttpParams()
      .set('startDate', params.startDate)
      .set('endDate', params.endDate);
    if (params.typeTva) {
      httpParams = httpParams.set('typeTva', params.typeTva);
    }
    return this.http.get(`${this.resourceUrl}/declaration/pdf`, {
      params: httpParams,
      observe: 'response',
      responseType: 'blob',
    });
  }
}
