import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TvaComponent } from './tva.component';

const tvaRoute: Routes = [
  {
    path: '',
    component: TvaComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.TVA],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default tvaRoute;
