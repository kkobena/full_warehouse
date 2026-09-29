import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared/util/request-util';
import { ICustomer } from 'app/shared/model/customer.model';
import { IClientTiersPayant } from 'app/shared/model/client-tiers-payant.model';
import { IAvoirClientDocument } from 'app/shared/model/avoir-client-document.model';
import { ISales } from 'app/shared/model/sales.model';
import { IDiffere, IReglementDiffere } from 'app/features/differes/data-access/models/differe.model';
import { IAlerteSante, ICustomerSynthese, IDossierSante, IProduitDelivre, IRelanceDiffere, ISituationCredit } from './customer-fiche.model';

type EntityResponseType = HttpResponse<ICustomer>;
type EntityArrayResponseType = HttpResponse<ICustomer[]>;

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly resourceUrl = SERVER_API_URL + 'api/customers';

  create(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(this.resourceUrl + '/assured', customer, { observe: 'response' });
  }

  update(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(this.resourceUrl + '/assured', customer, { observe: 'response' });
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http.get<ICustomer>(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(this.resourceUrl, { params: options, observe: 'response' });
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteAssuredCustomer(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assured/${id}`, { observe: 'response' });
  }

  deleteTiersPayant(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/assured/tiers-payants/${id}`, { observe: 'response' });
  }

  /** Désactive (`DISABLE`) ou réactive (`ENABLE`) un client. */
  changeStatus(id: number, status: 'ENABLE' | 'DISABLE'): Observable<HttpResponse<{}>> {
    return this.http.put(`${this.resourceUrl}/${id}/status`, null, { params: { status }, observe: 'response' });
  }

  createUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(`${this.resourceUrl}/uninsured`, customer, { observe: 'response' });
  }

  updateUninsuredCustomer(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(`${this.resourceUrl}/uninsured`, customer, { observe: 'response' });
  }

  queryUninsuredCustomers(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/uninsured`, { params: options, observe: 'response' });
  }

  createAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.post<ICustomer>(this.resourceUrl + '/ayant-droit', customer, { observe: 'response' });
  }

  updateAyantDroit(customer: ICustomer): Observable<EntityResponseType> {
    return this.http.put<ICustomer>(this.resourceUrl + '/ayant-droit', customer, { observe: 'response' });
  }

  uploadJsonData(file: any): Observable<HttpResponse<void>> {
    return this.http.post<void>(`${this.resourceUrl}/importjson`, file, {
      observe: 'response',
      headers: new HttpHeaders({ timeout: `7600000` }),
    });
  }

  /** Achats clôturés, du plus récent au plus ancien ; paginé (`X-Total-Count`). */
  purchases(req?: any): Observable<HttpResponse<ISales[]>> {
    const options = createRequestOption(req);
    return this.http.get<ISales[]>(`${this.resourceUrl}/purchases`, { params: options, observe: 'response' });
  }

  queryAssuredCustomer(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/assured`, { params: options, observe: 'response' });
  }

  addTiersPayant(clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<ICustomer>> {
    return this.http.post<ICustomer>(`${this.resourceUrl}/assured/tiers-payants`, clientTiersPayant, { observe: 'response' });
  }

  updateTiersPayant(clientTiersPayant: IClientTiersPayant): Observable<HttpResponse<ICustomer>> {
    return this.http.put<ICustomer>(`${this.resourceUrl}/assured/tiers-payants`, clientTiersPayant, { observe: 'response' });
  }

  queryVente(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/ventes`, { params: options, observe: 'response' });
  }

  fetchCustomersTiersPayant(id: number): Observable<HttpResponse<IClientTiersPayant[]>> {
    return this.http.get<IClientTiersPayant[]>(`${this.resourceUrl}/tiers-payants/${id}`, { observe: 'response' });
  }

  queryAyantDroits(id: number): Observable<EntityArrayResponseType> {
    return this.http.get<ICustomer[]>(`${this.resourceUrl}/ayant-droits/${id}`, { observe: 'response' });
  }

  // ── Fiche client 360° : lectures bornées au client ──

  synthese(customerId: number): Observable<ICustomerSynthese> {
    return this.http.get<ICustomerSynthese>(`${this.resourceUrl}/${customerId}/synthese`);
  }

  /** Produits délivrés sur la période ; paginé (`X-Total-Count`). */
  produitsDelivres(customerId: number, req: any): Observable<HttpResponse<IProduitDelivre[]>> {
    const options = createRequestOption(req);
    return this.http.get<IProduitDelivre[]>(`${this.resourceUrl}/${customerId}/produits-delivres`, { params: options, observe: 'response' });
  }

  /** Différés non soldés ; corps vide (204) si le client n'en a pas. */
  differes(customerId: number): Observable<IDiffere | null> {
    return this.http.get<IDiffere>(`${this.resourceUrl}/${customerId}/differes`);
  }

  reglementsDifferes(customerId: number, req: any): Observable<HttpResponse<IReglementDiffere[]>> {
    const options = createRequestOption(req);
    return this.http.get<IReglementDiffere[]>(`${this.resourceUrl}/${customerId}/reglements-differes`, {
      params: options,
      observe: 'response',
    });
  }

  // ── Sécurité du patient ──

  dossierSante(customerId: number): Observable<IDossierSante> {
    return this.http.get<IDossierSante>(`${this.resourceUrl}/${customerId}/dossier-sante`);
  }

  updateDossierSante(customerId: number, dossier: IDossierSante): Observable<IDossierSante> {
    return this.http.put<IDossierSante>(`${this.resourceUrl}/${customerId}/dossier-sante`, dossier);
  }

  alertesSante(customerId: number, produitId: number): Observable<IAlerteSante[]> {
    return this.http.get<IAlerteSante[]>(`${this.resourceUrl}/${customerId}/alertes-sante`, { params: { produitId } });
  }

  /** Délivrance malgré une alerte : sans le droit, `actionAuthorityKey` porte la clé d'un collègue qui le détient. */
  derogerAlerteSante(customerId: number, derogation: { produitId: number; motif: string; actionAuthorityKey?: string }): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/${customerId}/alertes-sante/derogations`, derogation);
  }

  // ── Crédit ──

  situationCredit(customerId: number): Observable<ISituationCredit> {
    return this.http.get<ISituationCredit>(`${this.resourceUrl}/${customerId}/limite-credit`);
  }

  /** Vente à crédit au-delà de la limite, pour une vente donnée ; tracée côté serveur. */
  derogerLimiteCredit(
    customerId: number,
    derogation: { saleId: number; saleDate: string; montant: number; motif: string; actionAuthorityKey?: string },
  ): Observable<void> {
    return this.http.post<void>(`${this.resourceUrl}/${customerId}/limite-credit/derogations`, derogation);
  }

  relancesDifferes(customerId: number): Observable<IRelanceDiffere[]> {
    return this.http.get<IRelanceDiffere[]>(`${this.resourceUrl}/${customerId}/relances-differes`);
  }

  relancerDifferes(customerId: number): Observable<IRelanceDiffere> {
    return this.http.post<IRelanceDiffere>(`${this.resourceUrl}/${customerId}/relances-differes`, null);
  }

  avoirsByCustomer(customerId: number): Observable<IAvoirClientDocument[]> {
    return this.http.get<IAvoirClientDocument[]>(`${SERVER_API_URL}api/sales/avoirs/documents/by-customer/${customerId}`);
  }
}
