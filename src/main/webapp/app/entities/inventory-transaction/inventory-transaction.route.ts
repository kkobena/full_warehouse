import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { InventoryTransactionComponent } from './inventory-transaction.component';

const inventoryTransactionRoute: Routes = [
  {
    path: '',
    component: InventoryTransactionComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.INVENTORY_TRANSACTION],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default inventoryTransactionRoute;
