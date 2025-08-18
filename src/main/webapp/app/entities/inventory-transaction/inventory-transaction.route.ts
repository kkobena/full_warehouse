import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const inventoryTransactionRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./inventory-transaction.component').then(m => m.InventoryTransactionComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.INVENTORY_TRANSACTION],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  }
];
export default inventoryTransactionRoute;
