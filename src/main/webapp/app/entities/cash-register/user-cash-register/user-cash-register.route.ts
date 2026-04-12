import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { CashRegisterService } from '../cash-register.service';
import { CashRegister } from '../model/cash-register.model';

export const CategorieResolve = (route: ActivatedRouteSnapshot): Observable<null | CashRegister> => {
  const id = route.params['id'];
  if (id) {
    return inject(CashRegisterService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<CashRegister>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new CashRegister());
};

const userCahsRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./user-cash-register.component').then(m => m.UserCashRegisterComponent),
  },
  {
    path: ':id/billetage',
    loadComponent: () => import('../ticketing/ticketing.component').then(m => m.TicketingComponent),
    resolve: { cashRegister: CategorieResolve },
    data: { pageTitle: 'Billetage' },
  },
];

export default userCahsRoutes;
