import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IMotifRetourProduit, MotifRetourProduit } from '../../shared/model/motif-retour-produit.model';
import { ModifRetourProduitService } from './motif-retour-produit.service';
import { MotifRetourProduitComponent } from './motif-retour-produit.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<IMotifRetourProduit> {
  constructor(private service: ModifRetourProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IMotifRetourProduit> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<MotifRetourProduit>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new MotifRetourProduit());
  }
}

export const motifRetourProduitRoute: Routes = [
  {
    path: '',
    component: MotifRetourProduitComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.MotifRetourProduit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
