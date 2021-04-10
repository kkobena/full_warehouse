import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IMenu, Menu } from 'app/shared/model/menu.model';
import { MenuService } from './menu.service';
import { MenuComponent } from './menu.component';
import { MenuDetailComponent } from './menu-detail.component';
import { MenuUpdateComponent } from './menu-update.component';

@Injectable({ providedIn: 'root' })
export class MenuResolve implements Resolve<IMenu> {
  constructor(private service: MenuService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IMenu> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((menu: HttpResponse<Menu>) => {
          if (menu.body) {
            return of(menu.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Menu());
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
    path: ':id/view',
    component: MenuDetailComponent,
    resolve: {
      menu: MenuResolve,
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
      menu: MenuResolve,
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
      menu: MenuResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.menu.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
