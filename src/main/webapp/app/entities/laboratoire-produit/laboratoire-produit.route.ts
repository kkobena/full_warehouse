import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const laboratoireProduitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./laboratoire-produit.component').then(m => m.LaboratoireProduitComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.LABORATOIRE],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  }
];
export default laboratoireProduitRoute;
