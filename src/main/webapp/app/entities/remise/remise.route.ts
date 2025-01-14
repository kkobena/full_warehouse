import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IRemise, Remise } from '../../shared/model/remise.model';
import { RemiseService } from './remise.service';
import { RemiseNavComponent } from './remise-nav/remise-nav.component';

export const RemiseResolve = (route: ActivatedRouteSnapshot): Observable<null | IRemise> => {
  const id = route.params['id'];
  if (id) {
    return inject(RemiseService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IRemise>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Remise());
};
const remiseRoute: Routes = [
  {
    path: '',
    component: RemiseNavComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.REMISE],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
];
export default remiseRoute;
