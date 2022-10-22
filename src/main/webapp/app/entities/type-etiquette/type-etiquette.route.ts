import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { TypeEtiquetteComponent } from './type-etiquette.component';
import { ITypeEtiquette, TypeEtiquette } from '../../shared/model/type-etiquette.model';
import { TypeEtiquetteService } from './type-etiquette.service';
@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<ITypeEtiquette> {
  constructor(private service: TypeEtiquetteService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ITypeEtiquette> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<TypeEtiquette>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new TypeEtiquette());
  }
}

export const typeEtiquetteRoute: Routes = [
  {
    path: '',
    component: TypeEtiquetteComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.typeEtiquette.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
