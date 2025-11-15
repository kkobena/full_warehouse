import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

const depotRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('../depot/depot-home/depot-home.component').then(m => m.DepotHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Gestion des Dépôts'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Dépôt'
    },
    canActivate: [UserRouteAccessService]
  },

  {
    path: 'new-vente',
    loadComponent: () => import('./vente-depot/vente-depot.component').then(m => m.VenteDepotComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Dépôt'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/:saleDate/edit-vente',
    loadComponent: () => import('./vente-depot/vente-depot.component').then(m => m.VenteDepotComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Dépôt'
    },
    canActivate: [UserRouteAccessService]
  },


  {
    path: ':id/edit',
    loadComponent: () => import('./depot-form.component').then(m => m.DepotFormComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Modifier Dépôt'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new-return',
    loadComponent: () => import('./depot-returns/depot-returns.component').then(m => m.DepotReturnsComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MENU_DEPOT,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Retour Dépôt'
    },
    canActivate: [UserRouteAccessService]
  }
];

export default depotRoutes;
