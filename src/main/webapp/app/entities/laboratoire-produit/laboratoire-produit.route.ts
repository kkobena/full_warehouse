import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { LaboratoireProduitService } from './laboratoire-produit.service';
import { ILaboratoire, Laboratoire } from '../../shared/model/laboratoire.model';
import { LaboratoireProduitComponent } from './laboratoire-produit.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<ILaboratoire> {
  constructor(private service: LaboratoireProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ILaboratoire> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((sales: HttpResponse<Laboratoire>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Laboratoire());
  }
}

export const laboratoireProduitRoute: Routes = [
  {
    path: '',
    component: LaboratoireProduitComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.LABORATOIRE],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.laboratoire.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
