import { Routes } from '@angular/router';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';

const mvtCaisseRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./mvt-caisse.component').then(m => m.MvtCaisseComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.MVT_CAISSE, Authority.TABLEAU_PHARMACIEN],
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

  // {
  //   path: ':id/stock-entry',
  //   // component: CommandeStockEntryComponent,
  //   resolve: {
  //     //   mvtCaisse: DeliveryResolver,
  //   },
  //   data: {
  //     authorities: [Authority.ADMIN, Authority.MVT_CAISSE, Authority.TABLEAU_PHARMACIEN],
  //   },
  //   canActivate: [UserRouteAccessService],
  // },
];
export default mvtCaisseRoute;
