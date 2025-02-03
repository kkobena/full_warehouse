import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Facture } from './facture.model';
import { FactureService } from './facture.service';





export const FactureResolve = (route: ActivatedRouteSnapshot): Observable<null | Facture> => {
  const id = route.params['id'];
  if (id) {
    return inject(FactureService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<Facture>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return EMPTY;
};

const factureRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./facturation.component').then(m => m.FacturationComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./edition/edition.component').then(m => m.EditionComponent),

    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./facture-detail/facture-detail.component').then(m => m.FactureDetailComponent),
    resolve: {
      facture: FactureResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: ':id/group-view',
    loadComponent: () => import('./groupe-facture-detail/groupe-facture-detail.component').then(m => m.GroupeFactureDetailComponent),
    resolve: {
      facture: FactureResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default factureRoute;
