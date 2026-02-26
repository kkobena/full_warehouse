import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

/**
 * Sales feature routes
 *
 * Routes:
 * - /sales-home              → Nouvelle vente (tab COMPTANT par defaut)
 * - /sales-home/edit/:id     → Editer une vente existante
 *   Query params optionnels:
 *     ?saleDate=2024-01-15   → Date de la vente (requis pour l'API)
 *     ?presale=true          → Mode prevente
 * - /sales-home/prevente     → Mode prevente (pas d'encaissement)
 * - /sales-home/prevente/edit/:id → Editer une prevente existante
 */
export const SALES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [UserRouteAccessService],
   // title: 'Point de vente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Point de vente',
    },
  },


  {
    path: 'prevente',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [UserRouteAccessService],
   // title: 'Prevente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Prevente',
    },
  },
  {
    path: 'devis',
    loadComponent: () => import('./feature/devis-home/devis-home.component').then(m => m.DevisHomeComponent),
    canActivate: [UserRouteAccessService],
//    title: 'Devis',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Devis',
    },
  },
  {
    path: 'gestion',
    loadComponent: () => import('./feature/sales-management-home/sales-management-home.component').then(m => m.SalesManagementHomeComponent),
    canActivate: [UserRouteAccessService],
   // title: 'Gestion des ventes',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Gestion des ventes',
    },
  }
];
