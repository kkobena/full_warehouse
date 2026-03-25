import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const semoisRoutes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('../../features/commande/feature/semois-dashboard/semois-dashboard.component').then(
        m => m.SemoisDashboardComponent,
      ),
    data: {
      authorities: [Authority.ADMIN, Authority.USER],
      pageTitle: 'Dashboard SEMOIS',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'suggestions',
    loadComponent: () =>
      import('../../features/commande/feature/semois-suggestions/semois-suggestions.component').then(
        m => m.SemoisSuggestionsComponent,
      ),
    data: {
      authorities: [Authority.ADMIN, Authority.USER],
      pageTitle: 'Suggestions SEMOIS',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'config-masse',
    loadComponent: () =>
      import('../../features/commande/feature/semois-config-masse/semois-config-masse.component').then(
        m => m.SemoisConfigMasseComponent,
      ),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Configuration SEMOIS en Masse',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'model-config',
    loadComponent: () =>
      import('../../features/commande/feature/semois-model-config/semois-model-config.component').then(
        m => m.SemoisModelConfigComponent,
      ),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Configuration Modèle Réapprovisionnement',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default semoisRoutes;
