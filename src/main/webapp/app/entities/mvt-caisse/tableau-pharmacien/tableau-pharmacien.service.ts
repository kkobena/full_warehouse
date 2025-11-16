import { inject, Injectable } from '@angular/core';
import { SERVER_API_URL } from '../../../app.constants';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../../shared/util/request-util';
import { TableauPharmacienWrapper } from './tableau-pharmacien.model';
import { IGroupeFournisseur } from '../../../shared/model/groupe-fournisseur.model';

@Injectable({
  providedIn: 'root',
})
export class TableauPharmacienService {
  public resourceUrl = SERVER_API_URL + 'api/';
  protected http = inject(HttpClient);

  query(req?: any): Observable<HttpResponse<TableauPharmacienWrapper>> {
    const options = createRequestOptions(req);
    return this.http.get<TableauPharmacienWrapper>(this.resourceUrl + 'tableau-pharmacien', {
      params: options,
      observe: 'response',
    });
  }

  exportToPdf(req: any): Observable<Blob> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/tableau-pharmacien/pdf`, {
      params: options,
      responseType: 'blob',
    });
  }

  fetchGroupGrossisteToDisplay(req?: any): Observable<HttpResponse<IGroupeFournisseur[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IGroupeFournisseur[]>(this.resourceUrl + 'top-groupe-fournisseurs', {
      params: options,
      observe: 'response',
    });
  }

  exportToExcel(req: any): Observable<HttpResponse<Blob>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/tableau-pharmacien/excel`, {
      params: options,
      observe: 'response',
      responseType: 'blob',
    });
  }
}
