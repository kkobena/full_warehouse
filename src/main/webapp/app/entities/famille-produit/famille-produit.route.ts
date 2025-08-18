import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const familleProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./famille-produit.component').then(m => m.FamilleProduitComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.FAMILLE_PRODUIT],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  }
];
export default familleProduitRoute;
