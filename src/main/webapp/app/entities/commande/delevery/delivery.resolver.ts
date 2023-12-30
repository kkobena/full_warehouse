import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Delivery, IDelivery } from '../../../shared/model/delevery.model';
import { DeliveryService } from './delivery.service';
import { HttpResponse } from '@angular/common/http';

const DeliveryResolver = (route: ActivatedRouteSnapshot): Observable<null | IDelivery> => {
  const id = route.params['id'];
  if (id) {
    return inject(DeliveryService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IDelivery>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Delivery());
};

export default DeliveryResolver;
