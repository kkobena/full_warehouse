import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';

export const SalesResolve = (route: ActivatedRouteSnapshot): Observable<null | ISales> => {
  const id = route.params['id'];
  const saleDate = route.params['saleDate'];
  if (id) {
    return inject(SalesService)
      .find({ id, saleDate })
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
    loadComponent: () => import('./sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/:saleDate/view',
    loadComponent: () => import('./sales-detail.component').then(m => m.SalesDetailComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':isPresale/new',
    loadComponent: () => import('./selling-home/selling-home.component').then(m => m.SellingHomeComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/:saleDate/:isPresale/edit',
    loadComponent: () => import('./selling-home/selling-home.component').then(m => m.SellingHomeComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      mode: 'edit',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'presale',
    loadComponent: () => import('./presale/presale.component').then(m => m.PresaleComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      mode: 'presale',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'ventes-en-cours',
    loadComponent: () => import('./vente-en-cours/vente-en-cours.component').then(m => m.VenteEnCoursComponent),
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
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
