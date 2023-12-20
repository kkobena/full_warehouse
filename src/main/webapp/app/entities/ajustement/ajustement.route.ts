import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { AjustementService } from './ajustement.service';
import { AjustementComponent } from './ajustement.component';
import { AjustementDetailComponent } from './ajustement-detail.component';
import { Ajust, IAjust } from '../../shared/model/ajust.model';

@Injectable({ providedIn: 'root' })
export class AjustementResolve implements Resolve<IAjust> {
  constructor(private service: AjustementService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IAjust> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((ajustement: HttpResponse<IAjust>) => {
          if (ajustement.body) {
            return of(ajustement.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Ajust());
  }
}

export const ajustementRoute: Routes = [
  {
    path: '',
    component: AjustementComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
      pageTitle: 'warehouseApp.ajustement.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: AjustementDetailComponent,
    resolve: {
      ajustement: AjustementResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
      pageTitle: 'warehouseApp.ajustement.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: AjustementDetailComponent,
    resolve: {
      ajustement: AjustementResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT],
      pageTitle: 'warehouseApp.ajustement.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
