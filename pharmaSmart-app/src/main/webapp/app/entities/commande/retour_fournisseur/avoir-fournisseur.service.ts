import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  IAvoirEncoursFournisseur,
  IAvoirFournisseur,
  IAvoirFournisseurCommand,
  IAvoirFromBonLignesCommand,
  AvoirFournisseurStatut,
} from 'app/shared/model/avoir-fournisseur.model';
import { ApplicationConfigService } from 'app/core/config/application-config.service';

@Injectable({ providedIn: 'root' })
export class AvoirFournisseurService {
  private readonly http = inject(HttpClient);
  private readonly applicationConfigService = inject(ApplicationConfigService);
  private readonly resourceUrl = this.applicationConfigService.getEndpointFor('api/avoirs-fournisseur');

  create(command: IAvoirFournisseurCommand): Observable<IAvoirFournisseur> {
    return this.http.post<IAvoirFournisseur>(this.resourceUrl, command);
  }

  createFromBonLignes(command: IAvoirFromBonLignesCommand): Observable<IAvoirFournisseur> {
    return this.http.post<IAvoirFournisseur>(`${this.resourceUrl}/from-bon-lignes`, command);
  }

  createFromReception(command: IAvoirFromBonLignesCommand): Observable<IAvoirFournisseur> {
    return this.http.post<IAvoirFournisseur>(`${this.resourceUrl}/from-reception`, command);
  }

  annuler(id: number, motif?: string): Observable<IAvoirFournisseur> {
    return this.http.post<IAvoirFournisseur>(`${this.resourceUrl}/${id}/annuler`, motif ? { motif } : {});
  }

  query(req?: {
    statut?: AvoirFournisseurStatut;
    fournisseurId?: number;
    dtStart?: string;
    dtEnd?: string;
    page?: number;
    size?: number;
  }): Observable<HttpResponse<IAvoirFournisseur[]>> {
    let params = new HttpParams();
    if (req?.statut) params = params.set('statut', req.statut);
    if (req?.fournisseurId != null) params = params.set('fournisseurId', req.fournisseurId);
    if (req?.dtStart) params = params.set('dtStart', req.dtStart);
    if (req?.dtEnd) params = params.set('dtEnd', req.dtEnd);
    if (req?.page != null) params = params.set('page', req.page);
    if (req?.size != null) params = params.set('size', req.size);
    return this.http.get<IAvoirFournisseur[]>(this.resourceUrl, { params, observe: 'response' });
  }

  getEncoursParFournisseur(): Observable<IAvoirEncoursFournisseur[]> {
    return this.http.get<IAvoirEncoursFournisseur[]>(`${this.resourceUrl}/encours-par-fournisseur`);
  }

  updateStatut(id: number, statut: AvoirFournisseurStatut): Observable<IAvoirFournisseur> {
    return this.http.patch<IAvoirFournisseur>(`${this.resourceUrl}/${id}/statut`, null, {
      params: { statut },
    });
  }

  countEnAttente(): Observable<number> {
    return this.http.get<number>(`${this.resourceUrl}/count-en-attente`);
  }
}
