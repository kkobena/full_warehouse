import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const gestionPerimesRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./gestion-peremption.component').then(m => m.GestionPeremptionComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_PERIMES, Authority.GESTION_STOCK]
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'edit',
    loadComponent: () => import('./ajout-perimes/ajout-perimes.component').then(m => m.AjoutPerimesComponent),

    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_PERIMES, Authority.GESTION_STOCK]
    },
    canActivate: [UserRouteAccessService]
  }
];
export default gestionPerimesRoute;
