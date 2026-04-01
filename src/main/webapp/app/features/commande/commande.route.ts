import {inject} from '@angular/core';
import {HttpResponse} from '@angular/common/http';
import {ActivatedRouteSnapshot, Router, Routes} from '@angular/router';
import {EMPTY, mergeMap, Observable, of} from 'rxjs';

import {Authority} from 'app/shared/constants/authority.constants';
import {UserRouteAccessService} from 'app/core/auth/user-route-access.service';
import {Commande, ICommande} from 'app/shared/model/commande.model';
import {CommandeService} from '../../entities/commande/commande.service';

export const CommandeResolve = (route: ActivatedRouteSnapshot): Observable<null | ICommande> => {
  const id = route.params['id'];
  const orderDate = route.params['orderDate'];
  if (id) {
    if (route.url.some(url => url.path.includes('stock-entry'))) {
      return inject(CommandeService)
        .findSaisieEntreeStock({id, orderDate})
        .pipe(
          mergeMap((commande: HttpResponse<ICommande>) => {
            if (commande.body) {
              return of(commande.body);
            } else {
              inject(Router).navigate(['404']);
              return EMPTY;
            }
          }),
        );
    }
    return inject(CommandeService)
      .find({id, orderDate})
      .pipe(
        mergeMap((res: HttpResponse<ICommande>) => {
          if (res.body) {
            return of(res.body);
          } else {
            inject(Router).navigate(['404']);
            return EMPTY;
          }
        }),
      );
  }
  return of(new Commande());
};

const commandeRoute: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./feature/commande-home/commande-home.component').then(m => m.CommandeHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },

  {
    path: 'new',
    loadComponent: () =>
      import('./feature/commande-detail/commande-detail.component').then(m => m.CommandeDetailComponent),
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/:orderDate/edit',
    loadComponent: () =>
      import('./feature/commande-detail/commande-detail.component').then(m => m.CommandeDetailComponent),
    resolve: {
      commande: CommandeResolve,
    },
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'warehouseApp.commande.home.title',
    },
    canActivate: [UserRouteAccessService],
  },


  {
    path: 'retour-fournisseur/new',
    loadComponent: () =>
      import('./feature/retour-fournisseur/ui/supplier-returns/supplier-returns.component').then(
        m => m.SupplierReturnsComponent,
      ),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Nouveau Retour Fournisseur',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'retour-fournisseur/:id/edit',
    loadComponent: () =>
      import('./feature/retour-fournisseur/ui/supplier-returns/supplier-returns.component').then(
        m => m.SupplierReturnsComponent,
      ),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Modifier le Retour Fournisseur',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'suggestions',
    loadComponent: () =>
      import('./feature/suggestion/suggestion-home.component').then(m => m.SuggestionHomeComponent),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Suggestions de commande',
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'semois-classe-config',
    loadComponent: () =>
      import('./feature/semois-classe-config/semois-classe-config.component').then(
        m => m.SemoisClasseConfigComponent,
      ),
    data: {
      authorities: [Authority.ADMIN, Authority.COMMANDE, Authority.ROLE_RESPONSABLE_COMMANDE],
      pageTitle: 'Configuration SEMOIS — Classes de criticité',
    },
    canActivate: [UserRouteAccessService],
  },
];

export default commandeRoute;
