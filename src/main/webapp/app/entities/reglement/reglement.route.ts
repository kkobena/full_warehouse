import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ReglementService } from './reglement.service';

import { Reglement } from './model/reglement.model';
import { doReglementResolver } from './do-reglement.resolver';

export const ReglementResolve = (route: ActivatedRouteSnapshot): Observable<null | Reglement> => {
  const id = route.params['id'];
  if (id) {
    return inject(ReglementService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<Reglement>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        })
      );
  }
  return EMPTY;
};
const reglementRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./reglement.component').then(m => m.ReglementComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_REGLEMENT_FACTURE],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    loadComponent: () => import('./faire-reglement/faire-reglement.component').then(m => m.FaireReglementComponent),

    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_REGLEMENT_FACTURE]
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/:typeFacture/faire-reglement',
    loadComponent: () => import('./reglement.component').then(m => m.ReglementComponent),
    resolve: {
      factureDossiers: doReglementResolver
    },
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_REGLEMENT_FACTURE]
    },
    canActivate: [UserRouteAccessService]
  },

  {
    path: ':id/view',
    loadComponent: () => import('./reglement-detail/reglement-detail.component').then(m => m.ReglementDetailComponent),
    resolve: {
      reglement: ReglementResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_REGLEMENT_FACTURE]
    },
    canActivate: [UserRouteAccessService]
  }
];
export default reglementRoute;
