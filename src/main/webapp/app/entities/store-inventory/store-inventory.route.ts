import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { IStoreInventory, StoreInventory } from 'app/shared/model/store-inventory.model';
import { StoreInventoryService } from './store-inventory.service';
import { StoreInventoryComponent } from './store-inventory.component';
import { StoreInventoryDetailComponent } from './store-inventory-detail.component';
import { StoreInventoryUpdateComponent } from './store-inventory-update.component';

@Injectable({ providedIn: 'root' })
export class StoreInventoryResolve implements Resolve<IStoreInventory> {
  constructor(private service: StoreInventoryService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<IStoreInventory> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((storeInventory: HttpResponse<StoreInventory>) => {
          if (storeInventory.body) {
            return of(storeInventory.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new StoreInventory());
  }
}

export const storeInventoryRoute: Routes = [
  {
    path: '',
    component: StoreInventoryComponent,
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.storeInventory.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.storeInventory.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.storeInventory.home.title',
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
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.storeInventory.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
