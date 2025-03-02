import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const rayonRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./rayon.component').then(m => m.RayonComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.RAYON],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default rayonRoute;
