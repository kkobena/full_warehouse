import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';
import { inject } from '@angular/core';
import { FactureService } from '../facturation/facture.service';
import { HttpResponse } from '@angular/common/http';
import { ReglementFactureDossier } from './model/reglement-facture-dossier.model';

export const doReglementResolver = (route: ActivatedRouteSnapshot): Observable<null | ReglementFactureDossier[]> => {
  const id = route.params['id'];
  const typeFacture = route.params['typeFacture'];
  const invoiceDate = route.params['invoiceDate'];
  if (id) {
    return inject(FactureService)
      .findDossierReglement(
        {
          id: id,
          invoiceDate: invoiceDate,
        },
        typeFacture,
        {
          page: 0,
          size: 999999,
        },
      )
      .pipe(
        mergeMap((res: HttpResponse<ReglementFactureDossier[]>) => {
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
