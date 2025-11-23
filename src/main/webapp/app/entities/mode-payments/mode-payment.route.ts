import { Routes } from '@angular/router';

import { Authority } from '../../shared/constants/authority.constants';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';

const modePaymentRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./mode-payment.component').then(m => m.ModePaymentComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL],
    },
    canActivate: [UserRouteAccessService],
  },
];

export default modePaymentRoute;
