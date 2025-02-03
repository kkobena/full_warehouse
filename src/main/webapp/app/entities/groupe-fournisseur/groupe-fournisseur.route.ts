import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';


const groupeFournisseurRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./groupe-fournisseur.component').then(m => m.GroupeFournisseurComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GROUPE_FOURNISSEUR],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default groupeFournisseurRoute;
