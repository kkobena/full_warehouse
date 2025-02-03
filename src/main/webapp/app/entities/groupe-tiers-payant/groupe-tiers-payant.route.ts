import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';


const groupeTiersPayantRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./groupe-tiers-payant.component').then(m => m.GroupeTiersPayantComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GROUPE_TIERS_PAYANT],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default groupeTiersPayantRoute;
