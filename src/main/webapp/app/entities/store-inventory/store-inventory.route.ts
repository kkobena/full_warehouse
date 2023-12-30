import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IStoreInventory, StoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';
import { StoreInventoryComponent } from './store-inventory.component';
import { StoreInventoryDetailComponent } from './store-inventory-detail.component';
import { StoreInventoryUpdateComponent } from './store-inventory-update.component';

export const StoreInventoryResolve = (route: ActivatedRouteSnapshot): Observable<null | IStoreInventory> => {
  const id = route.params['id'];
  if (id) {
    return inject(StoreInventoryService)
      .find(id)
      .pipe(
        mergeMap((res: HttpResponse<IStoreInventory>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new StoreInventory());
};
const storeInventoryRoute: Routes = [
  {
    path: '',
    component: StoreInventoryComponent,
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: StoreInventoryDetailComponent,
    resolve: {
      storeInventory: StoreInventoryResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: StoreInventoryUpdateComponent,
    resolve: {
      storeInventory: StoreInventoryResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: StoreInventoryUpdateComponent,
    resolve: {
      storeInventory: StoreInventoryResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
    },
    canActivate: [UserRouteAccessService],
  },
];
export default storeInventoryRoute;
