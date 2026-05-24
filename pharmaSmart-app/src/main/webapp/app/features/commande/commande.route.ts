import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRouteSnapshot, Router, Routes } from '@angular/router';
import { EMPTY, mergeMap, Observable, of } from 'rxjs';

import { Commande, ICommande } from 'app/shared/model/commande.model';
import { CommandeService } from '../../entities/commande/commande.service';

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
      defaultSort: 'id,asc',
      pageTitle: 'warehouseApp.commande.home.title',
    },
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./feature/commande-detail/commande-detail.component').then(m => m.CommandeDetailComponent),
    resolve: {
      commande: CommandeResolve,
    },
    data: { pageTitle: 'warehouseApp.commande.home.title' },
  },
  {
    path: ':id/:orderDate/edit',
    loadComponent: () =>
      import('./feature/commande-detail/commande-detail.component').then(m => m.CommandeDetailComponent),
    resolve: {
      commande: CommandeResolve,
    },
    data: { pageTitle: 'warehouseApp.commande.home.title' },
  },
  {
    path: 'retour-fournisseur/new',
    loadComponent: () =>
      import('./feature/retour-fournisseur/ui/supplier-returns/supplier-returns.component').then(
        m => m.SupplierReturnsComponent,
      ),
    data: { pageTitle: 'Nouveau Retour Fournisseur' },
  },
  {
    path: 'retour-fournisseur/:id/edit',
    loadComponent: () =>
      import('./feature/retour-fournisseur/ui/supplier-returns/supplier-returns.component').then(
        m => m.SupplierReturnsComponent,
      ),
    data: { pageTitle: 'Modifier le Retour Fournisseur' },
  },
  {
    path: 'suggestions',
    loadComponent: () =>
      import('./feature/suggestion/suggestion-home.component').then(m => m.SuggestionHomeComponent),
    data: { pageTitle: 'Suggestions de commande' },
  },
  {
    path: 'semois-classe-config',
    loadComponent: () =>
      import('./feature/semois-classe-config/semois-classe-config.component').then(
        m => m.SemoisClasseConfigComponent,
      ),
    data: { pageTitle: 'Configuration SEMOIS — Classes de criticité' },
  },
];

export default commandeRoute;
