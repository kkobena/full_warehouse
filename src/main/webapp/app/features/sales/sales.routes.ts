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
    title: 'Point de vente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Point de vente',
    },
  },
  /* {
    path: 'edit/:id', // Route pour éditer une vente dejà cloturée
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la vente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER],
      pageTitle: 'Modifier la vente',
      isEdit: true,
    },
  },

  {
    path: ':id',
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la vente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER],
      pageTitle: 'Modifier la vente',
    },
  },*/

  {
    path: 'prevente',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Prevente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Prevente',
    },
  },
  {
    path: 'devis',
    loadComponent: () => import('./feature/devis-home/devis-home.component').then(m => m.DevisHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Devis',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Devis',
    },
  } /*,
  {
    path: 'prevente/edit/:id',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la prevente',
    data: {
      authorities: [Authority.ADMIN, Authority.SALES, Authority.ROLE_CAISSIER, Authority.ROLE_VENDEUR],
      pageTitle: 'Modifier la prevente',
    },
  },*/,
];
