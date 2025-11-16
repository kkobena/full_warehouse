import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const motifAjustementRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./modif-ajustement.component').then(m => m.ModifAjustementComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL, Authority.MOTIF_AJUSTEMENT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default motifAjustementRoute;
