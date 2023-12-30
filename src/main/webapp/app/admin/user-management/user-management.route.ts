import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Routes } from '@angular/router';
import { of } from 'rxjs';

import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';
import { UserManagementComponent } from './list/user-management.component';
import { UserManagementDetailComponent } from './detail/user-management-detail.component';
import { UserManagementUpdateComponent } from './update/user-management-update.component';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

export const UserManagementResolve: ResolveFn<IUser | null> = (route: ActivatedRouteSnapshot) => {
  const login = route.paramMap.get('login');
  if (login) {
    return inject(UserManagementService).find(login);
  }
  return of(null);
};

const userManagementRoute: Routes = [
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
export default userManagementRoute;
