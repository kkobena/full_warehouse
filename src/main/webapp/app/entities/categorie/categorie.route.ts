import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Categorie, ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';

export const CategorieResolve = (route: ActivatedRouteSnapshot): Observable<null | ICategorie> => {
  const id = route.params['id'];
  if (id) {
    return inject(CategorieService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<ICategorie>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Categorie());
};
const categorieRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./categorie.component').then(m => m.CategorieComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./categorie-detail.component').then(m => m.CategorieDetailComponent),
    resolve: {
      categorie: CategorieResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('./categorie-update.component').then(m => m.CategorieUpdateComponent),
    resolve: {
      categorie: CategorieResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./categorie-update.component').then(m => m.CategorieUpdateComponent),
    resolve: {
      categorie: CategorieResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default categorieRoute;
