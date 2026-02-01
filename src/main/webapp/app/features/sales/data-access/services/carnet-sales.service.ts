import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { ISales } from '../../../../shared/model/sales.model';
import { ApplicationConfigService } from '../../../../core/config/application-config.service';

/**
 * Service pour les ventes CARNET
 * 
 * Note: Dans l'ancien code, les ventes CARNET utilisaient les mêmes endpoints
 * que les autres types de ventes (/assurance). La distinction se fait uniquement
 * par le champ 'typeVo' dans ISales qui doit être défini à 'CARNET'.
 */
@Injectable({
  providedIn: 'root',
})
export class CarnetSalesService {
  private http = inject(HttpClient);
  private applicationConfigService = inject(ApplicationConfigService);
  private resourceUrl = this.applicationConfigService.getEndpointFor('api/sales');

  /**
   * Créer une vente sur carnet
   * Utilise la même route /assurance que les autres ventes (ancien code VoSalesService.create())
   */
  createCarnetSale(sale: ISales): Observable<HttpResponse<ISales>> {
    return this.http.post<ISales>(`${this.resourceUrl}/assurance`, sale, { observe: 'response' });
  }
}
