import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { TableauProduitService } from './tableau-produit.service';
import { ITableau, Tableau } from '../../shared/model/tableau.model';

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
    loadComponent: () => import('./tableau-produit.component').then(m => m.TableauProduitComponent),
    data: { defaultSort: 'id,asc' },
  },
  {
    path: ':id/associe',
    loadComponent: () => import('./produits/produit-associes.component').then(m => m.ProduitAssociesComponent),
    resolve: { tableau: TableauProduitResolve },
  },
];

export default tableauProduitRoute;
