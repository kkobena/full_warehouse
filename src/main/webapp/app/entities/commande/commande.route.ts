import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Commande, ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';


import SuggestionResolver from './suggestion/suggestion.resolver';

export const CommandeResolve = (route: ActivatedRouteSnapshot): Observable<null | ICommande> => {
  const id = route.params['id'];
  const orderDate = route.params['orderDate'];
  if (id) {
    if (route.url.some(url => url.path.includes('stock-entry'))) {
      return inject(CommandeService)
        .findSaisieEntreeStock({ id: id, orderDate: orderDate })
        .pipe(
          mergeMap((commande: HttpResponse<ICommande>) => {
            if (commande.body) {
              return of(commande.body);
            } else {
              inject(Router).navigate(['404']);
              return EMPTY;
            }
          })
        );
    }
    return inject(CommandeService)
      .find({ id: id, orderDate: orderDate })
      .pipe(
        mergeMap((res: HttpResponse<ICommande>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        })
      );
  }
  return of(new Commande());
};

const commandeRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./commande.component').then(m => m.CommandeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE,Authority.ROLE_RESPONSABLE_COMMANDE],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.commande.home.title'
    },
    canActivate: [UserRouteAccessService]
  },

  {
    path: 'new',
    loadComponent: () => import('./commande-update.component').then(m => m.CommandeUpdateComponent),
    /*
    children: [{ path: ':id', component: ProductDetailComponent }],
    <a [routerLink]="['./', product.id]">{{product.name}}</a>
    this.router.navigate(['./', product.id], { relativeTo: this.route });
    */
    resolve: {
      commande: CommandeResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/:orderDate/edit',
    loadComponent: () => import('./commande-update.component').then(m => m.CommandeUpdateComponent),
    resolve: {
      commande: CommandeResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title'
    },
    canActivate: [UserRouteAccessService]
  },


  {
    path: ':id/suggestion',
    loadComponent: () => import('./suggestion/edit-suggestion.component').then(m => m.EditSuggestionComponent),
    resolve: {
      suggestion: SuggestionResolver
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'retour-fournisseur/new',
    loadComponent: () => import('./retour_fournisseur/supplier-returns.component').then(m => m.SupplierReturnsComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE,Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Retour Fournisseur'
    },
    canActivate: [UserRouteAccessService]
  }
];

export default commandeRoute;
