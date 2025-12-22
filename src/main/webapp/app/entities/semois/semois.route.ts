import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';


const semoisRoutes: Routes = [
  {
    path: 'suggestions',
    loadComponent: () => import('./semois-suggestions.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.USER],
      pageTitle: 'Suggestions SEMOIS',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'config-masse',
    loadComponent: () => import('./semois-config-masse.component'),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Configuration SEMOIS en Masse',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'model-config',
    loadComponent: () => import('./semois-model-config.component'),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Configuration Modèle Réapprovisionnement',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default semoisRoutes;
