import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ITiersPayant, TiersPayant } from '../../shared/model/tierspayant.model';
import { TiersPayantService } from './tierspayant.service';
import { TiersPayantComponent } from './tiers-payant.component';

@Injectable({ providedIn: 'root' })
export class GroupeTiersPayantResolve implements Resolve<ITiersPayant> {
  constructor(private service: TiersPayantService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ITiersPayant> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((groupe: HttpResponse<ITiersPayant>) => {
          if (groupe.body) {
            return of(groupe.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new TiersPayant());
  }
}

export const tiersPayantRoute: Routes = [
  {
    path: '',
    component: TiersPayantComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.TIERS_PAYANT],
      pageTitle: 'warehouseApp.tiersPayant.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
