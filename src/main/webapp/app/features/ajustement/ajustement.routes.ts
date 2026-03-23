import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

export const AJUSTEMENT_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/ajustement-home/ajustement-home.component').then(
        m => m.AjustementHomeComponent,
      ),
    canActivate: [UserRouteAccessService],
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Ajustements de stock',
    },
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./feature/ajustement-form/ajustement-form.component').then(
        m => m.AjustementFormComponent,
      ),
    canActivate: [UserRouteAccessService],
    data: {
      authorities: [Authority.ADMIN, Authority.AJUSTEMENT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouvel ajustement',
    },
  },
];

export default AJUSTEMENT_ROUTES;
