import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, Observable, of } from 'rxjs';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { MenuComponent } from './menu.component';
import { MenuDetailComponent } from './menu-detail.component';
import { MenuUpdateComponent } from './menu-update.component';
import { IAuthority, Privilege } from '../../shared/model/authority.model';
import { PrivillegeService } from './privillege.service';
import { Authority } from '../../config/authority.constants';
import { mergeMap } from 'rxjs/internal/operators/mergeMap';

@Injectable({ providedIn: 'root' })
export class MenuResolve implements Resolve<IAuthority> {
  constructor(private service: PrivillegeService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IAuthority> | Observable<never> {
    const name = route.params['name'];
    if (name) {
      return this.service.find(name).pipe(
        mergeMap((privilege: HttpResponse<Privilege>) => {
          if (privilege.body) {
            return of(privilege.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Privilege());
  }
}

export const menuRoute: Routes = [
  {
    path: '',
    component: MenuComponent,
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.menu.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.menu.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.menu.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.menu.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
