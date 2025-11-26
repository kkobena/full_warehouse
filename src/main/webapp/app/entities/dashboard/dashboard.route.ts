import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const dashboardRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./customizable-dashboard/customizable-dashboard.component'),
    data: {
      pageTitle: 'Dashboard Personnalisable',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default dashboardRoutes;
