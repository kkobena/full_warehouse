import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Authority } from 'app/shared/constants/authority.constants';

const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./nav-manager.component').then(m => m.NavManagerComponent),
    data: { authorities: [Authority.ADMIN] },
    canActivate: [UserRouteAccessService],
  },
];

export default routes;

