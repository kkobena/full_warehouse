import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, Routes, Router } from '@angular/router';
import { Observable, of, EMPTY } from 'rxjs';
import { flatMap } from 'rxjs/operators';

import { Authority } from 'app/shared/constants/authority.constants';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { ICommande, Commande } from 'app/shared/model/commande.model';
import { CommandeService } from './commande.service';
import { CommandeComponent } from './commande.component';
import { CommandeDetailComponent } from './commande-detail.component';
import { CommandeUpdateComponent } from './commande-update.component';

@Injectable({ providedIn: 'root' })
export class CommandeResolve implements Resolve<ICommande> {
  constructor(private service: CommandeService, private router: Router) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ICommande> | Observable<never> {
    const id = route.params['id'];
    if (id) {
      return this.service.find(id).pipe(
        flatMap((commande: HttpResponse<Commande>) => {
          if (commande.body) {
            return of(commande.body);
          } else {
            this.router.navigate(['404']);
            return EMPTY;
          }
        })
      );
    }
    return of(new Commande());
  }
}

export const commandeRoute: Routes = [
  {
    path: '',
    component: CommandeComponent,
    data: {
      authorities: [Authority.USER],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: CommandeDetailComponent,
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: CommandeUpdateComponent,
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: CommandeUpdateComponent,
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.USER],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
];
