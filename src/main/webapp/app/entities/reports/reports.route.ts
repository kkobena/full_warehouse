import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const reportsRoute: Routes = [
  {
    path: 'sales',
    loadComponent: () => import('./sales-reports/sales-reports.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: "Rapports Chiffre d'Affaires",
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'stock',
    loadComponent: () => import('./stock-reports/stock-reports.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Rapports Stock & Inventaire',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'finance',
    loadComponent: () => import('./finance-reports/finance-reports.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_CAISSIER],
      pageTitle: 'Rapports Trésorerie & Finance',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'partners',
    loadComponent: () => import('./partners-reports/partners-reports.component'),
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Rapports Clients & Fournisseurs',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: '',
    redirectTo: 'sales',
    pathMatch: 'full',
  },
];

export default reportsRoute;
