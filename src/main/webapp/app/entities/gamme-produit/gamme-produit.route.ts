import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const gammeProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./gamme-produit.component').then(m => m.GammeProduitComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GAMME_PRODUIT],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default gammeProduitRoute;
