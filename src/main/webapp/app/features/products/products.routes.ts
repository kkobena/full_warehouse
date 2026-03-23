import { Routes } from '@angular/router';
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';

export const PRODUCTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/produit-home/produit-home.component').then(m => m.ProduitHomeComponent),
    data: { authorities: [Authority.USER] },
    canActivate: [UserRouteAccessService],
  },
];

export default PRODUCTS_ROUTES;
