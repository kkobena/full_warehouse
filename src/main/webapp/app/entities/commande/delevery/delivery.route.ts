import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

import { DeliveryComponent } from './delivery.component';
import { CommandeStockEntryComponent } from '../commande-stock-entry.component';
import { DeliveryResolver } from './delivery.resolver';

export const deliveryRoute: Routes = [
  {
    path: '',
    component: DeliveryComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_ENTREE_STOCK, Authority.COMMANDE],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.delivery.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  /*{
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
    component: CommandeStockEntryComponent,
    resolve: {
      delivery: DeliveryResolver,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.GESTION_ENTREE_STOCK, Authority.COMMANDE],
      pageTitle: 'warehouseApp.delivery.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
