import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { ISales } from '../../../../shared/model/sales.model';
import { ApplicationConfigService } from '../../../../core/config/application-config.service';

/**
 * Service spécifique pour les ventes ASSURANCE
 * 
 * Gère les spécificités des ventes avec tiers-payant:
 * - Validation des cartes d'assurance
 * - Calcul part assuré / part assurance
 * - Gestion des plafonds
 * - Validation prescriptions
 */
@Injectable({
  providedIn: 'root',
})
export class AssuranceSalesService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);
  private resourceUrl = this.applicationConfigService.getEndpointFor('api/sales');

  /**
   * Créer une vente assurance
   */
  createAssuranceSale(sale: ISales): Observable<HttpResponse<ISales>> {
    return this.http.post<ISales>(`${this.resourceUrl}/assurance`, sale, { observe: 'response' });
  }

  /**
   * Mettre à jour les informations client/assurance
   */
  updateCustomerInformation(sale: ISales): Observable<HttpResponse<any>> {
    return this.http.put<any>(`${this.resourceUrl}/assurance/update-customer-information`, sale, { observe: 'response' });
  }
}
