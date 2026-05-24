import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';

export const SalesResolve = (route: ActivatedRouteSnapshot): Observable<null | ISales> => {
  const id = route.params['id'];
  const saleDate = route.params['saleDate'];
  if (id) {
    return inject(SalesService)
      .find({id, saleDate})
      .pipe(
        mergeMap((res: HttpResponse<ISales>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Sales());
};

const salesRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    data: {
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.sales.home.title',
    },
  },
  {
    path: 'comptant/:isPresale/new',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    resolve: { sales: SalesResolve },
    data: { pageTitle: 'Vente Comptant' },
  },
  {
    path: 'comptant/:id/:saleDate/:isPresale/edit',
    loadComponent: () => import('./comptant-home/comptant-home.component').then(m => m.ComptantHomeComponent),
    resolve: { sales: SalesResolve },
    data: {
      pageTitle: 'Vente Comptant',
      mode: 'edit',
    },
  },
];

export default salesRoute;
