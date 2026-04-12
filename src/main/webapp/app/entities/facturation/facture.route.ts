import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Facture } from './facture.model';
import { FactureService } from './facture.service';

export const FactureResolve = (route: ActivatedRouteSnapshot): Observable<null | Facture> => {
  const id = route.params['id'];
  const invoiceDate = route.params['invoiceDate'];
  if (id) {
    return inject(FactureService)
      .find({ id, invoiceDate })
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
    path: ':id/:invoiceDate/group-view',
    loadComponent: () => import('./groupe-facture-detail/groupe-facture-detail.component').then(m => m.GroupeFactureDetailComponent),
    resolve: { facture: FactureResolve },
  },
];

export default factureRoute;
