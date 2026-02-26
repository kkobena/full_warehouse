import {inject} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Router, Routes} from '@angular/router';
import {EMPTY, mergeMap, Observable, of} from 'rxjs';

import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {ISales, Sales} from 'app/shared/model/sales.model';
import {SalesService} from './sales.service';

export const SalesResolve = (route: ActivatedRouteSnapshot): Observable<null | ISales> => {
  const id = route.params['id'];
  const saleDate = route.params['saleDate'];
  if (id) {
    return inject(SalesService)
      .find({id, saleDate})
      .pipe(
        mergeMap((res: HttpResponse<ISales>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Sales());
};
const salesRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'comptant/:isPresale/new',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Vente Comptant',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'comptant/:id/:saleDate/:isPresale/edit',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Vente Comptant',
      mode: 'edit',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default salesRoute;
