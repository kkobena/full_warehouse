import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Categorie, ICategorie } from 'app/shared/model/categorie.model';
import { CategorieService } from './categorie.service';
import { CategorieComponent } from './categorie.component';
import { CategorieDetailComponent } from './categorie-detail.component';
import { CategorieUpdateComponent } from './categorie-update.component';

@Injectable({ providedIn: 'root' })
export class CategorieResolve implements Resolve<ICategorie> {
  constructor(private service: CategorieService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ICategorie> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((categorie: HttpResponse<Categorie>) => {
          if (categorie.body) {
            return of(categorie.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Categorie());
  }
}

export const categorieRoute: Routes = [
  {
    path: '',
    component: CategorieComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.CATEGORIE],
      pageTitle: 'warehouseApp.categorie.home.title',
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
      pageTitle: 'warehouseApp.categorie.home.title',
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
      pageTitle: 'warehouseApp.categorie.home.title',
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
      pageTitle: 'warehouseApp.categorie.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
