import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const reportsRoute: Routes = [
  {
    path: 'stock-alerts',
    loadComponent: () => import('./stock-alerts/stock-alerts.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Alertes Stock',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'cash-register-report',
    loadComponent: () => import('./cash-register-report/cash-register-report.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER],
      pageTitle: 'Rapport de Caisse',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'tiers-payant-creances',
    loadComponent: () => import('./tiers-payant-creances/tiers-payant-creances.component'),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Créances Tiers-Payants',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'sales-summary',
    loadComponent: () => import('./sales-summary/sales-summary.component'),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Synthèse des Ventes',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'top-products',
    loadComponent: () => import('./top-products/top-products.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Top Produits',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'stock-valuation',
    loadComponent: () => import('./stock-valuation/stock-valuation.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Valorisation du Stock',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'stock-rotation',
    loadComponent: () => import('./stock-rotation/stock-rotation.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Rotation du Stock',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'customer-segmentation',
    loadComponent: () => import('./customer-segmentation/customer-segmentation.component'),
    data: {
      authorities: [Authority.ADMIN],
      pageTitle: 'Segmentation Clients',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default reportsRoute;
