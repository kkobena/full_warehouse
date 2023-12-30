import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { GroupeTiersPayantComponent } from './groupe-tiers-payant.component';

const groupeTiersPayantRoute: Routes = [
  {
    path: '',
    component: GroupeTiersPayantComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.GROUPE_TIERS_PAYANT],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default groupeTiersPayantRoute;
