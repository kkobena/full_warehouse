import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { ISales, Sales } from 'app/shared/model/sales.model';
import { SalesService } from './sales.service';
import { SalesComponent } from './sales.component';
import { SalesDetailComponent } from './sales-detail.component';
import { SalesUpdateComponent } from './sales-update.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<ISales> {
  constructor(private service: SalesService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ISales> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<Sales>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Sales());
  }
}

export const salesRoute: Routes = [
  {
    path: '',
    component: SalesComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.sales.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: SalesDetailComponent,
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
