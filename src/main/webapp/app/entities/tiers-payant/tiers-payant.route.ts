import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TiersPayantComponent } from './tiers-payant.component';

export const tiersPayantRoute: Routes = [
  {
    path: '',
    component: TiersPayantComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.TIERS_PAYANT],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default tiersPayantRoute;
