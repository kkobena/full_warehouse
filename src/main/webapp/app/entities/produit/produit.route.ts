import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IProduit, Produit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';

export const ProduitResolve = (route: ActivatedRouteSnapshot): Observable<null | IProduit> => {
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
        })
      );
  }
  return of(new Produit());
};
const produitRoute: Routes = [
  {
    path: '',
    loadComponent: () => import('./produit.component').then(m => m.ProduitComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      defaultSort: 'id,asc'
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/view',
    loadComponent: () => import('./transaction/transaction.component').then(m => m.TransactionComponent),
    resolve: {
      produit: ProduitResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT]
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'transaction',
    loadComponent: () => import('./transaction/transaction.component').then(m => m.TransactionComponent),
    resolve: {
      produit: ProduitResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT]
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: 'new',
    loadComponent: () => import('./produit-update.component').then(m => m.ProduitUpdateComponent),
    resolve: {
      produit: ProduitResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT]
    },
    canActivate: [UserRouteAccessService]
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./produit-update.component').then(m => m.ProduitUpdateComponent),
    resolve: {
      produit: ProduitResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT]
    },
    canActivate: [UserRouteAccessService]
  },

  {
    path: ':id/detail',
    loadComponent: () => import('./detail-produit-form/detail-produit-form.component').then(m => m.DetailProduitFormComponent),
    resolve: {
      produit: ProduitResolve
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT]
    },
    canActivate: [UserRouteAccessService]
  }
];
export default produitRoute;
