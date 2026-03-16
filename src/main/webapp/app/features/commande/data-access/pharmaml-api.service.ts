import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SERVER_API_URL } from '../../../app.constants';
import {
  IEnvoiPharmaParams,
  IInfoProduit,
  ILigneRetour,
  IPharmaMlEnvoi,
  IPharmamlCommandeResponse,
  ISubstitutionProposee,
  IVerificationResponse,
} from '../../../shared/model/pharmaml.model';

@Injectable({ providedIn: 'root' })
export class PharmamlApiService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/pharmaml';

  envoi(params: IEnvoiPharmaParams): Observable<HttpResponse<IPharmamlCommandeResponse>> {
    return this.http.post<IPharmamlCommandeResponse>(`${this.resourceUrl}/envoi`, params, { observe: 'response' });
  }

  renvoi(params: IEnvoiPharmaParams): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/renvoi`, params, { observe: 'response' });
  }

  lignesRetour(commandeRef: string, orderId: string): Observable<HttpResponse<IVerificationResponse>> {
    return this.http.get<IVerificationResponse>(`${this.resourceUrl}/retour/${commandeRef}/${orderId}`, {
      observe: 'response',
    });
  }

  reponseRupture(ruptureId: string): Observable<HttpResponse<IVerificationResponse>> {
    return this.http.get<IVerificationResponse>(`${this.resourceUrl}/rupture/${ruptureId}`, { observe: 'response' });
  }

  historique(commandeId: number, orderDate: string): Observable<HttpResponse<IPharmaMlEnvoi[]>> {
    return this.http.get<IPharmaMlEnvoi[]>(`${this.resourceUrl}/historique/${commandeId}/${orderDate}`, {
      observe: 'response',
    });
  }

  statut(envoiId: number): Observable<HttpResponse<IPharmaMlEnvoi>> {
    return this.http.get<IPharmaMlEnvoi>(`${this.resourceUrl}/statut/${envoiId}`, { observe: 'response' });
  }

  substitutions(commandeId: number, orderDate: string): Observable<HttpResponse<ISubstitutionProposee[]>> {
    return this.http.get<ISubstitutionProposee[]>(`${this.resourceUrl}/substitutions/${commandeId}/${orderDate}`, {
      observe: 'response',
    });
  }

  accepterSubstitution(id: number): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/substitution/${id}/accepter`, null, { observe: 'response' });
  }

  refuserSubstitution(id: number): Observable<HttpResponse<void>> {
    return this.http.put<void>(`${this.resourceUrl}/substitution/${id}/refuser`, null, { observe: 'response' });
  }

  accuseReception(commandeId: number, orderDate: string): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/ack-reception/${commandeId}/${orderDate}`, null, { observe: 'response' });
  }

  annulation(commandeId: number, orderDate: string, motif?: string): Observable<HttpResponse<void>> {
    const params: Record<string, string> = {};
    if (motif) params['motif'] = motif;
    return this.http.post<void>(`${this.resourceUrl}/annulation/${commandeId}/${orderDate}`, null, { observe: 'response', params });
  }

  retour(commandeId: number, orderDate: string, lignes: ILigneRetour[]): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/retour/${commandeId}/${orderDate}`, lignes, { observe: 'response' });
  }

  disponibilite(commandeId: number, orderDate: string, grossisteId?: number): Observable<HttpResponse<IInfoProduit[]>> {
    const params: Record<string, string> = {};
    if (grossisteId != null) {
      params['grossisteId'] = grossisteId.toString();
    }
    return this.http.get<IInfoProduit[]>(`${this.resourceUrl}/disponibilite/${commandeId}/${orderDate}`, {
      observe: 'response',
      params,
    });
  }
}
