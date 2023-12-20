import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IProduit, Produit } from 'app/shared/model/produit.model';
import { ProduitService } from './produit.service';
import { ProduitComponent } from './produit.component';
import { ProduitDetailComponent } from './produit-detail.component';
import { ProduitUpdateComponent } from './produit-update.component';
import { DetailProduitFormComponent } from './detail-produit-form/detail-produit-form.component';

@Injectable({ providedIn: 'root' })
export class ProduitResolve implements Resolve<IProduit> {
  constructor(private service: ProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IProduit> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((produit: HttpResponse<Produit>) => {
          if (produit.body) {
            return of(produit.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Produit());
  }
}

export const produitRoute: Routes = [
  {
    path: '',
    component: ProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.produit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: ProduitDetailComponent,
    resolve: {
      produit: ProduitResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      pageTitle: 'warehouseApp.produit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: ProduitUpdateComponent,
    resolve: {
      produit: ProduitResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      pageTitle: 'warehouseApp.produit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: ProduitUpdateComponent,
    resolve: {
      produit: ProduitResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      pageTitle: 'warehouseApp.produit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: ':id/detail',
    component: DetailProduitFormComponent,
    resolve: {
      produit: ProduitResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.PRODUIT],
      pageTitle: 'warehouseApp.produit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
