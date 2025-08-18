import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

export const tiersPayantRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./tiers-payant-home.component').then(m => m.TiersPayantHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.TIERS_PAYANT]
    },
    canActivate: [UserRouteAccessService]
  }
];
export default tiersPayantRoute;
