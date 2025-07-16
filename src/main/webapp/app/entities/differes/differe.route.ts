import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Differe } from './model/differe.model';
import { DiffereService } from './differe.service';

export const DiffereResolve = (route: ActivatedRouteSnapshot): Observable<null | Differe> => {
  const id = route.params['id'];
  if (id) {
    return inject(DiffereService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<Differe>) => {
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

const differeRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./gestion-differes.component').then(m => m.GestionDifferesComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_DIFFERE],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/do-reglement-differe',
    loadComponent: () => import('./faire-reglement-differe/faire-reglement-differe.component').then(m => m.FaireReglementDiffereComponent),
    resolve: {
      differe: DiffereResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_DIFFERE],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default differeRoute;
