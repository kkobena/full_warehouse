import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../config/authority.constants';

const repartitionStockRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./repartition-stock.component').then(m => m.RepartitionStockComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.USER],
      defaultSort: 'created,desc',
      pageTitle: 'Répartition de Stock',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default repartitionStockRoutes;
