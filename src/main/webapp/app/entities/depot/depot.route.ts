import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

const depotRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./depot.component').then(m => m.DepotComponent),
    data: {
      authorities: [Authority.ADMIN,Authority.PR_MENU_DEPOT],
      pageTitle: 'Gestion des Dépôts'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    data: {
      authorities: [Authority.ADMIN,Authority.PR_MENU_DEPOT],
      pageTitle: 'Nouveau Dépôt'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    data: {
      authorities: [Authority.ADMIN,Authority.PR_MENU_DEPOT],
      pageTitle: 'Modifier Dépôt'
    },
    canActivate: [UserRouteAccessService]
  }
];

export default depotRoutes;
