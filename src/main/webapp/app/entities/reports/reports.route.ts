import { Routes } from '@angular/router';
import { AuthGuard } from 'app/core/auth/auth.guard';

const reportsRoute: Routes = [
  {
    path: 'sales',
    loadComponent: () => import('./sales-reports/sales-reports.component'),
    data: {
      pageTitle: "Rapports Chiffre d'Affaires",
      abilitySubject: 'rapport-ventes',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'stock',
    loadComponent: () => import('./stock-reports/stock-reports.component'),
    data: {
      pageTitle: 'Rapports Stock & Inventaire',
      abilitySubject: 'rapport-stock',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'partners',
    loadComponent: () => import('./partners-reports/partners-reports.component'),
    data: {
      pageTitle: 'Rapports Clients & Fournisseurs',
      abilitySubject: 'rapport-partners',
    },
    canActivate: [AuthGuard],
  },
  {
    path: 'finance',
    loadComponent: () => import('./finance-reports/finance-reports.component'),
    data: {
      pageTitle: 'Rapports Finance & Rentabilité',
      abilitySubject: 'rapport-finance',
    },
    canActivate: [AuthGuard],
  },
  {
    path: '',
    redirectTo: 'sales',
    pathMatch: 'full',
  },
];

export default reportsRoute;
