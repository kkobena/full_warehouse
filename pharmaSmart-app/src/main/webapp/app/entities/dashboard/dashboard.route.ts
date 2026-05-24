import { Routes } from '@angular/router';

const dashboardRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./customizable-dashboard/customizable-dashboard.component'),
    data: { pageTitle: 'Dashboard Personnalisable' },
  },
];

export default dashboardRoutes;
