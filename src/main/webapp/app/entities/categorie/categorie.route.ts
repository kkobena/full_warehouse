import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Categorie, ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';
import { CategorieComponent } from './categorie.component';
import { CategorieDetailComponent } from './categorie-detail.component';
import { CategorieUpdateComponent } from './categorie-update.component';

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
    component: CategorieComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: CategorieDetailComponent,
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
    component: CategorieUpdateComponent,
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
    component: CategorieUpdateComponent,
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
