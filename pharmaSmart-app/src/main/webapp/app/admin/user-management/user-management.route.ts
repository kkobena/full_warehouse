import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, Routes } from '@angular/router';
import { of } from 'rxjs';

import { IUser } from './user-management.model';
import { UserManagementService } from './service/user-management.service';

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
    loadComponent: () => import('./list/user-management.component'),
    data: {
      defaultSort: 'id,asc',
    },
  },
  {
    path: ':login/view',
    loadComponent: () => import('./detail/user-management-detail.component'),
    resolve: {
      user: UserManagementResolve,
    },
  },
  {
    path: 'new',
    loadComponent: () => import('./update/user-management-update.component'),
    resolve: {
      user: UserManagementResolve,
    },
  },
  {
    path: ':login/edit',
    loadComponent: () => import('./update/user-management-update.component'),
    resolve: {
      user: UserManagementResolve,
    },
  },
];

export default userManagementRoute;
