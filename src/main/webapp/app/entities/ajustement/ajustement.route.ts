import { inject } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { ActivatedRouteSnapshot, Router, Routes } from "@angular/router";
import { EMPTY, mergeMap, Observable, of } from "rxjs";
import { AjustementService } from "./ajustement.service";

import { Ajust, IAjust } from "../../shared/model/ajust.model";

export const AjustementResolve = (route: ActivatedRouteSnapshot): Observable<null | IAjust> => {
  const id = route.params["id"];
  if (id) {
    return inject(AjustementService)
      .find(id)
      .pipe(
        mergeMap((ajustement: HttpResponse<IAjust>) => {
          if (ajustement.body) {
            return of(ajustement.body);
          } else {
            inject(Router).navigate(["404"]);
            return EMPTY;
          }
        })
      );
  }
  return of(new Ajust());
};
const ajustementRoute: Routes = [];
export default ajustementRoute;
