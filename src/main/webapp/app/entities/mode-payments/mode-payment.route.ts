import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from '../../shared/constants/authority.constants';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { IPaymentMode, PaymentMode } from '../../shared/model/payment-mode.model';
import { ModePaymentService } from './mode-payment.service';

export const ModePaymentResolve = (route: ActivatedRouteSnapshot): Observable<null | IPaymentMode> => {
  const code = route.params['code'];
  if (code) {
    return inject(ModePaymentService)
      .query()
      .pipe(
        mergeMap((res: HttpResponse<IPaymentMode[]>) => {
          const paymentMode = res.body?.find(pm => pm.code === code);
          if (paymentMode) {
            return of(paymentMode);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new PaymentMode());
};

const modePaymentRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./mode-payment.component').then(m => m.ModePaymentComponent),
    data: {
      authorities: [Authority.ADMIN,Authority.REFERENTIEL],
    },
    canActivate: [UserRouteAccessService],
  }
];

export default modePaymentRoute;
