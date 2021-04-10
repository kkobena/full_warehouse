import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IPaymentMode, PaymentMode } from 'app/shared/model/payment-mode.model';
import { PaymentModeService } from './payment-mode.service';
import { PaymentModeComponent } from './payment-mode.component';
import { PaymentModeDetailComponent } from './payment-mode-detail.component';
import { PaymentModeUpdateComponent } from './payment-mode-update.component';

@Injectable({ providedIn: 'root' })
export class PaymentModeResolve implements Resolve<IPaymentMode> {
  constructor(private service: PaymentModeService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IPaymentMode> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((paymentMode: HttpResponse<PaymentMode>) => {
          if (paymentMode.body) {
            return of(paymentMode.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new PaymentMode());
  }
}

export const paymentModeRoute: Routes = [
  {
    path: '',
    component: PaymentModeComponent,
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentMode.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: PaymentModeDetailComponent,
    resolve: {
      paymentMode: PaymentModeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentMode.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: PaymentModeUpdateComponent,
    resolve: {
      paymentMode: PaymentModeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentMode.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: PaymentModeUpdateComponent,
    resolve: {
      paymentMode: PaymentModeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentMode.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
