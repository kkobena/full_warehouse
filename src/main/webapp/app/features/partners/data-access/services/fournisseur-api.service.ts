import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";
import { SERVER_API_URL } from "../../../../app.constants";
import { createRequestOptions } from "../../../../shared/util/request-util";
import { IFournisseur } from "../../../../shared/model/fournisseur.model";
import { IResponseDto } from "../../../../shared/util/response-dto";

@Injectable({ providedIn: "root" })
export class FournisseurApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + "api/fournisseurs";

  query(req?: any): Observable<HttpResponse<IFournisseur[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IFournisseur[]>(this.resourceUrl, {
      params: options,
      observe: "response"
    });
  }

  queryParents(req?: any): Observable<HttpResponse<IFournisseur[]>> {
    const options = createRequestOptions(req);
    return this.http.get<IFournisseur[]>(`${this.resourceUrl}/parents`, { params: options, observe: "response" });
  }

  findAgences(parentId: number): Observable<IFournisseur[]> {
    return this.http.get<IFournisseur[]>(`${this.resourceUrl}/${parentId}/agences`, { observe: "response" })
      .pipe(map(res => res.body ?? []));
  }

  create(fournisseur: IFournisseur): Observable<IFournisseur> {
    return this.http.post<IFournisseur>(this.resourceUrl, fournisseur, { observe: "response" })
      .pipe(map(res => res.body!));
  }

  update(fournisseur: IFournisseur): Observable<IFournisseur> {
    return this.http.put<IFournisseur>(this.resourceUrl, fournisseur, { observe: "response" })
      .pipe(map(res => res.body!));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { observe: "response" })
      .pipe(map((): void => void 0));
  }

  uploadFile(file: any): Observable<HttpResponse<IResponseDto>> {
    return this.http.post<IResponseDto>(`${this.resourceUrl}/importcsv`, file, { observe: "response" });
  }
}
