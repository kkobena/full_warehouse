import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IPaymentFournisseur, PaymentFournisseur } from 'app/shared/model/payment-fournisseur.model';
import { PaymentFournisseurService } from './payment-fournisseur.service';
import { PaymentFournisseurComponent } from './payment-fournisseur.component';
import { PaymentFournisseurDetailComponent } from './payment-fournisseur-detail.component';
import { PaymentFournisseurUpdateComponent } from './payment-fournisseur-update.component';

@Injectable({ providedIn: 'root' })
export class PaymentFournisseurResolve implements Resolve<IPaymentFournisseur> {
  constructor(private service: PaymentFournisseurService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IPaymentFournisseur> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((paymentFournisseur: HttpResponse<PaymentFournisseur>) => {
          if (paymentFournisseur.body) {
            return of(paymentFournisseur.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new PaymentFournisseur());
  }
}

export const paymentFournisseurRoute: Routes = [
  {
    path: '',
    component: PaymentFournisseurComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.paymentFournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: PaymentFournisseurDetailComponent,
    resolve: {
      paymentFournisseur: PaymentFournisseurResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentFournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: PaymentFournisseurUpdateComponent,
    resolve: {
      paymentFournisseur: PaymentFournisseurResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentFournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: PaymentFournisseurUpdateComponent,
    resolve: {
      paymentFournisseur: PaymentFournisseurResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.paymentFournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
