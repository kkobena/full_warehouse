import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { IRemise, Remise } from '../../shared/model/remise.model';
import { RemiseService } from './remise.service';

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
    loadComponent: () => import('./remise-nav/remise-nav.component').then(m => m.RemiseNavComponent),
    data: { defaultSort: 'id,asc' },
  },
];

export default remiseRoute;
