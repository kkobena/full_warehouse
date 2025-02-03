import { Routes } from '@angular/router';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';


const typeEtiquetteRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./type-etiquette.component').then(m => m.TypeEtiquetteComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.REFERENTIEL],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default typeEtiquetteRoute;
