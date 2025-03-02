import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import DeliveryResolver from './delivery.resolver';

const deliveryRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./delivery.component').then(m => m.DeliveryComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_ENTREE_STOCK, Authority.COMMANDE],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  /* {
    path: ':id/view',
    component: CommandeDetailComponent,
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },*/
  // {
  //   path: 'new',
  //   component: CommandeUpdateComponent,
  //   /*
  //   children: [{ path: ':id', component: ProductDetailComponent }],
  //   <a [routerLink]="['./', product.id]">{{product.name}}</a>
  //   this.router.navigate(['./', product.id], { relativeTo: this.route });
  //   */
  //   resolve: {
  //     commande: DeliveryResolver,
  //   },
  //   data: {
  //     authorities: [Authority.USER],
  //     pageTitle: 'warehouseApp.commande.home.title',
  //   },
  //   canActivate: [UserRouteAccessService],
  // },

  {
    path: ':id/stock-entry',
    loadComponent: () => import('../commande-stock-entry.component').then(m => m.CommandeStockEntryComponent),
    resolve: {
      delivery: DeliveryResolver,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_ENTREE_STOCK, Authority.COMMANDE],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default deliveryRoute;
