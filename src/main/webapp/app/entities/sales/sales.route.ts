import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { SalesDetailComponent } from './sales-detail.component';
import { PresaleComponent } from './presale/presale.component';
import { VenteEnCoursComponent } from './vente-en-cours/vente-en-cours.component';
import { SalesHomeComponent } from './sales-home/sales-home.component';
import { SellingHomeComponent } from './selling-home/selling-home.component';

export const SalesResolve = (route: ActivatedRouteSnapshot): Observable<null | ISales> => {
  const id = route.params['id'];
  if (id) {
    return inject(SalesService)
      .find(id)
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
    component: SalesHomeComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: SalesDetailComponent,
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
    component: SellingHomeComponent,
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/:isPresale/edit',
    component: SellingHomeComponent,
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
    component: PresaleComponent,
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
    component: VenteEnCoursComponent,
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default salesRoute;
