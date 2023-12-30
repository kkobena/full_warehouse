import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Commande, ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { CommandeComponent } from './commande.component';
import { CommandeUpdateComponent } from './commande-update.component';
import { CommandeStockEntryComponent } from './commande-stock-entry.component';
import DeliveryResolver from './delevery/delivery.resolver';

export const CommandeResolve = (route: ActivatedRouteSnapshot): Observable<null | ICommande> => {
  const id = route.params['id'];
  if (id) {
    if (route.url.some(url => url.path.includes('stock-entry'))) {
      return inject(CommandeService)
        .findSaisieEntreeStock(id)
        .pipe(
          mergeMap((commande: HttpResponse<ICommande>) => {
            if (commande.body) {
              return of(commande.body);
            } else {
              inject(Router).navigate(['404']);
              return EMPTY;
            }
          }),
        );
    }
    return inject(CommandeService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<ICommande>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Commande());
};

const commandeRoute: Routes = [
  {
    path: '',
    component: CommandeComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'new',
    component: CommandeUpdateComponent,
    /*
    children: [{ path: ':id', component: ProductDetailComponent }],
    <a [routerLink]="['./', product.id]">{{product.name}}</a>
    this.router.navigate(['./', product.id], { relativeTo: this.route });
    */
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: CommandeUpdateComponent,
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  /* {
     path: ':id/stock-entry',
     component: CommandeStockEntryComponent,
     resolve: {
       commande: CommandeResolve,
     },
     data: {
       authorities: [Authority.USER],
       pageTitle: 'warehouseApp.commande.home.title',
     },
     canActivate: [UserRouteAccessService],
   },*/
  {
    path: ':id/stock-entry',
    component: CommandeStockEntryComponent,
    resolve: {
      delivery: DeliveryResolver,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default commandeRoute;
