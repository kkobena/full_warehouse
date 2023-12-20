import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { JournalService } from './journal.service';
import { JournalComponent } from './journal.component';
import { SalesUpdateComponent } from '../sales/sales-update.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<ISales> {
  constructor(private service: JournalService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ISales> | Observable<never> {
    return of(new Sales());
  }
}

export const salesRoute: Routes = [
  {
    path: '',
    component: JournalComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.SALES],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.menu.entities.salesJournal',
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'new',
    component: SalesUpdateComponent,
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: SalesUpdateComponent,
    resolve: {
      sales: SalesResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
