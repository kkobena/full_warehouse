import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TableauProduitService } from './tableau-produit.service';
import { TableauProduitComponent } from './tableau-produit.component';
import { ITableau, Tableau } from '../../shared/model/tableau.model';
import { ProduitAssociesComponent } from './produits/produit-associes.component';

export const TableauProduitResolve = (route: ActivatedRouteSnapshot): Observable<null | ITableau> => {
  const id = route.params['id'];
  if (id) {
    return inject(TableauProduitService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<ITableau>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Tableau());
};
const tableauProduitRoute: Routes = [
  {
    path: '',
    component: TableauProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/associe',
    component: ProduitAssociesComponent,
    resolve: {
      tableau: TableauProduitResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default tableauProduitRoute;
