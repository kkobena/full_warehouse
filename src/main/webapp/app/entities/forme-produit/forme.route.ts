import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { FormeProduitService } from './forme-produit.service';
import { FormProduit, IFormProduit } from '../../shared/model/form-produit.model';
import { FormeProduitComponent } from './forme-produit.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<IFormProduit> {
  constructor(private service: FormeProduitService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IFormProduit> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<FormProduit>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new FormProduit());
  }
}

export const formeRoute: Routes = [
  {
    path: '',
    component: FormeProduitComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.formeProduit.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
