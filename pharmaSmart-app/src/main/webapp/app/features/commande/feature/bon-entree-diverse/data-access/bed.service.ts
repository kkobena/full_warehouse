import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpResponse } from "@angular/common/http";
import { Observable } from "rxjs";
import { SERVER_API_URL } from "app/app.constants";
import { IBed, IBedLigne, IBedSummary, MotifBed } from "./bed.model";
import { createRequestOptions } from "app/shared/util/request-util";


export type EntityBedArrayResponseType = HttpResponse<IBedSummary[]>;

@Injectable({ providedIn: "root" })
export class BedService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = `${SERVER_API_URL}api/beds`;

  create(bed: IBed): Observable<IBed> {
    return this.http.post<IBed>(this.resourceUrl, bed);
  }

  findAll(params: {
    search?: string;
    motifBed?: MotifBed;
    orderStatus?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Observable<EntityBedArrayResponseType> {
    const options = createRequestOptions(params);
    return this.http.get<IBedSummary[]>(this.resourceUrl, { params: options, observe: "response" });
  }

  findById(id: number, orderDate: string): Observable<IBed> {
    return this.http.get<IBed>(`${this.resourceUrl}/${id}`, { params: { orderDate } });
  }

  addLigne(id: number, orderDate: string, ligne: IBedLigne): Observable<IBed> {
    return this.http.post<IBed>(`${this.resourceUrl}/${id}/lignes`, ligne, { params: { orderDate } });
  }

  updateLigne(id: number, orderDate: string, ligneId: number, ligneDate: string, ligne: IBedLigne): Observable<IBed> {
    return this.http.patch<IBed>(`${this.resourceUrl}/${id}/lignes/${ligneId}`, ligne, {
      params: { orderDate, ligneDate },
    });
  }

  removeLigne(id: number, orderDate: string, ligneId: number, ligneDate: string): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}/lignes/${ligneId}`, {
      params: { orderDate, ligneDate }
    });
  }

  validate(id: number, req: { orderDate: string; motif?: string; fournisseurId?: number; commentaire?: string }): Observable<IBed> {
    const params: Record<string, string> = { orderDate: req.orderDate };
    if (req.motif) params['motif'] = req.motif;
    if (req.fournisseurId) params['fournisseurId'] = String(req.fournisseurId);
    if (req.commentaire) params['commentaire'] = req.commentaire;
    return this.http.post<IBed>(`${this.resourceUrl}/${id}/validate`, null, { params });
  }

  delete(id: number, orderDate: string): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/${id}`, { params: { orderDate } });
  }
}
