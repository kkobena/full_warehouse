import { ActivatedRouteSnapshot, Router, Routes } from "@angular/router";
import { UserRouteAccessService } from '../../core/auth/user-route-access.service';
import { Authority } from '../../shared/constants/authority.constants';
import { EMPTY, mergeMap, Observable, of } from "rxjs";
import { IProduit } from "../../shared/model";
import { inject } from "@angular/core";
import { ProduitService } from "../../entities/produit/produit.service";
import { HttpResponse } from "@angular/common/http";
import { Produit } from "../../shared/model/produit.model";

export const ProductResolve = (route: ActivatedRouteSnapshot): Observable<null | IProduit> => {
  const id = route.params['id'];
  if (id) {
    return inject(ProduitService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IProduit>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Produit());
};
export const PRODUCTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./feature/produit-home/produit-home.component').then(m => m.ProduitHomeComponent),
    data: { authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE, Authority.PRODUIT],},
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    loadComponent: () => import('../../entities/produit/produit-update.component').then(m => m.ProduitUpdateComponent),
    resolve: {
      produit: ProductResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE, Authority.PRODUIT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    loadComponent: () => import('../../entities/produit/produit-update.component').then(m => m.ProduitUpdateComponent),
    resolve: {
      produit: ProductResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.ROLE_RESPONSABLE_COMMANDE, Authority.PRODUIT],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'transaction',
    loadComponent: () => import('../../entities/produit/transaction/transaction.component').then(m => m.TransactionComponent),
    resolve: {
      produit: ProductResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
    },
    canActivate: [UserRouteAccessService],
  }

];

export default PRODUCTS_ROUTES;
