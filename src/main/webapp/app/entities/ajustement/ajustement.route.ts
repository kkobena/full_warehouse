import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IAjustement, Ajustement } from 'app/shared/model/ajustement.model';
import { AjustementService } from './ajustement.service';
import { AjustementComponent } from './ajustement.component';
import { AjustementDetailComponent } from './ajustement-detail.component';

@Injectable({ providedIn: 'root' })
export class AjustementResolve implements Resolve<IAjustement> {
  constructor(private service: AjustementService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IAjustement> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((ajustement: HttpResponse<Ajustement>) => {
          if (ajustement.body) {
            return of(ajustement.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Ajustement());
  }
}

export const ajustementRoute: Routes = [
  {
    path: '',
    component: AjustementComponent,
    data: {
      authorities: [Authority.USER],
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.ajustement.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
