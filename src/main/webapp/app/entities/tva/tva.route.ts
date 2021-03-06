import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { ITva, Tva } from '../../shared/model/tva.model';
import { TvaService } from './tva.service';
import { TvaComponent } from './tva.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<ITva> {
  constructor(private service: TvaService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ITva> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<Tva>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Tva());
  }
}

export const tvaRoute: Routes = [
  {
    path: '',
    component: TvaComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.tva.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
