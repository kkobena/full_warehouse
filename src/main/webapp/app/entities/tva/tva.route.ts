import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';


const tvaRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./tva.component').then(m => m.TvaComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.TVA],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default tvaRoute;
