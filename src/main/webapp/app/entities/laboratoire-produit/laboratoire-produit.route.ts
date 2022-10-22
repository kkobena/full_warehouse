import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
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
        flatMap((sales: HttpResponse<Laboratoire>) => {
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
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.laboratoire.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
