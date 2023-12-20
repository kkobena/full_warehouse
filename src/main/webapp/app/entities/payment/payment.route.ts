import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IPayment, Payment } from 'app/shared/model/payment.model';
import { PaymentService } from './payment.service';
import { PaymentComponent } from './payment.component';
import { PaymentDetailComponent } from './payment-detail.component';

@Injectable({ providedIn: 'root' })
export class PaymentResolve implements Resolve<IPayment> {
  constructor(private service: PaymentService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IPayment> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((payment: HttpResponse<Payment>) => {
          if (payment.body) {
            return of(payment.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Payment());
  }
}

export const paymentRoute: Routes = [
  {
    path: '',
    component: PaymentComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.PAYMENT],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.payment.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: PaymentDetailComponent,
    resolve: {
      payment: PaymentResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.payment.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
