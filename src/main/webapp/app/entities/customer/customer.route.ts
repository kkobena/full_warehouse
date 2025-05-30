import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Customer, ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from './customer.service';

export const CustomerResolve = (route: ActivatedRouteSnapshot): Observable<null | ICustomer> => {
  const id = route.params['id'];
  if (id) {
    return inject(CustomerService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<ICustomer>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Customer());
};
const customerRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./customer.component').then(m => m.CustomerComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.CLIENT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./customer-detail.component').then(m => m.CustomerDetailComponent),
    resolve: {
      customer: CustomerResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CLIENT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./customer-update.component').then(m => m.CustomerUpdateComponent),
    resolve: {
      customer: CustomerResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CLIENT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./customer-update.component').then(m => m.CustomerUpdateComponent),
    resolve: {
      customer: CustomerResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CLIENT],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default customerRoute;
