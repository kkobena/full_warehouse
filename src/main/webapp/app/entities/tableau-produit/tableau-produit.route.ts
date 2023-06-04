import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TableauProduitService } from './tableau-produit.service';
import { TableauProduitComponent } from './tableau-produit.component';
import { ITableau, Tableau } from '../../shared/model/tableau.model';
import { ProduitAssociesComponent } from './produits/produit-associes.component';

@Injectable({ providedIn: 'root' })
export class TableauProduitResolve implements Resolve<ITableau> {
  constructor(private service: TableauProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ITableau> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((tableau: HttpResponse<Tableau>) => {
          if (tableau.body) {
            return of(tableau.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Tableau());
  }
}

export const tableauProduitRoute: Routes = [
  {
    path: '',
    component: TableauProduitComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.tableau.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/associe',
    component: ProduitAssociesComponent,
    resolve: {
      tableau: TableauProduitResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.tableau.home.associe',
    },
    canActivate: [UserRouteAccessService],
  },
];
