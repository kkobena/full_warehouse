import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../../app.constants';
import { IResponseDto } from '../../../../shared/util/response-dto';
import { IProduit } from "../../../../shared/model";
import { createRequestOptions } from '../../../../shared/util/request-util';
import { IRayonProduit } from "../../models/rayon-produit.model";

export interface ProduitsRayonParams {
  page?: number;
  size?: number;
  rayonId: number;
}

@Injectable({ providedIn: 'root' })
export class RayonProduitApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/rayon-produits';
  private readonly produitUrl = SERVER_API_URL + 'api/produits';

  queryByRayon(params: ProduitsRayonParams): Observable<HttpResponse<IProduit[]>> {
    const options = createRequestOptions(params);
    return this.http.get<IProduit[]>(this.produitUrl, { params: options, observe: 'response' });
  }

  create(rayonProduit: IRayonProduit): Observable<HttpResponse<IRayonProduit>> {
    return this.http.post<IRayonProduit>(this.resourceUrl, rayonProduit, { observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  searchProduits(query: string): Observable<HttpResponse<IProduit[]>> {
    const options = createRequestOptions({ query, size: 20 });
    return this.http.get<IProduit[]>(this.produitUrl, { params: options, observe: 'response' });
  }

  importCsv(file: FormData, storageId: number): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/import?storageId=${storageId}`, file, { observe: 'response' });
  }
}
