import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

export const INVENTORY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/inventory-home/inventory-home.component').then(m => m.InventoryHomeComponent),
    canActivate: [UserRouteAccessService],
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
      pageTitle: 'Inventaires',
    },
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./feature/inventory-editor/inventory-editor.component').then(m => m.InventoryEditorComponent),
    canActivate: [UserRouteAccessService],
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
      pageTitle: "Éditeur d'inventaire",
    },
  },
];

export default INVENTORY_ROUTES;
