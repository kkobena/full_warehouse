import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { CatalogueExports, DemandeExport, ExportFichier, ExportModele } from '../models/exports.model';

@Injectable({ providedIn: 'root' })
export class ExportsApiService {
  private readonly resourceUrl = SERVER_API_URL + 'api/exports';
  private readonly http = inject(HttpClient);

  lireCatalogue(): Observable<CatalogueExports> {
    return this.http.get<CatalogueExports>(`${this.resourceUrl}/catalogue`);
  }

  demander(demande: DemandeExport): Observable<ExportFichier> {
    return this.http.post<ExportFichier>(`${this.resourceUrl}/fichiers`, demande);
  }

  listerHistorique(page: number, taille: number): Observable<HttpResponse<ExportFichier[]>> {
    const params = new HttpParams().set('page', page).set('size', taille);
    return this.http.get<ExportFichier[]>(`${this.resourceUrl}/fichiers`, { params, observe: 'response' });
  }

  telecharger(id: number): Observable<Blob> {
    return this.http.get(`${this.resourceUrl}/fichiers/${id}/contenu`, { responseType: 'blob' });
  }

  listerModeles(): Observable<ExportModele[]> {
    return this.http.get<ExportModele[]>(`${this.resourceUrl}/modeles`);
  }

  enregistrerModele(modele: ExportModele): Observable<ExportModele> {
    return this.http.post<ExportModele>(`${this.resourceUrl}/modeles`, modele);
  }

  supprimerModele(id: number): Observable<void> {
    return this.http.delete<void>(`${this.resourceUrl}/modeles/${id}`);
  }

  executerModele(id: number): Observable<ExportFichier> {
    return this.http.post<ExportFichier>(`${this.resourceUrl}/modeles/${id}/executions`, null);
  }
}
