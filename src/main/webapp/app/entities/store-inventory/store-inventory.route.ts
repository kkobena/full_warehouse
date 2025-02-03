import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { IStoreInventory, StoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';




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
    loadComponent: () => import('./store-inventory.component').then(m => m.StoreInventoryComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.STORE_INVENTORY],
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    loadComponent: () => import('./store-inventory-detail.component').then(m => m.StoreInventoryDetailComponent),
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
    loadComponent: () => import('./store-inventory-update.component').then(m => m.StoreInventoryUpdateComponent),
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
    loadComponent: () => import('./store-inventory-update.component').then(m => m.StoreInventoryUpdateComponent),
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
