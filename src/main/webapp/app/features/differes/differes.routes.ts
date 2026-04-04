import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../config/authority.constants';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/differes-layout/differes-layout.component').then(m => m.DifferesLayoutComponent),
    data: { authorities: [Authority.USER], pageTitle: 'facturation.differes' },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'reglements',
    redirectTo: '',
    pathMatch: 'full',
  },
];

export default routes;
