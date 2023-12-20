import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve, Routes } from '@angular/router';
import { Observable, of } from 'rxjs';

import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';
import { UserManagementComponent } from './list/user-management.component';
import { UserManagementDetailComponent } from './detail/user-management-detail.component';
import { UserManagementUpdateComponent } from './update/user-management-update.component';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

@Injectable({ providedIn: 'root' })
export class UserManagementResolve implements Resolve<IUser | null> {
  constructor(private service: UserManagementService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IUser | null> {
    const id = route.params['login'];
    if (id) {
      return this.service.find(id);
    }
    return of(null);
  }
}

export const userManagementRoute: Routes = [
  {
    path: '',
    component: UserManagementComponent,
    data: {
      defaultSort: 'id,asc',
    },
  },
  {
    path: ':login/view',
    component: UserManagementDetailComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.ADMINISTRATION],
    },
    resolve: {
      user: UserManagementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: UserManagementUpdateComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.ADMINISTRATION],
    },
    resolve: {
      user: UserManagementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':login/edit',
    component: UserManagementUpdateComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.ADMINISTRATION],
    },
    resolve: {
      user: UserManagementResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];
