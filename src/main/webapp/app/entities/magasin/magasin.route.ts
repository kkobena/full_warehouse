import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IMagasin, Magasin } from 'app/shared/model/magasin.model';
import { MagasinService } from './magasin.service';

export const MagasinResolve = (route: ActivatedRouteSnapshot): Observable<null | IMagasin> => {
  const id = route.params['id'];
  if (id) {
    return inject(MagasinService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IMagasin>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Magasin());
};
const magasinRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./magasin.component').then(m => m.MagasinComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MAGASIN],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./magasin-detail.component').then(m => m.MagasinDetailComponent),
    resolve: {
      magasin: MagasinResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MAGASIN],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./magasin-update.component').then(m => m.MagasinUpdateComponent),
    resolve: {
      magasin: MagasinResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MAGASIN],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./magasin-update.component').then(m => m.MagasinUpdateComponent),
    resolve: {
      magasin: MagasinResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MAGASIN],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default magasinRoute;
