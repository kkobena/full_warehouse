import { inject } from "@angular/core";
import { HttpResponse } from "@angular/common/http";
import { ActivatedRouteSnapshot, Router } from "@angular/router";
import { EMPTY, mergeMap, Observable, of } from "rxjs";

import { IProduit, Produit } from "app/shared/model/produit.model";
import { ProduitService } from "./produit.service";

export const ProduitResolve = (route: ActivatedRouteSnapshot): Observable<null | IProduit> => {
  const id = route.params["id"];
  if (id) {
    return inject(ProduitService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IProduit>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(["404"]);
            return EMPTY;
          }
        })
      );
  }
  return of(new Produit());
};


