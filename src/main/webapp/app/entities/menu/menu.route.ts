import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { MenuComponent } from './menu.component';
import { MenuDetailComponent } from './menu-detail.component';
import { MenuUpdateComponent } from './menu-update.component';
import { IAuthority, Privilege } from '../../shared/model/authority.model';
import { PrivillegeService } from './privillege.service';

import { mergeMap } from 'rxjs/internal/operators/mergeMap';
import { Authority } from '../../shared/constants/authority.constants';

export const MenuResolve = (route: ActivatedRouteSnapshot): Observable<null | IAuthority> => {
  const name = route.params['name'];
  if (name) {
    return inject(PrivillegeService)
      .find(name)
      .pipe(
        mergeMap((res: HttpResponse<IAuthority>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Privilege());
};
const menuRoute: Routes = [
  {
    path: '',
    component: MenuComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.MENU],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':name/view',
    component: MenuDetailComponent,
    resolve: {
      privilege: MenuResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MENU],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: MenuUpdateComponent,
    resolve: {
      privilege: MenuResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MENU],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: MenuUpdateComponent,
    resolve: {
      privilege: MenuResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.MENU],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default menuRoute;
