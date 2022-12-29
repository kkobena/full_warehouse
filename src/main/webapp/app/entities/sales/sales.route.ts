import {Injectable} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Resolve, Router, Routes} from '@angular/router';
import {EMPTY, Observable, of} from 'rxjs';
import {flatMap} from 'rxjs/operators';

import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {ISales, Sales} from 'app/shared/model/sales.model';
import {SalesService} from './sales.service';
import {SalesComponent} from './sales.component';
import {SalesDetailComponent} from './sales-detail.component';
import {SalesUpdateComponent} from './sales-update.component';
import {PresaleComponent} from './presale/presale.component';
import {VenteEnCoursComponent} from './vente-en-cours/vente-en-cours.component';

@Injectable({providedIn: 'root'})
export class SalesResolve implements Resolve<ISales> {
  constructor(private service: SalesService, private router: Router) {
  }

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
    path: ':isPresale/new',
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
    path: ':id/:isPresale/edit',
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
    path: 'presale',
    component: PresaleComponent,
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
    path: 'ventes-en-cours',
    component: VenteEnCoursComponent,
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
