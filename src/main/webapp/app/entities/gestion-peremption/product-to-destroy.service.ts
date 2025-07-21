import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { SERVER_API_URL } from '../../app.constants';
import { Observable } from 'rxjs';
import { createRequestOptions } from '../../shared/util/request-util';
import {
  ProductsToDestroyPayload,
  ProductToDestroy,
  ProductToDestroyFilter,
  ProductToDestroyPayload,
  ProductToDestroySum,
} from './model/product-to-destroy';
import { Keys } from '../../shared/model/keys.model';

type EntityArrayResponseType = HttpResponse<ProductToDestroy[]>;

@Injectable({
  providedIn: 'root',
})
export class ProductToDestroyService {
  private readonly http = inject(HttpClient);

  private readonly resourceUrl = SERVER_API_URL + 'api/product-to-destroy';

  addProductQuantity(payload: ProductsToDestroyPayload): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl, payload, { observe: 'response' });
  }

  destroy(payload: Keys): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/destroy', payload, { observe: 'response' });
  }

  delete(payload: Keys): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/delete', payload, { observe: 'response' });
  }

  getSum(req?: ProductToDestroyFilter): Observable<HttpResponse<ProductToDestroySum>> {
    const options = createRequestOptions(req);
    return this.http.get<ProductToDestroySum>(this.resourceUrl + '/sum', {
      params: options,
      observe: 'response',
    });
  }

  query(req?: ProductToDestroyFilter): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ProductToDestroy[]>(this.resourceUrl, {
      params: options,
      observe: 'response',
    });
  }

  queryForEdit(req?: ProductToDestroyFilter): Observable<EntityArrayResponseType> {
    const options = createRequestOptions(req);
    return this.http.get<ProductToDestroy[]>(this.resourceUrl + '/editing', {
      params: options,
      observe: 'response',
    });
  }

  addItem(payload: ProductToDestroyPayload): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/add-product', payload, { observe: 'response' });
  }

  modifyProductQuantity(payload: ProductToDestroyPayload): Observable<HttpResponse<{}>> {
    return this.http.post(this.resourceUrl + '/modify-product', payload, { observe: 'response' });
  }

  closeCurrent(): Observable<HttpResponse<{}>> {
    return this.http.get(this.resourceUrl + '/close', { observe: 'response' });
  }

  export(format: string, req?: ProductToDestroyFilter): Observable<HttpResponse<Blob>> {
    const options = createRequestOptions(req);
    return this.http.get(`${this.resourceUrl}/export/${format}`, {
      params: options,
      observe: 'response',
      responseType: 'blob',
    });
  }
}
