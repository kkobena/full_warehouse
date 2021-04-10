import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IInventoryTransaction, InventoryTransaction } from 'app/shared/model/inventory-transaction.model';
import { InventoryTransactionService } from './inventory-transaction.service';
import { InventoryTransactionComponent } from './inventory-transaction.component';
import { InventoryTransactionDetailComponent } from './inventory-transaction-detail.component';
@Injectable({ providedIn: 'root' })
export class InventoryTransactionResolve implements Resolve<IInventoryTransaction> {
  constructor(private service: InventoryTransactionService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IInventoryTransaction> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((inventoryTransaction: HttpResponse<InventoryTransaction>) => {
          if (inventoryTransaction.body) {
            return of(inventoryTransaction.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new InventoryTransaction());
  }
}

export const inventoryTransactionRoute: Routes = [
  {
    path: '',
    component: InventoryTransactionComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.inventoryTransaction.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: InventoryTransactionDetailComponent,
    resolve: {
      inventoryTransaction: InventoryTransactionResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.inventoryTransaction.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
