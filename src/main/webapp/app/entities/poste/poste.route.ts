import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const posteRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./poste.component').then(m => m.PosteComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.PR_MENU_POSTE],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  }
];
export default posteRoute;
