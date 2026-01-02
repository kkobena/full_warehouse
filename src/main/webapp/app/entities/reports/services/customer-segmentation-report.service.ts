import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { ICustomerSegmentation, CustomerClassification } from 'app/shared/model/report/customer-segmentation.model';

type EntityArrayResponseType = HttpResponse<ICustomerSegmentation[]>;
type EntityResponseType = HttpResponse<ICustomerSegmentation>;
type CountResponseType = HttpResponse<Record<CustomerClassification, number>>;

@Injectable({ providedIn: 'root' })
export class CustomerSegmentationReportService {
  private readonly resourceUrl = SERVER_API_URL + 'api/customers/segmentation';
  private readonly http = inject(HttpClient);

  /**
   * Get all customer segmentation data using RFM analysis
   */
  getAllCustomerSegmentation(): Observable<EntityArrayResponseType> {
    return this.http.get<ICustomerSegmentation[]>(this.resourceUrl, { observe: 'response' });
  }

  /**
   * Get customers filtered by classification
   */
  getCustomersByClassification(classification: CustomerClassification): Observable<EntityArrayResponseType> {
    const params = new HttpParams().set('classification', classification);
    return this.http.get<ICustomerSegmentation[]>(`${this.resourceUrl}/classification`, { params, observe: 'response' });
  }

  /**
   * Get champion customers (highest RFM scores)
   */
  getChampionCustomers(): Observable<EntityArrayResponseType> {
    return this.http.get<ICustomerSegmentation[]>(`${this.resourceUrl}/champions`, { observe: 'response' });
  }

  /**
   * Get at-risk customers (need attention)
   */
  getAtRiskCustomers(): Observable<EntityArrayResponseType> {
    return this.http.get<ICustomerSegmentation[]>(`${this.resourceUrl}/at-risk`, { observe: 'response' });
  }

  /**
   * Get count of customers by classification
   */
  getCustomerCountByClassification(): Observable<CountResponseType> {
    return this.http.get<Record<CustomerClassification, number>>(`${this.resourceUrl}/count`, { observe: 'response' });
  }

  /**
   * Get customer segmentation for a specific customer
   */
  getCustomerSegmentation(customerId: number): Observable<EntityResponseType> {
    return this.http.get<ICustomerSegmentation>(`${this.resourceUrl}/${customerId}`, { observe: 'response' });
  }

  /**
   * Export customer segmentation report as PDF
   */
  exportCustomerSegmentationToPdf(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.resourceUrl}/export`, { observe: 'response', responseType: 'blob' });
  }
}
