import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';

import { GroupeTiersPayantService } from './groupe-tierspayant.service';
import { GroupeTiersPayant, IGroupeTiersPayant } from '../../shared/model/groupe-tierspayant.model';
import { GroupeTiersPayantComponent } from './groupe-tiers-payant.component';

@Injectable({ providedIn: 'root' })
export class GroupeTiersPayantResolve implements Resolve<IGroupeTiersPayant> {
  constructor(private service: GroupeTiersPayantService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IGroupeTiersPayant> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((groupe: HttpResponse<IGroupeTiersPayant>) => {
          if (groupe.body) {
            return of(groupe.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new GroupeTiersPayant());
  }
}

export const groupeTiersPayantRoute: Routes = [
  {
    path: '',
    component: GroupeTiersPayantComponent,
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.groupeTiersPayant.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
