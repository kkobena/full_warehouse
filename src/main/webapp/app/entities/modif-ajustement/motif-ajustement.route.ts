import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IMotifAjustement, MotifAjustement } from '../../shared/model/motif-ajustement.model';
import { ModifAjustementService } from './motif-ajustement.service';
import { ModifAjustementComponent } from './modif-ajustement.component';

@Injectable({ providedIn: 'root' })
export class SalesResolve implements Resolve<IMotifAjustement> {
  constructor(private service: ModifAjustementService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IMotifAjustement> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((sales: HttpResponse<MotifAjustement>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new MotifAjustement());
  }
}

export const motifAjustementRoute: Routes = [
  {
    path: '',
    component: ModifAjustementComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.motifAjustement.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
