import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IRayon } from '../../models/rayon.model';
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IResponseDto } from '../../../../shared/util/response-dto';

export interface RayonQueryParams {
  page?: number;
  size?: number;
  search?: string;
  storageId?: number;
  typeZone?: string;
}

@Injectable({ providedIn: 'root' })
export class RayonApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/rayons';

  query(params?: RayonQueryParams): Observable<HttpResponse<IRayon[]>> {
    const options = createRequestOptions(params);
    return this.http.get<IRayon[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  find(id: number): Observable<HttpResponse<IRayon>> {
    return this.http.get<IRayon>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  create(rayon: IRayon): Observable<HttpResponse<IRayon>> {
    return this.http.post<IRayon>(this.resourceUrl, rayon, { observe: 'response' });
  }

  update(rayon: IRayon): Observable<HttpResponse<IRayon>> {
    return this.http.put<IRayon>(this.resourceUrl, rayon, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  uploadFile(file: FormData): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: 'response' });
  }

  cloner(rayons: IRayon[], storageId: number): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/clone/${storageId}`, rayons, { observe: 'response' });
  }

  exportCsv(storageId?: number): Observable<HttpResponse<Blob>> {
    const params = storageId != null ? { storageId: String(storageId) } : {};
    return this.http.get(`${this.resourceUrl}/export`, {
      params,
      observe: 'response',
      responseType: 'blob',
    });
  }
}
