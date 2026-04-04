import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../config/authority.constants';

const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/facturation-layout/facturation-layout.component').then(m => m.FacturationLayoutComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
      pageTitle: 'facturation.factures',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'reglements',
    redirectTo: '',
    pathMatch: 'full',
  },
];

export default routes;
