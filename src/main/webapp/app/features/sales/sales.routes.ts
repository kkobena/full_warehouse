import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';

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
      pageTitle: 'Point de vente',
    },
  },
  {
    path: 'edit/:id',
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la vente',
    data: {
      pageTitle: 'Modifier la vente',
      isEdit: true,
    },
  },
  {
    path: 'prevente',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Prevente',
    data: {
      pageTitle: 'Prevente',
    },
  },
  {
    path: 'prevente/edit/:id',
    loadComponent: () => import('./feature/presale-home/presale-home.component').then(m => m.PresaleHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la prevente',
    data: {
      pageTitle: 'Modifier la prevente',
      isEdit: true,
    },
  },
  // Retro-compatibilite avec l'ancienne route
  {
    path: ':id/:saleDate/:isPresale/edit',
    redirectTo: 'edit/:id',
    pathMatch: 'full',
  },
];
