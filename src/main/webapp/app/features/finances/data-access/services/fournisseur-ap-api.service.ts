import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from 'app/app.constants';
import {
  ICompteFournisseurAP,
  IFournisseurAPSummary,
  ILigneFournisseurAP,
  IReglementBL,
  IReglementFournisseurCommand,
  StatutLigne,
} from '../models';

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class FournisseurApApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/supplier-performance/ap';
  private readonly http = inject(HttpClient);

  getComptes(): Observable<HttpResponse<ICompteFournisseurAP[]>> {
    return this.http.get<ICompteFournisseurAP[]>(this.resourceUrl, { observe: 'response' });
  }

  getSummary(): Observable<HttpResponse<IFournisseurAPSummary>> {
    return this.http.get<IFournisseurAPSummary>(`${this.resourceUrl}/summary`, { observe: 'response' });
  }

  getLignes(
    fournisseurId: number,
    page: number,
    size: number,
    statut?: StatutLigne | null,
  ): Observable<HttpResponse<ILigneFournisseurAP[]>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (statut) params = params.set('statut', statut);
    return this.http.get<ILigneFournisseurAP[]>(
      `${SERVER_API_URL}api/supplier-performance/${fournisseurId}/ap/lignes`,
      { observe: 'response', params },
    );
  }

  enregistrerReglement(
    fournisseurId: number,
    command: IReglementFournisseurCommand,
  ): Observable<HttpResponse<void>> {
    return this.http.post<void>(
      `${SERVER_API_URL}api/supplier-performance/${fournisseurId}/ap/reglement`,
      command,
      { observe: 'response' },
    );
  }

  getReglementsBl(fournisseurId: number, commandeId: number): Observable<HttpResponse<IReglementBL[]>> {
    return this.http.get<IReglementBL[]>(
      `${SERVER_API_URL}api/supplier-performance/${fournisseurId}/ap/commandes/${commandeId}/reglements`,
      { observe: 'response' },
    );
  }
}
