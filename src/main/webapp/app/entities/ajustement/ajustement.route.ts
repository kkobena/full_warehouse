import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { AjustementService } from './ajustement.service';


import { Ajust, IAjust } from '../../shared/model/ajust.model';

export const AjustementResolve = (route: ActivatedRouteSnapshot): Observable<null | IAjust> => {
  const id = route.params['id'];
  if (id) {
    return inject(AjustementService)
      .find(id)
      .pipe(
        mergeMap((ajustement: HttpResponse<IAjust>) => {
          if (ajustement.body) {
            return of(ajustement.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Ajust());
};
const ajustementRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./ajustement.component').then(m => m.AjustementComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./ajustement-detail.component').then(m => m.AjustementDetailComponent),
    resolve: {
      ajustement: AjustementResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./ajustement-detail.component').then(m => m.AjustementDetailComponent),
    resolve: {
      ajustement: AjustementResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default ajustementRoute;
