import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { Facture } from './facture.model';
import { FactureService } from './facture.service';
import { FacturationComponent } from './facturation.component';
import { EditionComponent } from './edition/edition.component';
import { FactureDetailComponent } from './facture-detail/facture-detail.component';

export const FactureResolve = (route: ActivatedRouteSnapshot): Observable<null | Facture> => {
  const id = route.params['id'];
  if (id) {
    return inject(FactureService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<Facture>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return EMPTY;
};
const factureRoute: Routes = [
  {
    path: '',
    component: FacturationComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
      defaultSort: 'id,asc',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: EditionComponent,

    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: FactureDetailComponent,
    resolve: {
      facture: FactureResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.EDITION_FACTURATION, Authority.GESTION_FACTURATION],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default factureRoute;
