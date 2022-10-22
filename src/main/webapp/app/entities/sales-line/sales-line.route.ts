import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { ISalesLine, SalesLine } from 'app/shared/model/sales-line.model';
import { SalesLineService } from './sales-line.service';
import { SalesLineComponent } from './sales-line.component';
import { SalesLineDetailComponent } from './sales-line-detail.component';
import { SalesLineUpdateComponent } from './sales-line-update.component';

@Injectable({ providedIn: 'root' })
export class SalesLineResolve implements Resolve<ISalesLine> {
  constructor(private service: SalesLineService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ISalesLine> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((salesLine: HttpResponse<SalesLine>) => {
          if (salesLine.body) {
            return of(salesLine.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new SalesLine());
  }
}

export const salesLineRoute: Routes = [
  {
    path: '',
    component: SalesLineComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.salesLine.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: SalesLineDetailComponent,
    resolve: {
      salesLine: SalesLineResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.salesLine.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: SalesLineUpdateComponent,
    resolve: {
      salesLine: SalesLineResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.salesLine.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: SalesLineUpdateComponent,
    resolve: {
      salesLine: SalesLineResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.salesLine.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
