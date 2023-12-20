import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { FamilleProduit, IFamilleProduit } from '../../shared/model/famille-produit.model';
import { FamilleProduitService } from './famille-produit.service';
import { FamilleProduitComponent } from './famille-produit.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<IFamilleProduit> {
  constructor(private service: FamilleProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IFamilleProduit> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((sales: HttpResponse<FamilleProduit>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new FamilleProduit());
  }
}

export const familleProduitRoute: Routes = [
  {
    path: '',
    component: FamilleProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.FAMILLE_PRODUIT],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.familleProduit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
