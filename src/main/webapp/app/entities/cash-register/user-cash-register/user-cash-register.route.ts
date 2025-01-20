import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { CashRegisterService } from '../cash-register.service';
import { CashRegister } from '../model/cash-register.model';
import { UserCashRegisterComponent } from './user-cash-register.component';
import { TicketingComponent } from '../ticketing/ticketing.component';

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
    component: UserCashRegisterComponent,
    /*    data: {
         authorities: [Authority.USER],
       }, */
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/billetage',
    component: TicketingComponent,
    resolve: {
      cashRegister: CategorieResolve,
    },
    data: {
      /*  authorities: [Authority.USER], */
      pageTitle: 'Billetage',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default userCahsRoutes;
