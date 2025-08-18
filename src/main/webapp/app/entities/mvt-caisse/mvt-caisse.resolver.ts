import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { HttpResponse } from '@angular/common/http';
import { FinancialTransaction } from '../cash-register/model/cash-register.model';
import { MvtCaisseServiceService } from './mvt-caisse-service.service';

const MvtCaisseResolver = (route: ActivatedRouteSnapshot): Observable<null | FinancialTransaction> => {
  const id = route.params['id'];
  if (id) {
    return inject(MvtCaisseServiceService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<FinancialTransaction>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        })
      );
  }
  return of(new FinancialTransaction());
};

export default MvtCaisseResolver;
