import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Resolve, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IRayon, Rayon } from '../../shared/model/rayon.model';
import { RayonService } from './rayon.service';
import { RayonComponent } from './rayon.component';

@Injectable({ providedIn: 'root' })
export class RayonResolve implements Resolve<IRayon> {
  constructor(private service: RayonService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IRayon> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        mergeMap((sales: HttpResponse<Rayon>) => {
          if (sales.body) {
            return of(sales.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Rayon());
  }
}

export const rayonRoute: Routes = [
  {
    path: '',
    component: RayonComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.RAYON],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.rayon.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
