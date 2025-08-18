import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const fournisseurRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./fournisseur-home.component').then(m => m.FournisseurHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.FOURNISSEUR],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  }
];
export default fournisseurRoute;
