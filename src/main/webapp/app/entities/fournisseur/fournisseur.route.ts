import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { Fournisseur, IFournisseur } from '../../shared/model/fournisseur.model';
import { FournisseurService } from './fournisseur.service';
import { FournisseurComponent } from './fournisseur.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<IFournisseur> {
  constructor(private service: FournisseurService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IFournisseur> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<Fournisseur>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Fournisseur());
  }
}

export const fournisseurRoute: Routes = [
  {
    path: '',
    component: FournisseurComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.fournisseur.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
