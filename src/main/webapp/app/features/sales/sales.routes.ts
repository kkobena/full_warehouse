import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';

/**
 * Sales feature routes
 * Lazy-loaded routes for the sales module
 * 
 * Architecture similaire à l'ancien code:
 * - Route principale avec tabs COMPTANT/ASSURANCE/CARNET
 * - Paramètre :isPresale pour distinguer préventes et ventes normales
 * - Route d'édition de vente clôturée ASSURANCE/CARNET
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
    path: ':id/:saleDate/:isPresale/edit',
    loadComponent: () => import('./feature/sales-home/sales-home.component').then(m => m.SalesHomeComponent),
    canActivate: [UserRouteAccessService],
    title: 'Modifier la vente',
    data: {
      pageTitle: 'Modifier la vente',
      isEdit: true,
    },
  },
  // Routes additionnelles (Phase future - Administration des ventes)
  // - /sales/list : Liste des ventes avec filtres
  // - /sales/:id : Détail en lecture seule
];
